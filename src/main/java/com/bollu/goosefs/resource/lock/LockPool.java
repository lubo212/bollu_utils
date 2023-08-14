package com.bollu.goosefs.resource.lock;

import com.bollu.goosefs.common.constant.Constants;
import com.bollu.goosefs.common.utils.ThreadFactoryUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class LockPool<K> implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(LockPool.class);
  private static final float DEFAULT_LOAD_FACTOR = 0.75f;
  private static final String EVICTOR_THREAD_NAME = "LockPool Evictor";
  private final Map<K, Resource> mPool;
  private final Function<? super K, ? extends ReentrantReadWriteLock> mDefaultLoader;
  private final int mLowWatermark;
  private final int mHighWatermark;
  private final Lock mEvictLock = new ReentrantLock();
  private final Condition mOverHighWatermark = mEvictLock.newCondition();
  private final ExecutorService mEvictor;
  private final Future<?> mEvictorTask;

  public LockPool(Function<? super K, ? extends ReentrantReadWriteLock> defaultLoader,
                  int initialSize, int lowWatermark, int highWatermark, int concurrencyLevel) {
    mDefaultLoader = defaultLoader;
    mLowWatermark = lowWatermark;
    mHighWatermark = highWatermark;
    mPool = new ConcurrentHashMap<>(initialSize, DEFAULT_LOAD_FACTOR, concurrencyLevel);
    mEvictor = Executors.newSingleThreadExecutor(
        ThreadFactoryUtils.build(String.format("%s-%s", EVICTOR_THREAD_NAME, toString()), true));
    mEvictorTask = mEvictor.submit(new Evictor());
  }

  @Override
  public void close() throws IOException {
    mEvictorTask.cancel(true);
    mEvictor.shutdownNow(); // immediately halt the evictor thread.
    try {
      mEvictor.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Failed to await LockPool evictor termination", e);
    }
  }

  public LockResource get(K key, LockMode mode) {
    return get(key, mode, false);
  }

  public RWLockResource get(K key, LockMode mode, boolean useTryLock) {
    Resource resource = getResource(key);
    return new RefCountLockResource(resource.mLock, mode, true, resource.mRefCount, useTryLock);
  }

  public Optional<RWLockResource> tryGet(K key, LockMode mode) {
    Resource resource = getResource(key);
    ReentrantReadWriteLock lock = resource.mLock;
    Lock innerLock;
    switch (mode) {
      case READ:
        innerLock = lock.readLock();
        break;
      case WRITE:
        innerLock = lock.writeLock();
        break;
      default:
        throw new IllegalStateException("Unknown lock mode: " + mode);
    }
    if (!innerLock.tryLock()) {
      return Optional.empty();
    }
    return Optional.of(new RefCountLockResource(lock, mode, false, resource.mRefCount, false));
  }

  public ReentrantReadWriteLock getRawReadWriteLock(K key) {
    return mPool.getOrDefault(key, new Resource(new ReentrantReadWriteLock())).mLock;
  }

  private Resource getResource(K key) {
    Preconditions.checkNotNull(key, "key can not be null");
    Resource resource = mPool.compute(key, (k, v) -> {
      if (v != null && v.mRefCount.incrementAndGet() > 0) {
        // If the entry is to be removed, ref count will be INT_MIN, so incrementAndGet will < 0.
        v.mIsAccessed = true;
        return v;
      }
      return new Resource(mDefaultLoader.apply(k));
    });
    if (mPool.size() > mHighWatermark) {
      if (mEvictLock.tryLock()) {
        try {
          mOverHighWatermark.signal();
        } finally {
          mEvictLock.unlock();
        }
      }
    }
    return resource;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("lowWatermark", mLowWatermark)
        .add("highWatermark", mHighWatermark)
        .add("size", mPool.size())
        .toString();
  }

  public boolean containsKey(K key) {
    Preconditions.checkNotNull(key, "key can not be null");
    return mPool.containsKey(key);
  }

  public int size() {
    return mPool.size();
  }

  @VisibleForTesting
  public Map<K, ReentrantReadWriteLock> getEntryMap() {
    Map<K, ReentrantReadWriteLock> entries = new HashMap<>();
    mPool.forEach((key, value) -> entries.put(key, value.mLock));
    return entries;
  }

  private final class Evictor implements Runnable {
    private static final long OVER_HIGH_WATERMARK_LOG_INTERVAL = Constants.MINUTE_MS;
    private static final int EVICTION_MAX_AWAIT_TIME = 30 * Constants.SECOND_MS;
    private long mLastSizeWarningTime = 0;
    private Iterator<Map.Entry<K, Resource>> mIterator;

    public Evictor() {
      mIterator = mPool.entrySet().iterator();
    }

    @Override
    public void run() {
      try {
        while (!Thread.interrupted()) {
          awaitAndEvict();
        }
      } catch (InterruptedException e) {
        // Allow thread to exit.
      }
    }

    /**
     * Blocks until the size of the pool exceeds the high watermark, evicts entries with zero
     * references until pool size decreases below the low watermark or the whole pool is scanned.
     */
    private void awaitAndEvict() throws InterruptedException {
      try (LockResource l = new LockResource(mEvictLock)) {
        while (mPool.size() <= mHighWatermark) {
          mOverHighWatermark.await(EVICTION_MAX_AWAIT_TIME, TimeUnit.MILLISECONDS);
        }
        int numToEvict = mPool.size() - mLowWatermark;
        // The first round of scan uses the mIterator left from last eviction.
        // Then scan the pool from a new iterator for at most two round:
        // first round to mark candidate.mIsAccessed as false,
        // second round to remove the candidate from the pool.
        int roundToScan = 3;
        while (numToEvict > 0 && roundToScan > 0) {
          if (!mIterator.hasNext()) {
            mIterator = mPool.entrySet().iterator();
            roundToScan--;
          }

          Map.Entry<K, Resource> candidateMapEntry = mIterator.next();
          Resource candidate = candidateMapEntry.getValue();
          if (candidate.mIsAccessed) {
            candidate.mIsAccessed = false;
          } else {
            if (candidate.mRefCount.compareAndSet(0, Integer.MIN_VALUE)) {
              mIterator.remove();
              numToEvict--;
            }
          }
        }

        if (mPool.size() >= mHighWatermark) {
          if (System.currentTimeMillis() - mLastSizeWarningTime
              > OVER_HIGH_WATERMARK_LOG_INTERVAL) {
            LOG.warn("LockPool size grows over high watermark: "
                    + "pool size = {}, low watermark = {}, high watermark = {}",
                mPool.size(), mLowWatermark, mHighWatermark);
            mLastSizeWarningTime = System.currentTimeMillis();
          }
        }
      }
    }
  }

  /**
   * Resource containing the lock and other information to be stored in the pool.
   */
  private static final class Resource {
    private final ReentrantReadWriteLock mLock;
    //mIsAccessed这个是做什么的？
    private volatile boolean mIsAccessed;
    private AtomicInteger mRefCount;

    private Resource(ReentrantReadWriteLock lock) {
      mLock = lock;
      mIsAccessed = false;
      mRefCount = new AtomicInteger(1);
    }
  }
}

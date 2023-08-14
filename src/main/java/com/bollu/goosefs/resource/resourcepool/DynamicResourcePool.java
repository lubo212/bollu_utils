package com.bollu.goosefs.resource.resourcepool;

import com.bollu.goosefs.common.constant.Constants;
import com.bollu.goosefs.common.utils.ThreadFactoryUtils;
import com.bollu.goosefs.resource.lock.LockResource;
import com.bollu.goosefs.time.SystemClock;
import com.codahale.metrics.Counter;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class DynamicResourcePool<T> implements Pool<T> {
  public enum SelectionPolicy {
    // first-in-first-out, use the hottest resource
    FIFO,
    // last-in-first-out, use the coldest resource
    LIFO,
  }

  private static final Logger LOG = LoggerFactory.getLogger(DynamicResourcePool.class);

  protected class ResourceInternal<R> {
    /**
     * The resource.
     */
    private R mResource;

    /**
     * The last access time in ms.
     */
    private long mLastAccessTimeMs;

    /**
     * @param lastAccessTimeMs the last access time in ms
     */
    public void setLastAccessTimeMs(long lastAccessTimeMs) {
      mLastAccessTimeMs = lastAccessTimeMs;
    }

    /**
     * @return the last access time in ms
     */
    public long getLastAccessTimeMs() {
      return mLastAccessTimeMs;
    }

    /**
     * Creates a {@link ResourceInternal} instance.
     *
     * @param resource the resource
     */
    public ResourceInternal(R resource) {
      mResource = resource;
      mLastAccessTimeMs = mClock.millis();
    }
  }

  private static final int BLOCK_MASTER_CLIENT_POOL_GC_THREADPOOL_SIZE = 1;
  private static final ScheduledExecutorService GC_EXECUTOR =
      new ScheduledThreadPoolExecutor(BLOCK_MASTER_CLIENT_POOL_GC_THREADPOOL_SIZE,
          ThreadFactoryUtils.build("DefaultPoolGcThreads-%d", true));

  /**
   * Options to initialize a Dynamic resource pool.
   */
  public static final class Options {
    /**
     * The max capacity.
     */
    private int mMaxCapacity = 1024;

    /**
     * The min capacity.
     */
    private int mMinCapacity = 1;

    /**
     * The initial delay.
     */
    private long mInitialDelayMs = 100;

    /**
     * The gc interval.
     */
    private long mGcIntervalMs = 120L * Constants.SECOND_MS;

    /**
     * The gc executor.
     */
    private ScheduledExecutorService mGcExecutor = GC_EXECUTOR;

    /**
     * If set to true, when a resource needs to be taken from the pool, the last returned resource
     * will take priority. {@link #acquire()} tends to return a different object every time.
     * If set to false, the first returned resource will take priority.
     * {@link #acquire()} tends to reuse the most fresh resource if possible.
     */
    private SelectionPolicy mSelectionPolicy = SelectionPolicy.LIFO;

    /**
     * @return the max capacity
     */
    public int getMaxCapacity() {
      return mMaxCapacity;
    }

    /**
     * @return the min capacity
     */
    public int getMinCapacity() {
      return mMinCapacity;
    }

    /**
     * @return the initial delay
     */
    public long getInitialDelayMs() {
      return mInitialDelayMs;
    }

    /**
     * @return the gc interval
     */
    public long getGcIntervalMs() {
      return mGcIntervalMs;
    }

    /**
     * @return the gc executor
     */
    public ScheduledExecutorService getGcExecutor() {
      return mGcExecutor;
    }

    /**
     * @return the selection policy
     */
    public SelectionPolicy getSelectionPolicy() {
      return mSelectionPolicy;
    }

    /**
     * @param policy how to select a client from the pool
     * @return the updated object
     */
    public Options setSelectionPolicy(SelectionPolicy policy) {
      mSelectionPolicy = policy;
      return this;
    }

    /**
     * @param maxCapacity the max capacity
     * @return the updated object
     */
    public Options setMaxCapacity(int maxCapacity) {
      Preconditions.checkArgument(maxCapacity >= 1);
      mMaxCapacity = maxCapacity;
      return this;
    }

    /**
     * @param minCapacity the min capacity
     * @return the updated object
     */
    public Options setMinCapacity(int minCapacity) {
      Preconditions.checkArgument(minCapacity >= 0);
      mMinCapacity = minCapacity;
      return this;
    }

    /**
     * @param initialDelayMs the initial delay
     * @return the updated object
     */
    public Options setInitialDelayMs(long initialDelayMs) {
      Preconditions.checkArgument(initialDelayMs >= 0);
      mInitialDelayMs = initialDelayMs;
      return this;
    }

    /**
     * @param gcIntervalMs the gc interval
     * @return the updated object
     */
    public Options setGcIntervalMs(long gcIntervalMs) {
      Preconditions.checkArgument(gcIntervalMs > 0);
      mGcIntervalMs = gcIntervalMs;
      return this;
    }

    /**
     * @param gcExecutor the gc executor
     * @return updated object
     */
    public Options setGcExecutor(ScheduledExecutorService gcExecutor) {
      mGcExecutor = gcExecutor;
      return this;
    }

    private Options() {
    }

    /**
     * @return the default option
     */
    public static Options defaultOptions() {
      return new Options();
    }
  }

  private final ReentrantLock mLock = new ReentrantLock();
  private final Condition mNotEmpty = mLock.newCondition();
  /**
   * The max capacity.
   */
  private final int mMaxCapacity;
  /**
   * The min capacity.
   */
  private final int mMinCapacity;
  protected final SelectionPolicy mSelectionPolicy;
  @GuardedBy("mLock")
  private final Deque<ResourceInternal<T>> mAvailableResources;
  protected final ConcurrentHashMap<T, ResourceInternal<T>> mResources;
  private final Counter mMetricCounter;
  private ScheduledExecutorService mExecutor;
  private ScheduledFuture<?> mGcFuture;
  protected Clock mClock = new SystemClock();

  public DynamicResourcePool(Options options) {
    mResources = new ConcurrentHashMap<>();
    mExecutor = Preconditions.checkNotNull(options.getGcExecutor(), "executor");
    mMetricCounter = Preconditions.checkNotNull(getMetricCounter(), "cannot find resource count metric for %s", getClass().getName());
    mMaxCapacity = options.getMaxCapacity();
    mMinCapacity = options.getMinCapacity();
    mSelectionPolicy = options.getSelectionPolicy();
    mAvailableResources = new ArrayDeque<>();
    mGcFuture = mExecutor.scheduleAtFixedRate(() -> {
      List<T> resourcesToGc = new ArrayList<>();

      try (LockResource lockResource = new LockResource(mLock)) {
        //资源总量小于mMinCapacity， 此时不用执行回收动作
        if (mResources.size() <= mMinCapacity) {
          return;
        }
        int currentSize = mResources.size();
        Iterator<ResourceInternal<T>> iterator = mAvailableResources.iterator();
        while (iterator.hasNext()) {
          ResourceInternal<T> next = iterator.next();
          if (shouldGc(next)) {
            resourcesToGc.add(next.mResource);
            iterator.remove();
            mResources.remove(next.mResource);
            mMetricCounter.dec();
            currentSize--;
            if (currentSize <= mMinCapacity) {
              break;
            }
          }
        }
      }

      for (T resource : resourcesToGc) {
        LOG.debug("Resource {} is garbage collected.", resource);
        try {
          closeResource(resource);
        } catch (IOException e) {
          LOG.warn("Failed to close resource {}.", resource, e);
        }
      }
    }, options.getInitialDelayMs(), options.getGcIntervalMs(), TimeUnit.MILLISECONDS);
  }

  @Override
  public T acquire() throws IOException {
    try {
      return acquire(100  /* no timeout */, TimeUnit.DAYS);
    } catch (TimeoutException e) {
      // Never should timeout in acquire().
      throw new RuntimeException(e);
    }
  }

  public T acquire(long time, TimeUnit unit) throws TimeoutException, IOException {
    long endTimeMs = mClock.millis() + unit.toMillis(time);
    ResourceInternal<T> resource = poll();
    if (resource != null) {
      return checkHealthyAndRetry(resource.mResource, endTimeMs);
    }

    if (!isFull()) {
      // If the resource pool is empty but capacity is not yet full, create a new resource.
      T newResource = createNewResource();
      ResourceInternal<T> resourceInternal = new ResourceInternal<>(newResource);
      if (add(resourceInternal)) {
        return newResource;
      } else {
        closeResource(newResource);
      }
    }

    // Otherwise, try to take a resource from the pool, blocking if none are available.
    try (LockResource lockResource = new LockResource(mLock)) {
      while (true) {
        resource = poll();
        if (resource != null) {
          break;
        }
        long currTimeMs = mClock.millis();
        try {
          // one should use t1-t0<0, not t1<t0, because of the possibility of numerical overflow.
          // For further detail see: https://docs.oracle.com/javase/8/docs/api/java/lang/System.html
          if (endTimeMs - currTimeMs <= 0 || !mNotEmpty
              .await(endTimeMs - currTimeMs, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Acquire resource times out.");
          }
        } catch (InterruptedException e) {
          // TODO(calvin): Propagate the interrupted exception instead of converting to IOException
          Thread.currentThread().interrupt();
          throw new IOException("Thread interrupted while acquiring client from pool: " + this);
        }
      }
    }
    return checkHealthyAndRetry(resource.mResource, endTimeMs);
  }

  @Override
  public void release(T resource) {
    // We don't need to acquire mLock here because the resource is guaranteed not to be removed
    // if it is not available (i.e. not in mAvailableResources list).
    if (!mResources.containsKey(resource)) {
      throw new IllegalArgumentException(
          "Resource " + resource.toString() + " was not acquired from this resource pool.");
    }
    ResourceInternal<T> resourceInternal = mResources.get(resource);
    resourceInternal.setLastAccessTimeMs(mClock.millis());
    try {
      mLock.lock();
      mAvailableResources.addFirst(resourceInternal);
      mNotEmpty.signal();
    } finally {
      mLock.unlock();
    }
  }

  @Override
  public void close() throws IOException {
    try {
      mLock.lock();
      if (mAvailableResources.size() != mResources.size()) {
        LOG.warn("{} resources are not released when closing the resource pool.",
            mResources.size() - mAvailableResources.size());
      }
      for (ResourceInternal<T> resourceInternal : mAvailableResources) {
        closeResource(resourceInternal.mResource);
      }
      mAvailableResources.clear();
    } finally {
      mLock.unlock();
    }
    mGcFuture.cancel(true);
  }

  private ResourceInternal<T> poll() {
    try (LockResource lockResource = new LockResource(mLock)) {
      switch (mSelectionPolicy) {
        case FIFO:
          return mAvailableResources.pollLast();
        case LIFO:
          return mAvailableResources.pollFirst();
        default:
          throw new UnsupportedOperationException(
              "Policy " + mSelectionPolicy + " is not supported!");
      }
    }
  }

  /**
   * Checks whether the resource is healthy. If not retry. When this called, the resource
   * is not in mAvailableResources.
   *
   * @param resource  the resource to check
   * @param endTimeMs the end time to wait till
   * @return the resource
   * @throws TimeoutException if it times out to wait for a resource
   */
  private T checkHealthyAndRetry(T resource, long endTimeMs) throws TimeoutException, IOException {
    if (isHealthy(resource)) {
      return resource;
    } else {
      LOG.debug("Clearing unhealthy resource {}.", resource);
      remove(resource);
      closeResource(resource);
      return acquire(endTimeMs - mClock.millis(), TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Removes an existing resource from the pool.
   *
   * @param resource
   */
  private void remove(T resource) {
    try {
      mLock.lock();
      mResources.remove(resource);
      mMetricCounter.dec();
    } finally {
      mLock.unlock();
    }
  }

  private boolean isFull() {
    return mResources.size() >= mMaxCapacity;
  }

  private boolean add(ResourceInternal<T> resource) {
    try (LockResource lockResource = new LockResource(mLock)) {
      if (mResources.size() >= mMaxCapacity) {
        return false;
      } else {
        mResources.put(resource.mResource, resource);
        mMetricCounter.inc();
        return true;
      }
    }
  }

  protected abstract Counter getMetricCounter();

  protected abstract boolean shouldGc(ResourceInternal<T> resourceInternal);

  protected abstract void closeResource(T resource) throws IOException;

  protected abstract T createNewResource() throws IOException;

  protected abstract boolean isHealthy(T resource);
}

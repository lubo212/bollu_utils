package com.bollu.goosefs.resource.resourcepool;

import com.bollu.goosefs.resource.LockResource;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class representing a pool of resources to be temporarily used and returned. Inheriting classes
 * must implement the close method as well as initialize the resources in the constructor. The
 * implemented methods are thread-safe and inheriting classes should also written in a thread-safe
 * manner. See {@code FileSystemMasterClientPool} as an example.
 *
 * @param <T> the type of resource this pool manages
 */
@ThreadSafe
public abstract class ResourcePool<T> implements Pool<T> {
  private static final long WAIT_INDEFINITELY = -1;
  private final ReentrantLock mTakeLock;
  private final Condition mNotEmpty;
  protected final int mMaxCapacity;
  protected final ConcurrentLinkedQueue<T> mResources;
  protected final AtomicInteger mCurrentCapacity;

  public ResourcePool(int maxCapacity) {
    this(maxCapacity, new ConcurrentLinkedQueue<T>());
  }

  protected ResourcePool(int maxCapacity, ConcurrentLinkedQueue<T> resources) {
    mTakeLock = new ReentrantLock();
    mNotEmpty = mTakeLock.newCondition();
    mMaxCapacity = maxCapacity;
    mCurrentCapacity = new AtomicInteger();
    mResources = resources;
  }

  @Override
  public T acquire() {
    return acquire(WAIT_INDEFINITELY, null);
  }

  @Override
  @Nullable
  public T acquire(long time, TimeUnit unit) {
    Preconditions.checkState((time <= 0) == (unit == null));
    long endTimeMs = 0;
    if (time > 0) {
      endTimeMs = System.currentTimeMillis() + unit.toMillis(time);
    }

    T resource = mResources.poll();
    if (resource != null) {
      return resource;
    }

    if (mCurrentCapacity.get() < mMaxCapacity) {
      // If the resource pool is empty but capacity is not yet full, create a new resource.
      T newResource = createNewResource();
      mCurrentCapacity.incrementAndGet();
      return newResource;
    }

    mCurrentCapacity.decrementAndGet();
    try {
      //这把锁是为了公平竞争，避免前面的线程饿死
      mTakeLock.lockInterruptibly();
      try {
        while (true) {
          resource = mResources.poll();
          if (resource != null) {
            return resource;
          }
          if (time > 0) {
            long currTimeMs = System.currentTimeMillis();
            // one should use t1-t0<0, not t1<t0, because of the possibility of numerical overflow.
            // For further detail see: https://docs.oracle.com/javase/8/docs/api/java/lang/System.html
            if (endTimeMs - currTimeMs <= 0) {
              return null;
            }
            //mNotEmpty会被唤醒
            if (!mNotEmpty.await(endTimeMs - currTimeMs, TimeUnit.MILLISECONDS)) {
              return null;
            }
          } else {
            mNotEmpty.await();
          }
        }
      } finally {
        mTakeLock.unlock();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Override
  public void release(T resource) {
    if (resource != null) {
      mResources.add(resource);
      try (LockResource r = new LockResource(mTakeLock)) {
        mNotEmpty.signal();
      }
    }
  }

  @Override
  public int size() {
    return mCurrentCapacity.get();
  }

  public abstract void close() throws IOException;

  public abstract T createNewResource();
}

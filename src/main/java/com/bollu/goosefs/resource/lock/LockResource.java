package com.bollu.goosefs.resource.lock;

import com.bollu.goosefs.resource.CustomResourceLeakDetectorFactory;
import com.google.common.annotations.VisibleForTesting;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class LockResource implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(LockResource.class);

  private static final ResourceLeakDetector<LockResource> DETECTOR =
      CustomResourceLeakDetectorFactory.instance().newResourceLeakDetector(LockResource.class);

  protected Lock mLock;
  private final Runnable mCloseAction;
  @Nullable
  private ResourceLeakTracker<LockResource> mTracker = DETECTOR.track(this);

  public LockResource(Lock lock) {
    this(lock, true, false);
  }

  public LockResource(Lock lock, boolean acquireLock, boolean useTryLock) {
    this(lock, acquireLock, useTryLock, null);
  }

  public LockResource(Lock lock, boolean acquireLock, boolean useTryLock,
                      @Nullable Runnable closeAction) {
    mLock = lock;
    mCloseAction = closeAction;
    if (acquireLock) {
      if (useTryLock) {
        while (!mLock.tryLock()) { // returns immediately
          // The reason we don't use #tryLock(int, TimeUnit) here is because we found there is a bug
          // somewhere in the internal accounting of the ReentrantRWLock that, even though all
          // threads had released the lock, that a final thread would never be able to acquire it.
          LockSupport.parkNanos(10000);
        }
      } else {
        mLock.lock();
      }
    }
  }

  @VisibleForTesting
  public boolean hasSameLock(LockResource other) {
    return mLock == other.mLock;
  }

  @Override
  public void close() {
    if (mCloseAction != null) {
      mCloseAction.run();
    }
    if (mTracker != null) {
      mTracker.close(this);
    }
    mLock.unlock();
  }
}

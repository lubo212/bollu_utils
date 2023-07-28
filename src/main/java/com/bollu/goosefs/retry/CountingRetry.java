package com.bollu.goosefs.retry;

import com.bollu.goosefs.time.ThreadSleeper;
import com.google.common.base.Preconditions;

public abstract class CountingRetry implements RetryPolicy {

  private final int mMaxRetries;
  private int mAttemptCount = 0;

  protected CountingRetry(int maxRetries) {
    Preconditions.checkArgument(maxRetries > 0, "Max retries must be a positive number");
    mMaxRetries = maxRetries;
  }

  @Override
  public int getAttemptCount() {
    return mAttemptCount;
  }

  @Override
  public boolean attempt() {
    if (mAttemptCount <= mMaxRetries) {
      if (mAttemptCount == 0) {
        // first attempt, do not sleep
        mAttemptCount++;
        return true;
      }
      try {
        ThreadSleeper.INSTANCE.sleep(getSleepTime());
        mAttemptCount++;
        return true;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  public void reset() {
    mAttemptCount = 0;
  }

  protected abstract long getSleepTime();
}

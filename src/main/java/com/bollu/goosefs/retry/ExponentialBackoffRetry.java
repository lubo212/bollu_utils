package com.bollu.goosefs.retry;

import com.google.common.base.Preconditions;

import java.util.concurrent.ThreadLocalRandom;

public class ExponentialBackoffRetry extends CountingRetry{

  private final int mBaseSleepTimeMs;
  private final int mMaxSleepMs;

  public ExponentialBackoffRetry(int baseSleepTimeMs, int maxSleepMs, int maxRetries) {
    super(maxRetries);
    Preconditions.checkArgument(baseSleepTimeMs >= 0, "Base must be a positive number, or 0");
    Preconditions.checkArgument(maxSleepMs >= 0, "Max must be a positive number, or 0");

    mBaseSleepTimeMs = baseSleepTimeMs;
    mMaxSleepMs = maxSleepMs;
  }

  @Override
  protected long getSleepTime() {
    int count = getAttemptCount();
    if (count >= 30) {
      // current logic overflows at 30, so set value to max
      return mMaxSleepMs;
    } else {
      // use randomness to avoid contention between many operations using the same retry policy
      int sleepMs =
          mBaseSleepTimeMs * (ThreadLocalRandom.current().nextInt(1 << count, 1 << (count + 1)));
      return Math.min(abs(sleepMs, mMaxSleepMs), mMaxSleepMs);
    }
  }

  private static int abs(int value, int defaultValue) {
    int result = Math.abs(value);
    if (result == Integer.MIN_VALUE) {
      result = defaultValue;
    }
    return result;
  }
}

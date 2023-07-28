package com.bollu.goosefs.time;

public class ExponentialTimer {

  public enum Result {
    EXPIRED,
    NOT_READY,
    READY,
  }

  private final long mMaxIntervalMs;
  private final long mLastEventHorizonMs;
  private long mNumEvents;
  private long mNextEventMs;
  private long mIntervalMs;

  public ExponentialTimer(long initialIntervalMs, long maxIntervalMs, long initialWaitTimeMs,
                          long maxTotalWaitTimeMs) {
    mMaxIntervalMs = maxIntervalMs;
    mLastEventHorizonMs = System.currentTimeMillis() + maxTotalWaitTimeMs;
    mNextEventMs = System.currentTimeMillis() + initialWaitTimeMs;
    mIntervalMs = Math.min(initialIntervalMs, maxIntervalMs);
    mNumEvents = 0;
  }

  public long getNumEvents() {
    return mNumEvents;
  }

  public Result tick() {
    if (System.currentTimeMillis() >= mLastEventHorizonMs) {
      return Result.EXPIRED;
    }
    if (System.currentTimeMillis() < mNextEventMs) {
      return Result.NOT_READY;
    }
    mNextEventMs = System.currentTimeMillis() + mIntervalMs;
    long next = Math.min(mIntervalMs * 2, mMaxIntervalMs);
    if (next < mIntervalMs) {
      next = Integer.MAX_VALUE;
    }
    mIntervalMs = next;
    mNumEvents++;
    return Result.READY;
  }

}

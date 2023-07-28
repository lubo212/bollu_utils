package com.bollu.goosefs.retry;

import com.bollu.goosefs.time.Sleeper;
import com.bollu.goosefs.time.ThreadSleeper;
import com.bollu.goosefs.time.TimeContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public abstract class TimeBoundedRetry implements RetryPolicy {
  private final Clock mClock;
  private final Duration mMaxDuration;
  private final Instant mStartTime;
  private final Instant mEndTime;
  private int mAttemptCount = 0;

  public TimeBoundedRetry(TimeContext timeCtx, Duration maxDuration) {
    mClock = timeCtx.getClock();
    mMaxDuration = maxDuration;
    mStartTime = mClock.instant();
    mEndTime = mStartTime.plus(mMaxDuration);
  }

  @Override
  public int getAttemptCount() {
    return mAttemptCount;
  }

  @Override
  public boolean attempt() {
    if (mAttemptCount == 0) {
      mAttemptCount++;
      return true;
    }
    Instant now = mClock.instant();
    if (now.isAfter(mEndTime) || now.equals(mEndTime)) {
      return false;
    }
    Duration nextWaitTime = computeNextWaitTime();
    if (now.plus(nextWaitTime).isAfter(mEndTime)) {
      nextWaitTime = Duration.between(now, mEndTime);
    }
    if (nextWaitTime.getNano() > 0) {
      try {
        ThreadSleeper.INSTANCE.sleep(nextWaitTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    mAttemptCount++;
    return true;
  }

  protected abstract Duration computeNextWaitTime();

}

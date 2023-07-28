package com.bollu.goosefs.retry;

import com.bollu.goosefs.time.TimeContext;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class ExponentialTimeBoundedRetry extends TimeBoundedRetry{

  private final Duration mMaxSleep;
  private Duration mNextSleep;
  boolean mSkipInitialSleep;

  private ExponentialTimeBoundedRetry(TimeContext timeCtx, Duration maxDuration,
                                      Duration initialSleep, Duration maxSleep, boolean skipInitialSleep) {
    super(timeCtx, maxDuration);
    mMaxSleep = maxSleep;
    mNextSleep = initialSleep.compareTo(Duration.ofMillis(1L)) < 0 ? Duration.ofMillis(1L): initialSleep;
    mSkipInitialSleep = skipInitialSleep;
  }
  @Override
  protected Duration computeNextWaitTime() {
    if (mSkipInitialSleep && getAttemptCount()==0) {
      return Duration.ofNanos(0);
    }
    Duration next = mNextSleep;
    mNextSleep = mNextSleep.multipliedBy(2);
    if (mNextSleep.compareTo(mMaxSleep) > 0) {
      mNextSleep = mMaxSleep;
    }
    long jitter = Math.round(ThreadLocalRandom.current().nextDouble(0.1) * next.toMillis());
    return next.plusMillis(jitter);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private TimeContext mTimeCtx = TimeContext.SYSTEM;
    private Duration mMaxDuration;
    private Duration mInitialSleep;
    private Duration mMaxSleep;
    private boolean mSkipInitialSleep = false;

    /**
     * @param timeCtx time context
     * @return the builder
     */
    public Builder withTimeCtx(TimeContext timeCtx) {
      mTimeCtx = timeCtx;
      return this;
    }

    /**
     * @param maxDuration max total duration to retry for
     * @return the builder
     */
    public Builder withMaxDuration(Duration maxDuration) {
      mMaxDuration = maxDuration;
      return this;
    }

    /**
     * @param initialSleep initial sleep interval between retries
     * @return the builder
     */
    public Builder withInitialSleep(Duration initialSleep) {
      mInitialSleep = initialSleep;
      return this;
    }

    /**
     * @param maxSleep maximum sleep interval between retries
     * @return the builder
     */
    public Builder withMaxSleep(Duration maxSleep) {
      mMaxSleep = maxSleep;
      return this;
    }

    /**
     * first sleep will be skipped.
     *
     * @return the builder
     */
    public Builder withSkipInitialSleep() {
      mSkipInitialSleep = true;
      return this;
    }

    /**
     * @return the built retry mechanism
     */
    public ExponentialTimeBoundedRetry build() {
      return new ExponentialTimeBoundedRetry(
          mTimeCtx, mMaxDuration, mInitialSleep, mMaxSleep, mSkipInitialSleep);
    }
  }
}

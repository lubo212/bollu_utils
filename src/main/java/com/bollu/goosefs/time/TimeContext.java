package com.bollu.goosefs.time;

import java.time.Clock;

public class TimeContext {

  public static final TimeContext SYSTEM =
      new TimeContext(Clock.systemUTC(), ThreadSleeper.INSTANCE);

  private final Clock mClock;
  private final Sleeper mSleeper;

  public TimeContext(Clock clock, Sleeper sleeper) {
    mClock = clock;
    mSleeper = sleeper;
  }
  public Clock getClock() {
    return mClock;
  }

  public Sleeper getSleeper() {
    return mSleeper;
  }
}

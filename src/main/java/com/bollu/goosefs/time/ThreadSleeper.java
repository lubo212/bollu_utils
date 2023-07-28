package com.bollu.goosefs.time;

import java.time.Duration;

public class ThreadSleeper implements Sleeper {
  public static final ThreadSleeper INSTANCE = new ThreadSleeper();

  private ThreadSleeper() {
  }

  @Override
  public void sleep(Duration duration) throws InterruptedException {
    Thread.sleep(duration.toMillis());
  }

  @Override
  public void sleep(long duration) throws InterruptedException {
    Thread.sleep(duration);
  }
}

package com.bollu.goosefs.time;

import java.time.Duration;

public interface Sleeper {

  void sleep(Duration duration) throws InterruptedException;

  void sleep(long duration) throws InterruptedException;
}

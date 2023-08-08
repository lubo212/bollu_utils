package com.bollu.goosefs.common.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ThreadFactory;

public class ThreadFactoryUtils {
  private ThreadFactoryUtils() {}

  /**
   * Creates a {@link java.util.concurrent.ThreadFactory} that spawns off threads.
   *
   * @param nameFormat name pattern for each thread. should contain '%d' to distinguish between
   *                   threads.
   * @param isDaemon if true, the {@link java.util.concurrent.ThreadFactory} will create
   *                 daemon threads.
   * @return the created factory
   */
  public static ThreadFactory build(final String nameFormat, boolean isDaemon) {
    return new ThreadFactoryBuilder().setDaemon(isDaemon).setNameFormat(nameFormat).build();
  }
}

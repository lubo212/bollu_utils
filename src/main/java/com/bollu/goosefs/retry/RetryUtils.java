package com.bollu.goosefs.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

public class RetryUtils {
  private static final Logger LOG = LoggerFactory.getLogger(RetryUtils.class);

  public static void retry(String action, RunnableThrowsIOException f, RetryPolicy policy)
      throws IOException {
    IOException e = null;
    while (policy.attempt()) {
      try {
        f.run();
        return;
      } catch (IOException ioe) {
        e = ioe;
        LOG.warn("Failed to {} (attempt {}): {}", action, policy.getAttemptCount(), e.toString());
      }
    }
    throw e;
  }

  public static RetryPolicy noRetryPolicy() {
    return new InstantCountingRetry(0);
  }

  public static RetryPolicy defaultRetry(Duration maxRetryDuration, Duration baseSleepMs,
                                               Duration maxSleepMs) {
    return ExponentialTimeBoundedRetry.builder()
        .withMaxDuration(maxRetryDuration)
        .withInitialSleep(baseSleepMs)
        .withMaxSleep(maxSleepMs)
        .build();
  }


  @FunctionalInterface
  public interface RunnableThrowsIOException {
    /**
     * Runs the runnable.
     */
    void run() throws IOException;
  }

  private RetryUtils() {} // prevent instantiation
}

package com.bollu.goosefs.retry;

public class InstantCountingRetry extends CountingRetry {
  protected InstantCountingRetry(int maxRetries) {
    super(maxRetries);
  }

  @Override
  protected long getSleepTime() {
    return 0;
  }
}

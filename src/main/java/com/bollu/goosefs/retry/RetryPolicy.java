package com.bollu.goosefs.retry;

public interface RetryPolicy {

  int getAttemptCount();
  boolean attempt();
}

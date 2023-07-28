package com.bollu.goosefs.prometheus.metrics.sink;

public interface Sink {

  /**
   * Starts the reporter polling.
   */
  void start();

  /**
   * Stops the reporter.
   */
  void stop();

  /**
   * Reports the current values of all metrics.
   */
  void report();

}

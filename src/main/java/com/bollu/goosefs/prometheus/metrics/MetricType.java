package com.bollu.goosefs.prometheus.metrics;

public enum MetricType {
  GAUGE(0),
  COUNTER(1),
  METER(2),
  TIMER(3);

  private final Integer value;

  MetricType(Integer value) {
    this.value = value;
  }

  public Integer getValue() {
    return value;
  }
}

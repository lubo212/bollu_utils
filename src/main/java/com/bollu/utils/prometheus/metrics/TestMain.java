package com.bollu.utils.prometheus.metrics;

import com.codahale.metrics.Counter;

import java.util.Map;
import java.util.Set;

public class TestMain {

  public static final class Metrics {
    private static final Counter DIRECTORIES_CREATED
        = MetricsSystem.counter(MetricKey.MASTER_DIRECTORIES_CREATED.getName());

    static {
      MetricsSystem.registerGaugeIfAbsent(MetricKey.MASTER_LOST_BLOCK_COUNT.getName(),
          () -> 0);
    }
  }

  public static void main(String[] args) {
    Metrics.DIRECTORIES_CREATED.inc(10);

    Map<String, MetricValue> stringMetricValueMap = MetricsSystem.allMetrics();
    Set<Map.Entry<String, MetricValue>> entries = stringMetricValueMap.entrySet();
    for (Map.Entry<String, MetricValue> entry : entries) {
      System.out.println(entry.getKey() + " : " + entry.getValue().getDoubleValue());
    }
  }

}

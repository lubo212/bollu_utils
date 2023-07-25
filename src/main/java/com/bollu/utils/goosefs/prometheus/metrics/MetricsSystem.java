package com.bollu.utils.goosefs.prometheus.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ThreadSafe
public class MetricsSystem {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsSystem.class);
  private static final ConcurrentHashMap<String, String> CACHED_METRICS = new ConcurrentHashMap<>();

  //ms
  private static int sResolveTimeout = 5000;
  private static final ConcurrentHashMap<String, String> CACHED_ESCAPED_PATH = new ConcurrentHashMap<>();
  private static final Map<String, Long> LAST_REPORTED_METRICS = new HashMap<>();
  private static final Map<String, MetricType> SHOULD_REPORT_METRICS = new ConcurrentHashMap<>();
  // A pattern to get the <instance_type>.<metric_name> from the full metric name
  private static final Pattern METRIC_NAME_PATTERN = Pattern.compile("^(.*?[.].*?)[.].*");
  private static boolean sReported = false;

  private static Supplier<String> sSourceNameSupplier = () -> constructSourceName();


  public static final MetricRegistry METRIC_REGISTRY;


  public enum InstanceType {
    JOB_MASTER("JobMaster"),
    JOB_WORKER("JobWorker"),
    MASTER("Master"),
    WORKER("Worker"),
    CLUSTER("Cluster"),
    CLIENT("Client"),
    PROXY("Proxy");

    private String mValue;

    /**
     * Creates the instance type with value.
     *
     * @param value value of the instance type
     */
    InstanceType(String value) {
      mValue = value;
    }

    @Override
    public String toString() {
      return mValue;
    }

    /**
     * Creates an instance type from the string. This method is case insensitive.
     *
     * @param text the instance type in string
     * @return the created instance
     */
    public static InstanceType fromString(String text) {
      for (InstanceType type : InstanceType.values()) {
        if (type.toString().equalsIgnoreCase(text)) {
          return type;
        }
      }
      throw new IllegalArgumentException("No constant with text " + text + " found");
    }
  }

  static {
    METRIC_REGISTRY = new MetricRegistry();
    METRIC_REGISTRY.registerAll(new JvmAttributeGaugeSet());
    METRIC_REGISTRY.registerAll(new GarbageCollectorMetricSet());
    METRIC_REGISTRY.registerAll(new MemoryUsageGaugeSet());
  }

  //命名的二次填充
  public static String getMetricName(String name) {
    return name;
  }

  public static String escape(ConcurrentHashMap<String, String> CACHED_ESCAPED_PATH, String uri) {
    return CACHED_ESCAPED_PATH.computeIfAbsent(uri,
        u -> u.toString().replace("%", "%25")
            .replace("/", "%2F").replace(".", "%2E"));
  }

  public static String unescape(String uri) {
    return uri.replace("%2F", "/").replace("%2E", ".").replace("%25", "%");
  }

  /**
   * Get or add counter with the given name.
   * The counter stores in the metrics system is never removed but may reset to zero.
   *
   * @param name the name of the metric
   * @return a counter object with the qualified metric name
   */
  public static Counter counter(String name) {
    return METRIC_REGISTRY.counter(getMetricName(name));
  }

  /**
   * Get or add counter with the given name with tags.
   * The counter stores in the metrics system is never removed but may reset to zero.
   * If this metric can be aggregated at cluster level and should report to leading master,
   * add it to the should report metrics map.
   * <p>
   * This method is added to add worker metrics with ufs tags into the should report metrics map.
   *
   * @param name         the metric name
   * @param shouldReport whether this metric should be reported
   * @param tags         the tag name and tag value pairs
   * @return a counter object with the qualified metric name
   */
  public static Counter counterWithTags(String name, boolean shouldReport, String... tags) {
    String fullName = getMetricName(Metric.getMetricNameWithTags(name, tags));
    if (shouldReport) {
      SHOULD_REPORT_METRICS.putIfAbsent(fullName, MetricType.COUNTER);
    }
    return METRIC_REGISTRY.counter(fullName);
  }


  /**
   * Get or add meter with the given name.
   * The returned meter may be changed due to
   *
   * @param name the name of the metric
   * @return a meter object with the qualified metric name
   */
  public static Meter meter(String name) {
    return METRIC_REGISTRY.meter(getMetricName(name));
  }

  /**
   * Get or add meter with the given name.
   * The returned meter may be changed due to
   * If this metric can be aggregated at cluster level and should report to leading master,
   * add it to the should report metrics map.
   * <p>
   * This method is added to add worker metrics with ufs tags into the should report metrics map.
   *
   * @param name         the name of the metric
   * @param shouldReport whether this metric should be reported
   * @param tags         the tag name and tag value pairs
   * @return a meter object with the qualified metric name
   */
  public static Meter meterWithTags(String name, boolean shouldReport, String... tags) {
    String fullName = getMetricName(Metric.getMetricNameWithTags(name, tags));
    if (shouldReport) {
      SHOULD_REPORT_METRICS.putIfAbsent(fullName, MetricType.METER);
    }
    return METRIC_REGISTRY.meter(fullName);
  }

  /**
   * Get or add timer with the given name.
   * The returned timer may be changed due to
   *
   * @param name the name of the metric
   * @return a timer object with the qualified metric name
   */
  public static Timer timer(String name) {
    return METRIC_REGISTRY.timer(getMetricName(name));
  }

  /**
   * Registers a gauge if it has not been registered.
   *
   * @param name   the gauge name
   * @param metric the gauge
   * @param <T>    the type
   */
  public static synchronized <T> void registerGaugeIfAbsent(String name, Gauge<T> metric) {
    if (!METRIC_REGISTRY.getMetrics().containsKey(name)) {
      METRIC_REGISTRY.register(name, metric);
    }
  }

  /**
   * Registers a cached gauge if it has not been registered.
   *
   * @param name   the gauge name
   * @param metric the gauge
   * @param <T>    the type
   */
  public static synchronized <T> void registerCachedGaugeIfAbsent(String name, Gauge<T> metric) {
    if (!METRIC_REGISTRY.getMetrics().containsKey(name)) {
      METRIC_REGISTRY.register(name, new CachedGauge<T>(10, TimeUnit.MINUTES) {
        @Override
        protected T loadValue() {
          return metric.getValue();
        }
      });
    }
  }

  /**
   * Builds unique metric registry names with unique ID (set to host name). The pattern is
   * instance.metricName.hostname
   *
   * @param instance the instance name
   * @param name     the metric name
   * @return the metric registry name
   */
  private static String getMetricNameWithUniqueId(InstanceType instance, String name) {
    if (name.startsWith(instance.toString())) {
      return Joiner.on(".").join(name, sSourceNameSupplier.get());
    }
    return Joiner.on(".").join(instance, name, sSourceNameSupplier.get());
  }

  /**
   * Gets all the master metrics belongs to the given metric names.
   *
   * @param metricNames the name of the metrics to get
   * @return a metric map from metric name to metrics with this name
   */
  public static Map<String, Set<Metric>> getMasterMetrics(Set<String> metricNames) {
    Map<String, Set<Metric>> res = new HashMap<>();
    for (Map.Entry<String, com.codahale.metrics.Metric> entry
        : METRIC_REGISTRY.getMetrics().entrySet()) {
      Matcher matcher = METRIC_NAME_PATTERN.matcher(entry.getKey());
      if (matcher.matches()) {
        String name = matcher.group(1);
        if (metricNames.contains(name)) {
          res.computeIfAbsent(name, m -> new HashSet<>())
              .add(getGooseFSMetricFromCodahaleMetric(entry.getKey(), entry.getValue()));
        }
      }
    }
    return res;
  }

  /**
   * Gets metric with the given full metric name.
   *
   * @param fullName the full name of the metric to get
   * @return a metric set with the master metric of the given metric name
   */
  @Nullable
  public static Metric getMetricValue(String fullName) {
    Map<String, com.codahale.metrics.Metric> metricMap = METRIC_REGISTRY.getMetrics();
    com.codahale.metrics.Metric metric = metricMap.get(fullName);
    if (metric == null) {
      return null;
    }
    return getGooseFSMetricFromCodahaleMetric(fullName, metric);
  }

  @Nullable
  private static Metric getGooseFSMetricFromCodahaleMetric(String name,
                                                           com.codahale.metrics.Metric metric) {
    if (metric instanceof Gauge) {
      Gauge gauge = (Gauge) metric;
      return Metric.from(name, ((Number) gauge.getValue()).longValue(), MetricType.GAUGE);
    } else if (metric instanceof Counter) {
      Counter counter = (Counter) metric;
      return Metric.from(name, counter.getCount(), MetricType.COUNTER);
    } else if (metric instanceof Meter) {
      Meter meter = (Meter) metric;
      return Metric.from(name, meter.getOneMinuteRate(), MetricType.METER);
    } else if (metric instanceof Timer) {
      Timer timer = (Timer) metric;
      return Metric.from(name, timer.getCount(), MetricType.TIMER);
    }
    LOG.warn("Metric {} has invalid metric type {}", name, metric.getClass().getName());
    return null;
  }

  /**
   * @return a map of all metrics stored in the current node
   *         from metric name to {@link MetricValue}
   */
  public static Map<String, MetricValue> allMetrics() {
    Map<String, MetricValue> metricsMap = new HashMap<>();
    for (Map.Entry<String, com.codahale.metrics.Metric> entry
        : METRIC_REGISTRY.getMetrics().entrySet()) {
      MetricValue.Builder valueBuilder = MetricValue.newBuilder();
      com.codahale.metrics.Metric metric = entry.getValue();
      if (metric instanceof Gauge) {
        Object value = ((Gauge) metric).getValue();
        if (value instanceof Number) {
          valueBuilder.setDoubleValue(((Number) value).doubleValue());
        } else {
          valueBuilder.setStringValue(value.toString());
        }
        valueBuilder.setMetricType(MetricType.GAUGE);
      } else if (metric instanceof Counter) {
        valueBuilder.setMetricType(MetricType.COUNTER)
            .setDoubleValue((double) ((Counter) metric).getCount());
      } else if (metric instanceof Meter) {
        valueBuilder.setMetricType(MetricType.METER)
            .setDoubleValue(((Meter) metric).getOneMinuteRate());
      } else if (metric instanceof Timer) {
        valueBuilder.setMetricType(MetricType.TIMER)
            .setDoubleValue((double) ((Timer) metric).getCount());
      } else {
        LOG.warn("Metric {} has invalid metric type {}",
            entry.getKey(), metric.getClass().getName());
        continue;
      }
      metricsMap.put(entry.getKey(), valueBuilder.build());
    }
    return metricsMap;
  }

  /**
   * Resets all the metrics in the MetricsSystem.
   *
   * This method is not thread-safe and should be used sparingly.
   */
  public static synchronized void resetAllMetrics() {
    long startTime = System.currentTimeMillis();
    // Gauge metrics don't need to be changed because they calculate value when getting them
    // Counters can be reset to zero values.
    for (Counter counter : METRIC_REGISTRY.getCounters().values()) {
      counter.dec(counter.getCount());
    }

    // No reset logic exist in Meter, a remove and add combination is needed
    for (String meterName : METRIC_REGISTRY.getMeters().keySet()) {
      METRIC_REGISTRY.remove(meterName);
      METRIC_REGISTRY.meter(meterName);
    }

    // No reset logic exist in Timer, a remove and add combination is needed
    for (String timerName : METRIC_REGISTRY.getTimers().keySet()) {
      METRIC_REGISTRY.remove(timerName);
      METRIC_REGISTRY.timer(timerName);
    }
    LAST_REPORTED_METRICS.clear();
    LOG.info("Reset all metrics in the metrics system in {}ms",
        System.currentTimeMillis() - startTime);
  }

  /**
   * Resets the metric registry and removes all the metrics.
   */
  @VisibleForTesting
  public static void clearAllMetrics() {
    for (String name : METRIC_REGISTRY.getNames()) {
      METRIC_REGISTRY.remove(name);
    }
  }

  /**
   * Resets all counters to 0 and unregisters gauges for testing.
   */
  @VisibleForTesting
  public static void resetCountersAndGauges() {
    for (Map.Entry<String, Counter> entry : METRIC_REGISTRY.getCounters().entrySet()) {
      entry.getValue().dec(entry.getValue().getCount());
    }
    for (String gauge : METRIC_REGISTRY.getGauges().keySet()) {
      METRIC_REGISTRY.remove(gauge);
    }
  }

  private static String constructSourceName() {
    return "";
  }
}

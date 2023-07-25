package com.bollu.utils.goosefs.prometheus.metrics;

import com.bollu.utils.goosefs.prometheus.metrics.sink.MetricsConfig;
import com.bollu.utils.goosefs.prometheus.metrics.sink.Sink;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MetricsSinkSystem extends MetricsSystem {

  private static final Logger LOG = LoggerFactory.getLogger(MetricsSinkSystem.class);


  // A flag telling whether metrics have been reported yet.
  // Using this prevents us from initializing {@link #SHOULD_REPORT_METRICS} more than once
  private static boolean sReported = false;

  @GuardedBy("MetricsSystem")
  private static List<Sink> sSinks;

  public static final String SINK_REGEX = "^sink\\.(.+)\\.(.+)";

  private static final TimeUnit MINIMAL_POLL_UNIT = TimeUnit.SECONDS;
  private static final int MINIMAL_POLL_PERIOD = 1;

  /**
   * Starts sinks specified in the configuration.
   * This is an no-op if the sinks have already been started.
   * Note: This has to be called after GooseFS configuration is initialized.
   *
   * @param metricsConfFile the location of the metrics configuration file
   */
  public static void startSinks(String metricsConfFile) {
    synchronized (MetricsSystem.class) {
      if (sSinks != null) {
        LOG.debug("Sinks have already been started.");
        return;
      }
    }
    if (metricsConfFile.isEmpty()) {
      LOG.info("Metrics is not enabled.");
      return;
    }
    MetricsConfig config = new MetricsConfig(metricsConfFile);
    startSinksFromConfig(config);
  }

  /**
   * Starts sinks from a given metrics configuration. This is made public for unit test.
   *
   * @param config the metrics config
   */
  public static synchronized void startSinksFromConfig(MetricsConfig config) {
    if (sSinks != null) {
      LOG.debug("Sinks have already been started.");
      return;
    }
    LOG.info("Starting sinks with config: {}.", config);
    sSinks = new ArrayList<>();
    Map<String, Properties> sinkConfigs =
        MetricsConfig.subProperties(config.getProperties(), SINK_REGEX);
    for (Map.Entry<String, Properties> entry : sinkConfigs.entrySet()) {
      String classPath = entry.getValue().getProperty("class");
      if (classPath != null) {
        LOG.info("Starting sink {}.", classPath);
        try {
          Sink sink =
              (Sink) Class.forName(classPath).getConstructor(Properties.class, MetricRegistry.class)
                  .newInstance(entry.getValue(), METRIC_REGISTRY);
          sink.start();
          sSinks.add(sink);
        } catch (Exception e) {
          LOG.error("Sink class {} cannot be instantiated", classPath, e);
        }
      }
    }
  }

  /**
   * Stops all the sinks.
   */
  public static synchronized void stopSinks() {
    if (sSinks != null) {
      for (Sink sink : sSinks) {
        sink.stop();
      }
    }
    sSinks = null;
  }

  /**
   * @return the number of sinks started
   */
  public static synchronized int getNumSinks() {
    int sz = 0;
    if (sSinks != null) {
      sz = sSinks.size();
    }
    return sz;
  }



}

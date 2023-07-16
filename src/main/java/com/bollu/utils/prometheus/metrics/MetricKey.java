package com.bollu.utils.prometheus.metrics;

import com.bollu.utils.exception.ExceptionMessage;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricKey implements Comparable<MetricKey> {

  private static final Logger LOG = LoggerFactory.getLogger(MetricKey.class);

  /**
   * A map from default metric key's string name to the metric.
   * This map must be the first to initialize within this file.
   */
  private static final Map<String, MetricKey> METRIC_KEYS_MAP = new ConcurrentHashMap<>();

  private final String mName;
  private final String mDescription;
  private final MetricType mMetricType;

  /**
   * @param name name of this metric
   * @param description description of this metric
   * @param metricType the metric type of this metric
   */
  private MetricKey(String name, String description,
                    MetricType metricType) {
    mName = Preconditions.checkNotNull(name, "name");
    mDescription = Strings.isNullOrEmpty(description) ? "N/A" : description;
    mMetricType = metricType;
  }

  /**
   * @param name String of this property
   */
  private MetricKey(String name) {
    this(name, null, MetricType.GAUGE);
  }

  /**
   * @param name name of a metric
   * @return whether the given name is a valid Metric name
   */
  public static boolean isValid(String name) {
    return METRIC_KEYS_MAP.containsKey(name);
  }

  /**
   * Parses a given name and return its corresponding {@link MetricKey},
   * throwing exception if no such a Metric can be found.
   *
   * @param name name of the Metric key
   * @return corresponding Metric
   */
  public static MetricKey fromString(String name) {
    MetricKey key = METRIC_KEYS_MAP.get(name);
    if (key != null) {
      return key;
    }
    throw new IllegalArgumentException(ExceptionMessage.INVALID_METRIC_KEY.getMessage(name));
  }

  /**
   * @return all pre-defined GooseFS metric keys
   */
  public static Collection<? extends MetricKey> allMetricKeys() {
    return Sets.newHashSet(METRIC_KEYS_MAP.values());
  }

  public String getName() {
    return mName;
  }

  public String getMetricName() {
    String[] pieces = mName.split("\\.");
    if (pieces.length <= 1) {
      return mName;
    }
    return pieces[1];
  }

  public String getDescription() {
    return mDescription;
  }

  public MetricType getMetricType() {
    return mMetricType;
  }


//  public boolean isClusterAggregated() {
//    return mIsClusterAggregated;
//  }

  /**
   * Registers the given key to the global key map.
   *
   * @param key the Metric key
   * @return whether the Metric key is successfully registered
   */
  @VisibleForTesting
  public static boolean register(MetricKey key) {
    String name = key.getName();
    if (METRIC_KEYS_MAP.containsKey(name)) {
      return false;
    }

    METRIC_KEYS_MAP.put(name, key);
    return true;
  }

  /**
   * Unregisters the given key from the global key map.
   *
   * @param key the Metric to unregister
   */
  @VisibleForTesting
  public static void unregister(MetricKey key) {
    METRIC_KEYS_MAP.remove(key.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MetricKey)) {
      return false;
    }
    MetricKey that = (MetricKey) o;
    return Objects.equal(mName, that.mName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mName);
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public int compareTo(MetricKey o) {
    return mName.compareTo(o.mName);
  }

  /**
   * Builder to create {@link MetricKey} instances. Note that, <code>Builder.build()</code> will
   * throw exception if there is an existing Metric built with the same name.
   */
  public static final class Builder {
    private String mName;
    private String mDescription;
    private MetricType mMetricType = MetricType.GAUGE;

    /**
     * @param name name of the Metric
     */
    public Builder(String name) {
      mName = name;
    }

    /**
     * @param name name for the Metric
     * @return the updated builder instance
     */
    public MetricKey.Builder setName(String name) {
      mName = name;
      return this;
    }

    /**
     * @param description of the Metric
     * @return the updated builder instance
     */
    public MetricKey.Builder setDescription(String description) {
      mDescription = description;
      return this;
    }

    /**
     * @param metricType the metric type of this metric
     * @return the updated builder instance
     */
    public MetricKey.Builder setMetricType(MetricType metricType) {
      mMetricType = metricType;
      return this;
    }

    /**
     * Creates and registers the Metric key.
     *
     * @return the created Metric key instance
     */
    public MetricKey build() {
      MetricKey key = new MetricKey(mName, mDescription, mMetricType);
      Preconditions.checkState(MetricKey.register(key), "Cannot register existing metric \"%s\"",
          mName);
      return key;
    }
  }

  @ThreadSafe
  public static final class Name {
    public static final String MASTER_DIRECTORIES_CREATED = "Master.DirectoriesCreated";
    public static final String Master_LostBlockCount = "Master.LostBlockCount";

    private Name() {}
  }


  //eg
  public static final MetricKey MASTER_DIRECTORIES_CREATED =
      new Builder(Name.MASTER_DIRECTORIES_CREATED)
          .setDescription("Total number of the succeed CreateDirectory operations")
          .setMetricType(MetricType.COUNTER)
          .build();

  public static final MetricKey MASTER_LOST_BLOCK_COUNT =
      new Builder(Name.Master_LostBlockCount)
          .setDescription("Count of lost unique blocks")
          .setMetricType(MetricType.GAUGE)
          .build();

}

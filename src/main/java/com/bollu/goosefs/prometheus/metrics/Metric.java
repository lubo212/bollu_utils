package com.bollu.goosefs.prometheus.metrics;

import com.bollu.goosefs.prometheus.metrics.util.CommonUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AtomicDouble;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Metric {

  private static final long serialVersionUID = -2236393414222298333L;

  public static final String TAG_SEPARATOR = ":";

  public static final String TAG_USER = "User";

  private static final ConcurrentHashMap<UserMetricKey, String> CACHED_METRICS = new ConcurrentHashMap<>();

  private final MetricsSystem.InstanceType mInstanceType;
  private final String mSource;
  private final String mName;
  private final MetricType mMetricType;
  private final Map<String, String> mTags;

  /**
   * The unique identifier to represent this metric.
   * The pattern is instance.name[.tagName:tagValue]*[.source].
   * Fetched once and assumed to be immutable.
   */
  private final Supplier<String> mFullMetricNameSupplier =
      CommonUtils.memoize(this::constructFullMetricName);

  private AtomicDouble mValue;

  /**
   * Constructs a {@link Metric} instance.
   *
   * @param instanceType the instance type
   * @param source the metric source
   * @param metricType the type of the metric
   * @param name the metric name
   * @param value the value
   */
  public Metric(MetricsSystem.InstanceType instanceType, String source,
                MetricType metricType, String name, Double value) {
    Preconditions.checkNotNull(name, "name");
    mInstanceType = instanceType;
    mSource = source;
    mMetricType = metricType;
    mName = name;
    mValue = new AtomicDouble(value);
    mTags = new LinkedHashMap<>();
  }

  /**
   * Add metric value delta to the existing value.
   *
   * @param delta value to add
   */
  public void addValue(double delta) {
    mValue.addAndGet(delta);
  }

  public void setValue(double value) {
    mValue.set(value);
  }

  public double getValue() {
    return mValue.get();
  }

  public MetricsSystem.InstanceType getInstanceType() {
    return mInstanceType;
  }

  public void addTag(String name, String value) {
    mTags.put(name, value);
  }

  public String getSource() {
    return mSource;
  }

  public MetricType getMetricType() {
    return mMetricType;
  }

  public String getName() {
    return mName;
  }

  public Map<String, String> getTags() {
    return mTags;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!(other instanceof Metric)) {
      return false;
    }
    Metric metric = (Metric) other;
    return Objects.equal(getFullMetricName(), metric.getFullMetricName())
        && Objects.equal(mValue.get(), metric.mValue.get());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getFullMetricName(), mValue.get());
  }

  public String getFullMetricName() {
    return mFullMetricNameSupplier.get();
  }

  /**
   * Gets the metric name with the appendix of tags. The returned name is of the pattern
   * name[.tagName:tagValue]*.
   *
   * @param name the metric name
   * @param tags the tag name and tag value pairs
   * @return the name with the tags appended
   */
  public static String getMetricNameWithTags(String name, String... tags) {
    Preconditions.checkArgument(tags.length % 2 == 0,
        "The tag arguments should be tag name and tag value pairs");
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    for (int i = 0; i < tags.length; i += 2) {
      sb.append('.').append(tags[i]).append(TAG_SEPARATOR).append(tags[i + 1]);
    }
    return sb.toString();
  }

  /**
   * Gets a metric name with a specific user tag.
   *
   * @param metricName the name of the metric
   * @param userName the user
   * @return a metric name with the user tagged
   */
  public static String getMetricNameWithUserTag(String metricName, String userName) {
    UserMetricKey k = new UserMetricKey(metricName, userName);
    String result = CACHED_METRICS.get(k);
    if (result != null) {
      return result;
    }
    return CACHED_METRICS.computeIfAbsent(k, key -> metricName + "." + TAG_USER
        + TAG_SEPARATOR + userName);
  }

  public static MetricsSystem.InstanceType getMetricInstanceType(String fullName) {
    if(fullName == null) {
      return null;
    }

    String[] pieces = fullName.split("\\.");
    if (pieces.length >= 1) {
      try {
        return MetricsSystem.InstanceType.fromString(pieces[0]);
      } catch (IllegalArgumentException e) {
      }
    }

    return null;
  }

  /**
   * Gets the simple name without the tags.
   *
   * @param fullName the full metric name
   * @return the base name
   */
  public static String getBaseName(String fullName) {
    String[] pieces = fullName.split("\\.");
    if (pieces.length < 2) {
      return fullName;
    } else {
      return pieces[0] + "." + pieces[1];
    }
  }

  public static Metric from(String fullName, double value, MetricType metricType) {
    String[] pieces = fullName.split("\\.");
    Preconditions.checkArgument(pieces.length > 1, "Incorrect metrics name: %s.", fullName);
    int len = pieces.length;
    String source = null;
    int tagEndIndex = len;
    // Master or cluster metrics don't have source included.
    if (!pieces[0].equals(MetricsSystem.InstanceType.MASTER.toString())
        && !pieces[0].equals(MetricsSystem.InstanceType.CLUSTER.toString())) {
      source = pieces[len - 1];
      tagEndIndex = len - 1;
    }
    MetricsSystem.InstanceType instance = MetricsSystem.InstanceType.fromString(pieces[0]);
    Metric metric = new Metric(instance, source, metricType, pieces[1], value);
    // parse tags
    for (int i = 2; i < tagEndIndex; i++) {
      String tagStr = pieces[i];
      if (!tagStr.contains(TAG_SEPARATOR)) {
        // Unknown tag
        continue;
      }
      int tagSeparatorIdx = tagStr.indexOf(TAG_SEPARATOR);
      metric.addTag(tagStr.substring(0, tagSeparatorIdx), tagStr.substring(tagSeparatorIdx + 1));
    }
    return metric;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("source", mSource)
        .add("instanceType", mInstanceType)
        .add("metricType", mMetricType)
        .add("name", mName)
        .add("tags", mTags)
        .add("value", mValue.get())
        .toString();
  }



  /**
   * @return the fully qualified metric name, which is of pattern
   *         instance.name[.tagName:tagValue]*[.source], where the tags are appended
   *         at the end
   */
  private String constructFullMetricName() {
    StringBuilder sb = new StringBuilder();
    sb.append(mInstanceType).append('.');
    sb.append(mName);
    for (Map.Entry<String, String> entry : mTags.entrySet()) {
      sb.append('.').append(entry.getKey()).append(TAG_SEPARATOR).append(entry.getValue());
    }
    if (mSource != null) {
      sb.append('.');
      sb.append(mSource);
    }
    return sb.toString();
  }

  /**
   * Data structure representing a metric name and user name.
   */
  private static class UserMetricKey {
    private String mMetric;
    private String mUser;

    private UserMetricKey(String metricName, String userName) {
      mMetric = metricName;
      mUser = userName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof UserMetricKey)) {
        return false;
      }
      UserMetricKey that = (UserMetricKey) o;
      return Objects.equal(mMetric, that.mMetric)
          && Objects.equal(mUser, that.mUser);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mMetric, mUser);
    }
  }
}

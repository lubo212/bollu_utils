package com.bollu.utils.goosefs.prometheus.metrics;

public class MetricValue {

  private Double doubleValue;
  private String stringValue;

  private MetricType metricType;

  private MetricValue(Double doubleValue, String stringValue, MetricType metricType) {
    this.doubleValue = doubleValue;
    this.stringValue = stringValue;
    this.metricType = metricType;
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public String getStringValue() {
    return stringValue;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Double doubleValue;
    private String stringValue;

    private MetricType metricType;
    public Builder setDoubleValue(Double doubleValue) {
      this.doubleValue = doubleValue;
      return this;
    }

    public Builder setStringValue(String stringValue) {
      this.stringValue = stringValue;
      return this;
    }

    public Builder setMetricType(MetricType metricType) {
      this.metricType = metricType;
      return this;
    }

    public MetricValue build() {
      return new MetricValue(doubleValue, stringValue, metricType);
    }
  }
}


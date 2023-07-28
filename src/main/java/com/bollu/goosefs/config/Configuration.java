package com.bollu.goosefs.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Configuration {

  String get(PropertyKey key);

  String get(PropertyKey key, ConfigurationValueOptions options);

  default String getOrDefault(PropertyKey key, String defaultValue) {
    return isSet(key) ? get(key) : defaultValue;
  }

  default String getOrDefault(PropertyKey key, String defaultValue,
                              ConfigurationValueOptions options) {
    return isSet(key) ? get(key, options) : defaultValue;
  }

  boolean isSet(PropertyKey key);

  boolean isSetByUser(PropertyKey key);

  Set<PropertyKey> keySet();

  Set<PropertyKey> userKeySet();

  int getInt(PropertyKey key);

  long getLong(PropertyKey key);

  double getDouble(PropertyKey key);

  float getFloat(PropertyKey key);

  boolean getBoolean(PropertyKey key);

  List<String> getList(PropertyKey key, String delimiter);

  <T extends Enum<T>> T getEnum(PropertyKey key, Class<T> enumType);

  long getBytes(PropertyKey key);

  long getMs(PropertyKey key);

  Duration getDuration(PropertyKey key);

  <T> Class<T> getClass(PropertyKey key);

  Map<String, String> getNestedProperties(PropertyKey prefixKey);

  Properties copyProperties();

  Source getSource(PropertyKey key);

  default Map<String, String> toMap() {
    return toMap(ConfigurationValueOptions.defaults());
  }

  Map<String, String> toMap(ConfigurationValueOptions opts);

  void validate();

  boolean clusterDefaultsLoaded();

  default String hash() {
    return "";
  }

}

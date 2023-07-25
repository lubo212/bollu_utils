package com.bollu.utils.goosefs.config;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.toSet;

@NotThreadSafe
public class Properties {
  private static final Logger LOG = LoggerFactory.getLogger(Properties.class);

  private final ConcurrentHashMap<PropertyKey, Optional<String>> mUserProps =
      new ConcurrentHashMap<>();

  private final ConcurrentHashMap<PropertyKey, Source> mSources = new ConcurrentHashMap<>();

  private Hash mHash = new Hash(() -> keySet().stream()
      .filter(key -> get(key) != null)
      .sorted(Comparator.comparing(PropertyKey::getName))
      .map(key -> String.format("%s:%s:%s", key.getName(), get(key), getSource(key)).getBytes()));

  public Properties() {}

  public Properties(Properties goosefsProperties) {
    mUserProps.putAll(goosefsProperties.mUserProps);
    mSources.putAll(goosefsProperties.mSources);
  }

  @Nullable
  public String get(PropertyKey key) {
    if (mUserProps.containsKey(key)) {
      return mUserProps.get(key).orElse(null);
    }
    // In case key is not the reference to the original key
    return PropertyKey.fromString(key.toString()).getDefaultValue();
  }

  public void clear() {
    mUserProps.clear();
    mSources.clear();
  }

  public void put(PropertyKey key, String value, Source source) {
    if (!mUserProps.containsKey(key) || source.compareTo(getSource(key)) >= 0) {
      mUserProps.put(key, Optional.ofNullable(value));
      mSources.put(key, source);
      mHash.markOutdated();
    }
  }

  public void set(PropertyKey key, String value) {
    put(key, value, Source.RUNTIME);
  }

  /**
   * Merges the current configuration properties with new properties. If a property exists
   * both in the new and current configuration, the one from the new configuration wins if
   * its priority is higher or equal than the existing one.
   *
   * @param properties the source {@link java.util.Properties} to be merged
   * @param source the source of the the properties (e.g., system property, default and etc)
   */
  public void merge(Map<?, ?> properties, Source source) {
    if (properties == null || properties.isEmpty()) {
      return;
    }
    // merge the properties
    for (Map.Entry<?, ?> entry : properties.entrySet()) {
      String key = entry.getKey().toString().trim();
      String value = entry.getValue() == null ? null : entry.getValue().toString().trim();
      PropertyKey propertyKey;
      if (PropertyKey.isValid(key)) {
        propertyKey = PropertyKey.fromString(key);
      } else {
        // Add unrecognized properties
        LOG.trace("Property {} from source {} is unrecognized", key, source);
        propertyKey = PropertyKey.getOrBuildCustom(key);
      }
      put(propertyKey, value, source);
    }
    mHash.markOutdated();
  }

  public void remove(PropertyKey key) {
    // remove is a nop if the key doesn't already exist
    if (mUserProps.containsKey(key)) {
      mUserProps.remove(key);
      mSources.remove(key);
      mHash.markOutdated();
    }
  }

  public boolean isSet(PropertyKey key) {
    if (isSetByUser(key)) {
      return true;
    }
    // In case key is not the reference to the original key
    return PropertyKey.fromString(key.toString()).getDefaultValue() != null;
  }

  public boolean isSetByUser(PropertyKey key) {
    if (mUserProps.containsKey(key)) {
      Optional<String> val = mUserProps.get(key);
      return val.isPresent();
    }
    return false;
  }

  public Set<Map.Entry<PropertyKey, String>> entrySet() {
    return keySet().stream().map(key -> Maps.immutableEntry(key, get(key))).collect(toSet());
  }

  public Set<PropertyKey> keySet() {
    Set<PropertyKey> keySet = new HashSet<>(PropertyKey.defaultKeys());
    keySet.addAll(mUserProps.keySet());
    return Collections.unmodifiableSet(keySet);
  }

  public Set<PropertyKey> userKeySet() {
    return Collections.unmodifiableSet(mUserProps.keySet());
  }

  public void forEach(BiConsumer<? super PropertyKey, ? super String> action) {
    for (Map.Entry<PropertyKey, String> entry : entrySet()) {
      action.accept(entry.getKey(), entry.getValue());
    }
  }

  public Properties copy() {
    return new Properties(this);
  }

  public void setSource(PropertyKey key, Source source) {
    mSources.put(key, source);
    mHash.markOutdated();
  }

  public Source getSource(PropertyKey key) {
    Source source = mSources.get(key);
    if (source != null) {
      return source;
    }
    return Source.DEFAULT;
  }

  public String hash() {
    return mHash.get();
  }

}

package com.bollu.utils.goosefs.config;

import com.bollu.utils.goosefs.common.utils.FormatUtils;
import com.bollu.utils.goosefs.config.util.ConfigurationUtils;
import com.bollu.utils.goosefs.common.exception.ExceptionMessage;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstancedConfiguration implements Configuration {

  private static final Logger LOG = LoggerFactory.getLogger(InstancedConfiguration.class);

  public static final Configuration EMPTY_CONFIGURATION = new InstancedConfiguration(new Properties());

  /** Regex string to find "${key}" for variable substitution. */
  private static final String REGEX_STRING = "(\\$\\{([^{}]*)\\})";

  private static final Pattern CONF_REGEX = Pattern.compile(REGEX_STRING);

  protected Properties mProperties;

  private final boolean mClusterDefaultsLoaded;

  public static InstancedConfiguration defaults() {
    return new InstancedConfiguration(ConfigurationUtils.defaults());
  }

  public InstancedConfiguration(Properties properties) {
    mProperties = properties;
    mClusterDefaultsLoaded = false;
  }

  public InstancedConfiguration(Properties properties, boolean clusterDefaultsLoaded) {
    mProperties = properties;
    mClusterDefaultsLoaded = clusterDefaultsLoaded;
  }

  public InstancedConfiguration(Configuration conf) {
    mProperties = conf.copyProperties();
    mClusterDefaultsLoaded = conf.clusterDefaultsLoaded();
  }

  @Override
  public String get(PropertyKey key) {
    return get(key, ConfigurationValueOptions.defaults());
  }

  @Override
  public String get(PropertyKey key, ConfigurationValueOptions options) {
    String value = mProperties.get(key);
    if (value == null) {
      // if value or default value is not set in configuration for the given key
      throw new RuntimeException(ExceptionMessage.UNDEFINED_CONFIGURATION_KEY.getMessage(key));
    }
    if (!options.shouldUseRawValue()) {
      try {
        value = lookup(value);
      } catch (UnresolvablePropertyException e) {
        throw new RuntimeException("Could not resolve key \""
            + key.getName() + "\": " + e.getMessage(), e);
      }
    }
    return value;
  }

  private boolean isResolvable(PropertyKey key) {
    String val = mProperties.get(key);
    try {
      // Lookup to resolve any key before simply returning isSet. An exception will be thrown if
      // the key can't be resolved or if a lower level value isn't set.
      lookup(val);
      return true;
    } catch (UnresolvablePropertyException e) {
      return false;
    }
  }

  @Override
  public boolean isSet(PropertyKey key) {
    return mProperties.isSet(key) && isResolvable(key);
  }

  @Override
  public boolean isSetByUser(PropertyKey key) {
    return mProperties.isSetByUser(key) && isResolvable(key);
  }

  public void set(PropertyKey key, Object value) {
    set(key, String.valueOf(value), Source.RUNTIME);
  }

  public void set(@Nonnull PropertyKey key, @Nonnull Object value, @Nonnull Source source) {
    Preconditions.checkArgument(key != null && value != null && !value.equals(""),
        String.format("The key value pair (%s, %s) cannot be null", key, value));
    Preconditions.checkArgument(!value.equals(""),
        String.format("The key \"%s\" cannot be have an empty string as a value. Use "
            + "ServerConfiguration.unset to remove a key from the configuration.", key));
    mProperties.put(key, String.valueOf(value), source);
  }

  public void unset(PropertyKey key) {
    Preconditions.checkNotNull(key, "key");
    mProperties.remove(key);
  }

  public void merge(Map<?, ?> properties, Source source) {
    mProperties.merge(properties, source);
  }

  @Override
  public Set<PropertyKey> keySet() {
    return mProperties.keySet();
  }

  @Override
  public Set<PropertyKey> userKeySet() {
    return mProperties.userKeySet();
  }

  @Override
  public int getInt(PropertyKey key) {
    String rawValue = get(key);

    try {
      return Integer.parseInt(rawValue);
    } catch (NumberFormatException e) {
      throw new RuntimeException(ExceptionMessage.KEY_NOT_INTEGER.getMessage(rawValue, key));
    }
  }

  @Override
  public long getLong(PropertyKey key) {
    String rawValue = get(key);

    try {
      return Long.parseLong(rawValue);
    } catch (NumberFormatException e) {
      throw new RuntimeException(ExceptionMessage.KEY_NOT_LONG.getMessage(rawValue, key));
    }
  }

  @Override
  public double getDouble(PropertyKey key) {
    String rawValue = get(key);

    try {
      return Double.parseDouble(rawValue);
    } catch (NumberFormatException e) {
      throw new RuntimeException(ExceptionMessage.KEY_NOT_DOUBLE.getMessage(rawValue, key));
    }
  }

  @Override
  public float getFloat(PropertyKey key) {
    String rawValue = get(key);

    try {
      return Float.parseFloat(rawValue);
    } catch (NumberFormatException e) {
      throw new RuntimeException(ExceptionMessage.KEY_NOT_FLOAT.getMessage(rawValue, key));
    }
  }

  @Override
  public boolean getBoolean(PropertyKey key) {
    String rawValue = get(key);

    if (rawValue.equalsIgnoreCase("true")) {
      return true;
    } else if (rawValue.equalsIgnoreCase("false")) {
      return false;
    } else {
      throw new RuntimeException(ExceptionMessage.KEY_NOT_BOOLEAN.getMessage(rawValue, key));
    }
  }

  @Override
  public List<String> getList(PropertyKey key, String delimiter) {
    Preconditions.checkArgument(delimiter != null,
        "Illegal separator for GooseFS properties as list");
    String rawValue = get(key);

    return ConfigurationUtils.parseAsList(rawValue, delimiter);
  }

  @Override
  public <T extends Enum<T>> T getEnum(PropertyKey key, Class<T> enumType) {
    String rawValue = get(key);

    try {
      return Enum.valueOf(enumType, rawValue);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(ExceptionMessage.UNKNOWN_ENUM.getMessage(rawValue, key,
          Arrays.toString(enumType.getEnumConstants())));
    }
  }

  @Override
  public long getBytes(PropertyKey key) {
    String rawValue = get(key);

    try {
      return FormatUtils.parseSpaceSize(rawValue);
    } catch (Exception ex) {
      throw new RuntimeException(ExceptionMessage.KEY_NOT_BYTES.getMessage(rawValue, key));
    }
  }

  @Override
  public long getMs(PropertyKey key) {
    String rawValue = get(key);
    try {
      return FormatUtils.parseTimeSize(rawValue);
    } catch (Exception e) {
      throw new RuntimeException(ExceptionMessage.KEY_NOT_MS.getMessage(rawValue, key));
    }
  }

  @Override
  public Duration getDuration(PropertyKey key) {
    return Duration.ofMillis(getMs(key));
  }

  @Override
  public <T> Class<T> getClass(PropertyKey key) {
    String rawValue = get(key);
    try {
      @SuppressWarnings("unchecked")
      Class<T> clazz = (Class<T>) Class.forName(rawValue);
      return clazz;
    } catch (Exception e) {
      LOG.error("requested class could not be loaded: {}", rawValue, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, String> getNestedProperties(PropertyKey prefixKey) {
    Map<String, String> ret = Maps.newHashMap();
    for (Map.Entry<PropertyKey, String> entry: mProperties.entrySet()) {
      String key = entry.getKey().getName();
      if (prefixKey.isNested(key)) {
        String suffixKey = key.substring(prefixKey.length() + 1);
        ret.put(suffixKey, entry.getValue());
      }
    }
    return ret;
  }

  @Override
  public Properties copyProperties() {
    return mProperties.copy();
  }

  @Override
  public Source getSource(PropertyKey key) {
    return mProperties.getSource(key);
  }

  @Override
  public Map<String, String> toMap(ConfigurationValueOptions opts) {
    Map<String, String> map = new HashMap<>();
    // Cannot use Collectors.toMap because we support null keys.
    keySet().forEach(key -> map.put(key.getName(), getOrDefault(key, null, opts)));
    return map;
  }

  //ignore
  @Override
  public void validate() {
    if (!getBoolean(PropertyKey.CONF_VALIDATION_ENABLED)) {
      return;
    }
  }

  @Override
  public boolean clusterDefaultsLoaded() {
    return mClusterDefaultsLoaded;
  }
  @Override
  public String hash() {
    return mProperties.hash();
  }

  private String lookup(final String base) throws UnresolvablePropertyException {
    return lookupRecursively(base, new HashSet<>());
  }

  //找到value中的"${key}"，找到对应的key替换它
  private String lookupRecursively(String base, Set<String> seen)
      throws UnresolvablePropertyException {
    if (base == null) {
      throw new UnresolvablePropertyException("Can't resolve property with null value");
    }

    String resolved = base;
    Matcher matcher = CONF_REGEX.matcher(base);
    while (matcher.find()) {
      String match = matcher.group(2).trim();
      if (!seen.add(match)) {
        throw new RuntimeException(ExceptionMessage.KEY_CIRCULAR_DEPENDENCY.getMessage(match));
      }
      if (!PropertyKey.isValid(match)) {
        throw new RuntimeException(ExceptionMessage.INVALID_CONFIGURATION_KEY.getMessage(match));
      }
      String value = lookupRecursively(mProperties.get(PropertyKey.fromString(match)), seen);
      seen.remove(match);
      if (value == null) {
        throw new UnresolvablePropertyException(ExceptionMessage
            .UNDEFINED_CONFIGURATION_KEY.getMessage(match));
      }
      resolved = resolved.replaceFirst(REGEX_STRING, Matcher.quoteReplacement(value));
    }
    return resolved;
  }

  private class UnresolvablePropertyException extends Exception {

    public UnresolvablePropertyException(String msg) {
      super(msg);
    }
  }
}

package com.bollu.utils.goosefs.prometheus.metrics.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NotThreadSafe
public class MetricsConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsConfig.class);
  private Properties mProperties;

  /**
   * Creates a new {@code MetricsConfig} using the given config file.
   *
   * @param configFile config file to use
   */
  public MetricsConfig(String configFile) {
    mProperties = new Properties();
    if (Files.exists(Paths.get(configFile))) {
      loadConfigFile(configFile);
    }
  }

  /**
   * Creates a new {@code MetricsConfig} using the given {@link Properties}.
   *
   * @param properties properties to use
   */
  public MetricsConfig(Properties properties) {
    mProperties = new Properties();
    mProperties.putAll(properties);
  }

  /**
   * @return the properties
   */
  public Properties getProperties() {
    return mProperties;
  }

  /**
   * Uses regex to parse every original property key to a prefix and a suffix. Creates sub
   * properties that are grouped by the prefix.
   *
   * @param prop the original properties
   * @param regex prefix and suffix pattern
   * @return a {@code Map} from the prefix to its properties
   */
  public static Map<String, Properties> subProperties(Properties prop, String regex) {
    Map<String, Properties> subProperties = new HashMap<>();
    Pattern pattern = Pattern.compile(regex);

    for (Map.Entry<Object, Object> entry : prop.entrySet()) {
      Matcher m = pattern.matcher(entry.getKey().toString());
      if (m.find()) {
        String prefix = m.group(1);
        String suffix = m.group(2);
        if (!subProperties.containsKey(prefix)) {
          subProperties.put(prefix, new Properties());
        }
        subProperties.get(prefix).put(suffix, entry.getValue());
      }
    }

    return subProperties;
  }


  private void loadConfigFile(String configFile) {
    try (InputStream is = new FileInputStream(configFile)) {
      mProperties.load(is);
    } catch (Exception e) {
      LOG.error("Error loading metrics configuration file.", e);
    }
  }

  @Override
  public String toString() {
    return mProperties.toString();
  }

}
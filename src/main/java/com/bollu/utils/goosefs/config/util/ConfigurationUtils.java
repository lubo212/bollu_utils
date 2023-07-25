package com.bollu.utils.goosefs.config.util;

import com.bollu.utils.goosefs.config.*;
import com.bollu.utils.goosefs.common.constant.Constants;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ConfigurationUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUtils.class);
  @GuardedBy("DEFAULT_PROPERTIES_LOCK")
  private static volatile Properties sDefaultProperties = null;

  private static String sSourcePropertyFile = null;

  private static final Object DEFAULT_PROPERTIES_LOCK = new Object();

  private static final String MASTERS = "masters";
  private static final String WORKERS = "workers";

  private static final long expireTime = 30000;// 30sec
  private static final AtomicLong loadTime = new AtomicLong(-1);

  private ConfigurationUtils() {} // prevent instantiation

  public static Properties defaults() {
    if (sDefaultProperties == null) {
      synchronized (DEFAULT_PROPERTIES_LOCK) { // We don't want multiple threads to reload
        // properties at the same time.
        // Check if properties are still null so we don't reload a second time.
        if (sDefaultProperties == null) {
          reloadProperties();
        }
      }
    }
    return sDefaultProperties.copy();
  }

  public static void reloadProperties() {
    synchronized (DEFAULT_PROPERTIES_LOCK) {
      Properties properties = new Properties();
      InstancedConfiguration conf = new InstancedConfiguration(properties);
      java.util.Properties sysProps = new java.util.Properties();
      System.getProperties().stringPropertyNames()
          .forEach(key -> sysProps.setProperty(key, System.getProperty(key)));
      properties.merge(sysProps, Source.SYSTEM_PROPERTY);

      String confPaths = conf.get(PropertyKey.SITE_CONF_DIR);
      String[] confPathList = confPaths.split(",");
      String sitePropertyFile = ConfigurationUtils
          .searchPropertiesFile(Constants.SITE_PROPERTIES, confPathList);
      java.util.Properties siteProps = null;
      if (sitePropertyFile != null) {
        siteProps = loadPropertiesFromFile(sitePropertyFile);
        sSourcePropertyFile = sitePropertyFile;
      }
      properties.merge(siteProps, Source.siteProperty(sSourcePropertyFile));
      conf.validate();
      sDefaultProperties = properties;
    }
  }

  public static Configuration merge(Configuration conf, Map<?, ?> properties,
                                    Source source) {
    Properties props = conf.copyProperties();
    props.merge(properties, source);
    return new InstancedConfiguration(props);
  }

  @Nullable
  public static String searchPropertiesFile(String propertiesFile,
                                            String[] confPathList) {
    if (propertiesFile == null || confPathList == null) {
      return null;
    }
    for (String path : confPathList) {
      String file = PathUtils.concatPath(path, propertiesFile);
      java.util.Properties properties = loadPropertiesFromFile(file);
      if (properties != null) {
        // If a site conf is successfully loaded, stop trying different paths.
        return file;
      }
    }
    return null;
  }

  public static List<String> parseAsList(String value, String delimiter) {
    return Lists.newArrayList(Splitter.on(delimiter).trimResults().omitEmptyStrings()
        .split(value));
  }

  @Nullable
  public static java.util.Properties loadPropertiesFromFile(String filePath) {
    try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
      return loadProperties(fileInputStream);
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      LOG.warn("Failed to close property input stream from {}: {}", filePath, e.toString());
      return null;
    }
  }

  @Nullable
  public static java.util.Properties loadProperties(InputStream stream) {
    java.util.Properties properties = new java.util.Properties();
    try {
      properties.load(stream);
    } catch (IOException e) {
      LOG.warn("Unable to load properties: {}", e.toString());
      return null;
    }
    return properties;
  }

}

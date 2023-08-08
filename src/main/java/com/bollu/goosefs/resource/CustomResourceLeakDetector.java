package com.bollu.goosefs.resource;

import com.bollu.goosefs.config.InstancedConfiguration;
import com.bollu.goosefs.config.PropertyKey;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomResourceLeakDetector<T> extends ResourceLeakDetector<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomResourceLeakDetector.class);

  private static final String DOC_URL = "https://docs.alluxio.io/os/user/stable/en/operation/"
      + "Troubleshooting.html#resource-leak-detection";

  static {
    ResourceLeakDetector.Level lev = InstancedConfiguration.defaults()
        .getEnum(PropertyKey.LEAK_DETECTOR_LEVEL, Level.class);
    ResourceLeakDetector.setLevel(lev);
  }

  private final boolean mExitOnLeak;

  public CustomResourceLeakDetector(Class<?> resourceType, int samplingInterval,
                                     boolean exitOnLeak) {
    super(resourceType, samplingInterval);
    mExitOnLeak = exitOnLeak;
  }

  @Override
  protected void reportTracedLeak(String resourceType, String records) {
    LOGGER.error("LEAK: {}.close() was not called before resource is garbage-collected. "
            + "See {} for more information about this message.{}",
        resourceType, DOC_URL, records);
    if (mExitOnLeak) {
      LOGGER.error("Leak detected when {} set to true. Shutting down the JVM",
          PropertyKey.Name.LEAK_DETECTOR_EXIT_ON_LEAK);
      System.exit(1);
    }
  }

  @Override
  protected void reportUntracedLeak(String resourceType) {
    LOGGER.error("LEAK: {}.close() was not called before resource is garbage-collected. "
            + "See {} for more information about this message.",
        resourceType, DOC_URL);
    if (mExitOnLeak) {
      LOGGER.error("Leak detected when {} set to true. Shutting down the JVM",
          PropertyKey.Name.LEAK_DETECTOR_EXIT_ON_LEAK);
      System.exit(1);
    }
  }
}

package com.bollu.goosefs.resource;

import com.bollu.goosefs.config.InstancedConfiguration;
import com.bollu.goosefs.config.PropertyKey;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;

public class CustomResourceLeakDetectorFactory extends ResourceLeakDetectorFactory {
  private static final ResourceLeakDetectorFactory INSTANCE =
      new CustomResourceLeakDetectorFactory();

  private final boolean mExitOnLeak = (boolean) InstancedConfiguration.defaults().getBoolean(
      PropertyKey.LEAK_DETECTOR_EXIT_ON_LEAK);

  public static ResourceLeakDetectorFactory instance() {
    return INSTANCE;
  }

  @Override
  public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource,
                                                             int samplingInterval, long maxActive) {
    return newResourceLeakDetector(resource, samplingInterval);
  }

  @Override
  public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource,
                                                             int samplingInterval) {
    return new CustomResourceLeakDetector<>(resource, samplingInterval, mExitOnLeak);
  }
}

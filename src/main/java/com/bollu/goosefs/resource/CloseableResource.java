package com.bollu.goosefs.resource;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakTracker;

import javax.annotation.Nullable;
import java.io.Closeable;

public abstract class CloseableResource<T> implements Closeable {

  private static final ResourceLeakDetector<CloseableResource> DETECTOR =
      CustomResourceLeakDetectorFactory.instance()
          .newResourceLeakDetector(CloseableResource.class);

  private final T mResource;

  @Nullable
  private final ResourceLeakTracker<CloseableResource> mTracker = DETECTOR.track(this);

  public CloseableResource(T resource) {
    mResource = resource;
  }

  public T get() {
    return mResource;
  }

  @Override
  public void close() {
    if (mTracker != null) {
      mTracker.close(this);
    }
    closeResource();
  }

  public abstract void closeResource();
}

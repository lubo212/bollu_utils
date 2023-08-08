package com.bollu.goosefs.resource.resourcepool;

import com.bollu.goosefs.resource.CloseableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

/**
 * 这个类是为了防止池里的资源泄露
 * @param <T>
 */
public class PooledResource<T> extends CloseableResource<T> {
  private static final Logger LOG = LoggerFactory.getLogger(PooledResource.class);

  protected final WeakReference<Pool<T>> mPool;

  protected final String mPoolDescription;

  public PooledResource(T resource, Pool<T> pool) {
    super(resource);
    mPool = new WeakReference<>(pool);
    mPoolDescription = String.format("%s@%s",
        pool.getClass().getName(), Integer.toHexString(pool.hashCode())).intern();
  }

  @Override
  public void closeResource() {
    Pool<T> pool = mPool.get();
    if (pool != null) {
      pool.release(get());
    } else {
      // the pool is gone before this resource can be released, report a leak
      T leaked = get();
      String resType = leaked.getClass().getName();
      LOG.warn("resource of type {} leaked from pool {} which had been GCed before the resource "
          + "could be released", resType, mPoolDescription);
      // do a best effort attempt to close the resource
      if (leaked instanceof AutoCloseable) {
        try {
          ((AutoCloseable) leaked).close();
        } catch (Exception e) {
          throw new RuntimeException(
              String.format("failed to close leaked resource %s: %s", resType, e), e);
        }
      }
    }
  }
}

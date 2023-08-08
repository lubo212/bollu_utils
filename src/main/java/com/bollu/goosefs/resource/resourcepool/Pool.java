package com.bollu.goosefs.resource.resourcepool;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Pool<T> extends Closeable {

  T acquire() throws IOException;

  T acquire(long time, TimeUnit unit) throws TimeoutException, IOException;

  default PooledResource<T> acquireCloseable() throws IOException {
    return new PooledResource<>(acquire(), this);
  }

  default PooledResource<T> acquireCloseable(long time, TimeUnit unit)
      throws TimeoutException, IOException {
    return new PooledResource<>(acquire(time, unit), this);
  }

  void release(T resource);

  int size();
}

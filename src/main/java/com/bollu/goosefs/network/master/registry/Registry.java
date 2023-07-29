package com.bollu.goosefs.network.master.registry;

import com.bollu.goosefs.common.constant.Constants;
import com.bollu.goosefs.common.options.WaitForOptions;
import com.bollu.goosefs.common.utils.CommonUtils;
import com.bollu.goosefs.network.Server;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Registry<T extends Server<U>, U> {

  private final Map<Class<? extends Server<U>>, T> mRegistry = new HashMap<>();
  private final Map<Class<? extends Server<U>>, T> mAlias = new HashMap<>();

  private final Lock mLock = new ReentrantLock();

  public Registry() {}

  public <W extends T> W get(final Class<W> clazz) {
    return get(clazz, Constants.DEFAULT_REGISTRY_GET_TIMEOUT_MS);
  }

  public <W extends T> W get(final Class<W> clazz, int timeoutMs) {
    return getInternal(clazz, WaitForOptions.defaults().setTimeoutMs(timeoutMs), true);
  }

  public <W extends T> W getCanonical(final Class<W> type) {
    return getCanonical(type, Constants.DEFAULT_REGISTRY_GET_TIMEOUT_MS);
  }

  public <W extends T> W getCanonical(final Class<W> type, int timeoutMs) {
    return getInternal(type, WaitForOptions.defaults().setTimeoutMs(timeoutMs), false);
  }

  private <W extends T> W getInternal(Class<W> type, WaitForOptions options, boolean includeAlias) {
    try {
      return CommonUtils.waitForResult(
          "server " + type.getName() + " to be created",
          () -> getInternal(type, includeAlias),
          Objects::nonNull,
          options);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  <W extends T> W getInternal(Class<W> clazz, boolean includeAlias) {
//    try (LockResource r = new LockResource(mLock)) {
      T server = mRegistry.get(clazz);
      if (server == null && includeAlias) {
        server = mAlias.get(clazz);
      }
      try {
        return clazz.cast(server);
      } catch (ClassCastException e) {
        return null;
      }
//    }
  }

  /**
   * Registers a canonical mapping of the type to the server instance. Overrides any existing
   * mappings.
   *
   * @param clazz the class of the {@link Server} to add
   * @param server the {@link Server} to add
   * @param <W> the type of the {@link Server} to add
   */
  public <W extends T> void add(Class<W> clazz, T server) {
    Preconditions.checkNotNull(clazz, "clazz");
    Preconditions.checkNotNull(server, "server");
    Preconditions.checkArgument(clazz.isInstance(server),
        "Server %s is not an instance of %s", server.getClass(), clazz.getName());
//    try (LockResource r = new LockResource(mLock)) {
      mRegistry.put(clazz, server);
//    }
  }

}

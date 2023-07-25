package com.bollu.utils.goosefs.prometheus.metrics.util;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CommonUtils {
  private static final TimeUnit MINIMAL_POLL_UNIT = TimeUnit.SECONDS;
  private static final int MINIMAL_POLL_PERIOD = 1;

  public static void checkMinimalPollingPeriod(TimeUnit pollUnit, int pollPeriod)
      throws IllegalArgumentException {
    int period = (int) MINIMAL_POLL_UNIT.convert(pollPeriod, pollUnit);
    Preconditions.checkArgument(period >= MINIMAL_POLL_PERIOD,
        "Polling period %d %d is below the minimal polling period", pollPeriod, pollUnit);
  }

  /**
   * Memoize implementation for java.util.function.supplier.
   *
   * @param original the original supplier
   * @param <T> the object type
   * @return the supplier with memorization
   */
  //全局变量可能需要调用其他全局变量，这个函数可以支持延迟加载
  public static <T> Supplier<T> memoize(Supplier<T> original) {
    return new Supplier<T>() {
      Supplier<T> mDelegate = this::firstTime;
      boolean mInitialized;
      public T get() {
        return mDelegate.get();
      }

      private synchronized T firstTime() {
        if (!mInitialized) {
          T value = original.get();
          mDelegate = () -> value;
          mInitialized = true;
        }
        return mDelegate.get();
      }
    };
  }


}

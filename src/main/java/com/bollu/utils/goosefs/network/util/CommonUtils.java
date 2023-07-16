package com.bollu.utils.goosefs.network.util;

import java.util.concurrent.atomic.AtomicReference;

public class CommonUtils {

  public enum ProcessType {
    CLIENT,
    MASTER,
    PROXY,
    WORKER;
  }

  public static final AtomicReference<ProcessType> PROCESS_TYPE = new AtomicReference<>(ProcessType.CLIENT);

}

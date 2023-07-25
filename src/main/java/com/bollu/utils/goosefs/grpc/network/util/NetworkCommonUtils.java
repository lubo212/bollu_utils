package com.bollu.utils.goosefs.grpc.network.util;

import java.util.concurrent.atomic.AtomicReference;

public class NetworkCommonUtils {

  public enum ProcessType {
    CLIENT,
    MASTER,
    PROXY,
    WORKER;
  }

  public static final AtomicReference<ProcessType> PROCESS_TYPE = new AtomicReference<>(ProcessType.CLIENT);

}

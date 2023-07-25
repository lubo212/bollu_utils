package com.bollu.utils.goosefs.grpc.network;

import com.bollu.utils.goosefs.grpc.network.util.NetworkCommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Master {
  private static final Logger LOG = LoggerFactory.getLogger(Master.class);

  public static void main(String[] args) {
    NetworkCommonUtils.PROCESS_TYPE.set(NetworkCommonUtils.ProcessType.MASTER);

  }

}

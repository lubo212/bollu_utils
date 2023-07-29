package com.bollu.goosefs.network.master;

import com.bollu.goosefs.network.util.NetworkCommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterMain {
  private static final Logger LOG = LoggerFactory.getLogger(MasterMain.class);

  public static void main(String[] args) {
    NetworkCommonUtils.PROCESS_TYPE.set(NetworkCommonUtils.ProcessType.MASTER);

  }

}

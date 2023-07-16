package com.bollu.utils.goosefs.network;

import com.bollu.utils.goosefs.network.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Master {
  private static final Logger LOG = LoggerFactory.getLogger(Master.class);

  public static void main(String[] args) {
    CommonUtils.PROCESS_TYPE.set(CommonUtils.ProcessType.MASTER);
  }

}

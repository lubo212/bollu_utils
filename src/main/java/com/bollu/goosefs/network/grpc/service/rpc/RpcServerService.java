package com.bollu.goosefs.network.grpc.service.rpc;

import com.bollu.goosefs.network.grpc.service.SimpleService;
import com.bollu.goosefs.network.master.process.MasterProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RpcServerService implements SimpleService {
  protected static final Logger LOG = LoggerFactory.getLogger(RpcServerService.class);

//  protected final InetSocketAddress mBindAddress;
//  protected final MasterProcess mMasterProcess;

  @Override
  public void start() {

  }

  @Override
  public void promote() {

  }

  @Override
  public void demote() {

  }

  @Override
  public void stop() {

  }
}

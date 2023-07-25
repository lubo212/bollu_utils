package com.bollu.utils.goosefs.grpc.network.process;

import com.bollu.utils.goosefs.config.InstancedConfiguration;
import com.bollu.utils.goosefs.config.network.ServiceType;
import com.bollu.utils.goosefs.jetty.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public abstract class MasterProcess implements Process {

  private static final Logger LOG = LoggerFactory.getLogger(MasterProcess.class);

  final long mStartTimeMs = System.currentTimeMillis();

  final InetSocketAddress mRpcBindAddress;
  protected GrpcServer mGrpcServer;

  final InetSocketAddress mServiceRpcBindAddress;
  protected GrpcServer mServiceGrpcServer;

  public MasterProcess(ServiceType rpcService, ServiceType serviceRpcService){
    mRpcBindAddress = configureAddress(rpcService);
    mServiceRpcBindAddress = configureAddress(serviceRpcService);
  }

  @Override
  public void start() throws Exception {

  }

  @Override
  public void stop() throws Exception {

  }

  @Override
  public boolean waitForReady(int timeoutMs) {
    return false;
  }

  private static InetSocketAddress configureAddress(ServiceType service) {
    InstancedConfiguration conf = InstancedConfiguration.defaults();
  }
}

package com.bollu.goosefs.network.master.process;

import com.bollu.goosefs.common.utils.NetworkAddressUtils;
import com.bollu.goosefs.config.InstancedConfiguration;
import com.bollu.goosefs.network.ServiceType;
import com.bollu.goosefs.network.util.NetworkCommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public abstract class MasterProcess implements Process {

  private static final Logger LOG = LoggerFactory.getLogger(MasterProcess.class);

  final long mStartTimeMs = System.currentTimeMillis();

  final InetSocketAddress mRpcBindAddress;
//  protected GrpcServer mGrpcServer;

  final InetSocketAddress mServiceRpcBindAddress;
//  protected GrpcServer mServiceGrpcServer;

  public MasterProcess(ServiceType rpcService, ServiceType serviceRpcService) {
    mRpcBindAddress = configureAddress(rpcService);
    mServiceRpcBindAddress = configureAddress(serviceRpcService);
  }

  private static InetSocketAddress configureAddress(ServiceType service) {
    InstancedConfiguration conf = InstancedConfiguration.defaults();
    int port = NetworkAddressUtils.getPort(service, conf);
    if (!NetworkCommonUtils.isHaMode(conf) && port == 0) {
      throw new RuntimeException(
          String.format("%s port must be nonzero in single-master mode", service));
    }

    if (port == 0) {
      try (ServerSocket s = new ServerSocket(0)) {
        s.setReuseAddress(true);
        conf.set(service.getPortKey(), s.getLocalPort());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return NetworkAddressUtils.getBindAddress(service, conf);
  }

  public abstract InetSocketAddress getRpcAddress();

//  public abstract <T extends Master> T getMaster(Class<T> clazz);

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


}

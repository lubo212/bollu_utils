package com.bollu.goosefs.network.grpc;

import com.google.common.io.Closer;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

import java.io.Closeable;

public class GrpcService {

  private final ServerServiceDefinition mServiceDefinition;
  private boolean mAuthenticated = true;
  private Closer mCloser = Closer.create();

  public GrpcService(BindableService bindableService) {
    mServiceDefinition = bindableService.bindService();
  }

  public GrpcService(ServerServiceDefinition serviceDefinition) {
    mServiceDefinition = serviceDefinition;
  }

  public GrpcService disableAuthentication() {
    mAuthenticated = false;
    return this;
  }

  public GrpcService withCloseable(Closeable closeable) {
    mCloser.register(closeable);
    return this;
  }

  public Closer getCloser() {
    return mCloser;
  }

  public ServerServiceDefinition getServiceDefinition() {
    return mServiceDefinition;
  }

}

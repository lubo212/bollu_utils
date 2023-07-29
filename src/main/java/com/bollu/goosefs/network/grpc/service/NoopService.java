package com.bollu.goosefs.network.grpc.service;

public class NoopService implements SimpleService {
  @Override
  public void start() {}

  @Override
  public void promote() {}

  @Override
  public void demote() {}

  @Override
  public void stop() {}
}

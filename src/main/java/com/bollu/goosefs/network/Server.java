package com.bollu.goosefs.network;

import com.bollu.goosefs.network.grpc.GrpcService;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface Server<T> {

  Set<Class<? extends Server>> getDependencies();

  String getName();

  Map<ServiceType, GrpcService> getServices();

  void start(T options) throws IOException;

  void stop() throws IOException;

  void close() throws IOException;
}

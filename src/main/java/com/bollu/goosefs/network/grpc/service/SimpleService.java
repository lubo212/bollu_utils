package com.bollu.goosefs.network.grpc.service;

public interface SimpleService {

  void start();

  void promote();

  void demote();

  void stop();

}

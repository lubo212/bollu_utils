package com.bollu.utils.goosefs.grpc.network.process;

public interface Process {
  void start() throws Exception;

  void stop() throws Exception;

  //Waits until the process is ready to serve requests.
  boolean waitForReady(int timeoutMs);
}

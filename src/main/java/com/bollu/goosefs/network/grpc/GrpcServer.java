package com.bollu.goosefs.network.grpc;

import com.bollu.goosefs.network.grpc.authentication.AuthenticationServer;
import com.bollu.goosefs.retry.ExponentialBackoffRetry;
import com.bollu.goosefs.retry.RetryUtils;
import com.google.common.base.MoreObjects;
import com.google.common.io.Closer;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcServer {
  private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);

  private final Server mServer;
  private final Closer mCloser;
  private boolean mStarted = false;
  private final long mServerShutdownTimeoutMs;

  //todo 为什么closer要在GrpcServer里进行关闭
  public GrpcServer(Server server,  Closer closer,
                    long serverShutdownTimeoutMs) {
    mServer = server;
    mCloser = closer;
    mServerShutdownTimeoutMs = serverShutdownTimeoutMs;
  }

  public GrpcServer start() throws IOException {
    RetryUtils.retry("Starting gRPC server", () -> mServer.start(),
        new ExponentialBackoffRetry(100, 500, 5));
    mStarted = true;
    return this;
  }

  public int getBindPort() {
    return mServer.getPort();
  }

  public boolean shutdown() {
    // Stop accepting new connections.
    mServer.shutdown();
    // Close resources that potentially owns active streams.
    try {
      mCloser.close();
    } catch (IOException e) {
      LOG.error("Failed to close resources during shutdown.", e);
      // Do nothing.
    }
    // Force shutdown remaining calls.
    mServer.shutdownNow();
    // Wait until server terminates.
    try {
      return mServer.awaitTermination(mServerShutdownTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  public void awaitTermination() {
    try {
      mServer.awaitTermination();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      // Allow thread to exit.
    }
  }

  public boolean isServing() {
    return mStarted && !mServer.isShutdown() || !mServer.isTerminated();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("InternalServer", mServer)
        .toString();
  }
}

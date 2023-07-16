//package com.bollu.utils.goosefs.network.process;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//
//public abstract class MasterProcess implements Process {
//
//  private static final Logger LOG = LoggerFactory.getLogger(MasterProcess.class);
//
//  final long mStartTimeMs = System.currentTimeMillis();
//
//  final InetSocketAddress mRpcBindAddress;
//  protected GrpcServer mGrpcServer;
//
//  final InetSocketAddress mServiceRpcBindAddress;
//  protected GrpcServer mServiceGrpcServer;
//
//  final InetSocketAddress mWebBindAddress;
//  protected WebServer mWebServer;
//
//  @Override
//  public void start() throws Exception {
//
//  }
//
//  @Override
//  public void stop() throws Exception {
//
//  }
//
//  @Override
//  public boolean waitForReady(int timeoutMs) {
//    return false;
//  }
//}

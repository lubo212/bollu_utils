package com.bollu.goosefs.network.grpc.authentication;

import com.qcloud.cos.goosefs.grpc.SaslMessage;

import javax.security.sasl.SaslException;

public interface SaslClientHandler extends AutoCloseable {

  SaslMessage handleMessage(SaslMessage message) throws SaslException;

  @Override
  void close();
}

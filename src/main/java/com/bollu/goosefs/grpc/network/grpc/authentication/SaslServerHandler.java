package com.bollu.goosefs.grpc.network.grpc.authentication;

import com.qcloud.cos.goosefs.grpc.SaslMessage;

import javax.security.sasl.SaslException;

public interface SaslServerHandler extends AutoCloseable {
  SaslMessage handleMessage(SaslMessage message) throws SaslException;
  void setAuthenticatedUserInfo(AuthenticatedUserInfo userinfo);
  AuthenticatedUserInfo getAuthenticatedUserInfo();
  @Override
  void close();
}

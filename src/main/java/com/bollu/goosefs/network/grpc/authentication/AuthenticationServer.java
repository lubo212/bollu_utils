package com.bollu.goosefs.network.grpc.authentication;

import com.bollu.goosefs.common.exception.status.UnauthenticatedException;
import com.qcloud.cos.goosefs.grpc.ChannelAuthenticationScheme;
import io.grpc.BindableService;

import javax.security.sasl.SaslException;
import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

public interface AuthenticationServer extends BindableService, Closeable {

  void registerChannel(UUID channelId, AuthenticatedUserInfo userInfo,
                       AuthenticatedChannelServerDriver saslDriver);

  AuthenticatedUserInfo getUserInfoForChannel(UUID channelId) throws UnauthenticatedException;

  void unregisterChannel(UUID channelId);

  SaslServerHandler createSaslHandler(ChannelAuthenticationScheme scheme) throws SaslException;

  void close() throws IOException;
}

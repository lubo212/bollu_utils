package com.bollu.goosefs.network.grpc.authentication;

import com.qcloud.cos.goosefs.grpc.SaslMessage;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AuthenticatedChannelServerDriver implements StreamObserver<SaslMessage> {
  private static final Logger LOG = LoggerFactory.getLogger(AuthenticatedChannelServerDriver.class);
  private static final UUID EMPTY_UUID = new UUID(0L, 0L);
  private StreamObserver<SaslMessage> mRequestObserver = null;
  private AuthenticationServer mAuthenticationServer;

  private UUID mChannelId = EMPTY_UUID;
  private String mChannelRef;
  private SaslServerHandler mSaslServerHandler = null;
  private volatile boolean mChannelAuthenticated = false;
  @Override
  public void onNext(SaslMessage value) {

  }

  @Override
  public void onError(Throwable t) {

  }

  @Override
  public void onCompleted() {

  }
}

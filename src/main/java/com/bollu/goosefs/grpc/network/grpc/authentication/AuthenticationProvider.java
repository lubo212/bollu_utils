package com.bollu.goosefs.grpc.network.grpc.authentication;

import javax.security.sasl.AuthenticationException;

public interface AuthenticationProvider {

  void authenticate(String user, String password) throws AuthenticationException;
}

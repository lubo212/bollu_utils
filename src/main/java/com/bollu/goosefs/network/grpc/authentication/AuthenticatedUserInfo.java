package com.bollu.goosefs.network.grpc.authentication;

import com.google.common.base.MoreObjects;

public class AuthenticatedUserInfo {

  private String mAuthorizedUserName;
  private String mConnectionUserName;
  private String mAuthMethod;

  public AuthenticatedUserInfo() {
    mAuthorizedUserName = null;
    mConnectionUserName = null;
    mAuthMethod = null;
  }

  public AuthenticatedUserInfo(String authorizedUserName) {
    this();
    mAuthorizedUserName = authorizedUserName;
  }

  public AuthenticatedUserInfo(String authorizedUserName, String connectionUserName) {
    this(authorizedUserName);
    mConnectionUserName = connectionUserName;
  }

  public AuthenticatedUserInfo(String authorizedUserName, String connectionUserName,
                               String authMethod) {
    this(authorizedUserName, connectionUserName);
    mAuthMethod = authMethod;
  }

  public String getAuthorizedUserName() {
    return mAuthorizedUserName;
  }

  /**
   * @return the connection user
   */
  public String getConnectionUserName() {
    return mConnectionUserName;
  }

  /**
   * @return authentication method
   */
  public String getAuthMethod() {
    return mAuthMethod;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("AuthorizedUser", mAuthorizedUserName)
        .add("ConnectionUser", mConnectionUserName)
        .add("AuthenticationMethod", mAuthMethod)
        .toString();
  }
}

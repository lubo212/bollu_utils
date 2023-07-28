package com.bollu.goosefs.grpc.network;

import com.bollu.goosefs.config.PropertyKey;

public enum ServiceType {

  MASTER_RAFT("GooseFS Master Raft service", PropertyKey.MASTER_HOSTNAME,
      PropertyKey.MASTER_HOSTNAME, PropertyKey.MASTER_EMBEDDED_JOURNAL_PORT),
  MASTER_RPC("GooseFS Master RPC service", PropertyKey.MASTER_HOSTNAME,
      PropertyKey.MASTER_BIND_HOST, PropertyKey.MASTER_RPC_PORT);

  private final String mServiceName;
  private final PropertyKey mHostNameKey;

  private final PropertyKey mBindHostKey;

  private final PropertyKey mPortKey;

  ServiceType(String serviceName, PropertyKey hostNameKey, PropertyKey bindHostKey,
              PropertyKey portKey) {
    mServiceName = serviceName;
    mHostNameKey = hostNameKey;
    mBindHostKey = bindHostKey;
    mPortKey = portKey;
  }

  public String getServiceName() {
    return mServiceName;
  }

  /**
   * Gets the key of connect hostname.
   *
   * @return key of connect hostname
   */
  public PropertyKey getHostNameKey() {
    return mHostNameKey;
  }

  /**
   * Gets the key of bind hostname.
   *
   * @return key of bind hostname
   */
  public PropertyKey getBindHostKey() {
    return mBindHostKey;
  }

  /**
   * Gets the key of service port.
   *
   * @return key of service port
   */
  public PropertyKey getPortKey() {
    return mPortKey;
  }

  /**
   * Gets the default port number on service.
   *
   * @return default port
   */
  public int getDefaultPort() {
    return Integer.parseInt(mPortKey.getDefaultValue());
  }

}

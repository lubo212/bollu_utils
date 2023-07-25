package com.bollu.utils.goosefs.config.network;

import com.bollu.utils.goosefs.config.PropertyKey;
import com.google.common.base.Preconditions;

import java.net.InetSocketAddress;

public enum ServiceType {
  FUSE_WEB("GooseFS FUSE Web service", PropertyKey.FUSE_WEB_HOSTNAME,
      PropertyKey.FUSE_WEB_BIND_HOST, PropertyKey.FUSE_WEB_PORT),
  /**
   * Job master Raft service (Netty). The bind and connect hosts are the same because the
   * underlying Raft implementation doesn't differentiate between bind and connect hosts.
   */
  JOB_MASTER_RAFT("GooseFS Job Master Raft service", PropertyKey.JOB_MASTER_HOSTNAME,
      PropertyKey.JOB_MASTER_HOSTNAME, PropertyKey.JOB_MASTER_EMBEDDED_JOURNAL_PORT),

  /**
   * Master Raft service (Netty). The bind and connect hosts are the same because the
   * underlying Raft implementation doesn't differentiate between bind and connect hosts.
   */
  MASTER_RAFT("GooseFS Worker Raft service", PropertyKey.MASTER_HOSTNAME,
      PropertyKey.MASTER_HOSTNAME, PropertyKey.MASTER_EMBEDDED_JOURNAL_PORT),

  /**
   * Job master RPC service (gRPC).
   */
  JOB_MASTER_RPC("GooseFS Job Manager Master RPC service", PropertyKey.JOB_MASTER_HOSTNAME,
      PropertyKey.JOB_MASTER_BIND_HOST, PropertyKey.JOB_MASTER_RPC_PORT),

  /**
   * Job master web service (Jetty).
   */
  JOB_MASTER_WEB("GooseFS Job Manager Master Web service", PropertyKey.JOB_MASTER_WEB_HOSTNAME,
      PropertyKey.JOB_MASTER_WEB_BIND_HOST, PropertyKey.JOB_MASTER_WEB_PORT),

  /**
   * Job worker RPC service (gRPC).
   */
  JOB_WORKER_RPC("GooseFS Job Manager Worker RPC service", PropertyKey.JOB_WORKER_HOSTNAME,
      PropertyKey.JOB_WORKER_BIND_HOST, PropertyKey.JOB_WORKER_RPC_PORT),

  /**
   * Job master web service (Jetty).
   */
  JOB_WORKER_WEB("GooseFS Job Manager Worker Web service", PropertyKey.WORKER_WEB_HOSTNAME,
      PropertyKey.JOB_WORKER_WEB_BIND_HOST, PropertyKey.JOB_WORKER_WEB_PORT),

  /**
   * Master RPC service (gRPC).
   */
  MASTER_RPC("GooseFS Worker RPC service", PropertyKey.MASTER_HOSTNAME,
      PropertyKey.MASTER_BIND_HOST, PropertyKey.MASTER_RPC_PORT),

  /**
   * Master service RPC service (gRPC).
   */
  MASTER_SERVICE_RPC("GooseFS Master Service RPC service", PropertyKey.MASTER_HOSTNAME,
      PropertyKey.MASTER_BIND_HOST, PropertyKey.MASTER_SERVICE_RPC_PORT),

  /**
   * Master web service (Jetty).
   */
  MASTER_WEB("GooseFS Worker Web service", PropertyKey.MASTER_WEB_HOSTNAME,
      PropertyKey.MASTER_WEB_BIND_HOST, PropertyKey.MASTER_WEB_PORT),

  /**
   * Worker RPC service (gRPC).
   */
  WORKER_RPC("GooseFS Worker RPC service", PropertyKey.WORKER_HOSTNAME,
      PropertyKey.WORKER_BIND_HOST, PropertyKey.WORKER_RPC_PORT),

  /**
   * Worker web service (Jetty).
   */
  WORKER_WEB("GooseFS Worker Web service", PropertyKey.WORKER_WEB_HOSTNAME,
      PropertyKey.WORKER_WEB_BIND_HOST, PropertyKey.WORKER_WEB_PORT),

  /**
   * Proxy web service (Jetty).
   */
  PROXY_WEB("GooseFS Proxy Web service", PropertyKey.PROXY_WEB_HOSTNAME,
      PropertyKey.PROXY_WEB_BIND_HOST, PropertyKey.PROXY_WEB_PORT),
  ;


  // service name
  private final String mServiceName;

  // the key of connect hostname
  private final PropertyKey mHostNameKey;

  // the key of bind hostname
  private final PropertyKey mBindHostKey;

  // the key of service port
  private final PropertyKey mPortKey;

  ServiceType(String serviceName, PropertyKey hostNameKey, PropertyKey bindHostKey,
              PropertyKey portKey) {
    mServiceName = serviceName;
    mHostNameKey = hostNameKey;
    mBindHostKey = bindHostKey;
    mPortKey = portKey;
  }

  /**
   * Gets service name.
   *
   * @return service name
   */
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
   * Checks if the given port is valid.
   *
   * @param port the port to check
   */
  public static void assertValidPort(final int port) {
    Preconditions.checkArgument(port < 65536, "Port must be less than 65536");
    Preconditions.checkArgument(port >= 0, "Port must be non-negative");
  }

  /**
   * Checks if the given port in the address is valid.
   *
   * @param address the {@link InetSocketAddress} with the port to check
   */
  public static void assertValidPort(final InetSocketAddress address) {
    assertValidPort(address.getPort());
  }

  /**
   * Helper method to get the {@link InetSocketAddress} address for client to communicate with the
   * service.
   *
   * @param service the service name used to connect
   * @param conf the configuration to use for looking up the connect address
   * @return the service address that a client (typically outside the service machine) uses to
   *         communicate with service.
   */
  public static InetSocketAddress getConnectAddress(ServiceType service,
                                                    GooseFSConfiguration conf) {
    return InetSocketAddress.createUnresolved(getConnectHost(service, conf),
        getPort(service, conf));
  }
}

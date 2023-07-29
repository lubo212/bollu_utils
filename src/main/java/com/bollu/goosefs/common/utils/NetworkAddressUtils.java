package com.bollu.goosefs.common.utils;

import com.bollu.goosefs.config.Configuration;
import com.bollu.goosefs.config.PropertyKey;
import com.bollu.goosefs.network.ServiceType;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NetworkAddressUtils {
  private static final Logger LOG = LoggerFactory.getLogger(NetworkAddressUtils.class);

  public static final String WILDCARD_ADDRESS = "0.0.0.0";

  public static final boolean WINDOWS = OSUtils.isWindows();

  private static String sLocalHost;
  private static String sLocalHostMetricName;
  private static String sLocalIP;

  private NetworkAddressUtils() {
  }

  public static void assertValidPort(final int port) {
    Preconditions.checkArgument(port < 65536, "Port must be less than 65536");
    Preconditions.checkArgument(port >= 0, "Port must be non-negative");
  }

  public static void assertValidPort(final InetSocketAddress address) {
    assertValidPort(address.getPort());
  }

  public static InetSocketAddress getConnectAddress(ServiceType service,
                                                    Configuration conf) {
    return InetSocketAddress.createUnresolved(getConnectHost(service, conf),
        getPort(service, conf));
  }

  public static String getConnectHost(ServiceType service, Configuration conf) {
    if (conf.isSet(service.getHostNameKey())) {
      String connectHost = conf.get(service.getHostNameKey());
      if (!connectHost.isEmpty() && !connectHost.equals(WILDCARD_ADDRESS)) {
        return connectHost;
      }
    }

    if (conf.isSet(service.getBindHostKey())) {
      String bindHost = conf.get(service.getBindHostKey());
      if (!bindHost.isEmpty() && !bindHost.equals(WILDCARD_ADDRESS)) {
        return bindHost;
      }
    }

    if (conf.getBoolean(PropertyKey.NETWORK_IP_ADDRESS_USED)) {
      return getLocalIpAddress((int) conf.getMs(PropertyKey.NETWORK_HOST_RESOLUTION_TIMEOUT_MS));
    }
    return getLocalHostName((int) conf.getMs(PropertyKey.NETWORK_HOST_RESOLUTION_TIMEOUT_MS));
  }

  public static int getPort(ServiceType service, Configuration conf) {
    return conf.getInt(service.getPortKey());
  }

  public static InetSocketAddress getBindAddress(ServiceType service, Configuration conf) {
    int port = getPort(service, conf);
    assertValidPort(port);
    return new InetSocketAddress(getBindHost(service, conf), getPort(service, conf));
  }

  public static String getBindHost(ServiceType service, Configuration conf) {
    if (conf.isSet(service.getBindHostKey()) && !conf.get(service.getBindHostKey())
        .isEmpty()) {
      return conf.get(service.getBindHostKey());
    } else {
      return getLocalHostName((int) conf.getMs(PropertyKey.NETWORK_HOST_RESOLUTION_TIMEOUT_MS));
    }
  }

  public static String getClientHostName(Configuration conf) {
    if (conf.isSet(PropertyKey.USER_HOSTNAME)) {
      return conf.get(PropertyKey.USER_HOSTNAME);
    }
    return getLocalHostName((int) conf.getMs(PropertyKey.NETWORK_HOST_RESOLUTION_TIMEOUT_MS));
  }

  public static synchronized String getLocalHostName(int timeoutMs) {
    if (sLocalHost != null) {
      return sLocalHost;
    }

    try {
      sLocalHost = InetAddress.getByName(getLocalIpAddress(timeoutMs)).getCanonicalHostName();
      return sLocalHost;
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  public static synchronized String getLocalIpAddress(int timeoutMs) {
    if (sLocalIP != null) {
      return sLocalIP;
    }

    try {
      InetAddress address = InetAddress.getLocalHost();
      LOG.debug("address: {} isLoopbackAddress: {}, with host {} {}", address,
          address.isLoopbackAddress(), address.getHostAddress(), address.getHostName());
      if (!isValidAddress(address, timeoutMs)) {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        if (!WINDOWS) {
          List<NetworkInterface> netIFs = Collections.list(networkInterfaces);
          Collections.reverse(netIFs);
          networkInterfaces = Collections.enumeration(netIFs);
        }

        while (networkInterfaces.hasMoreElements()) {
          NetworkInterface ni = networkInterfaces.nextElement();
          Enumeration<InetAddress> addresses = ni.getInetAddresses();
          while (addresses.hasMoreElements()) {
            address = addresses.nextElement();
            if (isValidAddress(address, timeoutMs)) {
              sLocalIP = address.getHostAddress();
              return sLocalIP;
            }
          }
        }
      }

      sLocalIP = address.getHostAddress();
      return sLocalIP;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static synchronized String getLocalHostMetricName(int timeoutMs) {
    if (sLocalHostMetricName != null) {
      return sLocalHostMetricName;
    }
    sLocalHostMetricName = getLocalHostName(timeoutMs).replace('.', '_');
    return sLocalHostMetricName;
  }

  private static boolean isValidAddress(InetAddress address, int timeoutMs) throws IOException {
    return !address.isAnyLocalAddress() && !address.isLinkLocalAddress()
        && !address.isLoopbackAddress() && address.isReachable(timeoutMs)
        && (address instanceof Inet4Address);
  }

  public static boolean isServing(String host, int port) {
    if (port < 0) {
      return false;
    }
    try {
      Socket socket = new Socket(host, port);
      socket.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static String resolveIpAddress(String hostname) throws UnknownHostException {
    Preconditions.checkNotNull(hostname, "hostname");
    Preconditions.checkArgument(!hostname.isEmpty(),
        "Cannot resolve IP address for empty hostname");
    return InetAddress.getByName(hostname).getHostAddress();
  }

  /**
   * Gets FQDN(Full Qualified Domain Name) from Java representations of network address, except
   * String representation which should be handled by  which will
   * handle the situation where hostname is null.
   *
   * @param addr the input network address representation, can not be null
   * @return the resolved FQDN host name
   */
  public static String getFqdnHost(InetSocketAddress addr) {
    Preconditions.checkNotNull(addr.getAddress(), "the address of " + addr + " is invalid.");
    return addr.getAddress().getCanonicalHostName();
  }

  public static InetSocketAddress parseInetSocketAddress(String address) throws IOException {
    if (address == null) {
      return null;
    }
    String[] strArr = address.split(":");
    if (strArr.length != 2) {
      throw new IOException("Invalid InetSocketAddress " + address);
    }
    return InetSocketAddress.createUnresolved(strArr[0], Integer.parseInt(strArr[1]));
  }

  public static boolean containsLocalIp(List<InetSocketAddress> clusterAddresses,
                                        Configuration conf) {
    String localAddressIp = getLocalIpAddress((int) conf.getMs(PropertyKey
        .NETWORK_HOST_RESOLUTION_TIMEOUT_MS));
    for (InetSocketAddress addr : clusterAddresses) {
      String clusterNodeIp;
      try {
        clusterNodeIp = InetAddress.getByName(addr.getHostName()).getHostAddress();
        if (clusterNodeIp.equals(localAddressIp)) {
          return true;
        }
      } catch (UnknownHostException e) {
        LOG.error("Get raft cluster node ip by hostname({}) failed",
            addr.getHostName(), e);
      }
    }
    return false;
  }

}

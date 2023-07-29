package com.bollu.goosefs.network.util;

import com.bollu.goosefs.common.utils.NetworkAddressUtils;
import com.bollu.goosefs.config.Configuration;
import com.bollu.goosefs.config.PropertyKey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkCommonUtils {

  public enum ProcessType {
    CLIENT,
    MASTER,
    PROXY,
    WORKER;
  }

  public static final AtomicReference<ProcessType> PROCESS_TYPE = new AtomicReference<>(ProcessType.CLIENT);
  public static String getLocalNodeName(Configuration conf) {
    switch (PROCESS_TYPE.get()) {
      case CLIENT:
        if (conf.isSet(PropertyKey.USER_HOSTNAME)) {
          return conf.get(PropertyKey.USER_HOSTNAME);
        }
        break;
      case MASTER:
        if (conf.isSet(PropertyKey.MASTER_HOSTNAME)) {
          return conf.get(PropertyKey.MASTER_HOSTNAME);
        }
        break;
      case WORKER:
        if (conf.isSet(PropertyKey.WORKER_HOSTNAME)) {
          return conf.get(PropertyKey.WORKER_HOSTNAME);
        }
        break;
      default:
        break;
    }
    return NetworkAddressUtils.getLocalHostName((int) conf.getMs(PropertyKey.NETWORK_HOST_RESOLUTION_TIMEOUT_MS));
  }

  public static boolean isHaMode(Configuration conf) {
    return conf.getBoolean(PropertyKey.ZOOKEEPER_ENABLED) || getMasterRpcAddresses(conf).size() > 1;
  }

  public static List<InetSocketAddress> getMasterRpcAddresses(Configuration conf) {
    if (conf.isSet(PropertyKey.MASTER_RPC_ADDRESSES)) {
      return parseInetSocketAddresses(conf.getList(PropertyKey.MASTER_RPC_ADDRESSES, ","));
    }
    return new ArrayList<>();
  }

  private static List<InetSocketAddress> parseInetSocketAddresses(List<String> addresses) {
    List<InetSocketAddress> inetSocketAddresses = new ArrayList<>(addresses.size());
    for (String address : addresses) {
      try {
        inetSocketAddresses.add(NetworkAddressUtils.parseInetSocketAddress(address));
      } catch (IOException e) {
        throw new IllegalArgumentException("Failed to parse host:port: " + address, e);
      }
    }
    return inetSocketAddresses;
  }


}

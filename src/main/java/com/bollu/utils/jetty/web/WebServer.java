package com.bollu.utils.jetty.web;

import com.bollu.utils.jetty.web.servlet.StacksServlet;
import com.google.common.base.Preconditions;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 基于Jetty快速启动一个Java Web服务
 */
@NotThreadSafe
public class WebServer {

  private static final Logger LOG = LoggerFactory.getLogger(WebServer.class);
  public static final String SEPARATOR = "/";

  private final Server mServer;
  private final String mServiceName;
  private final InetSocketAddress mAddress;
  private final ServerConnector mServerConnector;
  protected final ServletContextHandler mServletContextHandler;

  public WebServer(String serviceName, InetSocketAddress address, int webThreadCount) {
    Preconditions.checkNotNull(serviceName, "Service name cannot be null");
    Preconditions.checkNotNull(address, "Server address cannot be null");

    mAddress = address;
    mServiceName = serviceName;

    mServer = new Server(new QueuedThreadPool(webThreadCount * 2 + 100, webThreadCount * 2 + 1));

    mServerConnector = new ServerConnector(mServer);
    mServerConnector.setPort(mAddress.getPort());
    mServerConnector.setHost(mAddress.getAddress().getHostAddress());
    mServerConnector.setReuseAddress(true);
    mServer.addConnector(mServerConnector);

    // Open the connector here so we can resolve the port if we are selecting a free port.
    try {
      mServerConnector.open();
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to listen on address %s for web server %s", mAddress, mServiceName),
          e);
    }

    System.setProperty("org.apache.jasper.compiler.disablejsr199", "false");
    mServletContextHandler = new ServletContextHandler(ServletContextHandler.SECURITY
        | ServletContextHandler.NO_SESSIONS);
    mServletContextHandler.setContextPath(SEPARATOR);

    mServletContextHandler.addServlet(StacksServlet.class, "/stacks");
    HandlerList handlers = new HandlerList();
    //添加handler
    handlers.setHandlers(new Handler[] {mServletContextHandler, new DefaultHandler()});

    mServer.setHandler(handlers);
  }

  public void addHandler(AbstractHandler handler) {
    HandlerList handlers = new HandlerList();
    handlers.addHandler(handler);
    for (Handler h : mServer.getHandlers()) {
      handlers.addHandler(h);
    }
    mServer.setHandler(handlers);
  }

  /**
   * @param handler to use
   */
  public void setHandler(AbstractHandler handler) {
    mServer.setHandler(handler);
  }

  public Server getServer() {
    return mServer;
  }

  public String getBindHost() {
    String bindHost = mServerConnector.getHost();
    return bindHost == null ? "0.0.0.0" : bindHost;
  }

  public int getLocalPort() {
    return mServerConnector.getLocalPort();
  }

  public void stop() throws Exception {
    // close all connectors and release all binding ports
    for (Connector connector : mServer.getConnectors()) {
      connector.stop();
    }

    mServer.stop();
  }

  /**
   * Starts the web server.
   */
  public void start() {
    try {
      LOG.info("{} starting @ {}", mServiceName, mAddress);
      mServer.start();
      LOG.info("{} started @ {}", mServiceName, mAddress);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}

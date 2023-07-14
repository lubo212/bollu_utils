package com.bollu.utils.jetty.web;

import java.net.InetSocketAddress;

public class TestMain {
  public static void main(String[] args) throws InterruptedException {
    WebServer webServer = new WebServer("myTest", new InetSocketAddress("127.0.0.1", 8891), 1);
    webServer.addHandler(new HelloWorldServlet().getHandler());
    webServer.start();

    Thread.sleep(100000);
  }
}

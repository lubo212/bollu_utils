package com.bollu.utils.goosefs.jetty.web.servlet;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloWorldServlet extends HttpServlet {

  public static final String SERVLET_PATH = "/test";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setContentType("text/plain; charset=UTF-8");
    resp.getOutputStream().write("hello world".getBytes());
  }

  public ServletContextHandler getHandler() {
    ServletContextHandler handler = new ServletContextHandler();
    handler.setContextPath(SERVLET_PATH);
    handler.addServlet(new ServletHolder(this), "/");
    return handler;
  }
}

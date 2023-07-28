package com.bollu.goosefs.jetty.web.servlet;

import com.bollu.goosefs.common.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;

public class StacksServlet extends HttpServlet {
  private static final long serialVersionUID = 4190506509049119126L;
  private static final Logger LOG = LoggerFactory.getLogger(StacksServlet.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("text/plain; charset=UTF-8");
    try (PrintStream out = new PrintStream(
        resp.getOutputStream(), false, "UTF-8")) {
      ThreadUtils.printThreadInfo(out, "");
    }
    ThreadUtils.logThreadInfo(LOG, "jsp requested", 1);
  }
}

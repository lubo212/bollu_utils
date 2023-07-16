package com.bollu.utils.jetty.web.servlet;

import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class PrometheusMetricsServlet {
  private static final String SERVLET_PATH = "/metrics";

  private CollectorRegistry mCollectorRegistry;

  public PrometheusMetricsServlet(MetricRegistry registry) {
    mCollectorRegistry = new CollectorRegistry();
    mCollectorRegistry.register(new DropwizardExports(registry));
  }

  public ServletContextHandler getHandler() {
    ServletContextHandler contextHandler = new ServletContextHandler();
    contextHandler.setContextPath(SERVLET_PATH);
    contextHandler.addServlet(new ServletHolder(new MetricsServlet(mCollectorRegistry)), "/");
    return contextHandler;
  }

}

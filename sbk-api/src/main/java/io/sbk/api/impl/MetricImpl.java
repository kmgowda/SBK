/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusRenameFilter;
import io.sbk.api.Metric;
import io.sbk.api.Parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Class for Implementing Prometheus MeterRegistry for metrics.
 */
public class MetricImpl implements Metric {
    private final static int DEFAULT_PORT = 8080;
    private final static String DEFAULT_CONTEXT = "/metrics";
    private final static String DEFAULT_FULL_CONTEXT = DEFAULT_PORT + DEFAULT_CONTEXT;
    private  int port;
    private String context;
    private boolean disabled;

    @Override
    public void addArgs(final Parameters params) {
        params.addOption("context", true, "Prometheus Metric context;" +
                "default context: " + DEFAULT_FULL_CONTEXT + "; 'no' disables the  metrics");
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        final String fullContext =  params.getOptionValue("context", DEFAULT_FULL_CONTEXT);
        if (fullContext.equalsIgnoreCase("no")) {
            disabled = true;
        } else {
            disabled = false;
            String[] str = fullContext.split("/", 2);
            port = Integer.parseInt(str[0]);
            if (str.length == 2 && str[1] != null) {
                context = "/" + str[1];
            } else {
                context = DEFAULT_CONTEXT;
            }
        }
    }

    @Override
    public MeterRegistry createMetric(final Parameters params) throws RuntimeException {
        if (disabled) {
            return  null;
        }
        final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheusRegistry.config().meterFilter(new PrometheusRenameFilter());
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext(context, httpExchange -> {
                String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prometheusRegistry;
    }
}


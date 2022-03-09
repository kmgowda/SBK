/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.logger.impl;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusRenameFilter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Class PrometheusServer.
 */
public final class PrometheusServer extends CompositeMeterRegistry {
    final private int port;
    final private String context;
    final private PrometheusMeterRegistry prometheusRegistry;
    final private HttpServer server;

    /**
     * Constructor PrometheusServer initializing values.
     *
     * @param port          int
     * @param context       String
     * @throws IOException  If it occurs.
     */
    public PrometheusServer(int port, String context) throws IOException {
        super();
        this.port = port;
        this.context = context;
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheusRegistry.config().meterFilter(new PrometheusRenameFilter());
        /*
        JMX Meter Registry is disabled
        this.add(new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM));
         */
        this.add(prometheusRegistry);
        this.server = createHttpServer();
    }

    /**
     * Starts this server in a new background thread.
     * The background thread inherits the priority,
     * thread group and context class loader of the caller.
     *
     * @throws IOException If it occurs
     */
    public void start() throws IOException {
        server.start();
    }


    /**
     * Stops this server by closing the listening socket and disallowing any new exchanges from being processed.
     *
     * @throws IOException If it occurs
     */
    public void stop() throws IOException {
        server.stop(0);
    }

    private @NotNull HttpServer createHttpServer() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(context, httpExchange -> {
            String response = prometheusRegistry.scrape();
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        return server;
    }
}

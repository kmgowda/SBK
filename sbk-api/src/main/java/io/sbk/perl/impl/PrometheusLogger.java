/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl.impl;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusRenameFilter;
import io.sbk.perl.MetricsConfig;
import io.sbk.perl.Print;
import io.sbk.perl.Time;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class PrometheusLogger implements Print {
    final private MetricsLogger metricsLogger;
    final private HttpServer server;

    public PrometheusLogger(String header, String storageName, String action, Time time,
                             int writers, int readers, double[] percentiles, MetricsConfig config) throws IOException {
        final CompositeMeterRegistry compositeRegistry = Metrics.globalRegistry;
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheusRegistry.config().meterFilter(new PrometheusRenameFilter());
        compositeRegistry.add(new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM));
        compositeRegistry.add(prometheusRegistry);
        metricsLogger = new MetricsLogger(header, storageName, action, time, writers, readers, percentiles,
                config.latencyTimeUnit, compositeRegistry);
        server = createHttpServer(prometheusRegistry, config);
    }

    private HttpServer createHttpServer(PrometheusMeterRegistry prometheusRegistry, MetricsConfig config) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(config.port), 0);
        server.createContext(config.context, httpExchange -> {
            String response = prometheusRegistry.scrape();
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        return server;
    }

    public void start() throws IOException  {
        server.start();
    }


    public void stop() throws  IOException {
        metricsLogger.close();
        server.stop(0);
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, long maxLatency,
                      long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        metricsLogger.print(bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
    }

}

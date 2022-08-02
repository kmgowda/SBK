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

import io.time.Time;
import io.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Class PrometheusMetricsServer.
 */
public non-sealed class PrometheusMetricsServer extends PrintMetrics {
    final private PrometheusServer server;

    private PrometheusMetricsServer(String header, double[] percentiles, Time time,
                                    @NotNull TimeUnit latencyTimeUnit, PrometheusServer server) {
        super(header, percentiles, time, latencyTimeUnit, server);
        this.server = server;
    }

    /**
     * Constructor PrometheusMetricsServer initializing all values.
     *
     * @param header                String
     * @param percentiles           double[]
     * @param time                  Time
     * @param latencyTimeUnit       NotNull TimeUnit
     * @param port                  int
     * @param context               String
     * @param tags                  String... common tags
     * @throws IOException If it occurs.
     */
    public PrometheusMetricsServer(String header, double[] percentiles, Time time,
                                   @NotNull TimeUnit latencyTimeUnit, int port, String context, String... tags) throws IOException {
        this(header, percentiles, time, latencyTimeUnit, new PrometheusServer(port, context, tags));
    }


    /**
     * Starts this server in a new background thread.
     * The background thread inherits the priority,
     * thread group and context class loader of the caller.
     *
     * @throws IOException If it occurs.
     */
    public void start() throws IOException {
        server.start();
    }

    /**
     * calls close method from it's super class
     * and Stops this server by closing the listening socket and
     * disallowing any new exchanges from being processed.
     *
     * @throws IOException If it occurs
     */
    public void stop() throws IOException {
        super.close();
        server.stop();
    }
}

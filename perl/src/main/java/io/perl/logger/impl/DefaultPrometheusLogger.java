/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */


package io.perl.logger.impl;

import io.micrometer.core.instrument.Tag;
import io.time.Time;
import io.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Prometheus-enabled results logger.
 *
 * <p>This logger extends {@link DefaultLogger} to publish metrics to a
 * locally-created Prometheus metrics server in addition to the textual
 * output. It instantiates a {@link PrometheusMetricsServer} that registers
 * Micrometer meters and exposes them via an HTTP endpoint.
 *
 * <p>Typical usage: construct with the required percentiles and a running
 * {@link io.time.Time} instance, call {@link #start()} to begin serving
 * metrics and call {@link #stop()} to shut the server down gracefully.
 */
public class DefaultPrometheusLogger extends DefaultLogger {
    private final PrometheusMetricsServer server;

    /**
     * Constructor DefaultPrometheusLogger pass values to super class and initialize server.
     *
     * @param header                    String
     * @param percentiles               double[]
     * @param latencyTimeUnit           NotNull TimeUnit
     * @param minLatency                long
     * @param maxLatency                long
     * @param time                      Time
     * @param port                      int
     * @param context                   String
     * @param tags                      Common tags
     * @throws IOException If it occurs.
     */
    public DefaultPrometheusLogger(String header, double[] percentiles,
                                   @NotNull TimeUnit latencyTimeUnit,
                                   long minLatency, long maxLatency,
                                   Time time, int port, String context, Iterable<Tag> tags) throws IOException {
        super(header, percentiles, latencyTimeUnit, minLatency, maxLatency);
        server = new PrometheusMetricsServer(header, percentiles, time, latencyTimeUnit, port, context, tags);
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
     * calls close method from it's super class and Stops this server by closing
     * the listening socket and disallowing any new exchanges from being processed.
     *
     * @throws IOException If it occurs.
     */
    public void stop() throws IOException {
        server.stop();
    }

    @Override
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts) {
        super.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
        server.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency,
                maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
    }

}

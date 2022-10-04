/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.logger.impl;

import io.sbm.logger.CountConnections;
import io.sbk.logger.MetricsConfig;
import io.sbk.logger.impl.PrometheusRWMetricsServer;
import io.time.Time;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class RamMetricsPrometheusServer.
 */
public final class SbmMetricsPrometheusServer extends PrometheusRWMetricsServer implements CountConnections {
    final private AtomicInteger connections;
    final private AtomicInteger maxConnections;

    /**
     * Constructor RamMetricsPrometheusServer initializing {@link #connections} and {@link #maxConnections}
     * and calling its super class.
     *
     * @param header            String
     * @param action            String
     * @param storageName       storageName
     * @param percentiles       double[]
     * @param time              Time
     * @param config            MetricsConfig
     * @throws IOException If it Occurs.
     */
    public SbmMetricsPrometheusServer(String header, String action, String storageName,
                                      double[] percentiles, Time time, MetricsConfig config) throws IOException {
        super(header, action, storageName, percentiles, time, config);
        final String name = rwMetricPrefix + "_Connections";
        final String maxName = rwMetricPrefix + "_Max_Connections";
        this.connections = this.registry.gauge(name, new AtomicInteger());
        this.maxConnections = this.registry.gauge(maxName, new AtomicInteger());
    }

    @Override
    public void incrementConnections() {
        connections.incrementAndGet();
        maxConnections.incrementAndGet();
    }

    @Override
    public void decrementConnections() {
        connections.decrementAndGet();
    }
}

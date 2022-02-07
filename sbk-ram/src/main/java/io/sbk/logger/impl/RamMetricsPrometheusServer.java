/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import io.sbk.logger.CountConnections;
import io.sbk.logger.MetricsConfig;
import io.time.Time;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public final class RamMetricsPrometheusServer extends PrometheusRWMetricsServer implements CountConnections {
    final private AtomicInteger connections;
    final private AtomicInteger maxConnections;

    public RamMetricsPrometheusServer(String header, String action, double[] percentiles, Time time,
                                      MetricsConfig config) throws IOException {
        super(header, action, percentiles, time, config);
        final String name = metricPrefix + "_Connections";
        final String maxName = metricPrefix + "_Max_Connections";
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

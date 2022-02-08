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

public non-sealed class PrometheusMetricsServer extends PrintMetrics {
    final private PrometheusServer server;

    private PrometheusMetricsServer(String header, double[] percentiles, Time time,
                                    @NotNull TimeUnit latencyTimeUnit, PrometheusServer server) {
        super(header, percentiles, time, latencyTimeUnit, server);
        this.server = server;
    }

    public PrometheusMetricsServer(String header, double[] percentiles, Time time,
                                   @NotNull TimeUnit latencyTimeUnit, int port, String context) throws IOException {
        this(header, percentiles, time, latencyTimeUnit, new PrometheusServer(port, context));
    }


    public void start() throws IOException {
        server.start();
    }

    public void stop() throws IOException {
        super.close();
        server.stop();
    }
}

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

import io.sbk.perl.MetricsConfig;
import io.sbk.perl.Print;
import io.sbk.perl.PrometheusServer;
import io.sbk.perl.Time;

import java.io.IOException;

public class RWPrometheusMetricsServer extends PrometheusServer implements Print {
    final private MetricsLogger metricsLogger;

    public RWPrometheusMetricsServer(String header, String action, double[] percentiles, Time time,
                                     MetricsConfig config, int readers, int writers) throws IOException {
        super(config.port, config.context);
        metricsLogger = new RWMetricsLogger(header, action, percentiles, time, config.latencyTimeUnit,
                this, readers, writers);
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, long maxLatency,
                      long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        metricsLogger.print(bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
    }
}

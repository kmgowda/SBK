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

import io.perl.logger.impl.PrometheusMetricsServer;
import io.sbk.config.Config;
import io.sbk.logger.CountRW;
import io.sbk.logger.MetricsConfig;
import io.time.Time;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class PrometheusRWMetricsServer extends PrometheusMetricsServer implements CountRW {
    final private AtomicInteger writers;
    final private AtomicInteger readers;
    final private AtomicInteger maxWriters;
    final private AtomicInteger maxReaders;

    public  PrometheusRWMetricsServer(String header, String action, String className, double[] percentiles, Time time,
                                        MetricsConfig config) throws IOException {
        super(header.toUpperCase()+" "+action, percentiles, time,
                config.latencyTimeUnit, config.port, config.context, Config.CLASS_OPTION, className);
        final String writersName = metricPrefix + "_Writers";
        final String readersName = metricPrefix + "_Readers";
        final String maxWritersName = metricPrefix + "_Max_Writers";
        final String maxReadersName = metricPrefix + "_Max_Readers";
        this.writers = this.registry.gauge(writersName, new AtomicInteger());
        this.readers = this.registry.gauge(readersName, new AtomicInteger());
        this.maxWriters = this.registry.gauge(maxWritersName, new AtomicInteger());
        this.maxReaders = this.registry.gauge(maxReadersName, new AtomicInteger());
    }

    public void incrementWriters() {
        writers.incrementAndGet();
        maxWriters.incrementAndGet();
    }

    public void decrementWriters() {
        writers.decrementAndGet();
    }

    public void setWriters(int val) {
        writers.set(val);
        maxWriters.set(Math.max(writers.get(), maxWriters.get()));
    }

    public void setMaxWriters(int val) {
        maxWriters.set(val);
    }

    public void incrementReaders() {
        readers.incrementAndGet();
        maxReaders.incrementAndGet();
    }

    public void decrementReaders() {
        readers.decrementAndGet();
    }

    public void setReaders(int val) {
        readers.set(val);
        maxReaders.set(Math.max(readers.get(), maxReaders.get()));
    }

    public void setMaxReaders(int val) {
        maxReaders.set(val);
    }

}

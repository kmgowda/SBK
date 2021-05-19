/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;


import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.sbk.api.RWCount;
import io.sbk.perl.Time;
import io.sbk.perl.TimeUnit;
import io.sbk.perl.impl.MetricsLogger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for recoding/printing benchmark results of Readers and Writers
 * on micrometer Composite Meter Registry.
 */
public class RWMetricsLogger extends MetricsLogger implements RWCount {
    final private AtomicInteger writers;
    final private AtomicInteger readers;
    final private AtomicInteger maxWriters;
    final private AtomicInteger maxReaders;

    public RWMetricsLogger(String header, String action, double[] percentiles, Time time, TimeUnit latencyTimeUnit,
                         CompositeMeterRegistry compositeRegistry) {
       super(header, action,  percentiles, time, latencyTimeUnit, compositeRegistry);
       final String writersName = metricPrefix + "_Writers";
       final String readersName = metricPrefix + "_Readers";
       final String maxWritersName = metricPrefix + "_Max_Writers";
       final String maxReadersName = metricPrefix + "_Max_Readers";
       this.writers = this.registry.gauge(writersName, new AtomicInteger());
       this.readers = this.registry.gauge(readersName, new AtomicInteger());
       this.maxWriters = this.registry.gauge(maxWritersName, new AtomicInteger());
       this.maxReaders = this.registry.gauge(maxReadersName, new AtomicInteger());
    }

    @Override
    public void incrementWriters(int val) {
        this.writers.set(writers.get() + val);
        this.maxWriters.set(maxWriters.get() + val);
    }

    @Override
    public void decrementWriters(int val) {
        this.writers.set(Math.max(writers.get()-val, 0));
    }

    @Override
    public void incrementReaders(int val) {
        this.readers.set(readers.get() + val);
        this.maxReaders.set(maxReaders.get() + val);
    }

    @Override
    public void decrementReaders(int val) {
        this.readers.set(Math.max(readers.get() - val, 0));
    }
}
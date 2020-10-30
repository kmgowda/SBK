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

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for recoding/printing benchmark results on micrometer Composite Meter Registry.
 */
public class MetricsLogger extends SystemResultLogger {
    final private Counter bytes;
    final private Counter records;
    final private Counter lowerDiscard;
    final private Counter higherDiscard;
    final private AtomicDouble mbPsec;
    final private AtomicDouble recsPsec;
    final private AtomicDouble avgLatency;
    final private AtomicInteger maxLatency;
    final private AtomicInteger[] percentileGauges;
    final private MeterRegistry registry;

    public MetricsLogger(String header, String prefix, double[] percentiles, int writers, int readers,
                         CompositeMeterRegistry compositeRegistry) {
        super(prefix, percentiles);
        final String metricPrefix = header.replace(" ", "_").toUpperCase() +
                "_" + prefix.replace(" ", "_");
        final String metricUnit = unit.replace(" ", "_");
        final String bytesName = metricPrefix + "_Bytes";
        final String recordsName = metricPrefix + "_Records";
        final String mbPsecName = metricPrefix + "_MBPerSec";
        final String recsPsecName = metricPrefix + "_RecordsPerSec";
        final String avgLatencyName = metricPrefix + "_" + metricUnit + "_AvgLatency";
        final String maxLatencyName = metricPrefix + "_" + metricUnit + "_MaxLatency";
        final String lowerDiscardName = metricPrefix + "_LowerDiscardedLatencyRecords";
        final String higherDiscardName = metricPrefix + "_HigherDiscardLatencyRecords";
        final String writersName = metricPrefix + "_Writers";
        final String readersName = metricPrefix + "_Readers";
        this.registry = compositeRegistry;
        this.registry.gauge(writersName, writers);
        this.registry.gauge(readersName, readers);
        this.bytes = this.registry.counter(bytesName);
        this.records = this.registry.counter(recordsName);
        this.lowerDiscard = this.registry.counter(lowerDiscardName);
        this.higherDiscard = this.registry.counter(higherDiscardName);
        this.mbPsec = this.registry.gauge(mbPsecName, new AtomicDouble());
        this.recsPsec = this.registry.gauge(recsPsecName, new AtomicDouble());
        this.avgLatency = this.registry.gauge(avgLatencyName, new AtomicDouble());
        this.maxLatency = this.registry.gauge(maxLatencyName, new AtomicInteger());
        this.percentileGauges = new AtomicInteger[this.percentiles.length];
        for (int i = 0; i < this.percentiles.length; i++) {
            this.percentileGauges[i] = this.registry.gauge(metricPrefix + "_" + metricUnit + "_" + format.format(percentiles[i]),
                    new AtomicInteger());

        }
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, int maxLatency,
               long lowerDiscard, long higherDiscard, int[] percentileValues) {
        super.print( bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                lowerDiscard, higherDiscard, percentileValues);
        this.bytes.increment(bytes);
        this.records.increment(records);
        this.lowerDiscard.increment(lowerDiscard);
        this.higherDiscard.increment(higherDiscard);
        this.recsPsec.set(recsPerSec);
        this.mbPsec.set(mbPerSec);
        this.avgLatency.set(avgLatency);
        this.maxLatency.set(maxLatency);
        for (int i = 0; i < Math.min(this.percentileGauges.length, percentileValues.length); i++) {
                    this.percentileGauges[i].set(percentileValues[i]);
        }
    }
}

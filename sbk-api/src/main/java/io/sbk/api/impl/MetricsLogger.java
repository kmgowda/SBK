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
import io.sbk.api.Config;
import io.sbk.api.Print;
import io.sbk.api.Time;
import io.sbk.api.TimeUnit;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class for recoding/printing benchmark results on micrometer Composite Meter Registry.
 */
public class MetricsLogger implements Print {
    final public DecimalFormat format;
    final private Counter bytes;
    final private Counter records;
    final private Counter invalidLatencyRecords;
    final private Counter lowerDiscard;
    final private Counter higherDiscard;
    final private AtomicDouble mbPsec;
    final private AtomicDouble recsPsec;
    final private AtomicDouble avgLatency;
    final private AtomicLong maxLatency;
    final private AtomicLong[] percentileGauges;
    final private MeterRegistry registry;
    final private ConvertTime convert;


    private interface ConvertTime {
        long toTimeUnit(long t);
    }


    public MetricsLogger(String storageName, String action, Time time, TimeUnit latencyTimeUnit,
                         double[] percentiles, int writers, int readers, CompositeMeterRegistry compositeRegistry) {
        this.format = new DecimalFormat(Config.SBK_PERCENTILE_FORMAT);
        final String metricPrefix = Config.NAME.replace(" ", "_").toUpperCase()
                + "_" + storageName.replace(" ", "_").toUpperCase()
                + "_" + action.replace(" ", "_");
        final String metricUnit = latencyTimeUnit.name().replace(" ", "_");
        final String bytesName = metricPrefix + "_Bytes";
        final String recordsName = metricPrefix + "_Records";
        final String mbPsecName = metricPrefix + "_MBPerSec";
        final String recsPsecName = metricPrefix + "_RecordsPerSec";
        final String avgLatencyName = metricPrefix + "_" + metricUnit + "_AvgLatency";
        final String maxLatencyName = metricPrefix + "_" + metricUnit + "_MaxLatency";
        final String invalidLatencyRecordsName = metricPrefix + "_InvalidLatencyRecords";
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
        this.invalidLatencyRecords = this.registry.counter(invalidLatencyRecordsName);
        this.mbPsec = this.registry.gauge(mbPsecName, new AtomicDouble());
        this.recsPsec = this.registry.gauge(recsPsecName, new AtomicDouble());
        this.avgLatency = this.registry.gauge(avgLatencyName, new AtomicDouble());
        this.maxLatency = this.registry.gauge(maxLatencyName, new AtomicLong());
        this.percentileGauges = new AtomicLong[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            this.percentileGauges[i] = this.registry.gauge(metricPrefix + "_" + metricUnit + "_" + format.format(percentiles[i]),
                    new AtomicLong());
        }
        if (latencyTimeUnit == TimeUnit.ns) {
            convert = time::convertToNanoSeconds;
        } else if (latencyTimeUnit == TimeUnit.mcs) {
            convert = time::convertToMicroSeconds;
        } else {
            convert = time::convertToMilliSeconds;
        }
    }

    public void close() {
        registry.close();
    }


    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, int maxLatency,
                      long invalid, long lowerDiscard, long higherDiscard, int[] percentileValues) {
        this.bytes.increment(bytes);
        this.records.increment(records);
        this.invalidLatencyRecords.increment(invalid);
        this.lowerDiscard.increment(lowerDiscard);
        this.higherDiscard.increment(higherDiscard);
        this.recsPsec.set(recsPerSec);
        this.mbPsec.set(mbPerSec);
        this.avgLatency.set(convert.toTimeUnit((long) avgLatency));
        this.maxLatency.set(convert.toTimeUnit(maxLatency));
        for (int i = 0; i < Math.min(this.percentileGauges.length, percentileValues.length); i++) {
            this.percentileGauges[i].set(convert.toTimeUnit(percentileValues[i]));
        }
    }


}
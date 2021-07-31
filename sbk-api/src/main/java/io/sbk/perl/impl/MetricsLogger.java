/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.perl.impl;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.sbk.perl.PerlConfig;
import io.sbk.perl.Print;
import io.sbk.perl.Time;
import io.sbk.perl.TimeUnit;

import java.text.DecimalFormat;

/**
 * Class for recoding/printing benchmark results on micrometer Composite Meter Registry.
 */
public class MetricsLogger implements Print {
    final public String metricPrefix;
    final public MeterRegistry registry;
    final public DecimalFormat format;
    final private Counter bytes;
    final private Counter records;
    final private Counter invalidLatencyRecords;
    final private Counter lowerDiscard;
    final private Counter higherDiscard;
    final private AtomicDouble mbPsec;
    final private AtomicDouble recsPsec;
    final private AtomicDouble avgLatency;
    final private AtomicDouble maxLatency;
    final private AtomicDouble[] percentileGauges;
    final private Convert convert;

    private interface Convert {
        double apply(double val);
    }

    public MetricsLogger(String header, String action, double[] percentiles,
                         Time time, TimeUnit latencyTimeUnit, CompositeMeterRegistry compositeRegistry) {
        this.format = new DecimalFormat(PerlConfig.PERCENTILE_FORMAT);
        this.metricPrefix = header.replace(" ", "_").toUpperCase() + "_"
                + action.replace(" ", "_");
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
        this.registry = compositeRegistry;
        this.bytes = this.registry.counter(bytesName);
        this.records = this.registry.counter(recordsName);
        this.lowerDiscard = this.registry.counter(lowerDiscardName);
        this.higherDiscard = this.registry.counter(higherDiscardName);
        this.invalidLatencyRecords = this.registry.counter(invalidLatencyRecordsName);
        this.mbPsec = this.registry.gauge(mbPsecName, new AtomicDouble());
        this.recsPsec = this.registry.gauge(recsPsecName, new AtomicDouble());
        this.avgLatency = this.registry.gauge(avgLatencyName, new AtomicDouble());
        this.maxLatency = this.registry.gauge(maxLatencyName, new AtomicDouble());
        this.percentileGauges = new AtomicDouble[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            this.percentileGauges[i] = this.registry.gauge(metricPrefix + "_" + metricUnit + "_" + format.format(percentiles[i]),
                    new AtomicDouble());
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
    final public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                            double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                            long[] percentileValues) {
        this.bytes.increment(bytes);
        this.records.increment(records);
        this.invalidLatencyRecords.increment(invalid);
        this.lowerDiscard.increment(lowerDiscard);
        this.higherDiscard.increment(higherDiscard);
        this.recsPsec.set(recsPerSec);
        this.mbPsec.set(mbPerSec);
        this.avgLatency.set(convert.apply(avgLatency));
        this.maxLatency.set(convert.apply((double) maxLatency));
        for (int i = 0; i < Math.min(this.percentileGauges.length, percentileValues.length); i++) {
            this.percentileGauges[i].set(convert.apply((double) percentileValues[i]));
        }
    }
}
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

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.perl.logger.Print;
import io.time.Time;
import io.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Class for recoding/printing benchmark results on micrometer Composite Meter Registry.
 */
public sealed class PrintMetrics extends Metrics implements Print permits PrometheusMetricsServer {

    /**
     *<code>MeterRegistry registry</code>.
     */
    final public MeterRegistry registry;

    /**
     *<code>Counter bytes</code>.
     */
    final private Counter bytes;

    /**
     *<code>Counter records</code>.
     */
    final private Counter records;

    /**
     *<code>Counter invalidLatencyRecords</code>.
     */
    final private Counter invalidLatencyRecords;

    /**
     *<code>Counter lowerDiscard</code>.
     */
    final private Counter lowerDiscard;

    /**
     *<code>Counter higherDiscard</code>.
     */
    final private Counter higherDiscard;

    /**
     *<code>AtomicDouble mbPsec</code>.
     */
    final private AtomicDouble mbPsec;

    /**
     *<code>AtomicDouble recsPsec</code>.
     */
    final private AtomicDouble recsPsec;

    /**
     *<code>AtomicDouble avgLatency</code>.
     */
    final private AtomicDouble avgLatency;

    /**
     *<code>AtomicDouble maxLatency</code>.
     */
    final private AtomicDouble maxLatency;

    /**
     *<code>AtomicDouble[] percentileGauges</code>.
     */
    final private AtomicDouble[] percentileGauges;

    /**
     *<code>AtomicLong slc1</code>.
     */
    final private AtomicLong slc1;

    /**
     *<code>AtomicLong slc2</code>.
     */
    final private AtomicLong slc2;

    /**
     * <code>Convert convert</code>.
     */
    final private Convert convert;

    /**
     * Constructor  PrintMetrics initializing all values.
     *
     * @param header                NotNull String
     * @param percentiles           NotNull double[]
     * @param time                  Time
     * @param latencyTimeUnit       NotNull TimeUnit
     * @param compositeRegistry     CompositeMeterRegistry
     */
    public PrintMetrics(@NotNull String header, @NotNull double[] percentiles,
                        Time time, @NotNull TimeUnit latencyTimeUnit, CompositeMeterRegistry compositeRegistry) {
        super(header, latencyTimeUnit.name(), percentiles);
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
        this.slc1 = this.registry.gauge(slc1Name, new AtomicLong());
        this.slc2 = this.registry.gauge(slc2Name, new AtomicLong());
        this.percentileGauges = new AtomicDouble[percentileNames.length];
        for (int i = 0; i < percentileNames.length; i++) {
            this.percentileGauges[i] = this.registry.gauge(percentileNames[i],
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

    /**
     * This method Closes this registry, releasing any resources in the process.
     * Once closed, this registry will no longer accept new meters
     * and any publishing activity will cease.
     */
    public void close() {
        registry.close();
    }

    @Override
    final public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                            double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                            long slc1, long slc2, long[] percentileValues) {
        this.bytes.increment(bytes);
        this.records.increment(records);
        this.invalidLatencyRecords.increment(invalid);
        this.lowerDiscard.increment(lowerDiscard);
        this.higherDiscard.increment(higherDiscard);
        this.recsPsec.set(recsPerSec);
        this.mbPsec.set(mbPerSec);
        this.slc1.set(slc1);
        this.slc2.set(slc2);
        this.avgLatency.set(convert.apply(avgLatency));
        this.maxLatency.set(convert.apply((double) maxLatency));
        for (int i = 0; i < Math.min(this.percentileGauges.length, percentileValues.length); i++) {
            this.percentileGauges[i].set(convert.apply((double) percentileValues[i]));
        }
    }

    private interface Convert {
        double apply(double val);
    }
}
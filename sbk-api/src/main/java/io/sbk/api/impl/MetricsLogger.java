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
    final private AtomicInteger percOne;
    final private AtomicInteger percTwo;
    final private AtomicInteger percThree;
    final private AtomicInteger percFour;
    final private AtomicInteger percFive;
    final private AtomicInteger percSix;
    final private AtomicInteger percSeven;
    final private AtomicInteger percEight;
    final private MeterRegistry registry;

    public MetricsLogger(String header, String prefix, int writers, int readers,
                         CompositeMeterRegistry compositeRegistry) {
        super(prefix);
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
        final String percOneName = metricPrefix + "_" + metricUnit + "_10th";
        final String percTwoName = metricPrefix + "_" + metricUnit + "_25th";
        final String percThreeName = metricPrefix + "_" + metricUnit + "_50th";
        final String percFourName = metricPrefix + "_" + metricUnit + "_75th";
        final String percFiveName = metricPrefix + "_" + metricUnit + "_90th";
        final String percSixName = metricPrefix + "_" + metricUnit + "_99th";
        final String percSevenName = metricPrefix + "_" + metricUnit + "_99.9th";
        final String percEightName = metricPrefix + "_" + metricUnit + "_99.99th";
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
        this.percOne = this.registry.gauge(percOneName, new AtomicInteger());
        this.percTwo = this.registry.gauge(percTwoName, new AtomicInteger());
        this.percThree = this.registry.gauge(percThreeName, new AtomicInteger());
        this.percFour = this.registry.gauge(percFourName, new AtomicInteger());
        this.percFive = this.registry.gauge(percFiveName, new AtomicInteger());
        this.percSix = this.registry.gauge(percSixName, new AtomicInteger());
        this.percSeven = this.registry.gauge(percSevenName, new AtomicInteger());
        this.percEight = this.registry.gauge(percEightName, new AtomicInteger());
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, int maxLatency,
               long lowerDiscard, long higherDiscard, int one, int two, int three, int four, int five, int six, int seven, int eight) {
        super.print( bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                lowerDiscard, higherDiscard, one, two, three, four, five, six, seven, eight);
        this.bytes.increment(bytes);
        this.records.increment(records);
        this.lowerDiscard.increment(lowerDiscard);
        this.higherDiscard.increment(higherDiscard);
        this.recsPsec.set(recsPerSec);
        this.mbPsec.set(mbPerSec);
        this.avgLatency.set(avgLatency);
        this.maxLatency.set(maxLatency);
        this.percOne.set(one);
        this.percTwo.set(two);
        this.percThree.set(three);
        this.percFour.set(four);
        this.percFive.set(five);
        this.percSix.set(six);
        this.percSeven.set(seven);
        this.percEight.set(eight);
    }
}

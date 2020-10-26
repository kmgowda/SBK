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
    private final String bytesName;
    private final String recordsName;
    private final String mbPsecName;
    private final String recsPsecName;
    private final String avgLatencyName;
    private final String maxLatencyName;
    private final String lowerDiscardName;
    private final String higherDiscardName;
    private final String percOneName;
    private final String percTwoName;
    private final String percThreeName;
    private final String percFourName;
    private final String percFiveName;
    private final String percSixName;
    private final String percSevenName;
    private final String percEightName;
    private final String writersName;
    private final String readersName;
    private final Counter bytes;
    private final Counter records;
    private final Counter lowerDiscard;
    private final Counter higherDiscard;
    private final AtomicDouble mbPsec;
    private final AtomicDouble recsPsec;
    private final AtomicDouble avgLatency;
    private final AtomicInteger maxLatency;
    private final AtomicInteger percOne;
    private final AtomicInteger percTwo;
    private final AtomicInteger percThree;
    private final AtomicInteger percFour;
    private final AtomicInteger percFive;
    private final AtomicInteger percSix;
    private final AtomicInteger percSeven;
    private final AtomicInteger percEight;
    private final MeterRegistry registry;
    private final int reportingInterval;


    public MetricsLogger(String header, String prefix, int writers, int readers,
                         int reportingInterval, CompositeMeterRegistry registry) {
        super(prefix);
        final String metricPrefix = header.replace(" ", "_").toUpperCase() + "_" + prefix.replace(" ", "_");
        final String metricUnit = unit.replace(" ", "_");
        this.registry = registry;
        this.reportingInterval = reportingInterval;
        this.bytesName = metricPrefix + "_Bytes";
        this.recordsName = metricPrefix + "_Records";
        this.mbPsecName = metricPrefix + "_MBPerSec";
        this.recsPsecName = metricPrefix + "_RecordsPerSec";
        this.avgLatencyName = metricPrefix + "_"+ metricUnit + "_AvgLatency";
        this.maxLatencyName = metricPrefix + "_" + metricUnit +"_MaxLatency";
        this.lowerDiscardName = metricPrefix + "_LowerDiscardedLatencyRecords";
        this.higherDiscardName = metricPrefix + "_HigherDiscardLatencyRecords";
        this.percOneName = metricPrefix + "_" + metricUnit + "_10th";
        this.percTwoName = metricPrefix + "_" + metricUnit + "_25th";
        this.percThreeName = metricPrefix +"_"+ metricUnit + "_50th";
        this.percFourName = metricPrefix +"_" + metricUnit +"_75th";
        this.percFiveName = metricPrefix +"_" + metricUnit +"_90th";
        this.percSixName = metricPrefix + "_" + metricUnit +"_99th";
        this.percSevenName = metricPrefix + "_" + metricUnit +"_99.9th";
        this.percEightName = metricPrefix + "_" + metricUnit +"_99.99th";
        this.writersName = metricPrefix + "_Writers";
        this.readersName = metricPrefix + "_Readers";
        this.registry.gauge(this.writersName, writers);
        this.registry.gauge(this.readersName, readers);
        this.bytes = registry.counter(bytesName);
        this.records = registry.counter(recordsName);
        this.lowerDiscard = registry.counter(lowerDiscardName);
        this.higherDiscard = registry.counter(higherDiscardName);
        this.mbPsec = registry.gauge(mbPsecName, new AtomicDouble());
        this.recsPsec = registry.gauge(recsPsecName, new AtomicDouble());
        this.avgLatency = registry.gauge(avgLatencyName, new AtomicDouble());
        this.maxLatency = registry.gauge(maxLatencyName, new AtomicInteger());
        this.percOne = registry.gauge(percOneName, new AtomicInteger());
        this.percTwo = registry.gauge(percTwoName, new AtomicInteger());
        this.percThree = registry.gauge(percThreeName, new AtomicInteger());
        this.percFour = registry.gauge(percFourName, new AtomicInteger());
        this.percFive = registry.gauge(percFiveName, new AtomicInteger());
        this.percSix = registry.gauge(percSixName, new AtomicInteger());
        this.percSeven = registry.gauge(percSevenName, new AtomicInteger());
        this.percEight = registry.gauge(percEightName, new AtomicInteger());
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

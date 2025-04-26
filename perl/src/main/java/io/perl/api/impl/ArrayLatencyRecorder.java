/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api.impl;

import io.perl.config.LatencyConfig;
import io.perl.api.LatencyPercentiles;
import io.perl.api.LatencyRecord;
import io.perl.api.LatencyRecordWindow;
import io.perl.api.ReportLatencies;
import io.time.Time;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * class for Performance statistics.
 */
@NotThreadSafe
final public class ArrayLatencyRecorder extends LatencyRecordWindow {
    final private long[] latencies;
    final private long maxMemorySizeBytes;
    private int minIndex;
    private int maxIndex;

    /**
     * Constructor ArrayLatencyRecorder initialize all values.
     *
     * @param lowLatency            long
     * @param highLatency           long
     * @param totalLatencyMax       long
     * @param totalRecordsMax       long
     * @param bytesMax              long
     * @param percentiles           double[]
     * @param time                  Time
     */
    public ArrayLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax,
                                long bytesMax, double[] percentiles, Time time) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentiles, time);
        final int size = (int) Math.min(highLatency - lowLatency, Integer.MAX_VALUE);
        this.latencies = new long[size];
        this.maxMemorySizeBytes = (long) LatencyConfig.LATENCY_VALUE_SIZE_BYTES * size;
        this.minIndex = size;
        this.maxIndex = 0;
    }

    @Override
    public void reset(long startTime) {
        super.reset(startTime);
        this.maxIndex = 0;
        this.minIndex = Integer.MAX_VALUE;
    }


    @Override
    public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies copyLatencies) {
        if (copyLatencies != null) {
            copyLatencies.reportLatencyRecord(this);
        }
        percentiles.reset(validLatencyRecords);
        long curIndex = 0;
        for (int i = minIndex; i < Math.min(latencies.length, this.maxIndex + 1); i++) {
            if (latencies[i] > 0) {
                final long latency = i + lowLatency;
                final long count = latencies[i];
                final long nextIndex = curIndex + count;

                if (copyLatencies != null) {
                    copyLatencies.reportLatency(latency, count);
                }
                percentiles.copyLatency(latency, count, curIndex, nextIndex);
                curIndex = nextIndex;
                latencies[i] = 0;
            }
        }
    }

    @Override
    public boolean isFull() {
        return super.isOverflow();
    }

    @Override
    public long getMaxMemoryBytes() {
        return this.maxMemorySizeBytes;
    }

    @Override
    public void reportLatencyRecord(LatencyRecord record) {
        super.update(record);
    }


    @Override
    public void reportLatency(long latency, long count) {
        final int index = (int) (latency - this.lowLatency);
        if (index < this.latencies.length) {
            this.minIndex = Math.min(this.minIndex, index);
            this.maxIndex = Math.max(this.maxIndex, index);
            this.latencies[index] += count;
        }
    }

    /**
     * Record the latency.
     *
     * @param startTime start time.
     * @param events    number of events(records).
     * @param bytes     number of bytes.
     * @param latency   latency value in milliseconds.
     */
    @Override
    public void recordLatency(long startTime, int events, int bytes, long latency) {
        if (record(events, bytes, latency)) {
            reportLatency(latency, events);
        }
    }

}

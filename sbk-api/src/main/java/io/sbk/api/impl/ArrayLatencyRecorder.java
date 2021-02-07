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

import io.sbk.api.Time;

import javax.annotation.concurrent.NotThreadSafe;

/**
 *  class for Performance statistics.
 */
@NotThreadSafe
public class ArrayLatencyRecorder extends LatencyWindow {
    final private long[] latencies;

    ArrayLatencyRecorder(long lowLatency, long highLatency, double[] percentiles, Time time, long startTime) {
        super(lowLatency, highLatency, percentiles, time, startTime);
        final int size = (int) Math.min(highLatency-lowLatency, Integer.MAX_VALUE);
        this.latencies = new long[size];
    }

    @Override
    public long[] getPercentiles() {
        final long[] values = new long[percentiles.length];
        final long[] percentileIds = new long[percentiles.length];
        long cur = 0;
        int index = 0;

        for (int i = 0; i < percentileIds.length; i++) {
            percentileIds[i] = (long) (validLatencyRecords * percentiles[i]);
        }

        for (int i = 0; i < Math.min(latencies.length, this.maxLatency+1); i++) {
            if (latencies[i] > 0) {
                while (index < values.length) {
                    if (percentileIds[index] >= cur && percentileIds[index] < (cur + latencies[i])) {
                        values[index] = i + lowLatency;
                        index += 1;
                    } else {
                        break;
                    }
                }
                cur += latencies[i];
                latencies[i] = 0;
            }
        }
        return values;
    }

    /**
     * Record the latency.
     *
     * @param startTime start time.
     * @param bytes number of bytes.
     * @param events number of events(records).
     * @param latency latency value in milliseconds.
     */
    @Override
    public void record(long startTime, int bytes, int events, long latency) {
        if (record(bytes, events, latency)) {
            final int index = (int) (latency - this.lowLatency);
            this.latencies[index] += events;
        }
    }
}

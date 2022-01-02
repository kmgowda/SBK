/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl;

final public class LatencyPercentiles {
    final public double[] fractions;
    final public long[] latencies;
    final public long[] latencyIndexes;
    final public long[] latencyCount;
    public long medianLatency;
    public long medianLatencyCount;
    public long medianIndex;
    private int index;

    public LatencyPercentiles(double[] percentileFractions) {
        this.fractions = percentileFractions;
        this.latencies = new long[this.fractions.length];
        this.latencyIndexes = new long[this.fractions.length];
        this.latencyCount = new long[this.fractions.length];
        this.medianLatency = 0;
        this.medianLatencyCount = 0;
        this.medianIndex = 0;
        this.index = 0;
    }

    public void reset(long totalRecords) {
        for (int i = 0; i < fractions.length; i++) {
            latencyIndexes[i] = (long) (totalRecords * fractions[i]);
            latencies[i] = 0;
            latencyCount[i] = 0;
        }
        medianIndex = totalRecords >> 1;
        medianLatency = 0;
        medianLatencyCount = 0;
        index = 0;
    }

    public int copyLatency(long latency, long count, long startIndex, long endIndex) {
        int ret = 0;

        if (medianIndex >= startIndex && medianIndex < endIndex) {
            medianLatency = latency;
            medianLatencyCount = count;
            ret++;
        }

        while (index < latencyIndexes.length) {
            if (latencyIndexes[index] >= startIndex && latencyIndexes[index] < endIndex) {
                latencies[index] = latency;
                latencyCount[index] = count;
                index++;
                ret++;
            } else {
                break;
            }
        }
        return ret;
    }

}

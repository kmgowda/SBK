/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api;

/**
 * Class LatencyPercentiles.
 */
final public class LatencyPercentiles {

    /**
     * <code>public double[] fractions</code>.
     */
    final public double[] fractions;

    /**
     * <code>public long[] latencies</code>.
     */
    final public long[] latencies;

    /**
     * <code>public long[] latencyIndexes</code>.
     */
    final public long[] latencyIndexes;

    /**
     * <code>public long[] latencyCount</code>.
     */
    final public long[] latencyCount;

    /**
     * <code>long medianLatency</code>.
     */
    public long medianLatency;

    /**
     * <code>long medianLatencyCount</code>.
     */
    public long medianLatencyCount;

    /**
     * <code>long medianIndex</code>.
     */
    public long medianIndex;

    /**
     * <code>int index</code>.
     */
    private int index;

    /**
     * Constructor LatencyPercentiles initializing all values.
     *
     * @param percentileFractions double[]
     */
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

    /**
     * This method reset all records.
     *
     * @param totalRecords long
     */
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

    /**
     * This method copy Latency.
     *
     * @param latency       long
     * @param count         long
     * @param startIndex    long
     * @param endIndex      long
     */
    public void copyLatency(long latency, long count, long startIndex, long endIndex) {
        if (medianIndex >= startIndex && medianIndex < endIndex) {
            medianLatency = latency;
            medianLatencyCount = count;
        }
        while (index < latencyIndexes.length) {
            if (latencyIndexes[index] >= startIndex && latencyIndexes[index] < endIndex) {
                latencies[index] = latency;
                latencyCount[index] = count;
                index++;
            } else {
                break;
            }
        }
    }

}

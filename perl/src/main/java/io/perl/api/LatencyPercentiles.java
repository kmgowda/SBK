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
 * Helper that holds the configured percentile fractions and computed percentile
 * latency results. The class is used by latency recorders to map bucketed
 * latencies into percentile values.
 *
 * <p>Usage overview:
 * <ol>
 *     <li>Create an instance with the desired percentile fractions
 *     (e.g. {0.5, 0.9, 0.99}).</li>
 *     <li>Call {@link #reset(long)} with the expected total number of records
 *     to compute internal target indexes for percentiles.</li>
 *     <li>Feed bucketed latency summaries to {@link #copyLatency(long, long, long, long)}
 *     which will populate the percentile latencies when the buckets cover the
 *     target indexes.</li>
 * </ol>
 */
final public class LatencyPercentiles {

    /** Percentile fractions (e.g. 0.5 for 50th percentile). */
    final public double[] fractions;

    /** Computed latency values for each configured percentile. */
    final public long[] latencies;

    /** Target indexes, computed in {@link #reset(long)} for each percentile. */
    final public long[] latencyIndexes;

    /** Counts for each percentile (populated during copyLatency). */
    final public long[] latenciesCount;

    /** Median latency value (computed as the 50th percentile approximation). */
    public long medianLatency;

    /** Median index position used to detect median bucket. */
    public long medianIndex;

    private int index;

    /**
     * Construct with the configured percentile fractions.
     *
     * @param percentileFractions array of fractions in range (0,1]
     */
    public LatencyPercentiles(double[] percentileFractions) {
        this.fractions = percentileFractions;
        this.latencies = new long[this.fractions.length];
        this.latencyIndexes = new long[this.fractions.length];
        this.latenciesCount = new long[this.fractions.length];
        this.medianLatency = 0;
        this.medianIndex = 0;
        this.index = 0;
    }

    /**
     * Reset internal target indexes and counters using the provided total
     * records count. After reset, callers should feed bucket summaries using
     * {@link #copyLatency(long,long,long,long)} to populate percentile values.
     *
     * @param totalRecords total number of records expected in the window
     */
    public void reset(long totalRecords) {
        for (int i = 0; i < fractions.length; i++) {
            latencyIndexes[i] = (long) (totalRecords * fractions[i]);
            latencies[i] = 0;
            latenciesCount[i] = 0;
        }
        medianIndex = totalRecords >> 1;
        medianLatency = 0;
        index = 0;
    }

    /**
     * Copy latency for a bucket spanning [startIndex, endIndex). If any
     * configured percentile target index lies inside the bucket, the
     * corresponding percentile value and count will be set.
     *
     * @param latency    latency value for the bucket
     * @param count      count of samples in the bucket
     * @param startIndex inclusive start index of the bucket
     * @param endIndex   exclusive end index of the bucket
     */
    public void copyLatency(long latency, long count, long startIndex, long endIndex) {
        if (medianIndex >= startIndex && medianIndex < endIndex) {
            medianLatency = latency;
        }
        while (index < latencyIndexes.length) {
            if (latencyIndexes[index] >= startIndex && latencyIndexes[index] < endIndex) {
                latencies[index] = latency;
                latenciesCount[index] = count;
                index++;
            } else {
                break;
            }
        }
    }

}

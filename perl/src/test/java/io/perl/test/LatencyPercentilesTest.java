/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import io.perl.api.LatencyPercentiles;
import io.perl.api.LatencyRecordWindow;
import io.perl.api.impl.ArrayLatencyRecorder;
import io.perl.api.impl.HashMapLatencyRecorder;
import io.perl.api.impl.HybridPagedLatencyRecorder;
import io.perl.api.impl.LongHashMapLatencyRecorder;
import io.time.NanoSeconds;

import java.util.List;

/**
 * Class LatencyPercentilesTest.
 */
public class LatencyPercentilesTest {

    private double[] fractions;
    private LatencyPercentiles percentiles;

    @BeforeEach
    public void setUp() {
        fractions = new double[]{0.5, 0.9, 0.99};
        percentiles = new LatencyPercentiles(fractions);
    }

    /**
     * Test constructor initializes arrays and fields correctly.
     */
    @Test
    public void testConstructor() {
        assertArrayEquals(fractions, percentiles.fractions, 0.0001);
        assertEquals(fractions.length, percentiles.latencies.length);
        assertEquals(fractions.length, percentiles.latencyIndexes.length);
        assertEquals(fractions.length, percentiles.latenciesCount.length);
        assertEquals(0, percentiles.medianLatency);
        assertEquals(0, percentiles.medianIndex);
    }

    /**
     * Test reset sets indexes and clears values.
     */
    @Test
    public void testReset() {
        percentiles.reset(100);
        assertEquals(50, percentiles.latencyIndexes[0]);
        assertEquals(90, percentiles.latencyIndexes[1]);
        assertEquals(99, percentiles.latencyIndexes[2]);
        for (int i = 0; i < fractions.length; i++) {
            assertEquals(0, percentiles.latencies[i]);
            assertEquals(0, percentiles.latenciesCount[i]);
        }
        assertEquals(50, percentiles.medianIndex);
        assertEquals(0, percentiles.medianLatency);
    }

    /**
     * Test copyLatency sets median and percentile values.
     */
    @Test
    public void testCopyLatency() {
        percentiles.reset(100);
        // Simulate a bucket covering indexes 0-60, latency=10, count=5
        percentiles.copyLatency(10, 5, 0, 60);
        // medianIndex=50, so medianLatency should be set
        assertEquals(10, percentiles.medianLatency);
        // 0.5 (index 0)  is within 0-60
        assertEquals(10, percentiles.latencies[0]);
        assertEquals(5, percentiles.latenciesCount[0]);
        assertEquals(0, percentiles.latencies[1]);
        assertEquals(0, percentiles.latenciesCount[1]);

        // Next bucket: 60-100, latency=20, count=2
        percentiles.copyLatency(20, 2, 60, 100);
        // 0.99 (index 2) is within 60-100
        assertEquals(20, percentiles.latencies[1]);
        assertEquals(2, percentiles.latenciesCount[1]);
    }


    /** Verifies P100 selects the final populated bucket, including repeated P100 requests. */
    @Test
    public void testHundredthPercentileWithBatchedCounts() {
        final LatencyPercentiles result = new LatencyPercentiles(new double[]{0.5, 0.99, 1.0, 1.0});
        result.reset(6);
        result.copyLatency(10, 3, 0, 3);
        result.copyLatency(20, 1, 3, 4);
        result.copyLatency(30, 2, 4, 6);

        assertArrayEquals(new long[]{20, 30, 30, 30}, result.latencies);
        assertArrayEquals(new long[]{1, 2, 2, 2}, result.latenciesCount);
        assertEquals(20, result.medianLatency);
    }

    /** Verifies a single sample, an empty reset, and reuse of the same P100 result. */
    @Test
    public void testHundredthPercentileAcrossEmptyAndSingleSampleWindows() {
        final LatencyPercentiles result = new LatencyPercentiles(new double[]{1.0});
        result.reset(1);
        result.copyLatency(37, 1, 0, 1);
        assertEquals(37, result.latencies[0]);
        assertEquals(1, result.latenciesCount[0]);

        result.reset(0);
        assertEquals(0, result.latencies[0]);
        assertEquals(0, result.latenciesCount[0]);
        assertEquals(0, result.medianLatency);

        result.reset(1);
        result.copyLatency(12, 1, 0, 1);
        assertEquals(12, result.latencies[0]);
        assertEquals(1, result.latenciesCount[0]);
    }

    /** Verifies P100 remains exact when long sample counts cannot be represented as doubles. */
    @Test
    public void testHundredthPercentileWithLargeSampleCounts() {
        final long[] counts = new long[]{(1L << 55) + 3, (1L << 55) + 5, Long.MAX_VALUE};
        final LatencyPercentiles result = new LatencyPercentiles(new double[]{1.0});
        for (long count : counts) {
            result.reset(count);
            result.copyLatency(10, count - 1, 0, count - 1);
            result.copyLatency(20, 1, count - 1, count);
            assertEquals(20, result.latencies[0]);
            assertEquals(1, result.latenciesCount[0]);
        }
    }

    /** Verifies P100 through every exact recorder, including filtering and reuse after an empty window. */
    @Test
    public void testHundredthPercentileAcrossExactRecorders() {
        final double[] requested = new double[]{0.5, 0.99, 1.0};
        final NanoSeconds time = new NanoSeconds();
        final List<LatencyRecordWindow> recorders = List.of(
                new ArrayLatencyRecorder(10, 100, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, requested, time),
                new HashMapLatencyRecorder(10, 100, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                        requested, time, 16),
                new LongHashMapLatencyRecorder(10, 100, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                        requested, time, 16),
                new HybridPagedLatencyRecorder(10, 100, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                        requested, time, 16, 8, 128,
                        HybridPagedLatencyRecorder.MemoryLimitPolicy.RELEASE_AFTER_WINDOW));
        for (LatencyRecordWindow recorder : recorders) {
            final LatencyPercentiles result = new LatencyPercentiles(requested);
            recorder.recordLatency(0, 2, 2, 10);
            recorder.recordLatency(0, 3, 3, 20);
            recorder.recordLatency(0, 1, 1, 100);
            recorder.recordLatency(0, 1, 1, -1);
            recorder.recordLatency(0, 1, 1, 9);
            recorder.recordLatency(0, 1, 1, 101);
            recorder.copyPercentiles(result, null);
            assertArrayEquals(new long[]{20, 100, 100}, result.latencies);
            assertArrayEquals(new long[]{3, 1, 1}, result.latenciesCount);
            assertEquals(6, recorder.getValidLatencyRecords());

            recorder.reset(0);
            recorder.copyPercentiles(result, null);
            assertArrayEquals(new long[]{0, 0, 0}, result.latencies);
            assertArrayEquals(new long[]{0, 0, 0}, result.latenciesCount);

            recorder.reset(0);
            recorder.recordLatency(0, 1, 1, 42);
            recorder.copyPercentiles(result, null);
            assertArrayEquals(new long[]{42, 42, 42}, result.latencies);
            assertArrayEquals(new long[]{1, 1, 1}, result.latenciesCount);
        }
    }
}
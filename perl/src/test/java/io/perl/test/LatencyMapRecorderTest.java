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

import io.perl.api.LatencyPercentiles;
import io.perl.api.LatencyRecord;
import io.perl.api.LatencyRecordWindow;
import io.perl.api.ReportLatencies;
import io.perl.api.impl.HashMapLatencyRecorder;
import io.perl.api.impl.LongHashMapLatencyRecorder;
import io.perl.api.impl.PerlBuilder;
import io.perl.config.LatencyConfig;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies that the primitive sparse latency recorder preserves the behavior
 * of the boxed reference implementation.
 */
public class LatencyMapRecorderTest {
    private static final double[] FRACTIONS =
            new double[]{0.5, 0.9, 0.99};

    /**
     * Compares counters, ordered latency buckets, and percentiles for valid,
     * discarded, invalid, repeated, and zero-valued latency samples.
     */
    @Test
    public void primitiveRecorderMatchesBoxedRecorder() {
        final LatencyRecordWindow boxed = newRecorder(true);
        final LatencyRecordWindow primitive = newRecorder(false);
        final long[] latencies = new long[]{
                500, 0, 999_999, 500, 42, -1, 1_000_001, 7
        };
        final int[] records = new int[]{2, 1, 3, 4, 2, 1, 2, 3};

        for (int index = 0; index < latencies.length; index++) {
            boxed.recordLatency(index, records[index], records[index] * 100,
                    latencies[index]);
            primitive.recordLatency(index, records[index],
                    records[index] * 100, latencies[index]);
        }

        assertRecordEquals(boxed, primitive);

        final LatencyPercentiles boxedPercentiles =
                new LatencyPercentiles(FRACTIONS);
        final LatencyPercentiles primitivePercentiles =
                new LatencyPercentiles(FRACTIONS);
        final LatencyCollector boxedLatencies = new LatencyCollector();
        final LatencyCollector primitiveLatencies = new LatencyCollector();

        boxed.copyPercentiles(boxedPercentiles, boxedLatencies);
        primitive.copyPercentiles(primitivePercentiles, primitiveLatencies);

        assertEquals(boxedLatencies.latencies, primitiveLatencies.latencies);
        assertEquals(boxedLatencies.counts, primitiveLatencies.counts);
        assertArrayEquals(boxedPercentiles.latencies,
                primitivePercentiles.latencies);
        assertArrayEquals(boxedPercentiles.latenciesCount,
                primitivePercentiles.latenciesCount);
        assertEquals(boxedPercentiles.medianLatency,
                primitivePercentiles.medianLatency);
    }

    /**
     * Confirms that sparse-window selection now returns the primitive map.
     */
    @Test
    public void builderSelectsPrimitiveRecorderForSparseRange() {
        final LatencyConfig config = new LatencyConfig();
        config.maxArraySizeMB = 0;

        final LatencyRecordWindow recorder =
                PerlBuilder.buildLatencyRecordWindow(config, new NanoSeconds(),
                        0, 1_000_000, FRACTIONS);

        assertInstanceOf(LongHashMapLatencyRecorder.class, recorder);
    }

    /**
     * Confirms that extraction clears both implementations for window reuse.
     */
    @Test
    public void primitiveAndBoxedRecordersRemainEquivalentAfterExtraction() {
        final LatencyRecordWindow boxed = newRecorder(true);
        final LatencyRecordWindow primitive = newRecorder(false);
        final LatencyPercentiles boxedPercentiles =
                new LatencyPercentiles(FRACTIONS);
        final LatencyPercentiles primitivePercentiles =
                new LatencyPercentiles(FRACTIONS);

        boxed.recordLatency(0, 1, 100, 1_000);
        primitive.recordLatency(0, 1, 100, 1_000);
        boxed.copyPercentiles(boxedPercentiles, null);
        primitive.copyPercentiles(primitivePercentiles, null);
        boxed.reset(1);
        primitive.reset(1);
        boxed.recordLatency(1, 3, 300, 2_000);
        primitive.recordLatency(1, 3, 300, 2_000);
        boxed.copyPercentiles(boxedPercentiles, null);
        primitive.copyPercentiles(primitivePercentiles, null);

        assertRecordEquals(boxed, primitive);
        assertArrayEquals(boxedPercentiles.latencies,
                primitivePercentiles.latencies);
        assertArrayEquals(boxedPercentiles.latenciesCount,
                primitivePercentiles.latenciesCount);
    }

    private LatencyRecordWindow newRecorder(boolean boxed) {
        if (boxed) {
            return new HashMapLatencyRecorder(0, 1_000_000,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    FRACTIONS, new NanoSeconds(), 16);
        }
        return new LongHashMapLatencyRecorder(0, 1_000_000,
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                FRACTIONS, new NanoSeconds(), 16);
    }

    private void assertRecordEquals(LatencyRecord expected,
                                    LatencyRecord actual) {
        assertEquals(expected.getTotalRecords(), actual.getTotalRecords());
        assertEquals(expected.getValidLatencyRecords(),
                actual.getValidLatencyRecords());
        assertEquals(expected.getLowerLatencyDiscardRecords(),
                actual.getLowerLatencyDiscardRecords());
        assertEquals(expected.getHigherLatencyDiscardRecords(),
                actual.getHigherLatencyDiscardRecords());
        assertEquals(expected.getInvalidLatencyRecords(),
                actual.getInvalidLatencyRecords());
        assertEquals(expected.getTotalBytes(), actual.getTotalBytes());
        assertEquals(expected.getTotalLatency(), actual.getTotalLatency());
        assertEquals(expected.getMinLatency(), actual.getMinLatency());
        assertEquals(expected.getMaxLatency(), actual.getMaxLatency());
    }

    private static final class LatencyCollector implements ReportLatencies {
        private final List<Long> latencies = new ArrayList<>();
        private final List<Long> counts = new ArrayList<>();

        @Override
        public void reportLatencyRecord(LatencyRecord record) {
            // Aggregate counters are compared directly by the enclosing test.
        }

        @Override
        public void reportLatency(long latency, long count) {
            latencies.add(latency);
            counts.add(count);
        }
    }
}

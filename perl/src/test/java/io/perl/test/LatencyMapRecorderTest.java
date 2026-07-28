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
import java.util.Random;

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

    /**
     * Verifies that growing and reusing the primitive sorting buffer does not
     * retain stale latency keys from a larger preceding window.
     */
    @Test
    public void primitiveRecorderReusesSortingBufferWithoutStaleLatencies() {
        final LongHashMapLatencyRecorder primitive =
                (LongHashMapLatencyRecorder) newRecorder(false);
        final LatencyPercentiles percentiles =
                new LatencyPercentiles(FRACTIONS);
        final LatencyCollector firstWindow = new LatencyCollector();
        final LatencyCollector secondWindow = new LatencyCollector();

        primitive.recordLatency(0, 1, 100, 9);
        primitive.recordLatency(0, 2, 200, 1);
        primitive.recordLatency(0, 3, 300, 5);
        primitive.recordLatency(0, 4, 400, 0);
        primitive.copyPercentiles(percentiles, firstWindow);

        assertEquals(List.of(0L, 1L, 5L, 9L), firstWindow.latencies);
        assertEquals(List.of(4L, 2L, 3L, 1L), firstWindow.counts);
        assertArrayEquals(new long[]{1, 9, 9}, percentiles.latencies);
        assertArrayEquals(new long[]{2, 1, 1}, percentiles.latenciesCount);
        assertEquals(1, percentiles.medianLatency);

        primitive.reset(1);
        primitive.recordLatency(1, 2, 200, 7);
        primitive.recordLatency(1, 1, 100, 2);
        primitive.copyPercentiles(percentiles, secondWindow);

        assertEquals(List.of(2L, 7L), secondWindow.latencies);
        assertEquals(List.of(1L, 2L), secondWindow.counts);
        assertArrayEquals(new long[]{7, 7, 7}, percentiles.latencies);
        assertArrayEquals(new long[]{2, 2, 2}, percentiles.latenciesCount);
        assertEquals(7, percentiles.medianLatency);
    }

    /**
     * Compares repeated empty, growing, and shrinking windows against the
     * boxed reference implementation using deterministic random samples.
     */
    @Test
    public void reusableSortingBufferMatchesBoxedRecorderAcrossWindows() {
        final LatencyRecordWindow boxed = newRecorder(true);
        final LatencyRecordWindow primitive = newRecorder(false);
        final Random random = new Random(7_241_993L);
        final int[] windowSizes =
                new int[]{0, 1, 2, 17, 257, 31, 1_024, 3, 0, 513};

        for (int window = 0; window < windowSizes.length; window++) {
            boxed.reset(window);
            primitive.reset(window);
            for (int sample = 0; sample < windowSizes[window]; sample++) {
                final long latency;
                if (sample == 0) {
                    latency = 0;
                } else if (sample == 1) {
                    latency = 1;
                } else {
                    latency = random.nextInt(1_000_000);
                }
                final int records = random.nextInt(7) + 1;
                final int bytes = records * 100;
                boxed.recordLatency(sample, records, bytes, latency);
                primitive.recordLatency(sample, records, bytes, latency);
            }
            assertExtractionEquals(boxed, primitive);
        }
    }

    private void assertExtractionEquals(LatencyRecordWindow boxed,
                                        LatencyRecordWindow primitive) {
        final LatencyPercentiles boxedPercentiles =
                new LatencyPercentiles(FRACTIONS);
        final LatencyPercentiles primitivePercentiles =
                new LatencyPercentiles(FRACTIONS);
        final LatencyCollector boxedLatencies = new LatencyCollector();
        final LatencyCollector primitiveLatencies = new LatencyCollector();

        boxed.copyPercentiles(boxedPercentiles, boxedLatencies);
        primitive.copyPercentiles(primitivePercentiles, primitiveLatencies);

        assertRecordEquals(boxed, primitive);
        assertEquals(boxedLatencies.latencies, primitiveLatencies.latencies);
        assertEquals(boxedLatencies.counts, primitiveLatencies.counts);
        assertArrayEquals(boxedPercentiles.latencies,
                primitivePercentiles.latencies);
        assertArrayEquals(boxedPercentiles.latenciesCount,
                primitivePercentiles.latenciesCount);
        assertEquals(boxedPercentiles.medianLatency,
                primitivePercentiles.medianLatency);
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

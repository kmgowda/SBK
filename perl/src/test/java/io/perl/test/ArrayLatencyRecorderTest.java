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
import io.perl.api.impl.ArrayLatencyRecorder;
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
 * Tests for the array-backed latency recorder.
 */
public class ArrayLatencyRecorderTest {
    private static final double[] FRACTIONS = new double[]{0.1, 0.5, 0.9, 0.99};
    private static final long LOW_LATENCY = 100;
    private static final long HIGH_LATENCY = 4_195;

    /**
     * Verify that the inclusive maximum latency has a dedicated array slot.
     */
    @Test
    public void testRecordsInclusiveMaximumLatency() {
        final LatencyConfig config = new LatencyConfig();
        config.maxArraySizeMB = 1;
        final LatencyRecordWindow window = PerlBuilder.buildLatencyRecordWindow(
                config, new NanoSeconds(), 0, 10, new double[]{0.5});
        final LatencyPercentiles percentiles = new LatencyPercentiles(new double[]{0.5});

        window.recordLatency(0, 1, 1, 10);
        window.copyPercentiles(percentiles, null);

        assertEquals(10, percentiles.latencies[0]);
        assertEquals(1, percentiles.latenciesCount[0]);
    }

    /**
     * Verifies that the builder selects the array recorder when its inclusive
     * range fits within the configured array-memory limit.
     */
    @Test
    public void builderSelectsArrayRecorderForDenseRange() {
        final LatencyConfig config = new LatencyConfig();
        config.maxArraySizeMB = 1;

        final LatencyRecordWindow window = PerlBuilder.buildLatencyRecordWindow(
                config, new NanoSeconds(), LOW_LATENCY, HIGH_LATENCY,
                FRACTIONS);

        assertInstanceOf(ArrayLatencyRecorder.class, window);
    }

    /**
     * Compares exact array and primitive-map results for non-zero bounds,
     * inclusive endpoints, discarded values, duplicates, and batched counts.
     */
    @Test
    public void matchesPrimitiveMapForBoundsDiscardsAndBatchedCounts() {
        final RecorderPair pair = newRecorderPair(100, 110);

        recordBoth(pair, 1, 2, 200, -1);
        recordBoth(pair, 2, 3, 300, 99);
        recordBoth(pair, 3, 4, 400, 100);
        recordBoth(pair, 4, 5, 500, 105);
        recordBoth(pair, 5, 2, 200, 105);
        recordBoth(pair, 6, 6, 600, 110);
        recordBoth(pair, 7, 7, 700, 111);

        final Extraction extraction = assertEquivalentExtraction(pair);

        assertEquals(List.of(100L, 105L, 110L),
                extraction.arrayLatencies.latencies);
        assertEquals(List.of(4L, 7L, 6L),
                extraction.arrayLatencies.counts);
        assertArrayEquals(new long[]{100, 105, 110, 110},
                extraction.arrayPercentiles.latencies);
        assertArrayEquals(new long[]{4, 7, 6, 6},
                extraction.arrayPercentiles.latenciesCount);
        assertEquals(105, extraction.arrayPercentiles.medianLatency);
        assertEquals(29, pair.array.getTotalRecords());
        assertEquals(17, pair.array.getValidLatencyRecords());
        assertEquals(2, pair.array.getInvalidLatencyRecords());
        assertEquals(3, pair.array.getLowerLatencyDiscardRecords());
        assertEquals(7, pair.array.getHigherLatencyDiscardRecords());
    }

    /**
     * Verifies that extracting an empty array window produces exactly the same
     * zeroed percentile and counter state as the primitive-map recorder.
     */
    @Test
    public void emptyWindowMatchesPrimitiveMap() {
        final RecorderPair pair = newRecorderPair(LOW_LATENCY, HIGH_LATENCY);

        final Extraction extraction = assertEquivalentExtraction(pair);

        assertEquals(List.of(), extraction.arrayLatencies.latencies);
        assertEquals(List.of(), extraction.arrayLatencies.counts);
        assertArrayEquals(new long[FRACTIONS.length],
                extraction.arrayPercentiles.latencies);
        assertArrayEquals(new long[FRACTIONS.length],
                extraction.arrayPercentiles.latenciesCount);
        assertEquals(0, extraction.arrayPercentiles.medianLatency);
    }

    /**
     * Exercises the production extract-then-reset lifecycle over growing,
     * shrinking, duplicate-heavy, and empty windows.
     */
    @Test
    public void repeatedGrowingAndShrinkingWindowsMatchPrimitiveMap() {
        final RecorderPair pair = newRecorderPair(LOW_LATENCY, HIGH_LATENCY);
        final Random random = new Random(8_613_407L);
        final int[] windowSizes =
                new int[]{1, 17, 257, 31, 1_024, 3, 0, 513, 2};

        for (int window = 0; window < windowSizes.length; window++) {
            pair.array.reset(window);
            pair.primitive.reset(window);
            for (int sample = 0; sample < windowSizes[window]; sample++) {
                final long latency = latencyForSample(random, sample);
                final int records = random.nextInt(8) + 1;
                recordBoth(pair, sample, records, records * 100, latency);
            }
            assertEquivalentExtraction(pair);
        }
    }

    /**
     * Verifies repeated empty resets before extraction and subsequent normal
     * record/extract cycles without changing production reset semantics.
     */
    @Test
    public void repeatedEmptyResetsAndExtractionMatchPrimitiveMap() {
        final RecorderPair pair = newRecorderPair(LOW_LATENCY, HIGH_LATENCY);

        for (int window = 0; window < 10; window++) {
            pair.array.reset(window);
            pair.primitive.reset(window);
        }
        assertEquivalentExtraction(pair);

        for (int window = 0; window < 20; window++) {
            pair.array.reset(window);
            pair.primitive.reset(window);
            final long latency = LOW_LATENCY + window;
            recordBoth(pair, window, window + 1, (window + 1) * 10,
                    latency);
            assertEquivalentExtraction(pair);
        }
    }

    private long latencyForSample(Random random, int sample) {
        return switch (sample) {
            case 0 -> LOW_LATENCY;
            case 1 -> HIGH_LATENCY;
            case 2, 3 -> LOW_LATENCY + 17;
            default -> LOW_LATENCY
                    + random.nextInt((int) (HIGH_LATENCY - LOW_LATENCY + 1));
        };
    }

    private RecorderPair newRecorderPair(long lowLatency, long highLatency) {
        final NanoSeconds time = new NanoSeconds();
        final LatencyRecordWindow array = new ArrayLatencyRecorder(
                lowLatency, highLatency, LatencyConfig.TOTAL_LATENCY_MAX,
                LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX, FRACTIONS,
                time);
        final LatencyRecordWindow primitive = new LongHashMapLatencyRecorder(
                lowLatency, highLatency, LatencyConfig.TOTAL_LATENCY_MAX,
                LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX, FRACTIONS,
                time, 16);
        return new RecorderPair(array, primitive);
    }

    private void recordBoth(RecorderPair pair, long startTime, int events,
                            int bytes, long latency) {
        pair.array.recordLatency(startTime, events, bytes, latency);
        pair.primitive.recordLatency(startTime, events, bytes, latency);
    }

    private Extraction assertEquivalentExtraction(RecorderPair pair) {
        final LatencyPercentiles arrayPercentiles =
                new LatencyPercentiles(FRACTIONS);
        final LatencyPercentiles primitivePercentiles =
                new LatencyPercentiles(FRACTIONS);
        final LatencyCollector arrayLatencies = new LatencyCollector();
        final LatencyCollector primitiveLatencies = new LatencyCollector();

        pair.array.copyPercentiles(arrayPercentiles, arrayLatencies);
        pair.primitive.copyPercentiles(primitivePercentiles,
                primitiveLatencies);

        assertRecordEquals(pair.array, pair.primitive);
        assertEquals(arrayLatencies.recordsReported,
                primitiveLatencies.recordsReported);
        assertEquals(arrayLatencies.latencies, primitiveLatencies.latencies);
        assertEquals(arrayLatencies.counts, primitiveLatencies.counts);
        assertArrayEquals(arrayPercentiles.latencies,
                primitivePercentiles.latencies);
        assertArrayEquals(arrayPercentiles.latenciesCount,
                primitivePercentiles.latenciesCount);
        assertEquals(arrayPercentiles.medianLatency,
                primitivePercentiles.medianLatency);
        assertArrayEquals(arrayPercentiles.latencyIndexes,
                primitivePercentiles.latencyIndexes);

        return new Extraction(arrayPercentiles, arrayLatencies);
    }

    private void assertRecordEquals(LatencyRecordWindow expected,
                                    LatencyRecordWindow actual) {
        assertEquals(expected.getTotalRecords(), actual.getTotalRecords());
        assertEquals(expected.getValidLatencyRecords(),
                actual.getValidLatencyRecords());
        assertEquals(expected.getInvalidLatencyRecords(),
                actual.getInvalidLatencyRecords());
        assertEquals(expected.getLowerLatencyDiscardRecords(),
                actual.getLowerLatencyDiscardRecords());
        assertEquals(expected.getHigherLatencyDiscardRecords(),
                actual.getHigherLatencyDiscardRecords());
        assertEquals(expected.getTotalBytes(), actual.getTotalBytes());
        assertEquals(expected.getTotalLatency(), actual.getTotalLatency());
        assertEquals(expected.getMinLatency(), actual.getMinLatency());
        assertEquals(expected.getMaxLatency(), actual.getMaxLatency());
    }

    private record RecorderPair(LatencyRecordWindow array,
                                LatencyRecordWindow primitive) {
    }

    private record Extraction(LatencyPercentiles arrayPercentiles,
                              LatencyCollector arrayLatencies) {
    }

    private static final class LatencyCollector implements ReportLatencies {
        private final List<Long> latencies = new ArrayList<>();
        private final List<Long> counts = new ArrayList<>();
        private int recordsReported;

        @Override
        public void reportLatencyRecord(LatencyRecord record) {
            recordsReported++;
        }

        @Override
        public void reportLatency(long latency, long count) {
            latencies.add(latency);
            counts.add(count);
        }
    }
}

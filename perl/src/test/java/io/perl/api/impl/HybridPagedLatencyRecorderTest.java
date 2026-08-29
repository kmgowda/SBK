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

import io.perl.api.LatencyPercentiles;
import io.perl.api.LatencyRecord;
import io.perl.api.ReportLatencies;
import io.perl.api.impl.HybridPagedLatencyRecorder.MemoryLimitPolicy;
import io.perl.config.LatencyConfig;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies exact hybrid-page aggregation against the primitive-map reference. */
final class HybridPagedLatencyRecorderTest {
    private static final double[] PERCENTILES = new double[]{0.5, 0.9, 0.99};
    private static final long MINIMUM_LATENCY = -2_000_000_000L;
    private static final long MAXIMUM_LATENCY = 2_000_000_000L;

    @Test
    void preservesExactOrderingAcrossNegativeAndPositivePageBoundaries() {
        final long[] latencies = new long[]{257, -257, 0, 256, -1, 1, -256, 255, 1_000_000_000L};
        final int[] counts = new int[]{2, 3, 5, 7, 11, 13, 17, 19, 23};

        assertReportedEquivalent(latencies, counts, 8, 128);
    }

    @Test
    void sparsePagesPromoteWithoutChangingExactCounts() {
        final long[] latencies = new long[]{15, 1, 9, 2, 7, 3, 8, 4, 8, 1};
        final int[] counts = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        assertEquivalent(latencies, counts, 4, 4);
    }

    @Test
    void matchesPrimitiveMapAcrossRepeatedRandomWindows() {
        final LongHashMapLatencyRecorder reference = referenceRecorder(64);
        final HybridPagedLatencyRecorder paged = pagedRecorder(64, 8, 128);
        final Random random = new Random(4_729_113L);

        for (int window = 0; window < 20; window++) {
            reference.reset(window);
            paged.reset(window);
            final int samples = 2_000 + window * 17;
            for (int sample = 0; sample < samples; sample++) {
                final long latency = random.nextInt(2_000_000) - 1_000_000L;
                final int count = random.nextInt(9) + 1;
                reference.recordLatency(sample, count, count * 10, latency);
                paged.recordLatency(sample, count, count * 10, latency);
            }
            assertExtractionEquals(reference, paged);
        }
    }

    @Test
    void denseNanosecondRangeRetainsLessThanFlatEntryPayload() {
        final HybridPagedLatencyRecorder paged = pagedRecorder(64, 8, 128);
        final int distinctLatencies = 65_536;

        for (int latency = 0; latency < distinctLatencies; latency++) {
            paged.recordLatency(latency, 1, 1, latency);
        }

        final long flatKeyValuePayloadBytes = (long) distinctLatencies
                * LatencyConfig.LATENCY_MAP_ENTRY_VALUE_COUNT * Long.BYTES;
        assertTrue(paged.getRetainedMemoryBytes() < flatKeyValuePayloadBytes,
                () -> "paged bytes " + paged.getRetainedMemoryBytes()
                        + " must be below flat payload " + flatKeyValuePayloadBytes);
    }

    @Test
    void totalWindowPolicyEndsAndReleasesAnOversizedWindow() {
        final HybridPagedLatencyRecorder paged = pagedRecorder(1, 8, 128,
                MemoryLimitPolicy.RESET_WINDOW_WHEN_FULL);
        for (int page = 0; page < 12_000; page++) {
            final long latency = (long) page << 8;
            paged.recordLatency(page, 1, 1, latency);
        }
        assertTrue(paged.isFull());

        paged.copyPercentiles(new LatencyPercentiles(PERCENTILES), null);

        assertEquals(0, paged.getRetainedMemoryBytes());
        assertFalse(paged.isFull());
    }

    @Test
    void periodicWindowPolicyReleasesCacheWithoutEndingTheCurrentWindow() {
        final HybridPagedLatencyRecorder paged = pagedRecorder(1, 8, 128,
                MemoryLimitPolicy.RELEASE_AFTER_WINDOW);
        for (int page = 0; page < 12_000; page++) {
            paged.reportLatency((long) page << 8, 1);
        }
        assertFalse(paged.isFull());

        paged.copyPercentiles(new LatencyPercentiles(PERCENTILES), null);

        assertEquals(0, paged.getRetainedMemoryBytes());
        assertFalse(paged.isFull());
    }

    @Test
    void rejectsInvalidPageGeometryAndMemoryTargets() {
        assertThrows(IllegalArgumentException.class, () -> pagedRecorder(0, 8, 32));
        assertThrows(IllegalArgumentException.class, () -> pagedRecorder(64, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> pagedRecorder(64, 17, 32));
        assertThrows(IllegalArgumentException.class, () -> pagedRecorder(64, 8, 0));
        assertThrows(IllegalArgumentException.class, () -> pagedRecorder(64, 8, 256));
        assertThrows(IllegalArgumentException.class,
                () -> pagedRecorder(64, 8, 128, null));
    }

    private void assertEquivalent(long[] latencies, int[] counts, int pageBits, int sparseEntries) {
        final LongHashMapLatencyRecorder reference = referenceRecorder(64);
        final HybridPagedLatencyRecorder paged = pagedRecorder(64, pageBits, sparseEntries);
        for (int index = 0; index < latencies.length; index++) {
            reference.recordLatency(index, counts[index], counts[index] * 10, latencies[index]);
            paged.recordLatency(index, counts[index], counts[index] * 10, latencies[index]);
        }
        assertExtractionEquals(reference, paged);
    }

    private void assertReportedEquivalent(long[] latencies, int[] counts, int pageBits,
                                          int sparseEntries) {
        final LongHashMapLatencyRecorder reference = referenceRecorder(64);
        final HybridPagedLatencyRecorder paged = pagedRecorder(64, pageBits, sparseEntries);
        for (int index = 0; index < latencies.length; index++) {
            reference.reportLatency(latencies[index], counts[index]);
            paged.reportLatency(latencies[index], counts[index]);
        }
        assertExtractionEquals(reference, paged);
    }

    private void assertExtractionEquals(LongHashMapLatencyRecorder reference,
                                        HybridPagedLatencyRecorder paged) {
        final LatencyPercentiles referencePercentiles = new LatencyPercentiles(PERCENTILES);
        final LatencyPercentiles pagedPercentiles = new LatencyPercentiles(PERCENTILES);
        final LatencyCollector referenceCollector = new LatencyCollector();
        final LatencyCollector pagedCollector = new LatencyCollector();

        reference.copyPercentiles(referencePercentiles, referenceCollector);
        paged.copyPercentiles(pagedPercentiles, pagedCollector);

        assertEquals(referenceCollector.latencies, pagedCollector.latencies);
        assertEquals(referenceCollector.counts, pagedCollector.counts);
        assertArrayEquals(referencePercentiles.latencies, pagedPercentiles.latencies);
        assertArrayEquals(referencePercentiles.latenciesCount, pagedPercentiles.latenciesCount);
        assertEquals(referencePercentiles.medianLatency, pagedPercentiles.medianLatency);
    }

    private LongHashMapLatencyRecorder referenceRecorder(int maximumMemoryMiB) {
        return new LongHashMapLatencyRecorder(MINIMUM_LATENCY, MAXIMUM_LATENCY,
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, PERCENTILES,
                new NanoSeconds(), maximumMemoryMiB);
    }

    private HybridPagedLatencyRecorder pagedRecorder(int maximumMemoryMiB, int pageBits,
                                                      int sparseEntries) {
        return pagedRecorder(maximumMemoryMiB, pageBits, sparseEntries,
                MemoryLimitPolicy.RESET_WINDOW_WHEN_FULL);
    }

    private HybridPagedLatencyRecorder pagedRecorder(int maximumMemoryMiB, int pageBits,
                                                      int sparseEntries,
                                                      MemoryLimitPolicy memoryLimitPolicy) {
        return new HybridPagedLatencyRecorder(MINIMUM_LATENCY, MAXIMUM_LATENCY,
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, PERCENTILES,
                new NanoSeconds(), maximumMemoryMiB, pageBits, sparseEntries,
                memoryLimitPolicy);
    }

    /** Captures sorted exact buckets copied during percentile extraction. */
    private static final class LatencyCollector implements ReportLatencies {
        private final List<Long> latencies = new ArrayList<>();
        private final List<Long> counts = new ArrayList<>();

        @Override
        public void reportLatencyRecord(LatencyRecord record) {
        }

        @Override
        public void reportLatency(long latency, long count) {
            latencies.add(latency);
            counts.add(count);
        }
    }
}

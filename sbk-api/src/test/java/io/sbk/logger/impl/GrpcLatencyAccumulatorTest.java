/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import io.perl.data.Bytes;
import io.sbp.grpc.MessageLatenciesRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies exact primitive latency accumulation and protobuf conversion.
 */
final class GrpcLatencyAccumulatorTest {
    @Test
    void preservesExactCountsInPackedRepresentation() {
        final GrpcLatencyAccumulator accumulator =
                new GrpcLatencyAccumulator(16L * Bytes.BYTES_PER_MB);
        assertTrue(accumulator.recordIfFits(100, 2));
        assertTrue(accumulator.recordIfFits(200, 3));
        assertTrue(accumulator.recordIfFits(100, 5));

        final MessageLatenciesRecord.Builder packedBuilder =
                MessageLatenciesRecord.newBuilder();
        accumulator.writePacked(packedBuilder);
        final MessageLatenciesRecord packed = packedBuilder.build();
        final Map<Long, Long> packedValues = new HashMap<>();
        for (int index = 0; index < packed.getLatencyValuesCount(); index++) {
            packedValues.put(packed.getLatencyValues(index), packed.getLatencyCounts(index));
        }

        assertEquals(Map.of(100L, 7L, 200L, 3L), packedValues);
        assertEquals(2, accumulator.size());
    }

    @Test
    void usesConfiguredLimitAndReusesStorageAfterClear() {
        final GrpcLatencyAccumulator accumulator =
                new GrpcLatencyAccumulator(512);
        long latency = Long.MAX_VALUE;
        while (accumulator.recordIfFits(latency, Long.MAX_VALUE)) {
            latency--;
        }

        final MessageLatenciesRecord.Builder builder = maximumMetadataBuilder();
        accumulator.writePacked(builder);
        assertTrue(builder.build().getSerializedSize() > 512 * 3 / 4);
        accumulator.clear();
        assertEquals(0, accumulator.size());
    }

    @Test
    void exactThresholdKeepsWorstCasePackedRecordBelowTransportLimit() {
        final long maximumMessageBytes = 16L * Bytes.BYTES_PER_MB;
        final GrpcLatencyAccumulator accumulator =
                new GrpcLatencyAccumulator(maximumMessageBytes);
        long latency = Long.MAX_VALUE;
        while (accumulator.recordIfFits(latency, Long.MAX_VALUE)) {
            latency--;
        }
        final MessageLatenciesRecord.Builder builder = maximumMetadataBuilder();
        accumulator.writePacked(builder);

        assertTrue(builder.build().getSerializedSize() <= maximumMessageBytes);
        assertFalse(accumulator.recordIfFits(latency, Long.MAX_VALUE));
    }

    @Test
    void countVarintGrowthParticipatesInExactLimit() {
        final GrpcLatencyAccumulator accumulator = new GrpcLatencyAccumulator(245);
        assertTrue(accumulator.recordIfFits(1, 127));

        assertFalse(accumulator.recordIfFits(1, 1));
    }

    private static MessageLatenciesRecord.Builder maximumMetadataBuilder() {
        return MessageLatenciesRecord.newBuilder()
                .setClientID(-1)
                .setSequenceNumber(-1)
                .setWriters(-1)
                .setReaders(-1)
                .setMaxWriters(-1)
                .setMaxReaders(-1)
                .setWriteRequestBytes(-1)
                .setWriteRequestRecords(-1)
                .setReadRequestBytes(-1)
                .setReadRequestRecords(-1)
                .setWriteTimeoutEvents(-1)
                .setReadTimeoutEvents(-1)
                .setTotalRecords(-1)
                .setValidLatencyRecords(-1)
                .setLowerLatencyDiscardRecords(-1)
                .setHigherLatencyDiscardRecords(-1)
                .setInvalidLatencyRecords(-1)
                .setTotalBytes(-1)
                .setTotalLatency(-1)
                .setMinLatency(-1)
                .setMaxLatency(-1);
    }
}

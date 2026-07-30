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
        accumulator.record(100, 2);
        accumulator.record(200, 3);
        accumulator.record(100, 5);

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
    void appliesConservativeMessageHeadroomAndReusesStorageAfterClear() {
        final GrpcLatencyAccumulator accumulator =
                new GrpcLatencyAccumulator(100);
        accumulator.record(1, 1);
        accumulator.record(2, 1);
        accumulator.record(3, 1);
        accumulator.record(4, 1);

        assertTrue(accumulator.isFull());
        accumulator.clear();
        assertEquals(0, accumulator.size());
    }

    @Test
    void conservativeThresholdKeepsWorstCasePackedRecordBelowTransportLimit() {
        final long maximumMessageBytes = 16L * Bytes.BYTES_PER_MB;
        final GrpcLatencyAccumulator accumulator =
                new GrpcLatencyAccumulator(maximumMessageBytes);
        long latency = Long.MAX_VALUE;
        while (!accumulator.isFull()) {
            accumulator.record(latency--, Long.MAX_VALUE);
        }
        final MessageLatenciesRecord.Builder builder =
                MessageLatenciesRecord.newBuilder();
        accumulator.writePacked(builder);

        assertFalse(builder.build().getSerializedSize() > maximumMessageBytes);
    }
}

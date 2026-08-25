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

import com.google.protobuf.CodedOutputStream;
import io.sbp.grpc.MessageLatenciesRecord;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;

/**
 * Accumulates exact latency frequencies without boxing keys or values.
 *
 * <p>The protobuf builder is populated only when a batch is ready for
 * transport. This keeps protobuf allocation out of the per-measurement path.
 */
final class GrpcLatencyAccumulator {
    private static final int LATENCY_VALUES_FIELD_NUMBER = MessageLatenciesRecord.LATENCYVALUES_FIELD_NUMBER;
    private static final int LATENCY_COUNTS_FIELD_NUMBER = MessageLatenciesRecord.LATENCYCOUNTS_FIELD_NUMBER;
    private static final int MAXIMUM_METADATA_BYTES = maximumMetadataBytes();
    private final LongLongHashMap latencies;
    private final long maximumMessageBytes;
    private long latencyValuesBytes;
    private long latencyCountsBytes;

    /**
     * Creates a primitive latency accumulator.
     *
     * @param maximumMessageBytes configured maximum protobuf message size
     */
    GrpcLatencyAccumulator(long maximumMessageBytes) {
        this.latencies = new LongLongHashMap();
        this.maximumMessageBytes = maximumMessageBytes;
        this.latencyValuesBytes = 0;
        this.latencyCountsBytes = 0;
    }

    /**
     * Records an exact latency frequency when it fits in the configured
     * serialized protobuf limit.
     *
     * <p>The calculation uses the actual unsigned-varint widths of the packed
     * latency and count arrays. The non-latency fields are represented by
     * their schema-derived maximum serialized size, rather than reserving a
     * percentage of the configured message capacity.
     *
     * @param latency non-negative latency value
     * @param count number of records represented by the latency
     * @return {@code true} when recorded; {@code false} when the caller must
     *         send the current batch first
     */
    boolean recordIfFits(long latency, long count) {
        final long current = latencies.get(latency);
        final long projectedValuesBytes;
        final long projectedCountsBytes;
        if (current == 0) {
            projectedValuesBytes = latencyValuesBytes + unsignedVarintBytes(latency);
            projectedCountsBytes = latencyCountsBytes + unsignedVarintBytes(count);
        } else {
            final long updated = current + count;
            projectedValuesBytes = latencyValuesBytes;
            projectedCountsBytes = latencyCountsBytes
                    - unsignedVarintBytes(current) + unsignedVarintBytes(updated);
        }
        if (serializedBytes(projectedValuesBytes, projectedCountsBytes) > maximumMessageBytes) {
            return false;
        }
        if (current == 0) {
            latencies.put(latency, count);
        } else {
            latencies.addToValue(latency, count);
        }
        latencyValuesBytes = projectedValuesBytes;
        latencyCountsBytes = projectedCountsBytes;
        return true;
    }

    /**
     * Copies frequencies to packed primitive protobuf fields.
     *
     * @param builder destination message builder
     */
    void writePacked(MessageLatenciesRecord.Builder builder) {
        latencies.forEachKeyValue((latency, count) -> {
            builder.addLatencyValues(latency);
            builder.addLatencyCounts(count);
        });
    }

    /**
     * Returns the number of distinct exact latency values.
     *
     * @return distinct latency count
     */
    int size() {
        return latencies.size();
    }

    /** Clears frequencies while retaining the primitive backing table. */
    void clear() {
        if (latencies.notEmpty()) {
            latencies.clear();
        }
        latencyValuesBytes = 0;
        latencyCountsBytes = 0;
    }

    private static long serializedBytes(long valuesBytes, long countsBytes) {
        return MAXIMUM_METADATA_BYTES
                + packedFieldBytes(LATENCY_VALUES_FIELD_NUMBER, valuesBytes)
                + packedFieldBytes(LATENCY_COUNTS_FIELD_NUMBER, countsBytes);
    }

    private static long packedFieldBytes(int fieldNumber, long payloadBytes) {
        if (payloadBytes == 0) {
            return 0;
        }
        return CodedOutputStream.computeTagSize(fieldNumber)
                + unsignedVarintBytes(payloadBytes) + payloadBytes;
    }

    private static int unsignedVarintBytes(long value) {
        return CodedOutputStream.computeUInt64SizeNoTag(value);
    }

    private static int maximumMetadataBytes() {
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
                .setMaxLatency(-1)
                .build()
                .getSerializedSize();
    }
}

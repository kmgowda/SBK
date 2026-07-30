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

import io.sbp.grpc.MessageLatenciesRecord;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;

/**
 * Accumulates exact latency frequencies without boxing keys or values.
 *
 * <p>The protobuf builder is populated only when a batch is ready for
 * transport. This keeps protobuf allocation out of the per-measurement path.
 */
final class GrpcLatencyAccumulator {
    static final int PACKED_ENTRY_MAX_BYTES = 20;
    private final LongLongHashMap latencies;
    private final long maximumEstimatedBytes;
    private long estimatedBytes;

    /**
     * Creates a primitive latency accumulator.
     *
     * @param maximumMessageBytes configured maximum protobuf message size
     */
    GrpcLatencyAccumulator(long maximumMessageBytes) {
        this.latencies = new LongLongHashMap();
        this.maximumEstimatedBytes = maximumMessageBytes * 3 / 4;
        this.estimatedBytes = 0;
    }

    /**
     * Records an exact latency frequency.
     *
     * @param latency non-negative latency value
     * @param count number of records represented by the latency
     */
    void record(long latency, long count) {
        final long current = latencies.get(latency);
        if (current == 0) {
            latencies.put(latency, count);
            estimatedBytes += PACKED_ENTRY_MAX_BYTES;
        } else {
            latencies.addToValue(latency, count);
        }
    }

    /**
     * Reports whether the conservative packed-size threshold was reached.
     *
     * @return {@code true} when the current batch should be flushed
     */
    boolean isFull() {
        return estimatedBytes >= maximumEstimatedBytes;
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
        estimatedBytes = 0;
    }
}

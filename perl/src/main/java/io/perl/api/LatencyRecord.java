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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class that holds aggregated latency and count statistics for a
 * measurement window or total aggregation. It provides common fields and a
 * {@link #reset()} method to initialize or clear the counters.
 *
 * <p>Fields include totalRecords, totalBytes, valid/invalid/discard counters,
 * min/max latency and aggregated total latency used to compute averages.
 */
@Slf4j
abstract public sealed class LatencyRecord permits LatencyRecorder {

    /** Total number of records observed. */
    @Getter
    protected long totalRecords;

    /** Number of records counted as valid latency samples. */
    @Getter
    protected long validLatencyRecords;

    /** Number of latency samples discarded because they were below the minimum threshold. */
    @Getter
    protected long lowerLatencyDiscardRecords;

    /** Number of latency samples discarded because they exceeded the maximum threshold. */
    @Getter
    protected long higherLatencyDiscardRecords;

    /** Number of invalid/negative latency samples observed. */
    @Getter
    protected long invalidLatencyRecords;

    /** Cumulative bytes observed during the aggregation period. */
    @Getter
    protected long totalBytes;

    /** Sum of latencies of valid samples; used to compute averages. */
    @Getter
    protected long totalLatency;

    /** Minimum observed latency; starts at Long.MAX_VALUE until samples are recorded. */
    @Getter
    protected long minLatency;

    /** Maximum observed latency. */
    @Getter
    protected long maxLatency;

    /**
     * Construct and reset counters.
     */
    public LatencyRecord() {
        reset();
    }

    /**
     * Reset all recording variables to initial state.
     */
    final public void reset() {
        this.totalRecords = 0;
        this.validLatencyRecords = 0;
        this.lowerLatencyDiscardRecords = 0;
        this.higherLatencyDiscardRecords = 0;
        this.invalidLatencyRecords = 0;
        this.totalBytes = 0;
        this.minLatency = Long.MAX_VALUE;
        this.maxLatency = 0;
        this.totalLatency = 0;
    }

}

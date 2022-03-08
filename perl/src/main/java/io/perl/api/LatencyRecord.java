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
 * Sealed Class LatencyRecord.
 */
@Slf4j
abstract public sealed class LatencyRecord permits LatencyRecorder {

    /**
     * <code>long totalRecords</code>.
     */
    @Getter
    protected long totalRecords;

    /**
     * <code>long validLatencyRecords</code>.
     */
    @Getter
    protected long validLatencyRecords;

    /**
     * <code>lowerLatencyDiscardRecords</code>.
     */
    @Getter
    protected long lowerLatencyDiscardRecords;

    /**
     * <code>long higherLatencyDiscardRecords</code>.
     */
    @Getter
    protected long higherLatencyDiscardRecords;

    /**
     * <code>long invalidLatencyRecords</code>.
     */
    @Getter
    protected long invalidLatencyRecords;

    /**
     * <code>long totalBytes</code>.
     */
    @Getter
    protected long totalBytes;

    /**
     * <code>long totalLatency</code>.
     */
    @Getter
    protected long totalLatency;

    /**
     * <code>long maxLatency</code>.
     */
    @Getter
    protected long maxLatency;

    /**
     * Method to reset all recording variables.
     */
    public LatencyRecord() {
        reset();
    }

    /**
     * Reset all recording variables.
     */
    final public void reset() {
        this.totalRecords = 0;
        this.validLatencyRecords = 0;
        this.lowerLatencyDiscardRecords = 0;
        this.higherLatencyDiscardRecords = 0;
        this.invalidLatencyRecords = 0;
        this.totalBytes = 0;
        this.maxLatency = 0;
        this.totalLatency = 0;
    }

}

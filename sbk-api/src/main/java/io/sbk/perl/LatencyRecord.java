/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract public class LatencyRecord {
    @Getter
    protected long totalRecords;
    @Getter
    protected long validLatencyRecords;
    @Getter
    protected long lowerLatencyDiscardRecords;
    @Getter
    protected long higherLatencyDiscardRecords;
    @Getter
    protected long invalidLatencyRecords;
    @Getter
    protected long totalBytes;
    @Getter
    protected long totalLatency;
    @Getter
    protected long maxLatency;

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

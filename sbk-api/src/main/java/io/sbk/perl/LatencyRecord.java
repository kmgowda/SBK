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

public class LatencyRecord {
    public long totalRecords;
    public long validLatencyRecords;
    public long lowerLatencyDiscardRecords;
    public long higherLatencyDiscardRecords;
    public long invalidLatencyRecords;
    public long totalBytes;
    public long totalLatency;
    public long maxLatency;


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

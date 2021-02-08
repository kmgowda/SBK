/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.BiConsumer;

@NotThreadSafe
public abstract class LatencyStore extends LatencyRecorder {
    public final double[] percentiles;

    LatencyStore(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                 double[] percentiles) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax);
        this.percentiles = percentiles;
    }

    @Override
    public void reset() {
        super.reset();
    }

    /**
     * get the Percentiles.
     * @param copyLatencies  Copy Latency values.
     * @return Array of percentiles.
     */
    abstract public long[] getPercentiles(BiConsumer<Long, Long> copyLatencies);
}

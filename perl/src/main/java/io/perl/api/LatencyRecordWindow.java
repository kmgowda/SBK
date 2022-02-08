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

import io.time.Time;

import javax.annotation.concurrent.NotThreadSafe;


@NotThreadSafe
public abstract non-sealed class LatencyRecordWindow extends LatencyWindow implements ReportLatency, ReportLatencies {

    public LatencyRecordWindow(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax,
                               long bytesMax, double[] percentilesFractions, Time time) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentilesFractions, time);
    }

}

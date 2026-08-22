/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api.impl;

import io.perl.api.LatencyRecordWindow;
import io.perl.api.PeriodicRecorder;
import io.perl.logger.Print;
import io.perl.logger.ReportLatency;
import io.time.Time;

/**
 * Periodic recorder specialized for PerL's bounded array window and a logger
 * that consumes individual latency values.
 *
 * <p>The array storage is preallocated and PerL resets it at every reporting
 * interval. Construction selects this implementation only for PerL's array
 * window with effectively unreachable counter limits, avoiding the generic
 * overflow check on each measurement while retaining the required logger
 * callback.
 */
public final class ArrayWindowLatencyPeriodicRecorder
        extends TotalLatencyRecordWindow implements PeriodicRecorder {
    private final Time time;
    private final ReportLatency reportLatency;

    /**
     * Creates an array-window recorder with an individual-latency callback.
     *
     * @param window periodic array latency window
     * @param totalWindow total latency window
     * @param windowLogger periodic result logger
     * @param totalLogger total result logger
     * @param reportLatency individual-latency callback
     * @param time benchmark clock
     */
    public ArrayWindowLatencyPeriodicRecorder(
            ArrayLatencyRecorder window, LatencyRecordWindow totalWindow,
            Print windowLogger, Print totalLogger,
            ReportLatency reportLatency, Time time) {
        super(window, totalWindow, windowLogger, totalLogger);
        this.reportLatency = reportLatency;
        this.time = time;
    }

    @Override
    public void record(long startTime, long endTime, int events, int bytes) {
        final long latency = time.elapsed(endTime, startTime);
        reportLatency.recordLatency(startTime, events, bytes, latency);
        window.recordLatency(startTime, events, bytes, latency);
    }
}

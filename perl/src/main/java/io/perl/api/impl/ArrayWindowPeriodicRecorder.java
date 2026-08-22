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
import io.time.Time;

/**
 * Periodic recorder specialized for PerL's bounded array window and loggers
 * that do not consume individual latency values.
 *
 * <p>The array storage cannot exhaust its preallocated capacity. PerL creates
 * this recorder with effectively unreachable counter limits and resets the
 * window at every reporting interval, so the generic per-event overflow
 * check is redundant. Keeping that decision at construction removes three
 * counter comparisons from every recorded measurement.
 */
public final class ArrayWindowPeriodicRecorder extends TotalLatencyRecordWindow
        implements PeriodicRecorder {
    private final Time time;

    /**
     * Creates an array-window recorder without an individual-latency callback.
     *
     * @param window periodic array latency window
     * @param totalWindow total latency window
     * @param windowLogger periodic result logger
     * @param totalLogger total result logger
     * @param time benchmark clock
     */
    public ArrayWindowPeriodicRecorder(ArrayLatencyRecorder window,
                                       LatencyRecordWindow totalWindow,
                                       Print windowLogger, Print totalLogger,
                                       Time time) {
        super(window, totalWindow, windowLogger, totalLogger);
        this.time = time;
    }

    @Override
    public void record(long startTime, long endTime, int events, int bytes) {
        window.recordLatency(startTime, events, bytes,
                time.elapsed(endTime, startTime));
    }
}

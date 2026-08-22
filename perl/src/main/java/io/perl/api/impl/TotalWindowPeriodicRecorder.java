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
 * Periodic recorder for loggers that do not consume individual latency values.
 */
public final class TotalWindowPeriodicRecorder extends TotalWindowLatencyRecorder implements PeriodicRecorder {
    private final Time time;

    /**
     * Creates a periodic recorder without a per-event logger callback.
     *
     * @param window periodic latency window
     * @param totalWindow total latency window
     * @param windowLogger periodic result logger
     * @param totalLogger total result logger
     * @param time benchmark clock
     */
    public TotalWindowPeriodicRecorder(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                       Print windowLogger, Print totalLogger, Time time) {
        super(window, totalWindow, windowLogger, totalLogger);
        this.time = time;
    }

    @Override
    public void record(long startTime, long endTime, int events, int bytes) {
        recordLatency(startTime, events, bytes, time.elapsed(endTime, startTime));
    }
}

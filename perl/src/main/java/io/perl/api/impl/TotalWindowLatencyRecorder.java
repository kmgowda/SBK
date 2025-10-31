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
import io.perl.logger.Print;
import io.perl.logger.ReportLatency;

/**
 * Recorder that forwards incoming latency samples to a windowed storage and
 * checks whether the window storage is full to trigger periodic resets.
 * This class also maintains a total aggregated window for end-of-run
 * reporting.
 */
public sealed class TotalWindowLatencyRecorder extends TotalLatencyRecordWindow
        implements ReportLatency permits TotalWindowLatencyPeriodicRecorder {

    /**
     * Construct recorder with per-window and total storage plus loggers.
     *
     * @param window       per-window latency store
     * @param totalWindow  total-window latency store used for final aggregation
     * @param windowLogger logger used for periodic window-level printing
     * @param totalLogger  logger used for total-level printing
     */
    public TotalWindowLatencyRecorder(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                      Print windowLogger, Print totalLogger) {
        super(window, totalWindow, windowLogger, totalLogger);
    }

    @Override
    public void recordLatency(long startTime, int events, int bytes, long latency) {
        window.recordLatency(startTime, events, bytes, latency);
        checkWindowFullAndReset(startTime);
    }
}

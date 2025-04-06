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
 * Class TotalWindowLatencyRecorder.
 */
public sealed class TotalWindowLatencyRecorder extends TotalLatencyRecordWindow
        implements ReportLatency permits TotalWindowLatencyPeriodicRecorder {

    /**
     * Constructor TotalWindowLatencyRecorder passing all values to its super class.
     *
     * @param window                LatencyRecordWindow
     * @param totalWindow           LatencyRecordWindow
     * @param windowLogger          Print
     * @param totalLogger           Print
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

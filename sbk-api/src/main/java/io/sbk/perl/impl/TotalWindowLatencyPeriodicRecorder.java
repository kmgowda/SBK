/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl.impl;

import io.sbk.perl.LatencyRecordWindow;
import io.sbk.perl.PeriodicRecorder;
import io.sbk.perl.Print;
import io.sbk.perl.ReportLatency;
import io.sbk.time.Time;

public final class TotalWindowLatencyPeriodicRecorder extends TotalWindowLatencyRecorder implements PeriodicRecorder {
    private final Time time;
    private final ReportLatency reportLatency;

    public TotalWindowLatencyPeriodicRecorder(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                              Print windowLogger, Print totalLogger,
                                              ReportLatency reportLatency, Time time) {
        super(window, totalWindow, windowLogger, totalLogger);
        this.reportLatency = reportLatency;
        this.time = time;
    }

    @Override
    public void record(long startTime, long endTime, int bytes, int events) {
        final long latency = time.elapsed(endTime, startTime);
        this.reportLatency.recordLatency(startTime, bytes, events, latency);
        recordLatency(startTime, bytes, events, latency);
    }
}

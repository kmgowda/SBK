/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.impl;

import io.perl.LatencyRecordWindow;
import io.perl.PeriodicLogger;
import io.perl.Print;
import io.perl.ReportLatency;
import io.time.Time;

public final class TotalWindowLatencyPeriodicLogger extends TotalWindowLatencyRecorder implements PeriodicLogger {
    private final Time time;
    private final ReportLatency reportLatency;

    public TotalWindowLatencyPeriodicLogger(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
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

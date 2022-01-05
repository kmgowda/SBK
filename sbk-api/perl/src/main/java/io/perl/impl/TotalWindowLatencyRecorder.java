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
import io.perl.Print;
import io.perl.ReportLatency;

public sealed class TotalWindowLatencyRecorder extends TotalLatencyRecordWindow
        implements ReportLatency permits TotalWindowLatencyPeriodicLogger {

    public TotalWindowLatencyRecorder(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                      Print windowLogger, Print totalLogger) {
        super(window, totalWindow, windowLogger, totalLogger);
    }

    @Override
    public void recordLatency(long startTime, int bytes, int events, long latency) {
        window.recordLatency(startTime, bytes, events, latency);
        checkWindowFullAndReset(startTime);
    }
}

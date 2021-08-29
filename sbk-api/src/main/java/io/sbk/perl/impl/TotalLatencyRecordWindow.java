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
import io.sbk.perl.Print;
import io.sbk.perl.TotalPeriodicWindow;

public class TotalLatencyRecordWindow implements TotalPeriodicWindow {
    final protected LatencyRecordWindow window;
    final protected LatencyRecordWindow totalWindow;
    final protected Print windowLogger;
    final protected Print totalLogger;

    public TotalLatencyRecordWindow(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                    Print windowLogger, Print totalLogger) {
        this.window = window;
        this.totalWindow = totalWindow;
        this.windowLogger = windowLogger;
        this.totalLogger = totalLogger;
    }

    public void checkWindowFullAndReset(long currTime) {
        if (window.isFull()) {
            stopWindow(currTime);
            startWindow(currTime);
        }
    }

    public void checkTotalWindowFullAndReset(long currTime) {
        if (totalWindow.isFull()) {
            stop(currTime);
            start(currTime);
        }
    }

    @Override
    public void startWindow(long startTime) {
        window.reset(startTime);
    }

    @Override
    public long elapsedMilliSecondsWindow(long currentTime) {
        return window.elapsedMilliSeconds(currentTime);
    }

    @Override
    public void stopWindow(long stopTime) {
        window.print(stopTime, windowLogger, totalWindow);
        checkTotalWindowFullAndReset(stopTime);
    }

    @Override
    public void start(long startTime) {
        totalWindow.reset(startTime);
    }

    @Override
    public void stop(long endTime) {
        if (window.getTotalRecords() > 0) {
            stopWindow(endTime);
        }
        totalWindow.print(endTime, totalLogger, null);
    }
}

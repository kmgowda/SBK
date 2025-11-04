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
import io.perl.api.TotalPeriodicWindow;

/**
 * Combines a per-window latency store and a total-window store and provides
 * the PeriodicWindow/TotalPeriodicWindow lifecycle semantics. This class is
 * used by the periodic recorder to manage window resets and to print both
 * window-level and total-level metrics.
 */
public class TotalLatencyRecordWindow implements TotalPeriodicWindow {

    /**
     * <code>LatencyRecordWindow window</code>.
     */
    final protected LatencyRecordWindow window;

    /**
     * <code>LatencyRecordWindow totalWindow</code>.
     */
    final protected LatencyRecordWindow totalWindow;

    /**
     * <code>Print windowLogger</code>.
     */
    final protected Print windowLogger;

    /**
     * <code>Print totalLogger</code>.
     */
    final protected Print totalLogger;

    /**
     * Construct with window and total-window storage and corresponding loggers.
     *
     * @param window            LatencyRecordWindow
     * @param totalWindow       LatencyRecordWindow
     * @param windowLogger      Print
     * @param totalLogger       Print
     */
    public TotalLatencyRecordWindow(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                    Print windowLogger, Print totalLogger) {
        this.window = window;
        this.totalWindow = totalWindow;
        this.windowLogger = windowLogger;
        this.totalLogger = totalLogger;
    }

    /**
     * Check if the current window is full and, if so, stop and restart it.
     *
     * @param currTime long
     */
    public void checkWindowFullAndReset(long currTime) {
        if (window.isFull()) {
            stopWindow(currTime);
            startWindow(currTime);
        }
    }

    /**
     * Check if the total window is full and flush/reset its contents.
     *
     * @param currTime long
     */
    public void checkTotalWindowFullAndReset(long currTime) {
        if (totalWindow.isFull()) {
            /* don't call stop here , it may cause recursion */
            totalWindow.print(currTime, totalLogger, null);
            totalWindow.reset(currTime);
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
            /* don't call stopWindow , it leads to recursion  */
            window.print(endTime, windowLogger, totalWindow);
            window.reset(endTime);
        }
        totalWindow.print(endTime, totalLogger, null);
    }
}

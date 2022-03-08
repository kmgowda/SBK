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
 * Class TotalLatencyRecordWindow.
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
     * Constructor TotalLatencyRecordWindow initialize all values.
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
     * This method check if window is full and Stop the Recording window.
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
     * This method checks if totalWindow is full and Stop the Total Window.
     *
     * @param currTime long
     */
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

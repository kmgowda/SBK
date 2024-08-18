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

import io.perl.api.Channel;
import io.perl.api.PerformanceRecorder;
import io.perl.api.PeriodicRecorder;
import io.perl.api.TimeStamp;
import io.perl.system.PerlPrinter;
import io.time.Time;

import javax.annotation.Nonnull;

public class PerformanceRecorderIdleSleep extends PerformanceRecorder {
    final private int sleepMS;

    /**
     * Constructor to initialize values.
     *
     * @param periodicRecorder    PeriodicRecorder
     * @param channels            Channel[]
     * @param time                Time
     * @param reportingIntervalMS int
     * @param sleepMS             int
     */
    public PerformanceRecorderIdleSleep(PeriodicRecorder periodicRecorder, @Nonnull Channel[] channels, Time time,
                                        int reportingIntervalMS, int sleepMS) {
        super(periodicRecorder, channels, time, reportingIntervalMS);
        this.sleepMS = sleepMS;
    }

    /**
     * Method run.
     *
     * @param secondsToRun final long.
     * @param totalRecords final long.
     */
    public void run(final long secondsToRun, final long totalRecords) {
        final long msToRun = secondsToRun * Time.MS_PER_SEC;
        final long startTime = time.getCurrentTime();
        boolean doWork = true;
        long ctime = startTime;
        long recordsCnt = 0;
        boolean notFound;
        TimeStamp t;
        PerlPrinter.log.info("PerformanceRecorderIdleSleep Started : {} milliseconds idle sleep",
                this.sleepMS);
        periodicRecorder.start(startTime);
        periodicRecorder.startWindow(startTime);
        while (doWork) {
            notFound = true;
            for (int i = 0; doWork && (i < channels.length); i++) {
                t = channels[i].receive(windowIntervalMS);
                if (t != null) {
                    notFound = false;
                    ctime = t.endTime;
                    if (t.isEnd()) {
                        doWork = false;
                    } else {
                        recordsCnt += t.records;
                        periodicRecorder.record(t.startTime, t.endTime, t.records, t.bytes);
                        if (msToRun > 0) {
                            if (time.elapsedMilliSeconds(ctime, startTime) >= msToRun) {
                                doWork = false;
                            }
                        } else if (totalRecords > 0 && recordsCnt >= totalRecords) {
                            doWork = false;
                        }
                    }
                    if (periodicRecorder.elapsedMilliSecondsWindow(ctime) > windowIntervalMS) {
                        periodicRecorder.stopWindow(ctime);
                        periodicRecorder.startWindow(ctime);
                    }
                }
            }
            if (doWork) {
                if (notFound) {
                    try {
                        Thread.sleep(this.sleepMS);
                    } catch (InterruptedException e) {
                        PerlPrinter.log.warn("PerformanceRecorderIdleSleep : {}", e.getMessage());
                    }
                    ctime = time.getCurrentTime();
                    final long diffTime = periodicRecorder.elapsedMilliSecondsWindow(ctime);
                    if (diffTime > windowIntervalMS) {
                        periodicRecorder.stopWindow(ctime);
                        periodicRecorder.startWindow(ctime);
                    }
                }
                if (msToRun > 0 && time.elapsedMilliSeconds(ctime, startTime) >= msToRun) {
                    doWork = false;
                }
            }
        }
        periodicRecorder.stop(ctime);
        PerlPrinter.log.info("PerformanceRecorderIdleSleep Exited");
    }
}

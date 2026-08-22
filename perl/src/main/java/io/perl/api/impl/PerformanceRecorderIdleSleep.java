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

/**
 * Performance recorder that sleeps briefly when no channel has data.
 */
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
     * @param idleTimeoutSeconds  maximum interval without a performance event
     */
    public PerformanceRecorderIdleSleep(PeriodicRecorder periodicRecorder, @Nonnull Channel[] channels, Time time,
                                        int reportingIntervalMS, int sleepMS, int idleTimeoutSeconds) {
        super(periodicRecorder, channels, time, reportingIntervalMS, idleTimeoutSeconds);
        this.sleepMS = sleepMS;
    }

    /**
     * Method run.
     *
     * @param secondsToRun final long.
     * @param totalRecords final long.
     */
    public void run(final long secondsToRun, final long totalRecords) {
        PerlPrinter.log.info("PerformanceRecorderIdleSleep Started : {} milliseconds idle sleep",
                this.sleepMS);
        // Keep the mode decision outside the latency-critical consumer loops. Combining these paths would add
        // idle-timeout bookkeeping to every duration-mode timestamp and measurably reduce recorder throughput.
        if (secondsToRun == 0 && totalRecords > 0) {
            runForRecords(totalRecords);
        } else {
            runForDuration(secondsToRun);
        }
        PerlPrinter.log.info("PerformanceRecorderIdleSleep Exited");
    }

    /**
     * Runs the duration-based recorder without fixed-record idle-timeout bookkeeping.
     *
     * @param secondsToRun benchmark duration, or zero to consume until an end marker
     */
    private void runForDuration(final long secondsToRun) {
        final long timeUnitsToRun = time.secondsToTimeUnits(secondsToRun);
        final long startTime = time.getCurrentTime();
        boolean doWork = true;
        long ctime = startTime;
        boolean notFound;
        TimeStamp t;
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
                        periodicRecorder.record(t.startTime, t.endTime, t.records, t.bytes);
                        if (timeUnitsToRun > 0 && time.elapsed(ctime, startTime) >= timeUnitsToRun) {
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
                if (timeUnitsToRun > 0 && time.elapsed(ctime, startTime) >= timeUnitsToRun) {
                    doWork = false;
                }
            }
        }
        periodicRecorder.stop(ctime);
    }

    /**
     * Runs the fixed-record recorder with idle-timeout detection.
     *
     * @param totalRecords target record count
     */
    private void runForRecords(final long totalRecords) {
        final long startTime = time.getCurrentTime();
        boolean doWork = true;
        long ctime = startTime;
        long lastEventTime = startTime;
        long recordsCnt = 0;
        long observedRecordsCnt = 0;
        boolean notFound;
        TimeStamp t;
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
                        if (recordsCnt >= totalRecords) {
                            doWork = false;
                        }
                    }
                    if (periodicRecorder.elapsedMilliSecondsWindow(ctime) > windowIntervalMS) {
                        periodicRecorder.stopWindow(ctime);
                        periodicRecorder.startWindow(ctime);
                    }
                }
            }
            if (doWork && notFound) {
                try {
                    Thread.sleep(this.sleepMS);
                } catch (InterruptedException e) {
                    PerlPrinter.log.warn("PerformanceRecorderIdleSleep : {}", e.getMessage());
                }
                ctime = time.getCurrentTime();
                if (recordsCnt > observedRecordsCnt) {
                    observedRecordsCnt = recordsCnt;
                    lastEventTime = ctime;
                }
                checkIdleTimeout(ctime, lastEventTime);
                final long diffTime = periodicRecorder.elapsedMilliSecondsWindow(ctime);
                if (diffTime > windowIntervalMS) {
                    periodicRecorder.stopWindow(ctime);
                    periodicRecorder.startWindow(ctime);
                }
            }
        }
        periodicRecorder.stop(ctime);
    }
}

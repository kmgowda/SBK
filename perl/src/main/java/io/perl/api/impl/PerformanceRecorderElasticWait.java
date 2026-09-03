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
import io.perl.config.PerlConfig;
import io.perl.system.PerlPrinter;
import io.perl.api.TimeStamp;
import io.time.Time;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Single-consumer performance recorder with adaptive idle parking.
 *
 * <p>Available timestamps are drained without an additional recorder clock
 * read. When every channel is empty, {@link ElasticWait} parks for the
 * configured idle duration and amortizes subsequent clock checks.</p>
 */
@NotThreadSafe
public final class PerformanceRecorderElasticWait extends PerformanceRecorder {
    final private int idleNS;

    /**
     * Creates an elastic-wait performance recorder.
     *
     * @param periodicRecorder periodic result recorder
     * @param channels timestamp channels to consume
     * @param time benchmark clock
     * @param reportingIntervalMS reporting-window duration in milliseconds
     * @param idleNS duration of each empty-channel park in nanoseconds
     * @param idleTimeoutSeconds maximum interval without a performance event
     */
    public PerformanceRecorderElasticWait(PeriodicRecorder periodicRecorder, @Nonnull Channel[] channels, Time time,
                                          int reportingIntervalMS, int idleNS, int idleTimeoutSeconds) {
        super(periodicRecorder, channels, time, reportingIntervalMS, idleTimeoutSeconds);
        this.idleNS = idleNS;
    }

    /**
     * Consumes timestamps until the configured duration or record count ends.
     *
     * @param secondsToRun benchmark duration, or zero for record-count mode
     * @param totalRecords target record count, or zero for duration mode
     */
    public void run(final long secondsToRun, final long totalRecords) {
        PerlPrinter.log.info("PerformanceRecorderElasticWait Started : {} nanoseconds adaptive idle park",
                this.idleNS);
        // Keep the mode decision outside the latency-critical consumer loops. Combining these paths would add
        // idle-timeout bookkeeping to every duration-mode timestamp and measurably reduce recorder throughput.
        if (secondsToRun == 0 && totalRecords > 0) {
            runForRecords(totalRecords);
        } else {
            runForDuration(secondsToRun);
        }
        PerlPrinter.log.info("PerformanceRecorderElasticWait Exited");
    }

    /**
     * Runs the duration-based recorder without fixed-record idle-timeout bookkeeping.
     *
     * @param secondsToRun benchmark duration, or zero to consume until an end marker
     */
    private void runForDuration(final long secondsToRun) {
        final long timeUnitsToRun = time.secondsToTimeUnits(secondsToRun);
        final ElasticWait idleWait = new ElasticWait(idleNS, windowIntervalMS,
                Math.min(windowIntervalMS, PerlConfig.DEFAULT_TIMEOUT_MS));
        final long startTime = time.getCurrentTime();
        boolean doWork = true;
        long ctime = startTime;
        boolean notFound;
        boolean dataSinceIdle = false;
        TimeStamp t;
        periodicRecorder.start(startTime);
        periodicRecorder.startWindow(startTime);
        while (doWork) {
            notFound = true;
            for (int i = 0; doWork && (i < channels.length); i++) {
                t = channels[i].receive(0);
                if (t != null) {
                    notFound = false;
                    dataSinceIdle = true;
                    ctime = t.endTime;
                    if (t.isEnd()) {
                        doWork = false;
                    } else {
                        periodicRecorder.record(t.startTime, t.endTime, t.records, t.bytes);
                        if (timeUnitsToRun > 0 && time.elapsed(ctime, startTime) >= timeUnitsToRun) {
                            doWork = false;
                        }
                    }
                    if (periodicRecorder.elapsedMilliSecondsWindow(ctime) >= windowIntervalMS) {
                        periodicRecorder.stopWindow(ctime);
                        periodicRecorder.startWindow(ctime);
                        idleWait.reset();
                        dataSinceIdle = false;
                    }
                }
            }
            if (doWork) {
                if (notFound) {
                    if (dataSinceIdle) {
                        idleWait.startIdle(
                                periodicRecorder.elapsedMilliSecondsWindow(
                                        ctime));
                        dataSinceIdle = false;
                    }
                    if (idleWait.waitAndCheck()) {
                        ctime = time.getCurrentTime();
                        final long diffTime = periodicRecorder.elapsedMilliSecondsWindow(ctime);
                        if (diffTime >= windowIntervalMS) {
                            periodicRecorder.stopWindow(ctime);
                            periodicRecorder.startWindow(ctime);
                            idleWait.setElastic(diffTime);
                        } else {
                            idleWait.updateElastic(diffTime);
                        }
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
        final ElasticWait idleWait = new ElasticWait(idleNS, windowIntervalMS,
                Math.min(windowIntervalMS, PerlConfig.DEFAULT_TIMEOUT_MS));
        final long startTime = time.getCurrentTime();
        boolean doWork = true;
        long ctime = startTime;
        long lastEventTime = startTime;
        long recordsCnt = 0;
        long observedRecordsCnt = 0;
        boolean notFound;
        boolean dataSinceIdle = false;
        TimeStamp t;
        periodicRecorder.start(startTime);
        periodicRecorder.startWindow(startTime);
        while (doWork) {
            notFound = true;
            for (int i = 0; doWork && (i < channels.length); i++) {
                t = channels[i].receive(0);
                if (t != null) {
                    notFound = false;
                    dataSinceIdle = true;
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
                    if (periodicRecorder.elapsedMilliSecondsWindow(ctime) >= windowIntervalMS) {
                        periodicRecorder.stopWindow(ctime);
                        periodicRecorder.startWindow(ctime);
                        idleWait.reset();
                        dataSinceIdle = false;
                    }
                }
            }
            if (doWork && notFound) {
                if (dataSinceIdle) {
                    idleWait.startIdle(periodicRecorder.elapsedMilliSecondsWindow(ctime));
                    dataSinceIdle = false;
                }
                if (recordsCnt > observedRecordsCnt) {
                    observedRecordsCnt = recordsCnt;
                    lastEventTime = ctime;
                }
                if (idleWait.waitAndCheck()) {
                    ctime = time.getCurrentTime();
                    checkIdleTimeout(ctime, lastEventTime);
                    final long diffTime = periodicRecorder.elapsedMilliSecondsWindow(ctime);
                    if (diffTime >= windowIntervalMS) {
                        periodicRecorder.stopWindow(ctime);
                        periodicRecorder.startWindow(ctime);
                        idleWait.setElastic(diffTime);
                    } else {
                        idleWait.updateElastic(diffTime);
                    }
                }
            }
        }
        periodicRecorder.stop(ctime);
    }
}

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
import io.perl.api.PeriodicRecorder;
import io.perl.config.PerlConfig;
import io.perl.system.PerlPrinter;
import io.perl.api.TimeStamp;
import io.time.Time;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Class for Performance Recording.
 */
@NotThreadSafe
public final class PerformanceRecorder {
    final private int windowIntervalMS;
    final private int idleNS;
    final private Time time;
    final private PeriodicRecorder periodicRecorder;
    final private Channel[] channels;

    public PerformanceRecorder(PeriodicRecorder periodicRecorder, Channel[] channels, Time time,
                               int reportingIntervalMS, int idleNS) {
        this.periodicRecorder = periodicRecorder;
        this.channels = channels;
        this.time = time;
        this.windowIntervalMS = reportingIntervalMS;
        this.idleNS = idleNS;
    }

    public void run(final long secondsToRun, final long totalRecords) {
        final long msToRun = secondsToRun * Time.MS_PER_SEC;
        final ElasticWait idleWait = new ElasticWait(idleNS, windowIntervalMS,
                Math.min(windowIntervalMS, PerlConfig.DEFAULT_TIMEOUT_MS));
        final long startTime = time.getCurrentTime();
        boolean doWork = true;
        long ctime = startTime;
        long recordsCnt = 0;
        boolean notFound;
        TimeStamp t;
        PerlPrinter.log.info("Performance Recorder Started");
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
                        periodicRecorder.record(t.startTime, t.endTime, t.bytes, t.records);
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
                        idleWait.reset();
                    }
                }
            }
            if (doWork) {
                if (notFound) {
                    if (idleWait.waitAndCheck()) {
                        ctime = time.getCurrentTime();
                        final long diffTime = periodicRecorder.elapsedMilliSecondsWindow(ctime);
                        if (diffTime > windowIntervalMS) {
                            periodicRecorder.stopWindow(ctime);
                            periodicRecorder.startWindow(ctime);
                            idleWait.reset();
                            idleWait.setElastic(diffTime);
                        } else {
                            idleWait.updateElastic(diffTime);
                        }
                    }
                }
                if (msToRun > 0 && time.elapsedMilliSeconds(ctime, startTime) >= msToRun) {
                    doWork = false;
                }
            }
        }
        periodicRecorder.stop(ctime);
        PerlPrinter.log.info("Performance Recorder Exited");
    }

}

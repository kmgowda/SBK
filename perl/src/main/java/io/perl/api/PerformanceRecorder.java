/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api;
import io.time.Time;

import javax.annotation.Nonnull;

abstract public class PerformanceRecorder {
    final protected int windowIntervalMS;
    final protected Time time;
    final protected PeriodicRecorder periodicRecorder;
    final protected Channel[] channels;

    /**
     * Constructor to initialize values.
     *
     * @param periodicRecorder      PeriodicRecorder
     * @param channels              Channel[]
     * @param time                  Time
     * @param reportingIntervalMS   int
     */
    public PerformanceRecorder(PeriodicRecorder periodicRecorder, @Nonnull Channel[] channels, Time time,
                                       int reportingIntervalMS) {
        this.periodicRecorder = periodicRecorder;
        this.channels = channels.clone();
        this.time = time;
        this.windowIntervalMS = reportingIntervalMS;
    }

    /**
     * Method run.
     *
     * @param secondsToRun      final long.
     * @param totalRecords      final long.
     */
    abstract public void run(final long secondsToRun, final long totalRecords);

}

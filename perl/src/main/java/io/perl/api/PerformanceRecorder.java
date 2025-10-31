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

/**
 * Base class for implementations that collect events from multiple channels
 * and drive periodic recorders. Concrete subclasses implement the dispatching
 * loop in {@link #run(long, long)} and manage channel consumption semantics
 * (for example busy-wait vs sleep strategies).
 */
abstract public class PerformanceRecorder {
    final protected int windowIntervalMS;
    final protected Time time;
    final protected PeriodicRecorder periodicRecorder;
    final protected Channel[] channels;

    /**
     * Constructor to initialize performance recorder.
     *
     * @param periodicRecorder      periodic aggregator/recorder
     * @param channels              array of channels (one per worker/thread)
     * @param time                  time helper for conversions
     * @param reportingIntervalMS   reporting interval in milliseconds
     */
    public PerformanceRecorder(PeriodicRecorder periodicRecorder, @Nonnull Channel[] channels, Time time,
                                       int reportingIntervalMS) {
        this.periodicRecorder = periodicRecorder;
        this.channels = channels.clone();
        this.time = time;
        this.windowIntervalMS = reportingIntervalMS;
    }

    /**
     * Main loop that consumes channel events and updates periodic recorders.
     * Implementations must honor the provided {@code secondsToRun} /
     * {@code totalRecords} termination semantics.
     *
     * @param secondsToRun final long seconds to run (0 for record-count-based run)
     * @param totalRecords total number of records when running in count-based mode
     */
    abstract public void run(final long secondsToRun, final long totalRecords);

}

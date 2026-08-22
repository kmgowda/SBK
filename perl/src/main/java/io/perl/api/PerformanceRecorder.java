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
import io.perl.exception.BenchmarkIdleTimeoutException;
import io.time.Time;

import javax.annotation.Nonnull;

/**
 * Base class for implementations that collect events from multiple channels
 * and drive periodic recorders. Concrete subclasses implement the dispatching
 * loop in {@link #run(long, long)} and manage channel consumption semantics
 * (for example busy-wait vs sleep strategies).
 */
abstract public class PerformanceRecorder {
    /** Periodic reporting interval in milliseconds. */
    final protected int windowIntervalMS;
    /** Time source used by the recorder. */
    final protected Time time;
    /** Aggregator receiving samples from all channels. */
    final protected PeriodicRecorder periodicRecorder;
    /** Producer channels consumed by this recorder. */
    final protected Channel[] channels;
    /** Maximum duration without a performance event, in milliseconds. */
    final private long idleTimeoutMS;
    /** Configured idle deadline retained for diagnostics. */
    final private int idleTimeoutSeconds;

    /**
     * Constructor to initialize performance recorder.
     *
     * @param periodicRecorder      periodic aggregator/recorder
     * @param channels              array of channels (one per worker/thread)
     * @param time                  time helper for conversions
     * @param reportingIntervalMS   reporting interval in milliseconds
     * @param idleTimeoutSeconds    maximum interval without a performance event
     * @throws IllegalArgumentException when the idle timeout is not positive or does not exceed the reporting interval
     */
    public PerformanceRecorder(PeriodicRecorder periodicRecorder, @Nonnull Channel[] channels, Time time,
                               int reportingIntervalMS, int idleTimeoutSeconds) {
        if (idleTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("PerL idle timeout seconds must be greater than zero");
        }
        final long configuredIdleTimeoutMS = Math.multiplyExact(
                (long) idleTimeoutSeconds, Time.MS_PER_SEC);
        if (configuredIdleTimeoutMS <= reportingIntervalMS) {
            throw new IllegalArgumentException("PerL idle timeout seconds must be greater than the reporting "
                    + "interval of " + reportingIntervalMS + " milliseconds");
        }
        this.periodicRecorder = periodicRecorder;
        this.channels = channels.clone();
        this.time = time;
        this.windowIntervalMS = reportingIntervalMS;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
        this.idleTimeoutMS = configuredIdleTimeoutMS;
    }

    /**
     * Fails an idle consumer after the configured interval.
     *
     * <p>This method is called only from an implementation's existing empty-channel slow path.
     * It is never called for a successful queue dequeue or latency-window update.</p>
     *
     * @param currentTime current benchmark time
     * @param lastEventTime time of the most recently consumed performance event
     * @throws BenchmarkIdleTimeoutException when the configured idle interval has elapsed
     */
    final protected void checkIdleTimeout(long currentTime, long lastEventTime) {
        if (time.elapsedMilliSeconds(currentTime, lastEventTime) >= idleTimeoutMS) {
            throw new BenchmarkIdleTimeoutException(idleTimeoutSeconds);
        }
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

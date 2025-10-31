/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.sbk.api.RateController;
import io.time.Time;

/**
 * Simple rate controller used by SBK to pace operations to a target
 * records-per-second value.
 *
 * <p>The controller computes an average inter-record sleep time in
 * nanoseconds and accumulates fractional sleep time to decide when to
 * perform an actual Thread.sleep with millisecond+nanosecond precision.
 * This avoids sleeping on every operation while preserving long-term
 * throughput accuracy.
 *
 * <p>Notes:
 * <ul>
 *   <li>If {@code recordsPerSec} is zero or negative the controller is inactive.</li>
 *   <li>Sleep durations smaller than {@link #MIN_SLEEP_NS} are accumulated and coalesced
 *       before a blocking sleep is issued to avoid excessive context switches.</li>
 * </ul>
 */
final public class SbkRateController implements RateController {
    private static final long MIN_SLEEP_NS = 2 * Time.NS_PER_MS;
    private long sleepTimeNs;
    private int recordsPerSec;
    private long toSleepNs;

    public SbkRateController() {
        this.recordsPerSec = 0;
        this.toSleepNs = 0;
    }

    /**
     * Start the Rate Controller.
     *
     * @param recordsPerSec Records Per Second.
     */
    @Override
    public void start(final int recordsPerSec) {
        this.recordsPerSec = recordsPerSec;
        this.sleepTimeNs = this.recordsPerSec > 0 ?
                Time.NS_PER_SEC / this.recordsPerSec : 0;
    }

    /**
     * Blocks for small amounts of time to achieve target Throughput/events per sec.
     *
     * @param events     current cumulative events
     * @param elapsedSec Elapsed seconds
     */
    @Override
    public void control(final long events, final double elapsedSec) {
        if (this.recordsPerSec <= 0) {
            return;
        }
        needSleep(events, elapsedSec);
    }

    private void needSleep(final long events, final double elapsedSec) {
        if ((events / elapsedSec) < this.recordsPerSec) {
            return;
        }

        // control throughput / number of events by sleeping, on average,
        toSleepNs += sleepTimeNs;
        // If threshold reached, sleep a little
        if (toSleepNs >= MIN_SLEEP_NS) {
            long sleepStart = System.nanoTime();
            try {
                final long sleepMs = toSleepNs / Time.NS_PER_MS;
                final long sleepNs = toSleepNs - (sleepMs * Time.NS_PER_MS);
                Thread.sleep(sleepMs, (int) sleepNs);
            } catch (InterruptedException e) {
                // will be taken care in finally block
            } finally {
                // in case of short sleeps or oversleep ;adjust it for next sleep duration
                final long sleptNS = System.nanoTime() - sleepStart;
                if (sleptNS > 0) {
                    toSleepNs -= sleptNS;
                } else {
                    toSleepNs = 0;
                }
            }
        }
    }
}

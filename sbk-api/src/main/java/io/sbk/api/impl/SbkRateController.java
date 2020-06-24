/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;
import io.sbk.api.Config;
import io.sbk.api.RateController;

final public class SbkRateController implements RateController {
    private static final long MIN_SLEEP_NS = 2 * Config.NS_PER_MS;
    private long startTime;
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
     * @param time   start time
     */
    @Override
    public void start(int recordsPerSec, long time) {
        this.recordsPerSec = recordsPerSec;
        this.startTime = time;
        this.sleepTimeNs = this.recordsPerSec > 0 ?
                Config.NS_PER_SEC / this.recordsPerSec : 0;
    }

    /**
     * Blocks for small amounts of time to achieve targetThroughput/events per sec.
     *
     * @param events current events
     * @param time   current time
     */
    @Override
    public void control(long events, long time) {
        if (this.recordsPerSec <= 0) {
            return;
        }
        needSleep(events, time);
    }

    private void needSleep(long events, long time) {
        float elapsedSec = (time - startTime) / 1000.f;

        if ((events / elapsedSec) < this.recordsPerSec) {
            return;
        }

        // control throughput / number of events by sleeping, on average,
        toSleepNs += sleepTimeNs;
        // If threshold reached, sleep a little
        if (toSleepNs >= MIN_SLEEP_NS) {
            long sleepStart = System.nanoTime();
            try {
                final long sleepMs = toSleepNs / Config.NS_PER_MS;
                final long sleepNs = toSleepNs - (sleepMs * Config.NS_PER_MS);
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

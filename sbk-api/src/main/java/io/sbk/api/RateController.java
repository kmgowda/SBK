/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

final public class RateController {
    private static final long MIN_SLEEP_NS = 2 * Config.NS_PER_MS;
    private final long startTime;
    private final long sleepTimeNs;
    private final int recordsPerSec;
    private long toSleepNs = 0;

    public RateController(long start, int recordsPerSec) {
        this.startTime = start;
        this.recordsPerSec = recordsPerSec;
        this.sleepTimeNs = this.recordsPerSec > 0 ?
                Config.NS_PER_SEC / this.recordsPerSec : 0;
    }

    /**
     * Blocks for small amounts of time to achieve targetThroughput/events per sec.
     *
     * @param events current events
     * @param time   current time
     */
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

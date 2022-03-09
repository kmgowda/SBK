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

import io.time.Time;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.locks.LockSupport;

/**
 * Class ElasticWait.
 */
@NotThreadSafe
public final class ElasticWait {
    final private int windowIntervalMS;
    final private int idleNS;
    final private double countRatio;
    final private long minIdleCount;
    private long elasticCount;
    private long idleCount;
    private long totalCount;

    /**
     * Constructor ElasticWait initialize all values.
     *
     * @param idleNS                int
     * @param windowIntervalMS      int
     * @param minIntervalMS         int
     */
    public ElasticWait(int idleNS, int windowIntervalMS, int minIntervalMS) {
        this.windowIntervalMS = windowIntervalMS;
        this.idleNS = idleNS;
        countRatio = (Time.NS_PER_MS * 1.0) / this.idleNS;
        minIdleCount = (long) (countRatio * minIntervalMS);
        elasticCount = minIdleCount;
        idleCount = 0;
        totalCount = 0;
    }

    /**
     * This method initialize {@link #idleCount} to zero.
     */
    public void reset() {
        idleCount = 0;
    }

    /**
     * Checks if {@link #idleCount} is greater than {@link #elasticCount}.
     *
     * @return true if {@link #idleCount} > {@link #elasticCount}.
     */
    public boolean waitAndCheck() {
        LockSupport.parkNanos(idleNS);
        idleCount++;
        totalCount++;
        return idleCount > elasticCount;
    }

    /**
     * This method update the {@link #elasticCount}.
     *
     * @param elapsedIntervalMS long
     */
    public void updateElastic(long elapsedIntervalMS) {
        elasticCount = Math.max((long) (countRatio * (windowIntervalMS - elapsedIntervalMS)), minIdleCount);
    }

    /**
     * This method sets the {@link #elasticCount} and initialize {@link #totalCount} to zero.
     *
     * @param currentIntervalMS long
     */
    public void setElastic(long currentIntervalMS) {
        elasticCount = (totalCount * windowIntervalMS) / currentIntervalMS;
        totalCount = 0;
    }
}

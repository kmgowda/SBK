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

@NotThreadSafe
public final class ElasticWait {
    final private int windowInterval;
    final private int idleNS;
    final private double countRatio;
    final private long minIdleCount;
    private long elasticCount;
    private long idleCount;
    private long totalCount;

    public ElasticWait(int windowInterval, int idleNS, int timeoutMS) {
        this.windowInterval = windowInterval;
        this.idleNS = idleNS;
        countRatio = (Time.NS_PER_MS * 1.0) / this.idleNS;
        minIdleCount = (long) (countRatio * timeoutMS);
        elasticCount = minIdleCount;
        idleCount = 0;
        totalCount = 0;
    }
    
    public void reset() {
        idleCount = 0;
    }

    public boolean waitAndCheck() {
        LockSupport.parkNanos(idleNS);
        idleCount++;
        totalCount++;
        return idleCount > elasticCount;
    }

    public void updateElastic(long elapsedInterval) {
        elasticCount = Math.max((long) (countRatio * (windowInterval - elapsedInterval)), minIdleCount);
    }

    public void setElastic(long currentInterval) {
        elasticCount = (totalCount * windowInterval) / currentInterval;
        totalCount = 0;
    }
}

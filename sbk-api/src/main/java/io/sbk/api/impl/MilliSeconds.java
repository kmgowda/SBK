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
import io.sbk.api.Time;

import java.util.concurrent.TimeUnit;

public class MilliSeconds implements Time {

    /**
     * get the Time Unit.
     * @return time Unit in milliseconds
     */
    public TimeUnit getTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }


    /**
     * get the current Time.
     * @return current Time
     */
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /**
     * get the current Time.
     * @param h time stamp in milliseconds
     * @param l time stamp in milliseconds, the l should be less than h
     * @return elapsed time in milliseconds
     */
    public double elapsedMilliSeconds(long h, long l) {
        return h-l;
    }

    /**
     * get the current Time.
     * @param h time stamp in milliseconds
     * @param l time stamp in milliseconds, the l should be less than h
     * @return elapsed time in seconds
     */
    public double elapsedSeconds(long h, long l) {
        return (h-l) / (Config.MS_PER_SEC * 1.0);
    }

}

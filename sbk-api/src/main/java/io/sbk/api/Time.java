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

public interface Time {


    /**
     * get the Time Unit.
     * @return time Unit
     */
    TimeUnit getTimeUnit();


    /**
     * get the current Time.
     * @return current Time
     */
    long getCurrentTime();


    /**
     * get the current Time.
     * @param h time stamp
     * @param l time stamp, the l should be less than h
     * @return elapsed time in milliseconds
     */
    double elapsedMilliSeconds(long h, long l);

    /**
     * get the current Time.
     * @param h time stamp
     * @param l time stamp, the l should be less than h
     * @return elapsed time in seconds
     */
    double elapsedSeconds(long h, long l);
}



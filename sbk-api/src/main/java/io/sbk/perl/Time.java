/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl;

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
     * get the duration between two time stamps.
     * @param h time stamp
     * @param l time stamp, the l should be less than h
     * @return elapsed time.
     */
    default long elapsed(long h, long l) {
        return h-l;
    }

    /**
     * get the elapsed Time in milliseconds.
     * @param h time stamp
     * @param l time stamp, the l should be less than h
     * @return elapsed time in milliseconds
     */
    double elapsedMilliSeconds(long h, long l);

    /**
     * get the elapsed Time in seconds.
     * @param h time stamp
     * @param l time stamp, the l should be less than h
     * @return elapsed time in seconds
     */
    double elapsedSeconds(long h, long l);

    /**
     * convert the time to Nanoseconds.
     * @param t time duration
     * @return converted time in nanoseconds
     */
    double convertToNanoSeconds(double t);

    /**
     * convert the time to Micro seconds.
     * @param t time duration
     * @return converted time in microseconds
     */
    double convertToMicroSeconds(double t);

    /**
     * convert the time to Milliseconds.
     * @param t time duration
     * @return converted time in Milliseconds
     */
    double convertToMilliSeconds(double t);

}



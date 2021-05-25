/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl.impl;
import io.sbk.perl.PerlConfig;
import io.sbk.perl.Time;
import io.sbk.perl.TimeUnit;


public class NanoSeconds implements Time {

    /**
     * get the Time Unit.
     * @return time Unit in nanoseconds.;
     */
    public TimeUnit getTimeUnit() {
        return TimeUnit.ns;
    }


    /**
     * get the current Time.
     * @return current Time
     */
    public long getCurrentTime() {
        return System.nanoTime();
    }

    /**
     * get the current Time.
     * @param h time stamp in Nano seconds
     * @param l time stamp in nano seconds, the l should be less than h
     * @return elapsed time in milliseconds
     */
    public double elapsedMilliSeconds(long h, long l) {
        return elapsed(h, l) / (PerlConfig.NS_PER_MS * 1.0);
    }

    /**
     * get the current Time.
     * @param h time stamp in Nano seconds.
     * @param l time stamp in Nano seconds, the l should be less than h
     * @return elapsed time in seconds
     */
    public double elapsedSeconds(long h, long l) {
        return elapsed(h, l) / (PerlConfig.NS_PER_SEC * 1.0);
    }

    /**
     * convert the time to Nanoseconds.
     * @param t time duration in nanoseconds.
     * @return converted time in nanoseconds
     */
    public double convertToNanoSeconds(double t) {
        return t;
    }

    /**
     * convert the time to Micro seconds.
     * @param t time duration in nanoseconds.
     * @return converted time in microseconds
     */
    public double convertToMicroSeconds(double t) {
        return t / PerlConfig.NS_PER_MICRO;
    }

    /**
     * convert the time to Milliseconds.
     * @param t time duration in nanoseconds.
     * @return converted time in Milliseconds
     */
    public double convertToMilliSeconds(double t) {
        return t / PerlConfig.NS_PER_MS;
    }

}

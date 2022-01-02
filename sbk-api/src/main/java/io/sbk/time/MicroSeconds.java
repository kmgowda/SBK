/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.time;

final public class MicroSeconds implements Time {

    /**
     * get the Time Unit.
     *
     * @return time Unit in Microseconds.;
     */
    public TimeUnit getTimeUnit() {
        return TimeUnit.mcs;
    }


    /**
     * get the current Time in Micro seconds.
     *
     * @return current Time
     */
    public long getCurrentTime() {
        return System.nanoTime() / Time.NS_PER_MICRO;
    }

    /**
     * get the current Time.
     *
     * @param h time stamp in Micro seconds
     * @param l time stamp in Micro seconds, the l should be less than h
     * @return elapsed time in milliseconds
     */
    public double elapsedMilliSeconds(long h, long l) {
        return elapsed(h, l) / (Time.MICROS_PER_MS * 1.0);
    }

    /**
     * get the current Time.
     *
     * @param h time stamp in Micro seconds.
     * @param l time stamp in Micro seconds, the l should be less than h
     * @return elapsed time in seconds
     */
    public double elapsedSeconds(long h, long l) {
        return elapsed(h, l) / (Time.MICROS_PER_SEC * 1.0);
    }

    /**
     * convert the time to Nanoseconds.
     *
     * @param t time duration in microseconds.
     * @return converted time in nanoseconds
     */
    public double convertToNanoSeconds(double t) {
        return t * Time.NS_PER_MICRO;
    }

    /**
     * convert the time to Micro seconds.
     *
     * @param t time duration in microseconds.
     * @return converted time in microseconds
     */
    public double convertToMicroSeconds(double t) {
        return t;
    }

    /**
     * convert the time to Milliseconds.
     *
     * @param t time duration in microseconds.
     * @return converted time in Milliseconds
     */
    public double convertToMilliSeconds(double t) {
        return t / Time.MICROS_PER_MS;
    }

}

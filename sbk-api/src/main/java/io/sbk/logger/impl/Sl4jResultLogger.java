/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for recoding/printing results on Sl4j.
 */
public class Sl4jResultLogger extends SystemLogger {
    final private Logger log;

    public Sl4jResultLogger() {
        super();
        log = LoggerFactory.getLogger("SBK");
    }

    private void print(String prefix, double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                       double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                       long slc1, long slc2, long[] percentileValues) {
        log.info(buildResultString(new StringBuilder(prefix), seconds, bytes, records, recsPerSec, mbPerSec, avgLatency,
                maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues));
    }

    @Override
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      long slc1, long slc2, long[] percentileValues) {
        print(prefix, seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }

    @Override
    public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           long slc1, long slc2, long[] percentileValues) {
        print(prefix + "(Total) ", seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }
}

/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Class for recoding/printing results on Sl4j.
 */
public class Sl4jResultLogger extends SystemResultLogger {
    final private Logger log;

    public Sl4jResultLogger(String prefix, TimeUnit timeUnit) {
        super(prefix, timeUnit);
        log = LoggerFactory.getLogger("SBK");
    }

    private void print(String prefix, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long lowerDiscard, long higherDiscard, int one, int two, int three, int four, int five, int six,
                      int seven, int eight) {
        log.info(String.format("%s %10d records, %9.1f records/sec, %8.2f MB/sec, %8.1f %s avg latency, %7d %s max latency;" +
                        " Discarded Latencies:%8d lower, %8d higher; " +
                        " Latency Percentiles: %7d %s 10th, %7d %s 25th, %7d %s 50th, %7d %s 75th, %7d %s 95th, %7d %s 99th, %7d %s 99.9th, %7d %s 99.99th.\n",
                this.prefix + prefix, records, recsPerSec, mbPerSec, avgLatency, unit, maxLatency, unit, lowerDiscard, higherDiscard,
                one, unit, two, unit, three, unit, four, unit, five, unit, six, unit, seven, unit, eight, unit));
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long lowerDiscard, long higherDiscard, int one, int two, int three, int four, int five, int six,
                      int seven, int eight) {
        print(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, lowerDiscard, higherDiscard,
                one, two, three, four, five, six, seven, eight);
    }

    @Override
    public void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                           int maxLatency, long lowerDiscard, long higherDiscard, int one, int two, int three, int four, int five, int six,
                           int seven, int eight) {
        print(prefix + "(Total) ", bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, lowerDiscard, higherDiscard,
                one, two, three, four, five, six, seven, eight);
    }
}

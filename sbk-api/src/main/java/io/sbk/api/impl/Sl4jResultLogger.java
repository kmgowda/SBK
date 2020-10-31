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

/**
 * Class for recoding/printing results on Sl4j.
 */
public class Sl4jResultLogger extends SystemResultLogger {
    final private Logger log;

    public Sl4jResultLogger(String prefix,  String timeUnit, double[] percentiles) {
        super(prefix, timeUnit, percentiles);
        log = LoggerFactory.getLogger("SBK");
    }

    private void print(String prefix, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long lowerDiscard, long higherDiscard, int[] percentilesValues) {
        log.info(String.format("%s %10d records, %9.1f records/sec, %8.2f MB/sec, %8.1f %s avg latency, %7d %s max latency;" +
                        " Discarded Latencies:%8d lower, %8d higher; " +
                        " Latency Percentiles: %s.\n",
                this.prefix + prefix, records, recsPerSec, mbPerSec, avgLatency, timeUnit, maxLatency, timeUnit, lowerDiscard, higherDiscard,
                buildPercentileString(percentilesValues)));
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long lowerDiscard, long higherDiscard, int[] percentileValues) {
        print(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, lowerDiscard, higherDiscard,
                percentileValues);
    }

    @Override
    public void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                           int maxLatency, long lowerDiscard, long higherDiscard, int[] percentileValues) {
        print(prefix + "(Total) ", bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, lowerDiscard, higherDiscard,
                percentileValues);
    }
}

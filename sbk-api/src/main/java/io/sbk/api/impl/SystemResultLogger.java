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

import io.sbk.api.ResultLogger;
import java.text.DecimalFormat;

/**
 * Class for recoding/printing results on System.out.
 */
public class SystemResultLogger implements ResultLogger {
    final public String prefix;
    final public String timeUnit;
    final public double[] percentiles;
    final public DecimalFormat format;

    public SystemResultLogger(String prefix, String timeUnit, double[] percentiles) {
        this.prefix = prefix;
        this.timeUnit = timeUnit;
        this.percentiles = percentiles;
        this.format = new DecimalFormat("0.##");
    }

    public String buildPercentileString(int[] percentileValues) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); i++) {
                if (i == 0) {
                    out = new StringBuilder(String.format("%7d %s %sth", percentileValues[i], timeUnit, format.format(percentiles[i])));
                } else {
                    out.append(String.format(", %7d %s %sth", percentileValues[i], timeUnit, format.format(percentiles[i])));
                }
        }
        return out.toString();
    }

    private void print(String prefix, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                       int maxLatency, long lowerDiscard, long higherDiscard, int[] percentileValues) {
        System.out.printf("%s %10d records, %9.1f records/sec, %8.2f MB/sec, %8.1f %s avg latency, %7d %s max latency;" +
                        " Discarded Latencies:%8d lower, %8d higher; " +
                        " Latency Percentiles: %s. \n",
                prefix, records, recsPerSec, mbPerSec, avgLatency, timeUnit, maxLatency, timeUnit, lowerDiscard, higherDiscard,
                buildPercentileString(percentileValues));
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long lowerDiscard, long higherDiscard, int[] percentileValues) {
        print(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, lowerDiscard, higherDiscard,
                    percentileValues);
    }

    @Override
    public void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long lowerDiscard, long higherDiscard, int[] percentilesValues) {
        print(prefix + "(Total) ", bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, lowerDiscard, higherDiscard,
                percentilesValues);
    }
}

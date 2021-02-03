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

import io.sbk.api.Action;
import io.sbk.api.Config;
import io.sbk.api.Logger;
import io.sbk.api.Parameters;
import io.sbk.api.Time;

import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Class for recoding/printing results on System.out.
 */
public class SystemLogger implements Logger {
    final public DecimalFormat format;
    public String prefix;
    public String timeUnit;
    public double[] percentiles;

    public SystemLogger() {
        this.format = new DecimalFormat(Config.SBK_PERCENTILE_FORMAT);
    }

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
    }

    @Override
    public void open(final Parameters params, final String storageName, Action action, Time time) throws  IOException {
        this.prefix = storageName+" "+action.name();
        this.timeUnit = getTimeUnit().name();
        this.percentiles = getPercentileIndices();
        for (double p: this.percentiles) {
            if (p < 0 || p > 100) {
                SbkLogger.log.error("Invalid percentiles indices : " + percentiles.toString());
                SbkLogger.log.error("Percentile indices should be greater than 0 and less than 100");
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void close(final Parameters params) throws IOException  {
    }


    public String buildResultString(String prefix, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                               int maxLatency, long invalid, long lowerDiscard, long higherDiscard, int[] percentileValues) {
        StringBuilder out = new StringBuilder();

        out.append(prefix);
        out.append(String.format("%11d records, %9.1f records/sec, %8.2f MB/sec, %8.1f %s avg latency, %7d %s max latency;" +
                        " %8d invalid latencies; Discarded Latencies:%8d lower, %8d higher;", records, recsPerSec, mbPerSec, avgLatency,
                timeUnit, maxLatency, timeUnit, invalid, lowerDiscard, higherDiscard));

        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); i++) {
            if (i == 0) {
                out.append(String.format("%7d %s %sth", percentileValues[i], timeUnit, format.format(percentiles[i])));
            } else {
                out.append(String.format(", %7d %s %sth", percentileValues[i], timeUnit, format.format(percentiles[i])));
            }
        }
        out.append(".\n");
        return out.toString();
    }


    private void print(String prefix, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                       int maxLatency, long invalid, long lowerDiscard, long higherDiscard, int[] percentileValues) {
        System.out.print(buildResultString(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency,  maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues));
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long invalid, long lowerDiscard, long higherDiscard, int[] percentileValues) {
        print(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
    }

    @Override
    public void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long invalid, long lowerDiscard, long higherDiscard, int[] percentilesValues) {
        print("Total : " + prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentilesValues);
    }
}

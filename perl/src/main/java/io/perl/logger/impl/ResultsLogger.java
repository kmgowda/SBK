/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.logger.impl;

import io.perl.config.PerlConfig;
import io.perl.data.Bytes;
import io.perl.config.LatencyConfig;
import io.perl.logger.PerformanceLogger;
import io.time.TimeUnit;

import javax.annotation.Nonnull;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Class ResultsLogger.
 */
public class ResultsLogger implements PerformanceLogger {

    /**
     * <code>String prefix</code>.
     */
    protected String prefix;

    /**
     * <code>String timeUnitName</code>.
     */
    protected String timeUnitName;

    /**
     * <code>String[] percentileNames</code>.
     */
    protected String[] percentileNames;

    /**
     * <code>TimeUnit timeUnit</code>.
     */
    protected TimeUnit timeUnit;

    /**
     * <code>double[] percentiles</code>.
     */
    protected double[] percentiles;

    /**
     * <code>long minLatency</code>.
     */
    protected long minLatency;

    /**
     * <code>long maxLatency</code>.
     */
    protected long maxLatency;

    /**
     * <code>DecimalFormat format</code>.
     */
    private final DecimalFormat format;

    /**
     * Constructor ResultsLogger initialize all values with given parameters.
     *
     * @param prefix            String
     * @param percentiles       double[]
     * @param timeUnit          TimeUnit
     * @param minLatency        long
     * @param maxLatency        long
     */
    public ResultsLogger(String prefix, @Nonnull double[] percentiles,
                         @Nonnull TimeUnit timeUnit, long minLatency, long maxLatency) {
        this.format = new DecimalFormat(LatencyConfig.PERCENTILE_FORMAT);
        this.prefix = prefix;
        this.timeUnit = timeUnit;
        this.percentiles = percentiles.clone();
        this.timeUnitName = timeUnit.name();
        this.minLatency = minLatency;
        this.maxLatency = maxLatency;
        setPercentileNames(percentiles);
    }

    /**
     * Constructor ResultsLogger takes no arguments but initialize all values with default values.
     */
    public ResultsLogger() {
        this(PerlConfig.NAME, LatencyConfig.PERCENTILES, TimeUnit.ms,
                LatencyConfig.DEFAULT_MIN_LATENCY, LatencyConfig.DEFAULT_MAX_LATENCY);
    }

    /**
     * Method setPercentileNames collecting all names and setting it in {@link #percentileNames}.
     *
     * @param percentiles     double[]
     */
    protected void setPercentileNames(double[] percentiles) {
        Arrays.sort(percentiles);
        percentileNames = new String[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            percentileNames[i] = format.format(percentiles[i]);
        }
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public long getMinLatency() {
        return minLatency;
    }

    @Override
    public long getMaxLatency() {
        return maxLatency;
    }

    @Override
    public double[] getPercentiles() {

        if (percentiles == null) {
            return null;
        }
        return percentiles.clone();
    }

    /**
     * Method buildResultString builds all result and return in String format.
     *
     * @param out                   StringBuilder
     * @param seconds               double
     * @param bytes                 long
     * @param records               long
     * @param recsPerSec            double
     * @param mbPerSec              double
     * @param avgLatency            double
     * @param minLatency            long
     * @param maxLatency            long
     * @param invalid               long
     * @param lowerDiscard          long
     * @param higherDiscard         long
     * @param slc1                  long
     * @param slc2                  long
     * @param percentileValues      long[]
     */
    protected final void appendResultString(StringBuilder out, double seconds, long bytes, long records,
                                          double recsPerSec, double mbPerSec, double avgLatency, long minLatency,
                                            long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                                            long slc1, long slc2, long[] percentileValues) {
        final double mBytes = (bytes * 1.0) / Bytes.BYTES_PER_MB;
        out.append(String.format("%8d seconds, %11.1f MB, %16d records, %11.1f records/sec, %8.2f MB/sec"
                        + ", %8.1f %s avg latency, %7d %s min latency, %7d %s max latency;"
                        + " %8d invalid latencies; Discarded Latencies:%8d lower, %8d higher;"
                        + " SLC-1: %3d, SLC-2: %3d;",
                (long) seconds, mBytes, records, recsPerSec, mbPerSec, avgLatency, timeUnitName,
                minLatency, timeUnitName, maxLatency, timeUnitName, invalid, lowerDiscard, higherDiscard, slc1, slc2));
        out.append(" Latency Percentiles: ");

        for (int i = 0; i < Math.min(percentileNames.length, percentileValues.length); i++) {
            if (i == 0) {
                out.append(String.format("%7d %s %sth", percentileValues[i], timeUnitName, percentileNames[i]));
            } else {
                out.append(String.format(", %7d %s %sth", percentileValues[i], timeUnitName, percentileNames[i]));
            }
        }
        out.append("\n");
    }

    @Override
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long minLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long slc1, long slc2,
                      long[] percentileValues) {
        final StringBuilder out = new StringBuilder(prefix);
        appendResultString(out, seconds, bytes, records, recsPerSec, mbPerSec,
                avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2,
                percentileValues);
        System.out.print(out);
    }

    @Override
    public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                           long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        final StringBuilder out = new StringBuilder("Total : "+ prefix);
        appendResultString(out, seconds, bytes, records, recsPerSec, mbPerSec,
                avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2,
                percentileValues);
        System.out.print(out);
    }
}

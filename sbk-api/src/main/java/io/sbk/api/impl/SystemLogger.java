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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.Action;
import io.sbk.api.InputOptions;
import io.sbk.api.Logger;
import io.sbk.perl.LoggerConfig;
import io.sbk.perl.PerlConfig;
import io.sbk.perl.Time;
import io.sbk.perl.TimeUnit;
import io.sbk.system.Printer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for recoding/printing results on System.out.
 */
public class SystemLogger implements Logger {
    final static String LOGGER_FILE = "logger.properties";
    final public DecimalFormat format;
    public String prefix;
    public String timeUnit;
    public InputOptions params;
    public AtomicInteger writers;
    public AtomicInteger readers;
    public AtomicInteger maxWriters;
    public AtomicInteger maxReaders;
    public double[] percentiles;
    private LoggerConfig loggerConfig;
    private String[] percentileNames;
    private long minLatency;
    private long maxLatency;

    public SystemLogger() {
        this.format = new DecimalFormat(PerlConfig.PERCENTILE_FORMAT);
    }


    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            loggerConfig = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(LOGGER_FILE),
                    LoggerConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        String[] percentilesList = loggerConfig.percentiles.split(",");
        percentiles = new double[percentilesList.length];
        for (int i = 0; i < percentilesList.length; i++) {
            percentiles[i] = Double.parseDouble(percentilesList[i].trim());
        }
        Arrays.sort(percentiles);
        percentileNames = new String[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            percentileNames[i] = format.format(percentiles[i]);
        }

        params.addOption("time", true, "Latency Time Unit " + getTimeUnitNames() +
                "; default: " + loggerConfig.timeUnit.name());
    }


    private String getTimeUnitNames() {
        String ret = "[";

        for (io.sbk.perl.TimeUnit value : TimeUnit.values()) {
            ret += value.name() +":" +value.toString() + ", ";
        }
        ret += "]";

        return ret.replace(", ]", "]");
    }


    @Override
    public void parseArgs(final InputOptions params) throws IllegalArgumentException {
        loggerConfig.timeUnit = TimeUnit.valueOf(params.getOptionValue("time", loggerConfig.timeUnit.name()));
        int val = 1;
        if (loggerConfig.timeUnit == TimeUnit.ns) {
            val = PerlConfig.NS_PER_MS;
        } else if (loggerConfig.timeUnit == TimeUnit.mcs) {
            val = PerlConfig.MICROS_PER_MS;
        }
        minLatency = (long) (((double) loggerConfig.minLatencyMS) * val);
        maxLatency = (long) (((double) loggerConfig.maxLatencyMS) * val);
    }

    @Override
    public void open(final InputOptions params, final String storageName, Action action, Time time) throws  IOException {
        this.params = params;
        this.prefix = storageName+" "+action.name();
        this.timeUnit = getTimeUnit().name();
        for (double p: this.percentiles) {
            if (p < 0 || p > 100) {
                Printer.log.error("Invalid percentiles indices : " + Arrays.toString(percentiles));
                Printer.log.error("Percentile indices should be greater than 0 and less than 100");
                throw new IllegalArgumentException();
            }
        }
        this.writers = new AtomicInteger(0);
        this.readers = new AtomicInteger(0);
        this.maxWriters = new AtomicInteger(0);
        this.maxReaders = new AtomicInteger(0);
    }

    @Override
    public void close(final InputOptions params) throws IOException  {
    }

    @Override
    public int getReportingIntervalSeconds() {
        return loggerConfig.reportingSeconds;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return loggerConfig.timeUnit;
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
        return percentiles;
    }


    private void incrementAtomic(AtomicInteger counter,   int val) {
        counter.set(counter.get()+val);
    }

    private void decrementAtomic(AtomicInteger counter, int val) {
        counter.set(counter.get()-val);
    }

    @Override
    public void incrementWriters(int val) {
        incrementAtomic(writers,  val);
        incrementAtomic(maxWriters,  val);
    }

    @Override
    public void decrementWriters(int val) {
        decrementAtomic(writers, val);
    }

    @Override
    public void incrementReaders(int val) {
        incrementAtomic(readers, val);
        incrementAtomic(maxReaders, val);
    }

    @Override
    public void decrementReaders(int val) {
        decrementAtomic(readers, val);
    }
    
    public String buildResultString(String prefix, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                               long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        StringBuilder out = new StringBuilder();
        out.append(prefix);
        out.append(String.format(" %5d Writers, %5d Readers, ", writers.get(), readers.get()));
        out.append(String.format(" %5d Max Writers, %5d Max Readers, ", maxWriters.get(), maxReaders.get()));
        out.append(String.format("%11d records, %9.1f records/sec, %8.2f MB/sec, %8.1f %s avg latency, %7d %s max latency;" +
                        " %8d invalid latencies; Discarded Latencies:%8d lower, %8d higher;", records, recsPerSec, mbPerSec, avgLatency,
                timeUnit, maxLatency, timeUnit, invalid, lowerDiscard, higherDiscard));

        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); i++) {
            if (i == 0) {
                out.append(String.format("%7d %s %sth", percentileValues[i], timeUnit, percentileNames[i]));
            } else {
                out.append(String.format(", %7d %s %sth", percentileValues[i], timeUnit, percentileNames[i]));
            }
        }
        out.append(".\n");
        return out.toString();
    }


    private void print(String prefix, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                       long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        System.out.print(buildResultString(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency,  maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues));
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        print(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
    }

    @Override
    public void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentilesValues) {
        print("Total : " + prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentilesValues);
    }
}

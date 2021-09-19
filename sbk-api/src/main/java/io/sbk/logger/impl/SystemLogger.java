/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.action.Action;
import io.sbk.config.PerlConfig;
import io.sbk.logger.Logger;
import io.sbk.logger.LoggerConfig;
import io.sbk.options.InputOptions;
import io.sbk.system.Printer;
import io.sbk.time.Time;
import io.sbk.time.TimeUnit;
import io.sbk.time.impl.MicroSeconds;
import io.sbk.time.impl.MilliSeconds;
import io.sbk.time.impl.NanoSeconds;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for recoding/printing results on System.out.
 */
public class SystemLogger implements Logger {
    private final static String LOGGER_FILE = "logger.properties";
    public final AtomicInteger writers;
    public final AtomicInteger readers;
    public final AtomicInteger maxWriters;
    public final AtomicInteger maxReaders;
    public String storageName;
    public String prefix;
    public String timeUnitText;
    public InputOptions params;
    public double[] percentiles;
    public String[] percentileNames;
    public Action action;
    public Time time;
    private final DecimalFormat format;
    private LoggerConfig loggerConfig;
    private TimeUnit timeUnit;
    private long minLatency;
    private long maxLatency;

    public SystemLogger() {
        this.format = new DecimalFormat(PerlConfig.PERCENTILE_FORMAT);
        this.writers = new AtomicInteger(0);
        this.readers = new AtomicInteger(0);
        this.maxWriters = new AtomicInteger(0);
        this.maxReaders = new AtomicInteger(0);
    }


    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            loggerConfig = mapper.readValue(
                    io.sbk.logger.impl.SystemLogger.class.getClassLoader().getResourceAsStream(LOGGER_FILE),
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
        params.addOption("minlatency", true,
                """
                        Minimum latency;
                        use '-time' for time unit; default:""" + loggerConfig.minLatency
                        + " " + loggerConfig.timeUnit.name());
        params.addOption("maxlatency", true,
                """
                        Maximum latency;
                        use '-time' for time unit; default:""" + loggerConfig.maxLatency
                        + " " + loggerConfig.timeUnit.name());
    }


    private String getTimeUnitNames() {
        StringBuilder ret = new StringBuilder("[");

        for (TimeUnit value : TimeUnit.values()) {
            ret.append(value.name()).append(":").append(value).append(", ");
        }
        ret.append("]");

        return ret.toString().replace(", ]", "]");
    }


    @Override
    public void parseArgs(final @NotNull InputOptions params) throws IllegalArgumentException {
        try {
            timeUnit = TimeUnit.valueOf(params.getOptionValue("time", loggerConfig.timeUnit.name()));
        } catch (IllegalArgumentException ex) {
            Printer.log.error("Invalid value for option '-time', valid values " +
                    Arrays.toString(Arrays.stream(TimeUnit.values()).map(Enum::name).toArray()));
            throw ex;
        }

        //copy the default values
        minLatency = loggerConfig.minLatency;
        maxLatency = loggerConfig.maxLatency;
        Time convertTime = null;
        switch (timeUnit) {
            case ms -> {
                convertTime = switch (loggerConfig.timeUnit) {
                    case ns -> new NanoSeconds();
                    case mcs -> new MicroSeconds();
                    default -> null;
                };
                if (convertTime != null) {
                    minLatency = (long) convertTime.convertToMilliSeconds(loggerConfig.minLatency);
                    maxLatency = (long) convertTime.convertToMicroSeconds(loggerConfig.maxLatency);
                }
            }
            case ns -> {
                convertTime = switch (loggerConfig.timeUnit) {
                    case ms -> new MilliSeconds();
                    case mcs -> new MicroSeconds();
                    default -> null;
                };
                if (convertTime != null) {
                    minLatency = (long) convertTime.convertToNanoSeconds(loggerConfig.minLatency);
                    maxLatency = (long) convertTime.convertToNanoSeconds(loggerConfig.maxLatency);
                }
            }
            case mcs -> {
                convertTime = switch (loggerConfig.timeUnit) {
                    case ms -> new MilliSeconds();
                    case ns -> new NanoSeconds();
                    default -> null;
                };
                if (convertTime != null) {
                    minLatency = (long) convertTime.convertToMicroSeconds(loggerConfig.minLatency);
                    maxLatency = (long) convertTime.convertToMicroSeconds(loggerConfig.maxLatency);
                }
            }
        }
        minLatency = Long.parseLong(params.getOptionValue("minlatency", Long.toString(minLatency)));
        maxLatency = Long.parseLong(params.getOptionValue("maxlatency", Long.toString(maxLatency)));
    }

    @Override
    public void open(final InputOptions params, final String storageName, @NotNull Action action, Time time) throws IOException {
        this.params = params;
        this.storageName = storageName;
        this.action = action;
        this.time = time;
        this.prefix = storageName + " " + action.name();
        this.timeUnitText = getTimeUnit().name();
        for (double p : this.percentiles) {
            if (p < 0 || p > 100) {
                Printer.log.error("Invalid percentiles indices : " + Arrays.toString(percentiles));
                Printer.log.error("Percentile indices should be greater than 0 and less than 100");
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void close(final InputOptions params) throws IOException {
    }

    @Override
    public int getReportingIntervalSeconds() {
        return loggerConfig.reportingSeconds;
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
        return percentiles;
    }

    @Override
    public void incrementWriters() {
        writers.incrementAndGet();
        maxWriters.incrementAndGet();
    }

    @Override
    public void decrementWriters() {
        writers.decrementAndGet();
    }

    @Override
    public void incrementReaders() {
        readers.incrementAndGet();
        maxReaders.incrementAndGet();
    }

    @Override
    public void decrementReaders() {
        readers.decrementAndGet();
    }

    public void appendPercentiles(@NotNull StringBuilder out, long seconds, double mBytes, long records, double recsPerSec,
                                  double mbPerSec, double avgLatency, long maxLatency, long invalid, long lowerDiscard,
                                  long higherDiscard, long slc1, long slc2, @NotNull long[] percentileValues) {
        out.append(String.format("%8d seconds, %11.1f MB, %16d records, %11.1f records/sec, %8.2f MB/sec"
                        + ", %8.1f %s avg latency, %7d %s max latency;"
                        + " %8d invalid latencies; Discarded Latencies:%8d lower, %8d higher;"
                        + " SLC-1: %3d, SLC-2: %3d;",
                seconds, mBytes, records, recsPerSec, mbPerSec, avgLatency, timeUnitText, maxLatency,
                timeUnitText, invalid, lowerDiscard, higherDiscard, slc1, slc2));
        out.append(" Latency Percentiles: ");

        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); i++) {
            if (i == 0) {
                out.append(String.format("%7d %s %sth", percentileValues[i], timeUnitText, percentileNames[i]));
            } else {
                out.append(String.format(", %7d %s %sth", percentileValues[i], timeUnitText, percentileNames[i]));
            }
        }
    }

    public void appendWritesAndReaders(@NotNull StringBuilder out) {
        out.append(String.format(" %5d Writers, %5d Readers, ", writers.get(), readers.get()));
        out.append(String.format(" %5d Max Writers, %5d Max Readers, ", maxWriters.get(), maxReaders.get()));
    }

    public String buildResultString(StringBuilder out, double seconds, long bytes, long records, double recsPerSec,
                                    double mbPerSec, double avgLatency, long maxLatency, long invalid, long lowerDiscard,
                                    long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        final double mBytes = (bytes * 1.0) / PerlConfig.BYTES_PER_MB;
        appendWritesAndReaders(out);
        appendPercentiles(out, (long) seconds, mBytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        out.append(".\n");
        return out.toString();
    }

    private void print(String prefix, double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                       double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                       long slc1, long slc2, long[] percentileValues) {

        System.out.print(buildResultString(new StringBuilder(prefix), seconds, bytes, records, recsPerSec, mbPerSec,
                avgLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues));
    }

    @Override
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      long slc1, long slc2, long[] percentileValues) {
        print(prefix, seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
    }

    @Override
    public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           long slc1, long slc2, long[] percentileValues) {
        print("Total : " + prefix, seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }
}

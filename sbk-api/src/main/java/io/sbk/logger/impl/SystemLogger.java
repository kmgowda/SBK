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
import io.sbk.logger.LoggerConfig;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.MicroSeconds;
import io.time.MilliSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import io.time.TimeUnit;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.Arrays;

/**
 * Class for recoding/printing results on System.out.
 */
@NotThreadSafe
public class SystemLogger extends AbstractRWLogger {
    private final static String LOGGER_FILE = "logger.properties";
    private LoggerConfig loggerConfig;

    public SystemLogger() {
        super();
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            loggerConfig = mapper.readValue(
                    SystemLogger.class.getClassLoader().getResourceAsStream(LOGGER_FILE),
                    LoggerConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        String[] percentilesList = loggerConfig.percentiles.split(",");
        double[] percentiles = new double[percentilesList.length];
        for (int i = 0; i < percentilesList.length; i++) {
            percentiles[i] = Double.parseDouble(percentilesList[i].trim());
        }
        setPercentiles(percentiles);

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

    @Override
    public int getPrintingIntervalSeconds() {
        return loggerConfig.reportingSeconds;
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
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        try {
            setTimeUnit(TimeUnit.valueOf(params.getOptionValue("time", loggerConfig.timeUnit.name())));
        } catch (IllegalArgumentException ex) {
            Printer.log.error("Invalid value for option '-time', valid values " +
                    Arrays.toString(Arrays.stream(TimeUnit.values()).map(Enum::name).toArray()));
            throw ex;
        }

        //copy the default values
        setMinLatency(loggerConfig.minLatency);
        setMaxLatency(loggerConfig.maxLatency);
        Time convertTime = null;
        switch (getTimeUnit()) {
            case ms -> {
                convertTime = switch (loggerConfig.timeUnit) {
                    case ns -> new NanoSeconds();
                    case mcs -> new MicroSeconds();
                    default -> null;
                };
                if (convertTime != null) {
                    setMinLatency((long) convertTime.convertToMilliSeconds(loggerConfig.minLatency));
                    setMaxLatency((long) convertTime.convertToMilliSeconds(loggerConfig.maxLatency));
                }
            }
            case ns -> {
                convertTime = switch (loggerConfig.timeUnit) {
                    case ms -> new MilliSeconds();
                    case mcs -> new MicroSeconds();
                    default -> null;
                };
                if (convertTime != null) {
                    setMinLatency((long) convertTime.convertToNanoSeconds(loggerConfig.minLatency));
                    setMaxLatency((long) convertTime.convertToNanoSeconds(loggerConfig.maxLatency));
                }
            }
            case mcs -> {
                convertTime = switch (loggerConfig.timeUnit) {
                    case ms -> new MilliSeconds();
                    case ns -> new NanoSeconds();
                    default -> null;
                };
                if (convertTime != null) {
                    setMinLatency((long) convertTime.convertToMicroSeconds(loggerConfig.minLatency));
                    setMaxLatency((long) convertTime.convertToMicroSeconds(loggerConfig.maxLatency));
                }
            }
        }
        setMinLatency(Long.parseLong(params.getOptionValue("minlatency", Long.toString(getMinLatency()))));
        setMaxLatency(Long.parseLong(params.getOptionValue("maxlatency", Long.toString(getMaxLatency()))));
    }

    public void open(final ParsedOptions params, final String storageName, final Action action, Time time) throws IOException {
        super.open(params, storageName, action, time);
    }

    @Override
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestsRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                      long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                      long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        StringBuilder out = new StringBuilder(getHeader());
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestsRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        System.out.println(out);
    }

    public void printTotal(int writers, int maxWriters, int readers, int maxReaders,
                           long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                           double writeRequestRecordsPerSec, long readRequestBytes, double readRequestsMbPerSec,
                           long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                           long writeResponsePendingBytes, long readResponsePendingRecords,
                           long readResponsePendingBytes, long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                           long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                           long readTimeoutEvents, double readTimeoutEventsPerSec,
                           double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                           long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        StringBuilder out = new StringBuilder("Total " + getHeader());
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestsMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        System.out.println(out);
    }

}

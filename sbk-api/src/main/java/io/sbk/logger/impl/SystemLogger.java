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
import io.perl.data.Bytes;
import io.perl.logger.impl.ResultsLogger;
import io.sbk.action.Action;
import io.sbk.logger.RWLogger;
import io.sbk.logger.LoggerConfig;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.MicroSeconds;
import io.time.MilliSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import io.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Class for recoding/printing results on System.out.
 */
public class SystemLogger extends ResultsLogger implements RWLogger {
    private final static String LOGGER_FILE = "logger.properties";
    protected final AtomicInteger writers;
    protected final AtomicInteger readers;
    protected final AtomicInteger maxWriters;
    protected final AtomicInteger maxReaders;
    protected final AtomicLong writeBytes;
    protected final AtomicLong writeRequests;
    protected final AtomicLong readBytes;
    protected final AtomicLong readRequests;
    protected String storageName;
    protected String timeUnitFullText;
    protected ParsedOptions params;
    protected Action action;
    protected Time time;
    protected boolean isRequestWrites;
    protected boolean isRequestReads;
    protected int writersCount;
    protected int readersCount;
    protected AtomicLongArray writeBytesArray;
    protected AtomicLongArray writeRequestsArray;
    protected AtomicLongArray readBytesArray;
    protected AtomicLongArray readRequestsArray;


    private LoggerConfig loggerConfig;



    public SystemLogger() {
        super();
        this.writers = new AtomicInteger(0);
        this.readers = new AtomicInteger(0);
        this.maxWriters = new AtomicInteger(0);
        this.maxReaders = new AtomicInteger(0);
        this.writeBytes = new AtomicLong(0);
        this.writeRequests = new AtomicLong(0);
        this.readBytes = new AtomicLong(0);
        this.readRequests = new AtomicLong(0);
        this.isRequestWrites = false;
        this.isRequestReads = false;
        this.readBytesArray = null;
        this.readRequestsArray = null;
        this.writeBytesArray = null;
        this.writeRequestsArray = null;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
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
        percentiles = new double[percentilesList.length];
        for (int i = 0; i < percentilesList.length; i++) {
            percentiles[i] = Double.parseDouble(percentilesList[i].trim());
        }
        setPercentileNames(percentiles);

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
        params.addOption("wq", true, "Benchmark Write Requests; default: "+ isRequestWrites);
        params.addOption("rq", true, "Benchmark Reade Requests; default: "+ isRequestReads);
    }


    @Override
    public int getMaxWriterIDs() {
        return writersCount;
    }

    @Override
    public int getMaxReaderIDs() {
        return readersCount;
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
        writersCount = Integer.parseInt(params.getOptionValue("writers", "1"));
        readersCount = Integer.parseInt(params.getOptionValue("readers", "1"));
        isRequestWrites = Boolean.parseBoolean(params.getOptionValue("wq", Boolean.toString(isRequestWrites)));
        isRequestReads = Boolean.parseBoolean(params.getOptionValue("rq", Boolean.toString(isRequestReads)));
    }

    @Override
    public void open(final ParsedOptions params, final String storageName, @NotNull Action action, Time time) throws IOException {
        this.params = params;
        this.storageName = storageName;
        this.action = action;
        this.time = time;
        this.prefix = storageName + " " + action.name();
        this.timeUnitName = getTimeUnit().name();
        this.timeUnitFullText = getTimeUnit().toString();
        for (double p : this.percentiles) {
            if (p < 0 || p > 100) {
                Printer.log.error("Invalid percentiles indices : " + Arrays.toString(percentiles));
                Printer.log.error("Percentile indices should be greater than 0 and less than 100");
                throw new IllegalArgumentException();
            }
        }
        this.writeBytesArray = new AtomicLongArray(writersCount);
        this.writeRequestsArray = new AtomicLongArray(writersCount);
        this.readBytesArray = new AtomicLongArray(readersCount);
        this.readRequestsArray = new AtomicLongArray(readersCount);
    }

    @Override
    public void close(final ParsedOptions params) throws IOException {
    }

    @Override
    public int getPrintingIntervalSeconds() {
        return loggerConfig.reportingSeconds;
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


    @Override
    public void recordWriteRequests(int writerId, long startTime, long bytes, long events) {
        writeRequestsArray.addAndGet(writerId, events);
        writeBytesArray.addAndGet(writerId, bytes);
    }


    @Override
    public void recordReadRequests(int readerId, long startTime, long bytes, long events) {
        readRequestsArray.addAndGet(readerId, events);
        readBytesArray.addAndGet(readerId, bytes);
    }


    protected void appendWriteAndReadRequests(@NotNull StringBuilder out, double seconds, boolean isTotal) {
        long writeRequestsSum = 0;
        long writeBytesSum = 0;
        long readRequestsSum = 0;
        long readBytesSum = 0;

        if (isRequestWrites) {
            for (int i = 0; i < writersCount; i++) {
                writeRequestsSum += writeRequestsArray.getAndSet(i, 0);
                writeBytesSum += writeBytesArray.getAndSet(i, 0);
            }

            if (isTotal) {
                writeRequestsSum += writeRequests.getAndSet(0);
                writeBytesSum += writeBytes.getAndSet(0);
            } else {
                writeRequests.addAndGet(writeRequestsSum);
                writeBytes.addAndGet(writeBytesSum);
            }
        }

        if (isRequestReads) {
            for (int i = 0; i < readersCount; i++) {
                readRequestsSum += readRequestsArray.getAndSet(i, 0);
                readBytesSum += readBytesArray.getAndSet(i, 0);
            }
            if (isTotal) {
                readRequestsSum += readRequests.getAndSet(0);
                readBytesSum += readBytes.getAndSet(0);
            } else {
                readBytes.addAndGet(readBytesSum);
                readRequests.addAndGet(readRequestsSum);
            }
        }

        final double wMB = writeBytesSum / (Bytes.BYTES_PER_MB * 1.0);
        final double rMB = readBytesSum /  (Bytes.BYTES_PER_MB * 1.0);
        final double wRequestsThroughput = writeRequestsSum / seconds;
        final double wMegaBytesThroughput = wMB / seconds;
        final double rRequestsThroughput = readRequestsSum / seconds;
        final double rMegaBytesThroughput = rMB / seconds;

        out.append(String.format(" %11.1f Write Requests MB, %16d Write Request records,"+
                        "%11.1f write request records/sec, %8.2f Write Requests MB/sec",
                wMB, writeRequestsSum, wRequestsThroughput, wMegaBytesThroughput));
        out.append(String.format(" %11.1f Read Requests MB, %16d Read Request records,"+
                        "%11.1f Read request records/sec, %8.2f Read Requests MB/sec",
                rMB, readRequestsSum, rRequestsThroughput, rMegaBytesThroughput));
    }

    protected void appendWritesAndReaders(@NotNull StringBuilder out) {
        out.append(String.format(" %5d Writers, %5d Readers, ", writers.get(), readers.get()));
        out.append(String.format(" %5d Max Writers, %5d Max Readers, ", maxWriters.get(), maxReaders.get()));
    }

    @Override
    protected String buildResultString(StringBuilder out, double seconds, long bytes, long records, double recsPerSec,
                                    double mbPerSec, double avgLatency, long minLatency, long maxLatency, long invalid,
                                       long lowerDiscard, long higherDiscard, long slc1, long slc2,
                                       long[] percentileValues) {
        appendWritesAndReaders(out);
        appendWriteAndReadRequests(out, seconds, false);
        appendResults(out, timeUnitName, percentileNames, (long) seconds, bytes, records, recsPerSec, mbPerSec,
                avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        out.append(".\n\n");
        return out.toString();
    }

    protected String buildTotalResultString(StringBuilder out, double seconds, long bytes, long records,
                                          double recsPerSec, double mbPerSec, double avgLatency,
                                            long minLatency, long maxLatency, long invalid, long lowerDiscard,
                                            long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        appendWritesAndReaders(out);
        appendWriteAndReadRequests(out, seconds, true);
        appendResults(out, timeUnitName, percentileNames, (long) seconds, bytes, records, recsPerSec, mbPerSec,
                avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        out.append(".\n\n");
        return out.toString();
    }

    @Override
    public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                           long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        System.out.print(buildTotalResultString(new StringBuilder("Total: "+ prefix), seconds, bytes, records, recsPerSec,
                mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2,
                percentileValues));

    }

}

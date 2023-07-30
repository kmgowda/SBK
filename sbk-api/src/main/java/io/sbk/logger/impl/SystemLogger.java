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

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for recoding/printing results on System.out.
 */
@NotThreadSafe
public class SystemLogger extends ResultsLogger implements RWLogger {
    private final static String LOGGER_FILE = "logger.properties";
    private final static VarHandle VAR_HANDLE_ARRAY;
    protected final AtomicInteger writers;
    protected final AtomicInteger readers;
    protected final AtomicInteger maxWriters;
    protected final AtomicInteger maxReaders;
    protected long writeRequestBytes;
    protected long writeRequestRecords;
    protected long writeMissEvents;
    protected long readRequestBytes;
    protected long readRequestRecords;
    protected long readMissEvents;
    protected long writeResponsePendingRecords;
    protected long writeResponsePendingBytes;
    protected long readResponsePendingRecords;
    protected long readResponsePendingBytes;
    protected String storageName;
    protected String timeUnitFullText;
    protected ParsedOptions params;
    protected Action action;
    protected Time time;
    protected boolean isRequestWrites;
    protected boolean isRequestReads;
    protected int maxWriterRequestIds;
    protected int maxReaderRequestIds;
    protected volatile long[] writeBytesArray;
    protected volatile long[] writeRequestRecordsArray;
    protected volatile long[] writeMissEventsArray;
    protected volatile long[] readBytesArray;
    protected volatile long[] readRequestRecordsArray;
    protected volatile long[] readMissEventsArray;
    private LoggerConfig loggerConfig;

    static {
        try {
            VAR_HANDLE_ARRAY = MethodHandles.arrayElementVarHandle( long[].class);
        } catch (IllegalArgumentException e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    public SystemLogger() {
        super();
        this.writers = new AtomicInteger(0);
        this.readers = new AtomicInteger(0);
        this.maxWriters = new AtomicInteger(0);
        this.maxReaders = new AtomicInteger(0);
        this.writeRequestBytes = 0;
        this.writeRequestRecords = 0;
        this.readRequestBytes = 0;
        this.readRequestRecords = 0;
        this.writeResponsePendingRecords = 0;
        this.writeResponsePendingBytes = 0;
        this.readResponsePendingRecords = 0;
        this.readResponsePendingBytes = 0;
        this.isRequestWrites = false;
        this.isRequestReads = false;
        this.readBytesArray = null;
        this.readRequestRecordsArray = null;
        this.writeBytesArray = null;
        this.writeRequestRecordsArray = null;
        this.maxReaderRequestIds = 0;
        this.maxWriterRequestIds = 0;
        this.readMissEventsArray = null;
        this.writeMissEventsArray = null;
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
        return maxWriterRequestIds;
    }

    @Override
    public int getMaxReaderIDs() {
        return maxReaderRequestIds;
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
        isRequestWrites = Boolean.parseBoolean(params.getOptionValue("wq", Boolean.toString(isRequestWrites)));
        isRequestReads = Boolean.parseBoolean(params.getOptionValue("rq", Boolean.toString(isRequestReads)));
        if (isRequestWrites) {
            maxWriterRequestIds = Integer.parseInt(params.getOptionValue("writers", "0"));
        }
        if (isRequestReads) {
            maxReaderRequestIds = Integer.parseInt(params.getOptionValue("readers", "0"));
        }

    }

    @Override
    public void open(final ParsedOptions params, final String storageName, final Action action, Time time) throws IOException {
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
        this.writeBytesArray = new long[maxWriterRequestIds];
        this.writeRequestRecordsArray = new long[maxWriterRequestIds];
        this.readBytesArray = new long[maxReaderRequestIds];
        this.readRequestRecordsArray = new long[maxReaderRequestIds];
        this.writeMissEventsArray = new long[maxWriterRequestIds];
        this.readMissEventsArray = new long[maxReaderRequestIds];
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
        VAR_HANDLE_ARRAY.getAndAdd(writeRequestRecordsArray, writerId, events);
        VAR_HANDLE_ARRAY.getAndAdd(writeBytesArray, writerId, bytes);
    }

    @Override
    public void recordWriteTimeoutEvents(int writerId, long startTime, long missEvents) {
        VAR_HANDLE_ARRAY.getAndAdd(writeMissEventsArray, writerId, missEvents);
    }

    @Override
    public void recordReadRequests(int readerId, long startTime, long bytes, long events) {
        VAR_HANDLE_ARRAY.getAndAdd(readRequestRecordsArray, readerId, events);
        VAR_HANDLE_ARRAY.getAndAdd(readBytesArray, readerId, bytes);
    }

    @Override
    public void recordReadTimeoutEvents(int readerId, long startTime, long missEvents) {
        VAR_HANDLE_ARRAY.getAndAdd(readMissEventsArray, readerId, missEvents);
    }

    protected final void appendWritesAndReaders(@NotNull StringBuilder out, int writers, int maxWriters,
                                          int readers, int maxReaders ) {
        out.append(String.format(" %5d writers, %5d readers, ", writers, readers));
        out.append(String.format(" %5d max writers, %5d max readers, ", maxWriters, maxReaders));
    }

    protected final void appendWriteAndReadRequests(@NotNull StringBuilder out, long writeRequestBytes,
                                              double writeRequestMbPerSec, long writesRequestRecords,
                                              double writeRequestRecordsPerSec, long readRequestBytes,
                                              double readRequestMbPerSec, long readRequestRecords,
                                              double readRequestRecordsPerSec) {
        out.append(String.format(" %11.1f write request MB, %16d write request records, "+
                        "%11.1f write request records/sec, %8.2f write request MB/sec,",
                (writeRequestBytes * 1.0) / Bytes.BYTES_PER_MB, writesRequestRecords,
                writeRequestRecordsPerSec, writeRequestMbPerSec));
        out.append(String.format(" %11.1f read request MB,  %16d read request records, "+
                        "%11.1f read request records/sec, %8.2f read request MB/sec,",
                (readRequestBytes * 1.0) / Bytes.BYTES_PER_MB, readRequestRecords,
                 readRequestRecordsPerSec, readRequestMbPerSec));
    }

    protected final void appendWriteAndReadRequestsPending(@NotNull StringBuilder out,
                                                           long writeResponsePendingRecords,
                                                           long writeResponsePendingBytes,
                                                           long readResponsePendingBytes,
                                                           long readResponsePendingRecords,
                                                           long writeReadRequestPendingRecords,
                                                           long writeReadRequestPendingBytes) {
        out.append(String.format(" %8.2f write response pending MB, %13d write response pending records, ",
                (writeResponsePendingBytes * 1.0) / Bytes.BYTES_PER_MB, writeResponsePendingRecords));
        out.append(String.format(" %8.2f read response pending MB, %13d read response pending records, ",
                (readResponsePendingBytes * 1.0) / Bytes.BYTES_PER_MB, readResponsePendingRecords));
        out.append(String.format(" %8.2f write read request pending MB, %13d write read request pending records, ",
                (writeReadRequestPendingBytes * 1.0) / Bytes.BYTES_PER_MB, writeReadRequestPendingRecords));
    }

    protected final void appendWriteAndReadMissEvents(@NotNull StringBuilder out,
                                                           long writeMissEvents,
                                                           double writeMissEventsPerSec,
                                                           long readMissEvents,
                                                           double readeMissEventsPerSec) {
        out.append(String.format(" %13d write miss events, %8.2f write miss events/sec, "+
                        "%13d read miss events, %8.2f read miss events/sec,",
               writeMissEvents, writeMissEventsPerSec, readMissEvents, readeMissEventsPerSec));

    }

    private record ReadWriteRequests(long readRequestRecords, long readRequestBytes, long readMissEvents,
                                     long writeRequestRecords, long writeRequestBytes, long writeMissEvents) {
    }

    protected final ReadWriteRequests getReadAndWriteRequests() {
        long writeRequestRecordssSum = 0;
        long writeBytesSum = 0;
        long writeMissEventsSum = 0;
        long readRequestRecordsSum = 0;
        long readBytesSum = 0;
        long readMissEventsSum = 0;

        if (isRequestWrites) {
            for (int i = 0; i < maxWriterRequestIds; i++) {
                writeRequestRecordssSum +=   (long) VAR_HANDLE_ARRAY.getAndSet(writeRequestRecordsArray, i, 0);
                writeBytesSum += (long) VAR_HANDLE_ARRAY.getAndSet(writeBytesArray, i, 0);
                writeMissEventsSum += (long) VAR_HANDLE_ARRAY.getAndSet(writeMissEventsArray, i, 0);
            }
        }

        if (isRequestReads) {
            for (int i = 0; i < maxReaderRequestIds; i++) {
                readRequestRecordsSum += (long) VAR_HANDLE_ARRAY.getAndSet(readRequestRecordsArray, i, 0);
                readBytesSum += (long) VAR_HANDLE_ARRAY.getAndSet(readBytesArray, i, 0);
                readMissEventsSum += (long) VAR_HANDLE_ARRAY.getAndSet(readMissEventsArray, i, 0);
            }
           }
        return new ReadWriteRequests(readRequestRecordsSum, readBytesSum, readMissEventsSum,
                writeRequestRecordssSum, writeBytesSum, writeMissEventsSum);
    }

    private static class ReadWriteRequestsPerformance {
        public final double writeRequestsPerSec;
        public final double writeRequestsMbPerSec;
        public final double writeMissEventsPerSec;
        public final double readRequestsPerSec;
        public final double readRequestsMBPerSec;
        public final double readMissEventsPerSec;


        public ReadWriteRequestsPerformance( double seconds, ReadWriteRequests req) {
            final double writeRequestMB = req.writeRequestBytes / (Bytes.BYTES_PER_MB * 1.0);
            final double readRequestMB = req.readRequestBytes /  (Bytes.BYTES_PER_MB * 1.0);

            writeRequestsPerSec = req.writeRequestRecords / seconds;
            writeRequestsMbPerSec = writeRequestMB / seconds;
            writeMissEventsPerSec = req.writeMissEvents / seconds;
            readRequestsPerSec = req.readRequestRecords / seconds;
            readRequestsMBPerSec = readRequestMB / seconds;
            readMissEventsPerSec = req.readMissEvents / seconds;
        }
    }


    protected final void appendResultString(StringBuilder out, int writers, int maxWriters, int readers, int maxReaders,
                                            long writeRequestBytes, double writeRequestMbPerSec,
                                            long writeRequestRecords, double writeRequestRecordsPerSec,
                                            long readRequestBytes, double readRequestMBPerSec,
                                            long readRequestRecords, double readRequestRecordsPerSec,
                                            long writeResponsePendingRecords, long writeResponsePendingBytes,
                                            long readResponsePendingBytes, long readResponsePendingRecords,
                                            long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                                            long writeMissEvents, double writeMissEventsPerSec,
                                            long readMissEvents, double readMissEventsPerSec,
                                            double seconds, long bytes,
                                            long records, double recsPerSec, double mbPerSec,
                                            double avgLatency, long minLatency, long maxLatency, long invalid,
                                            long lowerDiscard, long higherDiscard, long slc1, long slc2,
                                            long[] percentileValues) {
        appendWritesAndReaders(out, writers, maxWriters, readers, maxReaders);
        appendWriteAndReadRequests(out, writeRequestBytes, writeRequestMbPerSec, writeRequestRecords,
                writeRequestRecordsPerSec, readRequestBytes, readRequestMBPerSec, readRequestRecords,
                readRequestRecordsPerSec);
        appendWriteAndReadRequestsPending(out, writeResponsePendingRecords, writeResponsePendingBytes,
                readResponsePendingBytes, readResponsePendingRecords, writeReadRequestPendingRecords,
                writeReadRequestPendingBytes);
        appendWriteAndReadMissEvents(out, writeMissEvents, writeMissEventsPerSec, readMissEvents, readMissEventsPerSec);
        appendResultString(out, seconds, bytes, records, recsPerSec, mbPerSec,
                avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }

    @Override
    public final void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                         double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                            long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        final ReadWriteRequests req = getReadAndWriteRequests();
        final ReadWriteRequestsPerformance perf = new ReadWriteRequestsPerformance(seconds, req);
        final long writeReadPendingRecords;
        final long writeReadPendingBytes;

        if (isRequestWrites) {
            writeRequestRecords += req.writeRequestRecords;
            writeRequestBytes += req.writeRequestBytes;
            writeResponsePendingRecords += (req.writeRequestRecords - records);
            writeResponsePendingBytes += (req.writeRequestBytes - bytes);
            writeMissEvents += req.writeMissEvents;
        }
        if (isRequestReads) {
            readRequestRecords += req.readRequestRecords;
            readRequestBytes += req.readRequestBytes;
            readResponsePendingRecords += (req.readRequestRecords - records);
            readResponsePendingBytes += (req.readRequestBytes - bytes);
            readMissEvents += req.readMissEvents;
        }

        if (isRequestWrites && isRequestReads) {
            writeReadPendingRecords = writeRequestRecords - readRequestRecords;
            writeReadPendingBytes = writeRequestBytes - readRequestBytes;
        } else {
            writeReadPendingRecords = 0;
            writeReadPendingBytes = 0;
        }

        print(writers.get(), maxWriters.get(), readers.get(), maxReaders.get(),
                req.writeRequestBytes, perf.writeRequestsMbPerSec, req.writeRequestRecords, perf.writeRequestsPerSec,
                req.readRequestBytes, perf.readRequestsMBPerSec, req.readRequestRecords, perf.readRequestsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadPendingRecords, writeReadPendingBytes,
                req.writeMissEvents, perf.writeMissEventsPerSec, req.readMissEvents, perf.readMissEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid,
                lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }


    @Override
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestsRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                      long writeMissEvents, double writeMissEventsPerSec,
                      long readMissEvents, double readMissEventsPerSec,
                      double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        StringBuilder out = new StringBuilder(prefix);
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestsRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeMissEvents, writeMissEventsPerSec, readMissEvents, readMissEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        System.out.println(out);
    }


    @Override
    public final void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                           long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        final ReadWriteRequests req = getReadAndWriteRequests();
        final long writeReadPendingRecords;
        final long writeReadPendingBytes;

        if (isRequestWrites) {
            writeRequestRecords += req.writeRequestRecords;
            writeRequestBytes += req.writeRequestBytes;
            writeResponsePendingRecords = writeRequestRecords - records;
            writeResponsePendingBytes = writeRequestBytes - bytes;
            writeMissEvents += req.writeMissEvents;
        }
        if (isRequestReads) {
            readRequestRecords += req.readRequestRecords;
            readRequestBytes += req.readRequestBytes;
            readResponsePendingRecords = readRequestRecords - records;
            readResponsePendingBytes = readRequestBytes - bytes;
            readMissEvents += req.readMissEvents;
        }
        if (isRequestWrites && isRequestReads) {
            writeReadPendingRecords = writeRequestRecords - readRequestRecords;
            writeReadPendingBytes = writeRequestBytes - readRequestBytes;
        } else {
            writeReadPendingRecords = 0;
            writeReadPendingBytes = 0;
        }

        final ReadWriteRequests reqFinal = new ReadWriteRequests(readRequestRecords, readRequestBytes, readMissEvents,
                writeRequestRecords, writeRequestBytes, writeMissEvents);

        final ReadWriteRequestsPerformance perf = new ReadWriteRequestsPerformance(seconds, reqFinal);
        printTotal(writers.get(), maxWriters.get(), readers.get(), maxReaders.get(),
                reqFinal.writeRequestBytes, perf.writeRequestsMbPerSec, reqFinal.writeRequestRecords,
                perf.writeRequestsPerSec, reqFinal.readRequestBytes, perf.readRequestsMBPerSec,
                reqFinal.readRequestRecords, perf.readRequestsPerSec, writeResponsePendingRecords,
                writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                writeReadPendingRecords, writeReadPendingBytes,
                writeMissEvents, perf.writeMissEventsPerSec, readMissEvents, perf.readMissEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid,
                lowerDiscard, higherDiscard, slc1, slc2, percentileValues);

        readRequestRecords = readRequestBytes = writeRequestRecords = writeRequestBytes = 0;
        writeResponsePendingRecords = writeResponsePendingBytes = 0;
        readResponsePendingRecords = readResponsePendingBytes = 0;
    }

    public void printTotal(int writers, int maxWriters, int readers, int maxReaders,
                           long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                           double writeRequestRecordsPerSec, long readRequestBytes, double readRequestsMbPerSec,
                           long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                           long writeResponsePendingBytes, long readResponsePendingRecords,
                           long readResponsePendingBytes, long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                           long writeMissEvents, double writeMissEventsPerSec,
                           long readMissEvents, double readMissEventsPerSec,
                           double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                           long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        StringBuilder out = new StringBuilder("Total " + prefix);
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestsMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeMissEvents, writeMissEventsPerSec, readMissEvents, readMissEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        System.out.println(out);
    }

}

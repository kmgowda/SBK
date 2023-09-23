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

import io.perl.data.Bytes;
import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.Time;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Class for recoding/printing results to a CSV file called `out.csv`.
 */
public class CSVLogger extends SystemLogger {
    /**
     * <code>DISABLE_STRING = "no"</code>.
     */
    final static public String DISABLE_STRING = "no";

    /**
     * <code>REGULAR_PRINT = "Regular"</code>.
     */
    final static public String REGULAR_PRINT = "Regular";

    /**
     * <code>TOTAL_PRINT = "Total"</code>.
     */
    final static public String TOTAL_PRINT = "Total";
    /**
     * <code>csvFile = null</code>.
     */
    protected String csvFile;
    /**
     * <code>csvEnable = false</code>.
     */
    protected boolean csvEnable;
    /**
     * <code>csvWriter</code>.
     */
    private PrintWriter csvWriter;
    /**
     * <code>csvRowCounter = 0</code>.
     */
    private long csvRowCounter;

    /**
     * calls its super class SystemLogger.
     */
    public CSVLogger() {
        super();
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        params.addOption("csvfile", true,
                """
                        CSV file to record results;
                        'no' disables this option, default: no""");
        csvEnable = false;
        csvFile = DISABLE_STRING;
        csvRowCounter = 0;
    }

    @Override
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        csvFile = params.getOptionValue("csvfile", DISABLE_STRING);
        csvEnable = csvFile.compareToIgnoreCase(DISABLE_STRING) != 0;
    }

    /**
     * final public method to Open CSV.
     *
     * @throws IOException If an exception occurred.
     */
    final public void openCSV() throws IOException {
        final StringBuilder headerBuilder = new StringBuilder("ID,Header,Type,Connections,MaxConnections");
        headerBuilder.append(",Storage,Action,LatencyTimeUnit");
        headerBuilder.append(",Writers,Readers,MaxWriters,MaxReaders");
        headerBuilder.append(",WriteRequestMB,WriteRequestRecords,WriteRequestRecords/Sec,WriteRequestMB/Sec");
        headerBuilder.append(",ReadRequestMB,ReadRequestRecords,ReadRequestRecords/Sec,ReadRequestMB/Sec");
        headerBuilder.append(",WriteResponsePendingMB,WriteResponsePendingRecords");
        headerBuilder.append(",ReadResponsePendingMB,ReadResponsePendingRecords");
        headerBuilder.append(",WriteReadRequestPendingMB,WriteReadRequestPendingRecords");
        headerBuilder.append(",WriteTimeoutEvents,WriteTimeoutEventsPerSec");
        headerBuilder.append(",ReadTimeoutEvents,ReadTimeoutEventsPerSec");
        headerBuilder.append(",ReportSeconds,MB,Records,Records/Sec,MB/Sec");
        headerBuilder.append(",AvgLatency,MinLatency,MaxLatency,InvalidLatencies,LowerDiscard,HigherDiscard,SLC1,SLC2");
        for (String percentileName : getPercentileNames()) {
            headerBuilder.append(",Percentile_");
            headerBuilder.append(percentileName);
        }
        csvWriter = new PrintWriter(Files.newBufferedWriter(Paths.get(csvFile)));
        csvWriter.println(headerBuilder);
    }


    @Override
    public void open(final ParsedOptions params, final String storageName, final Action action, Time time) throws IOException {
        super.open(params, storageName, action, time);
        if (csvEnable) {
            openCSV();
            Printer.log.info("SBK CSV Logger Started");
        }
    }

    /**
     * final public method to write CSV.
     *
     * @param header                         String
     * @param type                           String
     * @param connections                    long
     * @param maxConnections                 long
     * @param writers                        number of active writers
     * @param maxWriters                     Max writers
     * @param readers                        number of active readers
     * @param maxReaders                     Max Readers
     * @param writeRequestBytes              Write requests Bytes
     * @param writeRequestMbPerSec           Write requests MB/sec
     * @param writeRequestRecords            Write Requests
     * @param writeRequestRecordsPerSec      Write Requests/sec
     * @param readRequestBytes               Read requests Bytes
     * @param readRequestsMbPerSec           Read requests MB/sec
     * @param readRequestRecords             Read requests
     * @param readRequestRecordsPerSec       Read Requests/sec
     * @param writeResponsePendingRecords    Write response pending records
     * @param writeResponsePendingBytes      Write response pending bytes
     * @param readResponsePendingRecords     Read response pending records
     * @param readResponsePendingBytes       Read response pending bytes
     * @param writeReadRequestPendingRecords Write read pending records
     * @param writeReadRequestPendingBytes   Write read pending bytes
     * @param writeTimeoutEvents             Timeout Write Events
     * @param writeTimeoutEventsPerSec       Timeout Write Events/sec
     * @param readTimeoutEvents              Timeout Read Events
     * @param readTimeoutEventsPerSec        Timeout Write Events/sec
     * @param seconds                        reporting duration in seconds
     * @param bytes                          number of bytes read/write
     * @param records                        data to write.
     * @param recsPerSec                     records per second.
     * @param mbPerSec                       Throughput value in terms of MB (Mega Bytes) per Second.
     * @param avgLatency                     Average Latency.
     * @param minLatency                     Minimum Latency.
     * @param maxLatency                     Maximum Latency.
     * @param invalid                        Number of invalid/negative latencies.
     * @param lowerDiscard                   number of discarded latencies which are less than minimum latency.
     * @param higherDiscard                  number of discarded latencies which are higher than maximum latency.
     * @param slc1                           Sliding Latency Coverage factor
     * @param slc2                           Sliding Latency Coverage factor
     * @param percentileValues               Array of percentile Values.
     */
    final public void writeToCSV(String header, String type, long connections, long maxConnections,
                                 int writers, int maxWriters, int readers, int maxReaders,
                                 long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                                 double writeRequestRecordsPerSec, long readRequestBytes, double readRequestsMbPerSec,
                                 long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                                 long writeResponsePendingBytes, long readResponsePendingRecords,
                                 long readResponsePendingBytes, long writeReadRequestPendingRecords,
                                 long writeReadRequestPendingBytes,
                                 long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                                 long readTimeoutEvents, double readTimeoutEventsPerSec,
                                 double seconds, long bytes,
                                 long records, double recsPerSec, double mbPerSec,
                                 double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                                 long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        final double mBytes = (bytes * 1.0) / Bytes.BYTES_PER_MB;
        StringBuilder data = new StringBuilder(
                String.format("%16d,%s,%s,%s,%s"
                                + ",%s,%s,%s"
                                + ",%5d,%5d,%5d,%5d"
                                + ",%11.1f,%16d,%11.1f,%8.2f"
                                + ",%11.1f,%16d,%11.1f,%8.2f"
                                + ",%8.2f,%13d"
                                + ",%8.2f,%13d"
                                + ",%8.2f,%13d"
                                + ",%13d,%8.2f"
                                + ",%13d,%8.2f"
                                + ",%8d,%11.1f,%16d,%11.1f,%8.2f,%8.1f,%7d,%7d"
                                + ",%8d,%8d,%8d,%2d,%2d",
                        ++csvRowCounter, header, type, connections, maxConnections,
                        storageName, action.name(), timeUnitFullText,
                        writers, readers, maxWriters, maxReaders,
                        (writeRequestBytes * 1.0) / Bytes.BYTES_PER_MB, writeRequestRecords, writeRequestRecordsPerSec,
                        writeRequestMbPerSec,
                        (readRequestBytes * 1.0) / Bytes.BYTES_PER_MB, readRequestRecords, readRequestRecordsPerSec,
                        readRequestsMbPerSec,
                        (writeResponsePendingBytes * 1.0) / Bytes.BYTES_PER_MB, writeResponsePendingRecords,
                        (readResponsePendingBytes * 1.0) / Bytes.BYTES_PER_MB, readResponsePendingRecords,
                        (writeReadRequestPendingBytes * 1.0) / Bytes.BYTES_PER_MB, writeReadRequestPendingRecords,
                        writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                        (long) seconds, mBytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                        invalid, lowerDiscard, higherDiscard, slc1, slc2)
        );

        for (int i = 0; i < Math.min(getPercentiles().length, percentileValues.length); ++i) {
            data.append(String.format(",%7d", percentileValues[i]));
        }
        csvWriter.println(data);
    }


    @Override
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                      long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                      long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes,
                      long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        super.print(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec, writeRequestRecords,
                writeRequestRecordsPerSec, readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        if (csvEnable) {
            writeToCSV(Config.NAME, REGULAR_PRINT, 0, 0,
                    writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec,
                    writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes, readRequestMbPerSec,
                    readRequestRecords, readRequestRecordsPerSec,   writeResponsePendingRecords,
                    writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                    writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
    }

    @Override
    public void printTotal(int writers, int maxWriters, int readers, int maxReaders,
                           long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                           double writeRequestRecordsPerSec, long readRequestBytes, double readRequestsMbPerSec,
                           long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                           long writeResponsePendingBytes, long readResponsePendingRecords,
                           long readResponsePendingBytes, long writeReadRequestPendingRecords,
                           long writeReadRequestPendingBytes,
                           long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                           long readTimeoutEvents, double readTimeoutEventsPerSec,
                           double seconds, long bytes,
                           long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                           long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        super.printTotal(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec,
                writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes, readRequestsMbPerSec, readRequestRecords,
                readRequestRecordsPerSec, writeResponsePendingRecords, writeResponsePendingBytes,
                readResponsePendingRecords, readResponsePendingBytes, writeReadRequestPendingRecords,
                writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        if (csvEnable) {
            writeToCSV(Config.NAME, TOTAL_PRINT, 0, 0,
                    writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec,
                    writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes, readRequestsMbPerSec,
                    readRequestRecords, readRequestRecordsPerSec,   writeResponsePendingRecords,
                    writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                    writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
            csvWriter.flush();
        }
    }

    @Override
    public void close(final ParsedOptions params) throws IOException {
        super.close(params);
        if (csvEnable) {
            csvWriter.close();
            Printer.log.info("SBK CSV Logger Shutdown");
        }
    }
}

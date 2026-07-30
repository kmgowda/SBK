/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.logger.impl;

import io.sbk.action.Action;
import io.sbk.webconsole.WebConsoleLoggerSupport;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.Time;

import java.io.IOException;

/**
 * SBM logger that publishes aggregated distributed benchmark summaries to the Local Web Console.
 */
public class SbmWebLogger extends AbstractRamLogger {
    private final WebConsoleLoggerSupport webConsole;

    /**
     * Creates an SBM web logger without starting the Local Web Console.
     */
    public SbmWebLogger() {
        webConsole = new WebConsoleLoggerSupport();
    }

    @Override
    public void addArgs(InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        webConsole.addArgs(params);
    }

    @Override
    public void parseArgs(ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        webConsole.parseArgs(params);
    }

    @Override
    public void open(ParsedOptions params, String storageName, Action action, Time time) throws IOException {
        super.open(params, storageName, action, time);
        webConsole.open(getWebConsoleSource(), storageName, action, getTimeUnit(), getPercentiles());
        Printer.log.info("SBM WebLogger Started");
    }

    @Override
    public void close(ParsedOptions params) throws IOException {
        webConsole.close();
        super.close(params);
        Printer.log.info("SBM WebLogger Shutdown");
    }

    /**
     * Returns the application label included in Local Web Console run metadata.
     *
     * @return source application label
     */
    protected String getWebConsoleSource() {
        return "SBM";
    }

    /**
     * Returns Local Web Console option names for GEM argument segregation.
     *
     * @return Local Web Console option names
     */
    protected final String[] getWebConsoleOptionsArgs() {
        return webConsole.getOptionsArgs();
    }

    /**
     * Returns parsed Local Web Console arguments for forwarding to local SBM.
     *
     * @return Local Web Console option/value pairs
     */
    protected final String[] getWebConsoleParsedArgs() {
        return webConsole.getParsedArgs();
    }

    @Override
    public void print(long reportTime, int connections, int maxConnections, int writers, int maxWriters, int readers,
                      int maxReaders, long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes, long writeTimeoutEvents,
                      double writeTimeoutEventsPerSec, long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long minLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts) {
        final StringBuilder output = new StringBuilder(getTimeStamp(reportTime) + ", " + SBM_PREFIX);
        appendConnections(output, connections, maxConnections);
        output.append(getPrefix());
        appendResultString(output, writers, maxWriters, readers, maxReaders, writeRequestBytes,
                writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes,
                readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec, writeResponsePendingRecords,
                writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                writeReadRequestPendingRecords, writeReadRequestPendingBytes, writeTimeoutEvents,
                writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec, seconds, bytes, records,
                recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard,
                slc1, slc2, percentileLatencies, percentileLatencyCounts);
        System.out.println(output);
        publish(connections, maxConnections, writers, maxWriters, readers, maxReaders, writeRequestBytes,
                writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes,
                readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec, writeResponsePendingRecords,
                writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                writeReadRequestPendingRecords, writeReadRequestPendingBytes, writeTimeoutEvents,
                writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec, seconds, bytes, records,
                recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard,
                slc1, slc2, percentileLatencies, percentileLatencyCounts);
    }

    @Override
    public void printTotal(long reportTime, int connections, int maxConnections, int writers, int maxWriters,
                           int readers, int maxReaders, long writeRequestBytes, double writeRequestMbPerSec,
                           long writeRequestRecords, double writeRequestRecordsPerSec, long readRequestBytes,
                           double readRequestMbPerSec, long readRequestRecords, double readRequestRecordsPerSec,
                           long writeResponsePendingRecords, long writeResponsePendingBytes,
                           long readResponsePendingRecords, long readResponsePendingBytes,
                           long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                           long writeTimeoutEvents, double writeTimeoutEventsPerSec, long readTimeoutEvents,
                           double readTimeoutEventsPerSec, double seconds, long bytes, long records,
                           double recsPerSec, double mbPerSec, double avgLatency, long minLatency,
                           long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts) {
        final StringBuilder output = new StringBuilder(getTimeStamp(reportTime) + " Total : " + SBM_PREFIX);
        appendConnections(output, connections, maxConnections);
        output.append(getPrefix());
        appendResultString(output, writers, maxWriters, readers, maxReaders, writeRequestBytes,
                writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes,
                readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec, writeResponsePendingRecords,
                writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                writeReadRequestPendingRecords, writeReadRequestPendingBytes, writeTimeoutEvents,
                writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec, seconds, bytes, records,
                recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard,
                slc1, slc2, percentileLatencies, percentileLatencyCounts);
        System.out.println(output);
    }

    private void publish(int connections, int maxConnections, int writers, int maxWriters,
                         int readers, int maxReaders, long writeRequestBytes, double writeRequestMbPerSec,
                         long writeRequestRecords, double writeRequestRecordsPerSec, long readRequestBytes,
                         double readRequestMbPerSec, long readRequestRecords, double readRequestRecordsPerSec,
                         long writeResponsePendingRecords, long writeResponsePendingBytes,
                         long readResponsePendingRecords, long readResponsePendingBytes,
                         long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                         long writeTimeoutEvents, double writeTimeoutEventsPerSec, long readTimeoutEvents,
                         double readTimeoutEventsPerSec, double seconds, long bytes, long records,
                         double recsPerSec, double mbPerSec, double avgLatency, long minLatency, long maxLatency,
                         long invalid, long lowerDiscard, long higherDiscard, long slc1, long slc2,
                         long[] percentileLatencies, long[] percentileLatencyCounts) {
        webConsole.publish(connections, maxConnections, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid,
                lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
    }
}

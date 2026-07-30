/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.api.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.perl.api.LatencyRecord;
import io.perl.api.LatencyRecordWindow;
import io.perl.logger.Print;
import io.perl.api.ReportLatencies;
import io.perl.api.impl.TotalLatencyRecordWindow;
import io.sbm.api.SbmPeriodicRecorder;
import io.sbp.grpc.MessageLatenciesRecord;
import io.sbk.logger.ReadRequestsLogger;
import io.sbk.logger.SetRW;
import io.sbk.logger.WriteRequestsLogger;
import org.jetbrains.annotations.NotNull;

import static io.sbm.api.SbmRegistry.BASE_CLIENT_ID_VALUE;

/**
 * Class RamTotalWindowLatencyPeriodicRecorder.
 */
final public class SbmTotalWindowLatencyPeriodicRecorder extends TotalLatencyRecordWindow
        implements ReportLatencies, SbmPeriodicRecorder {
    final private ReportLatencies reportLatencies;
    final private SetRW setRW;

    final private WriteRequestsLogger wRequestLogger;
    final private ReadRequestsLogger rRequestLogger;
    private final int[] readersByClient;
    private final int[] writersByClient;
    private final int[] maxReadersByClient;
    private final int[] maxWritersByClient;
    private final boolean[] activeClients;
    private final int[] activeClientIds;
    private int activeClientCount;

    /**
     * Constructor RamTotalWindowLatencyPeriodicRecorder initialize all values and pass all values to its upper class.
     *
     * @param window                LatencyRecordWindow
     * @param totalWindow           LatencyRecordWindow
     * @param windowLogger          Print
     * @param totalLogger           Print
     * @param reportLatencies       ReportLatencies
     * @param setRW                 SetRW
     * @param wLogger               Write Requests Logger
     * @param rLogger               Read Requests Logger
     * @param maximumClients        maximum number of concurrently registered clients
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SbmTotalWindowLatencyPeriodicRecorder(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                                 Print windowLogger, Print totalLogger,
                                                 ReportLatencies reportLatencies,
                                                 SetRW setRW,
                                                 WriteRequestsLogger wLogger,
                                                 ReadRequestsLogger rLogger,
                                                 int maximumClients) {
        super(window, totalWindow, windowLogger, totalLogger);
        this.reportLatencies = reportLatencies;
        this.setRW = setRW;
        this.wRequestLogger = wLogger;
        this.rRequestLogger = rLogger;
        this.readersByClient = new int[maximumClients];
        this.writersByClient = new int[maximumClients];
        this.maxReadersByClient = new int[maximumClients];
        this.maxWritersByClient = new int[maximumClients];
        this.activeClients = new boolean[maximumClients];
        this.activeClientIds = new int[maximumClients];
        this.activeClientCount = 0;
    }

    @Override
    public void reportLatencyRecord(LatencyRecord record) {
        totalWindow.reportLatencyRecord(record);
        reportLatencies.reportLatencyRecord(record);

    }

    @Override
    public void reportLatency(long latency, long count) {
        totalWindow.reportLatency(latency, count);
        reportLatencies.reportLatency(latency, count);
    }

    /**
     * Record the latency.
     *
     * @param currentTime current time.
     * @param record      Record Latencies
     */
    public void record(long currentTime, MessageLatenciesRecord record) {
        addLatenciesRecord(record);
        checkWindowFullAndReset(currentTime);
    }

    /**
     * adds latencies record.
     *
     * @param record NotNull LatenciesRecord
     * @throws IllegalArgumentException if the client ID is outside the configured range
     */
    public void addLatenciesRecord(@NotNull MessageLatenciesRecord record) {
        final int id = (int) (record.getClientID() - BASE_CLIENT_ID_VALUE);
        addRW(record.getClientID(), record.getReaders(), record.getWriters(),
                record.getMaxReaders(), record.getMaxWriters());
        wRequestLogger.recordWriteRequests(id, 0, record.getWriteRequestBytes(),
                record.getWriteRequestRecords());
        wRequestLogger.recordWriteTimeoutEvents(id, 0, record.getWriteTimeoutEvents());

        rRequestLogger.recordReadRequests(id, 0, record.getReadRequestBytes(),
                record.getReadRequestRecords());
        rRequestLogger.recordReadTimeoutEvents(id, 0, record.getReadTimeoutEvents());

        window.update(record.getTotalRecords(), record.getTotalLatency(), record.getTotalBytes(),
                record.getInvalidLatencyRecords(), record.getLowerLatencyDiscardRecords(),
                record.getHigherLatencyDiscardRecords(), record.getValidLatencyRecords(),
                record.getMinLatency(), record.getMaxLatency());

        final int latencyCount = record.getLatencyValuesCount();
        for (int index = 0; index < latencyCount; index++) {
            window.reportLatency(record.getLatencyValues(index), record.getLatencyCounts(index));
        }
    }

    /**
     * Method flush.
     *
     * @param currentTime   long
     */
    public void flush(long currentTime) {
        sumRW();
        window.print(currentTime, windowLogger, this);
    }

    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    public void stopWindow(long currentTime) {
        flush(currentTime);
        checkTotalWindowFullAndReset(currentTime);
    }

    private void addRW(long key, int readers, int writers, int maxReaders, int maxWriters) {
        final int clientIndex = Math.toIntExact(key - BASE_CLIENT_ID_VALUE);
        if (clientIndex < 0 || clientIndex >= activeClients.length) {
            throw new IllegalArgumentException("SBM client ID is outside the configured range: " + key);
        }
        if (!activeClients[clientIndex]) {
            activeClients[clientIndex] = true;
            activeClientIds[activeClientCount++] = clientIndex;
        }
        readersByClient[clientIndex] = Math.max(readersByClient[clientIndex], readers);
        writersByClient[clientIndex] = Math.max(writersByClient[clientIndex], writers);
        maxReadersByClient[clientIndex] = Math.max(maxReadersByClient[clientIndex], maxReaders);
        maxWritersByClient[clientIndex] = Math.max(maxWritersByClient[clientIndex], maxWriters);
    }

    private void sumRW() {
        int readers = 0;
        int writers = 0;
        int maxReaders = 0;
        int maxWriters = 0;
        for (int activeIndex = 0; activeIndex < activeClientCount; activeIndex++) {
            final int clientIndex = activeClientIds[activeIndex];
            readers += readersByClient[clientIndex];
            writers += writersByClient[clientIndex];
            maxReaders += maxReadersByClient[clientIndex];
            maxWriters += maxWritersByClient[clientIndex];
            readersByClient[clientIndex] = 0;
            writersByClient[clientIndex] = 0;
            maxReadersByClient[clientIndex] = 0;
            maxWritersByClient[clientIndex] = 0;
            activeClients[clientIndex] = false;
        }
        activeClientCount = 0;
        setRW.setReaders(readers);
        setRW.setWriters(writers);
        setRW.setMaxReaders(maxReaders);
        setRW.setMaxWriters(maxWriters);
    }

}

/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.ram.impl;

import io.sbk.grpc.LatenciesRecord;
import io.sbk.logger.SetRW;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.LatencyRecordWindow;
import io.sbk.perl.Print;
import io.sbk.perl.ReportLatencies;
import io.sbk.perl.impl.TotalLatencyRecordWindow;
import io.sbk.ram.RamPeriodicRecorder;

import java.util.HashMap;

public class RamTotalWindowLatencyPeriodicRecorder extends TotalLatencyRecordWindow
        implements  ReportLatencies, RamPeriodicRecorder {
    final protected ReportLatencies reportLatencies;
    private final SetRW setRW;
    private final HashMap<Long, RW> table;

    public RamTotalWindowLatencyPeriodicRecorder(LatencyRecordWindow window, LatencyRecordWindow totalWindow,
                                                 Print windowLogger, Print totalLogger,
                                                 ReportLatencies reportLatencies,
                                                 SetRW setRW) {
        super(window, totalWindow, windowLogger, totalLogger);
        this.reportLatencies = reportLatencies;
        this.setRW = setRW;
        this.table = new HashMap<>();
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


    private static class RW {
        public int readers;
        public int writers;
        public int maxReaders;
        public int maxWriters;

        public RW() {
            reset();
        }

        public void reset() {
            readers = writers = maxWriters = maxReaders = 0;
        }

        public void update(int readers, int writers, int maxReaders, int maxWriters) {
            this.readers = Math.max(this.readers, readers);
            this.writers = Math.max(this.writers, writers);
            this.maxReaders = Math.max(this.maxReaders, maxReaders);
            this.maxWriters = Math.max(this.maxWriters, maxWriters);
        }
    }


    /**
     * Record the latency.
     *
     * @param currentTime current time.
     * @param record Record Latencies
     */
    public void record(long currentTime, LatenciesRecord record) {
        addLatenciesRecord(record);
        checkWindowOverflowAndReset(currentTime);
    }


    public void addLatenciesRecord(LatenciesRecord record) {
        addRW(record.getClientID(), record.getReaders(), record.getWriters(),
                record.getMaxReaders(), record.getMaxWriters());
        window.maxLatency = Math.max(record.getMaxLatency(), window.maxLatency);
        window.totalRecords += record.getTotalRecords();
        window.totalBytes += record.getTotalBytes();
        window.totalLatency += record.getTotalLatency();
        window.higherLatencyDiscardRecords += record.getHigherLatencyDiscardRecords();
        window.lowerLatencyDiscardRecords += record.getLowerLatencyDiscardRecords();
        window.validLatencyRecords += record.getValidLatencyRecords();
        window.invalidLatencyRecords += record.getInvalidLatencyRecords();
        record.getLatencyMap().forEach(window::reportLatency);
    }

    public void flush(long currentTime) {
        final RW rwStore = new RW();
        sumRW(rwStore);
        setRW.setReaders(rwStore.readers);
        setRW.setWriters(rwStore.writers);
        setRW.setMaxReaders(rwStore.maxReaders);
        setRW.setMaxWriters(rwStore.maxWriters);
        window.print(currentTime, windowLogger, this);
    }

    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    public void stopWindow(long currentTime) {
        flush(currentTime);
        checkTotalOverflowAndReset(currentTime);
    }


    private void addRW(long key, int readers, int writers, int maxReaders, int maxWriters) {
       RW cur = table.get(key);
        if (cur == null) {
            cur = new RW();
            table.put(key, cur);
        }
        cur.update(readers, writers, maxReaders, maxWriters);
    }

    private void sumRW(RW ret) {
        table.forEach((k, data) -> {
            ret.readers += data.readers;
            ret.writers += data.writers;
            ret.maxReaders += data.maxReaders;
            ret.maxWriters += data.maxWriters;
        });
        table.clear();
    }

}
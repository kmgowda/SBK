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
import io.sbk.perl.impl.HashMapLatencyRecorder;
import io.sbk.ram.RamPeriodicRecorder;
import io.sbk.system.Printer;
import io.sbk.perl.ReportLatencies;
import io.sbk.config.PerlConfig;
import io.sbk.perl.Print;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;


/**
 *  class for Performance statistics.
 */
@NotThreadSafe
public class RamHashMapLatencyPeriodicRecorder extends HashMapLatencyRecorder implements RamPeriodicRecorder {
    final protected LatencyRecordWindow window;
    final protected Print windowLogger;
    final protected Print loggerTotal;
    final protected ReportLatencies dualReportLatencies;
    private final SetRW setRW;
    private final HashMap<Long, RW> table;

    public RamHashMapLatencyPeriodicRecorder(LatencyRecordWindow window, int maxHashMapSizeMB, Print logger,
                                             Print loggerTotal, ReportLatencies reportLatencies, SetRW setRW) {
        super(window.lowLatency, window.highLatency, window.totalLatencyMax,
                window.totalRecordsMax, window.totalBytesMax, window.percentiles.fractions, window.time, maxHashMapSizeMB);
        this.window = window;
        this.windowLogger = logger;
        this.loggerTotal = loggerTotal;
        this.dualReportLatencies = new DualReportLatencies(this, reportLatencies);
        this.setRW = setRW;
        this.table = new HashMap<>();
    }


    private static class DualReportLatencies implements ReportLatencies {
        final private ReportLatencies r1;
        final private ReportLatencies r2;

        public DualReportLatencies(ReportLatencies r1, ReportLatencies r2) {
            this.r1 = r1;
            this.r2 = r2;
        }

        @Override
        public void reportLatencyRecord(LatencyRecord record) {
            r1.reportLatencyRecord(record);
            r2.reportLatencyRecord(record);
        }

        @Override
        public void reportLatency(long latency, long count) {
            r1.reportLatency(latency, count);
            r2.reportLatency(latency, count);
        }
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
     * Start the window.
     *
     * @param startTime starting time.
     */
    public void start(long startTime) {
        reset(startTime);
    }

    /**
     * Reset the window.
     *
     * @param startTime starting time.
     */
    public void startWindow(long startTime) {
        window.reset(startTime);
    }


    /**
     * Get the current time duration of this window.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds from the startTime.
     */
    public long elapsedMilliSecondsWindow(long currentTime) {
        return window.elapsedMilliSeconds(currentTime);
    }


    /**
     * Record the latency.
     *
     * @param currentTime current time.
     * @param record Record Latencies
     */
    public void record(long currentTime, LatenciesRecord record) {
        addLatenciesRecord(record);
        if (window.isOverflow()) {
            flush(currentTime);
            if (isOverflow()) {
                print(currentTime, loggerTotal, null);
                reset(currentTime);
            }
        }
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
        window.print(currentTime, windowLogger, this.dualReportLatencies);
    }

    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    public void stopWindow(long currentTime) {
        flush(currentTime);
        if (isOverflow()) {
            if (hashMapBytesCount > maxHashMapSizeBytes) {
                Printer.log.warn("Hash Map memory size: " + maxHashMapSizeMB +
                        " exceeded! Current HashMap size in MB: " + (hashMapBytesCount / PerlConfig.BYTES_PER_MB));
            } else {
                Printer.log.warn("Total Bytes: " + totalBytes + ",  Total Records:" + totalRecords +
                        ", Total Latency: "+  totalLatency );
            }
            print(currentTime, loggerTotal, null);
            start(currentTime);
        }
    }

    /**
     * print the Final Latency Results.
     *
     * @param endTime current time.
     */
    public void stop(long endTime) {
        if (window.totalRecords > 0) {
            flush(endTime);
        }
        print(endTime, loggerTotal, null);
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

/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.perl.api.PerlChannel;
import io.perl.api.RunBenchmark;
import io.sbk.api.BiConsumer;
import io.sbk.api.DataWriter;
import io.sbk.logger.WriteRequestsLogger;
import io.sbk.params.ParameterOptions;
import io.sbk.api.RateController;
import io.sbk.api.Worker;
import io.sbk.data.DataType;
import io.sbk.logger.CountWriters;
import io.sbk.system.Printer;
import io.time.Time;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Writer Benchmarking Implementation.
 */
final public class SbkWriter extends Worker implements RunBenchmark {
    final private DataType<Object> dType;
    final private DataWriter<Object> writer;
    final private Time time;
    final private CountWriters wCount;
    final private ExecutorService executor;
    final private BiConsumer perf;
    final private RateController rCnt;
    final private Object payload;
    final private int dataSize;

    final private WriteRequestsLogger requestsLogger;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SbkWriter(int writerID, ParameterOptions params, PerlChannel perlChannel,
                     @NotNull DataType<Object> dType, Time time, DataWriter<Object> writer,
                     CountWriters wCount, WriteRequestsLogger requestsLogger, ExecutorService executor) {
        super(writerID, params, perlChannel);
        this.dType = dType;
        this.time = time;
        this.writer = writer;
        this.wCount = wCount;
        this.requestsLogger = requestsLogger;
        this.executor = executor;
        this.perf = createBenchmark();
        this.rCnt = new SbkRateController();
        this.payload = dType.create(params.getRecordSize());
        this.dataSize = dType.length(this.payload);
    }

    @Override
    public CompletableFuture<Void> run(long secondsToRun, long recordsCount) throws IOException, EOFException,
            IllegalStateException {
        return CompletableFuture.runAsync(() -> {
            wCount.incrementWriters();
            try {
                if (secondsToRun > 0) {
                    Printer.log.info("Writer " + id + " started , run seconds: " + secondsToRun);
                } else {
                    Printer.log.info("Writer " + id + " started , records: " + recordsCount);
                }
                perf.apply(secondsToRun, recordsCount);
                Printer.log.info("Writer " + id + " exited");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            wCount.decrementWriters();
        }, executor);
    }

    private @NotNull BiConsumer createBenchmark() {
        final BiConsumer perfWriter;
        if (params.getTotalSecondsToRun() > 0) {
            perfWriter = switch (params.getAction()) {
                case Write_OnlyReading -> requestsLogger != null ? this::RecordsWriterTimeROandRequests :
                    this::RecordsWriterTimeRO;
                case Write_Reading -> requestsLogger != null ? this::RecordsWriterTimeRWandRequests :
                    this::RecordsWriterTimeRW;
                default -> params.getRecordsPerSec() > 0 || params.getRecordsPerSync() < Integer.MAX_VALUE ?
                        requestsLogger != null ? this::RecordsWriterTimeSyncAndRequests : this::RecordsWriterTimeSync
                        :
                        requestsLogger != null ? this::RecordsWriterTimeAndRequests : this::RecordsWriterTime;
            };
        } else {
            perfWriter = switch (params.getAction()) {
                case Write_OnlyReading -> requestsLogger != null ? this::RecordsWriterROandRequests :
                    this::RecordsWriterRO;
                case Write_Reading -> requestsLogger != null ? this::RecordsWriterRWandRequests :
                    this::RecordsWriterRW;
                default -> params.getRecordsPerSec() > 0 || params.getRecordsPerSync() < Integer.MAX_VALUE ?
                        requestsLogger != null ? this::RecordsWriterSyncAndRequests : this::RecordsWriterSync
                        :
                        requestsLogger != null ? this::RecordsWriterAndRequests : this::RecordsWriter;
            };
        }
        return perfWriter;
    }

    private void RecordsWriter(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriter(this, recordsCount, dType, payload, dataSize, time);
    }


    private void RecordsWriterAndRequests(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriter(this, recordsCount, dType, payload, dataSize, time, requestsLogger);
    }


    private void RecordsWriterSync(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterSync(this, recordsCount, dType, payload, dataSize, time, rCnt);
    }

    private void RecordsWriterSyncAndRequests(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterSync(this, recordsCount, dType, payload, dataSize, time, rCnt, requestsLogger);
    }


    private void RecordsWriterTime(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTime(this, secondsToRun, dType, payload, dataSize, time);
    }


    private void RecordsWriterTimeAndRequests(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTime(this, secondsToRun, dType, payload, dataSize, time, requestsLogger);
    }


    private void RecordsWriterTimeSync(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeSync(this, secondsToRun, dType, payload, dataSize, time, rCnt);
    }


    private void RecordsWriterTimeSyncAndRequests(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeSync(this, secondsToRun, dType, payload, dataSize, time, rCnt, requestsLogger);
    }


    private void RecordsWriterRW(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterRW(this, recordsCount, dType, payload, dataSize, time, rCnt);
    }


    private void RecordsWriterRWandRequests(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterRW(this, recordsCount, dType, payload, dataSize, time, rCnt, requestsLogger);
    }

    private void RecordsWriterTimeRW(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeRW(this, secondsToRun, dType, payload, dataSize, time, rCnt);
    }

    private void RecordsWriterTimeRWandRequests(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeRW(this, secondsToRun, dType, payload, dataSize, time, rCnt, requestsLogger);
    }

    private void RecordsWriterRO(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterRO(this, recordsCount, dType, payload, dataSize, time, rCnt);
    }

    private void RecordsWriterROandRequests(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterRO(this, recordsCount, dType, payload, dataSize, time, rCnt, requestsLogger);
    }

    private void RecordsWriterTimeRO(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeRO(this, secondsToRun, dType, payload, dataSize, time, rCnt);
    }

    private void RecordsWriterTimeROandRequests(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeRO(this, secondsToRun, dType, payload, dataSize, time, rCnt, requestsLogger);
    }

}

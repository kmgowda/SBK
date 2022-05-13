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

import io.perl.api.PerlChannel;
import io.perl.api.RunBenchmark;
import io.sbk.api.BiConsumer;
import io.sbk.api.DataWriter;
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

    public SbkWriter(int writerID, ParameterOptions params, PerlChannel perlChannel,
                     @NotNull DataType<Object> dType, Time time, DataWriter<Object> writer,
                     CountWriters wCount, ExecutorService executor) {
        super(writerID, params, perlChannel);
        this.dType = dType;
        this.time = time;
        this.writer = writer;
        this.wCount = wCount;
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
            if (params.isWriteAndRead()) {
                perfWriter = params.isReadOnly() ? this::RecordsWriterTimeRO : this::RecordsWriterTimeRW;
            } else {
                perfWriter = params.getRecordsPerSec() > 0 || params.getRecordsPerSync() < Integer.MAX_VALUE ?
                        this::RecordsWriterTimeSync : this::RecordsWriterTime;
            }
        } else {
            if (params.isWriteAndRead()) {
                perfWriter = params.isReadOnly() ? this::RecordsWriterRO : this::RecordsWriterRW;
            } else {
                perfWriter = params.getRecordsPerSec() > 0 || params.getRecordsPerSync() < Integer.MAX_VALUE ?
                        this::RecordsWriterSync : this::RecordsWriter;
            }
        }
        return perfWriter;
    }

    private void RecordsWriter(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriter(this, recordsCount, dType, payload, dataSize, time);
    }


    private void RecordsWriterSync(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterSync(this, recordsCount, dType, payload, dataSize, time, rCnt);
    }


    private void RecordsWriterTime(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTime(this, secondsToRun, dType, payload, dataSize, time);
    }


    private void RecordsWriterTimeSync(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeSync(this, secondsToRun, dType, payload, dataSize, time, rCnt);
    }


    private void RecordsWriterRW(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterRW(this, recordsCount, dType, payload, dataSize, time, rCnt);
    }

    private void RecordsWriterTimeRW(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeRW(this, secondsToRun, dType, payload, dataSize, time, rCnt);
    }

    private void RecordsWriterRO(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterRO(this, recordsCount, dType, payload, dataSize, time, rCnt);
    }

    private void RecordsWriterTimeRO(long secondsToRun, long recordsCount) throws IOException {
        writer.RecordsWriterTimeRO(this, secondsToRun, dType, payload, dataSize, time, rCnt);
    }

}

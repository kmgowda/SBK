/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.sbk.api.BiConsumer;
import io.sbk.api.DataType;
import io.sbk.api.DataWriter;
import io.sbk.api.Parameters;
import io.sbk.api.RateController;
import io.sbk.api.RunBenchmark;

import io.sbk.perl.SendChannel;
import io.sbk.perl.Time;
import io.sbk.api.Worker;
import io.sbk.system.Printer;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Writer Benchmarking Implementation.
 */
public class SbkWriter extends Worker implements RunBenchmark {
    final private DataType<Object> dType;
    final private DataWriter<Object> writer;
    final private Time time;
    final private BiConsumer perf;
    final private RateController rCnt;
    final private Object payload;
    final private int dataSize;

    public SbkWriter(int writerID, int idMax, Parameters params, SendChannel sendChannel,
                     DataType<Object> dType, Time time, DataWriter<Object> writer) {
        super(writerID, idMax, params, sendChannel);
        this.dType = dType;
        this.writer = writer;
        this.time = time;
        this.perf = createBenchmark();
        this.rCnt = new SbkRateController();
        this.payload = dType.create(params.getRecordSize());
        this.dataSize = dType.length(this.payload);
    }

    @Override
    public CompletableFuture<Void> run(long secondsToRun, long recordsCount) throws IOException, EOFException,
            IllegalStateException {
        return  CompletableFuture.runAsync( () -> {
            try {
                if (secondsToRun > 0) {
                    Printer.log.info("Writer " + id +" started , seconds to run: "+secondsToRun);
                } else {
                    Printer.log.info("Writer " + id +" started , records per writer: "+recordsCount);
                }
                perf.apply(secondsToRun, recordsCount);
                Printer.log.info("Writer " + id +" exited");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private BiConsumer createBenchmark() {
        final BiConsumer perfWriter;
        if (params.getTotalSecondsToRun() > 0) {
            if (params.isWriteAndRead()) {
                perfWriter = this::RecordsWriterTimeRW;
            } else {
                if (params.getRecordsPerSec() > 0 || params.getRecordsPerSync() < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterTimeSync;
                } else {
                    perfWriter = this::RecordsWriterTime;
                }
            }
        } else {
            if (params.isWriteAndRead()) {
                perfWriter = this::RecordsWriterRW;
            } else {
                if (params.getRecordsPerSec() > 0 || params.getRecordsPerSync() < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterSync;
                } else {
                    perfWriter = this::RecordsWriter;
                }
            }
        }
        return perfWriter;
    }

    private void RecordsWriter(long secondsToRun, long recordsCount) throws  IOException {
        writer.RecordsWriter(this, recordsCount, dType, payload, dataSize, time);
    }


    private void RecordsWriterSync(long secondsToRun, long recordsCount) throws  IOException {
        writer.RecordsWriterSync(this, recordsCount, dType, payload, dataSize, time, rCnt);
    }


    private void RecordsWriterTime(long secondsToRun, long recordsCount) throws  IOException {
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

}

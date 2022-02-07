/**
 * Copyright (c) KMG. All Rights Reserved.
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
import io.sbk.api.DataReader;
import io.sbk.api.ParameterOptions;
import io.sbk.api.RateController;
import io.sbk.api.Worker;
import io.sbk.data.DataType;
import io.sbk.logger.CountReaders;
import io.sbk.system.Printer;
import io.time.Time;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Reader Benchmarking Implementation.
 */
final public class SbkReader extends Worker implements RunBenchmark {
    final private DataType<Object> dType;
    final private DataReader<Object> reader;
    final private Time time;
    final private CountReaders rCount;
    final private ExecutorService executor;
    final private RateController rCnt;
    final private BiConsumer perf;


    public SbkReader(int readerId, ParameterOptions params, PerlChannel perlChannel,
                     DataType<Object> dType, Time time, DataReader<Object> reader,
                     CountReaders rCount, ExecutorService executor) {
        super(readerId, params, perlChannel);
        this.dType = dType;
        this.time = time;
        this.reader = reader;
        this.rCount = rCount;
        this.executor = executor;
        this.rCnt = new SbkRateController();
        this.perf = createBenchmark();
    }

    @Override
    public CompletableFuture<Void> run(long secondsToRun, long recordsCount) throws IOException, EOFException,
            IllegalStateException {
        return CompletableFuture.runAsync(() -> {
            rCount.incrementReaders();
            try {
                if (secondsToRun > 0) {
                    Printer.log.info("Reader " + id + " started , run seconds: " + secondsToRun);
                } else {
                    Printer.log.info("Reader " + id + " started , records: " + recordsCount);
                }
                perf.apply(secondsToRun, recordsCount);
                Printer.log.info("Reader " + id + " exited");
            } catch (EOFException ex) {
                Printer.log.info("Reader " + id + " exited with EOF");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            rCount.decrementReaders();
        }, executor);
    }

    private @NotNull BiConsumer createBenchmark() {
        final BiConsumer perfReader;
        if (params.getTotalSecondsToRun() > 0) {
            if (params.isWriteAndRead()) {
                perfReader = params.getRecordsPerSec() > 0 ? this::RecordsTimeReaderRWRateControl : this::RecordsTimeReaderRW;
            } else {
                perfReader = params.getRecordsPerSec() > 0 ? this::RecordsTimeReaderRateControl : this::RecordsTimeReader;
            }
        } else {
            if (params.isWriteAndRead()) {
                perfReader = params.getRecordsPerSec() > 0 ? this::RecordsReaderRWRateControl : this::RecordsReaderRW;
            } else {
                perfReader = params.getRecordsPerSec() > 0 ? this::RecordsReaderRateControl : this::RecordsReader;
            }
        }
        return perfReader;
    }

    private void RecordsReader(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReader(this, recordsCount, dType, time);
    }


    private void RecordsReaderRW(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReaderRW(this, recordsCount, dType, time);
    }

    private void RecordsTimeReader(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReader(this, secondsToRun, dType, time);
    }

    private void RecordsTimeReaderRW(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReaderRW(this, secondsToRun, dType, time);
    }

    private void RecordsReaderRateControl(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReaderRateControl(this, recordsCount, dType, time, rCnt);
    }

    private void RecordsReaderRWRateControl(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReaderRWRateControl(this, recordsCount, dType, time, rCnt);
    }

    private void RecordsTimeReaderRateControl(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReaderRateControl(this, secondsToRun, dType, time, rCnt);
    }

    private void RecordsTimeReaderRWRateControl(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReaderRWRateControl(this, secondsToRun, dType, time, rCnt);
    }

}

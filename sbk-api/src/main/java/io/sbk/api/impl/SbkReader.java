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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.perl.api.PerlChannel;
import io.perl.api.RunBenchmark;
import io.sbk.action.Action;
import io.sbk.api.BiConsumer;
import io.sbk.api.DataReader;
import io.sbk.logger.ReadRequestsLogger;
import io.sbk.params.ParameterOptions;
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
 *
 * <p>This class adapts a {@link io.sbk.api.DataReader} into the SBK harness
 * by implementing the {@link io.perl.api.RunBenchmark} contract. It wires the
 * reader instance with per-worker context (id, params, perlChannel) and
 * exposes a set of pre-built benchmark variants (time-based, count-based,
 * with/without rate control and optional per-request logging).
 *
 * <p>Key behavior:
 * <ul>
 *   <li>The {@link #run(long,long)} method executes asynchronously on the
 *       supplied executor and returns a {@link java.util.concurrent.CompletableFuture}.</li>
 *   <li>Depending on {@link io.sbk.params.ParameterOptions}, the class selects
 *       specialized benchmark paths (e.g. {@code RecordsReaderRateControl}).</li>
 *   <li>It uses {@link io.sbk.api.impl.SbkRateController} to pace reads when a
 *       records-per-second limit is provided.</li>
 * </ul>
 *
 * <p>Implementors and maintainers: keep this class focused on orchestration —
 * the actual I/O semantics live in the driver-provided {@link io.sbk.api.DataReader}
 * implementation, which may override default 'recordRead' helpers for batching
 * or more efficient reads.
 */
final public class SbkReader extends Worker implements RunBenchmark {
    final private DataType<Object> dType;
    final private DataReader<Object> reader;
    final private Time time;
    final private CountReaders rCount;

    final private ReadRequestsLogger readRequestsLogger;

    final private ExecutorService executor;
    final private RateController rCnt;
    final private BiConsumer perf;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SbkReader(int readerId, ParameterOptions params, PerlChannel perlChannel,
                     DataType<Object> dType, Time time, DataReader<Object> reader,
                     CountReaders rCount, ReadRequestsLogger readRequestsLogger, ExecutorService executor) {
        super(readerId, params, perlChannel);
        this.dType = dType;
        this.time = time;
        this.reader = reader;
        this.rCount = rCount;
        this.readRequestsLogger = readRequestsLogger;
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
            if (params.getAction() == Action.Write_Reading) {
                perfReader = params.getRecordsPerSec() > 0 ?
                        readRequestsLogger != null ? this::RecordsTimeReaderRWRateControlAndRequests :
                                this::RecordsTimeReaderRWRateControl
                        :
                        readRequestsLogger != null ? this::RecordsTimeReaderRWandRequests :  this::RecordsTimeReaderRW;
            } else  {
                perfReader = params.getRecordsPerSec() > 0 ?
                        readRequestsLogger != null ? this::RecordsTimeReaderRateControlAndRequests :
                                this::RecordsTimeReaderRateControl
                        :
                        readRequestsLogger != null ? this::RecordsTimeReaderAndRequests : this::RecordsTimeReader;
            }
        } else {
            if (params.getAction() == Action.Write_Reading) {
                perfReader = params.getRecordsPerSec() > 0 ?
                        readRequestsLogger != null ? this::RecordsReaderRWRateControlAndRequests :
                                this::RecordsReaderRWRateControl
                        :
                        readRequestsLogger != null ? this::RecordsReaderRWandRequests : this::RecordsReaderRW;
            } else {
                perfReader = params.getRecordsPerSec() > 0 ?
                        readRequestsLogger != null ? this::RecordsReaderRateControlAndRequests :
                                this::RecordsReaderRateControl
                        :
                        readRequestsLogger != null ? this::RecordsReaderAndRequests : this::RecordsReader;
            }
        }
        return perfReader;
    }

    private void RecordsReader(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReader(this, recordsCount, dType, time);
    }

    private void RecordsReaderAndRequests(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReader(this, recordsCount, dType, time, readRequestsLogger);
    }


    private void RecordsReaderRW(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReaderRW(this, recordsCount, dType, time);
    }

    private void RecordsReaderRWandRequests(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReaderRW(this, recordsCount, dType, time, readRequestsLogger);
    }

    private void RecordsTimeReader(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReader(this, secondsToRun, dType, time);
    }

    private void RecordsTimeReaderAndRequests(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReader(this, secondsToRun, dType, time, readRequestsLogger);
    }

    private void RecordsTimeReaderRW(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReaderRW(this, secondsToRun, dType, time);
    }

    private void RecordsTimeReaderRWandRequests(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReaderRW(this, secondsToRun, dType, time, readRequestsLogger);
    }

    private void RecordsReaderRateControl(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReaderRateControl(this, recordsCount, dType, time, rCnt);
    }

    private void RecordsReaderRateControlAndRequests(long secondsToRun, long recordsCount) throws EOFException,
            IOException {
        reader.RecordsReaderRateControl(this, recordsCount, dType, time, rCnt, readRequestsLogger);
    }

    private void RecordsReaderRWRateControl(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsReaderRWRateControl(this, recordsCount, dType, time, rCnt);
    }

    private void RecordsReaderRWRateControlAndRequests(long secondsToRun, long recordsCount) throws EOFException,
            IOException {
        reader.RecordsReaderRWRateControl(this, recordsCount, dType, time, rCnt, readRequestsLogger);
    }

    private void RecordsTimeReaderRateControl(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReaderRateControl(this, secondsToRun, dType, time, rCnt);
    }

    private void RecordsTimeReaderRateControlAndRequests(long secondsToRun, long recordsCount) throws EOFException,
            IOException {
        reader.RecordsTimeReaderRateControl(this, secondsToRun, dType, time, rCnt, readRequestsLogger);
    }

    private void RecordsTimeReaderRWRateControl(long secondsToRun, long recordsCount) throws EOFException, IOException {
        reader.RecordsTimeReaderRWRateControl(this, secondsToRun, dType, time, rCnt);
    }

    private void RecordsTimeReaderRWRateControlAndRequests(long secondsToRun, long recordsCount) throws EOFException,
            IOException {
        reader.RecordsTimeReaderRWRateControl(this, secondsToRun, dType, time, rCnt, readRequestsLogger);
    }

}

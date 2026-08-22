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
import io.sbk.action.Action;
import io.sbk.api.Benchmark;
import io.sbk.api.Callback;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Worker;
import io.sbk.data.DataType;
import io.time.Time;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SBK Callback reader implementation.
 *
 * <p>This deprecated helper adapts callback-style drivers into the older SBK
 * Benchmark interface. It is retained for backwards compatibility but has been
 * superseded by the {@link io.sbk.api.impl.SbkReader} / {@link io.sbk.api.AbstractCallbackReader}
 * classes which provide richer orchestration and explicit lifecycle management.
 *
 * <p>If you are maintaining drivers, prefer implementing a {@link io.sbk.api.DataReader}
 * (synchronous or asynchronous) and integrate with {@link io.sbk.api.impl.SbkReader}
 * instead of using this deprecated callback path.
 *
 * @deprecated This interface is replaced by Abstract class AbstractCallbackReader and SbkReader.
 */
@Deprecated
final public class SbkCallbackReader extends Worker implements Callback<Object>, Benchmark {
    final private DataType<Object> dataType;
    final private Time time;
    final private CompletableFuture<Void> ret;
    final private Callback<Object> callback;
    final private BenchmarkRecorder benchmarkRecorder;
    private long beginTime;

    /**
     * Creates the deprecated callback-based benchmark reader.
     *
     * @param readerId reader identifier
     * @param params benchmark parameters
     * @param perlChannel performance channel
     * @param idMax maximum reader identifier
     * @param dataType storage data type
     * @param time benchmark time source
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SbkCallbackReader(int readerId, ParameterOptions params, PerlChannel perlChannel, int idMax,
                             DataType<Object> dataType, Time time) {
        super(readerId, params, perlChannel);
        this.dataType = dataType;
        this.time = time;
        this.ret = new CompletableFuture<>();
        this.beginTime = 0;
        if (params.getTotalSecondsToRun() > 0) {
            benchmarkRecorder = new DurationBenchmarkRecorder(
                    time.secondsToTimeUnits(params.getTotalSecondsToRun()));
        } else if (params.getTotalRecords() > 0) {
            benchmarkRecorder = new RecordsBenchmarkRecorder(params.getTotalRecords());
        } else {
            benchmarkRecorder = new UnboundedBenchmarkRecorder();
        }

        if (params.getAction() == Action.Write_Reading) {
            callback = this::consumeRW;
        } else {
            callback = this::consumeRead;
        }
    }

    @Override
    public CompletableFuture<Void> start() {
        this.beginTime = time.getCurrentTime();
        return ret.toCompletableFuture();
    }

    @Override
    public void stop() {
        ret.complete(null);
    }


    /**
     * Records a completed callback batch and completes the benchmark when its
     * configured duration or record target is reached.
     *
     * @param startTime operation start time in the configured time unit
     * @param endTime operation completion time in the configured time unit
     * @param dataSize total bytes represented by the callback batch
     * @param events number of records represented by the callback batch
     */
    @Override
    public void record(long startTime, long endTime, int dataSize, int events) {
        benchmarkRecorder.record(startTime, endTime, dataSize, events);
    }

    @Override
    public void consume(Object data) {
        callback.consume(data);
    }

    private void consumeRead(Object data) {
        final long endTime = time.getCurrentTime();
        record(endTime, endTime, dataType.length(data), 1);
    }

    private void consumeRW(Object data) {
        record(dataType.getTime(data), time.getCurrentTime(), dataType.length(data), 1);
    }

    private interface BenchmarkRecorder {
        void record(long startTime, long endTime, int dataSize, int events);
    }

    private final class DurationBenchmarkRecorder implements BenchmarkRecorder {
        private final long timeUnitsToRun;

        private DurationBenchmarkRecorder(long timeUnitsToRun) {
            this.timeUnitsToRun = timeUnitsToRun;
        }

        @Override
        public void record(long startTime, long endTime, int dataSize, int events) {
            perlChannel.send(startTime, endTime, events, dataSize);
            if (time.elapsed(endTime, beginTime) >= timeUnitsToRun) {
                ret.complete(null);
            }
        }
    }

    private final class RecordsBenchmarkRecorder implements BenchmarkRecorder {
        private final AtomicLong readCount = new AtomicLong();
        private final long totalRecords;

        private RecordsBenchmarkRecorder(long totalRecords) {
            this.totalRecords = totalRecords;
        }

        @Override
        public void record(long startTime, long endTime, int dataSize, int events) {
            final long count = readCount.addAndGet(events);
            perlChannel.send(startTime, endTime, events, dataSize);
            if (count >= totalRecords) {
                ret.complete(null);
            }
        }
    }

    private final class UnboundedBenchmarkRecorder implements BenchmarkRecorder {
        @Override
        public void record(long startTime, long endTime, int dataSize, int events) {
            perlChannel.send(startTime, endTime, events, dataSize);
        }
    }
}

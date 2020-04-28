/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import io.sbk.api.Benchmark;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;
import io.sbk.api.Callback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;


public class SbkCallback extends Worker implements Callback, Benchmark {
    final private DataType dataType;
    final private CompletableFuture<Void> ret;
    final private Callback callback;
    final private AtomicLong readCnt;
    private long beginTime;

    public SbkCallback(int readerId, int idMax, Parameters params, RecordTime recordTime, DataType dataType) {
        super(readerId, idMax, params, recordTime);
        this.dataType = dataType;
        this.ret = new CompletableFuture();
        this.readCnt = new AtomicLong(0);
        this.beginTime = 0;
        if (params.isWriteAndRead()) {
            callback = this::consumeRW;
        } else {
            callback = this::consumeRead;
        }
    }

    @Override
    public CompletableFuture<Void> start(long statTime) {
        this.beginTime = statTime;
        return ret;
    }

    @Override
    public void stop(long endTime) {
        ret.complete(null);
    }


    @Override
    public void record(long startTime, long endTime, int dataSize, int events) {
        final long cnt = readCnt.incrementAndGet();
        final int id = (int) (cnt % idMax);
        recordTime.accept(id, startTime, endTime, dataSize, events);
        if (params.getSecondsToRun() > 0 && (((endTime - beginTime) / 1000) >= params.getSecondsToRun())) {
            ret.complete(null);
        } else if (params.getRecordsPerReader() > cnt) {
            ret.complete(null);
        }
    }

    @Override
    public void consume(Object data) {
            callback.consume(data);
    }

    final private void consumeRead(Object data) {
        final long endTime = System.currentTimeMillis();
        record(endTime, endTime, dataType.length(data), 1);
    }

    final private void consumeRW(Object data) {
        record(dataType.getTime(data), System.currentTimeMillis(), dataType.length(data), 1);
    }
}

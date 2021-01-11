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
import io.sbk.api.Config;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.SendChannel;
import io.sbk.api.Callback;
import io.sbk.api.Time;
import io.sbk.api.Worker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;


public class SbkCallbackReader extends Worker implements Callback, Benchmark {
    final private DataType dataType;
    final private Time time;
    final private CompletableFuture<Void> ret;
    final private Callback callback;
    final private AtomicLong readCnt;
    final private long msToRun;
    final private int totalRecords;
    private long beginTime;

    public SbkCallbackReader(int readerId, int idMax, Parameters params, SendChannel sendChannel,
                             DataType dataType, Time time) {
        super(readerId, idMax,  params, sendChannel);
        this.dataType = dataType;
        this.time = time;
        this.ret = new CompletableFuture<>();
        this.readCnt = new AtomicLong(0);
        this.beginTime = 0;
        this.msToRun = params.getSecondsToRun() * Config.MS_PER_SEC;
        this.totalRecords = params.getRecordsPerReader() * params.getReadersCount();

        if (params.isWriteAndRead()) {
            callback = this::consumeRW;
        } else {
            callback = this::consumeRead;
        }
    }

    @Override
    public CompletableFuture<Void> start() {
        this.beginTime = time.getCurrentTime();
        return ret;
    }

    @Override
    public void stop() {
        ret.complete(null);
    }


    @Override
    public void record(long startTime, long endTime, int dataSize, int events) {
        final long cnt = readCnt.incrementAndGet();
        final int id = (int) (cnt % recordIDMax);
        sendChannel.send(id, startTime, endTime, dataSize, events);
        if (this.msToRun > 0 && ((endTime - beginTime)  >= this.msToRun)) {
            ret.complete(null);
        } else if (this.totalRecords > cnt) {
            ret.complete(null);
        }
    }

    @Override
    public void consume(Object data) {
            callback.consume(data);
    }

    final private void consumeRead(Object data) {
        final long endTime = time.getCurrentTime();
        record(endTime, endTime, dataType.length(data), 1);
    }

    final private void consumeRW(Object data) {
        record(dataType.getTime(data), time.getCurrentTime(), dataType.length(data), 1);
    }
}

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

import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.QuadConsumer;
import io.sbk.api.ReaderCallback;

import java.util.concurrent.CompletableFuture;


public class SbkAsyncReader extends Worker implements ReaderCallback {
    final private DataType dataType;
    final private CompletableFuture<Void> ret;
    final private ReaderCallback callback;
    private long readCnt;
    private long beginTime;

    public SbkAsyncReader(int readerId, Parameters params, QuadConsumer recordTime, DataType dataType) {
        super(readerId, params, recordTime);
        this.dataType = dataType;
        this.ret = new CompletableFuture();
        this.readCnt = 0;
        this.beginTime = 0;
        if (params.isWriteAndRead()) {
            callback = this::consumeRW;
        } else {
            callback = this::consumeRead;
        }
    }

    public CompletableFuture<Void> start(long statTime) {
        this.beginTime = statTime;
        return ret;
    }

    @Override
    public void consume(Object data) {
            callback.consume(data);
    }

    final private void consumeRead(Object data) {
        final long endTime = System.currentTimeMillis();
        recordTime.accept(endTime, endTime, dataType.length(data), 1);
        readCnt += 1;
        if (params.getSecondsToRun() > 0 && (((endTime - beginTime) / 1000) >= params.getSecondsToRun())) {
            ret.complete(null);
        } else if (params.getRecordsPerReader() > readCnt) {
            ret.complete(null);
        }
    }

    final private void consumeRW(Object data) {
        final long endTime = System.currentTimeMillis();
        final long startTime = dataType.getTime(data);
        recordTime.accept(startTime, endTime, dataType.length(data), 1);
        readCnt += 1;
        if (params.getSecondsToRun() > 0 && (((endTime - beginTime) / 1000) >= params.getSecondsToRun())) {
            ret.complete(null);
        } else if (params.getRecordsPerReader() > readCnt) {
            ret.complete(null);
        }
    }

}

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
    private long readCnt;
    private long startTime;

    public SbkAsyncReader(int readerId, Parameters params, QuadConsumer recordTime, DataType dataType) {
        super(readerId, params, recordTime);
        this.dataType = dataType;
        this.ret = new CompletableFuture();
        this.readCnt = 0;
        this.startTime = 0;
    }

    public CompletableFuture<Void> start(long statTime) {
        this.startTime = statTime;
        return ret;
    }

    @Override
    public void consume(Object data) {
        final long time = System.currentTimeMillis();
        recordTime.accept(time, time, dataType.length(data), 1);
        readCnt += 1;
        if (params.getSecondsToRun() > 0 && (((time - startTime) / 1000) > params.getSecondsToRun())) {
            ret.complete(null);
        } else if (params.getRecordsPerReader() > readCnt) {
            ret.complete(null);
        }
    }
}

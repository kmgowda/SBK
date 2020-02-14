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
import io.sbk.api.Reader;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Reader Implementation.
 */
public class SbkReader extends Worker implements Callable<Void> {
    final private static int MS_PER_SEC = 1000;
    final private DataType data;
    final private Reader reader;
    final private RunBenchmark perf;

    public SbkReader(int readerId, Parameters params, QuadConsumer recordTime, DataType data, Reader reader) {
        super(readerId, params, recordTime);
        this.data = data;
        this.reader = reader;
        this.perf = createBenchmark();
    }

    @Override
    public Void call() throws InterruptedException, ExecutionException, IOException {
        try {
            perf.run();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return null;
    }

    final private RunBenchmark createBenchmark() {
        final RunBenchmark perfReader;
        if (params.getSecondsToRun() > 0) {
            perfReader = params.isWriteAndRead() ? this::RecordsTimeReaderRW : this::RecordsTimeReader;
        } else {
            perfReader = params.isWriteAndRead() ? this::RecordsReaderRW : this::RecordsReader;
        }
        return perfReader;
    }


    final public void RecordsReader() throws IOException {
        Object ret = null;
        try {
            int i = 0;
            while (i < params.getRecordsCount()) {
                final long startTime = System.currentTimeMillis();
                ret = reader.read();
                if (ret != null) {
                    final long endTime = System.currentTimeMillis();
                    recordTime.accept(startTime, endTime, data.length(ret), 1);
                    i++;
                }
            }
        } finally {
            reader.close();
        }
    }


    final public void RecordsReaderRW() throws IOException {
        Object ret = null;
        try {
            int i = 0;
            while (i < params.getRecordsCount()) {
                ret = reader.read();
                if (ret != null) {
                    final long endTime = System.currentTimeMillis();
                    final long start = data.getTime(ret);
                    recordTime.accept(start, endTime, data.length(ret), 1);
                    i++;
                }
            }
        } finally {
            reader.close();
        }
    }


    final public void RecordsTimeReader() throws IOException {
        final long startTime = params.getStartTime();
        final long msToRun = params.getSecondsToRun() * MS_PER_SEC;
        Object ret = null;
        long time = System.currentTimeMillis();
        try {
            while ((time - startTime) < msToRun) {
                time = System.currentTimeMillis();
                ret = reader.read();
                if (ret != null) {
                    final long endTime = System.currentTimeMillis();
                    recordTime.accept(time, endTime, data.length(ret), 1);
                }
            }
        } finally {
            reader.close();
        }
    }

    final public void RecordsTimeReaderRW() throws IOException {
        final long startTime = params.getStartTime();
        final long msToRun = params.getSecondsToRun() * MS_PER_SEC;
        Object ret;
        long time = System.currentTimeMillis();
        try {
            while ((time - startTime) < msToRun) {
                ret = reader.read();
                time = System.currentTimeMillis();
                if (ret != null) {
                    final long start = data.getTime(ret);
                    recordTime.accept(start, time, data.length(ret), 1);
                }
            }
        } finally {
            reader.close();
        }
    }
}

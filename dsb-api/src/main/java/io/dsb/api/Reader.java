/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.dsb.api;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * An Abstract class for Readers.
 */
public abstract class Reader extends Worker implements Callable<Void> {
    final private static int MS_PER_SEC = 1000;
    final private RunBenchmark perf;

    public Reader(int readerId, QuadConsumer recordTime, Parameters params) {
        super(readerId, recordTime, params);
        this.perf = createBenchmark();
    }

    /**
     * read the data.
     * @return byte[] return the data.
     * @throws IOException If an exception occurred.
     */
    public abstract byte[] read() throws IOException;

    /**
     * close the consumer/reader.
     * @throws IOException If an exception occurred.
     */
    public abstract void close() throws IOException;

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
        if (params.secondsToRun > 0) {
            perfReader = params.writeAndRead ? this::RecordsTimeReaderRW : this::RecordsTimeReader;
        } else {
            perfReader = params.writeAndRead ? this::RecordsReaderRW : this::RecordsReader;
        }
        return perfReader;
    }


    final public void RecordsReader() throws IOException {
        byte[] ret = null;
        try {
            int i = 0;
            while (i < params.records) {
                final long startTime = System.currentTimeMillis();
                ret = read();
                if (ret != null) {
                    final long endTime = System.currentTimeMillis();
                    recordTime.accept(startTime, endTime, ret.length, 1);
                    i++;
                }
            }
        } finally {
            close();
        }
    }


    final public void RecordsReaderRW() throws IOException {
        final ByteBuffer timeBuffer = ByteBuffer.allocate(TIME_HEADER_SIZE);
        byte[] ret = null;
        try {
            int i = 0;
            while (i < params.records) {
                ret = read();
                if (ret != null) {
                    final long endTime = System.currentTimeMillis();
                    timeBuffer.clear();
                    timeBuffer.put(ret, 0, TIME_HEADER_SIZE);
                    final long start = timeBuffer.getLong(0);
                    recordTime.accept(start, endTime, ret.length, 1);
                    i++;
                }
            }
        } finally {
            close();
        }
    }


    final public void RecordsTimeReader() throws IOException {
        final long msToRun = params.secondsToRun * MS_PER_SEC;
        byte[] ret = null;
        long time = System.currentTimeMillis();
        try {
            while ((time - params.startTime) < msToRun) {
                time = System.currentTimeMillis();
                ret = read();
                if (ret != null) {
                    final long endTime = System.currentTimeMillis();
                    recordTime.accept(time, endTime, ret.length, 1);
                }
            }
        } finally {
            close();
        }
    }

    final public void RecordsTimeReaderRW() throws IOException {
        final long msToRun = params.secondsToRun * MS_PER_SEC;
        final ByteBuffer timeBuffer = ByteBuffer.allocate(TIME_HEADER_SIZE);
        byte[] ret = null;
        long time = System.currentTimeMillis();
        try {
            while ((time - params.startTime) < msToRun) {
                ret = read();
                time = System.currentTimeMillis();
                if (ret != null) {
                    timeBuffer.clear();
                    timeBuffer.put(ret, 0, TIME_HEADER_SIZE);
                    final long start = timeBuffer.getLong(0);
                    recordTime.accept(start, time, ret.length, 1);
                }
            }
        } finally {
            close();
        }
    }
}

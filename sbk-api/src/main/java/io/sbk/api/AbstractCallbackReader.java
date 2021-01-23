/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract class for Callback Reader.
 */
public abstract class AbstractCallbackReader<T> implements DataReader<T>, Callback<T> {
    private DataType<T> dataType;
    private Time time;
    private CompletableFuture<Void> ret;
    private Callback<T> callback;
    private AtomicLong readCnt;
    private long beginTime;
    private Worker reader;
    private double msToRun;
    private long totalRecords;

    /**
     * set the Callback and start the CallBack Reader.
     * @param callback Reader callback.
     * @throws IOException If an exception occurred.
     */
    abstract void start(Callback callback) throws IOException;


    /**
     * Close the CallBack Reader.
     * while overriding this method, make sure that super.close() is called.
     *
     * @throws IOException If an exception occurred.
     */
    @Override
    public void close() throws IOException {
        complete();
    }


    @Override
    public void record(long startTime, long endTime, int dataSize, int events) {
        final long cnt = readCnt.incrementAndGet();
        final int id = (int) (cnt % reader.recordIDMax);
        reader.sendChannel.send(id, startTime, endTime, dataSize, events);
        if (this.msToRun > 0 && ((endTime - beginTime)  >= this.msToRun)) {
            complete();
        } else if (this.totalRecords > cnt) {
            complete();
        }
    }

    @Override
    public void consume(T data) {
        callback.consume(data);
    }

    private void consumeRead(T data) {
        final long endTime = time.getCurrentTime();
        record(endTime, endTime, dataType.length(data), 1);
    }

    private void consumeRW(T data) {
        record(dataType.getTime(data), time.getCurrentTime(), dataType.length(data), 1);
    }

    /**
     * Default Implementation to initialize the callback reader.
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws IOException If an exception occurred.
     */
    public void initialize(Worker reader, DataType<T> dType, Time time) throws IOException {
        this.reader = reader;
        this.dataType = dType;
        this.time = time;
        this.readCnt = new AtomicLong(0);
        this.beginTime = time.getCurrentTime();
        this.msToRun = reader.params.getSecondsToRun() * Config.MS_PER_SEC;
        this.totalRecords = reader.params.getRecordsPerReader() * reader.params.getReadersCount();
        this.ret = new CompletableFuture<>();
        if (reader.params.isWriteAndRead()) {
            callback = this::consumeRW;
        } else {
            callback = this::consumeRead;
        }
        start(callback);
    }

    /**
     * Default Implementation complete the read.
     */
    public void complete()  {
        if (ret != null) {
            ret.complete(null);
        }
    }


    /**
     * Default Implementation to wait for the readers to complete.
     *
     * @throws IOException If an exception occurred.
     */
    public void waitToComplete() throws IOException {
        try {
            if (ret != null) {
                ret.get();
            }
        } catch (ExecutionException | InterruptedException ex) {
            throw  new IOException(ex);
        }
    }


    /**
     * Default Implementation run the Benchmark.
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws IOException If an exception occurred.
     */
    public void run(Worker reader, DataType<T> dType, Time time) throws IOException {
        initialize(reader, dType, time);
        waitToComplete();
    }


    /**
     * Implementation for benchmarking reader by reading given number of records.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    public void RecordsReader(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException {
        run(reader, dType, time);
    }

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     *
     * @param reader      Reader Descriptor
     * @param dType     dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    public void RecordsReaderRW(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException {
        run(reader, dType, time);
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    public void RecordsTimeReader(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException {
        run(reader, dType, time);
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    public void RecordsTimeReaderRW(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException {
        run(reader, dType, time);
    }

}

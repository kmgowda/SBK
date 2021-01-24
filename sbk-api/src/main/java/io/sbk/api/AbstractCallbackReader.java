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
public abstract class AbstractCallbackReader<T> implements DataReader<T> {
    private DataType<T> dataType;
    private Time time;
    private CompletableFuture<Void> ret;
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
    public abstract void start(Callback<T> callback) throws IOException;

    /**
     * Stop the CallBack Reader.
     * @throws IOException If an exception occurred.
     */
    public abstract void stop() throws IOException;


    /**
     * Close the CallBack Reader.
     * stops the callback reader.
     *
     * @throws IOException If an exception occurred.
     */
    @Override
    public final void close() throws IOException {
        stop();
        complete();
    }

    public void recordBenchmark(long startTime, long endTime, int dataSize, int events) {
        final long cnt = readCnt.incrementAndGet();
        final int id = (int) (cnt % reader.recordIDMax);
        reader.sendChannel.send(id, startTime, endTime, dataSize, events);
        if (this.msToRun > 0 && ((endTime - beginTime)  >= this.msToRun)) {
            complete();
        } else if (this.totalRecords > cnt) {
            complete();
        }
    }


    private class ConsumeRead implements Callback<T> {

        public void consume(final T data) {
            final long endTime = time.getCurrentTime();
            recordBenchmark(endTime, endTime, dataType.length(data), 1);
        }

        public void record(long startTime, long endTime, int dataSize, int records) {
            recordBenchmark(startTime, endTime, dataSize, records);
        }

    }


    private class ConsumeRW implements Callback<T> {

        public void consume(final T data) {
            recordBenchmark(dataType.getTime(data), time.getCurrentTime(), dataType.length(data), 1);
        }

        public void record(long startTime, long endTime, int dataSize, int records) {
            recordBenchmark(startTime, endTime, dataSize, records);
        }

    }

    /**
     * Default Implementation to initialize the callback reader.
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @param callback  Callback interface
     * @throws IOException If an exception occurred.
     */
    public void initialize(Worker reader, DataType<T> dType, Time time, Callback<T> callback) throws IOException {
        this.reader = reader;
        this.dataType = dType;
        this.time = time;
        this.readCnt = new AtomicLong(0);
        this.beginTime = time.getCurrentTime();
        this.msToRun = reader.params.getSecondsToRun() * Config.MS_PER_SEC;
        this.totalRecords = reader.params.getRecordsPerReader() * reader.params.getReadersCount();
        this.ret = new CompletableFuture<>();
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
     * @param callback  Callback interface
     * @throws IOException If an exception occurred.
     */
    public void run(Worker reader, DataType<T> dType, Time time, Callback<T> callback) throws IOException {
        initialize(reader, dType, time, callback);
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
        run(reader, dType, time, new ConsumeRead());
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
        run(reader, dType, time, new ConsumeRW());
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
        run(reader, dType, time, new ConsumeRead());
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
        run(reader, dType, time, new ConsumeRW());
    }

}

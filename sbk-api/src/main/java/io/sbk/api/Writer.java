/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Writers.
 */
public interface Writer<T>  {

    /**
     * Asynchronously Writes the data .
     * @param data data to write
     * @return CompletableFuture completable future. null if the write completed synchronously .
     * @throws IOException If an exception occurred.
     */
    CompletableFuture<?> writeAsync(T data) throws IOException;

    /**
     * Flush / Sync the  data.
     * @throws IOException If an exception occurred.
     */
    void sync() throws IOException;

    /**
     * Close the  Writer.
     * @throws IOException If an exception occurred.
     */
    void close() throws IOException;

    /**
     * Default implementation for writing data using {@link io.sbk.api.Writer#writeAsync(Object)}  )}
     * and recording the benchmark statistics.
     * If you are intend to NOT use the CompletableFuture returned by {@link io.sbk.api.Writer#writeAsync(Object)}  )}
     * then you can override this method. otherwise, use the default implementation and don't override this method.
     * If you are intend to use your own payload, then also you can use override this method.
     *
     * @param data   data to write
     * @param size  size of the data
     * @param recordTime to call for benchmarking
     * @param  id   Identifier for recordTime
     * @return time return the data sent time
     * @throws IOException If an exception occurred.
     */
    default long recordWrite(T data, int size, RecordTime recordTime, int id) throws IOException {
        CompletableFuture<?> ret;
        final long time = System.currentTimeMillis();
        ret = writeAsync(data);
        if (ret == null) {
            final long endTime = System.currentTimeMillis();
            recordTime.accept(id, time, endTime, size, 1);
        } else {
            ret.thenAccept(d -> {
                final long endTime = System.currentTimeMillis();
                recordTime.accept(id, time, endTime, size, 1);
            });
        }
        return time;
    }

    /**
     * Default implementation for writing data using {@link io.sbk.api.Writer#writeAsync(Object)})} with time
     * If you are intend to NOT use the CompletableFuture returned by {@link io.sbk.api.Writer#writeAsync(Object)}  )}
     * then you can override this method. otherwise, use the default implementation and don't override this method.
     * If you are intend to use your own payload, then also you can use override this method.
     *
     * @param dType   Data Type interface
     * @param data  data to writer
     * @return time return the data sent time
     * @throws IOException If an exception occurred.
     */
    default  long writeAsyncTime(DataType<T> dType, T data) throws IOException {
        final long time = System.currentTimeMillis();
        writeAsync(dType.setTime(data, time));
        return time;
    }


    /**
     * Default implementation for writer benchmarking by writing given number of records.
     * sync is invoked after writing all the records.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriter(Worker writer, DataType<T> dType, T data) throws IOException {
        final int size = dType.length(data);
        for (int i = 0; i < writer.params.getRecordsCount(); i++) {
            recordWrite(data, size, writer.recordTime, i % writer.recordIDMax);
        }
        sync();
    }

    /**
     * Default implementation for writer benchmarking by writing given number of records.
     * sync is invoked after writing given set of records.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param rController Rate Controller
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterSync(Worker writer, DataType<T> dType, T data, RateController rController) throws IOException {
        final int recordsCount = writer.params.getRecordsPerWriter();
        final int size = dType.length(data);
        int cnt = 0;
        rController.start(writer.params.getRecordsPerSec(), System.currentTimeMillis());
        while (cnt < recordsCount) {
            int loopMax = Math.min(writer.params.getRecordsPerSync(), recordsCount - cnt);
            for (int i = 0; i < loopMax; i++) {
                rController.control(cnt++, recordWrite(data, size, writer.recordTime, i % writer.recordIDMax));
            }
            sync();
        }
    }

    /**
     * Default implementation for writer benchmarking by continuously writing data records for specific time duration.
     * sync is invoked after writing records for given time.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterTime(Worker writer, DataType<T> dType, T data) throws IOException {
        final long startTime = writer.params.getStartTime();
        final long msToRun = writer.params.getSecondsToRun() * Config.MS_PER_SEC;
        final int size = dType.length(data);
        long time = System.currentTimeMillis();
        int id = writer.id % writer.recordIDMax;
        while ((time - startTime) < msToRun) {
            time = recordWrite(data, size, writer.recordTime, id);
            id += 1;
            if (id >= writer.recordIDMax) {
                id = 0;
            }
        }
        sync();
    }

    /**
     * Default implementation for writer benchmarking by continuously writing data records for specific time duration.
     * sync is invoked after writing given set of records.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param rController Rate Controller
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterTimeSync(Worker writer, DataType<T> dType, T data, RateController rController) throws IOException {
        final long startTime = writer.params.getStartTime();
        final long msToRun = writer.params.getSecondsToRun() * Config.MS_PER_SEC;
        final int size = dType.length(data);
        long time = System.currentTimeMillis();
        long msElapsed = time - startTime;
        int cnt = 0;
        rController.start(writer.params.getRecordsPerSec(), time);
        while (msElapsed < msToRun) {
            for (int i = 0; (msElapsed < msToRun) && (i < writer.params.getRecordsPerSync()); i++) {
                time = recordWrite(data, size, writer.recordTime, i % writer.recordIDMax);
                rController.control(cnt++, time);
                msElapsed = time - startTime;
            }
            sync();
        }
    }

    /**
     * Default implementation for writing given number of records. No Writer Benchmarking is performed.
     * Write is performed using {@link io.sbk.api.Writer#writeAsync(Object)}  )}.
     * sync is invoked after writing given set of records.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param rController Rate Controller
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterRW(Worker writer, DataType<T> dType, T data, RateController rController) throws IOException {
        final int recordsCount = writer.params.getRecordsPerWriter();
        int cnt = 0;
        long time;
        rController.start(writer.params.getRecordsPerSec(), System.currentTimeMillis());
        while (cnt < recordsCount) {
            int loopMax = Math.min(writer.params.getRecordsPerSync(), recordsCount - cnt);
            for (int i = 0; i < loopMax; i++) {
                time = writeAsyncTime(dType, data);
                rController.control(cnt++, time);
            }
            sync();
        }
    }

    /**
     * Default implementation for writing data records for specific time duration. No Writer Benchmarking is performed.
     * Write is performed using {@link io.sbk.api.Writer#writeAsync(Object)}  )}.
     * sync is invoked after writing given set of records.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param rController Rate Controller
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterTimeRW(Worker writer, DataType<T> dType, T data, RateController rController) throws IOException {
        final long startTime = writer.params.getStartTime();
        final long msToRun = writer.params.getSecondsToRun() * Config.MS_PER_SEC;
        long time = System.currentTimeMillis();
        long msElapsed = time - startTime;
        int cnt = 0;
        rController.start(writer.params.getRecordsPerSec(), time);
        while (msElapsed < msToRun) {
            for (int i = 0; (msElapsed < msToRun) && (i < writer.params.getRecordsPerSync()); i++) {
                time = writeAsyncTime(dType, data);
                rController.control(cnt++, time);
                msElapsed = time - startTime;
            }
            sync();
        }
    }
}

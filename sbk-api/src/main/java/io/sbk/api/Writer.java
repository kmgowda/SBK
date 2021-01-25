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
public interface Writer<T>  extends DataRecordsWriter<T> {

    /**
     * Asynchronously Writes the data .
     * @param data data to write
     * @return CompletableFuture completable future. null if the write completed synchronously .
     * @throws IOException If an exception occurred.
     */
    CompletableFuture<?> writeAsync(T data) throws IOException;

    /**
     * Close the  Writer.
     * @throws IOException If an exception occurred.
     */
    void close() throws IOException;

    /**
     * Flush / Sync the  data.
     * @throws IOException If an exception occurred.
     */
    default void sync() throws IOException {

    }

    /**
     * Default implementation for writing data using {@link io.sbk.api.Writer#writeAsync(Object)})} with time
     * If you are intend to NOT use the CompletableFuture returned by {@link io.sbk.api.Writer#writeAsync(Object)}  )}
     * then you can override this method. otherwise, use the default implementation and don't override this method.
     * If you are intend to use your own payload, then also you can use override this method.
     * you can write multiple records with this method.
     *
     * @param dType   Data Type interface
     * @param data  data to writer
     * @param size  size of the data
     * @param time  time interface
     * @param status  write status to return
     * @throws IOException If an exception occurred.
     */
    default void writeSetTime(DataType<T> dType, T data, int size, Time time, Status status) throws IOException {
        status.bytes = size;
        status.records = 1;
        status.startTime = time.getCurrentTime();
        writeAsync(dType.setTime(data, status.startTime));
    }


    /**
     * Default implementation for writing data using {@link io.sbk.api.Writer#writeAsync(Object)}  )}
     * and recording the benchmark statistics.
     * If you are intend to NOT use the CompletableFuture returned by {@link io.sbk.api.Writer#writeAsync(Object)}  )}
     * then you can override this method. otherwise, use the default implementation and don't override this method.
     * If you are intend to use your own payload, then also you can use override this method.
     * you can write multiple records with this method.
     *
     * @param dType   Data Type interface
     * @param data   data to write
     * @param size  size of the data
     * @param time  time interface
     * @param status Write status to return
     * @param sendChannel to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws IOException If an exception occurred.
     */
    default void recordWrite(DataType<T> dType, T data, int size, Time time,
                             Status status, SendChannel sendChannel, int id) throws IOException {
        CompletableFuture<?> ret;
        status.bytes = size;
        status.records =  1;
        status.startTime = time.getCurrentTime();
        ret = writeAsync(data);
        if (ret == null) {
            status.endTime = time.getCurrentTime();
            sendChannel.send(id, status.startTime, status.endTime, size, status.records);
        } else {
            final long beginTime =  status.startTime;
            ret.thenAccept(d -> {
                final long endTime = time.getCurrentTime();
                sendChannel.send(id, beginTime, endTime, size, status.records);
            });
        }
    }
}

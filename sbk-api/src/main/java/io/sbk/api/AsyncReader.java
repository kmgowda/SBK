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

/**
 * Interface for Asynchronous Readers.
 */
public interface AsyncReader<T> extends DataRecordsReader<T> {

    /**
     * Read the Asynchronously data.
     * @return Completable Future.
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    CompletableFuture<T> readAsync() throws EOFException, IOException;

    /**
     * Close the  Reader.
     * @throws IOException If an exception occurred.
     */
    default void close() throws IOException {

    }

    /**
     * Default implementation for Reading data using {@link AsyncReader#readAsync()}
     * and recording the benchmark statistics.
     * The end time of the status parameter {@link Status#endTime} of this method determines
     * the terminating condition for time based reader performance benchmarking.
     * If you are intend to not use {@link AsyncReader#readAsync()} then you can override this method.
     * If you are intend to read multiple records then you can override this method.
     * otherwise, use the default implementation and don't override this method.
     *
     * @param dType      dataType
     * @param time  time interface
     * @param status     Timestamp
     * @param sendChannel to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void recordRead(DataType<T> dType, Time time, Status status, SendChannel sendChannel, int id)
            throws EOFException, IOException {
        status.startTime = time.getCurrentTime();
        status.records = 1;
        status.bytes = 0;
        status.endTime = status.startTime;
        final CompletableFuture<T> ret = readAsync();
        if (ret == null) {
            throw new IOException();
        } else {
            final long beginTime = status.startTime;
            ret.thenAccept(d -> {
                final long endTime = time.getCurrentTime();
                sendChannel.send(id, beginTime, endTime, dType.length(d), status.records);
            });
        }
    }

    /**
     * Default implementation for Reading data using {@link Reader#read()}, extracting start time from data
     * and recording the benchmark statistics.
     * The end time of the status parameter {@link Status#endTime} of this method determines
     * the terminating condition for time based reader performance benchmarking.
     * If you are intend to not use {@link Reader#read()} then you can override this method.
     * If you are intend to read multiple records then you can override this method.
     * otherwise, use the default implementation and don't override this method.
     *
     * @param dType      dataType
     * @param time  time interface
     * @param status     Timestamp
     * @param sendChannel to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void recordReadTime(DataType<T> dType, Time time, Status status, SendChannel sendChannel, int id)
            throws EOFException, IOException {
        status.startTime = time.getCurrentTime();
        status.records = 1;
        status.bytes = 0;
        status.endTime = status.startTime;
        final CompletableFuture<T> ret = readAsync();
        if (ret == null) {
            throw new IOException();
        } else {
            ret.thenAccept(d -> {
                final long endTime = time.getCurrentTime();
                    sendChannel.send(id, dType.getTime(d), endTime, dType.length(d), status.records);
            });
        }
    }
}


/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import io.sbk.data.DataType;
import io.perl.PerlChannel;
import io.time.Time;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Asynchronous Readers.
 */
public non-sealed interface AsyncReader<T> extends DataRecordsReader<T> {

    /**
     * Read the dat asynchronously.
     *
     * @param size size of the data in bytes to read.
     * @return Completable Future.
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    CompletableFuture<T> readAsync(int size) throws EOFException, IOException;

    /**
     * Close the  Reader.
     *
     * @throws IOException If an exception occurred.
     */
    default void close() throws IOException {

    }

    /**
     * Default implementation for Reading data using {@link AsyncReader#readAsync(int)} ()}
     * and recording the benchmark statistics.
     * The end time of the status parameter {@link Status#endTime} of this method determines
     * the terminating condition for time based reader performance benchmarking.
     * If you are intend to not use {@link AsyncReader#readAsync(int)} ()} then you can override this method.
     * If you are intend to read multiple records then you can override this method.
     * otherwise, use the default implementation and don't override this method.
     *
     * @param dType       dataType
     * @param size        size of the data in bytes
     * @param time        time interface
     * @param status      Timestamp
     * @param perlChannel to call for benchmarking
     * @param id          Identifier for recordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void recordRead(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel, int id)
            throws EOFException, IOException {
        status.startTime = time.getCurrentTime();
        status.records = 1;
        status.bytes = size;
        status.endTime = status.startTime;
        final CompletableFuture<T> ret = readAsync(size);
        if (ret == null) {
            throw new IOException();
        } else {
            final long beginTime = status.startTime;
            ret.exceptionally(ex -> {
                perlChannel.sendException(ex);
                return null;
            });
            ret.thenAccept(d -> {
                final long endTime = time.getCurrentTime();
                perlChannel.send(beginTime, endTime, dType.length(d), status.records);
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
     * @param dType       dataType
     * @param size        size of the data in bytes
     * @param time        time interface
     * @param status      Timestamp
     * @param perlChannel to call for benchmarking
     * @param id          Identifier for recordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void recordReadTime(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel, int id)
            throws EOFException, IOException {
        status.startTime = time.getCurrentTime();
        status.records = 1;
        status.bytes = size;
        status.endTime = status.startTime;
        final CompletableFuture<T> ret = readAsync(size);
        if (ret == null) {
            throw new IOException();
        } else {
            ret.thenAccept(d -> {
                final long endTime = time.getCurrentTime();
                perlChannel.send(dType.getTime(d), endTime, dType.length(d), status.records);
            });
        }
    }
}


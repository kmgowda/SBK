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

/**
 * Interface for Readers.
 */
public interface Reader<T> extends DataRecordsReader<T> {

    /**
     * Read the data.
     * @return T return the data.
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    T read() throws EOFException, IOException;

    /**
     * Close the  Reader.
     * @throws IOException If an exception occurred.
     */
    default void close() throws IOException {

    }

    /**
     * Default implementation for Reading data using {@link Reader#read()}
     * and recording the benchmark statistics.
     * The end time of the status parameter {@link Status#endTime} of this method determines
     * the terminating condition for time based reader performance benchmarking.
     * If you are intend to not use {@link Reader#read()} then you can override this method.
     * If you are intend to read multiple records then you can override this method.
     * otherwise, use the default implementation and don't override this method.
     *
     * @param dType      dataType
     * @param size  size of the data in bytes
     * @param time  time interface
     * @param status     Timestamp
     * @param sendChannel to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void recordRead(DataType<T> dType, int size, Time time, Status status, SendChannel sendChannel, int id)
            throws EOFException, IOException {
        status.startTime = time.getCurrentTime();
        final T ret = read();
        if (ret == null) {
            status.records = 0;
            status.endTime = status.startTime;
        } else {
            status.endTime = time.getCurrentTime();
            status.bytes = dType.length(ret);
            status.records = 1;
            sendChannel.send(id, status.startTime, status.endTime, status.bytes, status.records);
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
     * @param size  size of the data in bytes
     * @param time  time interface
     * @param status     Timestamp
     * @param sendChannel to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void recordReadTime(DataType<T> dType, int size, Time time, Status status, SendChannel sendChannel, int id)
            throws EOFException, IOException {
        final T ret = read();
        if (ret == null) {
            status.endTime = time.getCurrentTime();
            status.records = 0;
        } else {
            status.startTime = dType.getTime(ret);
            status.endTime = time.getCurrentTime();
            status.bytes = dType.length(ret);
            status.records = 1;
            sendChannel.send(id, status.startTime, status.endTime, status.bytes, status.records);
        }
    }

}


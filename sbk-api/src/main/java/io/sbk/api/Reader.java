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

import java.io.IOException;

/**
 * Interface for Readers.
 */
public interface Reader<T> {
    /**
     * read the data.
     * @return T return the data.
     * @throws IOException If an exception occurred.
     */
    T read() throws IOException;

    /**
     * close the consumer/reader.
     * @throws IOException If an exception occurred.
     */
    void close() throws IOException;

    /**
     * Default implementation for Reading data using {@link Reader#read()}
     * and recording the benchmark statistics.
     * If you are intend to read multiple records then you can override this method.
     * otherwise, use the default implementation and don't override this method.
     *
     * @param dType      dataType
     * @param status     Timestamp
     * @param recordTime to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws IOException If an exception occurred.
     */
    default void recordRead(DataType<T> dType, TimeStamp status, RecordTime recordTime, int id) throws IOException {
        status.startTime = System.currentTimeMillis();
        final T ret = read();
        if (ret == null) {
            status.records = 0;
            status.endTime = status.startTime;
        } else {
            status.endTime = System.currentTimeMillis();
            status.bytes = dType.length(ret);
            status.records = 1;
            recordTime.accept(id, status.startTime, status.endTime, status.bytes, status.records);
        }
    }

    /**
     * Default implementation for Reading data using {@link Reader#read()}, extracting start time from data
     * and recording the benchmark statistics.
     * If you are intend to read multiple records then you can override this method.
     * otherwise, use the default implementation and don't override this method.
     *
     * @param dType      dataType
     * @param status     Timestamp
     * @param recordTime to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws IOException If an exception occurred.
     */
    default void recordReadTime(DataType<T> dType, TimeStamp status, RecordTime recordTime, int id) throws IOException {
        final T ret = read();
        if (ret == null) {
            status.endTime = System.currentTimeMillis();
            status.records = 0;
        } else {
            status.startTime = dType.getTime(ret);
            status.endTime = System.currentTimeMillis();
            status.bytes = dType.length(ret);
            status.records = 1;
            recordTime.accept(id, status.startTime, status.endTime, status.bytes, status.records);
        }
    }
}

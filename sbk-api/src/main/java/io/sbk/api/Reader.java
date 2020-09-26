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
public interface Reader<T> {
    /**
     * read the data.
     * @return T return the data.
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    T read() throws EOFException, IOException;

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
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void recordRead(DataType<T> dType, Status status, RecordTime recordTime, int id)
            throws EOFException, IOException {
        status.startTime = System.currentTimeMillis();
        final T ret = read();
        if (ret == null) {
            status.records = 0;
            status.endTime = status.startTime;
        } else {
            status.endTime = System.currentTimeMillis();
            status.bytes = dType.length(ret);
            status.records = 1;
            recordTime.send(id, status.startTime, status.endTime, status.bytes, status.records);
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
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void recordReadTime(DataType<T> dType, Status status, RecordTime recordTime, int id)
            throws EOFException, IOException {
        final T ret = read();
        if (ret == null) {
            status.endTime = System.currentTimeMillis();
            status.records = 0;
        } else {
            status.startTime = dType.getTime(ret);
            status.endTime = System.currentTimeMillis();
            status.bytes = dType.length(ret);
            status.records = 1;
            recordTime.send(id, status.startTime, status.endTime, status.bytes, status.records);
        }
    }

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     * This method uses the method {@link Reader#recordRead(DataType, Status, RecordTime, int)}
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void RecordsReader(Worker reader, DataType<T> dType) throws EOFException, IOException {
        final Status status = new Status();
        int i = 0, id = reader.id % reader.recordIDMax;
        while (i < reader.params.getRecordsPerReader()) {
            recordRead(dType, status, reader.recordTime, id++);
            i += status.records;
            if (id >= reader.recordIDMax) {
                id = 0;
            }
        }
    }

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     * This method uses the method {@link Reader#recordReadTime(DataType, Status, RecordTime, int)}}
     *
     * @param reader      Reader Descriptor
     * @param dType     dataType
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void RecordsReaderRW(Worker reader, DataType<T> dType) throws EOFException, IOException {
        final Status status = new Status();
        int i = 0, id = reader.id % reader.recordIDMax;
        while (i < reader.params.getRecordsPerReader()) {
            recordReadTime(dType, status, reader.recordTime, id++);
            i += status.records;
            if (id >= reader.recordIDMax) {
                id = 0;
            }
        }
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     * This method uses the method {@link Reader#recordRead(DataType, Status, RecordTime, int)}
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void RecordsTimeReader(Worker reader, DataType<T> dType) throws EOFException, IOException {
        final Status status = new Status();
        final long startTime = reader.params.getStartTime();
        final long msToRun = reader.params.getSecondsToRun() * Config.MS_PER_SEC;
        int id = reader.id % reader.recordIDMax;
        while ((status.endTime - startTime) < msToRun) {
            recordRead(dType, status, reader.recordTime, id++);
            if (id >= reader.recordIDMax) {
                id = 0;
            }
        }
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     * This method uses the method {@link Reader#recordReadTime(DataType, Status, RecordTime, int)}}
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void RecordsTimeReaderRW(Worker reader, DataType<T> dType) throws EOFException, IOException {
        final Status status = new Status();
        final long startTime = reader.params.getStartTime();
        final long msToRun = reader.params.getSecondsToRun() * Config.MS_PER_SEC;
        int id = reader.id % reader.recordIDMax;
        while ((status.endTime - startTime) < msToRun) {
            recordReadTime(dType, status, reader.recordTime, id++);
            if (id >= reader.recordIDMax) {
                id = 0;
            }
        }
    }
}


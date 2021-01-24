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
 * Interface for Data Record Readers.
 */
public interface DataRecordsReader<T> extends DataReader<T> {



    /**
     * Record the single or multiple reads performance statistics.
     *
     * @param dType      dataType
     * @param size      size of data in bytes
     * @param time  time interface
     * @param status     read status to return; {@link io.sbk.api.Status}
     * @param sendChannel to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void recordRead(DataType<T> dType, int size, Time time, Status status, SendChannel sendChannel, int id)
            throws EOFException, IOException;

    /**
     * Record the single or multiple reads performance statistics along with the starting time in the data.
     *
     * @param dType      dataType
     * @param size      size of data in bytes 
     * @param time  time interface
     * @param status     read status to return; {@link io.sbk.api.Status}
     * @param sendChannel to call for benchmarking
     * @param  id   Identifier for recordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void recordReadTime(DataType<T> dType, int size, Time time, Status status, SendChannel sendChannel, int id)
            throws EOFException, IOException;

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     * This method uses the method {@link DataRecordsReader#recordRead(DataType, int, Time, Status, SendChannel, int)}
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void RecordsReader(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException {
        final Status status = new Status();
        final int size = reader.params.getRecordSize();
        int  id = reader.id % reader.recordIDMax;
        long i = 0;
        while (i < reader.params.getRecordsPerReader()) {
            recordRead(dType, size, time, status, reader.sendChannel, id++);
            i += status.records;
            if (id >= reader.recordIDMax) {
                id = 0;
            }
        }
    }

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     * This method uses the method {@link DataRecordsReader#recordReadTime(DataType, int, Time, Status, SendChannel, int)}
     *
     * @param reader      Reader Descriptor
     * @param dType     dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void RecordsReaderRW(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException {
        final Status status = new Status();
        final int size = reader.params.getRecordSize();
        int id = reader.id % reader.recordIDMax;
        long i = 0;
        while (i < reader.params.getRecordsPerReader()) {
            recordReadTime(dType, size, time, status, reader.sendChannel, id++);
            i += status.records;
            if (id >= reader.recordIDMax) {
                id = 0;
            }
        }
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     * This method uses the method {@link DataRecordsReader#recordRead(DataType, int, Time, Status, SendChannel, int)}
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void RecordsTimeReader(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException {
        final long startTime = time.getCurrentTime();
        final int size = reader.params.getRecordSize();
        final Status status = new Status();
        final long msToRun = reader.params.getSecondsToRun() * Config.MS_PER_SEC;
        int id = reader.id % reader.recordIDMax;
        while (time.elapsedMilliSeconds(status.endTime, startTime) < msToRun) {
            recordRead(dType, size, time, status, reader.sendChannel, id++);
            if (id >= reader.recordIDMax) {
                id = 0;
            }
        }
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     * This method uses the method {@link DataRecordsReader#recordReadTime(DataType, int, Time, Status, SendChannel, int)}
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    default void RecordsTimeReaderRW(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException {
        final long startTime = time.getCurrentTime();
        final int size = reader.params.getRecordSize();
        final Status status = new Status();
        final long msToRun = reader.params.getSecondsToRun() * Config.MS_PER_SEC;
        int id = reader.id % reader.recordIDMax;
        while (time.elapsedMilliSeconds(status.endTime, startTime) < msToRun) {
            recordReadTime(dType, size, time, status, reader.sendChannel, id++);
            if (id >= reader.recordIDMax) {
                id = 0;
            }
        }
    }
}

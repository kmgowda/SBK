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

/**
 * Interface for Data Record Readers.
 */
public sealed interface DataRecordsReader<T> extends DataReader<T> permits AsyncReader, Reader {

    /**
     * Record the single or multiple reads performance statistics.
     *
     * @param dType       dataType
     * @param size        size of data in bytes
     * @param time        time interface
     * @param status      read status to return; {@link io.sbk.api.Status}
     * @param perlChannel to call for benchmarking
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    void recordRead(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel)
            throws EOFException, IOException;

    /**
     * Record the single or multiple reads performance statistics along with the starting time in the data.
     *
     * @param dType       dataType
     * @param size        size of data in bytes
     * @param time        time interface
     * @param status      read status to return; {@link io.sbk.api.Status}
     * @param perlChannel to call for benchmarking
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    void recordReadTime(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel)
            throws EOFException, IOException;

    default void genericRecordsReader(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                      RecordTime<T> recordTime) throws EOFException, IOException {
        final Status status = new Status();
        final int size = reader.params.getRecordSize();
        long i = 0;
        while (i < recordsCount) {
            recordTime.recordRead(dType, size, time, status, reader.perlChannel);
            i += status.records;
        }
    }

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     * This method uses the method {@link DataRecordsReader#recordRead(DataType, int, Time, Status, PerlChannel)}
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsReader(Worker reader, long recordsCount, DataType<T> dType, Time time) throws EOFException,
            IOException {
        genericRecordsReader(reader, recordsCount, dType, time, this::recordRead);
    }

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     * This method uses the method {@link DataRecordsReader#recordReadTime(DataType, int, Time, Status, PerlChannel, int)}
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsReaderRW(Worker reader, long recordsCount, DataType<T> dType, Time time) throws EOFException,
            IOException {
        genericRecordsReader(reader, recordsCount, dType, time, this::recordReadTime);
    }

    default void genericRecordsTimeReader(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                          RecordTime<T> recordTime) throws EOFException,
            IOException {
        final long startTime = time.getCurrentTime();
        final int size = reader.params.getRecordSize();
        final Status status = new Status();
        final long msToRun = secondsToRun * Time.MS_PER_SEC;
        while (time.elapsedMilliSeconds(status.endTime, startTime) < msToRun) {
            recordTime.recordRead(dType, size, time, status, reader.perlChannel);
        }
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     * This method uses the method {@link DataRecordsReader#recordRead(DataType, int, Time, Status, PerlChannel)}
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsTimeReader(Worker reader, long secondsToRun, DataType<T> dType, Time time) throws EOFException,
            IOException {
        genericRecordsTimeReader(reader, secondsToRun, dType, time, this::recordRead);

    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     * This method uses the method {@link DataRecordsReader#recordReadTime(DataType, int, Time, Status, PerlChannel, int)}
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsTimeReaderRW(Worker reader, long secondsToRun, DataType<T> dType, Time time) throws EOFException, IOException {
        genericRecordsTimeReader(reader, secondsToRun, dType, time, this::recordReadTime);
    }

    default void genericRecordsReaderRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                                 RateController rController, RecordTime<T> recordTime) throws EOFException, IOException {
        final Status status = new Status();
        final int size = reader.params.getRecordSize();
        long i = 0;
        double secondsElapsed = 0;
        final long loopStartTime = time.getCurrentTime();
        rController.start(reader.params.getRecordsPerSec());
        while (i < recordsCount) {
            recordTime.recordRead(dType, size, time, status, reader.perlChannel);
            i += status.records;
            secondsElapsed = time.elapsedSeconds(status.endTime, loopStartTime);
            rController.control(i, secondsElapsed);
        }
    }

    /**
     * Benchmarking reader by reading given number of records with Rate controlled.
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsReaderRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                          RateController rController) throws EOFException,
            IOException {
        genericRecordsReaderRateControl(reader, recordsCount, dType, time, rController, this::recordRead);
    }

    /**
     * Benchmarking reader by reading given number of records with Rate controlled.
     * used while another writer is writing the data.
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsReaderRWRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                            RateController rController) throws EOFException, IOException {
        genericRecordsReaderRateControl(reader, recordsCount, dType, time, rController, this::recordReadTime);
    }

    default void genericRecordsTimeReaderRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                                     RateController rController, RecordTime<T> recordTime) throws EOFException, IOException {
        final long startTime = time.getCurrentTime();
        final int size = reader.params.getRecordSize();
        final Status status = new Status();
        final long msToRun = secondsToRun * Time.MS_PER_SEC;
        final long loopStartTime = time.getCurrentTime();
        double secondsElapsed = 0;
        long cnt = 0;
        rController.start(reader.params.getRecordsPerSec());
        while (time.elapsedMilliSeconds(status.endTime, startTime) < msToRun) {
            recordTime.recordRead(dType, size, time, status, reader.perlChannel);
            cnt += status.records;
            secondsElapsed = time.elapsedSeconds(status.endTime, loopStartTime);
            rController.control(cnt, secondsElapsed);
        }
    }

    /**
     * Benchmarking reader by reading events/records for specific time duration with Rate controlled.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsTimeReaderRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                              RateController rController) throws EOFException,
            IOException {
        genericRecordsTimeReaderRateControl(reader, secondsToRun, dType, time, rController, this::recordRead);
    }

    /**
     * Benchmarking reader by reading events/records for specific time duration with Rate controlled.
     * used while another writer is writing the data.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsTimeReaderRWRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                                RateController rController) throws EOFException, IOException {
        genericRecordsTimeReaderRateControl(reader, secondsToRun, dType, time, rController, this::recordReadTime);
    }

    interface RecordTime<T> {

        void recordRead(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel)
                throws EOFException, IOException;
    }
}

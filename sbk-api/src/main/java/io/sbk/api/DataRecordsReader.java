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

import io.perl.api.PerlChannel;
import io.sbk.data.DataType;
import io.sbk.logger.ReadRequestsLogger;
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
     * Record the single or multiple reads performance statistics.
     *
     * @param dType       dataType
     * @param size        size of data in bytes
     * @param time        time interface
     * @param status      read status to return; {@link io.sbk.api.Status}
     * @param perlChannel to call for benchmarking
     * @param id          reader id
     * @param logger      Read Request logger
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    void recordRead(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel, int id,
                    ReadRequestsLogger logger)
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


    /**
     * Record the single or multiple reads performance statistics along with the starting time in the data.
     *
     * @param dType       dataType
     * @param size        size of data in bytes
     * @param time        time interface
     * @param status      read status to return; {@link io.sbk.api.Status}
     * @param perlChannel to call for benchmarking
     * @param id          reader id
     * @param logger      Read Request logger
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    void recordReadTime(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel, int id,
                        ReadRequestsLogger logger)
            throws EOFException, IOException;

    /**
     * Benchmarking reader by reading given number of records.
     *
     * @param reader            Worker
     * @param recordsCount       long
     * @param dType              DataType
     * @param time               Time
     * @param recordTime         RecordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
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
     * Benchmarking reader by reading given number of records.
     *
     * @param reader            Worker
     * @param recordsCount       long
     * @param dType              DataType
     * @param time               Time
     * @param recordTime         RecordTime
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void genericRecordsReader(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                      RecordTimeRequests<T> recordTime, ReadRequestsLogger logger)
            throws EOFException, IOException {
        final Status status = new Status();
        final int size = reader.params.getRecordSize();
        long i = 0;
        while (i < recordsCount) {
            recordTime.recordRead(dType, size, time, status, reader.perlChannel, reader.id, logger);
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
     * This method uses the method {@link DataRecordsReader#recordRead(DataType, int, Time, Status, PerlChannel)}
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsReader(Worker reader, long recordsCount, DataType<T> dType, Time time,
                               ReadRequestsLogger logger) throws EOFException, IOException {
        genericRecordsReader(reader, recordsCount, dType, time, this::recordRead, logger);
    }

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     * This method uses the method {@link DataRecordsReader#recordReadTime(DataType, int, Time, Status, PerlChannel)}
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


    /**
     * Default implementation for benchmarking reader by reading given number of records.
     * This method uses the method {@link DataRecordsReader#recordReadTime(DataType, int, Time, Status, PerlChannel)}
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsReaderRW(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                 ReadRequestsLogger logger) throws EOFException, IOException {
        genericRecordsReader(reader, recordsCount, dType, time, this::recordReadTime, logger);
    }

    /**
     * Benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader             Worker
     * @param secondsToRun       long
     * @param dType              DataType
     * @param time               Time
     * @param recordTime         RecordTime
     * @throws EOFException  If the End of the file occurred.
     * @throws IOException   If an exception occurred.
     */
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
     * Benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader             Worker
     * @param secondsToRun       long
     * @param dType              DataType
     * @param time               Time
     * @param recordTime         RecordTime
     * @param logger       log read requests
     * @throws EOFException  If the End of the file occurred.
     * @throws IOException   If an exception occurred.
     */
    default void genericRecordsTimeReader(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                          RecordTimeRequests<T> recordTime, ReadRequestsLogger logger) throws EOFException,
            IOException {
        final long startTime = time.getCurrentTime();
        final int size = reader.params.getRecordSize();
        final Status status = new Status();
        final long msToRun = secondsToRun * Time.MS_PER_SEC;
        while (time.elapsedMilliSeconds(status.endTime, startTime) < msToRun) {
            recordTime.recordRead(dType, size, time, status, reader.perlChannel, reader.id, logger);
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
     * This method uses the method {@link DataRecordsReader#recordRead(DataType, int, Time, Status, PerlChannel)}
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsTimeReader(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                   ReadRequestsLogger logger) throws EOFException, IOException {
        genericRecordsTimeReader(reader, secondsToRun, dType, time, this::recordRead, logger);

    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     * This method uses the method {@link DataRecordsReader#recordReadTime(DataType, int, Time, Status, PerlChannel)}
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

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     * This method uses the method {@link DataRecordsReader#recordReadTime(DataType, int, Time, Status, PerlChannel)}
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsTimeReaderRW(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                     ReadRequestsLogger logger) throws EOFException, IOException {
        genericRecordsTimeReader(reader, secondsToRun, dType, time, this::recordReadTime, logger);
    }

    /**
     * Benchmarking reader with Rate controlled.
     *
     * @param reader        Worker
     * @param recordsCount  long
     * @param dType         DataType
     * @param time          Time
     * @param rController   RateController
     * @param recordTime    RecordTime
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
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
     * Benchmarking reader with Rate controlled.
     *
     * @param reader        Worker
     * @param recordsCount  long
     * @param dType         DataType
     * @param time          Time
     * @param rController   RateController
     * @param recordTime    RecordTime
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void genericRecordsReaderRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                                 RateController rController, RecordTimeRequests<T> recordTime,
                                                 ReadRequestsLogger logger) throws EOFException, IOException {
        final Status status = new Status();
        final int size = reader.params.getRecordSize();
        long i = 0;
        double secondsElapsed = 0;
        final long loopStartTime = time.getCurrentTime();
        rController.start(reader.params.getRecordsPerSec());
        while (i < recordsCount) {
            recordTime.recordRead(dType, size, time, status, reader.perlChannel, reader.id, logger);
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
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsReaderRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                          RateController rController, ReadRequestsLogger logger) throws EOFException,
            IOException {
        genericRecordsReaderRateControl(reader, recordsCount, dType, time, rController, this::recordRead, logger);
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

    /**
     * Benchmarking reader by reading given number of records with Rate controlled.
     * used while another writer is writing the data.
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsReaderRWRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                            RateController rController, ReadRequestsLogger logger)
            throws EOFException, IOException {
        genericRecordsReaderRateControl(reader, recordsCount, dType, time, rController, this::recordReadTime, logger);
    }

    /**
     * Benchmarking reader by reading events/records with Rate Controls.
     *
     * @param reader             Worker
     * @param secondsToRun       long
     * @param dType              DataType
     * @param time               Time
     * @param rController        RateController
     * @param recordTime         RecordTime
     * @throws EOFException  If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
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
     * Benchmarking reader by reading events/records with Rate Controls.
     *
     * @param reader             Worker
     * @param secondsToRun       long
     * @param dType              DataType
     * @param time               Time
     * @param rController        RateController
     * @param recordTime         RecordTime
     * @param logger            log read requests
     * @throws EOFException  If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void genericRecordsTimeReaderRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                                     RateController rController, RecordTimeRequests<T> recordTime,
                                                     ReadRequestsLogger logger) throws EOFException, IOException {
        final long startTime = time.getCurrentTime();
        final int size = reader.params.getRecordSize();
        final Status status = new Status();
        final long msToRun = secondsToRun * Time.MS_PER_SEC;
        final long loopStartTime = time.getCurrentTime();
        double secondsElapsed = 0;
        long cnt = 0;
        rController.start(reader.params.getRecordsPerSec());
        while (time.elapsedMilliSeconds(status.endTime, startTime) < msToRun) {
            recordTime.recordRead(dType, size, time, status, reader.perlChannel, reader.id, logger);
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
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsTimeReaderRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                              RateController rController, ReadRequestsLogger logger)
            throws EOFException, IOException {
        genericRecordsTimeReaderRateControl(reader, secondsToRun, dType, time, rController, this::recordRead, logger);
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


    /**
     * Benchmarking reader by reading events/records for specific time duration with Rate controlled.
     * used while another writer is writing the data.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @param logger       log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    default void RecordsTimeReaderRWRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                                RateController rController, ReadRequestsLogger logger)
            throws EOFException, IOException {
        genericRecordsTimeReaderRateControl(reader, secondsToRun, dType, time, rController, this::recordReadTime, logger);
    }

    /**
     * interface RecordTime.
     *
     * @param <T>  Flexible parameter
     */
    interface RecordTime<T> {

        /**
         * Method to Read records.
         *
         * @param dType          DataType
         * @param size           int
         * @param time           Time
         * @param status         Status
         * @param perlChannel    PerlChannel
         * @throws EOFException  End of File exception.
         * @throws IOException  If an exception occurred.
         */
        void recordRead(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel)
                throws EOFException, IOException;
    }

    interface RecordTimeRequests<T> {

        /**
         * Method to Read records.
         *
         * @param dType          DataType
         * @param size           int
         * @param time           Time
         * @param status         Status
         * @param perlChannel    PerlChannel
         * @param id             Reader id
         * @param logger         log read requests
         * @throws EOFException  End of File exception.
         * @throws IOException  If an exception occurred.
         */
        void recordRead(DataType<T> dType, int size, Time time, Status status, PerlChannel perlChannel, int id,
                        ReadRequestsLogger logger) throws EOFException, IOException;
    }

}

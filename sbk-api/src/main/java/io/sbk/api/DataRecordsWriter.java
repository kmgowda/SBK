/**
 * Copyright (c) KMG. All Rights Reserved..
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

import java.io.IOException;

/**
 * Interface for Data Records Writers.
 */
public sealed interface DataRecordsWriter<T> extends DataWriter<T> permits Writer {

    /**
     * Flush / Sync the  data.
     *
     * @throws IOException If an exception occurred.
     */
    void sync() throws IOException;


    /**
     * Write the Data and attach the start time to data.
     *
     * @param dType  Data Type interface
     * @param data   data to writer
     * @param size   size of the data
     * @param time   time interface
     * @param status write status to return; {@link io.sbk.api.Status}
     * @throws IOException If an exception occurred.
     */
    void writeSetTime(DataType<T> dType, T data, int size, Time time, Status status) throws IOException;


    /**
     * Write the data and record the benchmark statistics.
     *
     * @param dType       Data Type interface
     * @param data        data to write
     * @param size        size of the data
     * @param time        time interface
     * @param status      Write status to return; {@link io.sbk.api.Status}
     * @param perlChannel to call for benchmarking
     * @throws IOException If an exception occurred.
     */
    void recordWrite(DataType<T> dType, T data, int size, Time time,
                     Status status, PerlChannel perlChannel) throws IOException;


    /**
     * Default implementation for writer benchmarking by writing given number of records.
     * Write is performed using {@link io.sbk.api.DataRecordsWriter#recordWrite(DataType, Object, int, Time, Status, PerlChannel)}
     * sync is invoked after writing all the records.
     *
     * @param writer       Writer Descriptor
     * @param recordsCount Records Count
     * @param dType        Data Type interface
     * @param data         data to write
     * @param size         size of the data
     * @param time         time interface
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriter(Worker writer, long recordsCount, DataType<T> dType, T data, int size, Time time) throws IOException {
        final Status status = new Status();
        long i = 0;
        while (i < recordsCount) {
            recordWrite(dType, data, size, time, status, writer.perlChannel);
            i += status.records;
        }
        sync();
    }

    /**
     * Default implementation for writer benchmarking by writing given number of records.
     * Write is performed using {@link io.sbk.api.DataRecordsWriter#recordWrite(DataType, Object, int, Time, Status, PerlChannel, int)}
     * sync is invoked after writing given set of records.
     *
     * @param writer       Writer Descriptor
     * @param recordsCount Records Count
     * @param dType        Data Type interface
     * @param data         data to write
     * @param size         size of the data
     * @param time         time interface
     * @param rController  Rate Controller
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterSync(Worker writer, long recordsCount, DataType<T> dType, T data, int size,
                                   Time time, RateController rController) throws IOException {
        final Status status = new Status();
        final long loopStartTime = time.getCurrentTime();
        long cnt = 0;
        rController.start(writer.params.getRecordsPerSec());
        while (cnt < recordsCount) {
            long loopMax = Math.min(writer.params.getRecordsPerSync(), recordsCount - cnt);
            long i = 0;
            while (i < loopMax) {
                recordWrite(dType, data, size, time, status, writer.perlChannel);
                i += status.records;
                cnt += status.records;
                rController.control(cnt, time.elapsedSeconds(status.startTime, loopStartTime));
            }
            sync();
        }
    }

    /**
     * Default implementation for writer benchmarking by continuously writing data records for specific time duration.
     * Write is performed using {@link io.sbk.api.DataRecordsWriter#recordWrite(DataType, Object, int, Time, Status, PerlChannel, int)}
     * sync is invoked after writing records for given time.
     *
     * @param writer       Writer Descriptor
     * @param secondsToRun Number of seconds to Run
     * @param dType        Data Type interface
     * @param data         data to write
     * @param size         size of the data
     * @param time         time interface
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterTime(Worker writer, long secondsToRun, DataType<T> dType, T data, int size,
                                   Time time) throws IOException {
        final Status status = new Status();
        final long msToRun = secondsToRun * Time.MS_PER_SEC;
        long startTime = time.getCurrentTime();
        status.startTime = startTime;
        double msElapsed = 0;
        while (msElapsed < msToRun) {
            recordWrite(dType, data, size, time, status, writer.perlChannel);
            msElapsed = time.elapsedMilliSeconds(status.startTime, startTime);
        }
        sync();
    }

    /**
     * Default implementation for writer benchmarking by continuously writing data records for specific time duration.
     * Write is performed using {@link io.sbk.api.DataRecordsWriter#recordWrite(DataType, Object, int, Time, Status, PerlChannel, int)}
     * sync is invoked after writing given set of records.
     *
     * @param writer       Writer Descriptor
     * @param secondsToRun Number of seconds to Run
     * @param dType        Data Type interface
     * @param data         data to write
     * @param size         size of the data
     * @param time         time interface
     * @param rController  Rate Controller
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterTimeSync(Worker writer, long secondsToRun, DataType<T> dType, T data, int size,
                                       Time time, RateController rController) throws IOException {
        final Status status = new Status();
        final long loopStartTime = time.getCurrentTime();
        int cnt = 0;
        double secondsElapsed = 0;
        status.startTime = loopStartTime;
        rController.start(writer.params.getRecordsPerSec());
        while (secondsElapsed < secondsToRun) {
            int i = 0;
            while ((secondsElapsed < secondsToRun) && (i < writer.params.getRecordsPerSync())) {
                recordWrite(dType, data, size, time, status, writer.perlChannel);
                i += status.records;
                cnt += status.records;
                secondsElapsed = time.elapsedSeconds(status.startTime, loopStartTime);
                rController.control(cnt, secondsElapsed);
            }
            sync();
        }
    }

    /**
     * Default implementation for writing given number of records. No Writer Benchmarking is performed.
     * Write is performed using {@link io.sbk.api.DataRecordsWriter#writeSetTime(DataType, Object, int, Time, Status)} .
     * sync is invoked after writing given set of records.
     *
     * @param writer       Writer Descriptor
     * @param recordsCount Records Count
     * @param dType        Data Type interface
     * @param data         data to write
     * @param size         size of the data
     * @param time         time interface
     * @param rController  Rate Controller
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterRW(Worker writer, long recordsCount, DataType<T> dType, T data, int size,
                                 Time time, RateController rController) throws IOException {
        final Status status = new Status();
        final long loopStartTime = time.getCurrentTime();
        long cnt = 0;
        rController.start(writer.params.getRecordsPerSec());
        while (cnt < recordsCount) {
            long loopMax = Math.min(writer.params.getRecordsPerSync(), recordsCount - cnt);
            long i = 0;
            while (i < loopMax) {
                writeSetTime(dType, data, size, time, status);
                i += status.records;
                cnt += status.records;
                rController.control(cnt, time.elapsedSeconds(status.startTime, loopStartTime));
            }
            sync();
        }
    }

    /**
     * Default implementation for writing data records for specific time duration. No Writer Benchmarking is performed.
     * Write is performed using {@link io.sbk.api.DataRecordsWriter#writeSetTime(DataType, Object, int, Time, Status)} .
     * sync is invoked after writing given set of records.
     *
     * @param writer       Writer Descriptor
     * @param secondsToRun Number of seconds to Run
     * @param dType        Data Type interface
     * @param data         data to write
     * @param size         size of the data
     * @param time         time interface
     * @param rController  Rate Controller
     * @throws IOException If an exception occurred.
     */
    default void RecordsWriterTimeRW(Worker writer, long secondsToRun, DataType<T> dType, T data, int size,
                                     Time time, RateController rController) throws IOException {
        final Status status = new Status();
        final long loopStartTime = time.getCurrentTime();
        long cnt = 0;
        double secondsElapsed = 0;
        status.startTime = loopStartTime;
        rController.start(writer.params.getRecordsPerSec());
        while (secondsElapsed < secondsToRun) {
            long i = 0;
            while ((secondsElapsed < secondsToRun) && (i < writer.params.getRecordsPerSync())) {
                writeSetTime(dType, data, size, time, status);
                i += status.records;
                cnt += status.records;
                secondsElapsed = time.elapsedSeconds(status.startTime, loopStartTime);
                rController.control(cnt, secondsElapsed);
            }
            sync();
        }
    }
}

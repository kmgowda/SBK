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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sbk.data.DataType;
import io.sbk.logger.ReadRequestsLogger;
import io.time.Time;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract helper for callback-style readers.
 *
 * <p>Provides a small lifecycle and metric reporting adapter that callback-based
 * storage drivers can extend. It centralises the timing and PerL reporting
 * logic while leaving the concrete 'consume' method for driver-specific
 * payload handling.
 *
 * <p>Callback delivery is controlled by the storage client rather than an
 * SBK read loop. Rate control and read-request logging are therefore rejected
 * before callback delivery starts instead of being silently ignored or adding
 * coordination to the callback hot path.
 *
 * @param <T> storage record type
 */
public abstract non-sealed class AbstractCallbackReader<T> implements DataReader<T> {
    private static final String REQUEST_LOGGING_UNSUPPORTED =
            "Callback readers do not support read-request logging";
    private static final String RATE_CONTROL_UNSUPPORTED =
            "Callback readers do not support rate control";
    private DataType<T> dataType;
    private Time time;
    private CompletableFuture<Void> ret;
    private Worker reader;
    private BenchmarkRecorder benchmarkRecorder;

    /**
     * Creates a callback-based reader whose runtime state is supplied by
     * {@link #initialize(Worker, long, long, DataType, Time, Callback)}.
     */
    public AbstractCallbackReader() {
    }

    /**
     * set the Callback and start the CallBack Reader.
     *
     * @param callback Reader callback.
     * @throws IOException If an exception occurred.
     */
    public abstract void start(Callback<T> callback) throws IOException;

    /**
     * Stop the CallBack Reader.
     *
     * @throws IOException If an exception occurred.
     */
    public abstract void stop() throws IOException;


    /**
     * Close the CallBack Reader.
     * stops the callback reader.
     *
     * @throws IOException If an exception occurred.
     */
    @Override
    public final void close() throws IOException {
        stop();
        complete();
    }

    /**
     * Keeps record of Benchmark if record gets completed it call complete function.
     *
     * @param startTime     long
     * @param endTime       long
     * @param dataSize      int
     * @param events        int
     */
    public void recordBenchmark(long startTime, long endTime, int dataSize, int events) {
        benchmarkRecorder.record(startTime, endTime, dataSize, events);
    }

    /**
     * Default Implementation to initialize the callback reader.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param callback     Callback interface
     * @throws IOException If an exception occurred.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void initialize(Worker reader, long secondsToRun, long recordsCount, DataType<T> dType, Time time,
                           Callback<T> callback) throws IOException {
        this.reader = reader;
        this.dataType = dType;
        this.time = time;
        if (secondsToRun > 0) {
            benchmarkRecorder = new DurationBenchmarkRecorder(
                    time, time.getCurrentTime(), time.secondsToTimeUnits(secondsToRun));
        } else if (recordsCount > 0) {
            benchmarkRecorder = new RecordsBenchmarkRecorder(recordsCount);
        } else {
            benchmarkRecorder = new UnboundedBenchmarkRecorder();
        }
        this.ret = new CompletableFuture<>();
        start(callback);
    }

    /**
     * Default Implementation complete the read.
     */
    public void complete() {
        if (ret != null) {
            ret.complete(null);
        }
    }

    /**
     * Default Implementation to wait for the readers to complete.
     *
     * @throws IOException If an exception occurred.
     */
    public void waitToComplete() throws IOException {
        try {
            if (ret != null) {
                ret.get();
            }
        } catch (ExecutionException | InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Default Implementation run the Benchmark.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param callback     Callback interface
     * @throws IOException If an exception occurred.
     */
    public void run(Worker reader, long secondsToRun, long recordsCount, DataType<T> dType, Time time,
                    Callback<T> callback) throws IOException {
        initialize(reader, secondsToRun, recordsCount, dType, time, callback);
        waitToComplete();
    }

    private interface BenchmarkRecorder {
        void record(long startTime, long endTime, int dataSize, int events);
    }

    private final class DurationBenchmarkRecorder implements BenchmarkRecorder {
        private final Time time;
        private final long beginTime;
        private final long timeUnitsToRun;

        private DurationBenchmarkRecorder(Time time, long beginTime, long timeUnitsToRun) {
            this.time = time;
            this.beginTime = beginTime;
            this.timeUnitsToRun = timeUnitsToRun;
        }

        @Override
        public void record(long startTime, long endTime, int dataSize, int events) {
            reader.perlChannel.send(startTime, endTime, events, dataSize);
            if (time.elapsed(endTime, beginTime) >= timeUnitsToRun) {
                complete();
            }
        }
    }

    private final class RecordsBenchmarkRecorder implements BenchmarkRecorder {
        private final AtomicLong readCount = new AtomicLong();
        private final long recordsCount;

        private RecordsBenchmarkRecorder(long recordsCount) {
            this.recordsCount = recordsCount;
        }

        @Override
        public void record(long startTime, long endTime, int dataSize, int events) {
            final long count = readCount.addAndGet(events);
            reader.perlChannel.send(startTime, endTime, events, dataSize);
            if (count >= recordsCount) {
                complete();
            }
        }
    }

    private final class UnboundedBenchmarkRecorder implements BenchmarkRecorder {
        @Override
        public void record(long startTime, long endTime, int dataSize, int events) {
            reader.perlChannel.send(startTime, endTime, events, dataSize);
        }
    }

    /**
     * Implementation for benchmarking reader by reading given number of records.
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsReader(Worker reader, long recordsCount, DataType<T> dType, Time time) throws EOFException,
            IOException {
        run(reader, 0, recordsCount, dType, time, new ConsumeRead());
    }


    /**
     * Implementation for benchmarking reader by reading given number of records.
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsReader(Worker reader, long recordsCount, DataType<T> dType, Time time,
                              ReadRequestsLogger logger) throws EOFException, IOException {
        throw new IOException(REQUEST_LOGGING_UNSUPPORTED);
    }

    /**
     * Default implementation for benchmarking reader by reading given number of records.
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsReaderRW(Worker reader, long recordsCount, DataType<T> dType, Time time) throws EOFException,
            IOException {
        run(reader, 0, recordsCount, dType, time, new ConsumeRW());
    }


    /**
     * Default implementation for benchmarking reader by reading given number of records.
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsReaderRW(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                ReadRequestsLogger logger) throws EOFException,
            IOException {
        throw new IOException(REQUEST_LOGGING_UNSUPPORTED);
    }


    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsTimeReader(Worker reader, long secondsToRun, DataType<T> dType, Time time)
            throws EOFException, IOException {
        run(reader, secondsToRun, 0, dType, time, new ConsumeRead());
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsTimeReader(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                  ReadRequestsLogger logger) throws EOFException, IOException {
        throw new IOException(REQUEST_LOGGING_UNSUPPORTED);
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsTimeReaderRW(Worker reader, long secondsToRun, DataType<T> dType, Time time) throws EOFException, IOException {
        run(reader, secondsToRun, 0, dType, time, new ConsumeRW());
    }

    /**
     * Default implementation for benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsTimeReaderRW(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                    ReadRequestsLogger logger) throws EOFException, IOException {
        throw new IOException(REQUEST_LOGGING_UNSUPPORTED);
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
    public void RecordsReaderRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                         RateController rController) throws EOFException, IOException {
        throw new IOException(RATE_CONTROL_UNSUPPORTED);
    }



    /**
     * Benchmarking reader by reading given number of records with Rate controlled.
     *
     * @param reader       Reader Descriptor
     * @param recordsCount Records count
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsReaderRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                         RateController rController, ReadRequestsLogger logger)
            throws EOFException, IOException {
        throw new IOException(RATE_CONTROL_UNSUPPORTED);
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
    public void RecordsReaderRWRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                           RateController rController) throws EOFException, IOException {
        throw new IOException(RATE_CONTROL_UNSUPPORTED);
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
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsReaderRWRateControl(Worker reader, long recordsCount, DataType<T> dType, Time time,
                                           RateController rController, ReadRequestsLogger logger)
            throws EOFException, IOException {
        throw new IOException(RATE_CONTROL_UNSUPPORTED);
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
    public void RecordsTimeReaderRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                             RateController rController) throws EOFException, IOException {
        throw new IOException(RATE_CONTROL_UNSUPPORTED);
    }

    /**
     * Benchmarking reader by reading events/records for specific time duration with Rate controlled.
     *
     * @param reader       Reader Descriptor
     * @param secondsToRun Number of seconds to run
     * @param dType        dataType
     * @param time         time interface
     * @param rController  Rate Controller
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsTimeReaderRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                             RateController rController, ReadRequestsLogger logger)
            throws EOFException, IOException {
        throw new IOException(RATE_CONTROL_UNSUPPORTED);
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
    public void RecordsTimeReaderRWRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                               RateController rController) throws EOFException, IOException {
        throw new IOException(RATE_CONTROL_UNSUPPORTED);
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
     * @param logger        log read requests
     * @throws EOFException If the End of the file occurred.
     * @throws IOException  If an exception occurred.
     */
    public void RecordsTimeReaderRWRateControl(Worker reader, long secondsToRun, DataType<T> dType, Time time,
                                               RateController rController, ReadRequestsLogger logger)
            throws EOFException, IOException {
        throw new IOException(RATE_CONTROL_UNSUPPORTED);
    }


    private class ConsumeRead implements Callback<T> {

        public void consume(final T data) {
            final long endTime = time.getCurrentTime();
            recordBenchmark(endTime, endTime, dataType.length(data), 1);
        }

        public void record(long startTime, long endTime, int dataSize, int records) {
            recordBenchmark(startTime, endTime, dataSize, records);
        }

    }

    private class ConsumeRW implements Callback<T> {

        public void consume(final T data) {
            recordBenchmark(dataType.getTime(data), time.getCurrentTime(), dataType.length(data), 1);
        }

        public void record(long startTime, long endTime, int dataSize, int records) {
            recordBenchmark(startTime, endTime, dataSize, records);
        }

    }

}

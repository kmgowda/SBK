/**
 * Copyright (c) 2020 KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perf.core;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract class for Writers.
 */
public abstract class Writer extends Worker implements Callable<Void> {
    final private static int MS_PER_SEC = 1000;
    final private Performance perf;
    final private byte[] payload;

    public Writer(int writerID, TriConsumer recordTime, Parameters params) {
        super(writerID, recordTime, params);
        this.payload = createPayload(params.recordSize);
        this.perf = createBenchmark();
    }

    /**
     * Asynchronously Writes the data .
     *
     * @param data data to write
     */
    public abstract CompletableFuture writeAsync(byte[] data);

    /**
     * Flush the  data.
     */
    public abstract void flush() throws IOException;

    /**
     * Flush the  data.
     */
    public abstract void close() throws IOException;

    private byte[] createPayload(int size) {
        Random random = new Random();
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; ++i) {
            bytes[i] = (byte) (random.nextInt(26) + 65);
        }
        return bytes;
    }

    /**
     * Writes the data and benchmark.
     *
     * @param data   data to write
     * @param record to call for benchmarking
     * @return time return the data sent time
     */
    public long recordWrite(byte[] data, TriConsumer record) {
        CompletableFuture ret;
        final long time = System.currentTimeMillis();
        ret = writeAsync(data);
        if (ret == null) {
            final long endTime = System.currentTimeMillis();
            record.accept(time, endTime, data.length);
        } else {
            ret.thenAccept(d -> {
                final long endTime = System.currentTimeMillis();
                record.accept(time, endTime, data.length);
            });
        }
        return time;
    }

    @Override
    public Void call() throws InterruptedException, ExecutionException, IOException {
        try {
            perf.benchmark();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return null;
    }


    final private Performance createBenchmark() {
        final Performance perfWriter;
        if (params.secondsToRun > 0) {
            if (params.writeAndRead) {
                perfWriter = this::RecordsWriterTimeRW;
            } else {
                if (params.recordsPerSec > 0 || params.recordsPerFlush < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterTimeSleep;
                } else {
                    perfWriter = this::RecordsWriterTime;
                }
            }
        } else {
            if (params.writeAndRead) {
                perfWriter = this::RecordsWriterRW;
            } else {
                if (params.recordsPerSec > 0 || params.recordsPerFlush < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterSleep;
                } else {
                    perfWriter = this::RecordsWriter;
                }
            }
        }
        return perfWriter;
    }




    final private void RecordsWriter() throws InterruptedException, IOException {
        for (int i = 0; i < params.records; i++) {
            recordWrite(payload, recordTime);
        }
        flush();
    }


    final private void RecordsWriterSleep() throws InterruptedException, IOException {
        final RateController eCnt = new RateController(System.currentTimeMillis(), params.recordsPerSec);
        int cnt = 0;
        while (cnt < params.records) {
            int loopMax = Math.min(params.recordsPerFlush, params.records - cnt);
            for (int i = 0; i < loopMax; i++) {
                eCnt.control(cnt++, recordWrite(payload, recordTime));
            }
            flush();
        }
    }


    final private void RecordsWriterTime() throws InterruptedException, IOException {
        final long msToRun = params.secondsToRun * MS_PER_SEC;
        long time = System.currentTimeMillis();
        while ((time - params.startTime) < msToRun) {
            time = recordWrite(payload, recordTime);
        }
        flush();
    }


    final private void RecordsWriterTimeSleep() throws InterruptedException, IOException {
        final long msToRun = params.secondsToRun * MS_PER_SEC;
        long time = System.currentTimeMillis();
        final RateController eCnt = new RateController(time, params.recordsPerSec);
        long msElapsed = time - params.startTime;
        int cnt = 0;
        while (msElapsed < msToRun) {
            for (int i = 0; (msElapsed < msToRun) && (i < params.recordsPerFlush); i++) {
                time = recordWrite(payload, recordTime);
                eCnt.control(cnt++, time);
                msElapsed = time - params.startTime;
            }
            flush();
        }
    }


    final private void RecordsWriterRW() throws InterruptedException, IOException {
        final ByteBuffer timeBuffer = ByteBuffer.allocate(TIME_HEADER_SIZE);
        final long time = System.currentTimeMillis();
        final RateController eCnt = new RateController(time, params.recordsPerSec);
        for (int i = 0; i < params.records; i++) {
            byte[] bytes = timeBuffer.putLong(0, System.currentTimeMillis()).array();
            System.arraycopy(bytes, 0, payload, 0, bytes.length);
            writeAsync(payload);
                /*
                flush is required here for following reasons:
                1. The writeData is called for End to End latency mode; hence make sure that data is sent.
                2. In case of kafka benchmarking, the buffering makes too many writes;
                   flushing moderates the kafka producer.
                3. If the flush called after several iterations, then flush may take too much of time.
                */
            flush();
            eCnt.control(i);
        }
    }


    final private void RecordsWriterTimeRW() throws InterruptedException, IOException {
        final long msToRun = params.secondsToRun * MS_PER_SEC;
        final ByteBuffer timeBuffer = ByteBuffer.allocate(TIME_HEADER_SIZE);
        long time = System.currentTimeMillis();
        final RateController eCnt = new RateController(time, params.recordsPerSec);

        for (int i = 0; (time - params.startTime) < msToRun; i++) {
            time = System.currentTimeMillis();
            byte[] bytes = timeBuffer.putLong(0, time).array();
            System.arraycopy(bytes, 0, payload, 0, bytes.length);
            writeAsync(payload);
                /*
                flush is required here for following reasons:
                1. The writeData is called for End to End latency mode; hence make sure that data is sent.
                2. In case of kafka benchmarking, the buffering makes too many writes;
                   flushing moderates the kafka producer.
                3. If the flush called after several iterations, then flush may take too much of time.
                */
            flush();
            eCnt.control(i);
        }
    }


    @NotThreadSafe
    final static private class RateController {
        private static final long NS_PER_MS = 1000000L;
        private static final long NS_PER_SEC = 1000 * NS_PER_MS;
        private static final long MIN_SLEEP_NS = 2 * NS_PER_MS;
        private final long startTime;
        private final long sleepTimeNs;
        private final int recordsPerSec;
        private long toSleepNs = 0;

        /**
         * @param recordsPerSec events per second
         */
        private RateController(long start, int recordsPerSec) {
            this.startTime = start;
            this.recordsPerSec = recordsPerSec;
            this.sleepTimeNs = this.recordsPerSec > 0 ?
                    NS_PER_SEC / this.recordsPerSec : 0;
        }

        /**
         * Blocks for small amounts of time to achieve targetThroughput/events per sec
         *
         * @param events current events
         */
        void control(long events) {
            if (this.recordsPerSec <= 0) {
                return;
            }
            needSleep(events, System.currentTimeMillis());
        }

        /**
         * Blocks for small amounts of time to achieve targetThroughput/events per sec
         *
         * @param events current events
         * @param time   current time
         */
        void control(long events, long time) {
            if (this.recordsPerSec <= 0) {
                return;
            }
            needSleep(events, time);
        }

        private void needSleep(long events, long time) {
            float elapsedSec = (time - startTime) / 1000.f;

            if ((events / elapsedSec) < this.recordsPerSec) {
                return;
            }

            // control throughput / number of events by sleeping, on average,
            toSleepNs += sleepTimeNs;
            // If threshold reached, sleep a little
            if (toSleepNs >= MIN_SLEEP_NS) {
                long sleepStart = System.nanoTime();
                try {
                    final long sleepMs = toSleepNs / NS_PER_MS;
                    final long sleepNs = toSleepNs - sleepMs * NS_PER_MS;
                    Thread.sleep(sleepMs, (int) sleepNs);
                } catch (InterruptedException e) {
                    // will be taken care in finally block
                } finally {
                    // in case of short sleeps or oversleep ;adjust it for next sleep duration
                    final long sleptNS = System.nanoTime() - sleepStart;
                    if (sleptNS > 0) {
                        toSleepNs -= sleptNS;
                    } else {
                        toSleepNs = 0;
                    }
                }
            }
        }
    }
}

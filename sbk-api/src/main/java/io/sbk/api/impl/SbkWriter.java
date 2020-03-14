/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.QuadConsumer;
import io.sbk.api.Writer;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

/**
 * Writer Benchmarking Implementation.
 */
public class SbkWriter extends Worker implements Runnable {
    final private static int MS_PER_SEC = 1000;
    final private DataType data;
    final private Writer writer;
    final private RunBenchmark perf;
    final private Object payload;

    public SbkWriter(int writerID, Parameters params, QuadConsumer recordTime, DataType data, Writer writer) {
        super(writerID, params, recordTime);
        this.data = data;
        this.writer = writer;
        this.payload = data.create(params.getRecordSize());
        this.perf = createBenchmark();
    }

    @Override
    public void run()  {
        try {
            perf.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    final private RunBenchmark createBenchmark() {
        final RunBenchmark perfWriter;
        if (params.getSecondsToRun() > 0) {
            if (params.isWriteAndRead()) {
                perfWriter = this::RecordsWriterTimeRW;
            } else {
                if (params.getRecordsPerSec() > 0 || params.getRecordsPerFlush() < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterTimeSleep;
                } else {
                    perfWriter = this::RecordsWriterTime;
                }
            }
        } else {
            if (params.isWriteAndRead()) {
                perfWriter = this::RecordsWriterRW;
            } else {
                if (params.getRecordsPerSec() > 0 || params.getRecordsPerFlush() < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterSleep;
                } else {
                    perfWriter = this::RecordsWriter;
                }
            }
        }
        return perfWriter;
    }


    final private void RecordsWriter() throws InterruptedException, IOException {
        final int size = data.length(payload);
        for (int i = 0; i < params.getRecordsCount(); i++) {
            writer.recordWrite(payload, size, recordTime);
        }
        writer.flush();
    }


    final private void RecordsWriterSleep() throws InterruptedException, IOException {
        final RateController eCnt = new RateController(System.currentTimeMillis(), params.getRecordsPerSec());
        final int recordsCount = params.getRecordsCount();
        final int size = data.length(payload);
        int cnt = 0;
        while (cnt < recordsCount) {
            int loopMax = Math.min(params.getRecordsPerFlush(), recordsCount - cnt);
            for (int i = 0; i < loopMax; i++) {
                eCnt.control(cnt++, writer.recordWrite(payload, size, recordTime));
            }
            writer.flush();
        }
    }


    final private void RecordsWriterTime() throws InterruptedException, IOException {
        final long startTime = params.getStartTime();
        final long msToRun = params.getSecondsToRun() * MS_PER_SEC;
        final int size = data.length(payload);
        long time = System.currentTimeMillis();
        while ((time - startTime) < msToRun) {
            time = writer.recordWrite(payload, size, recordTime);
        }
        writer.flush();
    }


    final private void RecordsWriterTimeSleep() throws InterruptedException, IOException {
        final long startTime = params.getStartTime();
        final long msToRun = params.getSecondsToRun() * MS_PER_SEC;
        final int size = data.length(payload);
        long time = System.currentTimeMillis();
        final RateController eCnt = new RateController(time, params.getRecordsPerSec());
        long msElapsed = time - startTime;
        int cnt = 0;
        while (msElapsed < msToRun) {
            for (int i = 0; (msElapsed < msToRun) && (i < params.getRecordsPerFlush()); i++) {
                time = writer.recordWrite(payload, size, recordTime);
                eCnt.control(cnt++, time);
                msElapsed = time - startTime;
            }
            writer.flush();
        }
    }


    final private void RecordsWriterRW() throws InterruptedException, IOException {
        final long time = System.currentTimeMillis();
        final RateController eCnt = new RateController(time, params.getRecordsPerSec());
        for (int i = 0; i < params.getRecordsCount(); i++) {
            writer.writeAsync(data.setTime(payload, System.currentTimeMillis()));
            /*
              flush is required here for following reasons:
              1. The writeData is called for End to End latency mode; hence make sure that data is sent.
              2. In case of kafka benchmarking, the buffering makes too many writes;
                 flushing moderates the kafka producer.
              3. If the flush called after several iterations, then flush may take too much of time.
            */
            writer.flush();
            eCnt.control(i);
        }
    }


    final private void RecordsWriterTimeRW() throws InterruptedException, IOException {
        final long startTime = params.getStartTime();
        final long msToRun = params.getSecondsToRun() * MS_PER_SEC;
        long time = System.currentTimeMillis();
        final RateController eCnt = new RateController(time, params.getRecordsPerSec());

        for (int i = 0; (time -startTime) < msToRun; i++) {
            time = System.currentTimeMillis();
            writer.writeAsync(data.setTime(payload, time));
            /*
              flush is required here for following reasons:
              1. The writeData is called for End to End latency mode; hence make sure that data is sent.
              2. In case of kafka benchmarking, the buffering makes too many writes;
                 flushing moderates the kafka producer.
              3. If the flush called after several iterations, then flush may take too much of time.
            */
            writer.flush();
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

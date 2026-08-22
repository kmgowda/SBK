/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.test;

import io.perl.api.Perl;
import io.perl.api.PerlChannel;
import io.perl.api.impl.PerlBuilder;
import io.perl.config.PerlConfig;
import io.perl.exception.BenchmarkIdleTimeoutException;
import io.perl.logger.impl.DefaultLogger;
import io.perl.logger.impl.ResultsLogger;
import io.perl.system.PerlPrinter;
import io.time.MicroSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class for PerL validation.
 */
public class PerlTest {
    public final static int PERL_THREADS = 2;
    public final static int PERL_TOTAL_RECORDS = 100;
    public final static int PERL_RECORDS_PER_THREAD = PERL_TOTAL_RECORDS / PERL_THREADS;
    public final static int PERL_RECORD_SIZE = 10;
    public final static int PERL_TIMEOUT_SECONDS = 5;
    public final static int PERL_SLEEP_MS = 100;

    public static class TestLogger extends ResultsLogger {
        public final AtomicLong latencyReporterCnt;
        public final AtomicLong printCnt;
        public final AtomicLong totalPrintCnt;

        public TestLogger() {
            super();
            latencyReporterCnt = new AtomicLong(0);
            printCnt = new AtomicLong(0);
            totalPrintCnt = new AtomicLong(0);
        }

        @Override
        public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                          double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                          long slc1, long slc2, long[] percentileLatencies, long[] percentileLatenciesCount) {
            super.print(seconds, bytes, records, recsPerSec, mbPerSec,
                    avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2,
                    percentileLatencies, percentileLatenciesCount);
            PerlPrinter.log.info("print : receiving records " + records);
            printCnt.addAndGet(records);
        }

        @Override
        public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                               double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                               long higherDiscard, long slc1, long slc2, long[] percentileLatencies,
                               long[] percentileLatenciesCount) {
            super.printTotal(seconds, bytes, records, recsPerSec, mbPerSec,
                    avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2,
                    percentileLatencies, percentileLatenciesCount);
            PerlPrinter.log.info("printTotal : receiving records " + records);
            totalPrintCnt.addAndGet(records);
        }

        @Override
        public void recordLatency(long startTime, int events, int bytes, long latency) {
            PerlPrinter.log.info("recordLatency : receiving records " + events);
            latencyReporterCnt.addAndGet(events);
        }
    }

    @Test
    public void defaultLoggerLatencyCallbackIsFinal() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(DefaultLogger.class.getMethod("recordLatency",
                long.class, int.class, int.class, long.class).getModifiers()));
    }


    private void runPerlRecords(final TestLogger logger, final Perl perl) throws IOException, ExecutionException,
            InterruptedException, TimeoutException {
        PerlChannel[] channels = new PerlChannel[PERL_THREADS];
        for (int i = 0; i < PERL_THREADS; i++) {
            channels[i] = perl.getPerlChannel();
        }
        CompletableFuture<Void> ret = perl.run(0, PERL_TOTAL_RECORDS);

        int records = PERL_TOTAL_RECORDS;
        int ch = 0;
        while (records > 0) {
            if (ch > PERL_THREADS) {
                ch = 0;
            }
            int finalCh = ch++;
            int finalRecords = records;
            CompletableFuture.runAsync(() -> {
                final long startTime = System.currentTimeMillis();
                channels[finalCh].send(startTime, startTime + 1,
                        Math.min(finalRecords, PERL_RECORDS_PER_THREAD), PERL_RECORD_SIZE);
            });
            records -= PERL_RECORDS_PER_THREAD;
        }
        ret.get(PERL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (logger.latencyReporterCnt.get() != PERL_TOTAL_RECORDS) {
            fail("Latency Reporter Count Failed! Latency Reporter Count : " + logger.latencyReporterCnt.get() +
                    " , Expected : " + PERL_TOTAL_RECORDS);
        }
        if (logger.printCnt.get() != PERL_TOTAL_RECORDS) {
            fail("Print Count Failed! Latency Reporter Count : " + logger.printCnt.get() +
                    " , Expected : " + PERL_TOTAL_RECORDS);
        }
        if (logger.totalPrintCnt.get() != PERL_TOTAL_RECORDS) {
            fail("Total Print Count Failed! Latency Reporter Count : " + logger.totalPrintCnt.get() +
                    " , Expected : " + PERL_TOTAL_RECORDS);
        }
    }

    @Test
    public void testPerlRecordsIdleNS() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        TestLogger logger = new TestLogger();
        Perl perl = PerlBuilder.build(logger, null, null, null);
        runPerlRecords(logger, perl);
    }

    @Test
    public void testPerlRecordsSleepMS() throws IOException, ExecutionException,
            InterruptedException, TimeoutException {
        TestLogger logger = new TestLogger();
        PerlConfig config = PerlConfig.build();
        config.sleepMS = PERL_SLEEP_MS;
        Perl perl = PerlBuilder.build(logger, null, config, null);
        runPerlRecords(logger, perl);
    }

    /**
     * Verifies record processing through the JDK concurrent-queue fallback.
     *
     * @throws IOException if PerL configuration cannot be loaded
     * @throws ExecutionException if asynchronous recording fails
     * @throws InterruptedException if the test thread is interrupted
     * @throws TimeoutException if recording does not finish in time
     */
    @Test
    public void testJdkConcurrentQueueFallback() throws IOException,
            ExecutionException, InterruptedException, TimeoutException {
        final TestLogger logger = new TestLogger();
        final PerlConfig config = PerlConfig.build();
        config.mpscQueueEnable = false;
        final Perl perl = PerlBuilder.build(logger, null, config, null);

        runPerlRecords(logger, perl);
    }

    /**
     * Verifies the default and explicit fallback values of
     * {@code MpscQueueEnable}.
     *
     * @throws IOException if the supplied properties cannot be loaded
     */
    @Test
    public void testMpscQueueEnablePropertyBinding() throws IOException {
        assertTrue(PerlConfig.build().mpscQueueEnable,
                "The optimized MPSC queue should be enabled by default");

        final byte[] disabled =
                "MpscQueueEnable=false\n".getBytes(StandardCharsets.UTF_8);
        final PerlConfig fallback = PerlConfig.build(
                new ByteArrayInputStream(disabled));
        assertFalse(fallback.mpscQueueEnable,
                "The property must select the JDK queue fallback");
        assertEquals("TimeStampMpscQueue (MPSC)",
                PerlConfig.build().getTimestampQueueName());
        assertEquals("ConcurrentLinkedQueue (JDK)",
                fallback.getTimestampQueueName());
    }

    @Test
    public void testNullLoggerThrowsException() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            PerlBuilder.build(null, null, null, null);
        });
    }

    @Test
    public void testMismatchedTimeUnitThrowsException() throws Exception {
        TestLogger logger = new TestLogger();
        Time wrongTime = new MicroSeconds();
        assertThrows(IllegalArgumentException.class, () -> {
            PerlBuilder.build(logger, wrongTime, null, null);
        });
    }

    @Test
    public void testCustomExecutorService() throws Exception {
        TestLogger logger = new TestLogger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Perl perl = PerlBuilder.build(logger, null, null, executor);
        runPerlRecords(logger, perl);
        executor.shutdown();
    }

    @Test
    public void testDifferentTimeUnits() throws Exception {
        TestLogger logger = new TestLogger();
        assertThrows(IllegalArgumentException.class, () -> {
            PerlBuilder.build(logger, new NanoSeconds(), null, null);
        });
    }

    @Test
    public void testZeroRecords() throws Exception {
        TestLogger logger = new TestLogger();
        Perl perl = PerlBuilder.build(logger, null, null, null);
        PerlChannel channel = perl.getPerlChannel();
        CompletableFuture<Void> ret = perl.run(0, 0);
        channel.send(0, 0, 0, 0);
        assertThrows(TimeoutException.class, () -> {
            ret.get(PERL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        });
        assertEquals(0, logger.latencyReporterCnt.get());
        perl.stop();
    }

    /** Verifies idle termination with the default elastic-wait consumer. */
    @Test
    public void testElasticWaitIdleTimeout() throws Exception {
        assertIdleTimeout(0);
    }

    /** Verifies idle termination with the configured sleeping consumer. */
    @Test
    public void testIdleSleepIdleTimeout() throws Exception {
        assertIdleTimeout(10);
    }

    /** Verifies that a zero-record timestamp does not count as benchmark progress. */
    @Test
    public void testZeroRecordTimestampDoesNotRenewIdleTimeout() throws Exception {
        final PerlConfig config = PerlConfig.build();
        config.idleTimeoutSeconds = 1;
        final Perl perl = PerlBuilder.build(new TestLogger(), null, config, null);
        final PerlChannel channel = perl.getPerlChannel();
        final CompletableFuture<Void> completion = perl.run(0, Long.MAX_VALUE);
        final long now = System.currentTimeMillis();
        channel.send(now, now, 0, 0);

        final ExecutionException failure = assertThrows(ExecutionException.class,
                () -> completion.get(4, TimeUnit.SECONDS));

        assertInstanceOf(BenchmarkIdleTimeoutException.class, failure.getCause());
    }

    /** Verifies that the fixed-record idle deadline is disabled for timed runs. */
    @Test
    public void testTimedRunDoesNotApplyIdleTimeout() throws Exception {
        final PerlConfig config = PerlConfig.build();
        config.idleTimeoutSeconds = 1;
        final Perl perl = PerlBuilder.build(new TestLogger(), null, config, null);

        perl.run(2, Long.MAX_VALUE).get(4, TimeUnit.SECONDS);
    }

    private void assertIdleTimeout(int sleepMS) throws Exception {
        final PerlConfig config = PerlConfig.build();
        config.sleepMS = sleepMS;
        config.idleTimeoutSeconds = 1;
        final Perl perl = PerlBuilder.build(new TestLogger(), null, config, null);

        final ExecutionException failure = assertThrows(ExecutionException.class,
                () -> perl.run(0, Long.MAX_VALUE).get(4, TimeUnit.SECONDS));

        assertInstanceOf(BenchmarkIdleTimeoutException.class, failure.getCause());
        assertEquals("No performance benchmarking event was received for 1 seconds",
                failure.getCause().getMessage());
    }

    @Test
    public void testHistogramAndCsvConfig() throws Exception {
        PerlConfig config = PerlConfig.build();
        TestLogger logger = new TestLogger();
        config.histogram = true;
        Perl perl = PerlBuilder.build(logger, null, config, null);
        runPerlRecords(logger, perl);

        TestLogger logger1 = new TestLogger();
        config.histogram = false;
        config.csv = true;
        Perl perlCsv = PerlBuilder.build(logger1, null, config, null);
        runPerlRecords(logger, perlCsv);
    }
}

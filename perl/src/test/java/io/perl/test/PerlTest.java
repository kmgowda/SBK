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
import io.perl.logger.impl.DefaultLogger;
import io.perl.system.PerlPrinter;
import io.time.MicroSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
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

    public static class TestLogger extends DefaultLogger {
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
                          long slc1, long slc2, long[] percentileValues) {
            super.print(seconds, bytes, records, recsPerSec, mbPerSec,
                    avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
            PerlPrinter.log.info("print : receiving records " + records);
            printCnt.addAndGet(records);
        }

        @Override
        public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                               double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                               long higherDiscard, long slc1, long slc2, long[] percentileValues) {
            super.printTotal(seconds, bytes, records, recsPerSec, mbPerSec,
                    avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2,
                    percentileValues);
            PerlPrinter.log.info("printTotal : receiving records " + records);
            totalPrintCnt.addAndGet(records);
        }

        @Override
        public void recordLatency(long startTime, int events, int bytes, long latency) {
            PerlPrinter.log.info("recordLatency : receiving records " + events);
            latencyReporterCnt.addAndGet(events);
        }
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
            CompletableFuture.runAsync(() -> channels[finalCh].send(finalCh, PERL_THREADS + finalCh,
                    Math.min(finalRecords, PERL_RECORDS_PER_THREAD), PERL_RECORD_SIZE));
            records -= PERL_RECORDS_PER_THREAD;
        }
        ret.get(PERL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (logger.latencyReporterCnt.get() != PERL_TOTAL_RECORDS) {
            Assert.fail("Latency Reporter Count Failed! Latency Reporter Count : " + logger.latencyReporterCnt.get() +
                    " , Expected : " + PERL_TOTAL_RECORDS);
        }
        if (logger.printCnt.get() != PERL_TOTAL_RECORDS) {
            Assert.fail("Print Count Failed! Latency Reporter Count : " + logger.printCnt.get() +
                    " , Expected : " + PERL_TOTAL_RECORDS);
        }
        if (logger.totalPrintCnt.get() != PERL_TOTAL_RECORDS) {
            Assert.fail("Total Print Count Failed! Latency Reporter Count : " + logger.totalPrintCnt.get() +
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

    @Test(expected = IllegalArgumentException.class)
    public void testNullLoggerThrowsException() throws Exception {
        PerlBuilder.build(null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMismatchedTimeUnitThrowsException() throws Exception {
        TestLogger logger = new TestLogger();
        Time wrongTime = new MicroSeconds();
        PerlBuilder.build(logger, wrongTime, null, null);
    }

    @Test
    public void testCustomExecutorService() throws Exception {
        TestLogger logger = new TestLogger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Perl perl = PerlBuilder.build(logger, null, null, executor);
        runPerlRecords(logger, perl);
        executor.shutdown();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDifferentTimeUnits() throws Exception {
        TestLogger logger = new TestLogger();
        Perl perlNs = PerlBuilder.build(logger, new NanoSeconds(), null, null);
        runPerlRecords(logger, perlNs);
    }

    @Test(expected = TimeoutException.class)
    public void testZeroRecords() throws Exception {
        TestLogger logger = new TestLogger();
        Perl perl = PerlBuilder.build(logger, null, null, null);
        PerlChannel channel = perl.getPerlChannel();
        CompletableFuture<Void> ret = perl.run(0, 0);
        channel.send(0, 0, 0, 0);
        ret.get(PERL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertEquals(0, logger.latencyReporterCnt.get());
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

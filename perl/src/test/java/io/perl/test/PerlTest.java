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
import io.perl.logger.impl.DefaultLogger;
import io.perl.system.PerlPrinter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class for PerL validation.
 */
public class PerlTest  {
    public final static int PERL_THREADS = 2;
    public final static int PERL_TOTAL_RECORDS = 100;
    public final static int PERL_RECORDS_PER_THREAD = PERL_TOTAL_RECORDS / PERL_THREADS;
    public final static int PERL_RECORD_SIZE = 10;
    public final static int PERL_TIMEOUT_SECONDS = 5;

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
                          double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                          long slc1, long slc2, long[] percentiles) {
            super.print(seconds, bytes, records, recsPerSec, mbPerSec,
                    avgLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentiles);
            PerlPrinter.log.info("print : receiving records " + records);
            printCnt.addAndGet(records);
        }

        @Override
        public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                               double avgLatency, long maxLatency, long invalid, long lowerDiscard,
                               long higherDiscard, long slc1, long slc2, long[] percentiles) {
            super.printTotal(seconds, bytes, records, recsPerSec, mbPerSec,
                    avgLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentiles);
            PerlPrinter.log.info("printTotal : receiving records " + records);
            totalPrintCnt.addAndGet(records);
        }

        @Override
        public void recordLatency(long startTime, int bytes, int events, long latency) {
            PerlPrinter.log.info("recordLatency : receiving records " + events);
            latencyReporterCnt.addAndGet(events);
        }
    }

    @Test
    public void testPerlRecords() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        TestLogger logger = new TestLogger();
        Perl perl = PerlBuilder.build(null, logger, logger, null, null);
        PerlChannel[] channels = new PerlChannel[PERL_THREADS];
        for (int i = 0; i < PERL_THREADS; i++) {
            channels[i] = perl.getPerlChannel();
        }
        CompletableFuture<Void>  ret = perl.run(0, PERL_TOTAL_RECORDS);

        int records = PERL_TOTAL_RECORDS;
        int ch = 0;
        while (records > 0) {
            if (ch > PERL_THREADS) {
                ch = 0;
            }
            int finalCh = ch++;
            int finalRecords = records;
            CompletableFuture.runAsync(() -> channels[finalCh].send(finalCh, PERL_THREADS + finalCh,
                    PERL_RECORD_SIZE, Math.min(finalRecords, PERL_RECORDS_PER_THREAD)));
            records -= PERL_RECORDS_PER_THREAD;
        }
        ret.get(PERL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (logger.latencyReporterCnt.get() != PERL_TOTAL_RECORDS) {
            Assert.fail("Latency Reporter Count Failed! Latency Reporter Count : "+ logger.latencyReporterCnt.get() +
            " , Expected : " + PERL_TOTAL_RECORDS);
        }
        if (logger.printCnt.get() != PERL_TOTAL_RECORDS) {
            Assert.fail("Print Count Failed! Latency Reporter Count : "+ logger.latencyReporterCnt.get() +
                    " , Expected : " + PERL_TOTAL_RECORDS);
        }
        if (logger.totalPrintCnt.get() != PERL_TOTAL_RECORDS) {
            Assert.fail("Total Print Count Failed! Latency Reporter Count : " + logger.latencyReporterCnt.get() +
                    " , Expected : " + PERL_TOTAL_RECORDS);
        }
    }
}

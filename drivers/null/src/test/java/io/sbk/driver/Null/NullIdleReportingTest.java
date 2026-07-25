/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.Null;

import io.perl.api.Perl;
import io.perl.api.impl.PerlBuilder;
import io.perl.logger.impl.DefaultLogger;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies time-driven reporting when the Null driver completes no writes.
 */
public class NullIdleReportingTest {
    private static final int REPORTING_INTERVAL_SECONDS = 1;
    private static final int RUN_SECONDS = 3;

    /**
     * A default Null write remains incomplete, yet PerL must print a zero-value
     * result at every configured reporting interval.
     *
     * @throws Exception if the timed PerL run cannot complete
     */
    @Test
    public void incompleteWritesStillPrintEveryReportingInterval() throws Exception {
        final NullWriter writer = new NullWriter(Integer.MAX_VALUE, 0);
        final CompletableFuture<?> incompleteWrite = writer.writeAsync(new byte[1]);
        final CountingLogger logger = new CountingLogger();
        final Perl perl = PerlBuilder.build(logger, null, null, null);

        try {
            assertFalse(incompleteWrite.isDone(),
                    "The default Null writer must not produce a completed timestamp");
            perl.run(RUN_SECONDS, 0).get(RUN_SECONDS + 5L, TimeUnit.SECONDS);

            assertEquals(RUN_SECONDS, logger.windowPrints.get(),
                    "PerL must print one empty result per configured interval");
            assertEquals(0, logger.windowRecords.get(),
                    "An incomplete Null write must not be counted as completed");
            assertEquals(0, logger.totalRecords.get(),
                    "The total must remain empty when no Null write completes");
        } finally {
            incompleteWrite.cancel(false);
            writer.close();
        }
    }

    private static final class CountingLogger extends DefaultLogger {
        private final AtomicInteger windowPrints = new AtomicInteger();
        private final AtomicInteger windowRecords = new AtomicInteger();
        private final AtomicInteger totalRecords = new AtomicInteger();

        @Override
        public int getPrintingIntervalSeconds() {
            return REPORTING_INTERVAL_SECONDS;
        }

        @Override
        public void print(double seconds, long bytes, long records,
                          double recordsPerSecond, double megabytesPerSecond,
                          double averageLatency, long minimumLatency,
                          long maximumLatency, long invalidLatencies,
                          long lowerDiscard, long higherDiscard, long slc1,
                          long slc2, long[] percentileLatencies,
                          long[] percentileLatencyCounts) {
            windowPrints.incrementAndGet();
            windowRecords.addAndGet(Math.toIntExact(records));
        }

        @Override
        public void printTotal(double seconds, long bytes, long records,
                               double recordsPerSecond,
                               double megabytesPerSecond,
                               double averageLatency, long minimumLatency,
                               long maximumLatency, long invalidLatencies,
                               long lowerDiscard, long higherDiscard, long slc1,
                               long slc2, long[] percentileLatencies,
                               long[] percentileLatencyCounts) {
            totalRecords.addAndGet(Math.toIntExact(records));
        }
    }
}

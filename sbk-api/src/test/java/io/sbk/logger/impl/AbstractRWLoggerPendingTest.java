/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import io.perl.data.Bytes;
import io.sbk.action.Action;
import io.sbk.params.impl.SbkInputOptions;
import io.time.MilliSeconds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests request/response pending-result formatting. */
final class AbstractRWLoggerPendingTest {

    /** Keeps read pending records and bytes in their declared positions. */
    @Test
    void formatsReadPendingRecordsAndBytesInOrder() {
        final PendingFormatter formatter = new PendingFormatter();
        final String result = formatter.format(11, 2L * Bytes.BYTES_PER_MB);

        assertTrue(result.contains("2.00 read response pending MB"));
        assertTrue(result.contains("11 read response pending records"));
    }

    /** Writing results must not be subtracted from an unrelated read-request stream. */
    @Test
    void writingDoesNotCreateNegativeReadPendingResults() throws Exception {
        final CapturingLogger logger = openLogger(Action.Writing);
        logger.recordWriteRequests(0, 0, 70, 7);

        logger.print(1, 50, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                new long[0], new long[0]);

        assertEquals(2, logger.writePendingRecords);
        assertEquals(20, logger.writePendingBytes);
        assertEquals(0, logger.readPendingRecords);
        assertEquals(0, logger.readPendingBytes);
    }

    /** Reading results must not be subtracted from an unrelated write-request stream. */
    @Test
    void readingDoesNotCreateNegativeWritePendingResults() throws Exception {
        final CapturingLogger logger = openLogger(Action.Reading);
        logger.recordReadRequests(0, 0, 110, 11);

        logger.print(1, 50, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                new long[0], new long[0]);

        assertEquals(0, logger.writePendingRecords);
        assertEquals(0, logger.writePendingBytes);
        assertEquals(6, logger.readPendingRecords);
        assertEquals(60, logger.readPendingBytes);
    }

    /** End-to-end write/read results are attributed to both request streams. */
    @Test
    void combinedActionAttributesCompletionsToBothRequestStreams() throws Exception {
        final CapturingLogger logger = openLogger(Action.Write_Reading);
        logger.recordWriteRequests(0, 0, 70, 7);
        logger.recordReadRequests(0, 0, 110, 11);

        logger.print(1, 50, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                new long[0], new long[0]);

        assertEquals(2, logger.writePendingRecords);
        assertEquals(20, logger.writePendingBytes);
        assertEquals(6, logger.readPendingRecords);
        assertEquals(60, logger.readPendingBytes);
    }

    /** Final totals use the same action attribution as periodic windows. */
    @Test
    void totalReadingDoesNotCreateNegativeWritePendingResults() throws Exception {
        final CapturingLogger logger = openLogger(Action.Reading);
        logger.recordReadRequests(0, 0, 110, 11);

        logger.printTotal(1, 50, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                new long[0], new long[0]);

        assertEquals(0, logger.writePendingRecords);
        assertEquals(0, logger.writePendingBytes);
        assertEquals(6, logger.readPendingRecords);
        assertEquals(60, logger.readPendingBytes);
    }

    private static CapturingLogger openLogger(Action action) throws Exception {
        final CapturingLogger logger = new CapturingLogger();
        final SbkInputOptions options = new SbkInputOptions("pending-test", "pending-test");
        options.addOption("writers", true, "writers");
        options.addOption("readers", true, "readers");
        logger.addArgs(options);
        options.parseArgs(new String[]{"-writers", "1", "-readers", "1", "-wq", "true", "-rq", "true"});
        logger.parseArgs(options);
        logger.open(options, "test", action, new MilliSeconds());
        return logger;
    }

    private static final class PendingFormatter extends SystemLogger {
        private String format(long readRecords, long readBytes) {
            final StringBuilder output = new StringBuilder();
            appendWriteAndReadRequestsPending(output, 0, 0, readRecords, readBytes, 0, 0);
            return output.toString();
        }
    }

    private static final class CapturingLogger extends AbstractRWLogger {
        private long writePendingRecords;
        private long writePendingBytes;
        private long readPendingRecords;
        private long readPendingBytes;

        @Override
        public void recordLatency(long startTime, int events, int bytes, long latency) {
        }

        @Override
        public boolean recordsIndividualLatencies() {
            return false;
        }

        @Override
        public void print(int writers, int maxWriters, int readers, int maxReaders,
                          long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                          double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                          long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                          long writeResponsePendingBytes, long readResponsePendingRecords,
                          long readResponsePendingBytes, long writeReadRequestPendingRecords,
                          long writeReadRequestPendingBytes, long writeTimeoutEvents,
                          double writeTimeoutEventsPerSec, long readTimeoutEvents,
                          double readTimeoutEventsPerSec, double seconds, long bytes, long records,
                          double recsPerSec, double mbPerSec, double avgLatency, long minLatency,
                          long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                          long slc1, long slc2, long[] percentileLatencies,
                          long[] percentileLatencyCounts) {
            this.writePendingRecords = writeResponsePendingRecords;
            this.writePendingBytes = writeResponsePendingBytes;
            this.readPendingRecords = readResponsePendingRecords;
            this.readPendingBytes = readResponsePendingBytes;
        }

        @Override
        public void printTotal(int writers, int maxWriters, int readers, int maxReaders,
                               long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                               double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                               long readRequestRecords, double readRequestRecordsPerSec,
                               long writeResponsePendingRecords, long writeResponsePendingBytes,
                               long readResponsePendingRecords, long readResponsePendingBytes,
                               long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                               long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                               long readTimeoutEvents, double readTimeoutEventsPerSec, double seconds,
                               long bytes, long records, double recsPerSec, double mbPerSec,
                               double avgLatency, long minLatency, long maxLatency, long invalid,
                               long lowerDiscard, long higherDiscard, long slc1, long slc2,
                               long[] percentileLatencies, long[] percentileLatencyCounts) {
            this.writePendingRecords = writeResponsePendingRecords;
            this.writePendingBytes = writeResponsePendingBytes;
            this.readPendingRecords = readResponsePendingRecords;
            this.readPendingBytes = readResponsePendingBytes;
        }
    }
}

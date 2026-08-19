/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import io.gem.api.RemoteResponse;
import io.gem.api.RemoteExecutionStatus;
import io.gem.api.SshCommandException;
import io.gem.api.SshResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests distributed result classification independent of SSH transport.
 */
final class SbkGemBenchmarkTest {
    @Test
    void acceptsOnlyWhenEveryRemoteSbkSucceeds() {
        final RemoteResponse[] results = {new RemoteResponse(0, "", "", "node-a"),
                new RemoteResponse(0, "", "", "node-b")};

        assertNull(SbkGemBenchmark.remoteCommandFailure(results));
    }

    @Test
    void reportsEveryFailedRemoteSbk() {
        final RemoteResponse[] results = {new RemoteResponse(2, "", "bad option", "node-a"),
                new RemoteResponse(0, "", "", "node-b"), new RemoteResponse(17, "", "failure", "node-c")};

        final IOException failure = SbkGemBenchmark.remoteCommandFailure(results);
        assertTrue(failure.getMessage().contains("node-a status EXIT_FAILURE returned 2"));
        assertTrue(failure.getMessage().contains("node-c status EXIT_FAILURE returned 17"));
        assertTrue(failure.getMessage().contains("bad option"));
        assertTrue(failure.getMessage().contains("failure"));
    }

    @Test
    void includesRemoteSbkExceptionInAggregateFailure() {
        final RemoteResponse[] results = {new RemoteResponse(1, "",
                "java.util.concurrent.ExecutionException: java.io.IOException: HTTP 503 Service Unavailable\n"
                        + "\tat io.sbk.Sbk.run(Sbk.java:117)", "node-a")};

        final IOException failure = SbkGemBenchmark.remoteCommandFailure(results);

        assertTrue(failure.getMessage().contains("HTTP 503 Service Unavailable"));
        assertTrue(failure.getMessage().contains("node-a status EXIT_FAILURE returned 1"));
    }

    @Test
    void preservesRemoteExceptionHeadAndRootCauseTailWithinTheDiagnosticLimit() {
        final String exceptionHead = "java.util.concurrent.ExecutionException: MinIO PUT failed ";
        final String rootCauseTail = "Caused by: HTTP 503 Service Unavailable";
        final String stderr = exceptionHead + "stack-frame ".repeat(100) + rootCauseTail;

        final String summary = SbkGemBenchmark.diagnosticSummary(stderr);

        assertEquals(512, summary.length());
        assertTrue(summary.startsWith(exceptionHead));
        assertTrue(summary.contains(" ... [truncated] ... "));
        assertTrue(summary.endsWith(rootCauseTail));
    }

    @Test
    void retainsNormalizedRemoteDiagnosticAtTheExactBoundary() {
        final String stderr = "x".repeat(512);

        final String summary = SbkGemBenchmark.diagnosticSummary(stderr);

        assertEquals(512, summary.length());
        assertEquals(stderr, summary);
    }

    @Test
    void includesHostInDerivedExitFailureMessage() {
        final RemoteResponse response = new RemoteResponse(2, "", "bad option", "node-a");

        assertEquals("Host 'node-a' remote process returned exit code 2", response.failureMessage);
    }

    @Test
    void classifiesTransportFailureWithHostAndBoundedDiagnostics() throws IOException {
        final SshResponse partial = new SshResponse(true, 32);
        partial.stdOutputStream.write("startup output".getBytes(StandardCharsets.UTF_8));
        partial.errOutputStream.write("connection reset".getBytes(StandardCharsets.UTF_8));
        final SshCommandException exception = new SshCommandException("node-a", partial, false,
                new IOException("SSH channel closed"));

        final RemoteResponse result = SbkGemBenchmark.remoteCommandResult("node-a", null, exception);

        assertEquals(RemoteExecutionStatus.SSH_ERROR, result.status);
        assertEquals(RemoteResponse.UNKNOWN_RETURN_CODE, result.returnCode);
        assertEquals("startup output", result.stdOutput);
        assertEquals("connection reset", result.errOutput);
        assertTrue(result.failureMessage.contains("node-a"));
    }

    @Test
    void classifiesTimeoutWithoutIncludingTheRemoteCommand() {
        final SshCommandException exception = new SshCommandException("node-b", new SshResponse(true), true,
                new SocketTimeoutException("Remote command timed out after 10 seconds"));

        final RemoteResponse result = SbkGemBenchmark.remoteCommandResult("node-b", null, exception);

        assertEquals(RemoteExecutionStatus.TIMEOUT, result.status);
        assertTrue(result.failureMessage.contains("node-b"));
        assertTrue(result.failureMessage.contains("timed out"));
    }

    @Test
    void aggregatesMixedExitAndTransportFailures() {
        final RemoteResponse[] results = {
                new RemoteResponse(0, "", "", "node-a"),
                new RemoteResponse(7, "", "driver failed", "node-b"),
                new RemoteResponse(RemoteResponse.UNKNOWN_RETURN_CODE, "", "", "node-c",
                        RemoteExecutionStatus.TIMEOUT, "node-c timed out")
        };

        final IOException failure = SbkGemBenchmark.remoteCommandFailure(results);

        assertTrue(failure.getMessage().contains("node-b status EXIT_FAILURE returned 7"));
        assertTrue(failure.getMessage().contains("node-c status TIMEOUT"));
    }

    @Test
    void classifiesCancelledRemoteCommand() {
        final RemoteResponse result = SbkGemBenchmark.remoteCommandResult("node-d", null,
                new CancellationException("cancelled after peer failure"));

        assertEquals(RemoteExecutionStatus.CANCELLED, result.status);
        assertTrue(result.failureMessage.contains("node-d"));
    }

    @Test
    void marksPartialOrMissingSbmParticipationAsIncomplete() {
        final RemoteResponse[] mixed = {new RemoteResponse(0, "", "", "node-a"),
                new RemoteResponse(9, "", "", "node-b")};
        final RemoteResponse[] successful = {new RemoteResponse(0, "", "", "node-a"),
                new RemoteResponse(0, "", "", "node-b")};

        assertEquals("INCOMPLETE", SbkGem.distributedRunStatus(mixed, 2));
        assertEquals("INCOMPLETE", SbkGem.distributedRunStatus(successful, 1));
        assertEquals("SUCCESS", SbkGem.distributedRunStatus(successful, 2));
    }

    @Test
    void failsDistributedRunWhenOnlySbmReportsFailure() {
        final IOException sbmFailure = new IOException("SBM client reported terminal failure");

        assertSame(sbmFailure, SbkGemBenchmark.combineTerminalFailures(null, sbmFailure));
    }

    @Test
    void keepsRemoteFailurePrimaryAndAttachesSbmFailure() {
        final IOException remoteFailure = new IOException("remote command failed");
        final IOException sbmFailure = new IOException("SBM client reported terminal failure");

        final Throwable failure = SbkGemBenchmark.combineTerminalFailures(remoteFailure, sbmFailure);

        assertSame(remoteFailure, failure);
        assertEquals(1, failure.getSuppressed().length);
        assertSame(sbmFailure, failure.getSuppressed()[0]);
    }

    @Test
    void distributesFixedRecordsAcrossNodesIncludingRemainder() {
        final List<List<String>> arguments = SbkGem.distributeTotalRecords(
                List.of("-class", "file"), 1001, 2, 1, false);

        assertEquals(List.of("-class", "file", "-records", "501"), arguments.get(0));
        assertEquals(List.of("-class", "file", "-records", "500"), arguments.get(1));
    }

    @Test
    void distributesExactAggregateRateInWholeWorkerUnits() {
        final List<List<String>> arguments = SbkGem.distributeTotalRecords(
                List.of("-class", "file", "-seconds", "30"), 1000, 3, 2, true);

        assertEquals("334", arguments.get(0).getLast());
        assertEquals("334", arguments.get(1).getLast());
        assertEquals("332", arguments.get(2).getLast());
        assertEquals(1000, arguments.stream().mapToLong(values -> Long.parseLong(values.getLast())).sum());
    }

    @Test
    void doesNotModifyCommonArgumentsDuringDistribution() {
        final List<String> commonArguments = List.of("-class", "file");

        SbkGem.distributeTotalRecords(commonArguments, 10, 2, 1, false);

        assertEquals(List.of("-class", "file"), commonArguments);
    }

    @Test
    void rejectsPerWorkerRateBeyondSbkIntegerLimit() {
        final long excessiveRate = (long) Integer.MAX_VALUE + 1;

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SbkGem.distributeTotalRecords(List.of("-class", "file"), excessiveRate, 1, 1, true));

        assertTrue(exception.getMessage().contains("exceeds"));
    }

    @Test
    void distributesAggregateThroughputAcrossNodes() {
        final List<List<String>> arguments = SbkGem.distributeTotalThroughput(
                List.of(List.of("-class", "file"), List.of("-class", "file")),
                new BigDecimal("10"), 4096, 1);

        assertEquals(List.of("-class", "file", "-throughput", "5"), arguments.get(0));
        assertEquals(List.of("-class", "file", "-throughput", "5"), arguments.get(1));
    }

    @Test
    void preservesExactDecimalTotalWhenThroughputDoesNotDivideEvenly() {
        final List<List<String>> arguments = SbkGem.distributeTotalThroughput(
                List.of(List.of(), List.of(), List.of()), new BigDecimal("10"), 4096, 1);

        final BigDecimal distributedTotal = arguments.stream()
                .map(values -> new BigDecimal(values.getLast()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("10.000000000000"), distributedTotal);
        assertEquals("3.333333333334", arguments.get(0).getLast());
        assertEquals("3.333333333333", arguments.get(1).getLast());
        assertEquals("3.333333333333", arguments.get(2).getLast());
    }

    @Test
    void retainsExistingNodeSpecificArgumentsWhenAddingThroughput() {
        final List<List<String>> source = List.of(
                List.of("-records", "51"), List.of("-records", "50"));

        final List<List<String>> arguments = SbkGem.distributeTotalThroughput(
                source, new BigDecimal("20"), 4096, 1);

        assertEquals(List.of("-records", "51", "-throughput", "10"), arguments.get(0));
        assertEquals(List.of("-records", "50", "-throughput", "10"), arguments.get(1));
        assertEquals(List.of("-records", "51"), source.get(0));
    }

    @Test
    void rejectsAggregateThroughputThatDisablesSbkRateControl() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SbkGem.distributeTotalThroughput(List.of(List.of(), List.of()),
                        new BigDecimal("0.001"), 1048576, 1));

        assertTrue(exception.getMessage().contains("at least one record/second"));
    }

    @Test
    void rejectsAggregateThroughputBeyondSbkIntegerRateLimit() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SbkGem.distributeTotalThroughput(List.of(List.of()),
                        new BigDecimal("1000000000"), 1, 1));

        assertTrue(exception.getMessage().contains("maximum record/second rate"));
    }
}

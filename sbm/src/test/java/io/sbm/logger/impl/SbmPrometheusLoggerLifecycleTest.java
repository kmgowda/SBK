/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.logger.impl;

import io.sbk.action.Action;
import io.sbk.params.ParseInputOptions;
import io.sbk.params.impl.SbkInputOptions;
import io.time.MilliSeconds;
import io.time.Time;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Verifies rollback and cleanup of SBM Prometheus logger resources. */
final class SbmPrometheusLoggerLifecycleTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void failedServerStartRollsBackCsvAndPreservesFailure() throws Exception {
        final SbmPrometheusServer server = mock(SbmPrometheusServer.class);
        final IOException startFailure = new IOException("start failed");
        final IOException stopFailure = new IOException("stop failed");
        doThrow(startFailure).when(server).start();
        doThrow(stopFailure).when(server).stop();
        final TestSbmPrometheusLogger logger = new TestSbmPrometheusLogger(server);
        final Path csvFile = temporaryDirectory.resolve("start-failure.csv");
        final ParseInputOptions options = configure(logger, csvFile);

        final IOException thrown = assertThrows(IOException.class,
                () -> logger.open(options, "File", Action.Reading, new MilliSeconds()));

        assertSame(startFailure, thrown);
        assertSame(stopFailure, thrown.getSuppressed()[0]);
        verify(server).stop();
        assertTrue(logger.isCsvClosed());
    }

    @Test
    void failedServerStopStillClosesCsvAndPreservesFailure() throws Exception {
        final SbmPrometheusServer server = mock(SbmPrometheusServer.class);
        final IOException stopFailure = new IOException("stop failed");
        doThrow(stopFailure).when(server).stop();
        final TestSbmPrometheusLogger logger = new TestSbmPrometheusLogger(server);
        final Path csvFile = temporaryDirectory.resolve("stop-failure.csv");
        final ParseInputOptions options = configure(logger, csvFile);
        logger.open(options, "File", Action.Reading, new MilliSeconds());

        final IOException thrown = assertThrows(IOException.class, () -> logger.close(options));

        assertSame(stopFailure, thrown);
        assertTrue(logger.isCsvClosed());
    }

    @Test
    void identicalStartAndStopFailureDoesNotInterruptRollback() throws Exception {
        final SbmPrometheusServer server = mock(SbmPrometheusServer.class);
        final IOException failure = new IOException("shared failure");
        doThrow(failure).when(server).start();
        doThrow(failure).when(server).stop();
        final TestSbmPrometheusLogger logger = new TestSbmPrometheusLogger(server);
        final Path csvFile = temporaryDirectory.resolve("shared-failure.csv");
        final ParseInputOptions options = configure(logger, csvFile);

        final IOException thrown = assertThrows(IOException.class,
                () -> logger.open(options, "File", Action.Reading, new MilliSeconds()));

        assertSame(failure, thrown);
        assertTrue(logger.isCsvClosed());
    }

    private static ParseInputOptions configure(SbmPrometheusLogger logger, Path csvFile) throws Exception {
        final ParseInputOptions options = new SbkInputOptions("test", "test");
        logger.addArgs(options);
        options.parseArgs(new String[]{"-csvfile", csvFile.toString(), "-context", "0/metrics"});
        logger.parseArgs(options);
        return options;
    }

    private static final class TestSbmPrometheusLogger extends SbmPrometheusLogger {
        private final SbmPrometheusServer server;

        private TestSbmPrometheusLogger(SbmPrometheusServer server) {
            this.server = server;
        }

        @Override
        protected SbmPrometheusServer createPrometheusServer(String storageName, Action action, Time time) {
            return server;
        }

        private boolean isCsvClosed() {
            return isCsvWriterClosed();
        }
    }
}

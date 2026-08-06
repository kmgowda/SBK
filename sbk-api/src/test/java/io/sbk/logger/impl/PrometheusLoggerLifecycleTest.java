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

import io.sbk.action.Action;
import io.sbk.params.ParseInputOptions;
import io.sbk.params.impl.SbkInputOptions;
import io.time.MilliSeconds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Verifies rollback and cleanup of SBK Prometheus logger resources. */
final class PrometheusLoggerLifecycleTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void failedServerStartRollsBackCsvAndPreservesFailure() throws Exception {
        final SbkPrometheusServer server = mock(SbkPrometheusServer.class);
        final IOException startFailure = new IOException("start failed");
        final IOException stopFailure = new IOException("stop failed");
        doThrow(startFailure).when(server).start();
        doThrow(stopFailure).when(server).stop();
        final TestPrometheusLogger logger = new TestPrometheusLogger(server);
        final Path csvFile = temporaryDirectory.resolve("start-failure.csv");
        final ParseInputOptions options = configure(logger, csvFile);

        final IOException thrown = assertThrows(IOException.class,
                () -> logger.open(options, "File", Action.Writing, new MilliSeconds()));

        assertSame(startFailure, thrown);
        assertSame(stopFailure, thrown.getSuppressed()[0]);
        verify(server).stop();
        assertFalse(Files.readString(csvFile).isEmpty());
    }

    @Test
    void failedServerStopStillClosesCsvAndPreservesFailure() throws Exception {
        final SbkPrometheusServer server = mock(SbkPrometheusServer.class);
        final IOException stopFailure = new IOException("stop failed");
        doThrow(stopFailure).when(server).stop();
        final TestPrometheusLogger logger = new TestPrometheusLogger(server);
        final Path csvFile = temporaryDirectory.resolve("stop-failure.csv");
        final ParseInputOptions options = configure(logger, csvFile);
        logger.open(options, "File", Action.Writing, new MilliSeconds());

        final IOException thrown = assertThrows(IOException.class, () -> logger.close(options));

        assertSame(stopFailure, thrown);
        assertFalse(Files.readString(csvFile).isEmpty());
    }

    private static ParseInputOptions configure(PrometheusLogger logger, Path csvFile) throws Exception {
        final ParseInputOptions options = new SbkInputOptions("test", "test");
        logger.addArgs(options);
        options.parseArgs(new String[]{"-csvfile", csvFile.toString(), "-context", "0/metrics"});
        logger.parseArgs(options);
        return options;
    }

    private static final class TestPrometheusLogger extends PrometheusLogger {
        private final SbkPrometheusServer server;

        private TestPrometheusLogger(SbkPrometheusServer server) {
            this.server = server;
        }

        @Override
        public SbkPrometheusServer getPrometheusRWMetricsServer() {
            return server;
        }
    }
}

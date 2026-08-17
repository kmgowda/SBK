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
import io.sbk.params.impl.SbkParameters;
import io.sbk.webconsole.WebConsoleServer;
import io.sbk.webconsole.WebConsoleSnapshot;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the SBK WebLogger adapter and independent Local Web Console runtime.
 */
final class WebLoggerWebConsoleTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Test
    void webLoggerRejectsNonPositiveIdleTimeout() throws Exception {
        final WebLogger logger = new WebLogger();
        final SbkParameters parameters = new SbkParameters("web-console-timeout-test");
        logger.addArgs(parameters);
        parameters.parseArgs(new String[]{"-writers", "1", "-size", "100", "-webtimeoutminutes", "0"});

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> logger.parseArgs(parameters));
        assertEquals("Local Web Console idle timeout minutes must be greater than zero", exception.getMessage());
    }

    @Test
    void webLoggerPublishesOnlyRegularIntervalResults() throws Exception {
        try (WebConsoleServer server = new WebConsoleServer(0, 4)) {
            server.start();
            final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            final WebLogger logger = new WebLogger();
            final SbkParameters parameters = new SbkParameters("web-console-test");
            logger.addArgs(parameters);
            assertFalse(parameters.hasOption("webhost"));
            assertFalse(parameters.hasOption("webstart"));
            assertFalse(parameters.hasOption("webminutes"));
            assertTrue(parameters.hasOption("websnapshotminutes"));
            parameters.parseArgs(new String[]{"-writers", "1", "-size", "100",
                    "-webport", Integer.toString(server.getAddress().getPort()), "-webopen", "false",
                    "-websnapshotminutes", "3", "-webtimeoutminutes", "3"});
            logger.parseArgs(parameters);

            logger.open(parameters, "File", Action.Writing, new NanoSeconds());
            final String runId;
            try {
                runId = activeRunId(baseUri);
                assertTrue(get(baseUri.resolve("/api/v1/runs")).body().contains("\"name\":\"SBK File\""));
                emitResult(logger, false, 10);
                assertEquals(1, waitForHistory(baseUri, runId, 1).length);
                emitResult(logger, true, 1000);
            } finally {
                logger.close(parameters);
            }

            final String historyJson =
                    get(baseUri.resolve("/api/v1/runs/" + runId + "/history")).body();
            final WebConsoleSnapshot[] history =
                    MAPPER.readValue(historyJson, WebConsoleSnapshot[].class);
            assertEquals(1, history.length);
            assertEquals(10, history[0].performance().records());
            assertFalse(historyJson.contains("\"total\""));
        }
    }

    @Test
    void webLoggersGenerateUniqueRunsOnTheSamePort() throws Exception {
        try (WebConsoleServer server = new WebConsoleServer(0, 4)) {
            server.start();
            final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            final WebLogger fileLogger = new WebLogger();
            final WebLogger minioLogger = new WebLogger();
            final SbkParameters fileParameters = webParameters("file-web-console-test",
                    server.getAddress().getPort(), fileLogger);
            final SbkParameters minioParameters = webParameters("minio-web-console-test",
                    server.getAddress().getPort(), minioLogger);

            fileLogger.open(fileParameters, "File", Action.Writing, new NanoSeconds());
            minioLogger.open(minioParameters, "MinIO", Action.Reading, new NanoSeconds());
            try {
                final List<?> runs = MAPPER.readValue(get(baseUri.resolve("/api/v1/runs")).body(), List.class);
                assertEquals(2, runs.size());
                final String runsJson = MAPPER.writeValueAsString(runs);
                assertTrue(runsJson.contains("\"name\":\"SBK File\""));
                assertTrue(runsJson.contains("\"name\":\"SBK MinIO\""));
                final Map<?, ?> firstView = (Map<?, ?>) runs.get(0);
                final Map<?, ?> secondView = (Map<?, ?>) runs.get(1);
                final Map<?, ?> firstRun = (Map<?, ?>) firstView.get("run");
                final Map<?, ?> secondRun = (Map<?, ?>) secondView.get("run");
                assertFalse(firstRun.get("runId").equals(secondRun.get("runId")));
            } finally {
                fileLogger.close(fileParameters);
                minioLogger.close(minioParameters);
            }
        }
    }

    private static SbkParameters webParameters(String applicationName, int port, WebLogger logger) throws Exception {
        final SbkParameters parameters = new SbkParameters(applicationName);
        logger.addArgs(parameters);
        parameters.parseArgs(new String[]{"-writers", "1", "-size", "100",
                "-webport", Integer.toString(port), "-webopen", "false"});
        logger.parseArgs(parameters);
        return parameters;
    }

    private static WebConsoleSnapshot[] waitForHistory(URI baseUri, String runId, int expected) throws Exception {
        final long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        WebConsoleSnapshot[] snapshots = new WebConsoleSnapshot[0];
        while (snapshots.length < expected && System.nanoTime() < deadline) {
            snapshots = MAPPER.readValue(get(baseUri.resolve("/api/v1/runs/" + runId + "/history")).body(),
                    WebConsoleSnapshot[].class);
            if (snapshots.length < expected) {
                Thread.sleep(25);
            }
        }
        return snapshots;
    }

    private static HttpResponse<String> get(URI uri) throws Exception {
        return HTTP_CLIENT.send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String activeRunId(URI baseUri) throws Exception {
        final List<?> runs = MAPPER.readValue(
                get(baseUri.resolve("/api/v1/runs")).body(), List.class);
        assertEquals(1, runs.size());
        final Map<?, ?> view = (Map<?, ?>) runs.getFirst();
        final Map<?, ?> run = (Map<?, ?>) view.get("run");
        return run.get("runId").toString();
    }

    private static void emitResult(WebLogger logger, boolean total, long records) {
        if (total) {
            logger.printTotal(System.currentTimeMillis(), 1, 1, 0, 0,
                    records * 100, 1, records, 1,
                    0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0,
                    5, records * 100, records, records, 1,
                    10, 1, 20, 0, 0, 0, 0, 0,
                    new long[]{10, 20}, new long[]{1, 1});
        } else {
            logger.print(System.currentTimeMillis(), 1, 1, 0, 0,
                    records * 100, 1, records, 1,
                    0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0,
                    5, records * 100, records, records, 1,
                    10, 1, 20, 0, 0, 0, 0, 0,
                    new long[]{10, 20}, new long[]{1, 1});
        }
    }
}

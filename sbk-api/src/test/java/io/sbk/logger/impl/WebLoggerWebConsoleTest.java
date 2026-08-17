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

/**
 * Integration tests for the SBK WebLogger adapter and independent Local Web Console runtime.
 */
final class WebLoggerWebConsoleTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Test
    void webLoggerPublishesOnlyRegularIntervalResults() throws Exception {
        try (WebConsoleServer server = new WebConsoleServer("127.0.0.1", 0, 4)) {
            server.start();
            final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            final WebLogger logger = new WebLogger();
            final SbkParameters parameters = new SbkParameters("web-console-test");
            logger.addArgs(parameters);
            parameters.parseArgs(new String[]{"-writers", "1", "-size", "100",
                    "-webhost", "127.0.0.1", "-webport",
                    Integer.toString(server.getAddress().getPort()),
                    "-webstart", "false", "-webopen", "false"});
            logger.parseArgs(parameters);

            logger.open(parameters, "File", Action.Writing, new NanoSeconds());
            final String runId;
            try {
                runId = activeRunId(baseUri);
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

/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.dashboard;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the local dashboard HTTP API and reusable client connection.
 */
final class DashboardServerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void reusesRunningServerAndRetainsOnlyConfiguredHistory() throws Exception {
        try (DashboardServer server = new DashboardServer("127.0.0.1", 0, 2)) {
            server.start();
            final int port = server.getAddress().getPort();
            final DashboardConfig config = config(port);
            final DashboardRun firstRun = run("run-one");
            final DashboardRun secondRun = run("run-two");
            final URI baseUri = URI.create("http://127.0.0.1:" + port);

            try (DashboardClient first = DashboardClient.connect(config, firstRun);
                 DashboardClient second = DashboardClient.connect(config, secondRun)) {
                first.publish(snapshot("run-one", 1));
                waitForHistory(baseUri, "run-one", 1);
                first.publish(snapshot("run-one", 2));
                waitForHistory(baseUri, "run-one", 2);
                first.publish(snapshot("run-one", 3));
                second.publish(snapshot("run-two", 4));
            }

            final DashboardSnapshot[] firstHistory = waitForHistory(baseUri, "run-one", 2);
            final DashboardSnapshot[] secondHistory = waitForHistory(baseUri, "run-two", 1);
            assertEquals(2, firstHistory.length);
            assertEquals(1, secondHistory.length);
            assertEquals(3, firstHistory[1].performance().records());
            assertTrue(get(baseUri.resolve("/api/v1/health")).body().contains("sbk-dashboard"));
            assertTrue(get(baseUri.resolve("/")).body().contains("SBK Live Dashboard"));
        }
    }

    @Test
    void closesPromptlyWhileBrowserEventStreamIsConnected() throws Exception {
        final DashboardServer server = new DashboardServer("127.0.0.1", 0, 2);
        server.start();
        final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (DashboardClient client = DashboardClient.connect(config(server.getAddress().getPort()),
                run("stream-run"))) {
            final HttpResponse<java.io.InputStream> events = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(baseUri.resolve("/api/v1/runs/stream-run/events")).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, events.statusCode());
            assertTimeoutPreemptively(Duration.ofSeconds(2), server::close);
            events.body().close();
        }
    }

    private static DashboardSnapshot[] waitForHistory(URI baseUri, String runId, int expected) throws Exception {
        final long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        DashboardSnapshot[] snapshots = new DashboardSnapshot[0];
        while (snapshots.length < expected && System.nanoTime() < deadline) {
            snapshots = MAPPER.readValue(get(baseUri.resolve("/api/v1/runs/" + runId + "/history")).body(),
                    DashboardSnapshot[].class);
            if (snapshots.length < expected) {
                Thread.sleep(25);
            }
        }
        return snapshots;
    }

    private static HttpResponse<String> get(URI uri) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static DashboardConfig config(int port) {
        final DashboardConfig config = new DashboardConfig();
        config.host = "127.0.0.1";
        config.port = port;
        config.start = false;
        config.open = false;
        config.retention = 2;
        config.name = "test";
        return config;
    }

    private static DashboardRun run(String runId) {
        return new DashboardRun(runId, "test", "SBK", "File", "Writing", "ns", "test", "25", 1);
    }

    private static DashboardSnapshot snapshot(String runId, long records) {
        return new DashboardSnapshot(runId, records, false,
                new DashboardSnapshot.WorkerMetrics(1, 1, 0, 0, 0, 0),
                new DashboardSnapshot.RequestMetrics(100, records, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                new DashboardSnapshot.PerformanceMetrics(records, records * 100, records, records, 1),
                new DashboardSnapshot.LatencyMetrics(10, 1, 20, 0, 0, 0, 0, 0,
                        new double[]{50, 99}, new long[]{10, 20}, new long[]{1, 1}));
    }
}

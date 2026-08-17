/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.webconsole;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Local Web Console HTTP API and reusable client connection.
 */
final class WebConsoleServerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Test
    void reusesRunningServerAndRetainsOnlyConfiguredHistory() throws Exception {
        try (WebConsoleServer server = new WebConsoleServer(0, 2)) {
            server.start();
            final int port = server.getAddress().getPort();
            final WebConsoleConfig config = config(port);
            final WebConsoleRun firstRun = run("run-one");
            final WebConsoleRun secondRun = run("run-two");
            final URI baseUri = URI.create("http://127.0.0.1:" + port);

            try (WebConsoleClient first = WebConsoleClient.connect(config, firstRun)) {
                first.publish(snapshot("run-one", 1));
                waitForHistory(baseUri, "run-one", 1);
                first.publish(snapshot("run-one", 2));
                waitForHistory(baseUri, "run-one", 2);
                first.publish(snapshot("run-one", 3));
            }
            try (WebConsoleClient second = WebConsoleClient.connect(config, secondRun)) {
                second.publish(snapshot("run-two", 4));
            }

            final WebConsoleSnapshot[] firstHistory = waitForHistory(baseUri, "run-one", 2);
            final WebConsoleSnapshot[] secondHistory = waitForHistory(baseUri, "run-two", 1);
            assertEquals(2, firstHistory.length);
            assertEquals(1, secondHistory.length);
            assertEquals(3, firstHistory[1].performance().records());
            assertTrue(get(baseUri.resolve("/api/v1/health")).body().contains("sbk-web-console"));
            assertTrue(get(baseUri.resolve("/")).body().contains("SBK Local Web Console"));
        }
    }

    @Test
    void createsOnlyTheHostLocalLoopbackLink() {
        final var links = WebConsoleClient.webConsoleLinks(9720, "test-run");
        assertEquals(1, links.size());
        assertEquals("Local", links.getFirst().label());
        assertEquals("127.0.0.1", links.getFirst().uri().getHost());
        assertEquals("http://127.0.0.1:9720/?run=test-run", links.getFirst().uri().toString());
    }

    @Test
    void standaloneEntryPointRejectsTheRemovedHostOption() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SbkWebConsoleMain.main(new String[]{"-host", "0.0.0.0"}));
        assertTrue(exception.getMessage().contains("Unknown Local Web Console option -host"));
    }

    @Test
    void standaloneEntryPointRejectsTheRemovedMinutesOption() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SbkWebConsoleMain.main(new String[]{"-minutes", "180"}));
        assertTrue(exception.getMessage().contains("Unknown Local Web Console option -minutes"));
    }

    @Test
    void standaloneEntryPointRejectsNonPositiveIdleTimeout() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SbkWebConsoleMain.main(new String[]{"-webtimeoutminutes", "0"}));
        assertTrue(exception.getMessage().contains("idle timeout must be greater than zero"));
    }

    @Test
    void rejectsASecondActiveBenchmarkWithOwnershipDetails() throws Exception {
        try (WebConsoleServer server = new WebConsoleServer(0, 2)) {
            server.start();
            final WebConsoleConfig config = config(server.getAddress().getPort());
            try (WebConsoleClient ignored = WebConsoleClient.connect(config, run("active-run"))) {
                final WebConsoleClient.WebConsoleBusyException exception = assertThrows(
                        WebConsoleClient.WebConsoleBusyException.class,
                        () -> WebConsoleClient.connect(config, run("competing-run")));
                assertTrue(exception.getMessage().contains("Web Console port " + server.getAddress().getPort()));
                assertTrue(exception.getMessage().contains("already serving active SBK run active-run"));
                assertTrue(exception.getMessage().contains("only one SBK, SBM, or SBK-GEM"));
                assertTrue(exception.getMessage().contains("-webport <different-port>"));
                assertTrue(exception.getMessage().contains("SbkWebConsoleMain"));
            }
        }
    }

    @Test
    void allowsActiveBenchmarksOnDifferentWebConsolePorts() throws Exception {
        try (WebConsoleServer firstServer = new WebConsoleServer(0, 2);
             WebConsoleServer secondServer = new WebConsoleServer(0, 2)) {
            firstServer.start();
            secondServer.start();
            final URI firstBaseUri = URI.create("http://127.0.0.1:" + firstServer.getAddress().getPort());
            final URI secondBaseUri = URI.create("http://127.0.0.1:" + secondServer.getAddress().getPort());
            try (WebConsoleClient first = WebConsoleClient.connect(config(firstServer.getAddress().getPort()),
                    run("first-port-run"));
                 WebConsoleClient second = WebConsoleClient.connect(config(secondServer.getAddress().getPort()),
                         run("second-port-run"))) {
                first.publish(snapshot("first-port-run", 1));
                second.publish(snapshot("second-port-run", 2));
                assertEquals(1, waitForHistory(firstBaseUri, "first-port-run", 1).length);
                assertEquals(1, waitForHistory(secondBaseUri, "second-port-run", 1).length);
            }
        }
    }

    @Test
    void retainsCompletedLogsForBrowserThenStopsAfterBrowserDisconnects() throws Exception {
        final Duration idleTimeout = Duration.ofMillis(500);
        final WebConsoleServer server = new WebConsoleServer(0, 2, idleTimeout,
                Duration.ofMillis(20));
        server.start();
        final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        final WebConsoleClient client = WebConsoleClient.connect(config(server.getAddress().getPort()),
                run("retained-run"));
        post(baseUri.resolve("/api/v1/browser/connect"), "{\"browserId\":\"test-browser\"}");
        final HttpResponse<java.io.InputStream> events = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/runs/retained-run/events")).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200, events.statusCode());

        client.close();
        for (int refresh = 0; refresh < 10; refresh++) {
            Thread.sleep(100);
            post(baseUri.resolve("/api/v1/browser/connect"), "{\"browserId\":\"test-browser\"}");
        }
        assertEquals(200, get(baseUri.resolve("/api/v1/health")).statusCode());

        post(baseUri.resolve("/api/v1/browser/disconnect"), "{\"browserId\":\"test-browser\"}");
        events.body().close();
        assertTimeoutPreemptively(Duration.ofSeconds(2), server::awaitTermination);
    }

    @Test
    void browserConnectingDuringIdleGraceCancelsOriginalShutdown() throws Exception {
        final Duration idleTimeout = Duration.ofSeconds(2);
        final WebConsoleServer server = new WebConsoleServer(0, 2, idleTimeout,
                Duration.ofMillis(20));
        server.start();
        final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        assertEquals(201, post(baseUri.resolve("/api/v1/runs"),
                MAPPER.writeValueAsString(run("browser-grace-run"))).statusCode());
        assertEquals(204, post(baseUri.resolve("/api/v1/runs/browser-grace-run/snapshots"),
                MAPPER.writeValueAsString(snapshot("browser-grace-run", 1))).statusCode());
        assertEquals(204, post(baseUri.resolve("/api/v1/runs/browser-grace-run/complete"), "{}").statusCode());

        Thread.sleep(1000);
        for (int refresh = 0; refresh < 16; refresh++) {
            post(baseUri.resolve("/api/v1/browser/connect"), "{\"browserId\":\"late-browser\"}");
            Thread.sleep(100);
        }
        assertEquals(200, get(baseUri.resolve("/api/v1/health")).statusCode());

        post(baseUri.resolve("/api/v1/browser/disconnect"), "{\"browserId\":\"late-browser\"}");
        assertTimeoutPreemptively(Duration.ofSeconds(3), server::awaitTermination);
    }

    @Test
    void benchmarkConnectingDuringIdleGraceCancelsOriginalShutdown() throws Exception {
        final Duration idleTimeout = Duration.ofSeconds(2);
        final WebConsoleServer server = new WebConsoleServer(0, 2, idleTimeout,
                Duration.ofMillis(20));
        server.start();
        final int port = server.getAddress().getPort();
        final URI baseUri = URI.create("http://127.0.0.1:" + port);
        try (WebConsoleClient first = WebConsoleClient.connect(config(port), run("first-grace-run"))) {
            first.publish(snapshot("first-grace-run", 1));
        }

        Thread.sleep(1000);
        try (WebConsoleClient second = WebConsoleClient.connect(config(port), run("second-grace-run"))) {
            Thread.sleep(1500);
            assertEquals(200, get(baseUri.resolve("/api/v1/health")).statusCode());
            second.publish(snapshot("second-grace-run", 2));
            assertEquals(1, waitForHistory(baseUri, "second-grace-run", 1).length);
        }
        // The shutdown cannot begin before idleTimeout elapses. Allow time for
        // the scheduler and HttpServer executor to finish on slower platforms.
        assertTimeoutPreemptively(idleTimeout.plusSeconds(2), server::awaitTermination);
    }

    @Test
    void closesPromptlyWhileBrowserEventStreamIsConnected() throws Exception {
        final WebConsoleServer server = new WebConsoleServer(0, 2);
        server.start();
        final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (WebConsoleClient client = WebConsoleClient.connect(config(server.getAddress().getPort()),
                run("stream-run"))) {
            final HttpResponse<java.io.InputStream> events = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(baseUri.resolve("/api/v1/runs/stream-run/events")).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, events.statusCode());
            assertTimeoutPreemptively(Duration.ofSeconds(2), server::close);
            events.body().close();
        }
    }

    @Test
    void abandonedRunWithoutBrowserStopsWebConsole() throws Exception {
        final Duration idleTimeout = Duration.ofMillis(300);
        final WebConsoleServer server = new WebConsoleServer(0, 2, idleTimeout,
                Duration.ofMillis(20));
        server.start();
        final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

        assertEquals(201, post(baseUri.resolve("/api/v1/runs"),
                MAPPER.writeValueAsString(run("abandoned-run"))).statusCode());

        assertTimeoutPreemptively(Duration.ofSeconds(2), server::awaitTermination);
    }

    @Test
    void abandonedRunRemainsForAttachedBrowserAndReleasesOwnership() throws Exception {
        final Duration idleTimeout = Duration.ofMillis(300);
        final WebConsoleServer server = new WebConsoleServer(0, 2, idleTimeout,
                Duration.ofMillis(20));
        server.start();
        final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        assertEquals(201, post(baseUri.resolve("/api/v1/runs"),
                MAPPER.writeValueAsString(run("browser-retained-abandoned-run"))).statusCode());

        String runs = "";
        for (int refresh = 0; refresh < 10 && !runs.contains("\"abandoned\":true"); refresh++) {
            post(baseUri.resolve("/api/v1/browser/connect"), "{\"browserId\":\"lease-browser\"}");
            Thread.sleep(100);
            runs = get(baseUri.resolve("/api/v1/runs")).body();
        }
        assertTrue(runs.contains("\"abandoned\":true"));
        assertEquals(201, post(baseUri.resolve("/api/v1/runs"),
                MAPPER.writeValueAsString(run("replacement-run"))).statusCode());
        assertEquals(204, post(baseUri.resolve("/api/v1/runs/replacement-run/complete"), "{}").statusCode());
        post(baseUri.resolve("/api/v1/browser/disconnect"), "{\"browserId\":\"lease-browser\"}");

        assertTimeoutPreemptively(Duration.ofSeconds(2), server::awaitTermination);
    }

    @Test
    void clientHeartbeatRenewsActiveRunLease() throws Exception {
        final Duration idleTimeout = Duration.ofMillis(300);
        final WebConsoleServer server = new WebConsoleServer(0, 2, idleTimeout,
                Duration.ofMillis(20));
        server.start();
        final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

        try (WebConsoleClient ignored = WebConsoleClient.connect(config(server.getAddress().getPort()),
                run("heartbeat-run"), Duration.ofMillis(75))) {
            Thread.sleep(800);
            assertEquals(200, get(baseUri.resolve("/api/v1/health")).statusCode());
            assertTrue(get(baseUri.resolve("/api/v1/runs")).body().contains("\"completed\":false"));
        }

        assertTimeoutPreemptively(Duration.ofSeconds(2), server::awaitTermination);
    }

    @Test
    void snapshotsRenewActiveRunLease() throws Exception {
        final Duration idleTimeout = Duration.ofMillis(300);
        final WebConsoleServer server = new WebConsoleServer(0, 2, idleTimeout,
                Duration.ofMillis(20));
        server.start();
        final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        assertEquals(201, post(baseUri.resolve("/api/v1/runs"),
                MAPPER.writeValueAsString(run("snapshot-lease-run"))).statusCode());

        for (int sequence = 1; sequence <= 6; sequence++) {
            Thread.sleep(100);
            assertEquals(204, post(baseUri.resolve("/api/v1/runs/snapshot-lease-run/snapshots"),
                    MAPPER.writeValueAsString(snapshot("snapshot-lease-run", sequence))).statusCode());
        }
        assertEquals(200, get(baseUri.resolve("/api/v1/health")).statusCode());
        assertTrue(get(baseUri.resolve("/api/v1/runs")).body().contains("\"completed\":false"));

        assertTimeoutPreemptively(Duration.ofSeconds(2), server::awaitTermination);
    }

    @Test
    void rejectsSnapshotsAfterRunCompletionWithoutChangingHistory() throws Exception {
        try (WebConsoleServer server = new WebConsoleServer(0, 2)) {
            server.start();
            final URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            assertEquals(201, post(baseUri.resolve("/api/v1/runs"),
                    MAPPER.writeValueAsString(run("completed-run"))).statusCode());
            assertEquals(204, post(baseUri.resolve("/api/v1/runs/completed-run/snapshots"),
                    MAPPER.writeValueAsString(snapshot("completed-run", 1))).statusCode());
            assertEquals(204, post(baseUri.resolve("/api/v1/runs/completed-run/complete"), "{}").statusCode());

            assertEquals(409, post(baseUri.resolve("/api/v1/runs/completed-run/snapshots"),
                    MAPPER.writeValueAsString(snapshot("completed-run", 2))).statusCode());
            assertEquals(1, waitForHistory(baseUri, "completed-run", 1).length);
        }
    }

    @Test
    void webConsoleWithoutBenchmarkOrBrowserStopsAfterIdleTimeout() throws Exception {
        final WebConsoleServer server = new WebConsoleServer(0, 2,
                Duration.ofMillis(200), Duration.ofMillis(20));
        server.start();

        assertTimeoutPreemptively(Duration.ofSeconds(2), server::awaitTermination);
    }

    @Test
    void defaultIdleTimeoutIsOneMinute() {
        assertEquals(Duration.ofMinutes(1), WebConsoleServer.DEFAULT_IDLE_TIMEOUT);
    }

    @Test
    void convertsWebConsoleMinutesToFiveSecondSnapshots() {
        assertEquals(2160, SbkWebConsoleMain.retentionSnapshots(180));
        assertEquals(12, SbkWebConsoleMain.retentionSnapshots(1));
        assertThrows(IllegalArgumentException.class, () -> SbkWebConsoleMain.retentionSnapshots(0));
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

    private static HttpResponse<String> post(URI uri, String body) throws Exception {
        return HTTP_CLIENT.send(HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static WebConsoleConfig config(int port) {
        final WebConsoleConfig config = new WebConsoleConfig();
        config.port = port;
        config.open = false;
        config.snapshotMinutes = 1;
        config.timeoutMinutes = 1;
        config.name = "test";
        return config;
    }

    private static WebConsoleRun run(String runId) {
        return new WebConsoleRun(runId, "test", "SBK", "File", "Writing", "ns", "test", "25", 1);
    }

    private static WebConsoleSnapshot snapshot(String runId, long records) {
        return new WebConsoleSnapshot(runId, records,
                new WebConsoleSnapshot.WorkerMetrics(1, 1, 0, 0, 0, 0),
                new WebConsoleSnapshot.RequestMetrics(100, records, 1, 1, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                new WebConsoleSnapshot.PerformanceMetrics(records, records * 100, records, records, 1),
                new WebConsoleSnapshot.LatencyMetrics(10, 1, 20, 0, 0, 0, 0, 0,
                        new double[]{50, 99}, new long[]{10, 20}, new long[]{1, 1}));
    }

}

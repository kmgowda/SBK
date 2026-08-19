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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Lightweight HTTP server that stores bounded SBK histories and serves the SBK Web Console.
 */
public final class WebConsoleServer implements AutoCloseable {
    /** Loopback address used by local Web Console clients. */
    public static final String LOCAL_HOST = "127.0.0.1";
    /** IPv4 wildcard address used by the Web Console server. */
    public static final String BIND_HOST = "0.0.0.0";
    /** Local Web Console HTTP API version. */
    public static final int API_VERSION = 5;
    private static final int EVENT_QUEUE_CAPACITY = 1;
    private static final int RUN_PATH_ELEMENT_COUNT = 3;
    private static final int RUN_PATH_RESOURCE_INDEX = 0;
    private static final int RUN_PATH_ACTION_INDEX = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(WebConsoleServer.class);
    private static final String RESOURCE_PREFIX = "/webconsole/";
    private static final String SSE_OWNED_ATTRIBUTE = "sbk.webconsole.sseOwned";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpServer server;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final int retention;
    private final ConcurrentHashMap<String, RunState> runs;
    private final Duration idleTimeout;
    private final Duration heartbeatInterval;
    private final int sseRetryMillis;
    private final Map<String, Integer> browserConfig;
    private final Object lifecycleLock;
    private final AtomicBoolean closed;
    private final CountDownLatch termination;
    private final Map<String, Long> browsers;
    private final Set<String> activeRunIds;
    private ScheduledFuture<?> idleShutdown;
    private boolean shuttingDown;

    /**
     * Creates a Web Console server bound to all IPv4 interfaces.
     *
     * @param port      TCP port
     * @param retention maximum snapshots retained per run
     * @throws IOException if the server cannot bind
     * @throws IllegalArgumentException if retention is not positive
     */
    public WebConsoleServer(int port, int retention) throws IOException {
        this(port, retention, defaultIdleTimeout(), defaultHeartbeatInterval());
    }

    /**
     * Creates a Local Web Console server with a configurable idle timeout.
     *
     * @param port        TCP port
     * @param retention   maximum snapshots retained per run
     * @param idleTimeout delay before an inactive web console without browsers stops
     * @throws IOException if the server cannot bind
     * @throws IllegalArgumentException if a size or duration is not positive
     */
    public WebConsoleServer(int port, int retention, Duration idleTimeout) throws IOException {
        this(port, retention, idleTimeout, defaultHeartbeatInterval());
    }

    /**
     * Creates a Local Web Console server with configurable lifecycle timings.
     *
     * @param port              TCP port
     * @param retention         maximum snapshots retained per run
     * @param idleTimeout       delay before an inactive web console without browsers stops
     * @param heartbeatInterval interval used to detect disconnected browser event streams
     * @throws IOException if the server cannot bind
     * @throws IllegalArgumentException if a size or duration is not positive
     */
    WebConsoleServer(int port, int retention, Duration idleTimeout, Duration heartbeatInterval)
            throws IOException {
        if (retention < 1) {
            throw new IllegalArgumentException("Local Web Console retention must be greater than zero");
        }
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("Local Web Console idle timeout must be greater than zero");
        }
        if (heartbeatInterval.isZero() || heartbeatInterval.isNegative()) {
            throw new IllegalArgumentException("Local Web Console heartbeat interval must be greater than zero");
        }
        this.retention = retention;
        this.idleTimeout = idleTimeout;
        this.heartbeatInterval = heartbeatInterval;
        final WebConsoleConfig config = WebConsoleConfig.load();
        this.sseRetryMillis = config.sseRetryMillis;
        this.browserConfig = Map.of(
                "browserHeartbeatMillis", config.browserHeartbeatMillis,
                "browserSnapshotLimit", config.browserSnapshotLimit,
                "chartSnapshotLimit", config.chartSnapshotLimit,
                "refreshMillis", config.refreshMillis);
        this.runs = new ConcurrentHashMap<>();
        this.lifecycleLock = new Object();
        this.closed = new AtomicBoolean();
        this.termination = new CountDownLatch(1);
        this.browsers = new ConcurrentHashMap<>();
        this.activeRunIds = new HashSet<>();
        this.server = HttpServer.create(new InetSocketAddress(BIND_HOST, port), config.httpBacklog);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .name("sbk-web-console-idle-monitor").daemon().factory());
        server.setExecutor(executor);
        server.createContext(WebConsoleProtocol.API_PREFIX, this::handleApi);
        server.createContext("/", this::handleResource);
    }

    private static Duration defaultIdleTimeout() {
        return Duration.ofMinutes(WebConsoleConfig.load().timeoutMinutes);
    }

    private static Duration defaultHeartbeatInterval() {
        return Duration.ofMillis(WebConsoleConfig.load().serverHeartbeatMillis);
    }

    /**
     * Starts accepting Local Web Console connections.
     */
    public void start() {
        server.start();
        synchronized (lifecycleLock) {
            scheduleIdleShutdownIfUnused();
        }
        LOGGER.info("SBK Web Console started on port {}: retention={}, idle timeout={} minute(s)",
                server.getAddress().getPort(), retention, idleTimeout.toMinutes());
    }

    /**
     * Returns the actual bound address.
     *
     * @return server address
     */
    public InetSocketAddress getAddress() {
        return server.getAddress();
    }

    /**
     * Waits until this server is closed explicitly or by its idle lifecycle policy.
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void awaitTermination() throws InterruptedException {
        termination.await();
    }

    @Override
    public void close() {
        close("shutdown requested");
    }

    private void close(String reason) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        final int activeWebLoggers;
        final int connectedBrowsers;
        synchronized (lifecycleLock) {
            shuttingDown = true;
            cancelIdleShutdown();
            runs.values().forEach(this::cancelRunExpiry);
            activeWebLoggers = activeRunIds.size();
            connectedBrowsers = browsers.size();
        }
        runs.values().forEach(RunState::close);
        server.stop(0);
        scheduler.shutdown();
        executor.close();
        LOGGER.info("SBK Web Console exited: reason={}, active WebLoggers={}, connected browsers={}, retained runs={}",
                reason, activeWebLoggers, connectedBrowsers, runs.size());
        termination.countDown();
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            final String path = exchange.getRequestURI().getPath();
            if (WebConsoleProtocol.HEALTH_PATH.equals(path)) {
                requireMethod(exchange, WebConsoleProtocol.METHOD_GET);
                sendJson(exchange, HTTP_OK, Map.of("service", "sbk-web-console", "apiVersion", API_VERSION,
                        "status", "ready"));
                return;
            }
            if (WebConsoleProtocol.CONFIG_PATH.equals(path)) {
                requireMethod(exchange, WebConsoleProtocol.METHOD_GET);
                sendJson(exchange, HTTP_OK, browserConfig);
                return;
            }
            if (WebConsoleProtocol.RUNS_PATH.equals(path)) {
                if (WebConsoleProtocol.METHOD_GET.equals(exchange.getRequestMethod())) {
                    sendJson(exchange, HTTP_OK, runs.values().stream().map(RunState::view).toList());
                } else {
                    requireMethod(exchange, WebConsoleProtocol.METHOD_POST);
                    final WebConsoleRun run = MAPPER.readValue(exchange.getRequestBody(), WebConsoleRun.class);
                    if (run.runId() == null || run.runId().isBlank()) {
                        sendText(exchange, HTTP_BAD_REQUEST, "runId is required", WebConsoleProtocol.TEXT_UTF_8);
                        return;
                    }
                    final String conflict = register(run);
                    if (conflict != null) {
                        sendText(exchange, HTTP_CONFLICT, conflict, WebConsoleProtocol.TEXT_UTF_8);
                    } else {
                        sendJson(exchange, HTTP_CREATED, run);
                    }
                }
                return;
            }
            if (WebConsoleProtocol.BROWSER_CONNECT_PATH.equals(path)
                    || WebConsoleProtocol.BROWSER_DISCONNECT_PATH.equals(path)) {
                requireMethod(exchange, WebConsoleProtocol.METHOD_POST);
                final Map<?, ?> request = MAPPER.readValue(exchange.getRequestBody(), Map.class);
                final String browserId = Objects.toString(request.get("browserId"), "");
                if (browserId.isBlank()) {
                    sendText(exchange, HTTP_BAD_REQUEST, "browserId is required", WebConsoleProtocol.TEXT_UTF_8);
                    return;
                }
                if (path.endsWith(WebConsoleProtocol.CONNECT_SUFFIX)) {
                    browserSeen(browserId);
                } else {
                    browserGone(browserId);
                }
                exchange.sendResponseHeaders(HTTP_NO_CONTENT, -1);
                return;
            }
            handleRunApi(exchange, path);
        } catch (MethodNotAllowedException ex) {
            sendText(exchange, HTTP_BAD_METHOD, ex.getMessage(), WebConsoleProtocol.TEXT_UTF_8);
        } catch (IllegalArgumentException ex) {
            sendText(exchange, HTTP_BAD_REQUEST, ex.getMessage(), WebConsoleProtocol.TEXT_UTF_8);
        } catch (Exception ex) {
            sendText(exchange, HTTP_INTERNAL_ERROR,
                    Objects.toString(ex.getMessage(), ex.getClass().getSimpleName()), WebConsoleProtocol.TEXT_UTF_8);
        } finally {
            if (!Boolean.TRUE.equals(exchange.getAttribute(SSE_OWNED_ATTRIBUTE))) {
                exchange.close();
            }
        }
    }

    private void handleRunApi(HttpExchange exchange, String path) throws IOException {
        final String relative = path.substring(WebConsoleProtocol.API_PREFIX.length());
        final String[] elements = relative.split("/");
        if (elements.length != RUN_PATH_ELEMENT_COUNT
                || !WebConsoleProtocol.RUNS_RESOURCE.equals(elements[RUN_PATH_RESOURCE_INDEX])) {
            sendText(exchange, HTTP_NOT_FOUND, "Not found", WebConsoleProtocol.TEXT_UTF_8);
            return;
        }
        final RunState state = runs.get(elements[1]);
        if (state == null) {
            sendText(exchange, HTTP_NOT_FOUND, "Unknown Local Web Console run", WebConsoleProtocol.TEXT_UTF_8);
            return;
        }
        switch (elements[RUN_PATH_ACTION_INDEX]) {
            case "snapshots" -> {
                requireMethod(exchange, WebConsoleProtocol.METHOD_POST);
                final WebConsoleSnapshot snapshot = MAPPER.readValue(exchange.getRequestBody(),
                        WebConsoleSnapshot.class);
                if (!state.run.runId().equals(snapshot.runId())) {
                    throw new IllegalArgumentException("Snapshot runId does not match URL");
                }
                if (!benchmarkSeen(state.run.runId())) {
                    sendText(exchange, HTTP_CONFLICT, "Local Web Console run lease has expired",
                            WebConsoleProtocol.TEXT_UTF_8);
                    return;
                }
                if (!state.add(snapshot)) {
                    sendText(exchange, HTTP_CONFLICT, "Local Web Console run has completed",
                            WebConsoleProtocol.TEXT_UTF_8);
                    return;
                }
                exchange.sendResponseHeaders(HTTP_NO_CONTENT, -1);
            }
            case "heartbeat" -> {
                requireMethod(exchange, WebConsoleProtocol.METHOD_POST);
                if (!benchmarkSeen(state.run.runId())) {
                    sendText(exchange, HTTP_CONFLICT, "Local Web Console run lease has expired",
                            WebConsoleProtocol.TEXT_UTF_8);
                    return;
                }
                exchange.sendResponseHeaders(HTTP_NO_CONTENT, -1);
            }
            case "complete" -> {
                requireMethod(exchange, WebConsoleProtocol.METHOD_POST);
                state.complete();
                benchmarkCompleted(state.run.runId());
                exchange.sendResponseHeaders(HTTP_NO_CONTENT, -1);
            }
            case "history" -> {
                requireMethod(exchange, WebConsoleProtocol.METHOD_GET);
                sendJson(exchange, HTTP_OK, state.history());
            }
            case "events" -> {
                requireMethod(exchange, WebConsoleProtocol.METHOD_GET);
                state.events(exchange);
            }
            default -> sendText(exchange, HTTP_NOT_FOUND, "Not found", WebConsoleProtocol.TEXT_UTF_8);
        }
    }

    private void handleResource(HttpExchange exchange) throws IOException {
        try {
            requireMethod(exchange, WebConsoleProtocol.METHOD_GET);
            final String path = exchange.getRequestURI().getPath();
            final String resource;
            final String contentType;
            if ("/".equals(path) || "/index.html".equals(path)) {
                resource = RESOURCE_PREFIX + "index.html";
                contentType = WebConsoleProtocol.HTML_UTF_8;
            } else if ("/app.js".equals(path)) {
                resource = RESOURCE_PREFIX + "app.js";
                contentType = WebConsoleProtocol.JAVASCRIPT_UTF_8;
            } else if ("/style.css".equals(path)) {
                resource = RESOURCE_PREFIX + "style.css";
                contentType = WebConsoleProtocol.CSS_UTF_8;
            } else {
                sendText(exchange, HTTP_NOT_FOUND, "Not found", WebConsoleProtocol.TEXT_UTF_8);
                return;
            }
            try (InputStream input = WebConsoleServer.class.getResourceAsStream(resource)) {
                if (input == null) {
                    sendText(exchange, HTTP_NOT_FOUND, "Local Web Console resource not found",
                            WebConsoleProtocol.TEXT_UTF_8);
                    return;
                }
                final byte[] body = input.readAllBytes();
                exchange.getResponseHeaders().set(WebConsoleProtocol.CONTENT_TYPE_HEADER, contentType);
                exchange.getResponseHeaders().set(WebConsoleProtocol.CACHE_CONTROL_HEADER, WebConsoleProtocol.NO_CACHE);
                exchange.sendResponseHeaders(HTTP_OK, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
            }
        } catch (MethodNotAllowedException ex) {
            sendText(exchange, HTTP_BAD_METHOD, ex.getMessage(), WebConsoleProtocol.TEXT_UTF_8);
        } finally {
            exchange.close();
        }
    }

    private static void requireMethod(HttpExchange exchange, String method) {
        if (!method.equals(exchange.getRequestMethod())) {
            throw new MethodNotAllowedException("Expected " + method);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        final byte[] bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set(WebConsoleProtocol.CONTENT_TYPE_HEADER, WebConsoleProtocol.JSON_UTF_8);
        exchange.getResponseHeaders().set(WebConsoleProtocol.CACHE_CONTROL_HEADER, WebConsoleProtocol.NO_STORE);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String body, String contentType)
            throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(WebConsoleProtocol.CONTENT_TYPE_HEADER, contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String register(WebConsoleRun run) {
        final int activeWebLoggers;
        final int connectedBrowsers;
        synchronized (lifecycleLock) {
            if (shuttingDown) {
                return "SBK Local Web Console is shutting down; retry the benchmark";
            }
            final RunState state = new RunState(run, retention);
            if (runs.putIfAbsent(run.runId(), state) != null) {
                return "Local Web Console runId already exists: " + run.runId();
            }
            cancelIdleShutdown();
            activeRunIds.add(run.runId());
            scheduleRunExpiry(state);
            activeWebLoggers = activeRunIds.size();
            connectedBrowsers = browsers.size();
        }
        LOGGER.info("WebLogger connected: runId={}, board={}, source={}; active WebLoggers={}, "
                        + "connected browsers={}", run.runId(), run.name(), run.source(), activeWebLoggers,
                connectedBrowsers);
        return null;
    }

    private boolean benchmarkSeen(String runId) {
        synchronized (lifecycleLock) {
            if (!activeRunIds.contains(runId) || shuttingDown) {
                return false;
            }
            final RunState state = runs.get(runId);
            if (state == null || state.completed) {
                return false;
            }
            state.touch();
            scheduleRunExpiry(state);
            return true;
        }
    }

    private void benchmarkCompleted(String runId) {
        int activeWebLoggers = 0;
        int connectedBrowsers = 0;
        boolean disconnected = false;
        synchronized (lifecycleLock) {
            if (activeRunIds.remove(runId)) {
                cancelRunExpiry(runs.get(runId));
                scheduleIdleShutdownIfUnused();
                activeWebLoggers = activeRunIds.size();
                connectedBrowsers = browsers.size();
                disconnected = true;
            }
        }
        if (disconnected) {
            LOGGER.info("WebLogger disconnected: runId={}, status=completed; active WebLoggers={}, "
                    + "connected browsers={}", runId, activeWebLoggers, connectedBrowsers);
        }
    }

    private void browserSeen(String browserId) {
        final boolean connected;
        final int activeWebLoggers;
        final int connectedBrowsers;
        synchronized (lifecycleLock) {
            connected = browsers.put(browserId, System.currentTimeMillis()) == null;
            if (activeRunIds.isEmpty()) {
                scheduleIdleShutdownIfUnused();
            }
            activeWebLoggers = activeRunIds.size();
            connectedBrowsers = browsers.size();
        }
        if (connected) {
            LOGGER.info("Web browser/client connected: browserId={}; active WebLoggers={}, connected browsers={}",
                    browserId, activeWebLoggers, connectedBrowsers);
        }
    }

    private void browserGone(String browserId) {
        final boolean disconnected;
        final int activeWebLoggers;
        final int connectedBrowsers;
        synchronized (lifecycleLock) {
            disconnected = browsers.remove(browserId) != null;
            if (activeRunIds.isEmpty()) {
                scheduleIdleShutdownIfUnused();
            }
            activeWebLoggers = activeRunIds.size();
            connectedBrowsers = browsers.size();
        }
        if (disconnected) {
            LOGGER.info("Web browser/client disconnected: browserId={}; active WebLoggers={}, "
                    + "connected browsers={}", browserId, activeWebLoggers, connectedBrowsers);
        }
    }

    private void scheduleRunExpiry(RunState state) {
        cancelRunExpiry(state);
        final long delay = Math.max(1, state.lastActivity + idleTimeout.toMillis() - System.currentTimeMillis());
        state.expiry = scheduler.schedule(() -> expireRun(state.run.runId()), delay, TimeUnit.MILLISECONDS);
    }

    private void expireRun(String runId) {
        expireRunAt(runId, System.currentTimeMillis() - idleTimeout.toMillis());
    }

    void expireRunAt(String runId, long expiry) {
        boolean closeNow = false;
        boolean abandoned = false;
        int activeWebLoggers = 0;
        int connectedBrowsers = 0;
        int expiredBrowsers = 0;
        synchronized (lifecycleLock) {
            final RunState state = runs.get(runId);
            if (state != null) {
                state.expiry = null;
            }
            if (!activeRunIds.contains(runId) || shuttingDown) {
                return;
            }
            if (state != null && state.lastActivity > expiry) {
                scheduleRunExpiry(state);
                return;
            }
            if (state != null) {
                state.abandon();
            }
            abandoned = activeRunIds.remove(runId);
            if (activeRunIds.isEmpty()) {
                final int previousBrowsers = browsers.size();
                browsers.entrySet().removeIf(entry -> entry.getValue() <= expiry);
                expiredBrowsers = previousBrowsers - browsers.size();
                if (browsers.isEmpty()) {
                    shuttingDown = true;
                    closeNow = true;
                } else {
                    scheduleIdleShutdownIfUnused();
                }
            }
            activeWebLoggers = activeRunIds.size();
            connectedBrowsers = browsers.size();
        }
        if (abandoned) {
            LOGGER.info("WebLogger disconnected: runId={}, status=abandoned; active WebLoggers={}, "
                    + "connected browsers={}", runId, activeWebLoggers, connectedBrowsers);
        }
        logExpiredBrowsers(expiredBrowsers, activeWebLoggers, connectedBrowsers);
        if (closeNow) {
            close("all WebLoggers disconnected and no browsers remain");
        }
    }

    private void scheduleIdleShutdownIfUnused() {
        if (!activeRunIds.isEmpty() || shuttingDown) {
            return;
        }
        cancelIdleShutdown();
        final long now = System.currentTimeMillis();
        final long lastBrowserSeen = browsers.values().stream().mapToLong(Long::longValue).max().orElse(now);
        final long delay = Math.max(1, lastBrowserSeen + idleTimeout.toMillis() - now);
        idleShutdown = scheduler.schedule(this::closeIfUnused, delay, TimeUnit.MILLISECONDS);
    }

    private void closeIfUnused() {
        int expiredBrowsers;
        synchronized (lifecycleLock) {
            idleShutdown = null;
            if (!activeRunIds.isEmpty() || shuttingDown) {
                return;
            }
            final long expiry = System.currentTimeMillis() - idleTimeout.toMillis();
            final int previousBrowsers = browsers.size();
            browsers.entrySet().removeIf(entry -> entry.getValue() <= expiry);
            expiredBrowsers = previousBrowsers - browsers.size();
            if (!browsers.isEmpty()) {
                scheduleIdleShutdownIfUnused();
                logExpiredBrowsers(expiredBrowsers, activeRunIds.size(), browsers.size());
                return;
            }
            shuttingDown = true;
        }
        logExpiredBrowsers(expiredBrowsers, 0, 0);
        close("idle timeout with no active WebLoggers or browsers");
    }

    private void logExpiredBrowsers(int expiredBrowsers, int activeWebLoggers, int connectedBrowsers) {
        if (expiredBrowsers > 0) {
            LOGGER.info("Expired {} inactive web browser/client connection(s); active WebLoggers={}, "
                    + "connected browsers={}", expiredBrowsers, activeWebLoggers, connectedBrowsers);
        }
    }

    private void cancelIdleShutdown() {
        if (idleShutdown != null) {
            idleShutdown.cancel(false);
            idleShutdown = null;
        }
    }

    private void cancelRunExpiry(RunState state) {
        if (state != null && state.expiry != null) {
            state.expiry.cancel(false);
            state.expiry = null;
        }
    }

    private final class RunState {
        private final WebConsoleRun run;
        private final int retention;
        private final ArrayDeque<WebConsoleSnapshot> history;
        private final CopyOnWriteArrayList<SseClient> clients;
        private volatile boolean completed;
        private volatile boolean abandoned;
        private volatile long lastActivity;
        private ScheduledFuture<?> expiry;

        private RunState(WebConsoleRun run, int retention) {
            this.run = run;
            this.retention = retention;
            this.history = new ArrayDeque<>(retention);
            this.clients = new CopyOnWriteArrayList<>();
            this.lastActivity = System.currentTimeMillis();
        }

        private synchronized boolean add(WebConsoleSnapshot snapshot) throws IOException {
            if (completed || abandoned) {
                return false;
            }
            if (history.size() == retention) {
                history.removeFirst();
            }
            history.addLast(snapshot);
            final String event = "event: snapshot\ndata: " + MAPPER.writeValueAsString(snapshot) + "\n\n";
            clients.forEach(client -> client.offer(event));
            return true;
        }

        private synchronized List<WebConsoleSnapshot> history() {
            return new ArrayList<>(history);
        }

        private synchronized void complete() {
            if (completed) {
                return;
            }
            completed = true;
            clients.forEach(client -> client.offer("event: complete\ndata: {\"abandoned\":" + abandoned
                    + "}\n\n"));
        }

        private synchronized void abandon() {
            if (completed) {
                return;
            }
            completed = true;
            abandoned = true;
            clients.forEach(client -> client.offer("event: complete\ndata: {\"abandoned\":true}\n\n"));
        }

        private void touch() {
            lastActivity = System.currentTimeMillis();
        }

        private WebConsoleRunView view() {
            return new WebConsoleRunView(run, completed, abandoned);
        }

        private void events(HttpExchange exchange) throws IOException {
            exchange.setAttribute(SSE_OWNED_ATTRIBUTE, Boolean.TRUE);
            exchange.getResponseHeaders().set(WebConsoleProtocol.CONTENT_TYPE_HEADER,
                    WebConsoleProtocol.EVENT_STREAM_UTF_8);
            exchange.getResponseHeaders().set(WebConsoleProtocol.CACHE_CONTROL_HEADER, WebConsoleProtocol.NO_CACHE);
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(HTTP_OK, 0);
            final SseClient client = new SseClient(exchange.getResponseBody(), heartbeatInterval,
                    sseRetryMillis);
            clients.add(client);
            try {
                client.run();
            } finally {
                clients.remove(client);
                client.close();
                exchange.close();
            }
        }

        private void close() {
            clients.forEach(SseClient::close);
            clients.clear();
        }
    }

    private record WebConsoleRunView(WebConsoleRun run, boolean completed, boolean abandoned) {
    }

    private static final class SseClient implements AutoCloseable {
        private static final String WAKE_EVENT = "";
        private final OutputStream output;
        private final ArrayBlockingQueue<String> events;
        private final Duration heartbeatInterval;
        private final int retryMillis;
        private volatile boolean open;

        private SseClient(OutputStream output, Duration heartbeatInterval, int retryMillis) {
            this.output = output;
            this.events = new ArrayBlockingQueue<>(EVENT_QUEUE_CAPACITY);
            this.heartbeatInterval = heartbeatInterval;
            this.retryMillis = retryMillis;
            this.open = true;
        }

        private void offer(String event) {
            if (!events.offer(event)) {
                events.poll();
                events.offer(event);
            }
        }

        private void run() throws IOException {
            output.write(("retry: " + retryMillis + "\n\n").getBytes(StandardCharsets.UTF_8));
            output.flush();
            while (open) {
                try {
                    final String event = events.poll(heartbeatInterval.toMillis(), TimeUnit.MILLISECONDS);
                    if (!open) {
                        return;
                    }
                    output.write((event == null ? ": heartbeat\n\n" : event).getBytes(StandardCharsets.UTF_8));
                    output.flush();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        @Override
        public void close() {
            open = false;
            offer(WAKE_EVENT);
            // The HTTP handler owns and closes the exchange after run()
            // returns. Closing its output here races with that cleanup and
            // can trip an assertion inside JDK HttpServer.
        }
    }

    private static final class MethodNotAllowedException extends IllegalArgumentException {
        private MethodNotAllowedException(String message) {
            super(message);
        }
    }
}

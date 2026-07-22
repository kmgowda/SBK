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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

/**
 * Lightweight local HTTP server that stores bounded SBK histories and serves the browser dashboard.
 */
public final class DashboardServer implements AutoCloseable {
    /** Dashboard HTTP API version. */
    public static final int API_VERSION = 4;
    /** Time an unused dashboard remains available after its benchmark exits. */
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofSeconds(5);
    private static final String API_PREFIX = "/api/v1/";
    private static final String RESOURCE_PREFIX = "/dashboard/";
    private static final String SSE_OWNED_ATTRIBUTE = "sbk.dashboard.sseOwned";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpServer server;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final int retention;
    private final ConcurrentHashMap<String, RunState> runs;
    private final Duration idleTimeout;
    private final Duration heartbeatInterval;
    private final Object lifecycleLock;
    private final AtomicBoolean closed;
    private final CountDownLatch termination;
    private final Map<String, Long> browsers;
    private String activeRunId;
    private ScheduledFuture<?> idleShutdown;
    private boolean shuttingDown;

    /**
     * Creates a dashboard server bound to the supplied address.
     *
     * @param host      local address on which to listen
     * @param port      TCP port
     * @param retention maximum snapshots retained per run
     * @throws IOException if the server cannot bind
     * @throws IllegalArgumentException if retention is not positive
     */
    public DashboardServer(String host, int port, int retention) throws IOException {
        this(host, port, retention, DEFAULT_IDLE_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL);
    }

    /**
     * Creates a dashboard server with configurable lifecycle timings.
     *
     * @param host              local address on which to listen
     * @param port              TCP port
     * @param retention         maximum snapshots retained per run
     * @param idleTimeout       delay before an inactive dashboard without browsers stops
     * @param heartbeatInterval interval used to detect disconnected browser event streams
     * @throws IOException if the server cannot bind
     * @throws IllegalArgumentException if a size or duration is not positive
     */
    DashboardServer(String host, int port, int retention, Duration idleTimeout, Duration heartbeatInterval)
            throws IOException {
        if (retention < 1) {
            throw new IllegalArgumentException("Dashboard retention must be greater than zero");
        }
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("Dashboard idle timeout must be greater than zero");
        }
        if (heartbeatInterval.isZero() || heartbeatInterval.isNegative()) {
            throw new IllegalArgumentException("Dashboard heartbeat interval must be greater than zero");
        }
        this.retention = retention;
        this.idleTimeout = idleTimeout;
        this.heartbeatInterval = heartbeatInterval;
        this.runs = new ConcurrentHashMap<>();
        this.lifecycleLock = new Object();
        this.closed = new AtomicBoolean();
        this.termination = new CountDownLatch(1);
        this.browsers = new ConcurrentHashMap<>();
        this.server = HttpServer.create(new InetSocketAddress(host, port), 32);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .name("sbk-dashboard-idle-monitor").daemon().factory());
        server.setExecutor(executor);
        server.createContext(API_PREFIX, this::handleApi);
        server.createContext("/", this::handleResource);
    }

    /**
     * Starts accepting dashboard connections.
     */
    public void start() {
        server.start();
        synchronized (lifecycleLock) {
            scheduleIdleShutdownIfUnused();
        }
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
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (lifecycleLock) {
            shuttingDown = true;
            cancelIdleShutdown();
        }
        runs.values().forEach(RunState::close);
        server.stop(0);
        scheduler.shutdown();
        executor.close();
        termination.countDown();
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            final String path = exchange.getRequestURI().getPath();
            if ((API_PREFIX + "health").equals(path)) {
                requireMethod(exchange, "GET");
                sendJson(exchange, 200, Map.of("service", "sbk-dashboard", "apiVersion", API_VERSION,
                        "status", "ready"));
                return;
            }
            if ((API_PREFIX + "runs").equals(path)) {
                if ("GET".equals(exchange.getRequestMethod())) {
                    sendJson(exchange, 200, runs.values().stream().map(RunState::view).toList());
                } else {
                    requireMethod(exchange, "POST");
                    final DashboardRun run = MAPPER.readValue(exchange.getRequestBody(), DashboardRun.class);
                    if (run.runId() == null || run.runId().isBlank()) {
                        sendText(exchange, 400, "runId is required", "text/plain; charset=utf-8");
                        return;
                    }
                    final String conflict = register(run);
                    if (conflict != null) {
                        sendText(exchange, 409, conflict, "text/plain; charset=utf-8");
                    } else {
                        sendJson(exchange, 201, run);
                    }
                }
                return;
            }
            if ((API_PREFIX + "browser/connect").equals(path)
                    || (API_PREFIX + "browser/disconnect").equals(path)) {
                requireMethod(exchange, "POST");
                final Map<?, ?> request = MAPPER.readValue(exchange.getRequestBody(), Map.class);
                final String browserId = Objects.toString(request.get("browserId"), "");
                if (browserId.isBlank()) {
                    sendText(exchange, 400, "browserId is required", "text/plain; charset=utf-8");
                    return;
                }
                if (path.endsWith("/connect")) {
                    browserSeen(browserId);
                } else {
                    browserGone(browserId);
                }
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            handleRunApi(exchange, path);
        } catch (MethodNotAllowedException ex) {
            sendText(exchange, 405, ex.getMessage(), "text/plain; charset=utf-8");
        } catch (IllegalArgumentException ex) {
            sendText(exchange, 400, ex.getMessage(), "text/plain; charset=utf-8");
        } catch (Exception ex) {
            sendText(exchange, 500, Objects.toString(ex.getMessage(), ex.getClass().getSimpleName()),
                    "text/plain; charset=utf-8");
        } finally {
            if (!Boolean.TRUE.equals(exchange.getAttribute(SSE_OWNED_ATTRIBUTE))) {
                exchange.close();
            }
        }
    }

    private void handleRunApi(HttpExchange exchange, String path) throws IOException {
        final String relative = path.substring(API_PREFIX.length());
        final String[] elements = relative.split("/");
        if (elements.length != 3 || !"runs".equals(elements[0])) {
            sendText(exchange, 404, "Not found", "text/plain; charset=utf-8");
            return;
        }
        final RunState state = runs.get(elements[1]);
        if (state == null) {
            sendText(exchange, 404, "Unknown dashboard run", "text/plain; charset=utf-8");
            return;
        }
        switch (elements[2]) {
            case "snapshots" -> {
                requireMethod(exchange, "POST");
                final DashboardSnapshot snapshot = MAPPER.readValue(exchange.getRequestBody(),
                        DashboardSnapshot.class);
                if (!state.run.runId().equals(snapshot.runId())) {
                    throw new IllegalArgumentException("Snapshot runId does not match URL");
                }
                if (!benchmarkSeen(state.run.runId())) {
                    sendText(exchange, 409, "Dashboard run lease has expired", "text/plain; charset=utf-8");
                    return;
                }
                state.add(snapshot);
                exchange.sendResponseHeaders(204, -1);
            }
            case "heartbeat" -> {
                requireMethod(exchange, "POST");
                if (!benchmarkSeen(state.run.runId())) {
                    sendText(exchange, 409, "Dashboard run lease has expired", "text/plain; charset=utf-8");
                    return;
                }
                exchange.sendResponseHeaders(204, -1);
            }
            case "complete" -> {
                requireMethod(exchange, "POST");
                state.complete();
                benchmarkCompleted(state.run.runId());
                exchange.sendResponseHeaders(204, -1);
            }
            case "history" -> {
                requireMethod(exchange, "GET");
                sendJson(exchange, 200, state.history());
            }
            case "events" -> {
                requireMethod(exchange, "GET");
                state.events(exchange);
            }
            default -> sendText(exchange, 404, "Not found", "text/plain; charset=utf-8");
        }
    }

    private void handleResource(HttpExchange exchange) throws IOException {
        try {
            requireMethod(exchange, "GET");
            final String path = exchange.getRequestURI().getPath();
            final String resource;
            final String contentType;
            if ("/".equals(path) || "/index.html".equals(path)) {
                resource = RESOURCE_PREFIX + "index.html";
                contentType = "text/html; charset=utf-8";
            } else if ("/app.js".equals(path)) {
                resource = RESOURCE_PREFIX + "app.js";
                contentType = "text/javascript; charset=utf-8";
            } else if ("/style.css".equals(path)) {
                resource = RESOURCE_PREFIX + "style.css";
                contentType = "text/css; charset=utf-8";
            } else {
                sendText(exchange, 404, "Not found", "text/plain; charset=utf-8");
                return;
            }
            try (InputStream input = DashboardServer.class.getResourceAsStream(resource)) {
                if (input == null) {
                    sendText(exchange, 404, "Dashboard resource not found", "text/plain; charset=utf-8");
                    return;
                }
                final byte[] body = input.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
            }
        } catch (MethodNotAllowedException ex) {
            sendText(exchange, 405, ex.getMessage(), "text/plain; charset=utf-8");
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
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String body, String contentType)
            throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String register(DashboardRun run) {
        synchronized (lifecycleLock) {
            if (shuttingDown) {
                return "SBK dashboard is shutting down; retry the benchmark";
            }
            if (activeRunId != null) {
                final RunState active = runs.get(activeRunId);
                final String owner = active == null ? activeRunId
                        : active.run.source() + " run " + active.run.runId();
                return "SBK dashboard port " + server.getAddress().getPort() + " is already serving active " + owner
                        + "; only one SBK, SBM, or SBK-GEM WebLogger benchmark may use a dashboard port at a time. "
                        + "Use '-dashboardport <different-port>' to start another SbkDashboardServerMain";
            }
            if (runs.putIfAbsent(run.runId(), new RunState(run, retention)) != null) {
                return "Dashboard runId already exists: " + run.runId();
            }
            cancelIdleShutdown();
            activeRunId = run.runId();
            scheduleActiveRunExpiry(runs.get(activeRunId));
            return null;
        }
    }

    private boolean benchmarkSeen(String runId) {
        synchronized (lifecycleLock) {
            if (!runId.equals(activeRunId) || shuttingDown) {
                return false;
            }
            final RunState state = runs.get(runId);
            if (state == null || state.completed) {
                return false;
            }
            state.touch();
            scheduleActiveRunExpiry(state);
            return true;
        }
    }

    private void benchmarkCompleted(String runId) {
        synchronized (lifecycleLock) {
            if (runId.equals(activeRunId)) {
                activeRunId = null;
                scheduleIdleShutdownIfUnused();
            }
        }
    }

    private void browserSeen(String browserId) {
        synchronized (lifecycleLock) {
            browsers.put(browserId, System.currentTimeMillis());
            if (activeRunId == null) {
                scheduleIdleShutdownIfUnused();
            }
        }
    }

    private void browserGone(String browserId) {
        synchronized (lifecycleLock) {
            browsers.remove(browserId);
            if (activeRunId == null) {
                scheduleIdleShutdownIfUnused();
            }
        }
    }

    private void scheduleActiveRunExpiry(RunState state) {
        cancelIdleShutdown();
        final long delay = Math.max(1, state.lastActivity + idleTimeout.toMillis() - System.currentTimeMillis());
        idleShutdown = scheduler.schedule(() -> expireActiveRun(state.run.runId()), delay, TimeUnit.MILLISECONDS);
    }

    private void expireActiveRun(String runId) {
        boolean closeNow = false;
        synchronized (lifecycleLock) {
            idleShutdown = null;
            if (!runId.equals(activeRunId) || shuttingDown) {
                return;
            }
            final RunState state = runs.get(runId);
            final long expiry = System.currentTimeMillis() - idleTimeout.toMillis();
            if (state != null && state.lastActivity > expiry) {
                scheduleActiveRunExpiry(state);
                return;
            }
            if (state != null) {
                state.abandon();
            }
            activeRunId = null;
            browsers.entrySet().removeIf(entry -> entry.getValue() <= expiry);
            if (browsers.isEmpty()) {
                shuttingDown = true;
                closeNow = true;
            } else {
                scheduleIdleShutdownIfUnused();
            }
        }
        if (closeNow) {
            close();
        }
    }

    private void scheduleIdleShutdownIfUnused() {
        if (activeRunId != null || shuttingDown) {
            return;
        }
        cancelIdleShutdown();
        final long now = System.currentTimeMillis();
        final long lastBrowserSeen = browsers.values().stream().mapToLong(Long::longValue).max().orElse(now);
        final long delay = Math.max(1, lastBrowserSeen + idleTimeout.toMillis() - now);
        idleShutdown = scheduler.schedule(this::closeIfUnused, delay, TimeUnit.MILLISECONDS);
    }

    private void closeIfUnused() {
        synchronized (lifecycleLock) {
            idleShutdown = null;
            if (activeRunId != null || shuttingDown) {
                return;
            }
            final long expiry = System.currentTimeMillis() - idleTimeout.toMillis();
            browsers.entrySet().removeIf(entry -> entry.getValue() <= expiry);
            if (!browsers.isEmpty()) {
                scheduleIdleShutdownIfUnused();
                return;
            }
            shuttingDown = true;
        }
        close();
    }

    private void cancelIdleShutdown() {
        if (idleShutdown != null) {
            idleShutdown.cancel(false);
            idleShutdown = null;
        }
    }

    private final class RunState {
        private final DashboardRun run;
        private final int retention;
        private final ArrayDeque<DashboardSnapshot> history;
        private final CopyOnWriteArrayList<SseClient> clients;
        private volatile boolean completed;
        private volatile boolean abandoned;
        private volatile long lastActivity;

        private RunState(DashboardRun run, int retention) {
            this.run = run;
            this.retention = retention;
            this.history = new ArrayDeque<>(retention);
            this.clients = new CopyOnWriteArrayList<>();
            this.lastActivity = System.currentTimeMillis();
        }

        private synchronized void add(DashboardSnapshot snapshot) throws IOException {
            if (history.size() == retention) {
                history.removeFirst();
            }
            history.addLast(snapshot);
            final String event = "event: snapshot\ndata: " + MAPPER.writeValueAsString(snapshot) + "\n\n";
            clients.forEach(client -> client.offer(event));
        }

        private synchronized List<DashboardSnapshot> history() {
            return new ArrayList<>(history);
        }

        private void complete() {
            completed = true;
            clients.forEach(client -> client.offer("event: complete\ndata: {\"abandoned\":" + abandoned
                    + "}\n\n"));
        }

        private void abandon() {
            completed = true;
            abandoned = true;
            clients.forEach(client -> client.offer("event: complete\ndata: {\"abandoned\":true}\n\n"));
        }

        private void touch() {
            lastActivity = System.currentTimeMillis();
        }

        private DashboardRunView view() {
            return new DashboardRunView(run, completed, abandoned);
        }

        private void events(HttpExchange exchange) throws IOException {
            exchange.setAttribute(SSE_OWNED_ATTRIBUTE, Boolean.TRUE);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);
            final SseClient client = new SseClient(exchange.getResponseBody(), heartbeatInterval);
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

    private record DashboardRunView(DashboardRun run, boolean completed, boolean abandoned) {
    }

    private static final class SseClient implements AutoCloseable {
        private static final String WAKE_EVENT = "";
        private final OutputStream output;
        private final ArrayBlockingQueue<String> events;
        private final Duration heartbeatInterval;
        private volatile boolean open;

        private SseClient(OutputStream output, Duration heartbeatInterval) {
            this.output = output;
            this.events = new ArrayBlockingQueue<>(1);
            this.heartbeatInterval = heartbeatInterval;
            this.open = true;
        }

        private void offer(String event) {
            if (!events.offer(event)) {
                events.poll();
                events.offer(event);
            }
        }

        private void run() throws IOException {
            output.write("retry: 2000\n\n".getBytes(StandardCharsets.UTF_8));
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
            try {
                output.close();
            } catch (IOException ignored) {
                // The browser may have already closed the connection.
            }
        }
    }

    private static final class MethodNotAllowedException extends IllegalArgumentException {
        private MethodNotAllowedException(String message) {
            super(message);
        }
    }
}

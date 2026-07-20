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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight local HTTP server that stores bounded SBK histories and serves the browser dashboard.
 */
public final class DashboardServer implements AutoCloseable {
    /** Dashboard HTTP API version. */
    public static final int API_VERSION = 1;
    private static final String API_PREFIX = "/api/v1/";
    private static final String RESOURCE_PREFIX = "/dashboard/";
    private static final String SSE_OWNED_ATTRIBUTE = "sbk.dashboard.sseOwned";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpServer server;
    private final ExecutorService executor;
    private final int retention;
    private final ConcurrentHashMap<String, RunState> runs;

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
        if (retention < 1) {
            throw new IllegalArgumentException("Dashboard retention must be greater than zero");
        }
        this.retention = retention;
        this.runs = new ConcurrentHashMap<>();
        this.server = HttpServer.create(new InetSocketAddress(host, port), 32);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext(API_PREFIX, this::handleApi);
        server.createContext("/", this::handleResource);
    }

    /**
     * Starts accepting dashboard connections.
     */
    public void start() {
        server.start();
    }

    /**
     * Returns the actual bound address.
     *
     * @return server address
     */
    public InetSocketAddress getAddress() {
        return server.getAddress();
    }

    @Override
    public void close() {
        runs.values().forEach(RunState::close);
        server.stop(0);
        executor.close();
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
                    runs.computeIfAbsent(run.runId(), ignored -> new RunState(run, retention));
                    sendJson(exchange, 201, run);
                }
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
                state.add(snapshot);
                exchange.sendResponseHeaders(204, -1);
            }
            case "complete" -> {
                requireMethod(exchange, "POST");
                state.complete();
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

    private static final class RunState {
        private final DashboardRun run;
        private final int retention;
        private final ArrayDeque<DashboardSnapshot> history;
        private final CopyOnWriteArrayList<SseClient> clients;
        private volatile boolean completed;

        private RunState(DashboardRun run, int retention) {
            this.run = run;
            this.retention = retention;
            this.history = new ArrayDeque<>(retention);
            this.clients = new CopyOnWriteArrayList<>();
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
            clients.forEach(client -> client.offer("event: complete\ndata: {}\n\n"));
        }

        private DashboardRunView view() {
            return new DashboardRunView(run, completed);
        }

        private void events(HttpExchange exchange) throws IOException {
            exchange.setAttribute(SSE_OWNED_ATTRIBUTE, Boolean.TRUE);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);
            final SseClient client = new SseClient(exchange.getResponseBody());
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

    private record DashboardRunView(DashboardRun run, boolean completed) {
    }

    private static final class SseClient implements AutoCloseable {
        private static final String WAKE_EVENT = "";
        private final OutputStream output;
        private final ArrayBlockingQueue<String> events;
        private volatile boolean open;

        private SseClient(OutputStream output) {
            this.output = output;
            this.events = new ArrayBlockingQueue<>(1);
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
                    final String event = events.take();
                    if (!open) {
                        return;
                    }
                    output.write(event.getBytes(StandardCharsets.UTF_8));
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

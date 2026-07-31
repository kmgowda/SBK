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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import tools.jackson.databind.ObjectMapper;
import io.sbk.system.Printer;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-blocking Local Web Console publisher used by SBK loggers.
 */
public final class WebConsoleClient implements AutoCloseable {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration START_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration LEASE_HEARTBEAT_INTERVAL = Duration.ofSeconds(15);
    private static final String RUNTIME_JVM_ARGS_PROPERTY = "sbk.runtimeJvmArgs";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient;
    private final URI baseUri;
    private final String runId;
    private final ArrayBlockingQueue<WebConsoleSnapshot> pending;
    private final AtomicBoolean closing;
    private final Thread publisherThread;
    private final Duration leaseHeartbeatInterval;
    private final List<WebConsoleLink> runLinks;

    private WebConsoleClient(HttpClient httpClient, URI baseUri, WebConsoleConfig config, WebConsoleRun run,
            Duration leaseHeartbeatInterval) throws IOException, InterruptedException {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
        this.runId = run.runId();
        this.pending = new ArrayBlockingQueue<>(1);
        this.closing = new AtomicBoolean(false);
        this.leaseHeartbeatInterval = leaseHeartbeatInterval;
        this.runLinks = webConsoleLinks(config.host, config.port, runId, localHostname(), localAddresses());
        postJson("/api/v1/runs", run, 201);
        this.publisherThread = Thread.ofVirtual().name("sbk-web-console-publisher-" + runId)
                .start(this::publishLoop);
    }

    /**
     * Connects to a compatible Local Web Console, starting one when configured and necessary.
     *
     * @param config web console configuration
     * @param run    benchmark run metadata
     * @return connected asynchronous client
     * @throws IOException if no compatible web console can be reached
     * @throws InterruptedException if server startup is interrupted
     */
    public static WebConsoleClient connect(WebConsoleConfig config, WebConsoleRun run)
            throws IOException, InterruptedException {
        return connect(config, run, LEASE_HEARTBEAT_INTERVAL);
    }

    static WebConsoleClient connect(WebConsoleConfig config, WebConsoleRun run, Duration leaseHeartbeatInterval)
            throws IOException, InterruptedException {
        if (leaseHeartbeatInterval.isZero() || leaseHeartbeatInterval.isNegative()) {
            throw new IllegalArgumentException("Local Web Console lease heartbeat interval must be greater than zero");
        }
        final URI baseUri = URI.create("http://" + connectionHost(config.host) + ":" + config.port);
        final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        final Health health = health(httpClient, baseUri);
        if (health == Health.INCOMPATIBLE) {
            throw new IOException("Port " + config.port + " is not a compatible SBK Local Web Console");
        }
        if (health == Health.UNAVAILABLE) {
            if (!config.start) {
                throw new IOException("SBK Local Web Console is unavailable and automatic startup is disabled");
            }
            startServer(config);
            waitUntilHealthy(httpClient, baseUri);
        }
        final WebConsoleClient client = new WebConsoleClient(httpClient, baseUri, config, run,
                leaseHeartbeatInterval);
        if (config.open) {
            client.openBrowser();
        }
        return client;
    }

    /**
     * Selects a reachable local address when the server is configured to listen on every network interface.
     * Wildcard addresses are valid bind addresses but are not suitable web console destinations for HTTP clients.
     *
     * @param host configured web console host or bind address
     * @return host used by the Local Web Console publisher and browser URL
     */
    static String connectionHost(String host) {
        return "0.0.0.0".equals(host) ? "127.0.0.1" : host;
    }

    /**
     * Returns the browser URL for this benchmark run.
     *
     * @return Local Web Console URL
     */
    public URI getRunUri() {
        return URI.create(baseUri + "/?run=" + runId);
    }

    /**
     * Returns copy-paste Local Web Console links reachable through the configured bind address.
     *
     * @return local link followed by hostname and available host-address links
     */
    public List<WebConsoleLink> getRunLinks() {
        return new ArrayList<>(runLinks);
    }

    static List<WebConsoleLink> webConsoleLinks(String bindHost, int port, String runId, String hostname,
            List<InetAddress> addresses) {
        final boolean wildcard = "0.0.0.0".equals(bindHost);
        final Map<String, String> hosts = new LinkedHashMap<>();
        hosts.put(connectionHost(bindHost), wildcard ? "Local" : "Configured");
        if (wildcard) {
            if (hostname != null && !hostname.isBlank()) {
                hosts.putIfAbsent(hostname, "Hostname");
            }
            addresses.stream()
                    .filter(Inet4Address.class::isInstance)
                    .filter(address -> !address.isAnyLocalAddress() && !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress() && !address.isMulticastAddress())
                    .sorted(Comparator.comparing(InetAddress::isSiteLocalAddress)
                            .thenComparing(InetAddress::getHostAddress))
                    .forEach(address -> hosts.putIfAbsent(address.getHostAddress(),
                            address.isSiteLocalAddress() ? "Private IP" : "Public IP"));
        }
        final List<WebConsoleLink> links = new ArrayList<>(hosts.size());
        hosts.forEach((host, label) -> links.add(new WebConsoleLink(label, runUri(host, port, runId))));
        return List.copyOf(links);
    }

    /**
     * Offers a snapshot without waiting for network I/O. If publication is behind, the newest snapshot replaces the
     * stale pending snapshot.
     *
     * @param snapshot latest benchmark summary
     */
    public void publish(WebConsoleSnapshot snapshot) {
        if (closing.get() || pending.offer(snapshot)) {
            return;
        }
        pending.poll();
        pending.offer(snapshot);
    }

    @Override
    public void close() {
        if (!closing.compareAndSet(false, true)) {
            return;
        }
        try {
            publisherThread.join(REQUEST_TIMEOUT.toMillis() * 3);
            if (publisherThread.isAlive()) {
                publisherThread.interrupt();
                publisherThread.join(REQUEST_TIMEOUT.toMillis());
            }
            if (publisherThread.isAlive()) {
                Printer.log.warn("SBK Local Web Console publisher did not stop; skipping completion notification "
                        + "to avoid racing an in-flight snapshot (the run lease will expire)");
                return;
            }
            postJson("/api/v1/runs/" + runId + "/complete", Map.of(), 204);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            Printer.log.warn("SBK Local Web Console completion notification failed: {}",
                    Objects.toString(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private void publishLoop() {
        long nextHeartbeat = System.nanoTime() + leaseHeartbeatInterval.toNanos();
        while (!closing.get() || !pending.isEmpty()) {
            try {
                final long heartbeatWait = Math.max(1,
                        TimeUnit.NANOSECONDS.toMillis(nextHeartbeat - System.nanoTime()));
                final WebConsoleSnapshot snapshot = pending.poll(Math.min(250, heartbeatWait), TimeUnit.MILLISECONDS);
                if (snapshot != null) {
                    postJson("/api/v1/runs/" + runId + "/snapshots", snapshot, 204);
                    nextHeartbeat = System.nanoTime() + leaseHeartbeatInterval.toNanos();
                } else if (!closing.get() && System.nanoTime() >= nextHeartbeat) {
                    postJson("/api/v1/runs/" + runId + "/heartbeat", Map.of(), 204);
                    nextHeartbeat = System.nanoTime() + leaseHeartbeatInterval.toNanos();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException ex) {
                Printer.log.warn("SBK Local Web Console publication failed: {}",
                        Objects.toString(ex.getMessage(), ex.getClass().getSimpleName()));
                nextHeartbeat = System.nanoTime() + leaseHeartbeatInterval.toNanos();
            }
        }
    }

    private void postJson(String path, Object body, int expectedStatus) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(MAPPER.writeValueAsBytes(body)))
                .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 409) {
            throw new WebConsoleBusyException(response.body());
        }
        if (response.statusCode() != expectedStatus) {
            throw new IOException("Local Web Console returned HTTP " + response.statusCode() + ": "
                    + response.body());
        }
    }

    private void openBrowser() {
        if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return;
        }
        Thread.ofVirtual().start(() -> {
            try {
                Desktop.getDesktop().browse(getRunUri());
            } catch (IOException | UnsupportedOperationException ex) {
                Printer.log.info("Open the SBK Local Web Console at {}", getRunUri());
            }
        });
    }

    private static Health health(HttpClient client, URI baseUri) {
        final HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v1/health"))
                .timeout(CONNECT_TIMEOUT).GET().build();
        try {
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Health.INCOMPATIBLE;
            }
            final Map<?, ?> values = MAPPER.readValue(response.body(), Map.class);
            return "sbk-web-console".equals(values.get("service"))
                    && Objects.equals(WebConsoleServer.API_VERSION, values.get("apiVersion"))
                    ? Health.AVAILABLE : Health.INCOMPATIBLE;
        } catch (IOException ex) {
            return Health.UNAVAILABLE;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Health.UNAVAILABLE;
        }
    }

    private static void waitUntilHealthy(HttpClient client, URI baseUri) throws IOException, InterruptedException {
        final long deadline = System.nanoTime() + START_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            final Health health = health(client, baseUri);
            if (health == Health.AVAILABLE) {
                return;
            }
            if (health == Health.INCOMPATIBLE) {
                throw new IOException("Another application is using the configured Local Web Console port");
            }
            Thread.sleep(100);
        }
        throw new IOException("SBK Local Web Console did not become ready within "
                + START_TIMEOUT.toSeconds() + " seconds");
    }

    private static void startServer(WebConsoleConfig config) throws IOException {
        final String javaExecutable = resolveJavaExecutable();
        final List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.addAll(runtimeJvmArgs());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(SbkWebConsoleMain.class.getName());
        command.add("-host");
        command.add(config.host);
        command.add("-port");
        command.add(Integer.toString(config.port));
        command.add("-minutes");
        command.add(Integer.toString(config.minutes));
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectInput(ProcessBuilder.Redirect.PIPE);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        builder.start();
    }

    private static List<String> runtimeJvmArgs() {
        final String configured = System.getProperty(RUNTIME_JVM_ARGS_PROPERTY, "");
        if (configured.isBlank()) {
            return List.of();
        }
        final List<String> arguments = new ArrayList<>();
        for (String value : configured.split(",")) {
            final String argument = value.trim();
            if (!argument.isEmpty()) {
                arguments.add(argument);
            }
        }
        return List.copyOf(arguments);
    }

    private static String localHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException ex) {
            return "";
        }
    }

    private static List<InetAddress> localAddresses() {
        final List<InetAddress> addresses = new ArrayList<>();
        try {
            final InetAddress primaryAddress = InetAddress.getLocalHost();
            if (!primaryAddress.isAnyLocalAddress() && !primaryAddress.isLoopbackAddress()
                    && !primaryAddress.isLinkLocalAddress()) {
                return List.of(primaryAddress);
            }
        } catch (IOException ex) {
            // Fall back to interface enumeration when the local hostname cannot be resolved.
        }
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return addresses;
            }
            while (interfaces.hasMoreElements()) {
                final Enumeration<InetAddress> interfaceAddresses = interfaces.nextElement().getInetAddresses();
                while (interfaceAddresses.hasMoreElements()) {
                    addresses.add(interfaceAddresses.nextElement());
                }
            }
        } catch (SocketException ex) {
            return List.of();
        }
        return addresses;
    }

    private static URI runUri(String host, int port, String runId) {
        try {
            return new URI("http", null, host, port, "/", "run=" + runId, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid Local Web Console address: " + host, ex);
        }
    }

    @SuppressFBWarnings(value = "ENV_USE_PROPERTY_INSTEAD_OF_ENV",
            justification = "SBK_JAVA_HOME and JAVA_HOME are documented SBK launcher overrides")
    private static String resolveJavaExecutable() {
        String javaHome = System.getenv("SBK_JAVA_HOME");
        if (javaHome == null || javaHome.isBlank()) {
            javaHome = System.getenv("JAVA_HOME");
        }
        if (javaHome == null || javaHome.isBlank()) {
            javaHome = System.getProperty("java.home");
        }
        final String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe" : "java";
        return Path.of(javaHome).toAbsolutePath().normalize().resolve("bin").resolve(executable).toString();
    }

    private enum Health {
        AVAILABLE,
        UNAVAILABLE,
        INCOMPATIBLE
    }

    /**
     * A labeled, copy-paste URL for one Local Web Console network address.
     *
     * @param label address type, such as hostname or public IP
     * @param uri   complete Local Web Console run URL
     */
    public record WebConsoleLink(String label, URI uri) {
    }

    /** Indicates that another benchmark owns the Local Web Console's single active-run lease. */
    public static final class WebConsoleBusyException extends IOException {
        /**
         * Creates a Local Web Console ownership exception.
         *
         * @param message ownership conflict details returned by the web console server
         */
        private WebConsoleBusyException(String message) {
            super(message);
        }
    }
}

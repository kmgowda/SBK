/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sbk.release;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/** Cross-platform black-box qualification of the installed SBK applications. */
class ReleaseFunctionalTest {
    private static final int MINIMUM_REUSED_WEB_RUNS = 2;
    private static final int HTTP_SUCCESS_MINIMUM = 200;
    private static final int HTTP_SUCCESS_MAXIMUM_EXCLUSIVE = 300;
    private static final String LOOPBACK = "127.0.0.1";

    private final Config config = Config.load();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.startupTimeoutSeconds)).build();
    private final List<Result> results = new ArrayList<>();
    private final List<RunningProcess> processes = new ArrayList<>();
    private DockerFixture dockerFixture;
    private String activeCase;
    private int webPort;

    /** Runs the complete functional release inventory. */
    @Test
    void qualifyInstalledApplications() throws Exception {
        Files.createDirectories(config.workDir);
        Files.createDirectories(config.logDir);
        try {
            if (config.profile.equals("local-docker")) {
                dockerFixture = DockerFixture.start(config, this::runInfrastructureCommand);
            }
            runInventory();
        } catch (Throwable throwable) {
            if (results.stream().noneMatch(result -> !result.passed)) {
                results.add(new Result("release-functional-infrastructure", false, throwable.getMessage()));
            }
            throw throwable;
        } finally {
            stopAllProcesses();
            if (dockerFixture != null) {
                dockerFixture.close();
            }
            writeReports();
        }
        long failed = results.stream().filter(result -> !result.passed).count();
        if (failed != 0) {
            fail("SBK functional release qualification failed: " + (results.size() - failed)
                    + " passed, " + failed + " failed; see " + config.reportDir);
        }
        System.out.println("SBK functional release qualification passed: " + results.size() + " tests");
    }

    private void runInventory() throws Exception {
        caseRun("launcher-sbk", () -> expect(config.sbk, "SBK Version: " + config.version, "-version"));
        caseRun("launcher-sbk-yal", () -> expect(config.sbkYal,
                "SBK-YAL Version: " + config.version, "-version"));
        caseRun("launcher-sbm", () -> expect(config.sbm, "SBM Version: " + config.version, "-version"));
        caseRun("launcher-sbk-gem", () -> expect(config.sbkGem,
                "SBK-GEM Version: " + config.version, "-version"));
        caseRun("launcher-sbk-gem-yal", () -> expect(config.sbkGemYal,
                "SBK-GEM-YAL Version: " + config.version, "-version"));
        caseRun("sbk-invalid-class", () -> reject(config.sbk, "not found|invalid|class",
                "-class", "DoesNotExist", "-writers", "1", "-records", "1"));
        caseRun("grpc-missing-sbm", () -> reject(config.sbk, "requires.*sbm|SBM host",
                "-class", "file", "-writers", "1", "-records", "1", "-out", "GrpcLogger"));
        caseRun("sbk-SystemLogger", () -> fileLogger("SystemLogger"));
        caseRun("sbk-Sl4jLogger", () -> fileLogger("Sl4jLogger"));
        caseRun("sbk-CSVLogger", this::csvLogger);
        caseRun("csv-contract", () -> requireFileContains(config.workDir.resolve("sbk.csv"), "Total"));
        caseRun("sbk-eof-prepare", this::prepareEofFile);
        caseRun("sbk-eof-reader", this::readEofFile);
        caseRun("eof-lifecycle", this::verifyEofLifecycle);
        caseRun("sbk-idle-timeout", this::sbkIdleTimeout);
        caseRun("sbk-timed-idle-disabled", this::sbkTimedIdleDisabled);
        caseRun("sbm-idle-timeout", this::sbmIdleTimeout);
        caseRun("prometheus-endpoint", this::prometheusEndpoint);
        caseRun("sbk-PrometheusLogger", this::prometheusLifecycle);
        caseRun("web-console-contract", this::webConsoleContract);
        caseRun("sbk-WebLogger", this::webLoggerLifecycle);
        caseRun("web-console-reuse", this::webConsoleReuse);
        Path yal = createYalFile();
        caseRun("sbk-yal-SystemLogger", () -> yalLogger(yal, "SystemLogger"));
        caseRun("sbk-yal-Sl4jLogger", () -> yalLogger(yal, "Sl4jLogger"));
        caseRun("sbk-yal-CSVLogger", () -> yalLogger(yal, "CSVLogger"));
        caseRun("sbk-yal-PrometheusLogger", () -> yalPrometheus(yal));
        caseRun("sbk-yal-WebLogger", () -> yalWeb(yal));
        caseRun("sbk-yal-missing", () -> reject(config.sbkYal, "not found|No such file",
                "-f", config.workDir.resolve("missing.yml").toString()));
        caseRun("sbk-yal-invalid", this::invalidYal);
        caseRun("sbm-prometheus-grpc", () -> sbmCase(yal, "SbmPrometheusLogger", false));
        caseRun("sbm-web-grpc", () -> sbmCase(yal, "SbmWebLogger", false));
        caseRun("sbk-yal-GrpcLogger", () -> sbmCase(yal, "SbmPrometheusLogger", true));
        if (config.profile.equals("release") || config.profile.equals("local-docker")) {
            Inventory inventory = dockerFixture == null
                    ? Inventory.load(config.inventory, Map.of(), config.sshPort) : dockerFixture.inventory;
            caseRun("sbk-gem-GemPrometheusLogger",
                    () -> gemLogger(inventory, "GemPrometheusLogger"));
            caseRun("sbk-gem-GemWebLogger", () -> gemLogger(inventory, "GemWebLogger"));
            caseRun("sbk-gem-yal-release", () -> gemYal(inventory));
        } else {
            pass("sbk-gem-external", "not mandatory in " + config.profile
                    + " profile; release profile requires inventory");
            pass("sbk-gem-yal-external", "not mandatory in " + config.profile
                    + " profile; release profile requires inventory");
        }
    }

    private void fileLogger(final String logger) throws Exception {
        expect(config.sbk, "(?s)PerL Shutdown: completed successfully in -records " + config.records
                        + " mode.*SBK Benchmark Shutdown: completed successfully in -records "
                        + config.records + " mode",
                "-class", "file", "-file", config.workDir.resolve("sbk-" + logger + ".dat").toString(),
                "-writers", "1", "-size", config.recordSize, "-records", config.records, "-out", logger);
    }

    private void csvLogger() throws Exception {
        expect(config.sbk, "CSV Logger Shutdown|SBK Benchmark Shutdown", "-class", "file", "-file",
                config.workDir.resolve("sbk-csv.dat").toString(), "-writers", "1", "-size", config.recordSize,
                "-records", config.records, "-out", "CSVLogger", "-csvfile",
                config.workDir.resolve("sbk.csv").toString());
    }

    private void prepareEofFile() throws Exception {
        expect(config.sbk, "Total File Writing", "-class", "file", "-file",
                config.workDir.resolve("sbk-eof.dat").toString(), "-writers", "1",
                "-size", config.recordSize, "-records", config.eofRecords);
    }

    private void readEofFile() throws Exception {
        expect(config.sbk, "EOF|Total File Reading", "-class", "file", "-file",
                config.workDir.resolve("sbk-eof.dat").toString(), "-readers", "1",
                "-size", config.recordSize, "-seconds", config.eofBenchmarkSeconds);
    }

    private void verifyEofLifecycle() throws Exception {
        long start = System.nanoTime();
        readEofFile();
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toSeconds();
        require(elapsed < config.eofMaximumSeconds, "reader took " + elapsed + "s after EOF");
    }

    private void sbkIdleTimeout() throws Exception {
        reject(config.sbk, "SBK Benchmark Shutdown: exited due to -idletimeoutseconds 6",
                "-class", "null", "-readers", "1", "-size", config.recordSize,
                "-records", config.records, "-idletimeoutseconds", "6");
    }

    private void sbmIdleTimeout() throws Exception {
        reject(config.sbm, "SBM Shutdown: exited due to -idletimeoutseconds 6",
                "-class", "File", "-port", Integer.toString(freePort()),
                "-records", config.records, "-idletimeoutseconds", "6");
    }

    private void sbkTimedIdleDisabled() throws Exception {
        expect(config.sbk, "(?s)PerL Shutdown: completed successfully in -seconds 7 mode"
                        + ".*SBK Benchmark Shutdown: completed successfully in -seconds 7 mode",
                "-class", "null", "-readers", "1", "-size", config.recordSize,
                "-seconds", "7", "-idletimeoutseconds", "6");
    }

    private void prometheusEndpoint() throws Exception {
        int port = freePort();
        RunningProcess process = start("prometheus-endpoint", config.sbk,
                "-class", "file", "-file", config.workDir.resolve("sbk-prom-endpoint.dat").toString(),
                "-writers", "1", "-size", config.recordSize, "-seconds", config.smokeBenchmarkSeconds,
                "-out", "PrometheusLogger", "-context", port + "/metrics");
        String metrics = waitForUrl("http://" + LOOPBACK + ":" + port + "/metrics");
        require(Pattern.compile("component=\"(?:sbk|SBK)\"").matcher(metrics).find(),
                "SBK component label is missing");
        require(process.await(config.processTimeoutSeconds, config.killGraceSeconds) == 0,
                "Prometheus benchmark failed; see " + process.log);
    }

    private void prometheusLifecycle() throws Exception {
        expect(config.sbk, "PrometheusLogger Shutdown", "-class", "file", "-file",
                config.workDir.resolve("sbk-prom-lifecycle.dat").toString(), "-writers", "1",
                "-size", config.recordSize, "-records", config.records, "-out", "PrometheusLogger",
                "-context", freePort() + "/metrics");
    }

    private void webConsoleContract() throws Exception {
        require(Files.isExecutable(config.webConsole),
                "missing executable " + config.webConsole);
        webPort = freePort();
        RunningProcess process = start("web-console-contract", config.sbk,
                "-class", "file", "-file", config.workDir.resolve("sbk-web-contract.dat").toString(),
                "-writers", "1", "-size", config.recordSize, "-seconds", config.smokeBenchmarkSeconds,
                "-out", "WebLogger", "-webopen", "false", "-webport", Integer.toString(webPort),
                "-webtimeoutminutes", config.webTimeoutMinutes);
        waitForUrl("http://" + LOOPBACK + ":" + webPort + "/api/v1/health");
        String runs = waitForUrlContaining("http://" + LOOPBACK + ":" + webPort + "/api/v1/runs", "SBK File");
        Files.writeString(config.workDir.resolve("web-runs.json"), runs, StandardCharsets.UTF_8);
        require(process.await(config.processTimeoutSeconds, config.killGraceSeconds) == 0,
                "WebLogger benchmark failed; see " + process.log);
    }

    private void webLoggerLifecycle() throws Exception {
        if (webPort == 0) {
            webPort = freePort();
        }
        expect(config.sbk, "Starting a new SBK Web Console|Using the existing SBK Web Console",
                "-class", "file", "-file", config.workDir.resolve("sbk-web-lifecycle.dat").toString(),
                "-writers", "1", "-size", config.recordSize, "-records", config.records,
                "-out", "WebLogger", "-webopen", "false", "-webport", Integer.toString(webPort),
                "-webtimeoutminutes", config.webTimeoutMinutes);
    }

    private void webConsoleReuse() throws Exception {
        require(webPort != 0, "Web Console contract did not establish a port");
        expect(config.sbk, "Using the existing SBK Web Console", "-class", "file", "-file",
                config.workDir.resolve("sbk-web-reuse.dat").toString(), "-writers", "1",
                "-size", config.recordSize, "-records", config.records, "-out", "WebLogger",
                "-webopen", "false", "-webport", Integer.toString(webPort),
                "-webtimeoutminutes", config.webTimeoutMinutes);
        String runs = get("http://" + LOOPBACK + ":" + webPort + "/api/v1/runs");
        require(count(runs, "\"runId\"") >= MINIMUM_REUSED_WEB_RUNS,
                "Web Console did not retain multiple runs");
    }

    private Path createYalFile() throws IOException {
        Path file = config.workDir.resolve("sbk-release.yml");
        Files.writeString(file, "sbkArgs:\n  class: file\n  file: " + config.workDir.resolve("sbk-yal.dat")
                + "\n  writers: 1\n  size: " + config.recordSize + "\n  records: " + config.records + "\n",
                StandardCharsets.UTF_8);
        return file;
    }

    private void yalLogger(final Path yal, final String logger) throws Exception {
        List<String> args = new ArrayList<>(List.of("-f", yal.toString(), "-out", logger));
        if (logger.equals("CSVLogger")) {
            args.addAll(List.of("-csvfile", config.workDir.resolve("sbk-yal.csv").toString()));
        }
        expect(config.sbkYal, "Merged YAML.*arguments|SBK Benchmark Shutdown", args.toArray(String[]::new));
    }

    private void yalPrometheus(final Path yal) throws Exception {
        int port = freePort();
        RunningProcess process = start("sbk-yal-PrometheusLogger", config.sbkYal,
                "-f", yal.toString(), "-seconds", config.smokeBenchmarkSeconds,
                "-out", "PrometheusLogger", "-context", port + "/metrics");
        waitForUrl("http://" + LOOPBACK + ":" + port + "/metrics");
        require(process.await(config.processTimeoutSeconds, config.killGraceSeconds) == 0,
                "SBK-YAL PrometheusLogger failed; see " + process.log);
    }

    private void yalWeb(final Path yal) throws Exception {
        require(webPort != 0, "Web Console contract did not establish a port");
        expect(config.sbkYal, "Using the existing SBK Web Console", "-f", yal.toString(),
                "-out", "WebLogger", "-webopen", "false", "-webport", Integer.toString(webPort),
                "-webtimeoutminutes", config.webTimeoutMinutes);
    }

    private void invalidYal() throws Exception {
        Path invalid = config.workDir.resolve("invalid.yml");
        Files.writeString(invalid, "sbkArgs:\n  class: [invalid\n", StandardCharsets.UTF_8);
        reject(config.sbkYal, "deserialize|MismatchedInput|parse|mapping|yaml|YAML", "-f", invalid.toString());
    }

    private void sbmCase(final Path yal, final String logger, final boolean yalClient) throws Exception {
        int sbmPort = freePort();
        int outputPort = freePort();
        List<String> args = new ArrayList<>(List.of("-out", logger, "-class", "file",
                "-action", "w", "-port", Integer.toString(sbmPort)));
        String readinessUrl;
        if (logger.equals("SbmPrometheusLogger")) {
            args.addAll(List.of("-context", outputPort + "/metrics"));
            readinessUrl = "http://" + LOOPBACK + ":" + outputPort + "/metrics";
        } else {
            args.addAll(List.of("-webopen", "false", "-webport", Integer.toString(outputPort),
                    "-webtimeoutminutes", config.webTimeoutMinutes));
            readinessUrl = "http://" + LOOPBACK + ":" + outputPort + "/api/v1/health";
        }
        String name = yalClient ? "sbk-yal-GrpcLogger"
                : logger.equals("SbmPrometheusLogger") ? "sbm-prometheus-grpc" : "sbm-web-grpc";
        RunningProcess sbm = start(name + "-sbm", config.sbm, args.toArray(String[]::new));
        waitForUrl(readinessUrl);
        waitForPort(sbmPort, sbm);
        ProcessOutcome client;
        if (yalClient) {
            client = run(name + "-client", config.sbkYal, Map.of(), "-f", yal.toString(),
                    "-out", "GrpcLogger", "-sbm", LOOPBACK, "-sbmport", Integer.toString(sbmPort));
        } else {
            client = run(name + "-client", config.sbk, Map.of(), "-class", "file", "-file",
                    config.workDir.resolve(name + ".dat").toString(), "-writers", "1",
                    "-size", config.recordSize, "-records", config.records, "-out", "GrpcLogger",
                    "-sbm", LOOPBACK, "-sbmport", Integer.toString(sbmPort));
        }
        Thread.sleep(Duration.ofSeconds(config.sbmSettleSeconds));
        sbm.stop(config.killGraceSeconds);
        require(client.exitCode == 0 && contains(client.log, "(?i)GRPC Logger Shutdown")
                        && contains(sbm.log, "SBM .*Logger Started|SBM Started"),
                "SBM/GRPC contract failed; see " + sbm.log + " and " + client.log);
    }

    private void gemLogger(final Inventory inventory, final String logger) throws Exception {
        List<String> args = inventory.baseArgs(config);
        if (logger.equals("GemPrometheusLogger")) {
            args.addAll(List.of("-totalrecords", config.records, "-seconds", config.smokeBenchmarkSeconds));
        } else {
            args.addAll(List.of("-records", config.records, "-totalthroughput", config.totalThroughput));
        }
        args.addAll(List.of("-out", logger));
        if (logger.equals("GemWebLogger")) {
            args.addAll(List.of("-webopen", "false", "-webport", Integer.toString(freePort()),
                    "-webtimeoutminutes", config.webTimeoutMinutes));
        } else {
            args.addAll(List.of("-context", freePort() + "/metrics"));
        }
        String deploymentPattern = inventory.successPattern();
        if (config.profile.equals("local-docker")) {
            deploymentPattern = logger.equals("GemPrometheusLogger")
                    ? "(?s)Immutable runtime archive verified and atomically activated.*" + deploymentPattern
                    : "(?s)already has immutable runtime.*skipping copy.*" + deploymentPattern;
        }
        expect(config.sbkGem, deploymentPattern, inventory.environment, args.toArray(String[]::new));
    }

    private void gemYal(final Inventory inventory) throws Exception {
        Path file = config.workDir.resolve("sbk-gem-release.yml");
        StringBuilder yaml = new StringBuilder("sbkGemArgs:\n  nodes: ").append(inventory.nodes)
                .append("\n  gemuser: ").append(inventory.user).append("\n  knownhosts: ")
                .append(inventory.knownHosts).append("\n  gemport: ").append(inventory.port).append('\n');
        if (!inventory.localhost.isBlank()) {
            yaml.append("  localhost: ").append(inventory.localhost).append('\n');
        }
        yaml.append("  class: file\n  writers: 1\n  size: ").append(config.recordSize)
                .append("\n  totalrecords: ").append(config.records)
                .append("\n  totalthroughput: ").append(config.totalThroughput)
                .append("\n  out: GemPrometheusLogger\n");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        final String expected = config.profile.equals("local-docker")
                ? "(?s)already has immutable runtime.*skipping copy.*" + inventory.successPattern()
                : inventory.successPattern();
        expect(config.sbkGemYal, expected, inventory.environment, "-f", file.toString());
    }

    private void expect(final Path executable, final String regex, final String... args) throws Exception {
        expect(executable, regex, Map.of(), args);
    }

    private void expect(final Path executable, final String regex, final Map<String, String> environment,
                        final String... args) throws Exception {
        require(Files.isExecutable(executable), "missing executable " + executable);
        ProcessOutcome outcome = run(currentCase(), executable, environment, args);
        require(outcome.exitCode == 0, "exit code " + outcome.exitCode + "; see " + outcome.log);
        require(contains(outcome.log, regex), "missing expected output '" + regex + "'; see " + outcome.log);
    }

    private void reject(final Path executable, final String regex, final String... args) throws Exception {
        ProcessOutcome outcome = run(currentCase(), executable, Map.of(), args);
        require(outcome.exitCode != 0, "invalid command exited zero; see " + outcome.log);
        require(contains(outcome.log, "(?i)" + regex),
                "failure did not explain '" + regex + "'; see " + outcome.log);
    }

    private ProcessOutcome run(final String name, final Path executable,
                               final Map<String, String> environment, final String... args) throws Exception {
        RunningProcess process = start(name, executable, environment, args);
        return new ProcessOutcome(process.await(config.processTimeoutSeconds, config.killGraceSeconds), process.log);
    }

    private RunningProcess start(final String name, final Path executable, final String... args) throws IOException {
        return start(name, executable, Map.of(), args);
    }

    private RunningProcess start(final String name, final Path executable,
                                 final Map<String, String> environment, final String... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(List.of(args));
        Path log = config.logDir.resolve(name + ".log");
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().putAll(environment);
        RunningProcess running = new RunningProcess(builder.start(), log);
        processes.add(running);
        return running;
    }

    private ProcessOutcome runInfrastructureCommand(final String name, final Path log,
                                                     final Map<String, String> environment,
                                                     final List<String> command, final long timeout) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().putAll(environment);
        RunningProcess process = new RunningProcess(builder.start(), log);
        processes.add(process);
        return new ProcessOutcome(process.await(timeout, config.killGraceSeconds), log);
    }

    private String waitForUrl(final String url) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < config.startupTimeoutSeconds; attempt++) {
            try {
                return get(url);
            } catch (IOException | InterruptedException exception) {
                last = exception;
                Thread.sleep(Duration.ofSeconds(config.shutdownPollSeconds));
            }
        }
        throw new AssertionError("URL did not become ready: " + url, last);
    }

    private String waitForUrlContaining(final String url, final String expected) throws Exception {
        for (int attempt = 0; attempt < config.startupTimeoutSeconds; attempt++) {
            try {
                String body = get(url);
                if (body.contains(expected)) {
                    return body;
                }
            } catch (IOException ignored) {
                // Retry while the server or run registration is starting.
            }
            Thread.sleep(Duration.ofSeconds(config.shutdownPollSeconds));
        }
        throw new AssertionError("URL response did not contain '" + expected + "': " + url);
    }

    private String get(final String url) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(config.startupTimeoutSeconds)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < HTTP_SUCCESS_MINIMUM
                || response.statusCode() >= HTTP_SUCCESS_MAXIMUM_EXCLUSIVE) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        return response.body();
    }

    private void waitForPort(final int port, final RunningProcess owner) throws Exception {
        for (int attempt = 0; attempt < config.startupTimeoutSeconds; attempt++) {
            require(owner.process.isAlive(), "process exited before port " + port + " became ready");
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(LOOPBACK, port), config.socketConnectTimeoutMillis);
                return;
            } catch (IOException exception) {
                Thread.sleep(Duration.ofSeconds(config.shutdownPollSeconds));
            }
        }
        throw new AssertionError("port did not become ready: " + port);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private void requireFileContains(final Path file, final String text) throws IOException {
        require(Files.isRegularFile(file) && Files.size(file) > 0 && Files.readString(file).contains(text),
                "required content is missing from " + file);
    }

    private static boolean contains(final Path file, final String regex) throws IOException {
        return Pattern.compile(regex, Pattern.DOTALL).matcher(Files.readString(file)).find();
    }

    private static int count(final String text, final String token) {
        int total = 0;
        for (int offset = 0; (offset = text.indexOf(token, offset)) >= 0; offset += token.length()) {
            total++;
        }
        return total;
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void caseRun(final String name, final CheckedRunnable test) {
        activeCase = name;
        try {
            test.run();
            pass(name, config.logDir.resolve(name + ".log").toString());
        } catch (Throwable throwable) {
            results.add(new Result(name, false, throwable.getMessage()));
            System.err.println("FAIL: " + name + " -- " + throwable.getMessage());
        } finally {
            activeCase = null;
        }
    }

    private String currentCase() {
        return activeCase == null ? "release-functional" : activeCase;
    }

    private void pass(final String name, final String detail) {
        results.add(new Result(name, true, detail));
        System.out.println("PASS: " + name);
    }

    private void stopAllProcesses() {
        processes.stream().filter(process -> process.process.isAlive()).forEach(process -> {
            try {
                process.stop(config.killGraceSeconds);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void writeReports() throws IOException {
        Files.createDirectories(config.reportDir);
        StringBuilder tsv = new StringBuilder();
        for (Result result : results) {
            tsv.append(result.name).append('\t').append(result.passed ? "PASS" : "FAIL")
                    .append('\t').append(sanitize(result.detail)).append('\n');
        }
        Files.writeString(config.reportDir.resolve("functional-results.tsv"), tsv, StandardCharsets.UTF_8);
        long failed = results.stream().filter(result -> !result.passed).count();
        String json = "{\n  \"profile\": \"" + json(config.profile) + "\",\n  \"passed\": "
                + (results.size() - failed) + ",\n  \"failed\": " + failed + ",\n  \"status\": \""
                + (failed == 0 ? "PASSED" : "FAILED") + "\"\n}\n";
        Files.writeString(config.reportDir.resolve("functional-summary.json"), json, StandardCharsets.UTF_8);
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static String json(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Result(String name, boolean passed, String detail) { }
    private record ProcessOutcome(int exitCode, Path log) { }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface CommandRunner {
        ProcessOutcome run(String name, Path log, Map<String, String> environment,
                           List<String> command, long timeout) throws Exception;
    }

    private static final class RunningProcess {
        private final Process process;
        private final Path log;

        private RunningProcess(final Process process, final Path log) {
            this.process = process;
            this.log = log;
        }

        private int await(final long timeoutSeconds, final long graceSeconds) throws InterruptedException {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                stop(graceSeconds);
            }
            return process.exitValue();
        }

        private void stop(final long graceSeconds) throws InterruptedException {
            destroyTree(false);
            if (!process.waitFor(graceSeconds, TimeUnit.SECONDS)) {
                destroyTree(true);
                process.waitFor(graceSeconds, TimeUnit.SECONDS);
            }
        }

        private void destroyTree(final boolean forcibly) {
            process.descendants().forEach(handle -> destroy(handle, forcibly));
            destroy(process.toHandle(), forcibly);
        }

        private static void destroy(final ProcessHandle handle, final boolean forcibly) {
            if (handle.isAlive()) {
                if (forcibly) {
                    handle.destroyForcibly();
                } else {
                    handle.destroy();
                }
            }
        }
    }

    private record Config(Path root, String profile, String version, String records,
                          String recordSize, String totalThroughput, long processTimeoutSeconds,
                          int startupTimeoutSeconds, String webTimeoutMinutes, long killGraceSeconds,
                          int shutdownPollSeconds, String eofRecords, String eofBenchmarkSeconds,
                          long eofMaximumSeconds, String smokeBenchmarkSeconds, long sbmSettleSeconds,
                          int dockerNodeCount, int dockerSshReadyAttempts, Path reportDir, Path workDir,
                          Path logDir, Path inventory, Path sbk, Path sbkYal, Path sbm, Path sbkGem,
                          Path sbkGemYal, Path webConsole, int socketConnectTimeoutMillis,
                          String sshPort, String dockerSshUser, String dockerHostAlias,
                          String dockerJdkImage) {
        private static Config load() {
            Path root = Path.of(required("sbk.release.root"));
            Path report = Path.of(required("sbk.release.reportDir"));
            String profile = required("sbk.release.profile");
            String inventory = System.getProperty("sbk.release.inventory", "");
            return new Config(root, profile, required("sbk.release.version"),
                    required("sbk.release.records"), required("sbk.release.recordSize"),
                    required("sbk.release.totalThroughputMBPerSec"), number("processTimeoutSeconds"),
                    (int) number("startupTimeoutSeconds"), required("sbk.release.webTimeoutMinutes"),
                    number("killGraceSeconds"), (int) number("shutdownPollSeconds"),
                    required("sbk.release.eofRecords"), required("sbk.release.eofBenchmarkSeconds"),
                    number("eofMaximumSeconds"), required("sbk.release.smokeBenchmarkSeconds"),
                    number("sbmSettleSeconds"), (int) number("dockerNodeCount"),
                    (int) number("dockerSshReadyAttempts"), report, root.resolve("build/release-qualification"),
                    report.resolve("logs"), inventory.isBlank() ? null : Path.of(inventory),
                    root.resolve("build/install/sbk/bin/sbk"), root.resolve("build/install/sbk/bin/sbk-yal"),
                    root.resolve("sbm/build/install/sbm/bin/sbm"), root.resolve("build/install/sbk/bin/sbk-gem"),
                    root.resolve("build/install/sbk/bin/sbk-gem-yal"),
                    root.resolve("sbk-web-console/build/install/sbk-web-console/bin/sbk-web-console"),
                    (int) number("socketConnectTimeoutMillis"), required("sbk.release.sshPort"),
                    required("sbk.release.dockerSshUser"), required("sbk.release.dockerHostAlias"),
                    required("sbk.release.dockerJdkImage"));
        }

        private static String required(final String name) {
            String value = System.getProperty(name);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing system property " + name);
            }
            return value;
        }

        private static long number(final String name) {
            return Long.parseLong(required("sbk.release." + name));
        }
    }

    private record Inventory(String nodes, String user, String knownHosts, String port,
                             String localhost, Map<String, String> environment) {
        private static Inventory load(final Path file, final Map<String, String> environment,
                                      final String defaultPort) throws IOException {
            require(file != null && Files.isRegularFile(file), "GEM inventory does not exist: " + file);
            Properties properties = new Properties();
            try (var input = Files.newInputStream(file)) {
                properties.load(input);
            }
            return new Inventory(required(properties, "gem.nodes"), required(properties, "gem.user"),
                    required(properties, "gem.knownHosts"), properties.getProperty("gem.port", defaultPort).trim(),
                    properties.getProperty("gem.localhost", "").trim(), Map.copyOf(environment));
        }

        private List<String> baseArgs(final Config config) {
            List<String> args = new ArrayList<>(List.of("-nodes", nodes, "-gemuser", user,
                    "-knownhosts", knownHosts, "-gemport", port, "-class", "file",
                    "-writers", "1", "-size", config.recordSize));
            if (!localhost.isBlank()) {
                args.addAll(List.of("-localhost", localhost));
            }
            return args;
        }

        private String successPattern() {
            int nodeCount = (int) Stream.of(nodes.split("[,\\s]+")).filter(node -> !node.isBlank()).count();
            return "expected nodes: " + nodeCount + "; successful nodes: " + nodeCount
                    + "; failed nodes: 0; maximum SBM registrations: " + nodeCount + "/" + nodeCount;
        }

        private static String required(final Properties properties, final String name) {
            String value = properties.getProperty(name);
            require(value != null && !value.isBlank(), "GEM inventory is missing " + name);
            return value.trim();
        }
    }

    private static final class DockerFixture implements AutoCloseable {
        private final Config config;
        private final CommandRunner runner;
        private final Path directory;
        private final List<String> containers;
        private final long agentPid;
        private final Map<String, String> agentEnvironment;
        private final Inventory inventory;

        private DockerFixture(final Config config, final CommandRunner runner, final Path directory,
                              final List<String> containers, final long agentPid,
                              final Map<String, String> agentEnvironment, final Inventory inventory) {
            this.config = config;
            this.runner = runner;
            this.directory = directory;
            this.containers = containers;
            this.agentPid = agentPid;
            this.agentEnvironment = agentEnvironment;
            this.inventory = inventory;
        }

        private static DockerFixture start(final Config config, final CommandRunner runner) throws Exception {
            Path directory = Files.createTempDirectory(config.workDir, "docker-gem.");
            String suffix = Long.toUnsignedString(System.nanoTime());
            require(config.dockerNodeCount > 0, "local-docker requires at least one GEM node");
            List<String> containers = IntStream.range(0, config.dockerNodeCount)
                    .mapToObj(index -> "sbk-release-gem-" + suffix + "-" + (index + 1))
                    .toList();
            String image = "sbk-release-gem-fixture:" + config.version;
            command(runner, config, directory, Map.of(), "ssh-keygen", "ssh-keygen", "-q", "-t", "ed25519",
                    "-N", "", "-C", "sbk-release-qualification", "-f",
                    directory.resolve("id_ed25519").toString());
            ProcessOutcome agent = command(runner, config, directory, Map.of(), "ssh-agent",
                    "ssh-agent", "-s");
            String agentOutput = Files.readString(agent.log);
            var socketMatcher = Pattern.compile("SSH_AUTH_SOCK=([^;\\r\\n]+);").matcher(agentOutput);
            var pidMatcher = Pattern.compile("SSH_AGENT_PID=([0-9]+);").matcher(agentOutput);
            boolean socketFound = socketMatcher.find();
            boolean pidFound = pidMatcher.find();
            if (!socketFound || !pidFound) {
                if (pidFound) {
                    ProcessHandle.of(Long.parseLong(pidMatcher.group(1))).ifPresent(ProcessHandle::destroy);
                }
                throw new IllegalStateException(
                        "Unable to read release qualification SSH agent environment; see " + agent.log);
            }
            String socket = socketMatcher.group(1);
            long agentPid = Long.parseLong(pidMatcher.group(1));
            Map<String, String> environment = Map.of("SSH_AUTH_SOCK", socket,
                    "SSH_AGENT_PID", Long.toString(agentPid));
            DockerFixture fixture = new DockerFixture(config, runner, directory, containers,
                    agentPid, environment, null);
            try {
                command(runner, config, directory, environment, "ssh-add", "ssh-add",
                        directory.resolve("id_ed25519").toString());
                command(runner, config, directory, environment, "docker-build", "docker", "build",
                        "--build-arg", "SBK_RELEASE_JDK_IMAGE=" + config.dockerJdkImage,
                        "--build-arg", "SBK_RELEASE_SSH_USER=" + config.dockerSshUser,
                        "--build-arg", "SBK_RELEASE_SSH_PORT=" + config.sshPort,
                        "--tag", image, config.root.resolve("scripts/release-gem-docker").toString());
                List<String> nodePorts = new ArrayList<>(config.dockerNodeCount);
                for (int index = 0; index < config.dockerNodeCount; index++) {
                    startContainer(runner, config, directory, environment, image, containers.get(index));
                    ProcessOutcome portResult = command(runner, config, directory, environment,
                            "docker-port-" + index, "docker", "port", containers.get(index),
                            config.sshPort + "/tcp");
                    nodePorts.add(publishedPort(portResult));
                }
                Path knownHosts = directory.resolve("known_hosts");
                Files.writeString(knownHosts, "", StandardCharsets.UTF_8);
                for (int index = 0; index < config.dockerNodeCount; index++) {
                    String port = nodePorts.get(index);
                    Path nodeKnownHosts = directory.resolve("known-hosts-" + index);
                    boolean ready = false;
                    for (int attempt = 0; attempt < config.dockerSshReadyAttempts; attempt++) {
                        ProcessOutcome scan = run(runner, config, directory, environment,
                                "ssh-keyscan-" + index, List.of("ssh-keyscan", "-p", port, LOOPBACK));
                        if (scan.exitCode == 0 && Files.size(scan.log) > 0) {
                            Files.copy(scan.log, nodeKnownHosts, StandardCopyOption.REPLACE_EXISTING);
                            ProcessOutcome probe = run(runner, config, directory, environment,
                                    "ssh-probe-" + index, List.of("ssh", "-p", port, "-o", "BatchMode=yes",
                                            "-o", "UserKnownHostsFile=" + nodeKnownHosts,
                                            config.dockerSshUser + "@" + LOOPBACK,
                                            "java", "-version"));
                            if (probe.exitCode == 0) {
                                ready = true;
                                break;
                            }
                        }
                        Thread.sleep(Duration.ofSeconds(config.shutdownPollSeconds));
                    }
                    require(ready, "Docker GEM fixture node did not become ready: " + LOOPBACK + ":" + port);
                    Files.writeString(knownHosts, Files.readString(nodeKnownHosts), StandardCharsets.UTF_8,
                            StandardOpenOption.APPEND);
                }
                Path inventoryFile = directory.resolve("inventory.properties");
                String nodes = nodePorts.stream().map(port -> LOOPBACK + ":" + port)
                        .collect(Collectors.joining(","));
                Files.writeString(inventoryFile, "gem.nodes=" + nodes + "\ngem.user=" + config.dockerSshUser
                        + "\ngem.knownHosts=" + knownHosts + "\ngem.port=" + config.sshPort
                        + "\ngem.localhost=" + config.dockerHostAlias + "\n", StandardCharsets.UTF_8);
                Inventory inventory = Inventory.load(inventoryFile, environment, config.sshPort);
                System.out.println("SBK local-docker GEM fixture ready: " + config.dockerSshUser + "@{" + nodes
                        + "} (" + config.dockerNodeCount + " clients)");
                return new DockerFixture(config, runner, directory, containers,
                        agentPid, environment, inventory);
            } catch (Throwable throwable) {
                fixture.close();
                throw throwable;
            }
        }

        private static void startContainer(final CommandRunner runner, final Config config,
                                           final Path directory, final Map<String, String> environment,
                                           final String image, final String container) throws Exception {
            command(runner, config, directory, environment, "docker-run-" + container, "docker", "run",
                    "--detach", "--name", container, "--add-host",
                    config.dockerHostAlias + ":host-gateway", "--publish",
                    LOOPBACK + "::" + config.sshPort, "--volume",
                    directory.resolve("id_ed25519.pub") + ":/run/sbk/authorized_key:ro", image);
        }

        private static String publishedPort(ProcessOutcome portResult) throws IOException {
            final String portText = Files.readString(portResult.log).trim();
            final String port = portText.substring(portText.lastIndexOf(':') + 1);
            require(port.matches("[0-9]+"), "Unable to determine fixture SSH port: " + portText);
            return port;
        }

        private static ProcessOutcome command(final CommandRunner runner, final Config config,
                                              final Path directory, final Map<String, String> environment,
                                              final String name, final String... command) throws Exception {
            ProcessOutcome outcome = run(runner, config, directory, environment, name, List.of(command));
            require(outcome.exitCode == 0, name + " failed with exit code " + outcome.exitCode
                    + "; see " + outcome.log);
            return outcome;
        }

        private static ProcessOutcome run(final CommandRunner runner, final Config config,
                                          final Path directory, final Map<String, String> environment,
                                          final String name, final List<String> command) throws Exception {
            return runner.run(name, config.logDir.resolve("docker-" + name + ".log"), environment,
                    command, config.processTimeoutSeconds);
        }

        @Override
        public void close() {
            for (String container : containers) {
                try {
                    runner.run("docker-cleanup", config.logDir.resolve("docker-cleanup-" + container + ".log"),
                            agentEnvironment, List.of("docker", "rm", "--force", container),
                            config.killGraceSeconds);
                } catch (Exception ignored) {
                    // Best-effort cleanup; the original qualification failure remains authoritative.
                }
            }
            try {
                runner.run("ssh-agent-cleanup", config.logDir.resolve("ssh-agent-cleanup.log"),
                        agentEnvironment, List.of("ssh-agent", "-k"), config.killGraceSeconds);
            } catch (Exception ignored) {
                // Fall through to the process-handle cleanup below.
            }
            ProcessHandle.of(agentPid).filter(ProcessHandle::isAlive).ifPresent(ProcessHandle::destroy);
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best-effort cleanup of disposable qualification state.
                    }
                });
            } catch (IOException ignored) {
                // Best-effort cleanup of disposable qualification state.
            }
        }
    }
}

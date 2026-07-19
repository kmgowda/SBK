/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.gem.api.SshResponse;
import io.gem.api.SshSession;
import io.gem.config.GemConfig;
import io.gem.api.GemBenchmark;
import io.gem.api.RemoteResponse;
import io.gem.api.ConnectionConfig;
import io.gem.params.GemParameters;
import io.sbk.api.Benchmark;
import io.sbk.system.Printer;
import io.state.State;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Coordinates remote SBK execution and local SBM lifecycle.
 *
 * <p>Responsibilities:
 * - Establish SSH sessions to all nodes and validate remote Java versions.
 * - Reconcile the expected SBK version on every remote host.
 * - Start SBM locally, then execute SBK remotely and collect results.
 * - Aggregate remote outputs into {@link io.gem.api.RemoteResponse}[] and shutdown cleanly.
 */
final public class SbkGemBenchmark implements GemBenchmark {
    private final Benchmark sbmBenchmark;
    private final GemConfig config;
    private final GemParameters params;
    private final List<String> sbkArgs;
    private final CompletableFuture<RemoteResponse[]> retFuture;
    private final RemoteResponse[] remoteResults;
    private final ExecutorService executor;
    private final SshSession[] nodes;
    private final ConnectionsMap consMap;

    @GuardedBy("this")
    private State state;

    @GuardedBy("this")
    private boolean sbmStarted;

    /**
     * Constructor SbkGemBenchmark is responsible for initializing all values.
     *
     * @param sbmBenchmark  Benchmark
     * @param config        NotNull GemConfig
     * @param params        NotNull GemParameters
     * @param sbkArgs       normalized remote SBK argument tokens
     */
    public SbkGemBenchmark(Benchmark sbmBenchmark, @NotNull GemConfig config, @NotNull GemParameters params,
                           List<String> sbkArgs) {
        this.sbmBenchmark = sbmBenchmark;
        this.config = config;
        this.params = params;
        this.sbkArgs = List.copyOf(sbkArgs);
        this.retFuture = new CompletableFuture<>();
        this.state = State.BEGIN;
        this.sbmStarted = false;
        final ConnectionConfig[] connections = params.getConnections();
        if (config.fork) {
            executor = new ForkJoinPool(connections.length + 10);
        } else {
            executor = Executors.newFixedThreadPool(connections.length + 10);
        }
        this.remoteResults = new RemoteResponse[connections.length];
        this.nodes = new SshSession[connections.length];
        for (int i = 0; i < connections.length; i++) {
            nodes[i] = new SshSession(connections[i], executor);
        }
        this.consMap = new ConnectionsMap(connections);
    }

    @Override
    @Synchronized
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @SuppressWarnings("unchecked")
    public CompletableFuture<RemoteResponse[]> start() throws IOException, InterruptedException, ExecutionException,
            IllegalStateException {
        if (state != State.BEGIN) {
            if (state == State.RUN) {
                Printer.log.warn("SBK GEM Benchmark is already running..");
            } else {
                Printer.log.warn("SBK GEM Benchmark is already shutdown..");
            }
            return retFuture.toCompletableFuture();
        }
        state = State.RUN;
        Printer.log.info("SBK GEM Benchmark Started");
        try {
            return startPreparedBenchmark();
        } catch (IOException | InterruptedException | ExecutionException | RuntimeException ex) {
            shutdown(ex);
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private CompletableFuture<RemoteResponse[]> startPreparedBenchmark() throws IOException, InterruptedException,
            ExecutionException {
        final CompletableFuture<?>[] cfArray = new CompletableFuture[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            cfArray[i] = nodes[i].createSessionAsync(config.remoteTimeoutSeconds);
        }
        final CompletableFuture<Void> connsFuture = CompletableFuture.allOf(cfArray);

        for (int i = 0; i < config.maxIterations && !connsFuture.isDone(); i++) {
            try {
                connsFuture.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for ssh session to remote hosts timeout");
            } catch (ExecutionException ex) {
                throw remoteSessionFailure(ex);
            }
        }
        if (!connsFuture.isDone()) {
            final String errMsg = "SBK-GEM, remote session failed after " + config.maxIterations + " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }
        if (connsFuture.isCompletedExceptionally()) {
            try {
                connsFuture.join();
            } catch (CompletionException ex) {
                throw remoteSessionFailure(ex);
            }
        }
        Printer.log.info("SBK-GEM: Ssh session establishment Success..");

        final CompletableFuture<SshResponse>[] cfResults = new CompletableFuture[nodes.length];
        final String[] javaHomes = prepareRemoteJava();
        final String remoteDir = Paths.get(params.getSbkDir()).getFileName().toString();
        final SbkDeploymentPlan deploymentPlan = planRemoteSbkDeployment(remoteDir, javaHomes);
        if (hasSelectedTarget(deploymentPlan.copyTargets())) {
            copySbkToRemoteTargets(deploymentPlan, remoteDir);
            verifyCopiedSbkVersions(deploymentPlan.copyTargets(), remoteDir, javaHomes);
        } else {
            Printer.log.info("SBK-GEM: SBK version " + config.sbkVersion + " is already available on every host");
        }
        final String[] absoluteSbkCommands = resolveRemoteSbkCommands(remoteDir);

        // start SBM
        synchronized (this) {
            sbmStarted = true;
        }
        sbmBenchmark.start();

        // Start remote SBK instances
        final SshResponse[] sbkResults = new SshResponse[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final List<String> commandTokens = new ArrayList<>(sbkArgs.size() + 1);
            commandTokens.add(absoluteSbkCommands[i]);
            commandTokens.addAll(sbkArgs);
            final String command = RemoteJavaDeployment.launchCommand(javaHomes[i],
                    RemoteSbkDeployment.shellJoin(commandTokens));
            Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() +
                    "' remote SBK command: " + command);
            cfResults[i] = nodes[i].runCommandAsync(command, false, benchmarkTimeoutSeconds());
        }
        final CompletableFuture<Void> sbkFuture = CompletableFuture.allOf(cfResults);
        sbkFuture.whenComplete((ignored, failure) -> {
            if (failure != null) {
                shutdown(unwrapCompletionFailure(failure));
                return;
            }
            for (int i = 0; i < cfResults.length; i++) {
                sbkResults[i] = cfResults[i].join();
            }
            fillSshResults(sbkResults);
            final IOException remoteFailure = remoteCommandFailure(remoteResults);
            if (remoteFailure != null) {
                SbkGem.printRemoteResults(remoteResults, false);
                shutdown(remoteFailure);
            } else {
                shutdown(null);
            }
        });

        return retFuture.toCompletableFuture();
    }

    private long benchmarkTimeoutSeconds() {
        final long benchmarkSeconds = params.getTotalSecondsToRun();
        if (benchmarkSeconds <= 0 || benchmarkSeconds >= Long.MAX_VALUE - config.remoteTimeoutSeconds) {
            return Long.MAX_VALUE;
        }
        return benchmarkSeconds + config.remoteTimeoutSeconds;
    }

    @SuppressWarnings("unchecked")
    private String[] prepareRemoteJava() throws ConnectException, InterruptedException, ExecutionException,
            IOException {
        final int expectedVersion = params.getJavaVersion();
        final String[] javaHomes = new String[nodes.length];
        final boolean[] unresolved = new boolean[nodes.length];
        final CompletableFuture<SshResponse>[] pathProbes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            pathProbes[i] = nodes[i].runCommandAsync(RemoteJavaDeployment.pathProbeCommand(), true,
                    config.remoteTimeoutSeconds);
        }
        waitFor(CompletableFuture.allOf(pathProbes), "remote Java discovery");
        for (int i = 0; i < nodes.length; i++) {
            final SshResponse response = pathProbes[i].get();
            if (RemoteJavaDeployment.hasExpectedVersion(response, expectedVersion)) {
                javaHomes[i] = RemoteJavaDeployment.javaHome(response);
            }
            unresolved[i] = javaHomes[i] == null || javaHomes[i].isBlank();
        }

        final String configuredJavaHome = normalizeRemotePath(params.getJavaDir());
        if (hasSelectedTarget(unresolved)) {
            final String[] destinationJavaHomes = new String[nodes.length];
            final CompletableFuture<SshResponse>[] homeProbes = new CompletableFuture[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                if (unresolved[i]) {
                    destinationJavaHomes[i] = RemoteJavaDeployment.destinationJavaHome(
                            nodes[i].connection.getDir(), configuredJavaHome, expectedVersion);
                    homeProbes[i] = nodes[i].runCommandAsync(
                            RemoteJavaDeployment.homeProbeCommand(destinationJavaHomes[i]), true,
                            config.remoteTimeoutSeconds);
                } else {
                    homeProbes[i] = CompletableFuture.completedFuture(new SshResponse(true));
                }
            }
            waitFor(CompletableFuture.allOf(homeProbes), "remote Java destination checks");
            for (int i = 0; i < nodes.length; i++) {
                if (unresolved[i] && RemoteJavaDeployment.hasExpectedVersion(homeProbes[i].get(), expectedVersion)) {
                    javaHomes[i] = destinationJavaHomes[i];
                    unresolved[i] = false;
                    Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() +
                            "' already has Java " + expectedVersion + " at '" + javaHomes[i] +
                            "'; skipping copy");
                }
            }
        }

        if (hasSelectedTarget(unresolved)) {
            if (!params.isJavaCopy()) {
                throw new InterruptedException("SBK-GEM: Java " + expectedVersion +
                        " is unavailable on one or more nodes and javacopy is false");
            }
            copyJavaToRemoteTargets(unresolved, javaHomes, configuredJavaHome, expectedVersion);
        }

        for (int i = 0; i < nodes.length; i++) {
            Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() + "' will use SBK_JAVA_HOME='" +
                    javaHomes[i] + "'");
        }
        Printer.log.info("SBK-GEM: Matching Java Major Version: " + expectedVersion + " Success..");
        return javaHomes;
    }

    @SuppressWarnings("unchecked")
    private void copyJavaToRemoteTargets(boolean[] copyTargets, String[] javaHomes, String configuredJavaHome,
                                         int expectedVersion) throws IOException, ConnectException,
            InterruptedException, ExecutionException {
        final Path localJavaHome = Paths.get(System.getProperty("java.home")).toAbsolutePath().normalize();
        final Path localJava = localJavaHome.resolve("bin").resolve("java");
        if (!Files.isExecutable(localJava)) {
            throw new IOException("Local Java executable not found at " + localJava);
        }
        final int localVersion = RemoteJavaDeployment.parseMajorVersion(System.getProperty("java.version"));
        if (localVersion != expectedVersion) {
            throw new IOException("Local Java " + localVersion + " cannot provide requested Java " +
                    expectedVersion);
        }

        final Path localFileName = localJavaHome.getFileName();
        if (localFileName == null) {
            throw new IOException("Unable to determine the local Java directory name from " + localJavaHome);
        }
        final String localDirectoryName = localFileName.toString();
        final String[] targets = new String[nodes.length];
        final String[] uploadTargets = new String[nodes.length];
        final String[] parents = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            if (copyTargets[i]) {
                targets[i] = RemoteJavaDeployment.destinationJavaHome(
                        nodes[i].connection.getDir(), configuredJavaHome, expectedVersion);
                if (!isSafeRemoteDirectory(targets[i])) {
                    throw new IOException("Refusing to replace unsafe remote Java directory: " + targets[i]);
                }
                parents[i] = remoteParent(targets[i]);
                uploadTargets[i] = remoteJoin(parents[i], localDirectoryName);
            }
        }

        final Set<Map.Entry<String, String>> visited = new HashSet<>();
        final CompletableFuture<SshResponse>[] prepareFutures = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final Map.Entry<String, String> key = javaTargetKey(i, targets[i]);
            if (!copyTargets[i] || !visited.add(key)) {
                prepareFutures[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                final String command = "rm -rf " + RemoteSbkDeployment.shellQuote(targets[i]) + " " +
                        RemoteSbkDeployment.shellQuote(uploadTargets[i]) + "; mkdir -p " +
                        RemoteSbkDeployment.shellQuote(parents[i]);
                prepareFutures[i] = nodes[i].runCommandAsync(command, true, config.remoteTimeoutSeconds);
            }
        }
        waitFor(CompletableFuture.allOf(prepareFutures), "remote Java directory preparation");
        requireSuccessful(prepareFutures, "Preparing the remote Java directory");

        visited.clear();
        final CompletableFuture<?>[] copyFutures = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final Map.Entry<String, String> key = javaTargetKey(i, targets[i]);
            if (!copyTargets[i] || !visited.add(key)) {
                copyFutures[i] = CompletableFuture.completedFuture(null);
            } else {
                copyFutures[i] = nodes[i].copyDirectoryAsync(localJavaHome.toString(), parents[i],
                        config.remoteTimeoutSeconds);
            }
        }
        waitFor(CompletableFuture.allOf(copyFutures), "Java runtime copy");

        visited.clear();
        final CompletableFuture<SshResponse>[] moveFutures = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final Map.Entry<String, String> key = javaTargetKey(i, targets[i]);
            if (!copyTargets[i] || targets[i].equals(uploadTargets[i]) || !visited.add(key)) {
                moveFutures[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                final String command = "mv " + RemoteSbkDeployment.shellQuote(uploadTargets[i]) + " " +
                        RemoteSbkDeployment.shellQuote(targets[i]);
                moveFutures[i] = nodes[i].runCommandAsync(command, true, config.remoteTimeoutSeconds);
            }
        }
        waitFor(CompletableFuture.allOf(moveFutures), "remote Java installation");
        requireSuccessful(moveFutures, "Installing the remote Java runtime");

        final CompletableFuture<SshResponse>[] verificationFutures = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            if (copyTargets[i]) {
                verificationFutures[i] = nodes[i].runCommandAsync(
                        RemoteJavaDeployment.homeProbeCommand(targets[i]), true, config.remoteTimeoutSeconds);
            } else {
                verificationFutures[i] = CompletableFuture.completedFuture(new SshResponse(true));
            }
        }
        waitFor(CompletableFuture.allOf(verificationFutures), "copied Java verification");
        for (int i = 0; i < nodes.length; i++) {
            if (copyTargets[i]) {
                if (!RemoteJavaDeployment.hasExpectedVersion(verificationFutures[i].get(), expectedVersion)) {
                    throw new InterruptedException("SBK-GEM: Copied Java verification failed on host " +
                            nodes[i].connection.getHost());
                }
                javaHomes[i] = targets[i];
            }
        }
    }

    private Map.Entry<String, String> javaTargetKey(int index, String target) {
        return Map.entry(nodes[index].connection.getHost().toLowerCase(), target == null ? "" :
                target.toLowerCase());
    }

    private static String normalizeRemotePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String remoteParent(String path) {
        final int separator = path.lastIndexOf('/');
        if (separator < 0) {
            return ".";
        }
        return separator == 0 ? "/" : path.substring(0, separator);
    }

    private static String remoteJoin(String parent, String child) {
        return "/".equals(parent) ? parent + child : parent + "/" + child;
    }

    private static boolean isSafeRemoteDirectory(String path) {
        return path != null && !path.isBlank() && !"/".equals(path) && !".".equals(path) && !"..".equals(path);
    }

    private static void requireSuccessful(CompletableFuture<SshResponse>[] futures, String operation)
            throws InterruptedException, ExecutionException {
        for (CompletableFuture<SshResponse> future : futures) {
            if (future.get().returnCode != 0) {
                throw new InterruptedException("SBK-GEM: " + operation + " failed");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private SbkDeploymentPlan planRemoteSbkDeployment(String remoteDir, String[] javaHomes) throws ConnectException,
            InterruptedException,
            ExecutionException {
        final boolean[] copyTargets = new boolean[nodes.length];
        final boolean[] deleteTargets = new boolean[nodes.length];

        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final String remoteCommand = nodes[i].connection.getDir() + "/" + remoteDir + "/" +
                    params.getSbkCommand();
            final String command = RemoteJavaDeployment.environmentPrefix(javaHomes[i]) +
                    RemoteSbkDeployment.versionProbeCommand(remoteCommand);
            probes[i] = nodes[i].runCommandAsync(command, true, config.remoteTimeoutSeconds);
        }
        waitFor(CompletableFuture.allOf(probes), "remote SBK version checks");

        for (int i = 0; i < nodes.length; i++) {
            final SshResponse response = probes[i].get();
            if (RemoteSbkDeployment.hasExpectedVersion(response, config.sbkVersion)) {
                Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() + "' already has SBK version " +
                        config.sbkVersion + "; skipping copy");
                continue;
            }

            if (response == null || response.returnCode != 0 && response.returnCode != 127) {
                final String remoteError = response == null ? "no response" :
                        response.errOutputStream.toString().trim();
                final String errMsg = "SBK-GEM: Host '" + nodes[i].connection.getHost() +
                        "' SBK version probe failed" +
                        (response == null ? "" : " with return code " + response.returnCode) +
                        (remoteError.isEmpty() ? "" : ": " + remoteError);
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }

            if (!params.isCopy()) {
                final String errMsg = "SBK-GEM: Host '" + nodes[i].connection.getHost() +
                        "' does not have expected SBK version " + config.sbkVersion +
                        " and copying is disabled";
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }

            copyTargets[i] = RemoteSbkDeployment.requiresCopy(true, response, config.sbkVersion);
            deleteTargets[i] = RemoteSbkDeployment.requiresDeleteBeforeCopy(params.isDelete(), response,
                    config.sbkVersion);
            if (response.returnCode == 127) {
                Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() +
                        "' has no remote SBK executable; scheduling copy");
            } else if (deleteTargets[i]) {
                Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() +
                        "' has a mismatched SBK version; scheduling deletion and copy");
            } else {
                Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() +
                        "' has a mismatched SBK version; scheduling copy without deletion");
            }
        }
        return new SbkDeploymentPlan(copyTargets, deleteTargets);
    }

    @SuppressWarnings("unchecked")
    private void verifyCopiedSbkVersions(boolean[] copyTargets, String remoteDir, String[] javaHomes)
            throws ConnectException, InterruptedException, ExecutionException {
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            if (copyTargets[i]) {
                final String remoteCommand = nodes[i].connection.getDir() + "/" + remoteDir + "/" +
                        params.getSbkCommand();
                final String command = RemoteJavaDeployment.environmentPrefix(javaHomes[i]) +
                        RemoteSbkDeployment.versionProbeCommand(remoteCommand);
                probes[i] = nodes[i].runCommandAsync(command, true, config.remoteTimeoutSeconds);
            } else {
                probes[i] = CompletableFuture.completedFuture(new SshResponse(true));
            }
        }
        waitFor(CompletableFuture.allOf(probes), "copied SBK version verification");
        for (int i = 0; i < nodes.length; i++) {
            if (copyTargets[i] && !RemoteSbkDeployment.hasExpectedVersion(probes[i].get(), config.sbkVersion)) {
                final String errMsg = "SBK-GEM: Copied SBK version verification failed on host '" +
                        nodes[i].connection.getHost() + "'; enable -delete true when replacing an existing " +
                        "installation";
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }
        }
        Printer.log.info("SBK-GEM: Copied SBK version " + config.sbkVersion + " verification Success..");
    }

    @SuppressWarnings("unchecked")
    private String[] resolveRemoteSbkCommands(String remoteDir) throws ConnectException, InterruptedException,
            ExecutionException {
        final String relativeCommand = remoteJoin(remoteDir, params.getSbkCommand());
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final String command = RemoteSbkDeployment.executablePathProbeCommand(
                    nodes[i].connection.getDir(), relativeCommand);
            probes[i] = nodes[i].runCommandAsync(command, true, config.remoteTimeoutSeconds);
        }
        waitFor(CompletableFuture.allOf(probes), "remote SBK executable discovery");

        final String[] commands = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final SshResponse response = probes[i].get();
            commands[i] = RemoteSbkDeployment.absoluteExecutablePath(response);
            if (commands[i] == null) {
                final String remoteError = response.errOutputStream.toString().trim();
                final String errMsg = "SBK-GEM: Unable to locate an executable SBK command on host '" +
                        nodes[i].connection.getHost() + "' under '" + nodes[i].connection.getDir() + "'" +
                        (remoteError.isEmpty() ? "" : ": " + remoteError);
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }
            Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() +
                    "' verified remote SBK executable: " + commands[i]);
        }
        return commands;
    }

    @SuppressWarnings("unchecked")
    private void copySbkToRemoteTargets(SbkDeploymentPlan deploymentPlan, String remoteDir) throws ConnectException,
            InterruptedException, ExecutionException {
        final boolean[] copyTargets = deploymentPlan.copyTargets();
        if (hasSelectedTarget(deploymentPlan.deleteTargets())) {
            if (!remoteSbkDirectoryDelete(deploymentPlan.deleteTargets(), remoteDir)) {
                final String errMsg = "SBK-GEM: Removing mismatched remote SBK directory '" + remoteDir +
                        "' failed";
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }
            Printer.log.info("SBK-GEM: Removing mismatched remote SBK directories Success..");
        } else {
            Printer.log.info("SBK-GEM: No mismatched remote SBK directory selected for deletion");
        }

        final CompletableFuture<SshResponse>[] mkdirFutures = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (!copyTargets[i] || consMap.isVisited(nodes[i].connection)) {
                mkdirFutures[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                consMap.visit(nodes[i].connection);
                final String command = "mkdir -p " + RemoteSbkDeployment.shellQuote(nodes[i].connection.getDir());
                mkdirFutures[i] = nodes[i].runCommandAsync(command, true, config.remoteTimeoutSeconds);
            }
        }
        waitFor(CompletableFuture.allOf(mkdirFutures), "remote directory creation");
        for (CompletableFuture<SshResponse> mkdirFuture : mkdirFutures) {
            if (mkdirFuture.get().returnCode != 0) {
                throw new InterruptedException("SBK-GEM: Creating a remote SBK directory failed");
            }
        }
        Printer.log.info("SBK-GEM: Creating selected remote directories Success..");

        final CompletableFuture<?>[] copyFutures = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (!copyTargets[i] || consMap.isVisited(nodes[i].connection)) {
                copyFutures[i] = CompletableFuture.completedFuture(null);
            } else {
                consMap.visit(nodes[i].connection);
                copyFutures[i] = nodes[i].copyDirectoryAsync(params.getSbkDir(), nodes[i].connection.getDir(),
                        config.remoteTimeoutSeconds);
            }
        }
        waitFor(CompletableFuture.allOf(copyFutures), "SBK copy");
        Printer.log.info("SBK-GEM: Copying SBK application to selected remote hosts Success..");
    }

    @SuppressWarnings("unchecked")
    private boolean remoteSbkDirectoryDelete(boolean[] deleteTargets, String remoteDir)
            throws InterruptedException, ConnectException, ExecutionException {
        final CompletableFuture<SshResponse>[] deleteFutures = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (!deleteTargets[i] || consMap.isVisited(nodes[i].connection)) {
                deleteFutures[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                consMap.visit(nodes[i].connection);
                final String sbkDirectory = nodes[i].connection.getDir() + "/" + remoteDir;
                deleteFutures[i] = nodes[i].runCommandAsync("rm -rf " +
                                RemoteSbkDeployment.shellQuote(sbkDirectory), true,
                        config.remoteTimeoutSeconds);
            }
        }
        waitFor(CompletableFuture.allOf(deleteFutures), "remote SBK directory deletion");
        for (CompletableFuture<SshResponse> deleteResult : deleteFutures) {
            if (deleteResult.get().returnCode != 0) {
                return false;
            }
        }
        return true;
    }

    private void waitFor(CompletableFuture<?> future, String operation) throws InterruptedException,
            ExecutionException {
        for (int i = 0; i < config.maxIterations && !future.isDone(); i++) {
            try {
                future.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for " + operation + " timeout");
            }
        }
        if (!future.isDone()) {
            final String errMsg = "SBK-GEM: " + operation + " timed out after " + config.maxIterations +
                    " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }
        future.get();
    }

    private static boolean hasSelectedTarget(boolean[] selected) {
        for (boolean value : selected) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    private static Throwable unwrapCompletionFailure(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) &&
                cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    static IOException remoteCommandFailure(RemoteResponse[] results) {
        final StringBuilder failures = new StringBuilder();
        for (RemoteResponse result : results) {
            if (result.returnCode != 0) {
                if (!failures.isEmpty()) {
                    failures.append(", ");
                }
                failures.append(result.host).append(" returned ").append(result.returnCode);
            }
        }
        return failures.isEmpty() ? null : new IOException("SBK-GEM: Remote SBK execution failed: " + failures);
    }

    @SuppressWarnings("unchecked")
    private boolean remoteDirectoryDelete(boolean[] deleteTargets) throws InterruptedException, ConnectException,
            ExecutionException {
        final CompletableFuture<SshResponse>[] rmCfArray = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (!deleteTargets[i] || consMap.isVisited(nodes[i].connection)) {
                rmCfArray[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                consMap.visit(nodes[i].connection);
                rmCfArray[i] = nodes[i].runCommandAsync("rm -rf " +
                                RemoteSbkDeployment.shellQuote(nodes[i].connection.getDir()),
                        true, config.remoteTimeoutSeconds );
            }
        }
        final CompletableFuture<Void> rmFuture = CompletableFuture.allOf(rmCfArray);
        waitFor(rmFuture, "remote directory deletion");
        for (CompletableFuture<SshResponse> rmResult : rmCfArray) {
            if (rmResult.get().returnCode != 0) {
                return false;
            }
        }
        return true;
    }

    @Synchronized
    @SuppressWarnings("unchecked")
    private void fillSshResults(SshResponse[] responseStreams) {
        final ConnectionConfig[] connections = params.getConnections();
        for (int i = 0; i < remoteResults.length; i++) {
            remoteResults[i] = new RemoteResponse(responseStreams[i].returnCode, responseStreams[i].stdOutputStream.toString(),
                    responseStreams[i].errOutputStream.toString(), connections[i].getHost());
        }
    }

    /**
     * Shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     * @param ex Throwable exception
     */
    @Synchronized
    private void shutdown(Throwable ex) {
        if (state != State.END) {
            state = State.END;
            if (params.isDeleteAfter()) {
                try {
                    final boolean[] deleteTargets = new boolean[nodes.length];
                    Arrays.fill(deleteTargets, true);
                    remoteDirectoryDelete(deleteTargets);
                } catch (InterruptedException | ConnectException | ExecutionException rmEx) {
                    rmEx.printStackTrace();
                }
            }
            for (SshSession node : nodes) {
                node.stop();
            }
            if (sbmStarted) {
                sbmBenchmark.stop();
                sbmStarted = false;
            }
            executor.shutdown();
            if (ex != null) {
                Printer.log.warn("SBK GEM Benchmark Shutdown with Exception " + ex);
                retFuture.completeExceptionally(ex);
            } else {
                Printer.log.info("SBK GEM Benchmark Shutdown");
                retFuture.complete(remoteResults);
            }
        }
    }

    @Override
    public void stop() {
        shutdown(null);
    }

    private static IOException remoteSessionFailure(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) &&
                cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof IOException ioException) {
            return ioException;
        }
        return new IOException("SBK-GEM: Remote SSH session establishment failed: " + cause.getMessage(), cause);
    }

    private record SbkDeploymentPlan(boolean[] copyTargets, boolean[] deleteTargets) {
    }

    /**
     * Tracks visited (host, remoteDir) combinations to avoid duplicate operations
     * when multiple connections point to the same remote target.
     */
    private final static class ConnectionsMap {
        private final Map<Map.Entry<String, String>, Boolean> kMap;

        public ConnectionsMap(@NotNull ConnectionConfig[] conn) {
            this.kMap = new HashMap<>();
            for (ConnectionConfig connectionConfig : conn) {
                this.kMap.put(Map.entry(connectionConfig.getHost().toLowerCase(), connectionConfig.getDir().toLowerCase()), false);
            }
        }

        void reset() {
            this.kMap.keySet().forEach(k -> this.kMap.put(k, false));
        }

        void visit(@NotNull ConnectionConfig conn) {
            this.kMap.put(Map.entry(conn.getHost().toLowerCase(), conn.getDir().toLowerCase()), true);
        }

        boolean isVisited(@NotNull ConnectionConfig conn) {
            return this.kMap.get(Map.entry(conn.getHost().toLowerCase(), conn.getDir().toLowerCase()));
        }
    }
}

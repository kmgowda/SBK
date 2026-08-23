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
import io.gem.api.ConnectionConfig;
import io.gem.api.GemBenchmark;
import io.gem.api.RemoteExecutionStatus;
import io.gem.api.RemoteResponse;
import io.gem.api.SshCommandException;
import io.gem.api.SshResponse;
import io.gem.api.SshSession;
import io.gem.config.GemConfig;
import io.gem.params.GemParameters;
import io.perl.api.BenchmarkTermination;
import io.perl.config.PerlConfig;
import io.sbk.config.ExitCode;
import io.sbk.system.Printer;
import io.sbk.utils.SbkUtils;
import io.sbm.api.impl.SbmBenchmark;
import io.state.State;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

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
    private static final GemConfig DEFAULT_CONFIG = loadDefaultConfig();
    private final SbmBenchmark sbmBenchmark;
    private final GemConfig config;
    private final GemParameters params;
    private final List<List<String>> sbkArgsByNode;
    private final CompletableFuture<RemoteResponse[]> retFuture;
    private final RemoteResponse[] remoteResults;
    private final ExecutorService executor;
    private final SshSession[] nodes;
    private final ConnectionsMap consMap;
    private final String runtimeLeaseRunId;
    private final boolean[] runtimeLeaseLaunched;

    @GuardedBy("this")
    private State state;

    @GuardedBy("this")
    private boolean sbmStarted;

    private CompletableFuture<Void> sbmCompletion;
    private volatile boolean remoteCommandsCompleted;
    private RuntimeDeployment runtimeDeployment;

    /**
     * Constructor SbkGemBenchmark is responsible for initializing all values.
     *
     * @param sbmBenchmark  embedded SBM benchmark
     * @param config        NotNull GemConfig
     * @param params        NotNull GemParameters
     * @param sbkArgsByNode normalized remote SBK argument tokens for each node
     * @throws IllegalArgumentException when the argument-set count does not match the connection count
     */
    public SbkGemBenchmark(SbmBenchmark sbmBenchmark, @NotNull GemConfig config, @NotNull GemParameters params,
                           List<List<String>> sbkArgsByNode) {
        this.sbmBenchmark = sbmBenchmark;
        this.config = config;
        this.params = params;
        this.sbkArgsByNode = sbkArgsByNode.stream().map(List::copyOf).toList();
        this.retFuture = new CompletableFuture<>();
        this.state = State.BEGIN;
        this.sbmStarted = false;
        this.sbmCompletion = null;
        this.remoteCommandsCompleted = false;
        this.runtimeDeployment = null;
        this.runtimeLeaseRunId = UUID.randomUUID().toString();
        final ConnectionConfig[] connections = params.getConnections();
        if (this.sbkArgsByNode.size() != connections.length) {
            throw new IllegalArgumentException("Remote SBK argument count must match the connection count");
        }
        if (config.fork) {
            executor = new ForkJoinPool(connections.length + config.executorThreadReserve);
        } else {
            executor = Executors.newFixedThreadPool(connections.length + config.executorThreadReserve);
        }
        this.remoteResults = new RemoteResponse[connections.length];
        this.nodes = new SshSession[connections.length];
        this.runtimeLeaseLaunched = new boolean[connections.length];
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
            shutdown(ex, BenchmarkTermination.INTERNAL_FAILURE);
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

        final CompletableFuture<RemoteResponse>[] cfResults = new CompletableFuture[nodes.length];
        final String[] absoluteConnectionDirs = resolveRemoteConnectionDirectories();
        final DeploymentPlatform platform = verifyHomogeneousDeploymentPlatform();
        runtimeDeployment = prepareRuntimeDeployment(absoluteConnectionDirs, platform);
        final String[] javaHomes = runtimeDeployment.javaHomes();
        final String[] absoluteSbkCommands = runtimeDeployment.sbkCommands();

        // start SBM
        synchronized (this) {
            sbmStarted = true;
        }
        sbmCompletion = sbmBenchmark.start();
        sbmCompletion.whenComplete((ignored, failure) -> {
            if (failure != null) {
                final Throwable cause = unwrapCompletionFailure(failure);
                Printer.log.warn("SBK-GEM: Embedded SBM terminated with a benchmark failure: {}",
                        cause.getMessage());
                shutdown(cause, BenchmarkTermination.INTERNAL_FAILURE);
            }
        });

        // Start remote SBK instances
        for (int i = 0; i < nodes.length; i++) {
            final List<String> sbkArgs = sbkArgsByNode.get(i);
            final List<String> commandTokens = new ArrayList<>(sbkArgs.size() + 1);
            commandTokens.add(absoluteSbkCommands[i]);
            commandTokens.addAll(sbkArgs);
            final String command = RemoteJavaDeployment.launchCommand(javaHomes[i],
                    RemoteSbkDeployment.shellJoin(commandTokens));
            final String managedCommand = RemoteRuntimeLifecycle.launchCommand(
                    runtimeDeployment.leasePaths()[i], runtimeDeployment.releaseCommands()[i], command);
            final String redactedCommand = RemoteJavaDeployment.launchCommand(javaHomes[i],
                    RemoteSbkDeployment.shellJoin(List.of(
                            SbkUtils.redactSensitiveOptionValues(
                                    commandTokens.toArray(String[]::new)))));
            Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() +
                    "' remote SBK command: " + redactedCommand);
            final String host = nodes[i].connection.getHost();
            final CompletableFuture<SshResponse> commandFuture;
            try {
                commandFuture = nodes[i].runCommandAsync(managedCommand, true, benchmarkTimeoutSeconds());
                runtimeLeaseLaunched[i] = true;
            } catch (ConnectException ex) {
                cfResults[i] = CompletableFuture.completedFuture(remoteCommandResult(host, null, ex));
                final RemoteResponse result = cfResults[i].join();
                sbmBenchmark.abortPendingRegistrations(result.failureMessage);
                continue;
            }
            cfResults[i] = commandFuture.handle((response, failure) ->
                    remoteCommandResult(host, response, failure));
            cfResults[i].thenAccept(result -> {
                if (result.status != RemoteExecutionStatus.SUCCESS) {
                    final int aborted = sbmBenchmark.abortPendingRegistrations(result.failureMessage);
                    if (aborted > 0) {
                        Printer.log.error("SBK-GEM: Aborted {} remote SBK client(s) waiting at the SBM " +
                                "coordinated-start barrier after host '{}' failed", aborted, result.host);
                    }
                }
            });
        }
        Printer.log.info("SBK-GEM: Remote SBK commands launched on {} host(s); waiting for SBM client " +
                        "registration ({}/{})", nodes.length, sbmBenchmark.getMaximumRegisteredClients(),
                nodes.length);
        CompletableFuture.runAsync(() -> {
            try {
                final long timeoutNanos = TimeUnit.SECONDS.toNanos(config.sbmRegistrationTimeoutSeconds);
                final long progressIntervalNanos = TimeUnit.SECONDS.toNanos(
                        PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS);
                final long startNanos = System.nanoTime();
                boolean coordinatedStart = false;
                long elapsedNanos = 0;
                while (!coordinatedStart && sbmBenchmark.getRegistrationFailure() == null &&
                        elapsedNanos < timeoutNanos) {
                    coordinatedStart = sbmBenchmark.awaitCoordinatedStart(
                            Math.min(progressIntervalNanos, timeoutNanos - elapsedNanos),
                            TimeUnit.NANOSECONDS);
                    elapsedNanos = System.nanoTime() - startNanos;
                    if (!coordinatedStart && sbmBenchmark.getRegistrationFailure() == null &&
                            elapsedNanos < timeoutNanos) {
                        Printer.log.info("SBK-GEM: Waiting for remote SBK clients to register with SBM " +
                                        "({}/{}); elapsed {} seconds", sbmBenchmark.getMaximumRegisteredClients(),
                                nodes.length, TimeUnit.NANOSECONDS.toSeconds(elapsedNanos));
                    }
                }
                if (coordinatedStart) {
                    Printer.log.info("SBK-GEM: All remote SBK clients registered with SBM ({}/{}); benchmark " +
                                    "is running. First performance results are expected after the {}-second " +
                                    "reporting interval", sbmBenchmark.getMaximumRegisteredClients(), nodes.length,
                            PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS);
                } else if (sbmBenchmark.getRegistrationFailure() == null) {
                    final String failure = "SBK-GEM: SBM coordinated start timed out after " +
                            config.sbmRegistrationTimeoutSeconds + " seconds; registered " +
                            sbmBenchmark.getMaximumRegisteredClients() + " of " + nodes.length + " remote clients";
                    Printer.log.error(failure);
                    sbmBenchmark.abortPendingRegistrations(failure);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                final String failure = "SBK-GEM: Interrupted while waiting for remote clients to register with SBM";
                Printer.log.error(failure, ex);
                sbmBenchmark.abortPendingRegistrations(failure);
            }
        }, executor);
        final CompletableFuture<Void> sbkFuture = CompletableFuture.allOf(cfResults);
        sbkFuture.whenComplete((ignored, failure) -> {
            for (int i = 0; i < cfResults.length; i++) {
                if (cfResults[i].isCompletedExceptionally()) {
                    remoteResults[i] = remoteCommandResult(nodes[i].connection.getHost(), null,
                            completionFailure(cfResults[i]));
                } else {
                    remoteResults[i] = cfResults[i].join();
                }
            }
            remoteCommandsCompleted = true;
            final IOException remoteFailure = remoteCommandFailure(remoteResults);
            if (remoteFailure != null) {
                shutdown(remoteFailure, BenchmarkTermination.INTERNAL_FAILURE);
            } else if (failure != null) {
                shutdown(unwrapCompletionFailure(failure), BenchmarkTermination.INTERNAL_FAILURE);
            } else {
                shutdown(null, BenchmarkTermination.configured(
                        params.getTotalSecondsToRun(), params.getTotalRecords()));
            }
        });

        return retFuture.toCompletableFuture();
    }

    private long benchmarkTimeoutSeconds() {
        final long benchmarkSeconds = params.getTotalSecondsToRun();
        if (config.remoteTimeoutSeconds >= Long.MAX_VALUE - config.runtimeManagementLockTimeoutSeconds) {
            return Long.MAX_VALUE;
        }
        final long shutdownAllowance = config.remoteTimeoutSeconds + config.runtimeManagementLockTimeoutSeconds;
        if (benchmarkSeconds <= 0 || benchmarkSeconds >= Long.MAX_VALUE - shutdownAllowance) {
            return Long.MAX_VALUE;
        }
        return benchmarkSeconds + shutdownAllowance;
    }

    @SuppressWarnings("unchecked")
    private DeploymentPlatform verifyHomogeneousDeploymentPlatform() throws ConnectException,
            InterruptedException, ExecutionException {
        final DeploymentPlatform localPlatform;
        try {
            localPlatform = DeploymentPlatform.local();
        } catch (IllegalArgumentException exception) {
            throw new InterruptedException("SBK-GEM: " + exception.getMessage());
        }
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            probes[i] = nodes[i].runCommandAsync(DeploymentPlatform.probeCommand(), true,
                    config.remoteTimeoutSeconds);
        }
        waitFor(CompletableFuture.allOf(probes), "remote deployment platform checks");
        for (int i = 0; i < nodes.length; i++) {
            final SshResponse response = probes[i].get();
            final DeploymentPlatform remotePlatform = DeploymentPlatform.fromProbe(response);
            if (remotePlatform == null) {
                final String diagnostic;
                if (response == null) {
                    diagnostic = "no response";
                } else {
                    final String errorOutput = response.errOutputStream.toString();
                    diagnostic = diagnosticSummary(errorOutput.isBlank()
                            ? response.stdOutputStream.toString() : errorOutput);
                }
                throw new InterruptedException("SBK-GEM: Host '" + nodes[i].connection.getHost()
                        + "' failed the runtime deployment preflight"
                        + (diagnostic.isEmpty() ? "" : ": " + diagnostic));
            }
            if (!localPlatform.equals(remotePlatform)) {
                throw new InterruptedException("SBK-GEM: Homogeneous deployment required; controller is "
                        + localPlatform.id() + " but host '" + nodes[i].connection.getHost() + "' is "
                        + remotePlatform.id());
            }
        }
        Printer.log.info("SBK-GEM: Homogeneous deployment platform {} verified on {} host(s)",
                localPlatform.id(), nodes.length);
        return localPlatform;
    }

    private RuntimeDeployment prepareRuntimeDeployment(String[] absoluteConnectionDirs,
                                                       DeploymentPlatform platform) throws IOException,
            ConnectException, InterruptedException, ExecutionException {
        final Path localJavaHome = Paths.get(System.getProperty("java.home")).toAbsolutePath().normalize();
        final int localJavaVersion = RemoteJavaDeployment.parseMajorVersion(System.getProperty("java.version"));
        if (params.isJavaCopy() && localJavaVersion != params.getJavaVersion()) {
            throw new IOException("Local Java " + localJavaVersion + " cannot provide requested Java "
                    + params.getJavaVersion());
        }
        final String[] externalJavaHomes = params.isJavaCopy() ? null
                : resolveRequiredRemoteJava(absoluteConnectionDirs);
        final Path configuredCache = Paths.get(config.runtimeCacheDirectory);
        final Path cacheDirectory = configuredCache.isAbsolute() ? configuredCache
                : Paths.get(System.getProperty("user.home")).resolve(configuredCache);
        Printer.log.info("SBK-GEM: Preparing immutable runtime bundle for {}; progress every {} second(s)",
                platform.id(), config.runtimeProgressIntervalSeconds);
        final SbkRuntimeBundle bundle;
        final long bundlePreparationSeconds;
        try (LifecycleProgress progress = new LifecycleProgress("Immutable runtime bundle preparation for "
                + platform.id(), config.runtimeProgressIntervalSeconds,
                () -> "validating, hashing, or compressing SBK/JDK files")) {
            bundle = SbkRuntimeBundle.create(Paths.get(params.getSbkDir()), GemConfig.SBK_COMMAND,
                    params.isJavaCopy() ? localJavaHome : null, config.sbkVersion, params.getJavaVersion(),
                    platform, cacheDirectory);
            bundlePreparationSeconds = progress.elapsedSeconds();
        }
        Printer.log.info("SBK-GEM: Runtime bundle {} prepared in {} second(s); content SHA-256 {}; "
                        + "archive SHA-256 {}", bundle.archive().getFileName(), bundlePreparationSeconds,
                bundle.contentDigest(), bundle.archiveDigest());
        final RuntimeDeployment deployment = deployRuntimeBundle(bundle, absoluteConnectionDirs,
                externalJavaHomes, platform);
        if (params.isRuntimeCleanup()) {
            final int removed = SbkRuntimeBundle.cleanupOtherCachedBundles(cacheDirectory,
                    bundle.deploymentName());
            Printer.log.info("SBK-GEM: Retained local runtime bundle {}; removed {} inactive non-current "
                    + "cached bundle(s)", bundle.deploymentName(), removed);
        }
        return deployment;
    }

    /** Emits bounded lifecycle progress without logging every file or network buffer. */
    private static final class LifecycleProgress implements AutoCloseable {
        private final String operation;
        private final Supplier<String> detail;
        private final long startedNanos;
        private final ScheduledExecutorService scheduler;

        private LifecycleProgress(String operation, int intervalSeconds, Supplier<String> detail) {
            this.operation = operation;
            this.detail = detail;
            this.startedNanos = System.nanoTime();
            this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                    .name("sbk-gem-runtime-progress").daemon(true).factory());
            scheduler.scheduleWithFixedDelay(this::logProgress, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }

        private void logProgress() {
            Printer.log.info("SBK-GEM: {} is still running; elapsed {} second(s); {}",
                    operation, elapsedSeconds(), detail.get());
        }

        private long elapsedSeconds() {
            return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedNanos);
        }

        @Override
        public void close() {
            scheduler.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    private RuntimeDeployment deployRuntimeBundle(SbkRuntimeBundle bundle, String[] absoluteConnectionDirs,
                                                  String[] externalJavaHomes, DeploymentPlatform platform)
            throws ConnectException, InterruptedException, ExecutionException, IOException {
        final String[] deploymentDirectories = new String[nodes.length];
        final String[] javaHomes = new String[nodes.length];
        final String[] sbkCommands = new String[nodes.length];
        final String[] parentDirectories = new String[nodes.length];
        final String[] deploymentNames = new String[nodes.length];
        final String[] leaseIds = new String[nodes.length];
        final String[] leasePaths = new String[nodes.length];
        final String[] releaseCommands = new String[nodes.length];
        final boolean[] copyTargets = new boolean[nodes.length];
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            deploymentDirectories[i] = remoteJoin(absoluteConnectionDirs[i], bundle.deploymentName());
            parentDirectories[i] = absoluteConnectionDirs[i];
            deploymentNames[i] = bundle.deploymentName();
            leaseIds[i] = runtimeLeaseRunId + "-" + i;
            leasePaths[i] = RemoteRuntimeLifecycle.leasePath(parentDirectories[i], deploymentNames[i], leaseIds[i]);
            releaseCommands[i] = RemoteRuntimeLifecycle.releaseCommand(parentDirectories[i], deploymentNames[i],
                    leaseIds[i], params.isRuntimeCleanup(), config.runtimeManagementLockTimeoutSeconds,
                    config.runtimeManagementLockStaleSeconds, config.runtimeLeaseReservationSeconds);
            javaHomes[i] = bundle.javaHome() == null ? externalJavaHomes[i]
                    : remoteJoin(deploymentDirectories[i], bundle.javaHome());
            sbkCommands[i] = remoteJoin(deploymentDirectories[i], bundle.sbkCommand());
            probes[i] = nodes[i].runCommandAsync(RemoteRuntimeDeployment.probeCommand(
                    deploymentDirectories[i], bundle.contentDigest(), javaHomes[i], sbkCommands[i],
                    config.sbkVersion, params.getJavaVersion()), true, config.remoteTimeoutSeconds);
        }
        waitFor(CompletableFuture.allOf(probes), "remote immutable runtime checks");
        for (int i = 0; i < nodes.length; i++) {
            if (probes[i].get().returnCode == ExitCode.SUCCESS) {
                Printer.log.info("SBK-GEM: Host '{}' already has immutable runtime {}; skipping copy",
                        nodes[i].connection.getHost(), bundle.deploymentName());
            } else {
                copyTargets[i] = true;
            }
        }
        if (hasSelectedTarget(copyTargets)) {
            uploadAndActivateRuntime(bundle, deploymentDirectories, copyTargets, platform);
            verifyRuntimeDeployment(bundle, deploymentDirectories, javaHomes, sbkCommands, copyTargets);
        } else {
            Printer.log.info("SBK-GEM: Immutable runtime {} is already available on every host",
                    bundle.deploymentName());
        }
        final RuntimeDeployment deployment = new RuntimeDeployment(javaHomes, sbkCommands,
                leasePaths, releaseCommands);
        runtimeDeployment = deployment;
        acquireRuntimeLeases(bundle, parentDirectories, deploymentNames, leaseIds);
        return deployment;
    }

    @SuppressWarnings("unchecked")
    private void acquireRuntimeLeases(SbkRuntimeBundle bundle, String[] parentDirectories,
                                      String[] deploymentNames, String[] leaseIds)
            throws InterruptedException, ExecutionException {
        final CompletableFuture<SshResponse>[] acquisitions = new CompletableFuture[nodes.length];
        final boolean[] selected = new boolean[nodes.length];
        final String[] targetHosts = new String[nodes.length];
        java.util.Arrays.fill(selected, true);
        for (int i = 0; i < nodes.length; i++) {
            targetHosts[i] = nodes[i].connection.getHost() + ":" + nodes[i].connection.getPort();
            try {
                acquisitions[i] = nodes[i].runCommandAsync(RemoteRuntimeLifecycle.acquireCommand(
                        parentDirectories[i], deploymentNames[i], bundle.contentDigest(), leaseIds[i],
                        params.isRuntimeCleanup(), config.runtimeManagementLockTimeoutSeconds,
                        config.runtimeManagementLockStaleSeconds, config.runtimeLeaseReservationSeconds),
                        true, config.deploymentTimeoutSeconds);
            } catch (ConnectException exception) {
                acquisitions[i] = CompletableFuture.failedFuture(exception);
            }
        }
        Printer.log.info("SBK-GEM: Reserving managed runtime {} on {} host(s); inactive runtime cleanup is {}; "
                        + "progress every {} second(s)", bundle.deploymentName(), nodes.length,
                params.isRuntimeCleanup() ? "enabled" : "disabled", config.runtimeProgressIntervalSeconds);
        final long acquisitionSeconds;
        try (LifecycleProgress progress = new LifecycleProgress("Runtime lease acquisition and cleanup",
                config.runtimeProgressIntervalSeconds,
                () -> futureProgress(acquisitions, targetHosts, "host operation(s)"))) {
            waitForDeployment(CompletableFuture.allOf(acquisitions), "runtime lease acquisition and cleanup");
            acquisitionSeconds = progress.elapsedSeconds();
        }
        requireSuccessfulWithDiagnostics(acquisitions, selected, "Acquiring managed runtime leases");
        Printer.log.info("SBK-GEM: Reserved runtime {} on {} host(s) in {} second(s); inactive non-current "
                        + "runtime cleanup is {}", bundle.deploymentName(), nodes.length, acquisitionSeconds,
                params.isRuntimeCleanup() ? "enabled" : "disabled");
    }

    @SuppressWarnings("unchecked")
    private void uploadAndActivateRuntime(SbkRuntimeBundle bundle, String[] deploymentDirectories,
                                          boolean[] copyTargets, DeploymentPlatform platform)
            throws ConnectException, InterruptedException, ExecutionException, IOException {
        final String transferId = Long.toUnsignedString(System.nanoTime());
        final String[] archivePaths = new String[nodes.length];
        final String[] stagingDirectories = new String[nodes.length];
        final CompletableFuture<SshResponse>[] prepareFutures = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            archivePaths[i] = deploymentDirectories[i] + "." + transferId + ".tar.gz";
            stagingDirectories[i] = deploymentDirectories[i] + "." + transferId + ".staging";
            if (!copyTargets[i] || consMap.isVisited(nodes[i].connection)) {
                prepareFutures[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                consMap.visit(nodes[i].connection);
                final String command = "mkdir -p " + RemoteSbkDeployment.shellQuote(
                        remoteParent(deploymentDirectories[i]));
                prepareFutures[i] = nodes[i].runCommandAsync(command, true, config.remoteTimeoutSeconds);
            }
        }
        waitFor(CompletableFuture.allOf(prepareFutures), "runtime deployment directory preparation");
        requireSuccessfulWithDiagnostics(prepareFutures, copyTargets,
                "Preparing remote runtime deployment directories");

        try (SbkRuntimeBundle.ArchiveUse ignored = bundle.acquireArchiveUse()) {
            final CompletableFuture<?>[] uploads = new CompletableFuture[nodes.length];
            final String[] transferHosts = new String[nodes.length];
            int transferCount = 0;
            consMap.reset();
            for (int i = 0; i < nodes.length; i++) {
                if (!copyTargets[i] || consMap.isVisited(nodes[i].connection)) {
                    uploads[i] = CompletableFuture.completedFuture(null);
                } else {
                    consMap.visit(nodes[i].connection);
                    transferHosts[i] = nodes[i].connection.getHost() + ":" + nodes[i].connection.getPort();
                    transferCount++;
                    uploads[i] = nodes[i].copyFileAsync(bundle.archive().toString(), archivePaths[i],
                            config.deploymentTimeoutSeconds);
                }
            }
            final long archiveBytes = java.nio.file.Files.size(bundle.archive());
            Printer.log.info("SBK-GEM: Copying immutable runtime archive {} ({} byte(s)) to {} unique "
                            + "remote target(s); progress every {} second(s)", bundle.archive().getFileName(),
                    archiveBytes, transferCount, config.runtimeProgressIntervalSeconds);
            final long transferSeconds;
            try (LifecycleProgress progress = new LifecycleProgress("Immutable runtime archive copy",
                    config.runtimeProgressIntervalSeconds,
                    () -> futureProgress(uploads, transferHosts, "transfer(s)"))) {
                waitForDeployment(CompletableFuture.allOf(uploads), "runtime archive upload");
                transferSeconds = progress.elapsedSeconds();
            }
            Printer.log.info("SBK-GEM: Copied immutable runtime archive {} to {} unique remote target(s) "
                            + "in {} second(s)", bundle.archive().getFileName(), transferCount, transferSeconds);
        }

        final CompletableFuture<SshResponse>[] activations = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (!copyTargets[i] || consMap.isVisited(nodes[i].connection)) {
                activations[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                consMap.visit(nodes[i].connection);
                final String command = RemoteRuntimeDeployment.activateCommand(archivePaths[i],
                        bundle.archiveDigest(), bundle.contentDigest(), stagingDirectories[i],
                        deploymentDirectories[i], platform.operatingSystem(), params.isDelete());
                activations[i] = nodes[i].runCommandAsync(command, true, config.deploymentTimeoutSeconds);
            }
        }
        waitForDeployment(CompletableFuture.allOf(activations), "runtime archive activation");
        requireSuccessfulWithDiagnostics(activations, copyTargets, "Activating immutable runtime");
        Printer.log.info("SBK-GEM: Immutable runtime archive verified and atomically activated");
    }

    static String futureProgress(CompletableFuture<?>[] futures, String[] targetHosts, String operationLabel) {
        int finished = 0;
        int total = 0;
        final List<String> pendingHosts = new ArrayList<>();
        for (int i = 0; i < targetHosts.length; i++) {
            if (targetHosts[i] != null) {
                total++;
                if (futures[i].isDone()) {
                    finished++;
                } else {
                    pendingHosts.add(targetHosts[i]);
                }
            }
        }
        return finished + " of " + total + " " + operationLabel + " finished; awaiting host(s): "
                + String.join(", ", pendingHosts);
    }

    @SuppressWarnings("unchecked")
    private void verifyRuntimeDeployment(SbkRuntimeBundle bundle, String[] deploymentDirectories,
                                         String[] javaHomes, String[] sbkCommands, boolean[] copyTargets)
            throws ConnectException, InterruptedException, ExecutionException {
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            probes[i] = copyTargets[i] ? nodes[i].runCommandAsync(RemoteRuntimeDeployment.probeCommand(
                    deploymentDirectories[i], bundle.contentDigest(), javaHomes[i], sbkCommands[i],
                    config.sbkVersion, params.getJavaVersion()), true, config.remoteTimeoutSeconds)
                    : CompletableFuture.completedFuture(new SshResponse(true));
        }
        waitFor(CompletableFuture.allOf(probes), "activated runtime verification");
        requireSuccessfulWithDiagnostics(probes, copyTargets, "Verifying activated immutable runtime");
        Printer.log.info("SBK-GEM: Runtime content {}, Java {}, and SBK {} verified on selected hosts",
                bundle.contentDigest(), params.getJavaVersion(), config.sbkVersion);
    }

    private void requireSuccessfulWithDiagnostics(CompletableFuture<SshResponse>[] futures,
                                                  boolean[] selected, String operation)
            throws InterruptedException, ExecutionException {
        for (int i = 0; i < futures.length; i++) {
            if (selected[i]) {
                final SshResponse response = futures[i].get();
                if (response.returnCode != ExitCode.SUCCESS) {
                    final String errorOutput = response.errOutputStream.toString();
                    final String diagnostic = diagnosticSummary(errorOutput.isBlank()
                            ? response.stdOutputStream.toString() : errorOutput);
                    throw new InterruptedException("SBK-GEM: " + operation + " failed on host '"
                            + nodes[i].connection.getHost() + "' with return code " + response.returnCode
                            + (diagnostic.isEmpty() ? "" : ": " + diagnostic));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String[] resolveRequiredRemoteJava(String[] absoluteConnectionDirs) throws ConnectException,
            InterruptedException, ExecutionException, IOException {
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
                            absoluteConnectionDirs[i], configuredJavaHome, expectedVersion);
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
                            "' will reuse Java " + expectedVersion + " at '" + javaHomes[i] + "'");
                }
            }
        }

        if (hasSelectedTarget(unresolved)) {
            throw new InterruptedException("SBK-GEM: Java " + expectedVersion
                    + " is unavailable on one or more nodes and javacopy is false");
        }

        for (int i = 0; i < nodes.length; i++) {
            Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() + "' will use SBK_JAVA_HOME='" +
                    javaHomes[i] + "'");
        }
        Printer.log.info("SBK-GEM: Matching Java Major Version: " + expectedVersion + " Success..");
        return javaHomes;
    }

    @SuppressWarnings("unchecked")
    private String[] resolveRemoteConnectionDirectories() throws ConnectException, InterruptedException,
            ExecutionException {
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            probes[i] = nodes[i].runCommandAsync(RemoteSbkDeployment.directoryPathProbeCommand(
                    nodes[i].connection.getDir()), true, config.remoteTimeoutSeconds);
        }
        waitFor(CompletableFuture.allOf(probes), "remote working-directory discovery");

        final String[] directories = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final SshResponse response = probes[i].get();
            directories[i] = RemoteSbkDeployment.absoluteDirectoryPath(response);
            if (directories[i] == null) {
                final String remoteError = response.errOutputStream.toString().trim();
                final String errMsg = "SBK-GEM: Unable to resolve remote directory '" +
                        nodes[i].connection.getDir() + "' on host '" + nodes[i].connection.getHost() + "'" +
                        (remoteError.isEmpty() ? "" : ": " + remoteError);
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }
        }
        return directories;
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

    private void waitForDeployment(CompletableFuture<?> future, String operation) throws InterruptedException,
            ExecutionException {
        try {
            future.get(config.deploymentTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            final String message = "SBK-GEM: " + operation + " timed out after "
                    + config.deploymentTimeoutSeconds + " seconds";
            Printer.log.error(message);
            throw new InterruptedException(message);
        }
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

    private static Throwable completionFailure(CompletableFuture<?> future) {
        try {
            future.join();
            return new IllegalStateException("Remote command completed without a result");
        } catch (CompletionException | CancellationException ex) {
            return unwrapCompletionFailure(ex);
        }
    }

    private static Throwable completedFutureFailure(CompletableFuture<?> future) {
        if (future == null) {
            return null;
        }
        if (!future.isDone()) {
            return new IllegalStateException("Embedded SBM completion remained pending during SBK-GEM shutdown");
        }
        try {
            future.join();
            return null;
        } catch (CompletionException | CancellationException exception) {
            return unwrapCompletionFailure(exception);
        }
    }

    static Throwable combineTerminalFailures(Throwable primary, Throwable additional) {
        primary = unwrapCompletionFailure(primary);
        additional = unwrapCompletionFailure(additional);
        if (primary == null) {
            return additional;
        }
        if (additional != null && additional != primary) {
            primary.addSuppressed(additional);
        }
        return primary;
    }

    static RemoteResponse remoteCommandResult(String host, SshResponse response, Throwable failure) {
        if (failure == null && response != null) {
            final RemoteExecutionStatus status = response.returnCode == ExitCode.SUCCESS
                    ? RemoteExecutionStatus.SUCCESS : RemoteExecutionStatus.EXIT_FAILURE;
            final String message = status == RemoteExecutionStatus.SUCCESS ? ""
                    : "SBK-GEM: Remote SBK on host '" + host + "' returned exit code " + response.returnCode;
            return new RemoteResponse(response.returnCode, response.stdOutputStream.toString(),
                    response.errOutputStream.toString(), host, status, message);
        }

        final Throwable cause = unwrapCompletionFailure(failure == null
                ? new IllegalStateException("Remote command did not provide a result") : failure);
        final SshResponse partialResponse;
        final RemoteExecutionStatus status;
        if (cause instanceof SshCommandException commandFailure) {
            partialResponse = commandFailure.getResponse();
            status = commandFailure.isTimeout() ? RemoteExecutionStatus.TIMEOUT : RemoteExecutionStatus.SSH_ERROR;
        } else {
            partialResponse = response;
            status = cause instanceof CancellationException
                    ? RemoteExecutionStatus.CANCELLED : RemoteExecutionStatus.SSH_ERROR;
        }
        final String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        final String message = detail.contains(host) ? detail
                : "SBK-GEM: Remote SBK command failed on host '" + host + "': " + detail;
        return new RemoteResponse(RemoteResponse.UNKNOWN_RETURN_CODE,
                partialResponse == null ? "" : partialResponse.stdOutputStream.toString(),
                partialResponse == null ? "" : partialResponse.errOutputStream.toString(),
                host, status, message);
    }

    static IOException remoteCommandFailure(RemoteResponse[] results) {
        final StringBuilder failures = new StringBuilder();
        for (RemoteResponse result : results) {
            if (result == null || result.status != RemoteExecutionStatus.SUCCESS) {
                if (!failures.isEmpty()) {
                    failures.append(", ");
                }
                if (result == null) {
                    failures.append("unknown host did not complete");
                } else {
                    failures.append(result.host).append(" status ").append(result.status);
                    if (result.returnCode != RemoteResponse.UNKNOWN_RETURN_CODE) {
                        failures.append(" returned ").append(result.returnCode);
                    }
                    final String diagnostic = diagnosticSummary(result.errOutput);
                    if (!diagnostic.isEmpty()) {
                        failures.append(": ").append(diagnostic);
                    }
                }
            }
        }
        return failures.isEmpty() ? null : new IOException("SBK-GEM: Remote SBK execution failed: " + failures);
    }

    static String diagnosticSummary(String errorOutput) {
        if (errorOutput == null || errorOutput.isBlank()) {
            return "";
        }
        final String normalized = errorOutput.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= DEFAULT_CONFIG.maximumDiagnosticCharacters) {
            return normalized;
        }
        final int suffixCharacters = DEFAULT_CONFIG.maximumDiagnosticCharacters
                - DEFAULT_CONFIG.diagnosticPrefixCharacters
                - GemConfig.DIAGNOSTIC_TRUNCATION_MARKER.length();
        return normalized.substring(0, DEFAULT_CONFIG.diagnosticPrefixCharacters)
                + GemConfig.DIAGNOSTIC_TRUNCATION_MARKER
                + normalized.substring(normalized.length() - suffixCharacters);
    }

    private static GemConfig loadDefaultConfig() {
        try {
            return GemConfig.load();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void releaseUnlaunchedRuntimeLeases() throws InterruptedException, ExecutionException {
        if (runtimeDeployment == null) {
            return;
        }
        final CompletableFuture<SshResponse>[] releases = new CompletableFuture[nodes.length];
        final boolean[] selected = new boolean[nodes.length];
        boolean releaseRequired = false;
        for (int i = 0; i < nodes.length; i++) {
            selected[i] = !runtimeLeaseLaunched[i];
            releaseRequired |= selected[i];
            if (selected[i]) {
                try {
                    releases[i] = nodes[i].runCommandAsync(runtimeDeployment.releaseCommands()[i], true,
                            config.deploymentTimeoutSeconds);
                } catch (ConnectException exception) {
                    releases[i] = CompletableFuture.failedFuture(exception);
                }
            } else {
                releases[i] = CompletableFuture.completedFuture(new SshResponse(true));
            }
        }
        if (releaseRequired) {
            waitForDeployment(CompletableFuture.allOf(releases), "unlaunched runtime lease release");
            requireSuccessfulWithDiagnostics(releases, selected, "Releasing unlaunched runtime leases");
        }
    }

    /**
     * Shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     * @param ex Throwable exception
     * @param requestedTermination lifecycle completion expected by the caller
     */
    @Synchronized
    private void shutdown(Throwable ex, BenchmarkTermination requestedTermination) {
        if (state != State.END) {
            state = State.END;
            Throwable terminalFailure = unwrapCompletionFailure(ex);
            int maximumRegisteredClients = -1;
            try {
                releaseUnlaunchedRuntimeLeases();
            } catch (InterruptedException | ExecutionException releaseFailure) {
                terminalFailure = combineTerminalFailures(terminalFailure, releaseFailure);
                if (releaseFailure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            for (SshSession node : nodes) {
                try {
                    node.stop();
                } catch (RuntimeException stopFailure) {
                    terminalFailure = combineTerminalFailures(terminalFailure, stopFailure);
                }
            }
            if (sbmStarted) {
                try {
                    maximumRegisteredClients = sbmBenchmark.getMaximumRegisteredClients();
                    sbmBenchmark.abortPendingRegistrations("SBK-GEM: Distributed benchmark is shutting down");
                    if (sbmCompletion == null || !sbmCompletion.isDone()) {
                        if (terminalFailure == null && remoteCommandsCompleted
                                && requestedTermination.isSuccessfulCompletion()) {
                            sbmBenchmark.completeSuccessfully(
                                    params.getTotalSecondsToRun(), params.getTotalRecords());
                        } else {
                            sbmBenchmark.stop();
                        }
                    }
                } catch (RuntimeException sbmShutdownFailure) {
                    terminalFailure = combineTerminalFailures(terminalFailure, sbmShutdownFailure);
                }
                terminalFailure = combineTerminalFailures(terminalFailure,
                        completedFutureFailure(sbmCompletion));
                sbmStarted = false;
            }
            /*
             * SbmBenchmark.stop() synchronously closes the total latency
             * window and prints "Total : SBM". Keep the distributed outcome
             * after that authoritative aggregate so the final host status is
             * the last benchmark result block presented to the operator.
             */
            if (remoteCommandsCompleted) {
                SbkGem.printRemoteResults(remoteResults, false, maximumRegisteredClients);
            }
            executor.shutdown();
            final BenchmarkTermination termination = BenchmarkTermination.resolve(
                    requestedTermination, terminalFailure);
            if (terminalFailure != null) {
                Printer.log.warn("SBK-GEM Benchmark Shutdown: {}", termination.describe(
                        params.getTotalSecondsToRun(), params.getTotalRecords(),
                        params.getIdleTimeoutSeconds(), terminalFailure), terminalFailure);
                retFuture.completeExceptionally(terminalFailure);
            } else {
                Printer.log.info("SBK-GEM Benchmark Shutdown: {}", termination.describe(
                        params.getTotalSecondsToRun(), params.getTotalRecords(),
                        params.getIdleTimeoutSeconds(), null));
                retFuture.complete(remoteResults);
            }
        }
    }

    @Override
    public void stop() {
        shutdown(null, BenchmarkTermination.STOP_REQUESTED);
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

    private record RuntimeDeployment(String[] javaHomes, String[] sbkCommands,
                                     String[] leasePaths, String[] releaseCommands) {
    }

    private record RemoteTarget(String host, int port, String path) {
    }

    /**
     * Tracks visited (host, port, remoteDir) combinations to avoid duplicate operations
     * when multiple connections point to the same remote target.
     */
    private final static class ConnectionsMap {
        private final Map<RemoteTarget, Boolean> kMap;

        public ConnectionsMap(@NotNull ConnectionConfig[] conn) {
            this.kMap = new HashMap<>();
            for (ConnectionConfig connectionConfig : conn) {
                this.kMap.put(key(connectionConfig), false);
            }
        }

        void reset() {
            this.kMap.replaceAll((key, visited) -> false);
        }

        void visit(@NotNull ConnectionConfig conn) {
            this.kMap.put(key(conn), true);
        }

        boolean isVisited(@NotNull ConnectionConfig conn) {
            return this.kMap.get(key(conn));
        }

        private static RemoteTarget key(ConnectionConfig connection) {
            return new RemoteTarget(connection.getHost().toLowerCase(), connection.getPort(),
                    connection.getDir().toLowerCase());
        }
    }
}

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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
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
    private final SbmBenchmark sbmBenchmark;
    private final GemConfig config;
    private final GemParameters params;
    private final List<List<String>> sbkArgsByNode;
    private final CompletableFuture<RemoteResponse[]> retFuture;
    private final RemoteResponse[] remoteResults;
    /** Execution resources separated by orchestration workload. */
    private final SbkGemExecutors executors;
    private final SshSession[] nodes;
    private final int controllerJavaVersion;
    private final String runtimeLeaseRunId;
    private final boolean[] runtimeLeaseLaunched;
    private final boolean[] runtimeLeaseActive;
    private final CompletableFuture<?>[] runtimeLeaseHeartbeats;
    private final ScheduledExecutorService runtimeLeaseHeartbeatScheduler;
    private final Object runtimeLeaseStateLock;
    private final BenchmarkLifecycle lifecycle;

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
        this.controllerJavaVersion = Runtime.version().feature();
        this.sbkArgsByNode = sbkArgsByNode.stream().map(List::copyOf).toList();
        this.retFuture = new CompletableFuture<>();
        this.lifecycle = new BenchmarkLifecycle();
        this.sbmCompletion = null;
        this.remoteCommandsCompleted = false;
        this.runtimeDeployment = null;
        this.runtimeLeaseRunId = UUID.randomUUID().toString();
        final ConnectionConfig[] connections = params.getConnections();
        if (this.sbkArgsByNode.size() != connections.length) {
            throw new IllegalArgumentException("Remote SBK argument count must match the connection count");
        }
        executors = SbkGemExecutors.create(config.controlExecutorThreads, config.transferExecutorThreads);
        this.remoteResults = new RemoteResponse[connections.length];
        this.nodes = new SshSession[connections.length];
        this.runtimeLeaseLaunched = new boolean[connections.length];
        this.runtimeLeaseActive = new boolean[connections.length];
        this.runtimeLeaseHeartbeats = new CompletableFuture<?>[connections.length];
        this.runtimeLeaseStateLock = new Object();
        this.runtimeLeaseHeartbeatScheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .name("sbk-gem-runtime-lease-heartbeat").daemon(true).factory());
        for (int i = 0; i < connections.length; i++) {
            nodes[i] = new SshSession(connections[i], executors.control(), executors.transfer(), executors.command(),
                    config.diagnosticBytes);
        }
    }

    @Override
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @SuppressWarnings("unchecked")
    public CompletableFuture<RemoteResponse[]> start() throws IOException, InterruptedException, ExecutionException,
            IllegalStateException {
        if (!lifecycle.begin()) {
            if (lifecycle.state() == io.state.State.RUN) {
                Printer.log.warn("SBK GEM Benchmark is already running..");
            } else {
                Printer.log.warn("SBK GEM Benchmark is already shutdown..");
            }
            return retFuture.toCompletableFuture();
        }
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
        try {
            waitForDeployment(connsFuture, "SSH session establishment");
        } catch (ExecutionException ex) {
            throw remoteSessionFailure(ex);
        }
        requireRunning("SSH session establishment");
        Printer.log.info("SBK-GEM: Ssh session establishment Success..");

        final CompletableFuture<RemoteResponse>[] cfResults = new CompletableFuture[nodes.length];
        final String[] absoluteConnectionDirs = resolveRemoteConnectionDirectories();
        requireRunning("remote working-directory discovery");
        final RemoteEnvironment environment = prepareRemoteEnvironment(absoluteConnectionDirs);
        requireRunning("remote environment preparation");
        final DeploymentPlatform platform = environment.platform();
        runtimeDeployment = prepareRuntimeDeployment(absoluteConnectionDirs, environment);
        requireRunning("runtime deployment");
        final String[] javaHomes = runtimeDeployment.javaHomes();

        // start SBM
        lifecycle.markSbmStarted();
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
            final String agentCommand = RemoteAgent.command(javaHomes[i] + "/bin/java",
                    runtimeDeployment.agentPaths()[i]);
            final List<String> jvmArgs = runtimeJvmArgs();
            final byte[] request = RemoteAgent.run(runtimeDeployment.deploymentDirectories()[i],
                    config.sbkVersion, jvmArgs, sbkArgs);
            final String redactedCommand = "java agent run " + java.util.Arrays.toString(
                    SbkUtils.redactSensitiveOptionValues(sbkArgs.toArray(String[]::new)));
            Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() +
                    "' remote SBK command: " + redactedCommand);
            final String host = nodes[i].connection.getHost();
            final CompletableFuture<SshResponse> commandFuture;
            try {
                commandFuture = nodes[i].runBenchmarkCommandAsync(agentCommand, request, true,
                        benchmarkTimeoutSeconds());
                runtimeLeaseLaunched[i] = true;
            } catch (ConnectException ex) {
                cfResults[i] = CompletableFuture.completedFuture(remoteCommandResult(host, null, ex));
                final RemoteResponse result = cfResults[i].join();
                sbmBenchmark.abortPendingRegistrations(result.failureMessage);
                continue;
            }
            final int nodeIndex = i;
            cfResults[i] = commandFuture.handle((response, failure) ->
                            remoteCommandResult(host, response, failure))
                    .thenCompose(result -> releaseRuntimeLeaseAsync(nodeIndex)
                            .handle((ignored, leaseFailure) -> {
                                if (leaseFailure != null) {
                                    Printer.log.warn("SBK-GEM: Unable to release managed runtime lease on host "
                                                    + "'{}:{}'; the lease will expire automatically: {}", host,
                                            nodes[nodeIndex].connection.getPort(),
                                            unwrapCompletionFailure(leaseFailure).getMessage());
                                }
                                return result;
                            }));
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
        }, executors.command());
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
            final IOException remoteFailure = remoteCommandFailure(remoteResults,
                    config.maximumDiagnosticCharacters, config.diagnosticPrefixCharacters);
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

    private static List<String> runtimeJvmArgs() {
        final String configured = System.getProperty("sbk.runtimeJvmArgs", "");
        if (configured.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(configured.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    private RuntimeDeployment prepareRuntimeDeployment(String[] absoluteConnectionDirs,
                                                       RemoteEnvironment environment) throws IOException,
            ConnectException, InterruptedException, ExecutionException {
        final DeploymentPlatform platform = environment.platform();
        final Path configuredCache = Paths.get(config.runtimeCacheDirectory);
        final Path cacheDirectory = configuredCache.isAbsolute() ? configuredCache
                : Paths.get(System.getProperty("user.home")).resolve(configuredCache);
        Printer.log.info("SBK-GEM: Preparing immutable runtime bundle for {}; progress every {} second(s)",
                platform.id(), config.runtimeProgressIntervalSeconds);
        final SbkRuntimeBundle bundle;
        final long bundlePreparationSeconds;
        try (LifecycleProgress progress = new LifecycleProgress("Immutable runtime bundle preparation for "
                + platform.id(), config.runtimeProgressIntervalSeconds,
                () -> "validating, hashing, or compressing SBK files")) {
            bundle = SbkRuntimeBundle.create(Paths.get(params.getSbkDir()), GemConfig.SBK_COMMAND,
                    config.sbkVersion, controllerJavaVersion, platform, cacheDirectory);
            bundlePreparationSeconds = progress.elapsedSeconds();
        }
        Printer.log.info("SBK-GEM: Runtime bundle {} prepared in {} second(s); content SHA-256 {}; "
                        + "archive SHA-256 {}", bundle.archive().getFileName(), bundlePreparationSeconds,
                bundle.contentDigest(), bundle.archiveDigest());
        final RuntimeDeployment deployment = deployRuntimeBundle(bundle, absoluteConnectionDirs,
                environment, platform);
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
                                                  RemoteEnvironment environment, DeploymentPlatform platform)
            throws ConnectException, InterruptedException, ExecutionException, IOException {
        final String[] deploymentDirectories = new String[nodes.length];
        final String[] javaHomes = new String[nodes.length];
        final String[] agentPaths = environment.agentPaths();
        final String[] parentDirectories = new String[nodes.length];
        final String[] deploymentNames = new String[nodes.length];
        final String[] leaseIds = new String[nodes.length];
        final boolean[] copyTargets = new boolean[nodes.length];
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            deploymentDirectories[i] = remoteJoin(absoluteConnectionDirs[i], bundle.deploymentName());
            parentDirectories[i] = absoluteConnectionDirs[i];
            deploymentNames[i] = bundle.deploymentName();
            leaseIds[i] = runtimeLeaseRunId + "-" + i;
            javaHomes[i] = environment.javaHomes()[i];
        }
        final RuntimeDeployment deployment = new RuntimeDeployment(javaHomes, agentPaths, deploymentDirectories,
                parentDirectories, deploymentNames, leaseIds);
        runtimeDeployment = deployment;
        reserveRuntimeLeases(parentDirectories, deploymentNames, leaseIds);
        for (int i = 0; i < nodes.length; i++) {
            probes[i] = nodes[i].runCommandAsync(RemoteAgent.command(javaHomes[i] + "/bin/java", agentPaths[i]),
                    RemoteAgent.verify(deploymentDirectories[i], bundle.contentDigest(), config.sbkVersion,
                            platform.operatingSystem()), true, config.remoteTimeoutSeconds);
        }
        waitForDeployment(CompletableFuture.allOf(probes), "remote immutable runtime checks");
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
            verifyRuntimeDeployment(bundle, deploymentDirectories, javaHomes, agentPaths, copyTargets, platform);
        } else {
            Printer.log.info("SBK-GEM: Immutable runtime {} is already available on every host",
                    bundle.deploymentName());
        }
        acquireRuntimeLeases(bundle, parentDirectories, deploymentNames, leaseIds);
        return deployment;
    }

    @SuppressWarnings("unchecked")
    private void reserveRuntimeLeases(String[] parentDirectories, String[] deploymentNames, String[] leaseIds)
            throws InterruptedException, ExecutionException {
        final CompletableFuture<Void>[] reservations = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final int nodeIndex = i;
            try {
                reservations[i] = nodes[i].runRemoteFileOperationAsync(fileSystem -> {
                    RemoteRuntimeFiles.reserve(fileSystem.getPath(parentDirectories[nodeIndex]),
                            deploymentNames[nodeIndex], leaseIds[nodeIndex],
                            config.runtimeManagementLockTimeoutSeconds,
                            config.runtimeManagementLockStaleSeconds);
                    return null;
                }, lifecycleOperationTimeoutSeconds()).thenRun(() -> activateRuntimeLease(nodeIndex));
            } catch (ConnectException exception) {
                reservations[i] = CompletableFuture.failedFuture(exception);
            }
        }
        waitForDeployment(CompletableFuture.allOf(reservations), "runtime deployment reservation");
        startRuntimeLeaseHeartbeats();
        Printer.log.info("SBK-GEM: Reserved runtime deployment identities through Apache MINA SFTP on {} host(s)",
                nodes.length);
    }

    @SuppressWarnings("unchecked")
    private void acquireRuntimeLeases(SbkRuntimeBundle bundle, String[] parentDirectories,
                                      String[] deploymentNames, String[] leaseIds)
            throws InterruptedException, ExecutionException {
        final CompletableFuture<Void>[] acquisitions = new CompletableFuture[nodes.length];
        final String[] targetHosts = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            targetHosts[i] = nodes[i].connection.getHost() + ":" + nodes[i].connection.getPort();
            try {
                final int nodeIndex = i;
                acquisitions[i] = nodes[i].runRemoteFileOperationAsync(fileSystem -> {
                    RemoteRuntimeFiles.acquire(fileSystem.getPath(parentDirectories[nodeIndex]),
                            deploymentNames[nodeIndex], bundle.contentDigest(), leaseIds[nodeIndex],
                            params.isRuntimeCleanup(), config.runtimeManagementLockTimeoutSeconds,
                            config.runtimeManagementLockStaleSeconds, config.runtimeLeaseReservationSeconds);
                    return null;
                }, lifecycleOperationTimeoutSeconds());
            } catch (ConnectException exception) {
                acquisitions[i] = CompletableFuture.failedFuture(exception);
            }
        }
        Printer.log.info("SBK-GEM: Reserving managed runtime {} through Apache MINA SFTP on {} host(s); "
                        + "inactive runtime retirement is {}; "
                        + "progress every {} second(s)", bundle.deploymentName(), nodes.length,
                params.isRuntimeCleanup() ? "enabled" : "disabled", config.runtimeProgressIntervalSeconds);
        final long acquisitionSeconds;
        try (LifecycleProgress progress = new LifecycleProgress("Runtime lease acquisition and retirement",
                config.runtimeProgressIntervalSeconds,
                () -> futureProgress(acquisitions, targetHosts, "host operation(s)"))) {
            waitForDeployment(CompletableFuture.allOf(acquisitions), "runtime lease acquisition and retirement");
            acquisitionSeconds = progress.elapsedSeconds();
        }
        startRetiredRuntimeCleanup(parentDirectories);
        Printer.log.info("SBK-GEM: Reserved runtime {} on {} host(s) in {} second(s); inactive non-current "
                        + "runtime retirement is {}", bundle.deploymentName(), nodes.length, acquisitionSeconds,
                params.isRuntimeCleanup() ? "enabled" : "disabled");
    }

    private void startRuntimeLeaseHeartbeats() {
        final long intervalSeconds = Math.max(1, config.runtimeLeaseReservationSeconds / 3);
        runtimeLeaseHeartbeatScheduler.scheduleWithFixedDelay(this::refreshRuntimeLeases,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        Printer.log.info("SBK-GEM: Managed runtime leases will be refreshed through Apache MINA SFTP every "
                + "{} second(s)", intervalSeconds);
    }

    private void refreshRuntimeLeases() {
        final RuntimeDeployment deployment = runtimeDeployment;
        if (deployment == null) {
            return;
        }
        for (int i = 0; i < nodes.length; i++) {
            if (!isRuntimeLeaseActive(i)
                    || (runtimeLeaseHeartbeats[i] != null && !runtimeLeaseHeartbeats[i].isDone())) {
                continue;
            }
            final int nodeIndex = i;
            try {
                runtimeLeaseHeartbeats[i] = nodes[i].runRemoteFileOperationAsync(fileSystem -> {
                    RemoteRuntimeFiles.heartbeat(fileSystem.getPath(deployment.parentDirectories()[nodeIndex]),
                            deployment.deploymentNames()[nodeIndex], deployment.leaseIds()[nodeIndex],
                            config.runtimeManagementLockTimeoutSeconds,
                            config.runtimeManagementLockStaleSeconds);
                    return null;
                }, lifecycleOperationTimeoutSeconds()).whenComplete((ignored, failure) -> {
                    if (failure != null && isRuntimeLeaseActive(nodeIndex)) {
                        Printer.log.warn("SBK-GEM: Managed runtime lease heartbeat failed on host '{}:{}': {}",
                                nodes[nodeIndex].connection.getHost(), nodes[nodeIndex].connection.getPort(),
                                unwrapCompletionFailure(failure).getMessage());
                    }
                });
            } catch (ConnectException exception) {
                Printer.log.warn("SBK-GEM: Unable to start managed runtime lease heartbeat on host '{}:{}': {}",
                        nodes[i].connection.getHost(), nodes[i].connection.getPort(), exception.getMessage());
            }
        }
    }

    private void startRetiredRuntimeCleanup(String[] parentDirectories) {
        if (!params.isRuntimeCleanup()) {
            return;
        }
        final RemoteTargetPlan targetPlan = RemoteTargetPlan.create(params.getConnections(), parentDirectories);
        for (int i = 0; i < nodes.length; i++) {
            if (!targetPlan.isRepresentative(i)) {
                continue;
            }
            final int nodeIndex = i;
            try {
                nodes[i].runRemoteTransferOperationAsync(fileSystem ->
                                RemoteRuntimeFiles.deleteRetired(fileSystem.getPath(parentDirectories[nodeIndex])),
                                config.deploymentTimeoutSeconds)
                        .whenComplete((deleted, failure) -> {
                            if (failure == null) {
                                Printer.log.info("SBK-GEM: Removed {} retired runtime tree(s) from host '{}:{}'",
                                        deleted, nodes[nodeIndex].connection.getHost(),
                                        nodes[nodeIndex].connection.getPort());
                            } else {
                                Printer.log.warn("SBK-GEM: Retired runtime deletion failed on host '{}:{}'; "
                                                + "a later run will retry: {}", nodes[nodeIndex].connection.getHost(),
                                        nodes[nodeIndex].connection.getPort(),
                                        unwrapCompletionFailure(failure).getMessage());
                            }
                        });
            } catch (ConnectException exception) {
                Printer.log.warn("SBK-GEM: Unable to start retired runtime deletion on host '{}:{}': {}",
                        nodes[i].connection.getHost(), nodes[i].connection.getPort(), exception.getMessage());
            }
        }
    }

    private CompletableFuture<Void> releaseRuntimeLeaseAsync(int nodeIndex) {
        if (!deactivateRuntimeLease(nodeIndex) || runtimeDeployment == null) {
            return CompletableFuture.completedFuture(null);
        }
        final RuntimeDeployment deployment = runtimeDeployment;
        try {
            return nodes[nodeIndex].runRemoteFileOperationAsync(fileSystem -> {
                RemoteRuntimeFiles.release(fileSystem.getPath(deployment.parentDirectories()[nodeIndex]),
                        deployment.deploymentNames()[nodeIndex], deployment.leaseIds()[nodeIndex],
                        params.isRuntimeCleanup(), config.runtimeManagementLockTimeoutSeconds,
                        config.runtimeManagementLockStaleSeconds, config.runtimeLeaseReservationSeconds);
                return null;
            }, lifecycleOperationTimeoutSeconds());
        } catch (ConnectException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private long lifecycleOperationTimeoutSeconds() {
        if (config.runtimeManagementLockTimeoutSeconds
                >= Long.MAX_VALUE - config.remoteTimeoutSeconds) {
            return Long.MAX_VALUE;
        }
        return config.runtimeManagementLockTimeoutSeconds + config.remoteTimeoutSeconds;
    }

    private void activateRuntimeLease(int nodeIndex) {
        synchronized (runtimeLeaseStateLock) {
            runtimeLeaseActive[nodeIndex] = true;
        }
    }

    private boolean isRuntimeLeaseActive(int nodeIndex) {
        synchronized (runtimeLeaseStateLock) {
            return runtimeLeaseActive[nodeIndex];
        }
    }

    private boolean deactivateRuntimeLease(int nodeIndex) {
        synchronized (runtimeLeaseStateLock) {
            final boolean active = runtimeLeaseActive[nodeIndex];
            runtimeLeaseActive[nodeIndex] = false;
            return active;
        }
    }

    @SuppressWarnings("unchecked")
    private void uploadAndActivateRuntime(SbkRuntimeBundle bundle, String[] deploymentDirectories,
                                          boolean[] copyTargets, DeploymentPlatform platform)
            throws ConnectException, InterruptedException, ExecutionException, IOException {
        final String transferId = Long.toUnsignedString(System.nanoTime());
        final String[] archivePaths = new String[nodes.length];
        final String[] stagingDirectories = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            archivePaths[i] = deploymentDirectories[i] + "." + transferId + ".tar.gz";
            stagingDirectories[i] = deploymentDirectories[i] + "." + transferId + ".staging";
        }
        final RemoteTargetPlan targetPlan = RemoteTargetPlan.create(params.getConnections(), deploymentDirectories);
        final boolean[] physicalCopyTargets = targetPlan.representativeSelection(copyTargets);

        try (SbkRuntimeBundle.ArchiveUse ignored = bundle.acquireArchiveUse()) {
            final CompletableFuture<?>[] uploads = new CompletableFuture[nodes.length];
            final String[] transferHosts = new String[nodes.length];
            int transferCount = 0;
            for (int i = 0; i < nodes.length; i++) {
                if (!physicalCopyTargets[i]) {
                    uploads[i] = CompletableFuture.completedFuture(null);
                } else {
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
        for (int i = 0; i < nodes.length; i++) {
            if (!physicalCopyTargets[i]) {
                activations[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                final String command = RemoteAgent.command(runtimeDeployment.javaHomes()[i] + "/bin/java",
                        runtimeDeployment.agentPaths()[i]);
                activations[i] = nodes[i].runCommandAsync(command, RemoteAgent.activate(archivePaths[i],
                                bundle.archiveDigest(), bundle.contentDigest(), stagingDirectories[i],
                                deploymentDirectories[i], platform.operatingSystem()),
                        true, config.deploymentTimeoutSeconds);
            }
        }
        waitForDeployment(CompletableFuture.allOf(activations), "runtime archive activation");
        requireSuccessfulWithDiagnostics(activations, physicalCopyTargets, "Activating immutable runtime");
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
                                         String[] javaHomes, String[] agentPaths, boolean[] copyTargets,
                                         DeploymentPlatform platform)
            throws ConnectException, InterruptedException, ExecutionException, IOException {
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            probes[i] = copyTargets[i] ? nodes[i].runCommandAsync(
                    RemoteAgent.command(javaHomes[i] + "/bin/java", agentPaths[i]),
                    RemoteAgent.verify(deploymentDirectories[i], bundle.contentDigest(), config.sbkVersion,
                            platform.operatingSystem()), true, config.remoteTimeoutSeconds)
                    : CompletableFuture.completedFuture(new SshResponse(true));
        }
        waitForDeployment(CompletableFuture.allOf(probes), "activated runtime verification");
        requireSuccessfulWithDiagnostics(probes, copyTargets, "Verifying activated immutable runtime");
        Printer.log.info("SBK-GEM: Runtime content {}, Java {} or newer, and SBK {} verified on selected hosts",
                bundle.contentDigest(), controllerJavaVersion, config.sbkVersion);
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
                                    ? response.stdOutputStream.toString() : errorOutput,
                            config.maximumDiagnosticCharacters, config.diagnosticPrefixCharacters);
                    throw new InterruptedException("SBK-GEM: " + operation + " failed on host '"
                            + nodes[i].connection.getHost() + "' with return code " + response.returnCode
                            + (diagnostic.isEmpty() ? "" : ": " + diagnostic));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private RemoteEnvironment prepareRemoteEnvironment(String[] absoluteConnectionDirs) throws ConnectException,
            InterruptedException, ExecutionException, IOException {
        final int expectedVersion = controllerJavaVersion;
        final DeploymentPlatform localPlatform = DeploymentPlatform.local();
        final Path localAgent = Path.of(params.getSbkDir(), "lib", "sbk-gem-agent-" + config.sbkVersion + ".jar")
                .toAbsolutePath().normalize();
        if (!java.nio.file.Files.isRegularFile(localAgent)) {
            throw new IOException("SBK-GEM remote agent is missing from the installed distribution: " + localAgent);
        }
        final String[] agentPaths = new String[nodes.length];
        final CompletableFuture<String>[] agentInstalls = new CompletableFuture[nodes.length];
        final RemoteTargetPlan targetPlan = RemoteTargetPlan.create(params.getConnections(),
                absoluteConnectionDirs);
        for (int i = 0; i < nodes.length; i++) {
            if (targetPlan.isRepresentative(i)) {
                final int nodeIndex = i;
                agentInstalls[i] = nodes[i].runRemoteTransferOperationAsync(fileSystem ->
                                RemoteAgentFiles.install(fileSystem, absoluteConnectionDirs[nodeIndex], localAgent,
                                        config.sbkVersion), config.deploymentTimeoutSeconds);
            } else {
                agentInstalls[i] = agentInstalls[targetPlan.representative(i)];
            }
        }
        waitForDeployment(CompletableFuture.allOf(agentInstalls), "remote Java-agent installation");
        for (int i = 0; i < nodes.length; i++) {
            agentPaths[i] = agentInstalls[i].get();
        }

        final String[] javaHomes = new String[nodes.length];
        final boolean[] unresolved = new boolean[nodes.length];
        final CompletableFuture<SshResponse>[] pathProbes = new CompletableFuture[nodes.length];
        final String configuredJavaHome = normalizeRemotePath(params.getJavaDir());
        for (int i = 0; i < nodes.length; i++) {
            final String javaExecutable = configuredJavaHome == null ? "java" : configuredJavaHome + "/bin/java";
            pathProbes[i] = nodes[i].runCommandAsync(RemoteAgent.command(javaExecutable, agentPaths[i]),
                    RemoteAgent.probe(expectedVersion), true, config.remoteTimeoutSeconds);
        }
        waitForDeployment(CompletableFuture.allOf(pathProbes), "remote Java discovery");
        for (int i = 0; i < nodes.length; i++) {
            final SshResponse response = pathProbes[i].get();
            javaHomes[i] = RemoteAgent.javaHome(response);
            unresolved[i] = javaHomes[i] == null || javaHomes[i].isBlank();
        }

        if (hasSelectedTarget(unresolved)) {
            Printer.log.info("SBK-GEM: Java {} or newer is missing on selected host(s); preparing a separate "
                    + "content-addressed JDK copy", expectedVersion);
            final ManagedJavaRuntime javaRuntime = ManagedJavaRuntime.create(
                    Path.of(System.getProperty("java.home")), expectedVersion);
            final String[] javaParentDirectories = new String[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                javaParentDirectories[i] = remoteParent(absoluteConnectionDirs[i]);
            }
            final RemoteTargetPlan javaTargetPlan = RemoteTargetPlan.create(params.getConnections(),
                    javaParentDirectories);
            final CompletableFuture<String>[] copies = new CompletableFuture[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                if (!javaTargetPlan.isRepresentative(i)) {
                    copies[i] = copies[javaTargetPlan.representative(i)];
                } else if (javaTargetPlan.hasSelectedNode(i, unresolved)) {
                    final int nodeIndex = i;
                    copies[i] = nodes[i].runRemoteTransferOperationAsync(fileSystem -> javaRuntime.install(fileSystem,
                            javaParentDirectories[nodeIndex]), config.deploymentTimeoutSeconds);
                } else {
                    copies[i] = CompletableFuture.completedFuture(javaHomes[i]);
                }
            }
            try (LifecycleProgress progress = new LifecycleProgress("Separate JDK copy",
                    config.runtimeProgressIntervalSeconds,
                    () -> futureProgress(copies, hostLabels(), "JDK operation(s)"))) {
                waitForDeployment(CompletableFuture.allOf(copies), "separate remote JDK provisioning");
            }
            for (int i = 0; i < nodes.length; i++) {
                javaHomes[i] = copies[i].get();
                final SshResponse verified = nodes[i].runCommandAsync(
                        RemoteAgent.command(javaHomes[i] + "/bin/java", agentPaths[i]),
                        RemoteAgent.probe(expectedVersion), true, config.remoteTimeoutSeconds).get();
                if (!RemoteAgent.successful(verified)) {
                    throw new IOException("Provisioned JDK verification failed on "
                            + nodes[i].connection.getHost() + ": " + diagnosticSummary(
                            verified.errOutputStream.toString(), config.maximumDiagnosticCharacters,
                            config.diagnosticPrefixCharacters));
                }
                unresolved[i] = false;
            }
        }

        if (hasSelectedTarget(unresolved)) {
            throw new InterruptedException("SBK-GEM: Java " + expectedVersion
                    + " or newer could not be provisioned");
        }

        DeploymentPlatform verifiedPlatform = null;
        for (int i = 0; i < nodes.length; i++) {
            final SshResponse response = nodes[i].runCommandAsync(
                    RemoteAgent.command(javaHomes[i] + "/bin/java", agentPaths[i]),
                    RemoteAgent.probe(expectedVersion), true, config.remoteTimeoutSeconds).get();
            final DeploymentPlatform platform = RemoteAgent.platform(response);
            if (platform == null || !localPlatform.equals(platform)) {
                throw new IOException("Homogeneous deployment required; controller is " + localPlatform.id()
                        + " but host '" + nodes[i].connection.getHost() + "' is "
                        + (platform == null ? "unknown" : platform.id()));
            }
            verifiedPlatform = platform;
            Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() + "' will use SBK_JAVA_HOME='" +
                    javaHomes[i] + "'");
        }
        Printer.log.info("SBK-GEM: Matching OS {} and Java major {} or newer verified on {} host(s)",
                verifiedPlatform.id(), expectedVersion, nodes.length);
        return new RemoteEnvironment(javaHomes, agentPaths, verifiedPlatform);
    }

    private String[] hostLabels() {
        final String[] labels = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            labels[i] = nodes[i].connection.getHost() + ":" + nodes[i].connection.getPort();
        }
        return labels;
    }

    private static String remoteParent(String path) {
        final int separator = path.lastIndexOf('/');
        return separator <= 0 ? "/" : path.substring(0, separator);
    }

    @SuppressWarnings("unchecked")
    private String[] resolveRemoteConnectionDirectories() throws InterruptedException, ExecutionException {
        final CompletableFuture<String>[] resolutions = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final int nodeIndex = i;
            try {
                resolutions[i] = nodes[i].runRemoteFileOperationAsync(fileSystem ->
                                RemoteRuntimeFiles.resolveDirectory(fileSystem,
                                        nodes[nodeIndex].connection.getDir()),
                        lifecycleOperationTimeoutSeconds());
            } catch (ConnectException exception) {
                resolutions[i] = CompletableFuture.failedFuture(exception);
            }
        }
        waitForDeployment(CompletableFuture.allOf(resolutions), "remote working-directory discovery");

        final String[] directories = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            directories[i] = resolutions[i].get();
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

    private static String remoteJoin(String parent, String child) {
        return "/".equals(parent) ? parent + child : parent + "/" + child;
    }

    private void requireRunning(String operation) {
        lifecycle.requireRunning(operation);
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
        return remoteCommandFailure(results, GemConfig.DEFAULT_MAXIMUM_DIAGNOSTIC_CHARACTERS,
                GemConfig.DEFAULT_DIAGNOSTIC_PREFIX_CHARACTERS);
    }

    private static IOException remoteCommandFailure(RemoteResponse[] results, int maximumCharacters,
                                                     int prefixCharacters) {
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
                    final String diagnostic = diagnosticSummary(result.errOutput, maximumCharacters,
                            prefixCharacters);
                    if (!diagnostic.isEmpty()) {
                        failures.append(": ").append(diagnostic);
                    }
                }
            }
        }
        return failures.isEmpty() ? null : new IOException("SBK-GEM: Remote SBK execution failed: " + failures);
    }

    static String diagnosticSummary(String errorOutput) {
        return diagnosticSummary(errorOutput, GemConfig.DEFAULT_MAXIMUM_DIAGNOSTIC_CHARACTERS,
                GemConfig.DEFAULT_DIAGNOSTIC_PREFIX_CHARACTERS);
    }

    static String diagnosticSummary(String errorOutput, int maximumCharacters, int prefixCharacters) {
        if (errorOutput == null || errorOutput.isBlank()) {
            return "";
        }
        final String normalized = errorOutput.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maximumCharacters) {
            return normalized;
        }
        final int suffixCharacters = maximumCharacters
                - prefixCharacters
                - GemConfig.DIAGNOSTIC_TRUNCATION_MARKER.length();
        return normalized.substring(0, prefixCharacters)
                + GemConfig.DIAGNOSTIC_TRUNCATION_MARKER
                + normalized.substring(normalized.length() - suffixCharacters);
    }

    @SuppressWarnings("unchecked")
    private void releaseUnlaunchedRuntimeLeases() throws InterruptedException, ExecutionException {
        if (runtimeDeployment == null) {
            return;
        }
        final CompletableFuture<Void>[] releases = new CompletableFuture[nodes.length];
        boolean releaseRequired = false;
        for (int i = 0; i < nodes.length; i++) {
            if (isRuntimeLeaseActive(i) && !runtimeLeaseLaunched[i]) {
                releaseRequired = true;
                releases[i] = releaseRuntimeLeaseAsync(i);
            } else {
                releases[i] = CompletableFuture.completedFuture(null);
            }
        }
        if (releaseRequired) {
            waitForDeployment(CompletableFuture.allOf(releases), "unlaunched runtime lease release");
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
    private void shutdown(Throwable ex, BenchmarkTermination requestedTermination) {
        if (!lifecycle.beginShutdown()) {
            return;
        }
        Throwable terminalFailure = unwrapCompletionFailure(ex);
        int maximumRegisteredClients = -1;
        runtimeLeaseHeartbeatScheduler.shutdownNow();
        for (SshSession node : nodes) {
            node.cancelActiveOperations();
        }
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
        final boolean stopSbm = lifecycle.takeSbmStarted();
        if (stopSbm) {
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
        executors.close();
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

    private record RuntimeDeployment(String[] javaHomes, String[] agentPaths, String[] deploymentDirectories,
                                     String[] parentDirectories, String[] deploymentNames,
                                     String[] leaseIds) {
    }

    private record RemoteEnvironment(String[] javaHomes, String[] agentPaths, DeploymentPlatform platform) {
    }

}

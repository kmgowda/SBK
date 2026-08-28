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
import io.gem.api.SshClientManager;
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
    private static final int FIRST_RESULT_REPORTING_WINDOWS = 2;
    private final SbmBenchmark sbmBenchmark;
    private final GemConfig config;
    private final GemParameters params;
    private final CompletableFuture<RemoteResponse[]> retFuture;
    /** Execution resources separated by orchestration workload. */
    private final SbkGemExecutors executors;
    private final List<RemoteNodeState> nodes;
    private final SshClientManager sshClientManager;
    private final int controllerJavaVersion;
    private final RuntimeCopyPolicy runtimeCopyPolicy;
    private final String runtimeLeaseRunId;
    private final ScheduledExecutorService runtimeLeaseHeartbeatScheduler;
    private final RuntimeLeaseManager runtimeLeaseManager;
    private final RemoteEnvironmentPreparer environmentPreparer;
    private final DeploymentOrchestrator deploymentOrchestrator;
    private final BenchmarkLifecycle lifecycle;

    private CompletableFuture<Void> sbmCompletion;
    private volatile boolean remoteCommandsCompleted;

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
        this.runtimeCopyPolicy = RuntimeCopyPolicy.select(config.fullcopy);
        this.runtimeLeaseRunId = UUID.randomUUID().toString();
        this.retFuture = new CompletableFuture<>();
        this.lifecycle = new BenchmarkLifecycle();
        this.sbmCompletion = null;
        this.remoteCommandsCompleted = false;
        final ConnectionConfig[] connections = params.getConnections();
        if (sbkArgsByNode.size() != connections.length) {
            throw new IllegalArgumentException("Remote SBK argument count must match the connection count");
        }
        executors = SbkGemExecutors.create(config.controlExecutorThreads,
                TransferExecutorSizing.initialThreads(config));
        this.nodes = new ArrayList<>(connections.length);
        this.sshClientManager = new SshClientManager();
        this.runtimeLeaseHeartbeatScheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .name("sbk-gem-lifecycle-scheduler").daemon(true).factory());
        for (int i = 0; i < connections.length; i++) {
            final SshSession session = sshClientManager.sessionFor(connections[i], executors.control(),
                    executors.transfer(), executors.command(), config.diagnosticBytes, config.sshCopyBufferBytes);
            nodes.add(new RemoteNodeState(i, session, sbkArgsByNode.get(i)));
        }
        this.runtimeLeaseManager = new RuntimeLeaseManager(config, params, nodes,
                runtimeLeaseHeartbeatScheduler);
        this.environmentPreparer = new RemoteEnvironmentPreparer(config, params, nodes,
                controllerJavaVersion, runtimeCopyPolicy, runtimeLeaseHeartbeatScheduler);
        final RuntimeDeploymentTransport runtimeTransport = new RuntimeDeploymentTransport(config,
                params, nodes, controllerJavaVersion, runtimeLeaseHeartbeatScheduler);
        this.deploymentOrchestrator = new DeploymentOrchestrator(config, params, nodes,
                controllerJavaVersion, runtimeCopyPolicy, runtimeLeaseManager,
                runtimeLeaseHeartbeatScheduler, runtimeLeaseRunId, runtimeTransport);
        Printer.log.info("SBK-GEM: Sharing {} Apache MINA SSH client(s) across {} remote node session(s)",
                sshClientManager.size(), connections.length);
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
        final CompletableFuture<?>[] cfArray = new CompletableFuture[nodes.size()];

        for (RemoteNodeState node : nodes) {
            cfArray[node.index()] = node.session().createSessionAsync(config.remoteTimeoutSeconds);
        }
        final CompletableFuture<Void> connsFuture = CompletableFuture.allOf(cfArray);
        try {
            DeploymentSupport.waitFor(connsFuture, config.deploymentTimeoutSeconds,
                    "SSH session establishment");
        } catch (ExecutionException ex) {
            throw remoteSessionFailure(ex);
        }
        requireRunning("SSH session establishment");
        for (RemoteNodeState node : nodes) {
            node.endpointIdentity(node.session().getRemoteEndpointIdentity());
        }
        configureTransferExecutor();
        configureSbmCallbackAddresses();
        Printer.log.info("SBK-GEM: Ssh session establishment Success..");

        final CompletableFuture<RemoteResponse>[] cfResults = new CompletableFuture[nodes.size()];
        final CompletableFuture<Void>[] leaseReleases = new CompletableFuture[nodes.size()];
        final DeploymentPlatform platform = environmentPreparer.prepare();
        requireRunning("remote environment preparation");
        deploymentOrchestrator.deploy(platform);
        requireRunning("runtime deployment");

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
        final List<String> jvmArgs = runtimeJvmArgs();
        final long remoteBenchmarkTimeoutSeconds = benchmarkTimeoutSeconds();
        for (RemoteNodeState node : nodes) {
            final int i = node.index();
            final List<String> sbkArgs = node.sbkArguments();
            final String agentCommand = RemoteAgent.command(DeploymentSupport.remoteJavaExecutable(node.javaHome()),
                    node.agentPath());
            final byte[] request = RemoteAgent.run(node.deploymentDirectory(),
                    config.sbkVersion, jvmArgs, sbkArgs);
            final String redactedCommand = "java agent run " + java.util.Arrays.toString(
                    SbkUtils.redactSensitiveOptionValues(sbkArgs.toArray(String[]::new)));
            Printer.log.info("SBK-GEM: Host '" + node.host() +
                    "' remote SBK command: " + redactedCommand);
            final String host = node.host();
            final CompletableFuture<SshResponse> commandFuture;
            try {
                commandFuture = runtimeLeaseManager.launch(node,
                        () -> node.session().runBenchmarkCommandAsync(agentCommand, request, true,
                                remoteBenchmarkTimeoutSeconds));
            } catch (ConnectException ex) {
                cfResults[i] = CompletableFuture.completedFuture(remoteCommandResult(host, null, ex));
                leaseReleases[i] = CompletableFuture.completedFuture(null);
                final RemoteResponse result = cfResults[i].join();
                sbmBenchmark.abortPendingRegistrations(result.failureMessage);
                continue;
            }
            cfResults[i] = commandFuture.handle((response, failure) ->
                    remoteCommandResult(host, response, failure));
            leaseReleases[i] = cfResults[i].thenCompose(result -> runtimeLeaseManager.release(node)
                            .handle((ignored, leaseFailure) -> {
                                if (leaseFailure != null) {
                                    Printer.log.warn("SBK-GEM: Unable to release managed runtime lease on host "
                                                    + "'{}:{}'; the lease will expire automatically: {}", host,
                                            node.session().connection.getPort(),
                                            unwrapCompletionFailure(leaseFailure).getMessage());
                                }
                                return null;
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
                        "registration ({}/{})", nodes.size(), sbmBenchmark.getMaximumRegisteredClients(),
                nodes.size());
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
                                nodes.size(), TimeUnit.NANOSECONDS.toSeconds(elapsedNanos));
                    }
                }
                if (coordinatedStart) {
                    sbmBenchmark.startLatencyAggregation();
                    final int releasedClients = sbmBenchmark.releaseCoordinatedStart();
                    Printer.log.info("SBK-GEM: All prepared remote SBK clients registered with SBM ({}/{}); " +
                                    "benchmark timing has started. Because remote SBK and SBM use independent " +
                                    "{}-second reporting windows, first performance results are expected within " +
                                    "{} seconds", releasedClients, nodes.size(),
                            PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS,
                            FIRST_RESULT_REPORTING_WINDOWS * PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS);
                } else if (sbmBenchmark.getRegistrationFailure() == null) {
                    final String failure = "SBK-GEM: SBM coordinated start timed out after " +
                            config.sbmRegistrationTimeoutSeconds + " seconds; registered " +
                            sbmBenchmark.getMaximumRegisteredClients() + " of " + nodes.size() + " remote clients";
                    Printer.log.error(failure);
                    sbmBenchmark.abortPendingRegistrations(failure);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                final String failure = "SBK-GEM: Interrupted while waiting for remote clients to register with SBM";
                Printer.log.error(failure, ex);
                sbmBenchmark.abortPendingRegistrations(failure);
            } catch (ExecutionException | RuntimeException ex) {
                final Throwable failure = unwrapCompletionFailure(ex);
                Printer.log.error("SBK-GEM: Unable to start SBM latency aggregation after all remote clients " +
                        "registered", failure);
                shutdown(failure, BenchmarkTermination.INTERNAL_FAILURE);
            }
        }, executors.command());
        final CompletableFuture<Void> remoteCommands = CompletableFuture.allOf(cfResults);
        remoteCommands.whenComplete((ignored, failure) -> {
            for (RemoteNodeState node : nodes) {
                final int i = node.index();
                if (cfResults[i].isCompletedExceptionally()) {
                    node.result(remoteCommandResult(node.host(), null, completionFailure(cfResults[i])));
                } else {
                    node.result(cfResults[i].join());
                }
            }
            remoteCommandsCompleted = true;
            final RemoteResponse[] remoteResults = remoteResults();
            final IOException remoteFailure = remoteCommandFailure(remoteResults,
                    config.maximumDiagnosticCharacters, config.diagnosticPrefixCharacters);
            if (!lifecycle.isRunning()) {
                return;
            }
            final Throwable commandFailure = remoteFailure == null
                    ? unwrapCompletionFailure(failure) : remoteFailure;
            final Throwable benchmarkFailure = completeSbmAfterRemoteCommands(commandFailure);
            CompletableFuture.allOf(leaseReleases)
                    .thenCompose(released -> runtimeLeaseManager.cleanupRetired(
                            DeploymentSupport.endpointIdentities(nodes)))
                    .whenComplete((cleaned, cleanupFailure) -> {
                        if (benchmarkFailure != null) {
                            shutdown(benchmarkFailure, BenchmarkTermination.INTERNAL_FAILURE);
                        } else {
                            shutdown(null, BenchmarkTermination.configured(
                                    params.getTotalSecondsToRun(), params.getTotalRecords()));
                        }
                    });
        });

        return retFuture.toCompletableFuture();
    }

    private void configureTransferExecutor() {
        final int uniqueTargets = RemoteTargetPlan.createBeforeDirectoryResolution(params.getConnections(),
                DeploymentSupport.endpointIdentities(nodes)).targetCount();
        final int transferThreads = TransferExecutorSizing.selectedThreads(config, uniqueTargets);
        executors.configureTransferThreads(transferThreads);
        if (config.transferExecutorThreads == 0) {
            Printer.log.info("SBK-GEM: Using {} concurrent deployment transfers for {} unique remote target(s); "
                            + "configured range: {}-{}; target waves: {}", transferThreads, uniqueTargets,
                    config.transferExecutorMinimumThreads, config.transferExecutorMaximumThreads,
                    config.transferTargetWaves);
        } else {
            Printer.log.info("SBK-GEM: Using configured {} concurrent deployment transfers for {} unique "
                    + "remote target(s)", transferThreads, uniqueTargets);
        }
    }

    private Throwable completeSbmAfterRemoteCommands(Throwable commandFailure) {
        try {
            if (commandFailure == null) {
                sbmBenchmark.completeSuccessfully(params.getTotalSecondsToRun(), params.getTotalRecords());
            } else {
                sbmBenchmark.stop();
            }
            return commandFailure;
        } catch (RuntimeException sbmFailure) {
            return combineTerminalFailures(commandFailure, sbmFailure);
        }
    }

    private RemoteResponse[] remoteResults() {
        return nodes.stream().map(RemoteNodeState::result).toArray(RemoteResponse[]::new);
    }

    private void configureSbmCallbackAddresses() throws ConnectException {
        if (params.isLocalHostOption()) {
            Printer.log.info("SBK-GEM: Using explicitly configured SBM callback address '{}:{}' for every "
                    + "remote node", params.getLocalHost(), params.getSbmPort());
            return;
        }
        for (RemoteNodeState node : nodes) {
            final String callbackAddress = node.session().getLocalRouteAddress();
            replaceOptionValue(node.sbkArguments(), "-sbm", callbackAddress);
            Printer.log.info("SBK-GEM: Host '{}' will connect to SBM at '{}:{}' using the numeric controller "
                            + "address selected by its authenticated SSH route", node.host(),
                    callbackAddress, params.getSbmPort());
        }
    }

    static void replaceOptionValue(List<String> arguments, String option, String value) {
        final int optionIndex = arguments.indexOf(option);
        if (optionIndex < 0 || optionIndex + 1 >= arguments.size()) {
            throw new IllegalArgumentException("Missing value for required remote SBK option: " + option);
        }
        arguments.set(optionIndex + 1, value);
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

    static String failureDescription(Throwable failure) {
        return DeploymentSupport.failureDescription(failure);
    }

    private void requireRunning(String operation) {
        lifecycle.requireRunning(operation);
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
        return DeploymentSupport.diagnosticSummary(errorOutput, maximumCharacters, prefixCharacters);
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
        for (RemoteNodeState node : nodes) {
            node.session().cancelActiveOperations();
        }
        try {
            runtimeLeaseManager.releaseUnlaunched();
        } catch (IOException | InterruptedException | ExecutionException releaseFailure) {
            terminalFailure = combineTerminalFailures(terminalFailure, releaseFailure);
            if (releaseFailure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        for (RemoteNodeState node : nodes) {
            try {
                node.session().stop();
            } catch (RuntimeException stopFailure) {
                terminalFailure = combineTerminalFailures(terminalFailure, stopFailure);
            }
        }
        try {
            sshClientManager.close();
        } catch (RuntimeException stopFailure) {
            terminalFailure = combineTerminalFailures(terminalFailure, stopFailure);
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
            SbkGem.printRemoteResults(remoteResults(), false, maximumRegisteredClients);
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
            retFuture.complete(remoteResults());
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

}

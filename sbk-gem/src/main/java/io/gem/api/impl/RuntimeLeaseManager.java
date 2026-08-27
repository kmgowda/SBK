/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import io.gem.api.SshResponse;
import io.gem.config.GemConfig;
import io.gem.params.GemParameters;
import io.sbk.system.Printer;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Owns remote runtime leases, heartbeats, releases, and retired-package cleanup. */
final class RuntimeLeaseManager {
    /** Starts one remote benchmark command while atomically transferring lease ownership to it. */
    @FunctionalInterface
    interface BenchmarkCommandStarter {
        CompletableFuture<SshResponse> start() throws ConnectException;
    }

    private static final int LEASE_HEARTBEAT_DIVISOR = 3;
    private static final long MINIMUM_INTERVAL_SECONDS = 1;

    private final GemConfig config;
    private final GemParameters params;
    private final List<RemoteNodeState> nodes;
    private final ScheduledExecutorService scheduler;
    private final Object stateLock;

    private ScheduledFuture<?> heartbeatTask;
    private boolean heartbeatsPaused;

    RuntimeLeaseManager(GemConfig config, GemParameters params, List<RemoteNodeState> nodes,
                        ScheduledExecutorService scheduler) {
        this.config = config;
        this.params = params;
        this.nodes = nodes;
        this.scheduler = scheduler;
        this.stateLock = new Object();
    }

    @SuppressWarnings("unchecked")
    void reserve() throws InterruptedException, ExecutionException, IOException {
        final CompletableFuture<Void>[] reservations = new CompletableFuture[nodes.size()];
        final String[] targetHosts = new String[nodes.size()];
        for (RemoteNodeState node : nodes) {
            targetHosts[node.index()] = node.hostAndPort();
            try {
                reservations[node.index()] = runOperation(node,
                        RemoteAgent.reserveRuntime(node.connectionDirectory(), node.deploymentName(),
                                node.leaseId(), config.runtimeManagementLockTimeoutSeconds,
                                config.runtimeManagementLockStaleSeconds), "Runtime reservation")
                        .thenRun(() -> activate(node));
            } catch (IOException exception) {
                reservations[node.index()] = CompletableFuture.failedFuture(exception);
            }
        }
        Printer.log.info("SBK-GEM: Reserving runtime on {} remote host(s); progress every {} second(s)",
                nodes.size(), config.runtimeProgressIntervalSeconds);
        try (LifecycleProgress progress = new LifecycleProgress("Remote runtime reservation",
                config.runtimeProgressIntervalSeconds, scheduler,
                () -> DeploymentProgress.pendingHosts(reservations, targetHosts))) {
            waitFor(CompletableFuture.allOf(reservations), "runtime deployment reservation");
        }
        startHeartbeats();
        Printer.log.info("SBK-GEM: Runtime reserved on {} remote host(s)", nodes.size());
    }

    @SuppressWarnings("unchecked")
    void acquire(SbkRuntimeBundle bundle) throws InterruptedException, ExecutionException, IOException {
        pauseHeartbeats();
        final CompletableFuture<Void>[] acquisitions = new CompletableFuture[nodes.size()];
        final String[] targetHosts = new String[nodes.size()];
        for (RemoteNodeState node : nodes) {
            targetHosts[node.index()] = node.hostAndPort();
            try {
                acquisitions[node.index()] = runOperation(node,
                        RemoteAgent.acquireRuntime(node.connectionDirectory(), node.deploymentName(),
                                bundle.contentDigest(), node.leaseId(), params.isPackagesCleanup(),
                                config.runtimeManagementLockTimeoutSeconds,
                                config.runtimeManagementLockStaleSeconds,
                                config.runtimeLeaseReservationSeconds), "Runtime setup");
            } catch (IOException exception) {
                acquisitions[node.index()] = CompletableFuture.failedFuture(exception);
            }
        }
        Printer.log.info("SBK-GEM: Preparing managed runtime {} on {} remote host(s); old-runtime cleanup is {}; "
                        + "progress every {} second(s)", bundle.deploymentName(), nodes.size(),
                params.isPackagesCleanup() ? "enabled" : "disabled", config.runtimeProgressIntervalSeconds);
        final long acquisitionSeconds;
        try (LifecycleProgress progress = new LifecycleProgress("Remote runtime setup",
                config.runtimeProgressIntervalSeconds, scheduler,
                () -> DeploymentProgress.pendingHosts(acquisitions, targetHosts))) {
            waitFor(CompletableFuture.allOf(acquisitions), "runtime lease acquisition and retirement");
            acquisitionSeconds = progress.elapsedSeconds();
        }
        startHeartbeats();
        Printer.log.info("SBK-GEM: Reserved runtime {} on {} host(s) in {} second(s); inactive non-current "
                        + "runtime retirement is {}", bundle.deploymentName(), nodes.size(), acquisitionSeconds,
                params.isPackagesCleanup() ? "enabled" : "disabled");
    }

    CompletableFuture<Void> release(RemoteNodeState node) {
        if (!claimActiveLease(node) || node.deploymentName() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return releaseClaimed(node);
    }

    CompletableFuture<SshResponse> launch(RemoteNodeState node, BenchmarkCommandStarter starter)
            throws ConnectException {
        synchronized (stateLock) {
            final CompletableFuture<SshResponse> command = starter.start();
            node.leaseLaunched(true);
            return command;
        }
    }

    boolean isLaunched(RemoteNodeState node) {
        synchronized (stateLock) {
            return node.leaseLaunched();
        }
    }

    private CompletableFuture<Void> releaseClaimed(RemoteNodeState node) {
        try {
            return runOperation(node, RemoteAgent.releaseRuntime(node.connectionDirectory(),
                    node.deploymentName(), node.leaseId(), params.isPackagesCleanup(),
                    config.runtimeManagementLockTimeoutSeconds, config.runtimeManagementLockStaleSeconds,
                    config.runtimeLeaseReservationSeconds), "Runtime lease release");
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    @SuppressWarnings("unchecked")
    void releaseUnlaunched() throws IOException, InterruptedException, ExecutionException {
        final CompletableFuture<Void>[] releases = new CompletableFuture[nodes.size()];
        boolean releaseRequired = false;
        for (RemoteNodeState node : nodes) {
            if (claimUnlaunchedLease(node)) {
                releaseRequired = true;
                releases[node.index()] = releaseClaimed(node);
            } else {
                releases[node.index()] = CompletableFuture.completedFuture(null);
            }
        }
        if (releaseRequired) {
            waitFor(CompletableFuture.allOf(releases), "unlaunched runtime lease release");
        }
    }

    CompletableFuture<Void> cleanupRetired(String[] endpointIdentities) {
        if (!params.isPackagesCleanup()) {
            return CompletableFuture.completedFuture(null);
        }
        final String[] parentDirectories = nodes.stream().map(RemoteNodeState::connectionDirectory)
                .toArray(String[]::new);
        final RemoteTargetPlan targetPlan = RemoteTargetPlan.create(params.getConnections(),
                endpointIdentities, parentDirectories);
        final CompletableFuture<?>[] cleanups = new CompletableFuture[nodes.size()];
        final String[] targetHosts = new String[nodes.size()];
        int targetCount = 0;
        for (RemoteNodeState node : nodes) {
            if (targetPlan.isRepresentative(node.index())) {
                targetHosts[node.index()] = node.hostAndPort();
                targetCount++;
            }
        }
        Printer.log.info("SBK-GEM: Removing unused SBK runtime installations from {} remote host(s); "
                        + "timeout {} second(s); progress every {} second(s)", targetCount,
                config.remoteTimeoutSeconds, config.runtimeProgressIntervalSeconds);
        for (RemoteNodeState node : nodes) {
            if (!targetPlan.isRepresentative(node.index())) {
                cleanups[node.index()] = CompletableFuture.completedFuture(null);
                continue;
            }
            try {
                final String command = RemoteAgent.command(DeploymentSupport.remoteJavaExecutable(node.javaHome()),
                        node.agentPath());
                cleanups[node.index()] = node.session().runCommandAsync(command,
                                RemoteAgent.cleanup(node.connectionDirectory()), true,
                                config.remoteTimeoutSeconds)
                        .thenApply(response -> cleanupResult(response))
                        .whenComplete((deleted, failure) -> logCleanup(node, deleted, failure));
            } catch (IOException exception) {
                cleanups[node.index()] = CompletableFuture.failedFuture(exception);
                Printer.log.warn("SBK-GEM: Unable to start unused SBK runtime cleanup on host '{}': {}",
                        node.hostAndPort(), exception.getMessage());
            }
        }
        final LifecycleProgress progress = new LifecycleProgress("Cleanup of unused SBK runtime installations",
                config.runtimeProgressIntervalSeconds, scheduler,
                () -> DeploymentProgress.pendingHosts(cleanups, targetHosts));
        return CompletableFuture.allOf(cleanups).whenComplete((ignored, failure) -> progress.close());
    }

    private int cleanupResult(SshResponse response) {
        if (!RemoteAgent.successful(response)) {
            final String error = response.errOutputStream.toString();
            final String diagnostic = error.isBlank() ? response.stdOutputStream.toString() : error;
            throw new CompletionException(new IOException("Remote Java-agent cleanup returned "
                    + response.returnCode + (diagnostic.isBlank() ? "" : ": "
                    + DeploymentSupport.diagnosticSummary(diagnostic, config))));
        }
        return RemoteAgent.retiredRuntimeCount(response);
    }

    private void logCleanup(RemoteNodeState node, Integer deleted, Throwable failure) {
        if (failure == null) {
            Printer.log.info("SBK-GEM: Removed {} unused SBK runtime installation(s) from host '{}'",
                    deleted, node.hostAndPort());
        } else {
            Printer.log.warn("SBK-GEM: Unable to remove unused SBK runtimes from host '{}': {}",
                    node.hostAndPort(), DeploymentSupport.failureDescription(failure));
        }
    }

    private void startHeartbeats() {
        final long intervalSeconds = Math.max(MINIMUM_INTERVAL_SECONDS,
                config.runtimeLeaseReservationSeconds / LEASE_HEARTBEAT_DIVISOR);
        synchronized (stateLock) {
            heartbeatsPaused = false;
        }
        try {
            heartbeatTask = scheduler.scheduleWithFixedDelay(this::refreshLeases,
                    intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        } catch (RejectedExecutionException exception) {
            if (!scheduler.isShutdown()) {
                throw exception;
            }
            synchronized (stateLock) {
                heartbeatsPaused = true;
            }
            return;
        }
        Printer.log.info("SBK-GEM: Managed runtime leases will be refreshed every {} second(s)", intervalSeconds);
    }

    private void pauseHeartbeats() throws InterruptedException, ExecutionException, IOException {
        synchronized (stateLock) {
            heartbeatsPaused = true;
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        final CompletableFuture<?>[] heartbeats = new CompletableFuture<?>[nodes.size()];
        synchronized (stateLock) {
            for (RemoteNodeState node : nodes) {
                final CompletableFuture<?> heartbeat = node.leaseHeartbeat();
                heartbeats[node.index()] = heartbeat == null
                        ? CompletableFuture.completedFuture(null) : heartbeat;
            }
        }
        final CompletableFuture<?>[] settled = new CompletableFuture<?>[heartbeats.length];
        for (int i = 0; i < heartbeats.length; i++) {
            settled[i] = heartbeats[i].handle((ignored, failure) -> null);
        }
        waitFor(CompletableFuture.allOf(settled), "runtime lease heartbeat pause");
    }

    private void refreshLeases() {
        for (RemoteNodeState node : nodes) {
            synchronized (stateLock) {
                if (heartbeatsPaused || !node.leaseActive()
                        || (node.leaseHeartbeat() != null && !node.leaseHeartbeat().isDone())) {
                    continue;
                }
                try {
                    node.leaseHeartbeat(runOperation(node, RemoteAgent.heartbeatRuntime(
                                    node.connectionDirectory(), node.deploymentName(), node.leaseId(),
                                    config.runtimeManagementLockTimeoutSeconds,
                                    config.runtimeManagementLockStaleSeconds), "Runtime lease refresh")
                            .whenComplete((ignored, failure) -> logHeartbeatFailure(node, failure)));
                } catch (IOException exception) {
                    Printer.log.warn("SBK-GEM: Unable to start managed runtime lease heartbeat on host '{}': {}",
                            node.hostAndPort(), exception.getMessage());
                }
            }
        }
    }

    private void logHeartbeatFailure(RemoteNodeState node, Throwable failure) {
        if (failure != null && isActive(node)) {
            Printer.log.warn("SBK-GEM: Managed runtime lease heartbeat failed on host '{}': {}",
                    node.hostAndPort(), DeploymentSupport.failureDescription(failure));
        }
    }

    private CompletableFuture<Void> runOperation(RemoteNodeState node, byte[] request, String operation)
            throws ConnectException {
        final String command = RemoteAgent.command(DeploymentSupport.remoteJavaExecutable(node.javaHome()),
                node.agentPath());
        return node.session().runCommandAsync(command, request, true, operationTimeoutSeconds())
                .thenApply(response -> {
                    if (!RemoteAgent.successful(response)) {
                        throw new CompletionException(operationFailure(node, operation, response));
                    }
                    return null;
                });
    }

    private IOException operationFailure(RemoteNodeState node, String operation, SshResponse response) {
        final String error = response.errOutputStream.toString();
        final String diagnostic = DeploymentSupport.diagnosticSummary(error.isBlank()
                ? response.stdOutputStream.toString() : error, config);
        return new IOException("SBK-GEM: " + operation + " failed on host '" + node.host()
                + "' with return code " + response.returnCode
                + (diagnostic.isEmpty() ? "" : ": " + diagnostic));
    }

    private long operationTimeoutSeconds() {
        if (config.runtimeManagementLockTimeoutSeconds >= Long.MAX_VALUE - config.remoteTimeoutSeconds) {
            return Long.MAX_VALUE;
        }
        return config.runtimeManagementLockTimeoutSeconds + config.remoteTimeoutSeconds;
    }

    private void waitFor(CompletableFuture<?> future, String operation) throws IOException,
            InterruptedException, ExecutionException {
        try {
            future.get(config.deploymentTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            final String message = "SBK-GEM: " + operation + " timed out after "
                    + config.deploymentTimeoutSeconds + " seconds";
            Printer.log.error(message);
            throw new IOException(message, exception);
        }
    }

    private void activate(RemoteNodeState node) {
        synchronized (stateLock) {
            node.leaseActive(true);
        }
    }

    private boolean isActive(RemoteNodeState node) {
        synchronized (stateLock) {
            return node.leaseActive();
        }
    }

    private boolean claimActiveLease(RemoteNodeState node) {
        synchronized (stateLock) {
            final boolean active = node.leaseActive();
            node.leaseActive(false);
            return active;
        }
    }

    boolean claimUnlaunchedLease(RemoteNodeState node) {
        synchronized (stateLock) {
            if (!node.leaseActive() || node.leaseLaunched()) {
                return false;
            }
            node.leaseActive(false);
            return true;
        }
    }
}

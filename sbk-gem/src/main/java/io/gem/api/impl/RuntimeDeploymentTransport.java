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
import io.sbk.config.ExitCode;
import io.sbk.system.Printer;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/** Transfers, activates, and verifies immutable SBK runtime bundles. */
final class RuntimeDeploymentTransport {
    private final GemConfig config;
    private final GemParameters params;
    private final List<RemoteNodeState> nodes;
    private final int controllerJavaVersion;
    private final ScheduledExecutorService scheduler;

    RuntimeDeploymentTransport(GemConfig config, GemParameters params, List<RemoteNodeState> nodes,
                               int controllerJavaVersion, ScheduledExecutorService scheduler) {
        this.config = config;
        this.params = params;
        this.nodes = nodes;
        this.controllerJavaVersion = controllerJavaVersion;
        this.scheduler = scheduler;
    }

    @SuppressWarnings("unchecked")
    void uploadAndActivate(SbkRuntimeBundle bundle, boolean[] copyTargets,
                           DeploymentPlatform platform) throws ConnectException,
            InterruptedException, ExecutionException, IOException {
        final String transferId = Long.toUnsignedString(System.nanoTime());
        final String[] archivePaths = new String[nodes.size()];
        final String[] stagingDirectories = new String[nodes.size()];
        final String[] deploymentDirectories = new String[nodes.size()];
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            deploymentDirectories[index] = node.deploymentDirectory();
            archivePaths[index] = node.deploymentDirectory() + "." + transferId + ".tar";
            stagingDirectories[index] = node.deploymentDirectory() + "." + transferId + ".staging";
        }
        final RemoteTargetPlan targetPlan = RemoteTargetPlan.create(params.getConnections(),
                endpointIdentities(), deploymentDirectories);
        final boolean[] physicalCopyTargets = targetPlan.representativeSelection(copyTargets);

        copyArchive(bundle, physicalCopyTargets, archivePaths, deploymentDirectories);
        final CompletableFuture<SshResponse>[] activations = activate(bundle, physicalCopyTargets,
                archivePaths, stagingDirectories, deploymentDirectories, platform);
        waitFor(CompletableFuture.allOf(activations), "runtime archive activation");
        final boolean[] retryTargets = archiveDigestMismatchTargets(activations, physicalCopyTargets);
        if (hasSelectedTarget(retryTargets)) {
            Printer.log.warn("SBK-GEM: Remote archive integrity verification failed; rebuilding the local "
                    + "runtime archive and retrying affected target(s) once");
            bundle.rebuildArchive();
            retryActivation(bundle, retryTargets, archivePaths, stagingDirectories,
                    deploymentDirectories, platform, activations);
        }
        requireSuccessful(activations, physicalCopyTargets, "Activating immutable runtime");
        Printer.log.info("SBK-GEM: Immutable runtime archive verified and atomically activated");
    }

    @SuppressWarnings("unchecked")
    void verify(SbkRuntimeBundle bundle, boolean[] copyTargets, DeploymentPlatform platform)
            throws ConnectException, InterruptedException, ExecutionException, IOException {
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.size()];
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            probes[index] = copyTargets[index] ? node.session().runCommandAsync(
                    RemoteAgent.command(DeploymentSupport.remoteJavaExecutable(node.javaHome()), node.agentPath()),
                    RemoteAgent.verify(node.deploymentDirectory(), bundle.contentDigest(), config.sbkVersion,
                            platform.operatingSystem()), true, config.remoteTimeoutSeconds)
                    : CompletableFuture.completedFuture(new SshResponse(true));
        }
        waitFor(CompletableFuture.allOf(probes), "activated runtime verification");
        requireSuccessful(probes, copyTargets, "Verifying activated immutable runtime");
        Printer.log.info("SBK-GEM: Runtime content {}, Java {} or newer, and SBK {} verified on selected hosts",
                bundle.contentDigest(), controllerJavaVersion, config.sbkVersion);
    }

    @SuppressWarnings("unchecked")
    private void copyArchive(SbkRuntimeBundle bundle, boolean[] physicalCopyTargets,
                             String[] archivePaths, String[] deploymentDirectories)
            throws IOException, InterruptedException, ExecutionException {
        try (SbkRuntimeBundle.ArchiveUse ignored = bundle.acquireArchiveUse()) {
            final CompletableFuture<?>[] uploads = new CompletableFuture[nodes.size()];
            final String[] transferHosts = new String[nodes.size()];
            final AtomicLong[] copiedBytes = new AtomicLong[nodes.size()];
            int transferCount = 0;
            for (RemoteNodeState node : nodes) {
                final int index = node.index();
                copiedBytes[index] = new AtomicLong();
                if (!physicalCopyTargets[index]) {
                    uploads[index] = CompletableFuture.completedFuture(null);
                } else {
                    transferHosts[index] = node.hostAndPort();
                    transferCount++;
                    uploads[index] = node.session().copyFileAsync(bundle.archive().toString(), archivePaths[index],
                            config.deploymentTimeoutSeconds, copiedBytes[index]::addAndGet);
                }
            }
            final long archiveBytes = java.nio.file.Files.size(bundle.archive());
            Printer.log.info("SBK-GEM: Bulk SCP copying immutable runtime archive {} ({}) to {} unique "
                            + "remote target(s); progress every {} second(s)", bundle.archive().getFileName(),
                    DeploymentProgress.formatSize(archiveBytes), transferCount,
                    config.runtimeProgressIntervalSeconds);
            logDestinations(physicalCopyTargets, archivePaths, deploymentDirectories);
            final long transferSeconds;
            final long copyStartedNanos = System.nanoTime();
            try (LifecycleProgress progress = new LifecycleProgress("Immutable runtime archive copy",
                    config.runtimeProgressIntervalSeconds, scheduler,
                    () -> DeploymentProgress.copyStatus(uploads, transferHosts, copiedBytes,
                            archiveBytes, copyStartedNanos, "transfer(s)"))) {
                waitFor(CompletableFuture.allOf(uploads), "runtime archive upload");
                transferSeconds = progress.elapsedSeconds();
            }
            Printer.log.info("SBK-GEM: Copied immutable runtime archive {} to {} unique remote target(s) "
                            + "in {} second(s)", bundle.archive().getFileName(), transferCount, transferSeconds);
        }
    }

    private void logDestinations(boolean[] selected, String[] archivePaths, String[] deploymentDirectories) {
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            if (selected[index]) {
                Printer.log.info("SBK-GEM: Host '{}:{}' temporary runtime archive destination: '{}'; "
                                + "SBK execution directory after activation: '{}'", node.host(),
                        node.session().connection.getPort(), archivePaths[index],
                        DeploymentSupport.remoteSbkDirectory(deploymentDirectories[index]));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SshResponse>[] activate(SbkRuntimeBundle bundle, boolean[] selected,
                                                       String[] archivePaths, String[] stagingDirectories,
                                                       String[] deploymentDirectories,
                                                       DeploymentPlatform platform) throws IOException {
        final CompletableFuture<SshResponse>[] activations = new CompletableFuture[nodes.size()];
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            if (!selected[index]) {
                activations[index] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                activations[index] = activate(node, bundle, archivePaths[index], stagingDirectories[index],
                        deploymentDirectories[index], platform);
            }
        }
        return activations;
    }

    private CompletableFuture<SshResponse> activate(RemoteNodeState node, SbkRuntimeBundle bundle,
                                                     String archivePath, String stagingDirectory,
                                                     String deploymentDirectory,
                                                     DeploymentPlatform platform) throws IOException {
        final String command = RemoteAgent.command(DeploymentSupport.remoteJavaExecutable(node.javaHome()),
                node.agentPath());
        return node.session().runCommandAsync(command, RemoteAgent.activate(archivePath,
                        bundle.archiveDigest(), bundle.contentDigest(), stagingDirectory,
                        deploymentDirectory, platform.operatingSystem()),
                true, config.deploymentTimeoutSeconds);
    }

    @SuppressWarnings("unchecked")
    private void retryActivation(SbkRuntimeBundle bundle, boolean[] retryTargets, String[] archivePaths,
                                 String[] stagingDirectories, String[] deploymentDirectories,
                                 DeploymentPlatform platform, CompletableFuture<SshResponse>[] activations)
            throws ConnectException, InterruptedException, ExecutionException, IOException {
        final CompletableFuture<?>[] retryUploads = new CompletableFuture[nodes.size()];
        try (SbkRuntimeBundle.ArchiveUse ignored = bundle.acquireArchiveUse()) {
            for (RemoteNodeState node : nodes) {
                final int index = node.index();
                retryUploads[index] = retryTargets[index]
                        ? node.session().copyFileAsync(bundle.archive().toString(), archivePaths[index],
                        config.deploymentTimeoutSeconds)
                        : CompletableFuture.completedFuture(null);
            }
            waitFor(CompletableFuture.allOf(retryUploads), "runtime archive integrity retry upload");
        }
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            if (retryTargets[index]) {
                activations[index] = activate(node, bundle, archivePaths[index], stagingDirectories[index],
                        deploymentDirectories[index], platform);
            }
        }
        waitFor(CompletableFuture.allOf(activations), "runtime archive integrity retry activation");
    }

    private void requireSuccessful(CompletableFuture<SshResponse>[] futures, boolean[] selected,
                                   String operation) throws IOException, InterruptedException,
            ExecutionException {
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            if (selected[index]) {
                final SshResponse response = futures[index].get();
                if (response.returnCode != ExitCode.SUCCESS) {
                    final String errorOutput = response.errOutputStream.toString();
                    final String diagnostic = DeploymentSupport.diagnosticSummary(errorOutput.isBlank()
                            ? response.stdOutputStream.toString() : errorOutput, config);
                    throw new IOException("SBK-GEM: " + operation + " failed on host '"
                            + node.host() + "' with return code " + response.returnCode
                            + (diagnostic.isEmpty() ? "" : ": " + diagnostic));
                }
            }
        }
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

    private String[] endpointIdentities() {
        return nodes.stream().map(RemoteNodeState::endpointIdentity).toArray(String[]::new);
    }

    static boolean[] archiveDigestMismatchTargets(CompletableFuture<SshResponse>[] activations,
                                                   boolean[] selected)
            throws InterruptedException, ExecutionException {
        final boolean[] retryTargets = new boolean[selected.length];
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                final SshResponse response = activations[i].get();
                if (response.returnCode != ExitCode.SUCCESS && !RemoteAgent.archiveDigestMismatch(response)) {
                    return new boolean[selected.length];
                }
                retryTargets[i] = RemoteAgent.archiveDigestMismatch(response);
            }
        }
        return retryTargets;
    }

    private static boolean hasSelectedTarget(boolean[] selected) {
        for (boolean value : selected) {
            if (value) {
                return true;
            }
        }
        return false;
    }
}

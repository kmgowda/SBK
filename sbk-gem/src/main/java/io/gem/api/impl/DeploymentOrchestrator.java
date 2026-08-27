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

import io.gem.agent.RemotePath;
import io.gem.api.SshResponse;
import io.gem.config.GemConfig;
import io.gem.params.GemParameters;
import io.sbk.config.ExitCode;
import io.sbk.system.Printer;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Coordinates immutable SBK bundle creation, deployment, activation, and lease acquisition. */
final class DeploymentOrchestrator {
    /** Performs the archive transport and post-activation verification. */
    interface RuntimeTransport {
        void uploadAndActivate(SbkRuntimeBundle bundle, boolean[] copyTargets, DeploymentPlatform platform)
                throws ConnectException, InterruptedException, ExecutionException, IOException;

        void verify(SbkRuntimeBundle bundle, boolean[] copyTargets, DeploymentPlatform platform)
                throws ConnectException, InterruptedException, ExecutionException, IOException;
    }

    private final GemConfig config;
    private final GemParameters params;
    private final List<RemoteNodeState> nodes;
    private final int controllerJavaVersion;
    private final RuntimeCopyPolicy copyPolicy;
    private final RuntimeLeaseManager leaseManager;
    private final ScheduledExecutorService scheduler;
    private final String leaseRunId;
    private final RuntimeTransport transport;

    DeploymentOrchestrator(GemConfig config, GemParameters params, List<RemoteNodeState> nodes,
                           int controllerJavaVersion, RuntimeCopyPolicy copyPolicy,
                           RuntimeLeaseManager leaseManager, ScheduledExecutorService scheduler,
                           String leaseRunId, RuntimeTransport transport) {
        this.config = config;
        this.params = params;
        this.nodes = nodes;
        this.controllerJavaVersion = controllerJavaVersion;
        this.copyPolicy = copyPolicy;
        this.leaseManager = leaseManager;
        this.scheduler = scheduler;
        this.leaseRunId = leaseRunId;
        this.transport = transport;
    }

    void deploy(DeploymentPlatform platform) throws IOException, ConnectException,
            InterruptedException, ExecutionException {
        final Path cacheDirectory = runtimeCacheDirectory();
        final Path sourceDirectory = Paths.get(params.getSbkDir()).toAbsolutePath().normalize();
        Printer.log.info("SBK-GEM: Preparing immutable runtime bundle for {}; progress every {} second(s)",
                platform.id(), config.runtimeProgressIntervalSeconds);
        final SbkRuntimeBundle bundle;
        final long preparationMillis;
        try (LifecycleProgress progress = new LifecycleProgress("Immutable runtime bundle preparation for "
                + platform.id(), config.runtimeProgressIntervalSeconds, scheduler,
                () -> "validating, hashing, or compressing SBK files")) {
            bundle = copyPolicy.createSbkRuntime(new RuntimeCopyPolicy.SbkRuntimeSource(
                    sourceDirectory, GemConfig.SBK_COMMAND, config.sbkVersion, controllerJavaVersion,
                    platform, cacheDirectory, config.driverClass));
            preparationMillis = progress.elapsedMillis();
        }
        Printer.log.info("SBK-GEM: {} SBK runtime bundle '{}' {} source directory '{}' in {} ms; {}; "
                        + "content SHA-256 {}; archive SHA-256 {}",
                bundle.archiveReused() ? "Reused cached" : "Built", bundle.archive(),
                bundle.archiveReused() ? "for" : "from", sourceDirectory, preparationMillis,
                SbkGemBenchmark.formatTransferSize(Files.size(bundle.archive())), bundle.contentDigest(),
                bundle.archiveDigest());
        deployBundle(bundle, platform);
        if (params.isPackagesCleanup()) {
            final int removed = SbkRuntimeBundle.cleanupOtherCachedBundles(cacheDirectory,
                    bundle.deploymentName());
            Printer.log.info("SBK-GEM: Retained local runtime bundle {}; removed {} inactive non-current "
                    + "cached bundle(s)", bundle.deploymentName(), removed);
        }
    }

    @SuppressWarnings("unchecked")
    private void deployBundle(SbkRuntimeBundle bundle, DeploymentPlatform platform) throws ConnectException,
            InterruptedException, ExecutionException, IOException {
        final boolean[] copyTargets = new boolean[nodes.size()];
        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.size()];
        for (RemoteNodeState node : nodes) {
            node.deploymentName(bundle.deploymentName());
            node.deploymentDirectory(RemotePath.join(node.connectionDirectory(), bundle.deploymentName()));
            node.leaseId(leaseRunId + "-" + node.index());
        }
        leaseManager.reserve();
        for (RemoteNodeState node : nodes) {
            probes[node.index()] = node.session().runCommandAsync(
                    RemoteAgent.command(DeploymentSupport.remoteJavaExecutable(node.javaHome()), node.agentPath()),
                    RemoteAgent.verify(node.deploymentDirectory(), bundle.contentDigest(), config.sbkVersion,
                            platform.operatingSystem()), true, config.remoteTimeoutSeconds);
        }
        waitFor(CompletableFuture.allOf(probes), "remote immutable runtime checks");
        for (RemoteNodeState node : nodes) {
            if (probes[node.index()].get().returnCode == ExitCode.SUCCESS) {
                Printer.log.info("SBK-GEM: Host '{}' will use existing SBK installation '{}'; skipping copy",
                        node.host(), SbkGemBenchmark.remoteSbkDirectory(node.deploymentDirectory()));
            } else {
                copyTargets[node.index()] = true;
            }
        }
        if (hasSelectedTarget(copyTargets)) {
            transport.uploadAndActivate(bundle, copyTargets, platform);
            transport.verify(bundle, copyTargets, platform);
            for (RemoteNodeState node : nodes) {
                if (copyTargets[node.index()]) {
                    Printer.log.info("SBK-GEM: Host '{}' will use newly activated SBK installation '{}'",
                            node.host(), SbkGemBenchmark.remoteSbkDirectory(node.deploymentDirectory()));
                }
            }
        } else {
            Printer.log.info("SBK-GEM: Immutable runtime {} is already available on every host",
                    bundle.deploymentName());
        }
        leaseManager.acquire(bundle);
    }

    private Path runtimeCacheDirectory() {
        final Path configuredCache = Paths.get(config.runtimeCacheDirectory);
        return configuredCache.isAbsolute() ? configuredCache
                : Paths.get(System.getProperty("user.home")).resolve(configuredCache);
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

    private static boolean hasSelectedTarget(boolean[] selected) {
        for (boolean value : selected) {
            if (value) {
                return true;
            }
        }
        return false;
    }
}

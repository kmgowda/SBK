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
import io.gem.config.GemConfig;
import io.gem.params.GemParameters;
import io.sbk.system.Printer;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

/** Coordinates immutable SBK bundle creation, deployment, activation, and lease acquisition. */
final class DeploymentOrchestrator {
    private final GemConfig config;
    private final Path sourceDirectory;
    private final boolean packagesCleanup;
    private final List<RemoteNodeState> nodes;
    private final int controllerJavaVersion;
    private final RuntimeCopyPolicy copyPolicy;
    private final RuntimeLeaseController leaseManager;
    private final ScheduledExecutorService scheduler;
    private final String leaseRunId;
    private final DeploymentTransport transport;

    DeploymentOrchestrator(GemConfig config, GemParameters params, List<RemoteNodeState> nodes,
                           int controllerJavaVersion, RuntimeCopyPolicy copyPolicy,
                           RuntimeLeaseController leaseManager, ScheduledExecutorService scheduler,
                           String leaseRunId, DeploymentTransport transport) {
        this.config = config;
        this.sourceDirectory = Path.of(params.getSbkDir()).toAbsolutePath().normalize();
        this.packagesCleanup = params.isPackagesCleanup();
        this.nodes = nodes;
        this.controllerJavaVersion = controllerJavaVersion;
        this.copyPolicy = copyPolicy;
        this.leaseManager = leaseManager;
        this.scheduler = scheduler;
        this.leaseRunId = leaseRunId;
        this.transport = transport;
    }

    DeploymentOrchestrator(GemConfig config, Path sourceDirectory, boolean packagesCleanup,
                           List<RemoteNodeState> nodes, int controllerJavaVersion,
                           RuntimeCopyPolicy copyPolicy, RuntimeLeaseController leaseManager,
                           ScheduledExecutorService scheduler, String leaseRunId,
                           DeploymentTransport transport) {
        this.config = config;
        this.sourceDirectory = sourceDirectory.toAbsolutePath().normalize();
        this.packagesCleanup = packagesCleanup;
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
        final Path cacheDirectory = DeploymentSupport.runtimeCacheDirectory(config);
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
                DeploymentProgress.formatSize(Files.size(bundle.archive())), bundle.contentDigest(),
                bundle.archiveDigest());
        deployBundle(bundle, platform);
        if (packagesCleanup) {
            final int removed = SbkRuntimeBundle.cleanupOtherCachedBundles(cacheDirectory,
                    bundle.deploymentName());
            Printer.log.info("SBK-GEM: Retained local runtime bundle {}; removed {} inactive non-current "
                    + "cached bundle(s)", bundle.deploymentName(), removed);
        }
    }

    private void deployBundle(SbkRuntimeBundle bundle, DeploymentPlatform platform) throws ConnectException,
            InterruptedException, ExecutionException, IOException {
        for (RemoteNodeState node : nodes) {
            node.deploymentName(bundle.deploymentName());
            node.deploymentDirectory(RemotePath.join(node.connectionDirectory(), bundle.deploymentName()));
            node.leaseId(leaseRunId + "-" + node.index());
        }
        leaseManager.reserve();
        final boolean[] copyTargets = transport.missingTargets(bundle, platform);
        for (RemoteNodeState node : nodes) {
            if (!copyTargets[node.index()]) {
                Printer.log.info("SBK-GEM: Host '{}' will use existing SBK installation '{}'; skipping copy",
                        node.host(), DeploymentSupport.remoteSbkDirectory(node.deploymentDirectory()));
            }
        }
        if (DeploymentSupport.hasSelectedTarget(copyTargets)) {
            transport.uploadAndActivate(bundle, copyTargets, platform);
            transport.verify(bundle, copyTargets, platform);
            for (RemoteNodeState node : nodes) {
                if (copyTargets[node.index()]) {
                    Printer.log.info("SBK-GEM: Host '{}' will use newly activated SBK installation '{}'",
                            node.host(), DeploymentSupport.remoteSbkDirectory(node.deploymentDirectory()));
                }
            }
        } else {
            Printer.log.info("SBK-GEM: Immutable runtime {} is already available on every host",
                    bundle.deploymentName());
        }
        leaseManager.acquire(bundle);
    }

}

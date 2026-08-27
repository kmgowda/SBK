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
import io.gem.api.ConnectionConfig;
import io.gem.api.SshResponse;
import io.gem.config.GemConfig;
import io.gem.params.GemParameters;
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
import java.util.concurrent.atomic.AtomicLong;

/** Bootstraps the remote agent and provisions a compatible Java runtime when required. */
final class RemoteEnvironmentPreparer {
    private static final String SYSTEM_JAVA_EXECUTABLE = "java";

    private final GemConfig config;
    private final GemParameters params;
    private final List<RemoteNodeState> nodes;
    private final int controllerJavaVersion;
    private final RuntimeCopyPolicy copyPolicy;
    private final ScheduledExecutorService scheduler;

    RemoteEnvironmentPreparer(GemConfig config, GemParameters params, List<RemoteNodeState> nodes,
                              int controllerJavaVersion, RuntimeCopyPolicy copyPolicy,
                              ScheduledExecutorService scheduler) {
        this.config = config;
        this.params = params;
        this.nodes = nodes;
        this.controllerJavaVersion = controllerJavaVersion;
        this.copyPolicy = copyPolicy;
        this.scheduler = scheduler;
    }

    @SuppressWarnings("unchecked")
    DeploymentPlatform prepare() throws ConnectException, InterruptedException, ExecutionException, IOException {
        final DeploymentPlatform localPlatform = DeploymentPlatform.local();
        final Path localAgent = localAgent();
        final String agentDigest = RemoteAgentFiles.digest(localAgent);
        final ConnectionConfig[] connections = params.getConnections();
        final RemoteTargetPlan targetPlan = RemoteTargetPlan.createBeforeDirectoryResolution(connections,
                endpointIdentities());
        final CompletableFuture<RemoteJavaBootstrap>[] bootstraps = new CompletableFuture[nodes.size()];
        final String[] targetHosts = new String[nodes.size()];
        final String configuredJavaHome = RemotePath.normalize(params.getJavaDir());
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            if (targetPlan.isRepresentative(index)) {
                targetHosts[index] = node.hostAndPort();
                bootstraps[index] = prepareAgent(node, connections[index], localAgent,
                        agentDigest, configuredJavaHome);
            } else {
                bootstraps[index] = bootstraps[targetPlan.representative(index)];
            }
        }
        try (LifecycleProgress progress = new LifecycleProgress("Remote Java bootstrap",
                config.runtimeProgressIntervalSeconds, scheduler,
                () -> DeploymentProgress.pendingHosts(bootstraps, targetHosts))) {
            waitFor(CompletableFuture.allOf(bootstraps), "remote Java bootstrap");
        }

        final boolean[] unresolved = new boolean[nodes.size()];
        final SshResponse[] javaProbes = new SshResponse[nodes.size()];
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            final RemoteJavaBootstrap bootstrap = bootstraps[index].get();
            node.connectionDirectory(bootstrap.directory());
            node.agentPath(bootstrap.agentPath());
            javaProbes[index] = bootstrap.javaProbe();
            node.javaHome(RemoteAgent.javaHome(bootstrap.javaProbe()));
            unresolved[index] = node.javaHome() == null || node.javaHome().isBlank();
        }

        if (hasSelectedTarget(unresolved)) {
            provisionJava(unresolved, javaProbes);
        }
        if (hasSelectedTarget(unresolved)) {
            throw new IOException("SBK-GEM: Java " + controllerJavaVersion
                    + " or newer could not be provisioned");
        }
        return verifyPlatforms(javaProbes, localPlatform);
    }

    private Path localAgent() throws IOException {
        final Path agent = Path.of(params.getSbkDir(), "lib", "sbk-gem-agent-"
                + config.sbkVersion + ".jar").toAbsolutePath().normalize();
        if (!Files.isRegularFile(agent)) {
            throw new IOException("SBK-GEM remote agent is missing from the installed distribution: " + agent);
        }
        return agent;
    }

    private CompletableFuture<RemoteJavaBootstrap> prepareAgent(RemoteNodeState node,
                                                                 ConnectionConfig connection,
                                                                 Path localAgent, String agentDigest,
                                                                 String configuredJavaHome)
            throws ConnectException {
        final CompletableFuture<RemoteAgentFiles.AgentBootstrap> agentPreparation =
                node.session().runRemoteFileOperationAsync(
                        fileSystem -> RemoteAgentFiles.prepare(fileSystem, connection.getDir(), localAgent,
                                config.sbkVersion, agentDigest), config.deploymentTimeoutSeconds);
        return agentPreparation.thenCompose(agent -> {
            final String javaExecutable = configuredJavaHome == null ? SYSTEM_JAVA_EXECUTABLE
                    : DeploymentSupport.remoteJavaExecutable(configuredJavaHome);
            try {
                return node.session().runCommandAsync(RemoteAgent.command(javaExecutable, agent.agentPath()),
                                RemoteAgent.probe(controllerJavaVersion), true, config.remoteTimeoutSeconds)
                        .thenApply(response -> new RemoteJavaBootstrap(agent.directory(), agent.agentPath(),
                                response));
            } catch (IOException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void provisionJava(boolean[] unresolved, SshResponse[] javaProbes)
            throws IOException, InterruptedException, ExecutionException {
        final String javaDeploymentName = copyPolicy.javaDeploymentName();
        Printer.log.info("SBK-GEM: Java {} or newer is missing on selected host(s); preparing a separate "
                        + "content-addressed {} bulk SCP transfer", controllerJavaVersion,
                javaDeploymentName);
        final Path javaSourceDirectory = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
        final Path localSbkDirectory = Paths.get(params.getSbkDir()).toAbsolutePath().normalize();
        final ManagedJavaRuntime javaRuntime = copyPolicy.createJavaRuntime(
                new RuntimeCopyPolicy.JavaRuntimeSource(javaSourceDirectory, controllerJavaVersion,
                        runtimeCacheDirectory(), localSbkDirectory));
        final Path javaArchive;
        final long archivePreparationMillis;
        try (LifecycleProgress progress = new LifecycleProgress("Managed " + javaDeploymentName
                + " archive preparation", config.runtimeProgressIntervalSeconds, scheduler,
                () -> "creating or validating the cached single-file tar archive")) {
            javaArchive = javaRuntime.prepareArchive();
            archivePreparationMillis = progress.elapsedMillis();
        }
        Printer.log.info("SBK-GEM: {} managed {} archive '{}' {} source directory '{}' in {} ms; {}",
                javaRuntime.archiveReused() ? "Reused cached" : "Built", javaDeploymentName, javaArchive,
                javaRuntime.archiveReused() ? "for" : "from", javaSourceDirectory,
                archivePreparationMillis, DeploymentProgress.formatSize(javaRuntime.archiveBytes()));

        final String[] javaParentDirectories = new String[nodes.size()];
        for (RemoteNodeState node : nodes) {
            javaParentDirectories[node.index()] = RemotePath.parent(node.connectionDirectory());
        }
        final RemoteTargetPlan targetPlan = RemoteTargetPlan.create(params.getConnections(),
                endpointIdentities(), javaParentDirectories);
        final CompletableFuture<String>[] copies = new CompletableFuture[nodes.size()];
        final AtomicLong[] copiedBytes = new AtomicLong[nodes.size()];
        final String[] copyHosts = new String[nodes.size()];
        final long copyStartedNanos = System.nanoTime();
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            copiedBytes[index] = new AtomicLong();
            if (!targetPlan.isRepresentative(index)) {
                copies[index] = copies[targetPlan.representative(index)];
            } else if (targetPlan.hasSelectedNode(index, unresolved)) {
                copyHosts[index] = node.hostAndPort();
                copies[index] = javaRuntime.installBulk(node.session(), javaParentDirectories[index],
                        config.deploymentTimeoutSeconds, copiedBytes[index]::addAndGet);
            } else {
                copies[index] = CompletableFuture.completedFuture(node.javaHome());
            }
        }
        final long copySeconds;
        try (LifecycleProgress progress = new LifecycleProgress("Separate Java runtime copy",
                config.runtimeProgressIntervalSeconds, scheduler,
                () -> DeploymentProgress.copyStatus(copies, copyHosts, copiedBytes,
                        javaRuntime.archiveBytes(), copyStartedNanos, "Java operation(s)"))) {
            waitFor(CompletableFuture.allOf(copies), "separate remote Java provisioning");
            copySeconds = progress.elapsedSeconds();
        }
        Printer.log.info("SBK-GEM: Separate Java provisioning completed in {} second(s); {} transferred",
                copySeconds, DeploymentProgress.formatSize(DeploymentProgress.copiedByteCount(copiedBytes)));
        verifyProvisionedJava(targetPlan, unresolved, copies, javaProbes);
    }

    @SuppressWarnings("unchecked")
    private void verifyProvisionedJava(RemoteTargetPlan targetPlan, boolean[] unresolved,
                                       CompletableFuture<String>[] copies, SshResponse[] javaProbes)
            throws IOException, InterruptedException, ExecutionException {
        final CompletableFuture<SshResponse>[] provisionedProbes = new CompletableFuture[nodes.size()];
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            node.javaHome(copies[index].get());
            if (!targetPlan.isRepresentative(index)) {
                provisionedProbes[index] = provisionedProbes[targetPlan.representative(index)];
            } else if (targetPlan.hasSelectedNode(index, unresolved)) {
                provisionedProbes[index] = node.session().runCommandAsync(
                        RemoteAgent.command(DeploymentSupport.remoteJavaExecutable(node.javaHome()),
                                node.agentPath()), RemoteAgent.probe(controllerJavaVersion), true,
                        config.remoteTimeoutSeconds);
            } else {
                provisionedProbes[index] = CompletableFuture.completedFuture(javaProbes[index]);
            }
        }
        waitFor(CompletableFuture.allOf(provisionedProbes), "provisioned Java verification");
        for (RemoteNodeState node : nodes) {
            final int index = node.index();
            javaProbes[index] = provisionedProbes[index].get();
            if (!RemoteAgent.successful(javaProbes[index])) {
                throw new IOException("Provisioned JDK verification failed on " + node.host() + ": "
                        + DeploymentSupport.diagnosticSummary(javaProbes[index].errOutputStream.toString(),
                        config));
            }
            unresolved[index] = false;
        }
    }

    private DeploymentPlatform verifyPlatforms(SshResponse[] javaProbes,
                                                DeploymentPlatform localPlatform) throws IOException {
        DeploymentPlatform verifiedPlatform = null;
        for (RemoteNodeState node : nodes) {
            final DeploymentPlatform platform = RemoteAgent.platform(javaProbes[node.index()]);
            if (platform == null || !localPlatform.equals(platform)) {
                throw new IOException("Homogeneous deployment required; controller is " + localPlatform.id()
                        + " but host '" + node.host() + "' is "
                        + (platform == null ? "unknown" : platform.id()));
            }
            verifiedPlatform = platform;
            Printer.log.info("SBK-GEM: Host '{}' will use SBK_JAVA_HOME='{}'", node.host(), node.javaHome());
        }
        Printer.log.info("SBK-GEM: Matching OS {} and Java major {} or newer verified on {} host(s)",
                verifiedPlatform.id(), controllerJavaVersion, nodes.size());
        return verifiedPlatform;
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

    private String[] endpointIdentities() {
        return nodes.stream().map(RemoteNodeState::endpointIdentity).toArray(String[]::new);
    }

    private static boolean hasSelectedTarget(boolean[] selected) {
        for (boolean value : selected) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    private record RemoteJavaBootstrap(String directory, String agentPath, SshResponse javaProbe) {
    }
}

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

import io.gem.api.ConnectionConfig;
import io.gem.api.SshResponse;
import io.gem.api.SshSession;
import io.gem.config.GemConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Direct happy-path coverage for remote Java/agent environment preparation. */
final class RemoteEnvironmentPreparerTest {
    @TempDir
    private Path temporaryDirectory;

    private final ExecutorService sshExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void shutdownExecutors() {
        scheduler.shutdownNow();
        sshExecutor.shutdownNow();
    }

    @Test
    void acceptsExistingCompatibleRemoteJavaWithoutProvisioning() throws Exception {
        final Path sbk = Files.createDirectories(temporaryDirectory.resolve("sbk"));
        final Path agent = Files.createDirectories(sbk.resolve("lib")).resolve("sbk-gem-agent-10.6.jar");
        Files.writeString(agent, "agent", StandardCharsets.UTF_8);
        final ConnectionConfig connection = new ConnectionConfig("node", "user", "", 22,
                "/remote", false, "");
        final SshSession session = new SshSession(connection, sshExecutor);
        final RemoteNodeState node = new RemoteNodeState(0, session, List.of());
        node.endpointIdentity("127.0.0.1");
        final GemConfig config = new GemConfig();
        config.sbkVersion = "10.6";
        config.runtimeProgressIntervalSeconds = 60;
        config.deploymentTimeoutSeconds = 5;
        final DeploymentPlatform expected = DeploymentPlatform.local();
        final SshResponse probe = new SshResponse(true);
        probe.stdOutputStream.write(("SBK_JAVA_HOME=/remote/java\nSBK_OS=" + expected.operatingSystem() + "\n")
                .getBytes(StandardCharsets.UTF_8));
        final RuntimeCopyPolicy noProvisioning = new RuntimeCopyPolicy() {
            @Override
            public String javaDeploymentName() {
                return "unused";
            }

            @Override
            public ManagedJavaRuntime createJavaRuntime(JavaRuntimeSource source) {
                throw new AssertionError("Compatible remote Java must not be provisioned");
            }

            @Override
            public SbkRuntimeBundle createSbkRuntime(SbkRuntimeSource source) {
                throw new AssertionError("SBK deployment is a later phase");
            }
        };
        final RemoteEnvironmentPreparer preparer = new RemoteEnvironmentPreparer(config,
                new ConnectionConfig[]{connection}, sbk, "", List.of(node), Runtime.version().feature(),
                noProvisioning, scheduler, (state, ignoredConnection, ignoredAgent, ignoredDigest,
                        ignoredJavaHome) -> java.util.concurrent.CompletableFuture.completedFuture(
                                new RemoteEnvironmentPreparer.RemoteJavaBootstrap("/remote/sbk-gem",
                                        "/remote/agent.jar", probe)));
        try {
            assertEquals(expected, preparer.prepare());
        } finally {
            session.stop();
        }

        assertEquals("/remote/sbk-gem", node.connectionDirectory());
        assertEquals("/remote/agent.jar", node.agentPath());
        assertEquals("/remote/java", node.javaHome());
    }
}

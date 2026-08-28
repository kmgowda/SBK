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
import io.gem.api.SshSession;
import io.gem.config.GemConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Direct happy-path coverage for the immutable runtime deployment coordinator. */
final class DeploymentOrchestratorTest {
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
    void reservesCopiesVerifiesAndAcquiresSelectedRuntime() throws Exception {
        final DeploymentPlatform platform = DeploymentPlatform.local();
        final Path distribution = createDistribution();
        final SbkRuntimeBundle bundle = SbkRuntimeBundle.create(distribution, "bin/sbk", "10.6", 25,
                platform, temporaryDirectory.resolve("cache"));
        final GemConfig config = config();
        final SshSession session = session();
        final RemoteNodeState node = new RemoteNodeState(0, session, List.of());
        node.connectionDirectory("/remote/sbk-gem");
        final RecordingLeaseController leases = new RecordingLeaseController();
        final RecordingTransport transport = new RecordingTransport(true);
        final RuntimeCopyPolicy policy = fixedPolicy(bundle);
        final DeploymentOrchestrator orchestrator = new DeploymentOrchestrator(config, distribution,
                false, List.of(node), 25, policy, leases, scheduler, "run", transport);
        try {
            orchestrator.deploy(platform);
        } finally {
            session.stop();
        }

        assertTrue(leases.reserved);
        assertTrue(leases.acquired);
        assertTrue(transport.uploaded);
        assertTrue(transport.verified);
        assertEquals("run-0", node.leaseId());
        assertTrue(node.deploymentDirectory().endsWith(bundle.deploymentName()));
    }

    @Test
    void skipsTransportWhenRuntimeAlreadyExists() throws Exception {
        final DeploymentPlatform platform = DeploymentPlatform.local();
        final Path distribution = createDistribution();
        final SbkRuntimeBundle bundle = SbkRuntimeBundle.create(distribution, "bin/sbk", "10.6", 25,
                platform, temporaryDirectory.resolve("cache-existing"));
        final SshSession session = session();
        final RemoteNodeState node = new RemoteNodeState(0, session, List.of());
        node.connectionDirectory("/remote/sbk-gem");
        final RecordingTransport transport = new RecordingTransport(false);
        final DeploymentOrchestrator orchestrator = new DeploymentOrchestrator(config(), distribution,
                false, List.of(node), 25, fixedPolicy(bundle), new RecordingLeaseController(), scheduler,
                "run", transport);
        try {
            orchestrator.deploy(platform);
        } finally {
            session.stop();
        }

        assertFalse(transport.uploaded);
        assertFalse(transport.verified);
    }

    private RuntimeCopyPolicy fixedPolicy(SbkRuntimeBundle bundle) {
        return new RuntimeCopyPolicy() {
            @Override
            public String javaDeploymentName() {
                return "test Java";
            }

            @Override
            public ManagedJavaRuntime createJavaRuntime(JavaRuntimeSource source) {
                throw new AssertionError("Java provisioning is not part of deployment coordination");
            }

            @Override
            public SbkRuntimeBundle createSbkRuntime(SbkRuntimeSource source) {
                return bundle;
            }
        };
    }

    private SshSession session() {
        return new SshSession(new ConnectionConfig("node", "user", "", 22, "/", false, ""), sshExecutor);
    }

    private GemConfig config() {
        final GemConfig config = new GemConfig();
        config.runtimeCacheDirectory = temporaryDirectory.resolve("runtime-cache").toString();
        config.runtimeProgressIntervalSeconds = 60;
        config.deploymentTimeoutSeconds = 5;
        config.sbkVersion = "10.6";
        return config;
    }

    private Path createDistribution() throws IOException {
        final Path sbk = Files.createDirectories(temporaryDirectory.resolve("sbk-" + System.nanoTime()));
        final Path bin = Files.createDirectories(sbk.resolve("bin"));
        final Path lib = Files.createDirectories(sbk.resolve("lib"));
        Files.writeString(bin.resolve("sbk"), "launcher", StandardCharsets.UTF_8);
        assertTrue(bin.resolve("sbk").toFile().setExecutable(true));
        Files.writeString(lib.resolve("dependency.jar"), "dependency", StandardCharsets.UTF_8);
        Files.writeString(lib.resolve("sbk-10.6.jar"), "sbk", StandardCharsets.UTF_8);
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, "dependency.jar sbk-10.6.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(lib.resolve("sbk-pathing-10.6.jar")),
                manifest)) {
            output.finish();
        }
        Files.writeString(sbk.resolve(SbkRuntimeBundle.RUNTIME_IDENTITY_FILE),
                "format.version=1\nsbk.version=10.6\nbuild.sha256=" + "a".repeat(64) + "\n",
                StandardCharsets.UTF_8);
        return sbk;
    }

    private static final class RecordingLeaseController implements RuntimeLeaseController {
        private boolean reserved;
        private boolean acquired;

        @Override
        public void reserve() {
            reserved = true;
        }

        @Override
        public void acquire(SbkRuntimeBundle bundle) {
            acquired = true;
        }
    }

    private static final class RecordingTransport implements DeploymentTransport {
        private final boolean missing;
        private boolean uploaded;
        private boolean verified;

        private RecordingTransport(boolean missing) {
            this.missing = missing;
        }

        @Override
        public boolean[] missingTargets(SbkRuntimeBundle bundle, DeploymentPlatform platform) {
            return new boolean[]{missing};
        }

        @Override
        public void uploadAndActivate(SbkRuntimeBundle bundle, boolean[] copyTargets,
                                      DeploymentPlatform platform) {
            uploaded = true;
        }

        @Override
        public void verify(SbkRuntimeBundle bundle, boolean[] copyTargets, DeploymentPlatform platform) {
            verified = true;
        }
    }
}

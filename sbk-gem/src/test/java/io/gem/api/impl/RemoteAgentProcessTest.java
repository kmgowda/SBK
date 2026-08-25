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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executes the packaged remote agent for probe, activation, and verification. */
final class RemoteAgentProcessTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void packagedAgentActivatesAndVerifiesSbkWithoutPlatformCommands() throws Exception {
        final DeploymentPlatform platform = DeploymentPlatform.local();
        final SbkRuntimeBundle bundle = SbkRuntimeBundle.create(createSbkDistribution(), "bin/sbk", "10.6", 25,
                platform, temporaryDirectory.resolve("cache"));
        final Path uploaded = temporaryDirectory.resolve("uploaded.tar");
        Files.copy(bundle.archive(), uploaded, StandardCopyOption.REPLACE_EXISTING);
        final Path staging = temporaryDirectory.resolve("staging");
        final Path deployed = temporaryDirectory.resolve(bundle.deploymentName());
        Files.createDirectories(deployed);
        Files.writeString(deployed.resolve("invalid-runtime"), "corrupt", StandardCharsets.UTF_8);

        final ProcessResult activation = execute(RemoteAgent.activate(uploaded.toString(), bundle.archiveDigest(),
                bundle.contentDigest(), staging.toString(), deployed.toString(), platform.operatingSystem()));
        assertEquals(0, activation.exitCode(), activation.output());
        assertTrue(Files.isDirectory(deployed.resolve("sbk/lib")));
        assertTrue(Files.notExists(deployed.resolve("invalid-runtime")));
        assertTrue(Files.notExists(uploaded));

        final ProcessResult verification = execute(RemoteAgent.verify(deployed.toString(), bundle.contentDigest(),
                "10.6", platform.operatingSystem()));
        assertEquals(0, verification.exitCode(), verification.output());
        assertTrue(verification.output().contains("SBK_VERSION=10.6"));
    }

    @Test
    void packagedAgentProbesCurrentJdk() throws Exception {
        final int current = Runtime.version().feature();
        final ProcessResult result = execute(RemoteAgent.probe(current));
        final ProcessResult newerThanMinimum = execute(RemoteAgent.probe(current - 1));
        final ProcessResult olderThanMinimum = execute(RemoteAgent.probe(current + 1));

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("SBK_JAVA_MAJOR=" + current));
        assertEquals(0, newerThanMinimum.exitCode(), newerThanMinimum.output());
        assertTrue(olderThanMinimum.exitCode() != 0);
        assertTrue(olderThanMinimum.output().contains("required " + (current + 1) + " or newer"));
    }

    @Test
    void packagedAgentDeletesRetiredRuntimeTree() throws Exception {
        final Path parent = Files.createDirectories(temporaryDirectory.resolve("managed runtimes"));
        final Path retired = Files.createDirectories(parent.resolve(".sbk-runtime-retired.old/sbk/lib"));
        final Path current = Files.createDirectories(parent.resolve("sbk-runtime-current/sbk/lib"));
        Files.writeString(retired.resolve("retired.jar"), "retired", StandardCharsets.UTF_8);
        Files.writeString(current.resolve("current.jar"), "current", StandardCharsets.UTF_8);

        final ProcessResult result = execute(RemoteAgent.cleanup(parent.toString()));

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("SBK_RETIRED_RUNTIMES=1"));
        assertTrue(Files.notExists(retired.getParent().getParent()));
        assertTrue(Files.isRegularFile(current.resolve("current.jar")));
    }

    @Test
    void packagedAgentManagesRuntimeLeaseLocally() throws Exception {
        final Path parent = Files.createDirectories(temporaryDirectory.resolve("runtime parent"));
        final String deployment = "sbk-runtime-10.6-linux-test";
        final String digest = "0123456789abcdef";
        final String leaseId = "test-run";
        final Path runtime = Files.createDirectories(parent.resolve(deployment));
        Files.writeString(runtime.resolve("deployment.properties"), "content.sha256=" + digest + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(runtime.resolve(".sbk-runtime.sha256"), digest + "\n", StandardCharsets.UTF_8);
        final Path lease = parent.resolve(".sbk-runtime-leases").resolve(deployment).resolve(leaseId);

        final ProcessResult reservation = execute(RemoteAgent.reserveRuntime(parent.toString(), deployment,
                leaseId, 5, 60));
        assertEquals(0, reservation.exitCode(), reservation.output());
        assertTrue(reservation.output().contains("SBK_RUNTIME_LIFECYCLE=reserved"));
        assertTrue(Files.isRegularFile(lease));

        final ProcessResult acquisition = execute(RemoteAgent.acquireRuntime(parent.toString(), deployment,
                digest, leaseId, true, 5, 60, 300));
        assertEquals(0, acquisition.exitCode(), acquisition.output());
        assertTrue(acquisition.output().contains("SBK_RUNTIME_LIFECYCLE=acquired"));
        assertEquals(deployment, Files.readString(parent.resolve(".sbk-runtime-current")).trim());

        Files.writeString(lease, "active:1\n", StandardCharsets.UTF_8);
        final ProcessResult heartbeat = execute(RemoteAgent.heartbeatRuntime(parent.toString(), deployment,
                leaseId, 5, 60));
        assertEquals(0, heartbeat.exitCode(), heartbeat.output());
        assertTrue(heartbeat.output().contains("SBK_RUNTIME_LIFECYCLE=refreshed"));
        assertFalse(Files.readString(lease).trim().equals("active:1"));

        final ProcessResult release = execute(RemoteAgent.releaseRuntime(parent.toString(), deployment,
                leaseId, true, 5, 60, 300));
        assertEquals(0, release.exitCode(), release.output());
        assertTrue(release.output().contains("SBK_RUNTIME_LIFECYCLE=released"));
        assertTrue(Files.notExists(lease));
        assertTrue(Files.isDirectory(runtime));
    }

    private ProcessResult execute(byte[] request) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-jar", System.getProperty("sbk.gem.agentJar")).redirectErrorStream(true).start();
        process.getOutputStream().write(request);
        process.getOutputStream().close();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.waitFor(), output);
    }

    private Path createSbkDistribution() throws IOException {
        final Path sbk = Files.createDirectories(temporaryDirectory.resolve("sbk"));
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

    private record ProcessResult(int exitCode, String output) {
    }
}

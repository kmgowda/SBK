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
        final Path uploaded = temporaryDirectory.resolve("uploaded.tar.gz");
        Files.copy(bundle.archive(), uploaded, StandardCopyOption.REPLACE_EXISTING);
        final Path staging = temporaryDirectory.resolve("staging");
        final Path deployed = temporaryDirectory.resolve(bundle.deploymentName());

        final ProcessResult activation = execute(RemoteAgent.activate(uploaded.toString(), bundle.archiveDigest(),
                bundle.contentDigest(), staging.toString(), deployed.toString(), platform.operatingSystem(), true));
        assertEquals(0, activation.exitCode(), activation.output());
        assertTrue(Files.isDirectory(deployed.resolve("sbk/lib")));
        assertTrue(Files.notExists(uploaded));

        final ProcessResult verification = execute(RemoteAgent.verify(deployed.toString(), bundle.contentDigest(),
                "10.6", platform.operatingSystem()));
        assertEquals(0, verification.exitCode(), verification.output());
        assertTrue(verification.output().contains("SBK_VERSION=10.6"));
    }

    @Test
    void packagedAgentProbesCurrentJdk() throws Exception {
        final ProcessResult result = execute(RemoteAgent.probe(Runtime.version().feature()));

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("SBK_JAVA_MAJOR=" + Runtime.version().feature()));
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
        return sbk;
    }

    private record ProcessResult(int exitCode, String output) {
    }
}

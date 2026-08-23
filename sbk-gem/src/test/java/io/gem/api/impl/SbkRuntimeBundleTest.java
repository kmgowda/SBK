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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests creation and validation of immutable runtime archives. */
final class SbkRuntimeBundleTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void packagesCompleteSbkAndJavaWithContentIdentity() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path java = createJavaHome();
        final Path cache = temporaryDirectory.resolve("cache");

        final SbkRuntimeBundle first = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux", "amd64"), cache);
        final SbkRuntimeBundle cached = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux", "amd64"), cache);

        assertEquals(first.archive(), cached.archive());
        assertEquals(first.contentDigest(), cached.contentDigest());
        assertEquals(64, first.archiveDigest().length());
        final List<String> entries = archiveEntries(first.archive());
        assertTrue(entries.contains("runtime/sbk/bin/sbk"));
        assertTrue(entries.contains("runtime/sbk/lib/dependency.jar"));
        assertTrue(entries.contains("runtime/java/bin/java"));
        assertTrue(entries.contains("runtime/java/bin/javac"));
        assertTrue(entries.contains("runtime/java/LICENSE"));
        assertTrue(entries.contains("runtime/deployment.properties"));
        assertTrue(entries.contains("runtime/deployment-files.sha256"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/sbk/bin/sbk"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/java/bin/java"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/java/bin/javac"));
    }

    @Test
    void changedDependencyProducesDifferentDeploymentIdentity() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path java = createJavaHome();
        final Path cache = temporaryDirectory.resolve("cache");
        final SbkRuntimeBundle first = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux", "amd64"), cache);

        Files.writeString(sbk.resolve("lib/dependency.jar"), "changed", StandardCharsets.UTF_8);
        final SbkRuntimeBundle changed = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux", "amd64"), cache);

        assertNotEquals(first.contentDigest(), changed.contentDigest());
        assertNotEquals(first.deploymentName(), changed.deploymentName());
    }

    @Test
    void rejectsIncompletePathingClasspath() throws IOException {
        final Path sbk = createSbkDistribution();
        Files.delete(sbk.resolve("lib/dependency.jar"));

        final IOException exception = assertThrows(IOException.class, () -> SbkRuntimeBundle.create(sbk,
                "bin/sbk", createJavaHome(), "10.6", 25, new DeploymentPlatform("linux", "amd64"),
                temporaryDirectory.resolve("cache")));

        assertTrue(exception.getMessage().contains("pathing dependency is missing"));
    }

    private Path createSbkDistribution() throws IOException {
        final Path sbk = Files.createDirectories(temporaryDirectory.resolve("sbk"));
        final Path bin = Files.createDirectories(sbk.resolve("bin"));
        final Path lib = Files.createDirectories(sbk.resolve("lib"));
        executable(bin.resolve("sbk"));
        Files.writeString(lib.resolve("dependency.jar"), "dependency", StandardCharsets.UTF_8);
        Files.writeString(lib.resolve("sbk-10.6.jar"), "sbk", StandardCharsets.UTF_8);
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, "dependency.jar sbk-10.6.jar");
        try (JarOutputStream output = new JarOutputStream(
                Files.newOutputStream(lib.resolve("sbk-pathing-10.6.jar")), manifest)) {
            output.finish();
        }
        return sbk;
    }

    private Path createJavaHome() throws IOException {
        final Path java = Files.createDirectories(temporaryDirectory.resolve("java"));
        final Path bin = Files.createDirectories(java.resolve("bin"));
        executable(bin.resolve("java"));
        executable(bin.resolve("javac"));
        final Path legal = Files.createDirectories(java.resolve("legal/java.base"));
        Files.writeString(legal.resolve("LICENSE"), "license", StandardCharsets.UTF_8);
        Files.createSymbolicLink(java.resolve("LICENSE"), Path.of("legal/java.base/LICENSE"));
        return java;
    }

    private static void executable(Path path) throws IOException {
        Files.writeString(path, "#!/bin/sh\nexit 0\n", StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true));
    }

    private static List<String> archiveEntries(Path archive) throws IOException {
        final List<String> entries = new ArrayList<>();
        try (InputStream fileInput = Files.newInputStream(archive);
             GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(fileInput);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    private static int archiveMode(Path archive, String name) throws IOException {
        try (InputStream fileInput = Files.newInputStream(archive);
             GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(fileInput);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return entry.getMode();
                }
            }
        }
        throw new IOException("Archive entry not found: " + name);
    }
}

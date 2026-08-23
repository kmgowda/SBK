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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                new DeploymentPlatform("linux"), cache);
        final SbkRuntimeBundle cached = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux"), cache);

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
        final String descriptor = archiveText(first.archive(), "runtime/deployment.properties");
        assertTrue(descriptor.contains("platform.os=linux"));
        assertFalse(descriptor.contains("platform.arch"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/sbk/bin/sbk"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/sbk/lib/"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/java/bin/java"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/java/bin/javac"));
    }

    @Test
    void changedDependencyProducesDifferentDeploymentIdentity() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path java = createJavaHome();
        final Path cache = temporaryDirectory.resolve("cache");
        final SbkRuntimeBundle first = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux"), cache);

        Files.writeString(sbk.resolve("lib/dependency.jar"), "changed", StandardCharsets.UTF_8);
        final SbkRuntimeBundle changed = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux"), cache);

        assertNotEquals(first.contentDigest(), changed.contentDigest());
        assertNotEquals(first.deploymentName(), changed.deploymentName());
    }

    @Test
    void cleanupRetainsCurrentAndRemovesLowerAndHigherCachedVersions() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path java = createJavaHome();
        final Path cache = temporaryDirectory.resolve("cache");
        final DeploymentPlatform platform = new DeploymentPlatform("linux");
        final SbkRuntimeBundle lower = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.5", 25,
                platform, cache);
        final SbkRuntimeBundle current = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                platform, cache);
        final SbkRuntimeBundle higher = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.7", 25,
                platform, cache);

        assertEquals(2, SbkRuntimeBundle.cleanupOtherCachedBundles(cache, current.deploymentName()));

        assertFalse(Files.exists(lower.archive()));
        assertTrue(Files.exists(current.archive()));
        assertFalse(Files.exists(higher.archive()));
    }

    @Test
    void cleanupRetainsCachedBundleWhileAnotherDeploymentUsesItsArchive() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path java = createJavaHome();
        final Path cache = temporaryDirectory.resolve("cache");
        final DeploymentPlatform platform = new DeploymentPlatform("linux");
        final SbkRuntimeBundle active = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.5", 25,
                platform, cache);
        final SbkRuntimeBundle current = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                platform, cache);

        try (SbkRuntimeBundle.ArchiveUse ignored = active.acquireArchiveUse()) {
            assertEquals(0, SbkRuntimeBundle.cleanupOtherCachedBundles(cache, current.deploymentName()));
            assertTrue(Files.exists(active.archive()));
        }

        assertEquals(1, SbkRuntimeBundle.cleanupOtherCachedBundles(cache, current.deploymentName()));
        assertFalse(Files.exists(active.archive()));
        assertTrue(Files.exists(current.archive()));
    }

    @Test
    void rejectsIncompletePathingClasspath() throws IOException {
        final Path sbk = createSbkDistribution();
        Files.delete(sbk.resolve("lib/dependency.jar"));

        final IOException exception = assertThrows(IOException.class, () -> SbkRuntimeBundle.create(sbk,
                "bin/sbk", createJavaHome(), "10.6", 25, new DeploymentPlatform("linux"),
                temporaryDirectory.resolve("cache")));

        assertTrue(exception.getMessage().contains("pathing dependency is missing"));
    }

    @Test
    void rejectsSymbolicLinksOutsideSourceTree() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path external = temporaryDirectory.resolve("external.txt");
        Files.writeString(external, "external", StandardCharsets.UTF_8);
        Files.createSymbolicLink(sbk.resolve("external-link"), Path.of("..", "external.txt"));

        final IOException exception = assertThrows(IOException.class, () -> SbkRuntimeBundle.create(sbk,
                "bin/sbk", createJavaHome(), "10.6", 25, new DeploymentPlatform("linux"),
                temporaryDirectory.resolve("cache")));

        assertTrue(exception.getMessage().contains("symbolic link escapes its source tree"));
    }

    @Test
    void preservesContainedDirectoryLinksAndTheirTargets() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path shared = Files.createDirectories(sbk.resolve("shared"));
        Files.writeString(shared.resolve("data.txt"), "data", StandardCharsets.UTF_8);
        Files.createSymbolicLink(sbk.resolve("shared-link"), Path.of("shared"));

        final SbkRuntimeBundle bundle = SbkRuntimeBundle.create(sbk, "bin/sbk", createJavaHome(), "10.6", 25,
                new DeploymentPlatform("linux"), temporaryDirectory.resolve("cache"));

        assertTrue(archiveEntries(bundle.archive()).contains("runtime/sbk/shared/data.txt"));
        assertEquals("shared", archiveLinkTarget(bundle.archive(), "runtime/sbk/shared-link"));
    }

    @Test
    void recreatesCorruptedCachedArchive() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path java = createJavaHome();
        final Path cache = temporaryDirectory.resolve("cache");
        final SbkRuntimeBundle first = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux"), cache);
        Files.writeString(first.archive(), "corrupted", StandardCharsets.UTF_8);

        final SbkRuntimeBundle repaired = SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                new DeploymentPlatform("linux"), cache);

        assertEquals(64, repaired.archiveDigest().length());
        assertEquals(repaired.archiveDigest(), Files.readString(
                repaired.archive().resolveSibling(repaired.archive().getFileName() + ".sha256"),
                StandardCharsets.UTF_8).trim());
        assertTrue(archiveEntries(repaired.archive()).contains("runtime/sbk/bin/sbk"));
    }

    @Test
    void serializesConcurrentCacheCreation() throws IOException, ExecutionException, InterruptedException {
        final Path sbk = createSbkDistribution();
        final Path java = createJavaHome();
        final Path cache = temporaryDirectory.resolve("cache");
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                    new DeploymentPlatform("linux"), cache));
            final var second = executor.submit(() -> SbkRuntimeBundle.create(sbk, "bin/sbk", java, "10.6", 25,
                    new DeploymentPlatform("linux"), cache));

            assertEquals(first.get().archiveDigest(), second.get().archiveDigest());
            assertEquals(first.get().archive(), second.get().archive());
        }
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

    private static String archiveLinkTarget(Path archive, String name) throws IOException {
        try (InputStream fileInput = Files.newInputStream(archive);
             GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(fileInput);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return entry.getLinkName();
                }
            }
        }
        throw new IOException("Archive entry not found: " + name);
    }

    private static String archiveText(Path archive, String name) throws IOException {
        try (InputStream fileInput = Files.newInputStream(archive);
             GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(fileInput);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return new String(tarInput.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IOException("Archive entry not found: " + name);
    }
}

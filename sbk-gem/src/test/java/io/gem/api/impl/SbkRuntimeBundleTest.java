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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
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
    void packagesOnlyCompleteSbkWithContentIdentity() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path cache = temporaryDirectory.resolve("cache");

        final SbkRuntimeBundle first = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), cache);
        final SbkRuntimeBundle cached = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), cache);

        assertEquals(first.archive(), cached.archive());
        assertEquals(first.contentDigest(), cached.contentDigest());
        assertFalse(first.archiveReused());
        assertTrue(cached.archiveReused());
        assertEquals(64, first.archiveDigest().length());
        final List<String> entries = archiveEntries(first.archive());
        assertTrue(entries.contains("runtime/sbk/lib/dependency.jar"));
        assertFalse(entries.stream().anyMatch(entry -> entry.startsWith("runtime/java/")));
        assertTrue(entries.contains("runtime/deployment.properties"));
        assertTrue(entries.contains("runtime/deployment-files.sha256"));
        final String descriptor = archiveText(first.archive(), "runtime/deployment.properties");
        assertTrue(descriptor.contains("platform.os=linux"));
        assertFalse(descriptor.contains("platform.arch"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/sbk/bin/sbk"));
        assertEquals(0755, archiveMode(first.archive(), "runtime/sbk/lib/"));
        assertTrue(descriptor.contains("includes.java=false"));
    }

    @Test
    void packagesOnlyTheSelectedGradleDriverRuntimeWhenRequested() throws IOException {
        final Path sbk = createSbkDistribution();
        Files.writeString(sbk.resolve("lib/unrelated-driver.jar"), "unrelated", StandardCharsets.UTF_8);
        createDriverRuntimeManifest(sbk, "file", List.of("dependency.jar", "sbk-10.6.jar"));
        final DriverRuntimeManifest driverRuntime = DriverRuntimeManifest.load(sbk, "File", "10.6");

        final SbkRuntimeBundle bundle = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), temporaryDirectory.resolve("cache"), driverRuntime);

        final List<String> entries = archiveEntries(bundle.archive());
        assertTrue(bundle.deploymentName().contains("-file-"));
        assertFalse(entries.contains("runtime/sbk/bin/sbk"));
        assertTrue(entries.contains("runtime/sbk/lib/dependency.jar"));
        assertTrue(entries.contains("runtime/sbk/lib/sbk-10.6.jar"));
        assertTrue(entries.contains("runtime/sbk/lib/sbk-pathing-10.6.jar"));
        assertFalse(entries.contains("runtime/sbk/lib/unrelated-driver.jar"));
    }

    @Test
    void changedDependencyProducesDifferentDeploymentIdentity() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path cache = temporaryDirectory.resolve("cache");
        final SbkRuntimeBundle first = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), cache);

        Files.writeString(sbk.resolve("lib/dependency.jar"), "changed", StandardCharsets.UTF_8);
        writeRuntimeIdentity(sbk, "b".repeat(64));
        final SbkRuntimeBundle changed = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), cache);

        assertNotEquals(first.contentDigest(), changed.contentDigest());
        assertNotEquals(first.deploymentName(), changed.deploymentName());
    }

    @Test
    void localhostManagedRuntimeStateDoesNotContaminateBundleIdentity() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path cache = temporaryDirectory.resolve("cache");
        final DeploymentPlatform platform = new DeploymentPlatform("macos");
        final SbkRuntimeBundle clean = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                platform, cache);

        final Path nestedRuntime = Files.createDirectories(sbk.resolve(
                "sbk-runtime-10.5-macos-inactive/sbk/lib"));
        Files.writeString(nestedRuntime.resolve("nested-runtime.jar"), "must not be bundled",
                StandardCharsets.UTF_8);
        Files.writeString(sbk.resolve(".sbk-runtime-current"), clean.deploymentName(), StandardCharsets.UTF_8);
        Files.createDirectories(sbk.resolve(".sbk-runtime-leases").resolve(clean.deploymentName()));
        Files.createDirectories(sbk.resolve(".sbk-runtime-management.lock"));
        Files.writeString(sbk.resolve("sbk-runtime-transfer.tar"), "partial", StandardCharsets.UTF_8);

        final SbkRuntimeBundle withManagedState = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                platform, cache);

        assertEquals(clean.contentDigest(), withManagedState.contentDigest());
        assertEquals(clean.archive(), withManagedState.archive());
        assertFalse(archiveEntries(withManagedState.archive()).stream()
                .anyMatch(name -> name.contains("sbk-runtime-10.5") || name.contains(".sbk-runtime-")));
    }

    @Test
    void cleanupRetainsCurrentAndRemovesLowerAndHigherCachedVersions() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path cache = temporaryDirectory.resolve("cache");
        final DeploymentPlatform platform = new DeploymentPlatform("linux");
        writeRuntimeIdentity(sbk, "10.5", "5".repeat(64));
        final SbkRuntimeBundle lower = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.5", 25,
                platform, cache);
        writeRuntimeIdentity(sbk, "10.6", "6".repeat(64));
        final SbkRuntimeBundle current = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                platform, cache);
        writeRuntimeIdentity(sbk, "10.7", "7".repeat(64));
        final SbkRuntimeBundle higher = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.7", 25,
                platform, cache);
        final Path legacyArchive = cache.resolve("sbk-runtime-10.4-linux-legacy.tar.gz");
        Files.writeString(legacyArchive, "legacy", StandardCharsets.UTF_8);
        Files.writeString(cache.resolve(legacyArchive.getFileName() + ".sha256"), "legacy", StandardCharsets.UTF_8);

        assertEquals(3, SbkRuntimeBundle.cleanupOtherCachedBundles(cache, current.deploymentName()));

        assertFalse(Files.exists(lower.archive()));
        assertTrue(Files.exists(current.archive()));
        assertFalse(Files.exists(higher.archive()));
        assertFalse(Files.exists(legacyArchive));
        assertFalse(Files.exists(cache.resolve(legacyArchive.getFileName() + ".sha256")));
    }

    @Test
    void cleanupRetainsCachedBundleWhileAnotherDeploymentUsesItsArchive() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path cache = temporaryDirectory.resolve("cache");
        final DeploymentPlatform platform = new DeploymentPlatform("linux");
        writeRuntimeIdentity(sbk, "10.5", "5".repeat(64));
        final SbkRuntimeBundle active = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.5", 25,
                platform, cache);
        writeRuntimeIdentity(sbk, "10.6", "6".repeat(64));
        final SbkRuntimeBundle current = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
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
                "bin/sbk", "10.6", 25, new DeploymentPlatform("linux"),
                temporaryDirectory.resolve("cache")));

        assertTrue(exception.getMessage().contains("pathing dependency is missing"));
    }

    @Test
    void rejectsSymbolicLinksOutsideSourceTree() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path external = temporaryDirectory.resolve("external.txt");
        Files.writeString(external, "external", StandardCharsets.UTF_8);
        Files.createSymbolicLink(sbk.resolve("lib/external-link"), Path.of("..", "..", "external.txt"));

        final IOException exception = assertThrows(IOException.class, () -> SbkRuntimeBundle.create(sbk,
                "bin/sbk", "10.6", 25, new DeploymentPlatform("linux"),
                temporaryDirectory.resolve("cache")));

        assertTrue(exception.getMessage().contains("symbolic link escapes its source tree"));
    }

    @Test
    void preservesContainedDirectoryLinksAndTheirTargets() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path shared = Files.createDirectories(sbk.resolve("lib/shared"));
        Files.writeString(shared.resolve("data.txt"), "data", StandardCharsets.UTF_8);
        Files.createSymbolicLink(sbk.resolve("lib/shared-link"), Path.of("shared"));

        final SbkRuntimeBundle bundle = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), temporaryDirectory.resolve("cache"));

        assertTrue(archiveEntries(bundle.archive()).contains("runtime/sbk/lib/shared/data.txt"));
        assertEquals("shared", archiveLinkTarget(bundle.archive(), "runtime/sbk/lib/shared-link"));
    }

    @Test
    void recreatesCorruptedCachedArchive() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path cache = temporaryDirectory.resolve("cache");
        final SbkRuntimeBundle first = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), cache);
        Files.writeString(first.archive(), "corrupted", StandardCharsets.UTF_8);

        final SbkRuntimeBundle repaired = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), cache);

        assertEquals(64, repaired.archiveDigest().length());
        assertEquals(repaired.archiveDigest(), Files.readString(
                repaired.archive().resolveSibling(repaired.archive().getFileName() + ".sha256"),
                StandardCharsets.UTF_8).trim());
        assertTrue(archiveEntries(repaired.archive()).contains("runtime/sbk/bin/sbk"));
    }

    @Test
    void rebuildsSameSizeCorruptionAfterRemoteIntegrityFailure() throws IOException {
        final Path sbk = createSbkDistribution();
        final Path cache = temporaryDirectory.resolve("cache");
        final SbkRuntimeBundle first = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), cache);
        final byte[] corrupted = Files.readAllBytes(first.archive());
        corrupted[corrupted.length / 2] ^= 1;
        Files.write(first.archive(), corrupted);

        final SbkRuntimeBundle cached = SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                new DeploymentPlatform("linux"), cache);
        assertEquals(first.archiveDigest(), cached.archiveDigest());
        assertNotEquals(cached.archiveDigest(), sha256(cached.archive()));

        cached.rebuildArchive();

        assertEquals(cached.archiveDigest(), sha256(cached.archive()));
        assertTrue(archiveEntries(cached.archive()).contains("runtime/sbk/bin/sbk"));
    }

    @Test
    void serializesConcurrentCacheCreation() throws IOException, ExecutionException, InterruptedException {
        final Path sbk = createSbkDistribution();
        final Path cache = temporaryDirectory.resolve("cache");
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
                    new DeploymentPlatform("linux"), cache));
            final var second = executor.submit(() -> SbkRuntimeBundle.create(sbk, "bin/sbk", "10.6", 25,
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
        writeRuntimeIdentity(sbk, "10.6", "a".repeat(64));
        return sbk;
    }

    private static void createDriverRuntimeManifest(Path sbk, String driver, List<String> libraries)
            throws IOException {
        final Path directory = Files.createDirectories(sbk.resolve(DriverRuntimeManifest.DIRECTORY));
        final String pathingName = driver + "-sbk-pathing-10.6.jar";
        final Manifest pathingManifest = new Manifest();
        pathingManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        pathingManifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, String.join(" ", libraries));
        try (JarOutputStream output = new JarOutputStream(
                Files.newOutputStream(directory.resolve(pathingName)), pathingManifest)) {
            output.finish();
        }
        Files.writeString(directory.resolve(driver + ".properties"),
                "format.version=1\n"
                        + "driver.name=" + driver + "\n"
                        + "sbk.version=10.6\n"
                        + "runtime.pathing=" + pathingName + "\n"
                        + "runtime.files=" + String.join(",", libraries) + "\n"
                        + "runtime.sha256=" + "c".repeat(64) + "\n",
                StandardCharsets.UTF_8);
    }

    private static void writeRuntimeIdentity(Path sbk, String identity) throws IOException {
        writeRuntimeIdentity(sbk, "10.6", identity);
    }

    private static void writeRuntimeIdentity(Path sbk, String version, String identity) throws IOException {
        Files.writeString(sbk.resolve(SbkRuntimeBundle.RUNTIME_IDENTITY_FILE),
                "format.version=1\nsbk.version=" + version + "\nbuild.sha256=" + identity + "\n",
                StandardCharsets.UTF_8);
    }

    private static void executable(Path path) throws IOException {
        Files.writeString(path, "#!/bin/sh\nexit 0\n", StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true));
    }

    private static List<String> archiveEntries(Path archive) throws IOException {
        final List<String> entries = new ArrayList<>();
        try (InputStream fileInput = Files.newInputStream(archive);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(fileInput)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    private static int archiveMode(Path archive, String name) throws IOException {
        try (InputStream fileInput = Files.newInputStream(archive);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(fileInput)) {
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
             TarArchiveInputStream tarInput = new TarArchiveInputStream(fileInput)) {
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
             TarArchiveInputStream tarInput = new TarArchiveInputStream(fileInput)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return new String(tarInput.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IOException("Archive entry not found: " + name);
    }

    private static String sha256(Path path) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

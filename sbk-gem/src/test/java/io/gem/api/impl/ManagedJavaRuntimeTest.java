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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests independent, content-addressed JDK provisioning. */
final class ManagedJavaRuntimeTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void installsAndReusesJdkSeparatelyFromSbk() throws IOException {
        final Path javaHome = createJavaHome();
        final Path remoteParent = Files.createDirectories(temporaryDirectory.resolve("remote"));
        final ManagedJavaRuntime runtime = ManagedJavaRuntime.create(javaHome, 25);

        final String first = runtime.install(remoteParent.getFileSystem(), remoteParent.toString());
        final String reused = runtime.install(remoteParent.getFileSystem(), remoteParent.toString());

        assertEquals(first, reused);
        assertTrue(Files.isExecutable(Path.of(first).resolve("bin/java")));
        assertTrue(Files.isRegularFile(Path.of(first).resolve(".sbk-java.sha256")));
        assertTrue(Path.of(first).getFileName().toString().startsWith("sbk-java-25-"));
    }

    @Test
    void reportsBufferedJdkCopyProgressWithoutRecountingReuse() throws IOException {
        final Path javaHome = createJavaHome();
        Files.write(javaHome.resolve("large-runtime-image"), new byte[1024 * 1024 + 7]);
        final Path remoteParent = Files.createDirectories(temporaryDirectory.resolve("remote"));
        final ManagedJavaRuntime runtime = ManagedJavaRuntime.create(javaHome, 25);
        final AtomicLong copiedBytes = new AtomicLong();
        final AtomicInteger updates = new AtomicInteger();

        runtime.install(remoteParent.getFileSystem(), remoteParent.toString(), copied -> {
            copiedBytes.addAndGet(copied);
            updates.incrementAndGet();
        });

        assertEquals(runtime.contentBytes(), copiedBytes.get());
        assertTrue(updates.get() < 20);
        copiedBytes.set(0);
        runtime.install(remoteParent.getFileSystem(), remoteParent.toString(), copiedBytes::addAndGet);
        assertEquals(0, copiedBytes.get());
    }

    @Test
    void changedJdkBytesProduceAnotherIdentity() throws IOException {
        final Path javaHome = createJavaHome();
        final ManagedJavaRuntime first = ManagedJavaRuntime.create(javaHome, 25);
        Files.writeString(javaHome.resolve("release"), "changed", StandardCharsets.UTF_8);
        final ManagedJavaRuntime changed = ManagedJavaRuntime.create(javaHome, 25);

        assertNotEquals(first.directoryName(), changed.directoryName());
    }

    @Test
    void changedJdkPermissionsProduceAnotherIdentity() throws IOException {
        final Path javaHome = createJavaHome();
        final Path release = javaHome.resolve("release");
        final ManagedJavaRuntime first = ManagedJavaRuntime.create(javaHome, 25);
        final boolean executable = release.toFile().setExecutable(true);
        final ManagedJavaRuntime changed = ManagedJavaRuntime.create(javaHome, 25);

        assertTrue(executable);
        assertNotEquals(first.directoryName(), changed.directoryName());
    }

    @Test
    void cachesFullJdkDigestUntilFilesystemMetadataChanges() throws IOException {
        final Path javaHome = createJavaHome();
        final Path cache = temporaryDirectory.resolve("cache");
        final ManagedJavaRuntime first = ManagedJavaRuntime.create(javaHome, 25, cache);
        final ManagedJavaRuntime cached = ManagedJavaRuntime.create(javaHome, 25, cache);

        assertEquals(first.directoryName(), cached.directoryName());
        try (var files = Files.list(cache)) {
            assertEquals(1, files.filter(path -> java.util.Objects.requireNonNull(path.getFileName()).toString()
                    .endsWith(".properties")).count());
        }

        Files.writeString(javaHome.resolve("release"), "JAVA_VERSION=25_CHANGED", StandardCharsets.UTF_8);
        final ManagedJavaRuntime changed = ManagedJavaRuntime.create(javaHome, 25, cache);

        assertNotEquals(first.directoryName(), changed.directoryName());
    }

    @Test
    void repairsCachedJdkWithInvalidExecutablePermissions() throws IOException {
        final Path javaHome = createJavaHome();
        final Path remoteParent = Files.createDirectories(temporaryDirectory.resolve("remote"));
        final ManagedJavaRuntime runtime = ManagedJavaRuntime.create(javaHome, 25);
        final Path installed = Path.of(runtime.install(remoteParent.getFileSystem(), remoteParent.toString()));
        assertTrue(installed.resolve("bin/java").toFile().setExecutable(false));

        final Path repaired = Path.of(runtime.install(remoteParent.getFileSystem(), remoteParent.toString()));

        assertEquals(installed, repaired);
        assertTrue(Files.isExecutable(repaired.resolve("bin/java")));
    }

    @Test
    void copiesRelativeJdkSymbolicLinks() throws IOException {
        final Path javaHome = createJavaHome();
        final Path linkParent = Files.createDirectories(javaHome.resolve("legal/java.compiler"));
        final Path linkTarget = Path.of("..", "java.base", "LICENSE");
        Files.createDirectories(javaHome.resolve("legal/java.base"));
        Files.writeString(javaHome.resolve("legal/java.base/LICENSE"), "license", StandardCharsets.UTF_8);
        Files.createSymbolicLink(linkParent.resolve("LICENSE"), linkTarget);
        final Path remoteParent = Files.createDirectories(temporaryDirectory.resolve("remote"));

        final String installed = ManagedJavaRuntime.create(javaHome, 25)
                .install(remoteParent.getFileSystem(), remoteParent.toString());

        assertEquals(linkTarget, Files.readSymbolicLink(Path.of(installed).resolve("legal/java.compiler/LICENSE")));
    }

    @Test
    void convertsLinkTargetsToDestinationFileSystemProvider() throws IOException {
        final URI archive = URI.create("jar:" + temporaryDirectory.resolve("remote.zip").toUri());
        try (FileSystem remoteFileSystem = FileSystems.newFileSystem(archive, Map.of("create", "true"))) {
            final Path destination = remoteFileSystem.getPath("/jdk/legal/java.compiler/LICENSE");
            final Path converted = ManagedJavaRuntime.remoteLinkTarget(
                    destination, Path.of("..", "java.base", "LICENSE"));

            assertSame(remoteFileSystem, converted.getFileSystem());
            assertEquals("../java.base/LICENSE", converted.toString());
        }
    }

    @Test
    void concurrentInstallersReuseTheSameCompleteJdk()
            throws IOException, ExecutionException, InterruptedException {
        final Path javaHome = createJavaHome();
        final Path remoteParent = Files.createDirectories(temporaryDirectory.resolve("remote"));
        final ManagedJavaRuntime runtime = ManagedJavaRuntime.create(javaHome, 25);

        try (var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(
                    () -> runtime.install(remoteParent.getFileSystem(), remoteParent.toString()));
            final var second = executor.submit(
                    () -> runtime.install(remoteParent.getFileSystem(), remoteParent.toString()));

            assertEquals(first.get(), second.get());
            assertTrue(Files.isExecutable(Path.of(first.get()).resolve("bin/java")));
        }
    }

    private Path createJavaHome() throws IOException {
        final Path home = Files.createDirectories(temporaryDirectory.resolve("jdk"));
        final Path bin = Files.createDirectories(home.resolve("bin"));
        executable(bin.resolve("java"));
        executable(bin.resolve("javac"));
        Files.writeString(home.resolve("release"), "JAVA_VERSION=25", StandardCharsets.UTF_8);
        return home;
    }

    private static void executable(Path path) throws IOException {
        Files.writeString(path, "binary", StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true));
    }
}

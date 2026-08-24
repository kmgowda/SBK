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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void changedJdkBytesProduceAnotherIdentity() throws IOException {
        final Path javaHome = createJavaHome();
        final ManagedJavaRuntime first = ManagedJavaRuntime.create(javaHome, 25);
        Files.writeString(javaHome.resolve("release"), "changed", StandardCharsets.UTF_8);
        final ManagedJavaRuntime changed = ManagedJavaRuntime.create(javaHome, 25);

        assertNotEquals(first.directoryName(), changed.directoryName());
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

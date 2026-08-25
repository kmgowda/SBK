/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests remote-local runtime leases, cleanup, and concurrency. */
final class RemoteRuntimeFilesTest {
    private static final long TEST_TIMEOUT_SECONDS = 5;
    private static final long TEST_STALE_SECONDS = 60;
    private static final long TEST_RESERVATION_SECONDS = 60;
    private static final String DIGEST = "0123456789abcdef";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void resolvesAndCreatesDeploymentDirectoryWithoutRemoteShell() throws Exception {
        final Path configured = temporaryDirectory.resolve("deployment parent");

        final String resolved = RemoteRuntimeFiles.resolveDirectory(
                configured.getFileSystem(), configured.toString());

        assertEquals(configured.toRealPath().toString(), resolved);
        assertTrue(Files.isDirectory(configured));
    }

    @Test
    void acquisitionRetainsCurrentRuntimeAndRemovesAllInactiveNonCurrentVersions() throws Exception {
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        final String old = "sbk-runtime-10.5-linux-amd64-old";
        final String newer = "sbk-runtime-10.7-linux-amd64-newer";
        createRuntime(current, DIGEST);
        createRuntime(old, "old-digest");
        createRuntime(newer, "newer-digest");

        RemoteRuntimeFiles.acquire(temporaryDirectory, current, DIGEST, "run-1", true,
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);

        assertTrue(Files.isDirectory(temporaryDirectory.resolve(current)));
        assertFalse(Files.exists(temporaryDirectory.resolve(old)));
        assertFalse(Files.exists(temporaryDirectory.resolve(newer)));
        assertEquals(current, Files.readString(temporaryDirectory.resolve(".sbk-runtime-current")).trim());
        assertTrue(Files.readString(Path.of(RemoteRuntimeFiles.leasePath(
                temporaryDirectory.toString(), current, "run-1"))).startsWith("active:"));
    }

    @Test
    void concurrentAcquisitionsPreserveEveryActiveRuntime() throws Exception {
        final String first = "sbk-runtime-10.6-macos-first";
        final String second = "sbk-runtime-10.6-macos-second";
        createRuntime(first, DIGEST);
        createRuntime(second, "second-digest");
        RemoteRuntimeFiles.reserve(temporaryDirectory, first, "first-run",
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS);
        RemoteRuntimeFiles.reserve(temporaryDirectory, second, "second-run",
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS);

        final var firstAcquire = java.util.concurrent.CompletableFuture.runAsync(() ->
                acquireUnchecked(first, DIGEST, "first-run"));
        final var secondAcquire = java.util.concurrent.CompletableFuture.runAsync(() ->
                acquireUnchecked(second, "second-digest", "second-run"));
        java.util.concurrent.CompletableFuture.allOf(firstAcquire, secondAcquire).get(
                TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(Files.isDirectory(temporaryDirectory.resolve(first)));
        assertTrue(Files.isDirectory(temporaryDirectory.resolve(second)));
        assertTrue(Files.isRegularFile(Path.of(RemoteRuntimeFiles.leasePath(
                temporaryDirectory.toString(), first, "first-run"))));
        assertTrue(Files.isRegularFile(Path.of(RemoteRuntimeFiles.leasePath(
                temporaryDirectory.toString(), second, "second-run"))));
    }

    @Test
    void multipleControllersShareOneRuntimeWithoutLeavingTheLifecycleLock() throws Exception {
        final String deployment = "sbk-runtime-10.6-linux-shared";
        createRuntime(deployment, DIGEST);
        final var operations = new java.util.ArrayList<java.util.concurrent.CompletableFuture<Void>>();
        for (int i = 0; i < 16; i++) {
            final String leaseId = "shared-" + i;
            operations.add(java.util.concurrent.CompletableFuture.runAsync(() ->
                    acquireUnchecked(deployment, DIGEST, leaseId)));
        }
        java.util.concurrent.CompletableFuture.allOf(operations.toArray(java.util.concurrent.CompletableFuture[]::new))
                .get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        final Path leases = temporaryDirectory.resolve(RemoteRuntimeFiles.LEASE_DIRECTORY).resolve(deployment);
        try (var entries = Files.list(leases)) {
            assertEquals(16, entries.count());
        }
        assertFalse(Files.exists(temporaryDirectory.resolve(RemoteRuntimeFiles.LOCK_DIRECTORY)));
    }

    @Test
    void recursiveDeletionIsSeparatedFromLeaseAcquisition() throws Exception {
        final Path parent = temporaryDirectory.resolve("detached-cleanup");
        final String current = "sbk-runtime-10.6-macos-current";
        final String inactive = "sbk-runtime-10.5-macos-inactive";
        createRuntime(parent, current, DIGEST);
        createRuntime(parent, inactive, "inactive-digest");
        final long startedNanos = System.nanoTime();
        RemoteRuntimeFiles.acquire(parent, current, DIGEST, "detached-acquire", true,
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);

        assertTrue(elapsedMillis < TimeUnit.SECONDS.toMillis(2),
                "lease acquisition waited " + elapsedMillis + " ms for recursive deletion");
        assertTrue(Files.isDirectory(parent.resolve(current)));
        assertFalse(Files.exists(parent.resolve(inactive)));
        assertEquals(1, RemoteRuntimeFiles.deleteRetired(parent));
        assertNoRetiredRuntime(parent);
    }

    @Test
    void acquisitionPreservesANonCurrentRuntimeWithALiveProcessLease() throws Exception {
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        final String active = "sbk-runtime-10.7-linux-amd64-active";
        createRuntime(current, DIGEST);
        createRuntime(active, "active-digest");
        RemoteRuntimeFiles.acquire(temporaryDirectory, active, "active-digest", "active-run", false,
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);
        final Path activeLease = Path.of(RemoteRuntimeFiles.leasePath(
                temporaryDirectory.toString(), active, "active-run"));
        RemoteRuntimeFiles.acquire(temporaryDirectory, current, DIGEST, "run-2", true,
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);

        assertTrue(Files.isDirectory(temporaryDirectory.resolve(active)));
        assertTrue(Files.isRegularFile(activeLease));
    }

    @Test
    void heartbeatRefreshesAnExistingLease() throws Exception {
        final String current = "sbk-runtime-10.6-linux-current";
        createRuntime(current, DIGEST);
        RemoteRuntimeFiles.acquire(temporaryDirectory, current, DIGEST, "heartbeat", false,
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);
        final Path lease = Path.of(RemoteRuntimeFiles.leasePath(
                temporaryDirectory.toString(), current, "heartbeat"));
        Files.writeString(lease, "active:1\n", StandardCharsets.UTF_8);

        RemoteRuntimeFiles.heartbeat(temporaryDirectory, current, "heartbeat",
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS);

        assertFalse(Files.readString(lease).trim().equals("active:1"));
    }

    @Test
    void releasingOlderLeaseRemovesFormerRuntimeButRetainsAuthoritativeCurrentRuntime() throws Exception {
        final String old = "sbk-runtime-10.5-linux-amd64-old";
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        createRuntime(old, "old-digest");
        RemoteRuntimeFiles.acquire(temporaryDirectory, old, "old-digest", "old-run", true,
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);
        createRuntime(current, DIGEST);
        RemoteRuntimeFiles.acquire(temporaryDirectory, current, DIGEST, "current-run", true,
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);
        assertTrue(Files.isDirectory(temporaryDirectory.resolve(old)));

        RemoteRuntimeFiles.release(temporaryDirectory, old, "old-run", true,
                TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);

        assertFalse(Files.exists(temporaryDirectory.resolve(old)));
        assertTrue(Files.isDirectory(temporaryDirectory.resolve(current)));
    }

    private void createRuntime(String name, String digest) throws IOException {
        createRuntime(temporaryDirectory, name, digest);
    }

    private static void createRuntime(Path parent, String name, String digest) throws IOException {
        final Path runtime = parent.resolve(name);
        Files.createDirectories(runtime);
        Files.writeString(runtime.resolve("deployment.properties"),
                "content.sha256=" + digest + "\n", StandardCharsets.UTF_8);
        Files.writeString(runtime.resolve(".sbk-runtime.sha256"),
                digest + "\n", StandardCharsets.UTF_8);
    }

    private void acquireUnchecked(String deploymentName, String digest, String leaseId) {
        try {
            RemoteRuntimeFiles.acquire(temporaryDirectory, deploymentName, digest, leaseId, true,
                    TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);
        } catch (IOException | InterruptedException exception) {
            throw new java.util.concurrent.CompletionException(exception);
        }
    }

    private static void assertNoRetiredRuntime(Path parent) throws IOException {
        try (var paths = Files.list(parent)) {
            assertTrue(paths.noneMatch(path -> String.valueOf(path.getFileName())
                    .startsWith(RemoteRuntimeFiles.RETIRED_PREFIX)));
        }
    }
}

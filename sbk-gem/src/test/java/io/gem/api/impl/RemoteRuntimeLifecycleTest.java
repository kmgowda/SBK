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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests managed remote runtime leases and inactive-version cleanup using the local POSIX shell. */
final class RemoteRuntimeLifecycleTest {
    private static final long TEST_TIMEOUT_SECONDS = 5;
    private static final long TEST_STALE_SECONDS = 60;
    private static final long TEST_RESERVATION_SECONDS = 60;
    private static final String DIGEST = "0123456789abcdef";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void acquisitionRetainsCurrentRuntimeAndRemovesInactiveOlderRuntime() throws Exception {
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        final String old = "sbk-runtime-10.5-linux-amd64-old";
        createRuntime(current, DIGEST);
        createRuntime(old, "old-digest");

        run(RemoteRuntimeLifecycle.acquireCommand(temporaryDirectory.toString(), current, DIGEST,
                "run-1", true, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));

        assertTrue(Files.isDirectory(temporaryDirectory.resolve(current)));
        assertFalse(Files.exists(temporaryDirectory.resolve(old)));
        assertEquals(current, Files.readString(temporaryDirectory.resolve(".sbk-runtime-current")).trim());
        assertTrue(Files.readString(Path.of(RemoteRuntimeLifecycle.leasePath(
                temporaryDirectory.toString(), current, "run-1"))).startsWith("reserved:"));
    }

    @Test
    void acquisitionPreservesAnOlderRuntimeWithALiveProcessLease() throws Exception {
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        final String active = "sbk-runtime-10.5-linux-amd64-active";
        createRuntime(current, DIGEST);
        createRuntime(active, "active-digest");
        final Path activeLease = Path.of(RemoteRuntimeLifecycle.leasePath(
                temporaryDirectory.toString(), active, "active-run"));
        Files.createDirectories(Objects.requireNonNull(activeLease.getParent()));
        Files.writeString(activeLease, "pid:" + ProcessHandle.current().pid() + "\n", StandardCharsets.UTF_8);

        run(RemoteRuntimeLifecycle.acquireCommand(temporaryDirectory.toString(), current, DIGEST,
                "run-2", true, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));

        assertTrue(Files.isDirectory(temporaryDirectory.resolve(active)));
        assertTrue(Files.isRegularFile(activeLease));
    }

    @Test
    void launchConvertsReservationToPidAndReleasesItWithoutDeletingCurrentRuntime() throws Exception {
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        final String leaseId = "run-3";
        createRuntime(current, DIGEST);
        run(RemoteRuntimeLifecycle.acquireCommand(temporaryDirectory.toString(), current, DIGEST,
                leaseId, false, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));
        final String lease = RemoteRuntimeLifecycle.leasePath(temporaryDirectory.toString(), current, leaseId);
        final String release = RemoteRuntimeLifecycle.releaseCommand(temporaryDirectory.toString(), current,
                leaseId, false, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS);

        run(RemoteRuntimeLifecycle.launchCommand(lease, release, "true"));

        assertFalse(Files.exists(Path.of(lease)));
        assertTrue(Files.isDirectory(temporaryDirectory.resolve(current)));
    }

    @Test
    void releasingOlderLeaseRemovesFormerRuntimeButRetainsAuthoritativeCurrentRuntime() throws Exception {
        final String old = "sbk-runtime-10.5-linux-amd64-old";
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        createRuntime(old, "old-digest");
        run(RemoteRuntimeLifecycle.acquireCommand(temporaryDirectory.toString(), old, "old-digest",
                "old-run", true, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));
        createRuntime(current, DIGEST);
        run(RemoteRuntimeLifecycle.acquireCommand(temporaryDirectory.toString(), current, DIGEST,
                "current-run", true, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));
        assertTrue(Files.isDirectory(temporaryDirectory.resolve(old)));

        run(RemoteRuntimeLifecycle.releaseCommand(temporaryDirectory.toString(), old, "old-run",
                true, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));

        assertFalse(Files.exists(temporaryDirectory.resolve(old)));
        assertTrue(Files.isDirectory(temporaryDirectory.resolve(current)));
    }

    private void createRuntime(String name, String digest) throws IOException {
        final Path runtime = temporaryDirectory.resolve(name);
        Files.createDirectories(runtime);
        Files.writeString(runtime.resolve(SbkRuntimeBundle.DESCRIPTOR_FILE),
                "content.sha256=" + digest + "\n", StandardCharsets.UTF_8);
        Files.writeString(runtime.resolve(SbkRuntimeBundle.REMOTE_DIGEST_FILE),
                digest + "\n", StandardCharsets.UTF_8);
    }

    private static void run(String command) throws Exception {
        final Process process = new ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
    }
}

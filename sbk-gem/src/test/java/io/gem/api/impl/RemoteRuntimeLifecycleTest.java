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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests managed remote runtime leases and non-current-version cleanup using the local POSIX shell. */
final class RemoteRuntimeLifecycleTest {
    private static final long TEST_TIMEOUT_SECONDS = 5;
    private static final long TEST_STALE_SECONDS = 60;
    private static final long TEST_RESERVATION_SECONDS = 60;
    private static final String DIGEST = "0123456789abcdef";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void acquisitionRetainsCurrentRuntimeAndRemovesAllInactiveNonCurrentVersions() throws Exception {
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        final String old = "sbk-runtime-10.5-linux-amd64-old";
        final String newer = "sbk-runtime-10.7-linux-amd64-newer";
        createRuntime(current, DIGEST);
        createRuntime(old, "old-digest");
        createRuntime(newer, "newer-digest");

        run(RemoteRuntimeLifecycle.acquireCommand(temporaryDirectory.toString(), current, DIGEST,
                "run-1", true, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));

        assertTrue(Files.isDirectory(temporaryDirectory.resolve(current)));
        assertFalse(Files.exists(temporaryDirectory.resolve(old)));
        assertFalse(Files.exists(temporaryDirectory.resolve(newer)));
        assertEquals(current, Files.readString(temporaryDirectory.resolve(".sbk-runtime-current")).trim());
        assertTrue(Files.readString(Path.of(RemoteRuntimeLifecycle.leasePath(
                temporaryDirectory.toString(), current, "run-1"))).startsWith("reserved:"));
    }

    @Test
    void acquisitionAndCleanupCompleteWithAvailableShells() throws Exception {
        for (String shell : availableShells()) {
            final String shellName = shell.substring(shell.lastIndexOf('/') + 1);
            final Path parent = temporaryDirectory.resolve(shellName);
            final String current = "sbk-runtime-10.6-macos-current";
            final String inactive = "sbk-runtime-10.5-macos-inactive";
            createRuntime(parent, current, DIGEST);
            createRuntime(parent, inactive, "inactive-digest");

            run(shell, RemoteRuntimeLifecycle.acquireCommand(parent.toString(), current, DIGEST,
                    shellName + "-acquire", true, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS,
                    TEST_RESERVATION_SECONDS));

            assertTrue(Files.isDirectory(parent.resolve(current)), shell + " current runtime");
            assertFalse(Files.exists(parent.resolve(inactive)), shell + " inactive runtime cleanup");
        }
    }

    @Test
    void acquisitionPreservesANonCurrentRuntimeWithALiveProcessLease() throws Exception {
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        final String active = "sbk-runtime-10.7-linux-amd64-active";
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
    void launchPreservesCommandExitCodesAndReleasesLeaseWithAvailableShells() throws Exception {
        final String current = "sbk-runtime-10.6-linux-current";
        createRuntime(current, DIGEST);
        for (String shell : availableShells()) {
            verifyExit(shell, current, "success", "true", 0);
            verifyExit(shell, current, "failure", "exit 47", 47);
        }
    }

    @Test
    void launchPreservesSignalExitCodesAndReleasesLeaseWithAvailableShells() throws Exception {
        final String current = "sbk-runtime-10.6-linux-amd64-current";
        createRuntime(current, DIGEST);
        for (String shell : availableShells()) {
            verifySignal(shell, current, "HUP", 129);
            verifySignal(shell, current, "INT", 130);
            verifySignal(shell, current, "TERM", 143);
        }
    }

    @Test
    void launchUsesShellPortableExitCodeVariable() {
        final String command = RemoteRuntimeLifecycle.launchCommand("/tmp/lease", "true", "true");

        assertTrue(command.contains("sbk_exit_code=$?"));
        assertFalse(command.contains("status=$?"));
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
        createRuntime(temporaryDirectory, name, digest);
    }

    private static void createRuntime(Path parent, String name, String digest) throws IOException {
        final Path runtime = parent.resolve(name);
        Files.createDirectories(runtime);
        Files.writeString(runtime.resolve(SbkRuntimeBundle.DESCRIPTOR_FILE),
                "content.sha256=" + digest + "\n", StandardCharsets.UTF_8);
        Files.writeString(runtime.resolve(SbkRuntimeBundle.REMOTE_DIGEST_FILE),
                digest + "\n", StandardCharsets.UTF_8);
    }

    private void verifySignal(String shell, String deploymentName, String signal, int expectedExitCode)
            throws Exception {
        final String leaseId = shell.substring(shell.lastIndexOf('/') + 1) + "-" + signal.toLowerCase();
        run(RemoteRuntimeLifecycle.acquireCommand(temporaryDirectory.toString(), deploymentName, DIGEST,
                leaseId, false, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));
        final String lease = RemoteRuntimeLifecycle.leasePath(temporaryDirectory.toString(), deploymentName,
                leaseId);
        final String release = RemoteRuntimeLifecycle.releaseCommand(temporaryDirectory.toString(),
                deploymentName, leaseId, false, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS,
                TEST_RESERVATION_SECONDS);
        final Process process = new ProcessBuilder(shell, "-c", RemoteRuntimeLifecycle.launchCommand(
                lease, release, "while :; do sleep 1; done"))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        try {
            waitForPidLease(Path.of(lease));
            final Process signalProcess = new ProcessBuilder("kill", "-" + signal,
                    Long.toString(process.pid())).start();
            assertEquals(0, signalProcess.waitFor(), shell + " " + signal + " signal delivery");
            assertTrue(process.waitFor(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    shell + " did not exit after " + signal);
            assertEquals(expectedExitCode, process.exitValue(), shell + " " + signal + " exit code");
            assertFalse(Files.exists(Path.of(lease)), shell + " " + signal + " lease cleanup");
        } finally {
            process.destroyForcibly();
            process.waitFor();
        }
    }

    private void verifyExit(String shell, String deploymentName, String suffix, String command,
                            int expectedExitCode) throws Exception {
        final String shellName = shell.substring(shell.lastIndexOf('/') + 1);
        final String leaseId = shellName + "-" + suffix;
        run(RemoteRuntimeLifecycle.acquireCommand(temporaryDirectory.toString(), deploymentName, DIGEST,
                leaseId, false, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS, TEST_RESERVATION_SECONDS));
        final String lease = RemoteRuntimeLifecycle.leasePath(temporaryDirectory.toString(), deploymentName,
                leaseId);
        final String release = RemoteRuntimeLifecycle.releaseCommand(temporaryDirectory.toString(),
                deploymentName, leaseId, false, TEST_TIMEOUT_SECONDS, TEST_STALE_SECONDS,
                TEST_RESERVATION_SECONDS);
        final Process process = new ProcessBuilder(shell, "-c",
                RemoteRuntimeLifecycle.launchCommand(lease, release, command))
                .redirectErrorStream(true)
                .start();
        try {
            assertTrue(process.waitFor(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS), shell + " did not exit");
            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(expectedExitCode, process.exitValue(), output);
            assertFalse(Files.exists(Path.of(lease)), shell + " lease cleanup");
        } finally {
            process.destroyForcibly();
            process.waitFor();
        }
    }

    private static List<String> availableShells() {
        return List.of("/bin/sh", "/bin/bash", "/bin/zsh").stream()
                .filter(shell -> Files.isExecutable(Path.of(shell)))
                .toList();
    }

    private static void waitForPidLease(Path lease) throws Exception {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TEST_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(lease) && Files.readString(lease).startsWith("pid:")) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for PID lease: " + lease);
    }

    private static void run(String command) throws Exception {
        run("sh", command);
    }

    private static void run(String shell, String command) throws Exception {
        final Process process = new ProcessBuilder(shell, "-c", command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS), shell + " command did not exit");
        assertEquals(0, process.exitValue(), output);
    }
}

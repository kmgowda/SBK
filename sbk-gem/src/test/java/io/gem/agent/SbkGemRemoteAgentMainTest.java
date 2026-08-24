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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests remote Java compatibility with the controller minimum. */
final class SbkGemRemoteAgentMainTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void acceptsMatchingOrNewerJavaAndRejectsOlderJava() {
        assertTrue(SbkGemRemoteAgentMain.isJavaCompatible(25, 25));
        assertTrue(SbkGemRemoteAgentMain.isJavaCompatible(26, 25));
        assertFalse(SbkGemRemoteAgentMain.isJavaCompatible(24, 25));
    }

    @Test
    void stopsRemoteSbkProcessAndEveryCapturedDescendant() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            final Path pidFile = temporaryDirectory.resolve("process-tree.pids");
            final Process parent = new ProcessBuilder(javaExecutable(), "-classpath",
                    System.getProperty("java.class.path"), ProcessTreeFixture.class.getName(),
                    "parent", pidFile.toString()).start();
            try {
                final List<ProcessHandle> handles = awaitProcessTree(pidFile);

                SbkGemRemoteAgentMain.stopProcessTree(parent);

                awaitStopped(handles);
                assertTrue(handles.stream().noneMatch(ProcessHandle::isAlive));
            } finally {
                parent.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
                parent.destroyForcibly();
            }
        });
    }

    private static List<ProcessHandle> awaitProcessTree(Path pidFile) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (Files.isRegularFile(pidFile)) {
                final List<ProcessHandle> handles = Files.readAllLines(pidFile).stream()
                        .filter(value -> !value.isBlank()).mapToLong(Long::parseLong)
                        .mapToObj(ProcessHandle::of).flatMap(java.util.Optional::stream).toList();
                if (handles.size() == 3) {
                    return handles;
                }
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Java process-tree fixture did not start");
    }

    private static void awaitStopped(List<ProcessHandle> handles) throws InterruptedException {
        for (int attempt = 0; attempt < 100 && handles.stream().anyMatch(ProcessHandle::isAlive); attempt++) {
            Thread.sleep(50);
        }
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}

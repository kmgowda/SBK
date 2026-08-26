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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void remoteSbkReceivesTheSelectedJavaHomeAndSource() throws Exception {
        final Path javaHome = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
        final ProcessBuilder processBuilder = new ProcessBuilder(javaExecutable(), "-classpath",
                System.getProperty("java.class.path"), JavaEnvironmentFixture.class.getName())
                .redirectErrorStream(true);
        SbkGemRemoteAgentMain.configureRemoteJavaEnvironment(processBuilder, javaHome);

        final Process process = processBuilder.start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

        assertEquals(0, process.waitFor(), output);
        assertEquals(String.join("|", javaHome.toString(), javaHome.toString(),
                "SBK_GEM_REMOTE_JDK", javaHome.toString()), output);
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

    @Test
    void deletesOnlyRetiredRuntimeTreesLocally() throws Exception {
        final Path retired = temporaryDirectory.resolve(".sbk-runtime-retired.old");
        final Path current = temporaryDirectory.resolve("sbk-runtime-10.6-current");
        Files.createDirectories(retired.resolve("sbk/lib/nested"));
        Files.writeString(retired.resolve("sbk/lib/nested/runtime.jar"), "retired");
        Files.createDirectories(current);
        Files.writeString(current.resolve("runtime.jar"), "current");

        assertEquals(1, SbkGemRemoteAgentMain.cleanupRetiredRuntimes(temporaryDirectory));
        assertFalse(Files.exists(retired));
        assertTrue(Files.isRegularFile(current.resolve("runtime.jar")));
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

    static final class JavaEnvironmentFixture {
        private JavaEnvironmentFixture() {
        }

        /**
         * Print the Java environment inherited by the launched process.
         *
         * @param args unused
         */
        @SuppressFBWarnings(value = "ENV_USE_PROPERTY_INSTEAD_OF_ENV",
                justification = "This fixture verifies the environment contract passed to remote SBK")
        public static void main(String[] args) {
            System.out.print(String.join("|", System.getenv("SBK_JAVA_HOME"), System.getenv("JAVA_HOME"),
                    System.getenv("SBK_JAVA_SOURCE"),
                    Path.of(System.getProperty("java.home")).toAbsolutePath().normalize().toString()));
        }
    }
}

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Java-only child-process tree used by remote-agent lifecycle tests. */
public final class ProcessTreeFixture {
    private static final long WAIT_MILLIS = 60_000;

    private ProcessTreeFixture() {
    }

    /**
     * Start a parent, child, or grandchild process and report its PID.
     *
     * @param args role and shared PID file
     * @throws Exception when process creation or reporting fails
     */
    public static void main(String[] args) throws Exception {
        final String role = args[0];
        final Path pidFile = Path.of(args[1]);
        Files.writeString(pidFile, ProcessHandle.current().pid() + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (!"grandchild".equals(role)) {
            final String nextRole = "parent".equals(role) ? "child" : "grandchild";
            new ProcessBuilder(javaExecutable(), "-classpath", System.getProperty("java.class.path"),
                    ProcessTreeFixture.class.getName(), nextRole, pidFile.toString()).start();
        }
        Thread.sleep(WAIT_MILLIS);
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}

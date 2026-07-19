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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests discovery of the actual SBK version selected for remote deployment.
 */
final class LocalSbkDeploymentTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void discoversVersionFromSelectedExecutable() throws IOException, InterruptedException {
        final Path executable = launcher("printf 'SBK Version: 10.7\\n'");

        assertEquals("10.7", LocalSbkDeployment.discoverVersion(executable, 2));
    }

    @Test
    void rejectsInvalidVersionOutput() throws IOException {
        final Path executable = launcher("printf 'not an SBK version\\n'");

        assertThrows(IOException.class, () -> LocalSbkDeployment.discoverVersion(executable, 2));
    }

    private Path launcher(String command) throws IOException {
        final Path executable = temporaryDirectory.resolve("sbk test launcher");
        Files.writeString(executable, "#!/bin/sh\n" + command + "\n");
        if (!executable.toFile().setExecutable(true)) {
            throw new IOException("Unable to make test launcher executable");
        }
        return executable;
    }
}

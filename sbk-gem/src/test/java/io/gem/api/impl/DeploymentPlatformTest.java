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

import io.gem.api.SshResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests homogeneous deployment platform discovery. */
final class DeploymentPlatformTest {
    @Test
    void normalizesLinuxArchitectures() throws IOException {
        final SshResponse response = response(0, "SBK_OS=Linux\nSBK_ARCH=x86_64\nSBK_SHA256=sha256sum\n", "");

        assertEquals(new DeploymentPlatform("linux", "amd64"), DeploymentPlatform.fromProbe(response));
    }

    @Test
    void normalizesMacArmArchitecture() throws IOException {
        final SshResponse response = response(0, "SBK_OS=Darwin\nSBK_ARCH=arm64\nSBK_SHA256=shasum\n", "");

        assertEquals(new DeploymentPlatform("macos", "arm64"), DeploymentPlatform.fromProbe(response));
    }

    @Test
    void rejectsUnsupportedOrFailedProbe() throws IOException {
        assertNull(DeploymentPlatform.fromProbe(response(0, "SBK_OS=Windows\nSBK_ARCH=amd64\n", "")));
        assertNull(DeploymentPlatform.fromProbe(response(127, "", "tar command is required")));
    }

    @Test
    void preflightRequiresArchiveAndHashTools() {
        final String command = DeploymentPlatform.probeCommand();

        assertTrue(command.contains("command -v tar"));
        assertTrue(command.contains("command -v sha256sum"));
        assertTrue(command.contains("command -v shasum"));
    }

    private static SshResponse response(int returnCode, String output, String error) throws IOException {
        final SshResponse response = new SshResponse(true);
        response.returnCode = returnCode;
        response.stdOutputStream.write(output.getBytes(StandardCharsets.UTF_8));
        response.errOutputStream.write(error.getBytes(StandardCharsets.UTF_8));
        return response;
    }
}

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests remote Java discovery and launch-environment helpers.
 */
final class RemoteJavaDeploymentTest {

    @Test
    void parsesModernJavaVersion() {
        assertEquals(25, RemoteJavaDeployment.parseMajorVersion("openjdk version \"25.0.2\" 2026-01-20"));
    }

    @Test
    void parsesLegacyJavaVersion() {
        assertEquals(8, RemoteJavaDeployment.parseMajorVersion("java version \"1.8.0_402\""));
    }

    @Test
    void acceptsExpectedVersionAndExtractsJavaHome() throws IOException {
        final SshResponse response = response(0, "SBK_JAVA_HOME=/opt/jdk-25\n",
                "openjdk version \"25.0.2\"\n");

        assertTrue(RemoteJavaDeployment.hasExpectedVersion(response, 25));
        assertEquals("/opt/jdk-25", RemoteJavaDeployment.javaHome(response));
    }

    @Test
    void rejectsMissingOrMismatchedJava() throws IOException {
        final SshResponse missing = response(127, "", "");
        final SshResponse mismatched = response(0, "SBK_JAVA_HOME=/opt/jdk-21\n",
                "openjdk version \"21.0.7\"\n");

        assertFalse(RemoteJavaDeployment.hasExpectedVersion(missing, 25));
        assertFalse(RemoteJavaDeployment.hasExpectedVersion(mismatched, 25));
    }

    @Test
    void exportsSelectedJavaHomeForRemoteSbk() {
        final String prefix = RemoteJavaDeployment.environmentPrefix("/opt/SBK Java/jdk's");

        assertTrue(prefix.contains("export SBK_JAVA_HOME='/opt/SBK Java/jdk'\\''s'"));
        assertTrue(prefix.contains("export PATH=\"$SBK_JAVA_HOME/bin:$PATH\""));
    }

    @Test
    void buildsNodeSpecificRemoteSbkCommands() {
        final String firstCommand = RemoteJavaDeployment.launchCommand("/opt/jdk-25",
                "/opt/sbk/bin/sbk -class file");
        final String secondCommand = RemoteJavaDeployment.launchCommand("/srv/java/jdk-25",
                "/srv/sbk/bin/sbk -class file");

        assertTrue(firstCommand.startsWith("export SBK_JAVA_HOME='/opt/jdk-25';"));
        assertTrue(firstCommand.endsWith("/opt/sbk/bin/sbk -class file"));
        assertTrue(secondCommand.startsWith("export SBK_JAVA_HOME='/srv/java/jdk-25';"));
        assertTrue(secondCommand.endsWith("/srv/sbk/bin/sbk -class file"));
    }

    @Test
    void rejectsRemoteLaunchWithoutResolvedJavaHome() {
        assertThrows(IllegalArgumentException.class,
                () -> RemoteJavaDeployment.launchCommand("", "/opt/sbk/bin/sbk"));
        assertThrows(IllegalArgumentException.class,
                () -> RemoteJavaDeployment.launchCommand("/opt/jdk-25", ""));
    }

    @Test
    void probesConfiguredJavaExecutable() {
        final String command = RemoteJavaDeployment.homeProbeCommand("/opt/SBK Java");

        assertTrue(command.contains("[ -x '/opt/SBK Java/bin/java' ]"));
        assertTrue(command.contains("SBK_JAVA_HOME=%s"));
    }

    private static SshResponse response(int returnCode, String standardOutput, String errorOutput)
            throws IOException {
        final SshResponse response = new SshResponse(true);
        response.returnCode = returnCode;
        response.stdOutputStream.write(standardOutput.getBytes(StandardCharsets.UTF_8));
        response.errOutputStream.write(errorOutput.getBytes(StandardCharsets.UTF_8));
        return response;
    }
}

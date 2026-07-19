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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests remote SBK deployment decisions.
 */
final class RemoteSbkDeploymentTest {
    private String expectedVersion;

    @BeforeEach
    void readExpectedVersionFromPackagedJar() throws IOException {
        final String jarPath = System.getProperty("io.sbk.test.jar");
        assertNotNull(jarPath, "Gradle must provide the packaged SBK-GEM JAR path");
        final URL[] classPath = {Path.of(jarPath).toUri().toURL()};
        try (URLClassLoader classLoader = new URLClassLoader(classPath, ClassLoader.getPlatformClassLoader())) {
            final Class<?> sbkGemClass;
            try {
                sbkGemClass = Class.forName(SbkGem.class.getName(), false, classLoader);
            } catch (ClassNotFoundException ex) {
                throw new IOException("Unable to load SBK-GEM from its packaged JAR", ex);
            }
            expectedVersion = sbkGemClass.getPackage().getImplementationVersion();
        }
        assertNotNull(expectedVersion, "SBK-GEM JAR must contain Implementation-Version");
    }

    @Test
    void skipsCopyWhenRemoteVersionMatches() throws IOException {
        final SshResponse response = response(0, "SBK Version: " + expectedVersion + "\n");

        assertFalse(RemoteSbkDeployment.requiresCopy(false, response, expectedVersion));
    }

    @Test
    void copiesWhenEnabledAndRemoteVersionDiffers() throws IOException {
        final SshResponse response = response(0, "SBK Version: " + expectedVersion + "-different\n");

        assertTrue(RemoteSbkDeployment.requiresCopy(true, response, expectedVersion));
    }

    @Test
    void copiesWhenEnabledAndRemoteExecutableIsMissing() throws IOException {
        final SshResponse response = response(127, "");

        assertTrue(RemoteSbkDeployment.requiresCopy(true, response, expectedVersion));
    }

    @Test
    void matchingVersionIsNeverCopied() throws IOException {
        final SshResponse response = response(0, "SBK Version: " + expectedVersion + "\n");

        assertFalse(RemoteSbkDeployment.requiresCopy(true, response, expectedVersion));
    }

    @Test
    void disabledCopyDoesNotReplaceMismatch() throws IOException {
        final SshResponse response = response(0, "SBK Version: " + expectedVersion + "-different\n");

        assertFalse(RemoteSbkDeployment.requiresCopy(false, response, expectedVersion));
    }

    @Test
    void deletesOnlyExistingMismatchWhenEnabled() throws IOException {
        final SshResponse mismatch = response(0, "SBK Version: " + expectedVersion + "-different\n");
        final SshResponse missing = response(127, "");
        final SshResponse probeFailure = response(126, "");
        final SshResponse matching = response(0, "SBK Version: " + expectedVersion + "\n");

        assertTrue(RemoteSbkDeployment.requiresDeleteBeforeCopy(true, mismatch, expectedVersion));
        assertFalse(RemoteSbkDeployment.requiresDeleteBeforeCopy(false, mismatch, expectedVersion));
        assertFalse(RemoteSbkDeployment.requiresDeleteBeforeCopy(true, missing, expectedVersion));
        assertFalse(RemoteSbkDeployment.requiresDeleteBeforeCopy(true, probeFailure, expectedVersion));
        assertFalse(RemoteSbkDeployment.requiresDeleteBeforeCopy(true, matching, expectedVersion));
        assertFalse(RemoteSbkDeployment.requiresCopy(true, probeFailure, expectedVersion));
    }

    @Test
    void quotesRemoteCommandPaths() {
        final String probe = RemoteSbkDeployment.versionProbeCommand("work dir/sbk's/bin/sbk");

        assertTrue(probe.contains("'work dir/sbk'\\''s/bin/sbk' -version"));
    }

    @Test
    void resolvesAndVerifiesQuotedRemoteExecutablePath() throws IOException {
        final String probe = RemoteSbkDeployment.executablePathProbeCommand("work dir", "sbk's/bin/sbk");
        final SshResponse response = response(0, "/home/user/work dir/sbk's/bin/sbk\n");

        assertTrue(probe.startsWith("cd -- 'work dir' && base=\"$(pwd -P)\""));
        assertTrue(probe.contains("sbk_path=\"$base/\"'sbk'\\''s/bin/sbk'"));
        assertTrue(probe.contains("if [ -x \"$sbk_path\" ]"));
        assertEquals("/home/user/work dir/sbk's/bin/sbk",
                RemoteSbkDeployment.absoluteExecutablePath(response));
    }

    @Test
    void rejectsFailedOrRelativeExecutableResolution() throws IOException {
        assertNull(RemoteSbkDeployment.absoluteExecutablePath(response(1, "/home/user/sbk/bin/sbk\n")));
        assertNull(RemoteSbkDeployment.absoluteExecutablePath(response(0, "relative/sbk/bin/sbk\n")));
    }

    private static SshResponse response(int returnCode, String standardOutput) throws IOException {
        final SshResponse response = new SshResponse(true);
        response.returnCode = returnCode;
        response.stdOutputStream.write(standardOutput.getBytes(StandardCharsets.UTF_8));
        return response;
    }
}

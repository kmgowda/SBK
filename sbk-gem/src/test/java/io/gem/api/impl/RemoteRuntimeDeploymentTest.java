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

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests immutable runtime remote command construction. */
final class RemoteRuntimeDeploymentTest {
    @Test
    void probeChecksDigestJavaCompilerAndSbkVersion() {
        final String command = RemoteRuntimeDeployment.probeCommand("/srv/runtime", "content-sha",
                "/srv/runtime/java", "/srv/runtime/sbk/bin/sbk", "10.6", 25);

        assertTrue(command.contains(".sbk-runtime.sha256"));
        assertTrue(command.contains("/bin/java"));
        assertTrue(command.contains("/bin/javac"));
        assertTrue(command.contains("/srv/runtime/sbk/bin/sbk"));
        assertTrue(command.contains("SBK_RUNTIME_CONTENT"));
    }

    @Test
    void activationVerifiesArchiveFilesPlatformAndUsesAtomicDirectoryMove() {
        final String command = RemoteRuntimeDeployment.activateCommand("/srv/archive.tgz", "archive-sha",
                "content-sha", "/srv/staging", "/srv/final", "linux", "amd64", false);

        assertTrue(command.contains("sha256sum"));
        assertTrue(command.contains("shasum -a 256"));
        assertTrue(command.contains("tar -xzf"));
        assertTrue(command.contains("deployment-files.sha256"));
        assertTrue(command.contains("platform.os=linux"));
        assertTrue(command.contains("platform.arch=amd64"));
        assertTrue(command.contains("else mv '/srv/staging/runtime' \"$final_dir\""));
        assertTrue(command.contains("trap cleanup EXIT HUP INT TERM"));
    }
}

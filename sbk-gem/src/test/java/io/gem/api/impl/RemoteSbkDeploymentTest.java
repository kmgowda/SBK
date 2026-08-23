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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests remote SBK deployment decisions.
 */
final class RemoteSbkDeploymentTest {
    @Test
    void quotesEveryRemoteCommandToken() {
        final String command = RemoteSbkDeployment.shellJoin(List.of("/opt/SBK dir/bin/sbk", "-file",
                "/tmp/a file; touch /tmp/not-created", "value's"));

        assertEquals("'/opt/SBK dir/bin/sbk' '-file' '/tmp/a file; touch /tmp/not-created' 'value'\\''s'",
                command);
    }

    @Test
    void parsesOnlyAuthoritativeVersionLine() {
        assertEquals("10.6", RemoteSbkDeployment.parseVersion("SBK Version: 10.6\n"));
        assertNull(RemoteSbkDeployment.parseVersion("SBK-GEM Version: 10.6\n"));
    }

    @Test
    void resolvesRemoteDirectoryToAbsolutePath() throws IOException {
        final String probe = RemoteSbkDeployment.directoryPathProbeCommand("work dir");
        final SshResponse response = response(0, "/home/user/work dir\n");

        assertEquals("case 'work dir' in /*) printf '%s\\n' 'work dir';; *) printf '%s/%s\\n' " +
                "\"$(pwd -P)\" 'work dir';; esac", probe);
        assertEquals("/home/user/work dir", RemoteSbkDeployment.absoluteDirectoryPath(response));
        assertNull(RemoteSbkDeployment.absoluteDirectoryPath(response(0, "relative/work dir\n")));
        assertNull(RemoteSbkDeployment.absoluteDirectoryPath(response(1, "/home/user/work dir\n")));
    }

    private static SshResponse response(int returnCode, String standardOutput) throws IOException {
        final SshResponse response = new SshResponse(true);
        response.returnCode = returnCode;
        response.stdOutputStream.write(standardOutput.getBytes(StandardCharsets.UTF_8));
        return response;
    }
}

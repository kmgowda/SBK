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

import io.gem.agent.RemoteAgentProtocol;
import io.gem.api.SshResponse;
import io.sbk.config.ExitCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests typed remote-agent requests and executable-only SSH commands. */
final class RemoteAgentTest {
    @Test
    void preservesBenchmarkArgumentsWithoutShellSerialization() throws Exception {
        final List<String> arguments = List.of("-file", "/tmp/a file; false", "value's");
        final byte[] bytes = RemoteAgent.run("/srv/runtime", "10.6", List.of("-Xms1g"), arguments);
        final RemoteAgentProtocol.Request request = RemoteAgentProtocol.read(
                new DataInputStream(new ByteArrayInputStream(bytes)));

        assertEquals("run", request.operation());
        assertEquals(List.of("/srv/runtime", "10.6", "1", "-Xms1g", "-file",
                "/tmp/a file; false", "value's"), request.values());
    }

    @Test
    void createsTypedRetiredRuntimeCleanupRequest() throws Exception {
        final byte[] bytes = RemoteAgent.cleanup("/srv/sbk gem");
        final RemoteAgentProtocol.Request request = RemoteAgentProtocol.read(
                new DataInputStream(new ByteArrayInputStream(bytes)));

        assertEquals("cleanup", request.operation());
        assertEquals(List.of("/srv/sbk gem"), request.values());
    }

    @Test
    void createsTypedRuntimeLifecycleRequests() throws Exception {
        assertRequest(RemoteAgent.reserveRuntime("/srv/sbk gem", "runtime-a", "lease-a", 30, 60),
                "runtime-reserve", List.of("/srv/sbk gem", "runtime-a", "lease-a", "30", "60"));
        assertRequest(RemoteAgent.acquireRuntime("/srv/sbk gem", "runtime-a", "digest-a", "lease-a",
                        true, 30, 60, 300),
                "runtime-acquire", List.of("/srv/sbk gem", "runtime-a", "digest-a", "lease-a", "true",
                        "30", "60", "300"));
        assertRequest(RemoteAgent.heartbeatRuntime("/srv/sbk gem", "runtime-a", "lease-a", 30, 60),
                "runtime-heartbeat", List.of("/srv/sbk gem", "runtime-a", "lease-a", "30", "60"));
        assertRequest(RemoteAgent.releaseRuntime("/srv/sbk gem", "runtime-a", "lease-a", false,
                        30, 60, 300),
                "runtime-release", List.of("/srv/sbk gem", "runtime-a", "lease-a", "false", "30", "60",
                        "300"));
    }

    @Test
    void commandContainsOnlyTheJavaAgentInvocation() {
        final String command = RemoteAgent.command("/opt/JDK 25/bin/java", "/srv/agent's.jar");

        assertEquals("'/opt/JDK 25/bin/java' -jar '/srv/agent'\\''s.jar'", command);
        assertFalse(command.contains("sh -c"));
        assertFalse(command.contains(";"));
        assertFalse(command.contains("&&"));
    }

    @Test
    void rejectsControlCharactersInExecutablePaths() {
        assertThrows(IllegalArgumentException.class, () -> RemoteAgent.command("java\nfalse", "/agent.jar"));
    }

    @Test
    void rejectsAgentRequestsWithTooManyValues() {
        assertThrows(java.io.IOException.class, () -> RemoteAgentProtocol.encode("run",
                Collections.nCopies(RemoteAgentProtocol.MAX_VALUES + 1, "value")));
    }

    @Test
    void recognizesOnlyArchiveIntegrityFailuresAsRetryable() throws Exception {
        final SshResponse integrityFailure = new SshResponse(true);
        integrityFailure.returnCode = 70;
        integrityFailure.errOutputStream.write(("SBK-GEM remote agent failed: IOException: "
                + RemoteAgentProtocol.ARCHIVE_DIGEST_MISMATCH).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        final SshResponse otherFailure = new SshResponse(true);
        otherFailure.returnCode = 70;
        otherFailure.errOutputStream.write("permission denied".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        final SshResponse success = new SshResponse(true);
        success.returnCode = ExitCode.SUCCESS;

        assertTrue(RemoteAgent.archiveDigestMismatch(integrityFailure));
        assertFalse(RemoteAgent.archiveDigestMismatch(otherFailure));
        assertFalse(RemoteAgent.archiveDigestMismatch(success));
    }

    @Test
    void parsesRetiredRuntimeCleanupCount() throws Exception {
        final SshResponse success = new SshResponse(true);
        success.returnCode = ExitCode.SUCCESS;
        success.stdOutputStream.write("SBK_RETIRED_RUNTIMES=3\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(3, RemoteAgent.retiredRuntimeCount(success));
    }

    private static void assertRequest(byte[] bytes, String operation, List<String> values) throws Exception {
        final RemoteAgentProtocol.Request request = RemoteAgentProtocol.read(
                new DataInputStream(new ByteArrayInputStream(bytes)));
        assertEquals(operation, request.operation());
        assertEquals(values, request.values());
    }
}

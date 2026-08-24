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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}

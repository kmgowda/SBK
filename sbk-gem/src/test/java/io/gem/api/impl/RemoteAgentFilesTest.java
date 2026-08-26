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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests combined remote-directory and Java-agent preparation. */
final class RemoteAgentFilesTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void resolvesDirectoryAndReusesAgentWithinOneFileSystemOperation() throws IOException {
        final Path agent = temporaryDirectory.resolve("agent.jar");
        Files.writeString(agent, "agent", StandardCharsets.UTF_8);
        final Path configured = temporaryDirectory.resolve("remote");
        final String digest = RemoteAgentFiles.digest(agent);

        final RemoteAgentFiles.AgentBootstrap first = RemoteAgentFiles.prepare(
                configured.getFileSystem(), configured.toString(), agent, "10.6", digest);
        final RemoteAgentFiles.AgentBootstrap reused = RemoteAgentFiles.prepare(
                configured.getFileSystem(), configured.toString(), agent, "10.6", digest);

        assertEquals(configured.toRealPath().toString(), first.directory());
        assertEquals(first, reused);
        assertTrue(Files.isRegularFile(Path.of(first.agentPath())));
        assertEquals(digest, Files.readString(configured.resolve(".sbk-gem-agent-10.6.sha256")).trim());
    }
}

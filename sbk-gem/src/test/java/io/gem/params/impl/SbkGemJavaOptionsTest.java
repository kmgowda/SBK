/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.params.impl;

import io.gem.config.GemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Java-provisioning defaults exposed by SBK-GEM.
 */
final class SbkGemJavaOptionsTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadsJavaProvisioningDefaultsFromGemProperties() throws IOException {
        final InputStream properties = SbkGemJavaOptionsTest.class.getClassLoader()
                .getResourceAsStream("gem.properties");
        assertNotNull(properties);
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());
        final GemConfig config;
        try (properties) {
            config = mapper.readValue(properties, GemConfig.class);
        }

        assertTrue(config.javacopy);
        assertEquals(25, config.javaversion);
        assertTrue(config.javadir == null || config.javadir.isEmpty());
        assertTrue(config.delete);
        assertTrue(config.runtimecleanup);
        assertTrue(config.hostkeycheck);
        assertTrue(config.knownhosts == null || config.knownhosts.isEmpty());
        assertEquals(120, config.sbmRegistrationTimeoutSeconds);
    }

    @Test
    void parsesJavaProvisioningCliOverrides() throws Exception {
        final Path binDirectory = temporaryDirectory.resolve("bin");
        final Path command = binDirectory.resolve("sbk");
        Files.createDirectories(binDirectory);
        Files.createFile(command);
        assertTrue(command.toFile().setExecutable(true));

        final GemConfig config = defaultConfig(temporaryDirectory);
        final SbkGemParameters parameters = new SbkGemParameters("test", new String[0], new String[0], config,
                9717, 10);
        parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1", "-records", "1", "-size", "1",
                "-javacopy", "false", "-javaversion", "21", "-javadir", "/opt/java-21"});

        assertFalse(parameters.isJavaCopy());
        assertEquals(21, parameters.getJavaVersion());
        assertEquals("/opt/java-21", parameters.getJavaDir());
    }

    @Test
    void parsesSbkCommandCliOverride() throws Exception {
        final Path customBinDirectory = temporaryDirectory.resolve("custom-bin");
        final Path customCommand = customBinDirectory.resolve("custom-sbk");
        Files.createDirectories(customBinDirectory);
        Files.createFile(customCommand);
        assertTrue(customCommand.toFile().setExecutable(true));

        final GemConfig config = defaultConfig(temporaryDirectory);
        final SbkGemParameters parameters = new SbkGemParameters("test", new String[0], new String[0], config,
                9717, 10);
        parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1", "-records", "1", "-size", "1",
                "-sbkcommand", "custom-bin/custom-sbk"});

        assertEquals("custom-bin/custom-sbk", parameters.getSbkCommand());
    }

    @Test
    void parsesSbkDeploymentLifecycleOverride() throws Exception {
        final Path binDirectory = temporaryDirectory.resolve("bin");
        final Path command = binDirectory.resolve("sbk");
        Files.createDirectories(binDirectory);
        Files.createFile(command);
        assertTrue(command.toFile().setExecutable(true));

        final GemConfig config = defaultConfig(temporaryDirectory);
        final SbkGemParameters parameters = new SbkGemParameters("test", new String[0], new String[0], config,
                9717, 10);
        parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1", "-records", "1", "-size", "1",
                "-delete", "false", "-runtimecleanup", "false"});

        assertFalse(parameters.isDelete());
        assertFalse(parameters.isRuntimeCleanup());
    }

    @Test
    void rejectsRemovedCopyAndDeleteAfterOptionsWithMigrationGuidance() throws Exception {
        createSbkCommand();
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException copyFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-copy", "false"}));
        assertTrue(copyFailure.getMessage().contains("copied automatically"));

        final IllegalArgumentException deleteFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-deleteafter", "true"}));
        assertTrue(deleteFailure.getMessage().contains("-runtimecleanup"));
    }

    @Test
    void parsesSshHostKeyOverrides() throws Exception {
        final Path binDirectory = temporaryDirectory.resolve("bin");
        final Path command = binDirectory.resolve("sbk");
        Files.createDirectories(binDirectory);
        Files.createFile(command);
        assertTrue(command.toFile().setExecutable(true));

        final GemConfig config = defaultConfig(temporaryDirectory);
        final SbkGemParameters parameters = new SbkGemParameters("test", new String[0], new String[0], config,
                9717, 10);
        parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1", "-records", "1", "-size", "1",
                "-hostkeycheck", "false", "-knownhosts", "/tmp/sbk-known-hosts"});

        assertFalse(parameters.getConnections()[0].isHostKeyCheck());
        assertEquals("/tmp/sbk-known-hosts", parameters.getConnections()[0].getKnownHosts());
    }

    @Test
    void parsesNodeSpecificSshPorts() throws Exception {
        createSbkCommand();
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(new String[]{"-nodes", "node-a:2201,node-b:2202,[::1]:2203", "-writers", "1",
                "-records", "3", "-size", "1"});

        assertEquals("node-a", parameters.getConnections()[0].getHost());
        assertEquals(2201, parameters.getConnections()[0].getPort());
        assertEquals("node-b", parameters.getConnections()[1].getHost());
        assertEquals(2202, parameters.getConnections()[1].getPort());
        assertEquals("::1", parameters.getConnections()[2].getHost());
        assertEquals(2203, parameters.getConnections()[2].getPort());
    }

    @Test
    void usesGlobalSshPortWhenNodeDoesNotOverrideIt() throws Exception {
        createSbkCommand();
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(new String[]{"-nodes", "node-a", "-gemport", "2222", "-writers", "1",
                "-records", "1", "-size", "1"});

        assertEquals(2222, parameters.getConnections()[0].getPort());
    }

    @Test
    void rejectsInvalidNodeSpecificSshPort() throws Exception {
        createSbkCommand();
        final SbkGemParameters parameters = parameters();

        assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-nodes", "node-a:70000", "-writers", "1",
                        "-records", "1", "-size", "1"}));
    }

    private SbkGemParameters parameters() {
        return new SbkGemParameters("test", new String[0], new String[0], defaultConfig(temporaryDirectory),
                9717, 10);
    }

    private void createSbkCommand() throws IOException {
        final Path binDirectory = temporaryDirectory.resolve("bin");
        final Path command = binDirectory.resolve("sbk");
        Files.createDirectories(binDirectory);
        Files.createFile(command);
        assertTrue(command.toFile().setExecutable(true));
    }

    private static GemConfig defaultConfig(Path sbkDirectory) {
        final GemConfig config = new GemConfig();
        config.nodes = "localhost";
        config.gemuser = "user";
        config.gempass = "";
        config.gemport = 22;
        config.hostkeycheck = true;
        config.knownhosts = "";
        config.sbkdir = sbkDirectory.toString();
        config.sbkcommand = "bin/sbk";
        config.javacopy = true;
        config.javaversion = 25;
        config.javadir = "";
        config.delete = true;
        config.runtimecleanup = true;
        config.timeoutSeconds = 5;
        config.remoteDir = "sbk-gem-test";
        return config;
    }
}

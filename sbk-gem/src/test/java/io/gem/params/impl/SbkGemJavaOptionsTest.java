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

        assertTrue(config.javadir == null || config.javadir.isEmpty());
        assertTrue(config.packagescleanup);
        assertFalse(config.fullcopy);
        assertFalse(config.hostkeycheck);
        assertTrue(config.knownhosts == null || config.knownhosts.isEmpty());
        assertEquals(120, config.sbmRegistrationTimeoutSeconds);
    }

    @Test
    void parsesPreferredRemoteJavaHome() throws Exception {
        final Path binDirectory = temporaryDirectory.resolve("bin");
        final Path command = binDirectory.resolve("sbk");
        Files.createDirectories(binDirectory);
        Files.createFile(command);
        assertTrue(command.toFile().setExecutable(true));

        final GemConfig config = defaultConfig(temporaryDirectory);
        final SbkGemParameters parameters = new SbkGemParameters("test", new String[0], new String[0], config,
                9717, 10);
        parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1", "-records", "1", "-size", "1",
                "-javadir", "/opt/java-25"});

        assertEquals("/opt/java-25", parameters.getJavaDir());
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
                "-packagescleanup", "false"});

        assertFalse(parameters.isPackagesCleanup());
    }

    @Test
    void enablesFullJavaAndSbkDistributionCopyOnlyWhenExplicitlyRequested() throws Exception {
        createSbkCommand();
        final GemConfig config = defaultConfig(temporaryDirectory);
        final SbkGemParameters parameters = new SbkGemParameters("test", new String[0], new String[0], config,
                9717, 10);

        parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1", "-records", "1", "-size", "1",
                "-fullcopy", "true"});

        assertTrue(config.fullcopy);
    }

    @Test
    void rejectsInvalidBooleanOptions() throws Exception {
        createSbkCommand();
        final SbkGemParameters parameters = parameters();

        assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1",
                    "-records", "1", "-size", "1", "-hostkeycheck", "yes"}));
    }

    @Test
    void detectsExplicitSbmCallbackAddressOverride() throws Exception {
        createSbkCommand();
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1", "-records", "1", "-size", "1",
                "-localhost", "controller.example"});

        assertTrue(parameters.isLocalHostOption());
        assertEquals("controller.example", parameters.getLocalHost());
    }

    @Test
    void enablesRouteSelectedSbmAddressWhenOverrideIsAbsent() throws Exception {
        createSbkCommand();
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(new String[]{"-nodes", "node-a", "-writers", "1", "-records", "1", "-size", "1"});

        assertFalse(parameters.isLocalHostOption());
    }

    @Test
    void rejectsRemovedDeploymentOptionsWithMigrationGuidance() throws Exception {
        createSbkCommand();
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException cleanupFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-runtimecleanup", "true"}));
        assertTrue(cleanupFailure.getMessage().contains("-packagescleanup"));

        final IllegalArgumentException compactCopyFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-copyonlydrivers", "true"}));
        assertTrue(compactCopyFailure.getMessage().contains("-fullcopy"));
        final IllegalArgumentException formerCompactCopyFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-compactruntimecopy", "true"}));
        assertTrue(formerCompactCopyFailure.getMessage().contains("-fullcopy"));
        final IllegalArgumentException compactNameFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-compactcopy", "true"}));
        assertTrue(compactNameFailure.getMessage().contains("-fullcopy"));

        final IllegalArgumentException copyFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-copy", "false"}));
        assertTrue(copyFailure.getMessage().contains("copied automatically"));

        final IllegalArgumentException deleteFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-deleteafter", "true"}));
        assertTrue(deleteFailure.getMessage().contains("-packagescleanup"));

        final IllegalArgumentException managedDeleteFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-delete", "false"}));
        assertTrue(managedDeleteFailure.getMessage().contains("repaired automatically"));
        assertFalse(parameters.getHelpText().contains("-delete "));

        final IllegalArgumentException commandFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-sbkcommand", "custom-bin/custom-sbk"}));
        assertTrue(commandFailure.getMessage().contains(GemConfig.SBK_COMMAND));
        assertFalse(parameters.getHelpText().contains("-sbkcommand"));

        final IllegalArgumentException directoryFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-sbkdir", temporaryDirectory.toString()}));
        assertTrue(directoryFailure.getMessage().contains("sbk.appHome"));
        assertFalse(parameters.getHelpText().contains("-sbkdir"));

        final IllegalArgumentException javaCopyFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-javacopy", "false"}));
        assertTrue(javaCopyFailure.getMessage().contains("provisions the controller JDK automatically"));
        assertFalse(parameters.getHelpText().contains("-javacopy"));

        final IllegalArgumentException javaVersionFailure = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(new String[]{"-javaversion", "21"}));
        assertTrue(javaVersionFailure.getMessage().contains("controller Java major version or newer"));
        assertFalse(parameters.getHelpText().contains("-javaversion"));
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
        config.javadir = "";
        config.packagescleanup = true;
        config.fullcopy = false;
        config.timeoutSeconds = 5;
        config.remoteDir = "sbk-gem-test";
        return config;
    }
}

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
        assertFalse(config.delete);
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

    private static GemConfig defaultConfig(Path sbkDirectory) {
        final GemConfig config = new GemConfig();
        config.nodes = "localhost";
        config.gemuser = "user";
        config.gempass = "";
        config.gemport = 22;
        config.sbkdir = sbkDirectory.toString();
        config.sbkcommand = "bin/sbk";
        config.copy = false;
        config.javacopy = true;
        config.javaversion = 25;
        config.javadir = "";
        config.delete = false;
        config.timeoutSeconds = 5;
        config.remoteDir = "sbk-gem-test";
        return config;
    }
}

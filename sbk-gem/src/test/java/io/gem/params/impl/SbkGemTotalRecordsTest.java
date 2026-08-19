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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests aggregate record parsing and validation for SBK-GEM. */
final class SbkGemTotalRecordsTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void parsesFixedAggregateRecordCount() throws Exception {
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(arguments("node-a,node-b", "-writers", "1", "-totalrecords", "1001"));

        assertTrue(parameters.isTotalRecordsOption());
        assertEquals(1001, parameters.getTotalRecords());
        assertEquals(0, parameters.getTotalSecondsToRun());
    }

    @Test
    void parsesAggregateRecordsPerSecond() throws Exception {
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(arguments("node-a,node-b,node-c", "-writers", "2", "-totalrecords", "1000",
                "-seconds", "30"));

        assertTrue(parameters.isTotalRecordsOption());
        assertEquals(1000, parameters.getTotalRecords());
        assertEquals(30, parameters.getTotalSecondsToRun());
    }

    @Test
    void rejectsPerClientAndAggregateRecordOptionsTogether() throws IOException {
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments("node-a,node-b", "-writers", "1", "-records", "10",
                        "-totalrecords", "20")));

        assertTrue(exception.getMessage().contains("mutually exclusive"));
    }

    @Test
    void rejectsThroughputAndAggregateRecordOptionsTogether() throws IOException {
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments("node-a", "-writers", "1", "-totalrecords", "20",
                        "-throughput", "1", "-seconds", "10")));

        assertTrue(exception.getMessage().contains("mutually exclusive"));
    }

    @Test
    void rejectsFixedTotalThatCannotGiveEveryNodeWork() throws IOException {
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments("node-a,node-b", "-writers", "1", "-totalrecords", "1")));

        assertTrue(exception.getMessage().contains("at least the number of nodes"));
    }

    @Test
    void rejectsRateThatCannotBeDividedExactlyAcrossWorkers() throws IOException {
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments("node-a,node-b", "-writers", "2", "-totalrecords", "101",
                        "-seconds", "10")));

        assertTrue(exception.getMessage().contains("divisible"));
    }

    @Test
    void exposesAggregateOptionInHelp() throws IOException {
        assertTrue(parameters().getHelpText().contains("-totalrecords"));
    }

    private SbkGemParameters parameters() throws IOException {
        final Path binDirectory = temporaryDirectory.resolve("bin");
        final Path command = binDirectory.resolve("sbk");
        Files.createDirectories(binDirectory);
        if (Files.notExists(command)) {
            Files.createFile(command);
        }
        assertTrue(command.toFile().setExecutable(true));
        return new SbkGemParameters("test", new String[0], new String[0], defaultConfig(), 9717, 10);
    }

    private GemConfig defaultConfig() {
        final GemConfig config = new GemConfig();
        config.nodes = "localhost";
        config.gemuser = "user";
        config.gempass = "";
        config.gemport = 22;
        config.hostkeycheck = true;
        config.knownhosts = "";
        config.sbkdir = temporaryDirectory.toString();
        config.sbkcommand = "bin/sbk";
        config.copy = true;
        config.javacopy = true;
        config.javaversion = 25;
        config.javadir = "";
        config.delete = true;
        config.deleteafter = false;
        config.timeoutSeconds = 5;
        config.remoteDir = "sbk-gem-test";
        return config;
    }

    private static String[] arguments(String nodes, String... benchmarkArguments) {
        final String[] arguments = new String[benchmarkArguments.length + 4];
        arguments[0] = "-nodes";
        arguments[1] = nodes;
        arguments[2] = "-size";
        arguments[3] = "1";
        System.arraycopy(benchmarkArguments, 0, arguments, 4, benchmarkArguments.length);
        return arguments;
    }
}

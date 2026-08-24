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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests aggregate throughput parsing and validation for SBK-GEM. */
final class SbkGemTotalThroughputTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void parsesAggregateThroughput() throws Exception {
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(arguments("node-a,node-b", "-writers", "2", "-totalthroughput", "300.5",
                "-seconds", "30"));

        assertTrue(parameters.isTotalThroughputOption());
        assertEquals(new BigDecimal("300.5"), parameters.getTotalThroughput());
        assertEquals(30, parameters.getTotalSecondsToRun());
    }

    @Test
    void rejectsPerClientAndAggregateThroughputTogether() throws IOException {
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments("node-a", "-writers", "1", "-throughput", "10",
                        "-totalthroughput", "20", "-seconds", "10")));

        assertTrue(exception.getMessage().contains("mutually exclusive"));
    }

    @Test
    void rejectsNonPositiveAggregateThroughput() throws IOException {
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments("node-a", "-writers", "1", "-totalthroughput", "0",
                        "-seconds", "10")));

        assertTrue(exception.getMessage().contains("greater than zero"));
    }

    @Test
    void permitsPerClientRecordCountWithAggregateThroughput() throws Exception {
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(arguments("node-a,node-b", "-writers", "1", "-records", "100",
                "-totalthroughput", "20"));

        assertEquals(100, parameters.getTotalRecords());
        assertEquals(0, parameters.getTotalSecondsToRun());
        assertEquals(new BigDecimal("20"), parameters.getTotalThroughput());
    }

    @Test
    void permitsFixedAggregateRecordsWithAggregateThroughput() throws Exception {
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(arguments("node-a,node-b", "-writers", "1", "-totalrecords", "101",
                "-totalthroughput", "20"));

        assertTrue(parameters.isTotalRecordsOption());
        assertEquals(101, parameters.getTotalRecords());
        assertEquals(new BigDecimal("20"), parameters.getTotalThroughput());
    }

    @Test
    void rejectsTwoAggregateRateControlsInTimedMode() throws IOException {
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments("node-a,node-b", "-writers", "1", "-totalrecords", "100",
                        "-totalthroughput", "20", "-seconds", "10")));

        assertTrue(exception.getMessage().contains("both would define the benchmark rate"));
    }

    @Test
    void rejectsAggregateThroughputWithUnequalMixedWorkerCounts() throws IOException {
        final SbkGemParameters parameters = parameters();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments("node-a,node-b", "-writers", "1", "-readers", "2",
                        "-totalthroughput", "20", "-seconds", "10")));

        assertTrue(exception.getMessage().contains("requires equal writer and reader counts"));
    }

    @Test
    void permitsAggregateThroughputWithEqualMixedWorkerCounts() throws Exception {
        final SbkGemParameters parameters = parameters();

        parameters.parseArgs(arguments("node-a,node-b", "-writers", "2", "-readers", "2",
                "-totalthroughput", "20", "-seconds", "10"));

        assertEquals(new BigDecimal("20"), parameters.getTotalThroughput());
        assertEquals(2, parameters.getWritersCount());
        assertEquals(2, parameters.getReadersCount());
    }

    @Test
    void exposesAggregateThroughputOptionInHelp() throws IOException {
        assertTrue(parameters().getHelpText().contains("-totalthroughput"));
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
        config.javadir = "";
        config.runtimecleanup = true;
        config.timeoutSeconds = 5;
        config.remoteDir = "sbk-gem-test";
        return config;
    }

    private static String[] arguments(String nodes, String... benchmarkArguments) {
        final String[] arguments = new String[benchmarkArguments.length + 4];
        arguments[0] = "-nodes";
        arguments[1] = nodes;
        arguments[2] = "-size";
        arguments[3] = "4096";
        System.arraycopy(benchmarkArguments, 0, arguments, 4, benchmarkArguments.length);
        return arguments;
    }
}

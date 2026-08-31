/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.MinIO;

import io.sbk.params.InputParameterOptions;
import io.sbk.params.impl.SbkDriversParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validates MinIO operation and asynchronous command-line options.
 */
public class MinIOOptionsTest {
    private static final String[] DRIVERS = {"MinIO"};
    private static final String[] LOGGERS = {};

    @Test
    public void allWriterOperationsParse() {
        String[] operations = {
            "put", "update", "copy", "delete", "tag-set", "tag-delete", "bucket-create"
        };
        for (String operation : operations) {
            assertDoesNotThrow(() -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                    "-write-operation", operation));
        }
        assertDoesNotThrow(() -> parse("-writers", "1", "-size", "1", "-seconds", "1",
                "-write-operation", "bucket-delete", "-bucket-targets", "one,two"));
    }

    @Test
    public void allReaderOperationsParse() {
        String[] operations = {
            "get", "range-get", "stat", "tag-get", "list", "bucket-stat", "bucket-list"
        };
        for (String operation : operations) {
            assertDoesNotThrow(() -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                    "-read-operation", operation));
        }
    }

    @Test
    public void weightedMixesAndVerificationOptionsParse() {
        assertDoesNotThrow(() -> parse("-writers", "1", "-readers", "1",
                "-size", "100", "-seconds", "1",
                "-write-mix", "put=80,copy=20",
                "-read-mix", "get=90,stat=10",
                "-data-seed", "42", "-verify-read-size", "true"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-write-mix", "put=0"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-write-mix", "get=100"));
    }

    @Test
    public void asyncAndOperationValidationRejectsUnsafeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-async", "true", "-async-depth", "0"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-async", "true", "-async-depth", "1025"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-write-operation", "bucket-delete"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-read-operation", "delete"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "1048576", "-seconds", "1",
                        "-async", "true", "-async-depth", "8",
                        "-async-max-memory-mb", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-catalog-max-objects", "0"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-retry-max-attempts", "0"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-partition-count", "2", "-partition-index", "2"));
    }

    @Test
    public void endpointTransportDefaultsToPlainHttp() {
        assertEquals("http://node1:9000", MinIO.normalizeEndpoint("node1:9000"));
        assertEquals("http://node2:9000", MinIO.normalizeEndpoint(" http://node2:9000 "));
        assertEquals("https://node3:9443", MinIO.normalizeEndpoint("https://node3:9443"));
    }

    @Test
    public void urlAcceptsOneOrMoreOrderedUniqueEndpoints() {
        assertEquals(List.of("http://node1:9000"),
                MinIO.configuredEndpoints("node1:9000"));
        assertEquals(List.of("http://node1:9000", "https://node2:9443"),
                MinIO.configuredEndpoints(
                        "node1:9000, https://node2:9443, http://node1:9000"));
        assertDoesNotThrow(() -> parse("-writers", "4", "-size", "100", "-seconds", "1",
                "-url", "node1:9000,node2:9000"));
    }

    @Test
    public void urlRejectsAnEmptyEndpointList() {
        assertThrows(IllegalArgumentException.class,
                () -> MinIO.configuredEndpoints(" , "));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-url", " , "));
    }

    @Test
    public void environmentCredentialsOverrideConfiguredDefaults() {
        assertEquals("environment-access",
                MinIO.credentialDefault("environment-access", "configured-access"));
        assertEquals("configured-access",
                MinIO.credentialDefault("", "configured-access"));
        assertEquals("configured-access",
                MinIO.credentialDefault(null, "configured-access"));
    }

    private static void parse(String... args) throws Exception {
        InputParameterOptions parameters = new SbkDriversParameters(
                "SBK MinIO option test", DRIVERS, LOGGERS);
        MinIO driver = new MinIO();
        driver.addArgs(parameters);
        parameters.parseArgs(args);
        driver.parseArgs(parameters);
    }
}

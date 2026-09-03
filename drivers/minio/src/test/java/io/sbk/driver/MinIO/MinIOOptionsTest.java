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

import java.io.IOException;
import java.nio.file.Path;
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
        assertDoesNotThrow(() -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                "-object-size-distribution", "weighted:64=3,1024=1",
                "-key-distribution", "hashed", "-partition-count", "2",
                "-partition-index", "1", "-partition-by-prefix", "true",
                "-warmup-operation", "put-get", "-endpoint-metrics", "true",
                "-retry-max-attempts", "4", "-retry-backoff-ms", "10",
                "-retry-strategy", "exponential", "-retry-max-backoff-ms", "1000",
                "-retry-jitter", "true", "-endpoint-preflight", "all"));
        assertDoesNotThrow(() -> parse("-readers", "1", "-size", "4096", "-seconds", "1",
                "-read-operation", "range-get", "-range-offset", "1024",
                "-range-length", "1024", "-range-offset-distribution", "random",
                "-range-window-length", "8192", "-range-alignment", "4096",
                "-list-max-keys", "100", "-list-max-entries", "10000",
                "-list-api-version", "2", "-list-start-after", "marker",
                "-list-delimiter", "/", "-list-fetch-owner", "true",
                "-list-include-user-metadata", "true", "-mixed-read-source", "catalog"));
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
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "10485760", "-seconds", "1",
                        "-mpu-concurrent-parts", "2"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "10485760", "-seconds", "1",
                        "-part-size", "5242880", "-mpu-concurrent-parts", "2",
                        "-checksum", "sha256"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-async", "ture"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-range-length", "2147483648"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-extra-headers", "x-emc-namespace=ns,broken"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-extra-headers", "x-emc-namespace=one,x-emc-namespace=two"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-extra-headers", "X-EMC-Namespace=one,x-emc-namespace=two"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-write-mix", "put=1,tag-set=1", "-tagging-tags", "broken"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-write-mix", "put=1,put=2"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-range-offset-distribution", "zipf"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-list-api-version", "3"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-list-api-version", "1", "-list-fetch-owner", "true"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-endpoint-preflight", "some"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("-readers", "1", "-size", "100", "-seconds", "1",
                        "-retry-strategy", "linear"));
    }

    @Test
    public void mixedBucketOperationsValidateTheirOwnRequiredArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "1", "-seconds", "1",
                        "-write-operation", "put", "-write-mix", "bucket-delete=1"));
        assertDoesNotThrow(() -> parse("-writers", "1", "-size", "1", "-seconds", "1",
                "-write-operation", "put", "-write-mix", "bucket-delete=1",
                "-bucket-targets", "one,two"));
    }

    @Test
    public void endpointTransportDefaultsToPlainHttp() {
        assertEquals("http://node1:9000", MinIO.normalizeEndpoint("node1:9000"));
        assertEquals("http://node2:9000", MinIO.normalizeEndpoint(" http://node2:9000 "));
        assertEquals("https://node3:9443", MinIO.normalizeEndpoint("https://node3:9443"));
        assertEquals("http://[2001:db8::1]:9000", MinIO.normalizeEndpoint("[2001:db8::1]:9000"));
        assertThrows(IllegalArgumentException.class,
                () -> MinIO.normalizeEndpoint("ftp://node1:9000"));
        assertThrows(IllegalArgumentException.class,
                () -> MinIO.normalizeEndpoint("http://user:secret@node1:9000"));
        assertThrows(IllegalArgumentException.class,
                () -> MinIO.normalizeEndpoint("http://node1:9000/s3"));
        assertThrows(IllegalArgumentException.class,
                () -> MinIO.normalizeEndpoint("http://node1:9000?test=true"));
    }

    @Test
    public void rejectsUnsupportedSignatureVersionInsteadOfFallingBack() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("-writers", "1", "-size", "100", "-seconds", "1",
                        "-auth-version", "2"));
    }

    @Test
    public void asyncMemoryEstimateIncludesReusablePayloadsAndResponseBuffers() {
        assertEquals(20L * 1024 * 1024,
                MinIO.estimateBufferBytes(2, 0, 10 * 1024 * 1024, 2, 2, true));
        assertEquals(4L * 64 * 1024,
                MinIO.estimateBufferBytes(0, 2, 1024, 2, 4, true));
        assertEquals(20L * 1024 * 1024,
                MinIO.estimateBufferBytes(1, 0, 20 * 1024 * 1024, 1, 1, true));
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

    @Test
    public void objectManifestEntriesAreStrictAndRetainOptionalVersions() throws Exception {
        Path path = Path.of("objects.csv");
        assertEquals(new S3ObjectRef("prefix/object", "v1", 1024, 0),
                MinIO.parseManifestEntry(path, "prefix/object,1024,v1", 1));
        assertEquals(new S3ObjectRef("prefix/object", null, 0, 0),
                MinIO.parseManifestEntry(path, "prefix/object,0", 2));
        assertThrows(IOException.class,
                () -> MinIO.parseManifestEntry(path, "missing-size", 3));
        assertThrows(IOException.class,
                () -> MinIO.parseManifestEntry(path, "object,-1", 4));
        assertThrows(IOException.class,
                () -> MinIO.parseManifestEntry(path, "object,not-a-number", 5));
        assertThrows(IOException.class,
                () -> MinIO.parseManifestEntry(path, "key,with,comma,1", 6));
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

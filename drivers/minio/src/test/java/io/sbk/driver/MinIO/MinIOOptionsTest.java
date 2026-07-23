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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

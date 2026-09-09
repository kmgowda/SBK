/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import io.sbk.action.Action;
import io.sbk.params.impl.SbkParameters;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that CSV output is independent of the process default locale.
 */
final class CSVLoggerLocaleTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    @ResourceLock("default-locale")
    void commaDecimalLocaleDoesNotCreateExtraCsvColumns() throws Exception {
        final Locale originalLocale = Locale.getDefault();
        try {
            final String rootRow = writeCsvRow(Locale.ROOT, "root.csv");
            final String germanRow = writeCsvRow(Locale.GERMANY, "german.csv");

            assertEquals(rootRow.split(",", -1).length,
                    germanRow.split(",", -1).length);
            assertTrue(germanRow.contains("1.25"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private String writeCsvRow(Locale locale, String fileName) throws Exception {
        Locale.setDefault(locale);
        final Path csv = temporaryDirectory.resolve(fileName);
        final CSVLogger logger = new CSVLogger();
        final SbkParameters params = new SbkParameters("csv-locale-test");
        logger.addArgs(params);
        params.parseArgs(new String[]{"-writers", "1", "-size", "10", "-records", "1",
                "-csvfile", csv.toString(), "-time", "ns"});
        logger.parseArgs(params);
        logger.open(params, "File", Action.Writing, new NanoSeconds());
        final long[] percentileValues = new long[logger.getPercentiles().length];
        logger.writeToCSV("SBK", "Total", 0, 0, 0,
                1, 1, 0, 0,
                1, 1.25, 1, 2.5,
                0, 3.75, 0, 4.5,
                0, 0, 0, 0, 0, 0,
                0, 5.25, 0, 6.5,
                1.5, 1, 1, 7.25, 8.5,
                9.75, 1, 1, 0, 0, 0, 0, 0,
                percentileValues, percentileValues);
        logger.close(params);

        final List<String> lines = Files.readAllLines(csv);
        assertEquals(2, lines.size());
        return lines.get(1);
    }
}

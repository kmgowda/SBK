/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.BookKeeper;

import io.sbk.params.InputParameterOptions;
import io.sbk.params.impl.SbkDriversParameters;
import io.sbk.config.Config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

//create the test Class for Bookkeeper Benchmarking
public class BookKeeperTest {
    /**
     * .
     * * Initializing variable
     */
    private final static String CONFIGFILE = "BookKeeper.properties";
    final String[] drivers = {"BookKeeper"};
    final String[] loggers = {};
    final String benchmarkName = Config.NAME + " -class bookkeeper";
    private InputParameterOptions params;
    private BookKeeper bk;

    @Test
    public void addArgsTest() {
        params = new SbkDriversParameters(benchmarkName, drivers, loggers);
        bk = new BookKeeper();
        bk.addArgs(params);

    }

    /**
     * Test code for parseArgs.
     */
    @Test
    public void parseArgs() {
        params = new SbkDriversParameters(benchmarkName, drivers, loggers);
        bk = new BookKeeper();
        bk.addArgs(params);
        Exception exception = null;
        try {
            bk.parseArgs(params);
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);

    }

    @Test
    public void testParseArgsNullLogName() {
        final String[] args = {"-class", "bookkeeper", "-uri", "distributedlog://localhost:2181/streams", "-writers", "1", "-size", "100"};
        params = new SbkDriversParameters(benchmarkName, drivers, loggers);
        bk = new BookKeeper();
        bk.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("ParseArgs Failed!");
        }
        assertThrows(IllegalArgumentException.class, () -> {
            bk.parseArgs(params);
        });
    }

    @Test
    public void testParseArgsNullUri() {
        final String[] args = {"-class", "bookkeeper", "-log", "logName", "writers", "1", "size", "100"};
        params = new SbkDriversParameters(benchmarkName, drivers, loggers);
        bk = new BookKeeper();
        bk.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            //fail("ParseArgs Failed!");
        }
        assertThrows(IllegalArgumentException.class, () -> {
            bk.parseArgs(params);
        });
    }
}

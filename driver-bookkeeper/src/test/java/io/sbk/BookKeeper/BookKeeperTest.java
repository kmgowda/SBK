/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.BookKeeper;

import io.sbk.api.ParameterOptions;
import io.sbk.api.impl.SbkDriversParameters;
import io.sbk.config.Config;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

//create the test Class for Bookkeeper Benchmarking
public class BookKeeperTest {
    /**.
     *  * Initializing variable        */
    private final static String CONFIGFILE = "BookKeeper.properties";
    final String[] drivers = {"BookKeeper"};
    final String benchmarkName = Config.NAME + " -class bookkeeper";
    private ParameterOptions params;
    private BookKeeper bk;

    @Test
    public void addArgsTest() {
        params = new SbkDriversParameters(benchmarkName, drivers);
        params.addOption("log", true, "Log name");
        params.addOption("uri", true, "URI");
        params.addOption("ensembleSize", true,
                "EnsembleSize(default value is in " + CONFIGFILE + ")");
        params.addOption("writeQuorum", true,
                "WriteQuorum(default value is in " + CONFIGFILE + " )");
        params.addOption("ackQuorum", true,
                "AckQuorum(default value is in " + CONFIGFILE + " )");
        params.addOption("recreate", true,
                "If the log is already existing, delete and recreate the same");
        bk = new BookKeeper();
        bk.addArgs(params);

    }

    /**
     Test code for parseArgs.
     */
    @Test
    public void parseArgs() {
        params = new SbkDriversParameters(benchmarkName, drivers);
        params.addOption("log", true, "Log name");
        params.addOption("uri", true, "URI");
        params.addOption("ensembleSize", true,
                "EnsembleSize (default value is in " + CONFIGFILE + " )");
        params.addOption("writeQuorum", true,
                "WriteQuorum  (default value is in " + CONFIGFILE + " )");
        params.addOption("ackQuorum", true,
                "AckQuorum (default value is in " + CONFIGFILE + " )");
        params.addOption("recreate", true,
                "If the log is already exist, delete and recreate the same)");
        bk = new BookKeeper();

        Exception exception = null;
        try {
            bk.parseArgs(params);
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseArgsNullLogName() {
        final String[] args = {"-class", "bookkeeper", "-uri", "distributedlog://localhost:2181/streams", "-writers", "1", "-size", "100"};
        params = new SbkDriversParameters(benchmarkName, drivers);
        bk = new BookKeeper();
        bk.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("ParseArgs Failed!");
        }
        bk.parseArgs(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseArgsNullUri() {
        final String[] args = {"-class", "bookkeeper", "-log", "logName", "writers", "1", "size", "100"};
        params = new SbkDriversParameters(benchmarkName, drivers);
        bk = new BookKeeper();
        bk.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            //Assert.fail("ParseArgs Failed!");
        }
        bk.parseArgs(params);
    }
}

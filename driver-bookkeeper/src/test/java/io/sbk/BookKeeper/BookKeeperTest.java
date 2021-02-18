/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.BookKeeper;


import io.sbk.api.Parameters;
import io.sbk.api.impl.SbkParameters;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

//create the test Class for Bookkeeper Benchmarking
public class BookKeeperTest {
    /**.
     *  * Initializing variable        */
    private final static String CONFIGFILE = "BookKeeper.properties";
    // test list of driver strings
    /** Adding Driver List. */
    private  List<String> driverList =  new ArrayList<>();
    private String benchmarkName = "BookkeeperBench";

    /** BookKeeperTest Method.
     */
    public BookKeeperTest() {

    }

    @Test
    public void addArgsTest() throws Exception {
        driverList.add("bookkeeper");
        Parameters params = new SbkParameters(benchmarkName, driverList);
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
        BookKeeper bk = new BookKeeper();
        bk.addArgs(params);

    }

    /**
    Test code for parseArgs.
     */
    @Test
    public void parseArgs() throws Exception {
        List<String> listStrings = new ArrayList<String>();
        driverList.add("bookkeeper");
        Parameters params = new SbkParameters(benchmarkName, driverList);
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
        BookKeeper bk = new BookKeeper();
        // doThrow(IllegalArgumentException.class).when(params).parseArgs(any());
        Exception exception = null;
        try {
            bk.parseArgs(params);
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);

    }
}

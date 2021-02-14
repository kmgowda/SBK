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

import com.google.common.collect.Lists;
import io.sbk.api.Parameters;
import io.sbk.api.impl.SbkParameters;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.Assert.assertNotNull;

//create the test Class for Bookkeeper Benchmarking
public class BookKeeperTest {
    /**.
     *  * Initializing variable        */
    private final static String CONFIGFILE = "BookKeeper.properties";
    private long startTime = System.currentTimeMillis();
    // test list of driver strings
    /** Adding Driver List. */
    private final List<String> driverList;

    {
        driverList = Lists.newArrayList("bookkeeper", "rabbitmq", "hdfs");
    }
    /**
     *  * Initialization version benchmarkName className. */
    private String version = "0.7";
    private String benchmarkName = "BookkeeperBench";
    private String className = "BookKeeperClass";
    private String[] testCmdArgs = new String[]{
                "-log", "abcd", "-uri", "testuri", "-ensembleSize", "1",
                "-writeQuorum", "1", "-ackQuorum", "1",
                "-recreate", "True", "-writers", "1", "-readers", "1", "-size", "8"};

    // test_args holds test keys and values so that later
    // we can verify on params

    private org.apache.distributedlog.api.namespace.Namespace namespace;
    private java.net.URI dlogUri;
    private org.apache.distributedlog.DistributedLogConfiguration conf;
    private org.apache.distributedlog.api.DistributedLogManager dlm;
    private String logName;
    private java.net.URI uriName;
    private boolean recreate;
    private int ensembleSize;
    private int writeQuorum;
    private int ackQuorum;

    /** BookKeeperTest Method.
     */
    public BookKeeperTest() {

    }

    @Test
    public void addArgsTest() throws Exception {
        Hashtable<String, String> testArgs = new Hashtable<String, String>();
        List<String> listStrings = new ArrayList<String>();
        driverList.add("bookkeeper bench");
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
        driverList.add("bookkeeper bench");
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

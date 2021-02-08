
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
import com.beust.jcommander.internal.Lists;
import io.sbk.api.impl.SbkParameters;
import io.sbk.api.Parameters;
import org.junit.Test;
import java.util.Hashtable;
import java.util.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

//create the test Class for Bookkeeper Benchmarking
public class BookKeeperTest {
    long startTime = System.currentTimeMillis();
    // test list of driver strings
    final List<String> driverList = Lists.newArrayList("bookkeeper", "rabbitmq", "hdfs");
    String version = "0.7";
    String benchmarkName = "BookkeeperBench";
    String className = "BookKeeperClass";
    String[] test_cmd_args = {
            "-log","abcd","-uri","testuri","-ensembleSize", "1",
            "-writeQuorum", "1", "-ackQuorum", "1",
            "-recreate","True","-writers", "1", "-readers", "1","-size","8"};    

    // test_args holds test keys and values so that later
    // we can verify on params
    private final static String CONFIGFILE = "BookKeeper.properties";
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

    @Test
    public void addArgsTest() throws Exception {
        Hashtable<String, String> test_args = new Hashtable<String, String>();
        List<String> listStrings = new ArrayList<String>();
        driverList.add("bookkeeper bench");
        Parameters params = new SbkParameters(benchmarkName,driverList);
        params.addOption("log", true, "Log name");
        params.addOption("uri", true, "URI");
        params.addOption("ensembleSize", true, "EnsembleSize (default value is in "+CONFIGFILE+" )");
        params.addOption("writeQuorum", true, "WriteQuorum  (default value is in "+CONFIGFILE+" )");
        params.addOption("ackQuorum", true, "AckQuorum (default value is in "+CONFIGFILE+" )");
        params.addOption("recreate", true,
                "If the log is already existing, delete and recreate the same (default: false)");
        BookKeeper bk = new BookKeeper();
        bk.addArgs(params);

    }
    @Test
    public void parseArgs() throws Exception {
        List<String> listStrings = new ArrayList<String>();
        driverList.add("bookkeeper bench");
        Parameters params = new SbkParameters(benchmarkName,driverList);
        params.addOption("log", true, "Log name");
        params.addOption("uri", true, "URI");
        params.addOption("ensembleSize", true, "EnsembleSize (default value is in "+CONFIGFILE+" )");
        params.addOption("writeQuorum", true, "WriteQuorum  (default value is in "+CONFIGFILE+" )");
        params.addOption("ackQuorum", true, "AckQuorum (default value is in "+CONFIGFILE+" )");
        params.addOption("recreate", true,
                "If the log is already existing, delete and recreate the same (default: false)");
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
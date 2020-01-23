/**
 * Copyright (c) 2020 KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.dsb.api;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

/**
 * class for Basic parameters.
 */
final public class Parameters {
    static final int MAXTIME = 60 * 60 * 24;
    static final int TIMEOUT = 1000;

    final public long startTime;
    final public int timeout;

    public int records;
    public int recordSize;
    public int recordsPerSec;
    public int recordsPerWriter;
    public int recordsPerReader;
    public int recordsPerFlush;
    public int secondsToRun;
    public int writersCount;
    public int readersCount;
    public double throughput;
    public String csvFile;
    public boolean writeAndRead;
    public boolean fork;

    final private String benchmarkName;
    final private Options options;
    final private HelpFormatter formatter;
    final private CommandLineParser parser;
    private CommandLine commandline;

    public Parameters(String name, long startTime) {
        options = new Options();
        formatter = new HelpFormatter();
        parser = new DefaultParser();
        benchmarkName = name;
        commandline = null;
        this.timeout = TIMEOUT;
        this.startTime = startTime;
        this.fork = true;

        options.addOption("class", true, "Benchmark class (refer to driver-* folder)");
        options.addOption("writers", true, "Number of writers");
        options.addOption("readers", true, "Number of readers");
        options.addOption("records", true,
                "Number of records(events) if 'time' not specified;\n" +
                        "otherwise, Maximum records per second by writer(s) " +
                        "and/or Number of records per reader");
        options.addOption("flush", true,
                "Each Writer calls flush after writing <arg> number of of events(records); " +
                        "Not applicable, if both writers and readers are specified");
        options.addOption("time", true, "Number of seconds the DSB runs (24hrs by default)");
        options.addOption("size", true, "Size of each message (event or record)");
        options.addOption("throughput", true,
                "if > 0 , throughput in MB/s\n" +
                        "if 0 , writes 'events'\n" +
                        "if -1, get the maximum throughput");
        options.addOption("csv", true, "CSV file to record write/read latencies");
        options.addOption("help", false, "Help message");
    }

    public Options addOption(String name, boolean hasarg, String description) {
        return options.addOption(name, hasarg, description);
    }

    public Options addOption(String name, String description) {
        return options.addOption(name, description);
    }

    public void printHelp() {
        formatter.printHelp(benchmarkName, options);
    }

    public boolean hasOption(String name) {
        if (commandline != null) {
            return commandline.hasOption(name);
        } else {
            return false;
        }
    }

    public String getOptionValue(String name) {
        if (commandline != null) {
            return commandline.getOptionValue(name);
        } else {
            return null;
        }
    }

    public String getOptionValue(String name, String defaultValue) {
        if (commandline != null) {
            return commandline.getOptionValue(name, defaultValue);
        } else {
            return defaultValue;
        }
    }

    public void parseArgs(String[] args) throws ParseException {
        commandline = parser.parse(options, args);
        if (commandline.hasOption("help")) {
            printHelp();
            return;
        }
        writersCount = Integer.parseInt(commandline.getOptionValue("writers", "0"));
        readersCount = Integer.parseInt(commandline.getOptionValue("readers", "0"));

        if (writersCount == 0 && readersCount == 0) {
            throw new IllegalArgumentException("Error: Must specify the number of writers or readers");
        }

        records = Integer.parseInt(commandline.getOptionValue("records", "0"));
        recordSize = Integer.parseInt(commandline.getOptionValue("size", "0"));
        csvFile = commandline.getOptionValue("csv", null);
        int flushRecords = Integer.parseInt(commandline.getOptionValue("flush", "0"));
        if (flushRecords > 0) {
            recordsPerFlush = flushRecords;
        } else {
            recordsPerFlush = Integer.MAX_VALUE;
        }

        if (commandline.hasOption("time")) {
            secondsToRun = Integer.parseInt(commandline.getOptionValue("time"));
        } else if (records > 0) {
            secondsToRun = 0;
        } else {
            secondsToRun = MAXTIME;
        }

        if (commandline.hasOption("throughput")) {
            throughput = Double.parseDouble(commandline.getOptionValue("throughput"));
        } else {
            throughput = -1;
        }

        if (writersCount > 0) {
            if (recordSize == 0) {
                throw new IllegalArgumentException("Error: Must specify the record 'size'");
            }
            writeAndRead = readersCount > 0;
            recordsPerWriter = (records + writersCount - 1) / writersCount;
            if (throughput < 0 && secondsToRun > 0) {
                recordsPerSec = readersCount / writersCount;
            } else if (throughput > 0) {
                recordsPerSec = (int) (((throughput * 1024 * 1024) / recordSize) / writersCount);
            } else {
                recordsPerSec = 0;
            }
        } else {
            recordsPerWriter = 0;
            recordsPerSec = 0;
            writeAndRead = false;
        }

        if (readersCount > 0) {
            recordsPerReader = records / readersCount;
        } else {
            recordsPerReader = 0;
        }
    }
}

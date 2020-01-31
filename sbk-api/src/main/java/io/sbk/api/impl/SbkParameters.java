/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import io.sbk.api.Parameters;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import java.util.List;

/**
 * class for Command Line Parameters.
 */
@Slf4j
final public class SbkParameters implements Parameters {
    static final int MAXTIME = 60 * 60 * 24;
    static final int TIMEOUT = 1000;
    final private String benchmarkName;
    final private String className;
    final private String desc;
    final private String version;
    final private Options options;
    final private HelpFormatter formatter;
    final private CommandLineParser parser;
    final private List<String> driversList;

    @Getter
    final private long startTime;
    @Getter
    final private int timeout;
    @Getter
    private int recordsCount;
    @Getter
    private int recordSize;
    @Getter
    private int recordsPerSec;
    @Getter
    private int recordsPerWriter;
    @Getter
    private int recordsPerReader;
    @Getter
    private int recordsPerFlush;
    @Getter
    private int secondsToRun;
    @Getter
    private int writersCount;
    @Getter
    private int readersCount;
    @Getter
    private String csvFile;
    @Getter
    private boolean writeAndRead;
    private double throughput;
    private CommandLine commandline;

    public SbkParameters(String name, String desc, String version, String className, List<String> driversList, long startTime) {
        this.options = new Options();
        this.formatter = new HelpFormatter();
        this.parser = new DefaultParser();
        this.benchmarkName = name;
        this.desc = desc;
        this.className = className;
        this.version = version;
        this.timeout = TIMEOUT;
        this.driversList = driversList;
        this.startTime = startTime;
        this.commandline = null;
        options.addOption("class", true, "Benchmark Driver Class,\n Available Drivers "
                            + this.driversList.toString());
        options.addOption("writers", true, "Number of writers");
        options.addOption("readers", true, "Number of readers");
        options.addOption("records", true,
                "Number of records(events) if 'time' not specified;\n" +
                        "otherwise, Maximum records per second by writer(s) " +
                        "and/or Number of records per reader");
        options.addOption("flush", true,
                "Each Writer calls flush after writing <arg> number of of events(records); " +
                        "Not applicable, if both writers and readers are specified");
        options.addOption("time", true, "Number of seconds this SBK runs (24hrs by default)");
        options.addOption("size", true, "Size of each message (event or record)");
        options.addOption("throughput", true,
                "if > 0 , throughput in MB/s\n" +
                        "if 0 , writes 'events'\n" +
                        "if -1, get the maximum throughput");
        options.addOption("csv", true, "CSV file to record write/read latencies");
        options.addOption("help", false, "Help message");
        options.addOption("version", false, "Version");
    }

    @Override
    public Options addOption(String name, boolean hasArg, String description) {
        return options.addOption(name, hasArg, description);
    }

    @Override
    public Options addOption(String name, String description) {
        return options.addOption(name, description);
    }

    @Override
    public void printHelp() {
        if (className.length() > 0) {
            formatter.printHelp(benchmarkName + " -class " + className, options);
        } else {
            formatter.printHelp(benchmarkName, options);
        }
    }

    @Override
    public void printVersion() {
        System.out.println(desc + ", " + benchmarkName +" version: " + version);
    }


    @Override
    public boolean hasOption(String name) {
        if (commandline != null) {
            return commandline.hasOption(name);
        } else {
            return false;
        }
    }

    @Override
    public String getOptionValue(String name) {
        if (commandline != null) {
            return commandline.getOptionValue(name);
        } else {
            return null;
        }
    }

    @Override
    public String getOptionValue(String name, String defaultValue) {
        if (commandline != null) {
            return commandline.getOptionValue(name, defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException {
        commandline = parser.parse(options, args);
        if (commandline.hasOption("help")) {
            printHelp();
            return;
        }
        if (commandline.hasOption("version")) {
            printVersion();
            return;
        }

        writersCount = Integer.parseInt(commandline.getOptionValue("writers", "0"));
        readersCount = Integer.parseInt(commandline.getOptionValue("readers", "0"));

        if (writersCount == 0 && readersCount == 0) {
            throw new IllegalArgumentException("Error: Must specify the number of writers or readers");
        }

        recordsCount = Integer.parseInt(commandline.getOptionValue("records", "0"));
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
        } else if (recordsCount > 0) {
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
            recordsPerWriter = (recordsCount + writersCount - 1) / writersCount;
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
            recordsPerReader = recordsCount / readersCount;
        } else {
            recordsPerReader = 0;
        }
    }
}

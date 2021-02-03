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

import io.sbk.api.Config;
import io.sbk.api.DataType;
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
 * Class for processing command Line arguments/parameters.
 */
@Slf4j
final public class SbkParameters implements Parameters {
    final private String benchmarkName;
    final private Options options;
    final private HelpFormatter formatter;
    final private CommandLineParser parser;
    final private List<String> driversList;

    @Getter
    final private int timeoutMS;
    @Getter
    private int recordSize;
    @Getter
    private int writersCount;
    @Getter
    private int readersCount;
    @Getter
    private int recordsPerSec;
    @Getter
    private int recordsPerSync;
    @Getter
    private long recordsCount;
    @Getter
    private long secondsToRun;
    @Getter
    private long recordsPerWriter;
    @Getter
    private long recordsPerReader;
    @Getter
    private String csvFile;
    @Getter
    private boolean writeAndRead;

    @Getter
    private int delta;

    @Getter
    private int intervalSeconds;

    private double throughput;
    private CommandLine commandline;

    public SbkParameters(String name, List<String> driversList) {
        this.options = new Options();
        this.formatter = new HelpFormatter();
        this.parser = new DefaultParser();
        this.benchmarkName = name;
        this.timeoutMS = Config.DEFAULT_TIMEOUT_MS;
        this.driversList = driversList;
        this.commandline = null;

        if (this.driversList != null) {
            options.addOption("class", true, "Storage Driver Class,\n Available Drivers "
                    + this.driversList.toString());
        }
        options.addOption("writers", true, "Number of writers");
        options.addOption("readers", true, "Number of readers");
        options.addOption("size", true, "Size of each message (event or record)");
        options.addOption("records", true,
                "Number of records(events) if 'seconds' not specified;\n" +
                        "otherwise, Maximum records per second by writer(s) " +
                        "and/or Number of records per reader");
        options.addOption("sync", true,
                "Each Writer calls flush/sync after writing <arg> number of of events(records)" +
                        " ; <arg> number of events(records) per Write or Read Transaction");
        options.addOption("seconds", true, "Number of seconds to run; if not specified, runs forever");
        options.addOption("throughput", true,
                "if > 0 , throughput in MB/s\n" +
                        "if 0 , writes/reads 'records'\n" +
                        "if -1, get the maximum throughput (default: -1)");
        options.addOption("delta", true,
                "delta/batch of writers/readers to increase");

        options.addOption("interval", true,
                "interval in seconds to increase writers/readers");
        options.addOption("help", false, "Help message");
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
        formatter.printHelp(benchmarkName, options);
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
        writersCount = Integer.parseInt(commandline.getOptionValue("writers", "0"));
        readersCount = Integer.parseInt(commandline.getOptionValue("readers", "0"));
        delta = Integer.parseInt(commandline.getOptionValue("delta", "1"));
        intervalSeconds = Integer.parseInt(commandline.getOptionValue("interval", "0"));

        if (writersCount == 0 && readersCount == 0) {
            throw new IllegalArgumentException("Error: Must specify the number of writers or readers");
        }

        recordsCount = Long.parseLong(commandline.getOptionValue("records", "0"));
        recordSize = Integer.parseInt(commandline.getOptionValue("size", "0"));
        csvFile = commandline.getOptionValue("csv", null);
        int syncRecords = Integer.parseInt(commandline.getOptionValue("sync", "0"));
        if (syncRecords > 0) {
            recordsPerSync = syncRecords;
        } else {
            recordsPerSync = Integer.MAX_VALUE;
        }

        if (commandline.hasOption("seconds")) {
            secondsToRun = Long.parseLong(commandline.getOptionValue("seconds"));
        } else if (recordsCount > 0) {
            secondsToRun = 0;
        } else {
            secondsToRun = Config.DEFAULT_RUNTIME_SECONDS;
        }

        if (commandline.hasOption("throughput")) {
            throughput = Double.parseDouble(commandline.getOptionValue("throughput"));
        } else {
            throughput = -1;
        }

        int workersCnt = writersCount;
        if (workersCnt == 0) {
            workersCnt = readersCount;
        }

        if (throughput < 0 && secondsToRun > 0) {
            long recsPerSec = recordsCount / workersCnt;
            if (recsPerSec > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Error: The Records per Second value :" +recsPerSec +"is more than "+Integer.MAX_VALUE);
            }
            recordsPerSec = (int) recsPerSec;
        } else if (throughput > 0) {
            recordsPerSec = (int) (((throughput * 1024 * 1024) / recordSize) / workersCnt);
        } else {
            recordsPerSec = 0;
        }

        if (writersCount > 0) {
            if (recordSize == 0) {
                throw new IllegalArgumentException("Error: Must specify the record 'size'");
            }

            if (readersCount > 0) {
                if (recordSize < DataType.TIME_HEADER_BYTES) {
                    throw new IllegalArgumentException("Error: In case of write and read, minimum data size should be " + DataType.TIME_HEADER_BYTES);
                }
            }
            writeAndRead = readersCount > 0;
            recordsPerWriter = recordsCount / writersCount;
        } else {
            recordsPerWriter = 0;
            writeAndRead = false;
        }

        if (readersCount > 0) {
            recordsPerReader = recordsCount / readersCount;
        } else {
            recordsPerReader = 0;
        }
    }
}

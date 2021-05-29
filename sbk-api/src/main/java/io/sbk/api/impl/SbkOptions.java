/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.sbk.api.InputOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class SbkOptions implements InputOptions {
    final private String benchmarkName;
    final private Options options;
    final private HelpFormatter formatter;
    final private CommandLineParser parser;
    private CommandLine commandline;

    public SbkOptions(String name) {
        this.options = new Options();
        this.formatter = new HelpFormatter();
        this.parser = new DefaultParser();
        this.benchmarkName = name;
        this.commandline = null;

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
        }
    }
}

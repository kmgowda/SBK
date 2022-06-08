/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.params.impl;

import io.sbk.config.Config;
import io.sbk.exception.HelpException;
import io.sbk.params.ParseInputOptions;
import io.sbk.system.Printer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


public class SbkInputOptions implements ParseInputOptions {
    final private String benchmarkName;
    final private String header;
    final private String footer;
    final private Options options;
    final private HelpFormatter formatter;
    final private CommandLineParser parser;
    final private List<String> namesList;
    final private boolean stopAtNonOption;
    private CommandLine commandline;

    private SbkInputOptions(String name, String header, String footer, boolean stopAtNonOption) {
        this.options = new Options();
        this.formatter = new HelpFormatter();
        this.parser = new DefaultParser();
        this.benchmarkName = name;
        this.header = header + "\n\n";
        this.footer = footer;
        this.namesList = new ArrayList<>();
        this.stopAtNonOption = stopAtNonOption;
        this.commandline = null;

        addOption(Config.HELP_OPTION, false, "Help message");
    }

    public SbkInputOptions(String name, String header) {
        this(name, header, Config.SBK_FOOTER, false);
    }

    @Override
    final public void addOption(String name, boolean hasArg, String description) throws IllegalArgumentException {
        if (hasOption(name)) {
            throw new IllegalArgumentException("The matching option: '" + name +"' already exists");
        }
        namesList.add(name);
        options.addOption(name, hasArg, description);
    }

    @Override
    final public boolean hasOption(String name) {
        for (String x : namesList) {
           if ( x.equalsIgnoreCase(name)) {
               return true;
           }
        }
        return false;
    }

    @Override
    final public String getHelpText() {
        final OutputStream outStream = new ByteArrayOutputStream();
        final PrintWriter helpPrinter = new PrintWriter(outStream);
        formatter.printHelp(helpPrinter, HelpFormatter.DEFAULT_WIDTH, benchmarkName, header, options,
                HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, footer);
        helpPrinter.flush();
        try {
            outStream.close();
        } catch (Exception ex) {
            Printer.log.error(ex.toString());
        }
        return outStream.toString();
    }


    @Override
    public boolean hasOptionValue(String name) {
        return commandline != null && commandline.hasOption(name);
    }

    @Override
    public String getOptionValue(String name) {
        return commandline != null ? commandline.getOptionValue(name) : null;
    }

    @Override
    public String getOptionValue(String name, String defaultValue) {
        return commandline != null ? commandline.getOptionValue(name, defaultValue) : defaultValue;
    }

    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        commandline = parser.parse(options, args, stopAtNonOption);
        if (commandline.hasOption(Config.HELP_OPTION)) {
            throw new HelpException(getHelpText());
        }
    }
}

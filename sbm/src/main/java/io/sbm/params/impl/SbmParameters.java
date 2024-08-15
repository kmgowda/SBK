/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.params.impl;

import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbm.config.SbmConfig;
import io.sbk.exception.HelpException;
import io.sbm.params.RamParameterOptions;
import io.sbk.params.impl.SbkInputOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;


/**
 * Class for processing command Line arguments/parameters.
 */
@Slf4j
final public class SbmParameters extends SbkInputOptions implements RamParameterOptions {

    @Getter
    private String storageName;

    @Getter
    private Action action;

    @Getter
    private int maxConnections;

    @Getter
    private int port;

    final private String[] loggerNames;
    /**
     * Constructor SbmParameters initializing all values.
     *
     * @param name           String
     * @param port           int
     * @param maxConnections int
     * @param loggerNames
     */
    public SbmParameters(String name, int port, int maxConnections, String[] loggerNames) {
        super(name, SbmConfig.DESC);
        this.maxConnections = maxConnections;
        this.port = port;
        if (loggerNames != null && loggerNames.length > 0) {
            this.loggerNames = loggerNames.clone();
        } else {
            this.loggerNames = new String[]{""};
        }
        addOption(Config.CLASS_OPTION, true, "storage class name; run 'sbk -help' to see the list");
        addOption(Config.LOGGER_OPTION, true, "logger driver class,\n Available Drivers "
                + Arrays.toString(this.loggerNames));
        addOption("action", true,
                """
                            action [r: read, w: write,
                            wr: write and read, wro: write but only read,
                            rw: read and write, rwo: read but only write],
                            default: r""");
        addOption("port", true, "SBM port number; default: " + this.port);
        addOption("max", true, "Maximum number of connections; default: " + maxConnections);
    }


    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        super.parseArgs(args);

        final String name = getOptionValue("class", null);
        if (name == null) {
            throw new UnrecognizedOptionException("storage 'class' name is NOT supplied! ");
        }
        storageName = StringUtils.capitalize(name);

        String actionString = getOptionValue("action", "r");
        action = switch (actionString.toLowerCase()) {
            case "wro" -> Action.Write_OnlyReading;
            case "wr" -> Action.Write_Reading;
            case "w" -> Action.Writing;
            case "rwo" -> Action.Read_OnlyWriting;
            case "rw" -> Action.Read_Writing;
            default -> Action.Reading;
        };
        maxConnections = Integer.parseInt(getOptionValue("max", Integer.toString(maxConnections)));
        port = Integer.parseInt(getOptionValue("port", Integer.toString(port)));
    }

}

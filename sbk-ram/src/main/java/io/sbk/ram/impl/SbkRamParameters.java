/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.ram.impl;

import io.sbk.api.Action;
import io.sbk.ram.RamConfig;
import io.sbk.ram.RamParameterOptions;
import io.sbk.api.impl.SbkOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;


/**
 * Class for processing command Line arguments/parameters.
 */
@Slf4j
final public class SbkRamParameters extends SbkOptions implements RamParameterOptions {

    @Getter
    private String storageName;

    @Getter
    private Action action;

    @Getter
    private int maxConnections;

    @Getter
    private  int ramPort;

    public SbkRamParameters(String name, int port, int maxConnections) {
        super(name, RamConfig.DESC);
        this.maxConnections = maxConnections;
        this.ramPort = port;
        addOption("class", true, "storage class name; run 'sbk -help' to see the list");
        addOption("action", true, "action [r: read, w: write, wr: write and read], default: r");
        addOption("ramport", true, "RAM port number; default: "+ramPort);
        addOption("max", true, "Maximum number of connections; default: "+maxConnections);
    }


    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException {
        super.parseArgs(args);
        if (hasOption("help")) {
            return;
        }

        storageName = getOptionValue("class", null);

        if (storageName == null) {
            throw new UnrecognizedOptionException("storage 'class' name is NOT supplied! ");
        }

        String actionString = getOptionValue("action", "r");

        if (actionString.equalsIgnoreCase("wr")) {
            action = Action.Write_Reading;
        } else if (actionString.equalsIgnoreCase("w")) {
            action = Action.Writing;
        } else {
            action = Action.Reading;
        }

        maxConnections = Integer.parseInt(getOptionValue("max", Integer.toString(maxConnections)));
        ramPort = Integer.parseInt(getOptionValue("ramport", Integer.toString(ramPort)));
    }

}

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

import io.sbk.api.Action;
import io.sbk.api.ServerParameterOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;


/**
 * Class for processing command Line arguments/parameters.
 */
@Slf4j
final public class SbkServerParameters extends SbkOptions implements ServerParameterOptions {

    @Getter
    private String storageName;

    @Getter
    private Action action;

    @Getter
    private int maxConnections;

    public SbkServerParameters(String name, int maxConnections) {
        super(name);
        this.maxConnections = maxConnections;
        addOption("class", true, "storage class name; run 'sbk -help' to see the list");
        addOption("action", true, "action [r: read, w: write, wr: write and read]; default: r");
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
            throw new IllegalArgumentException("Storage class name should be supplied");
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

    }



}

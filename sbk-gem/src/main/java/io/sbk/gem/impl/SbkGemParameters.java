/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem.impl;

import io.sbk.api.impl.SbkParameters;
import io.sbk.gem.GemConfig;
import io.sbk.gem.GemParameters;
import io.sbk.perl.PerlConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;

import java.util.List;

@Slf4j
public class SbkGemParameters extends SbkParameters implements GemParameters {
    final private static int DEFAULT_TIMEOUT_MS = 5 * PerlConfig.MS_PER_SEC;
    final private GemConfig config;

    @Getter
    private String[] nodes;

    @Getter
    private String user;

    @Getter
    private String password;

    @Getter
    private int port;

    public SbkGemParameters(String name, List<String> driversList, GemConfig config) {
        super(name, driversList);
        this.config = config;
        addOption("nodes", true, "remote hostnames separated by `,` ; default: "+config.nodes);
        addOption("gem-user", true, "ssh user name of the remote hosts" + config.user);
        addOption("gem-pass", true, "ssh user password of the remote hosts" + config.password);
        addOption("gem-port", true, "ssh port of the remote hosts" + config.port);

    }

    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException {
        super.parseArgs(args);
        if (hasOption("help")) {
            return;
        }

        final String nodeString = getOptionValue("nodes", config.nodes);
        nodes = nodeString.split(",");
        user = getOptionValue("gem-user", config.user);
        password = getOptionValue("gem-pass", config.password);
        port = Integer.parseInt(getOptionValue("gem-port", Integer.toString(config.port)));
    }

    @Override
    public int getTimeoutMS() {
        return DEFAULT_TIMEOUT_MS;
    }

}

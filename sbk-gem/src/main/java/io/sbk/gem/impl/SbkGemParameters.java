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
import io.sbk.gem.GemParameterOptions;
import io.sbk.gem.SshConnection;
import io.sbk.perl.PerlConfig;
import io.sbk.system.Printer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class SbkGemParameters extends SbkParameters implements GemParameterOptions {
    final static String BIN_EXT_PATH = "bin";

    final private GemConfig config;

    @Getter
    final private int timeoutMS;

    @Getter
    private String user;

    @Getter
    private String password;

    @Getter
    private int port;

    @Getter
    private SshConnection[] connections;

    @Getter
    private String sbkDir;

    @Getter
    private String sbkCommand;


    public SbkGemParameters(String name, List<String> driversList, GemConfig config) {
        super(name, driversList);
        this.config = config;
        this.timeoutMS = config.timeoutSeconds * PerlConfig.MS_PER_SEC;
        addOption("nodes", true, "remote hostnames separated by `,` , default: "+config.nodes);
        addOption("gemuser", true, "ssh user name of the remote hosts, default: " + config.user);
        addOption("gempass", true, "ssh user password of the remote hosts, default: " + config.password);
        addOption("gemport", true, "ssh port of the remote hosts, default: " + config.port);
        addOption("sbkdir", true, "directory path of sbk application, default: " + config.sbkPath);
        addOption("sbkcommand", true, "sbk command, default: " + config.sbkCommand);

    }

    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException {
        super.parseArgs(args);
        if (hasOption("help")) {
            return;
        }

        final String nodeString = getOptionValue("nodes", config.nodes);
        String[] nodes = nodeString.split(",");
        user = getOptionValue("gem-user", config.user);
        password = getOptionValue("gem-pass", config.password);
        port = Integer.parseInt(getOptionValue("gem-port", Integer.toString(config.port)));
        sbkDir = getOptionValue("sbkdir", config.sbkPath);
        final String command = getOptionValue("sbkcommand", config.sbkCommand);
        connections = new SshConnection[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            connections[i] = new SshConnection(nodes[i], user, password, port);
        }

        if (!Files.isDirectory(Paths.get(sbkDir))) {
            String errMsg = "The SBK application directory: "+sbkDir +" not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        sbkCommand = sbkDir +"/"+BIN_EXT_PATH+"/"+command;
        Path sbkCommandPath = Paths.get(sbkCommand);

        if (!Files.exists(sbkCommandPath)) {
            String errMsg = "The sbk executable command: "+sbkCommand+" not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isExecutable(sbkCommandPath)) {
            String errMsg = "The executable permissions are not found for command: "+sbkCommand;
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

    }

}

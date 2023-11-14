/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.params.impl;

import io.gem.config.GemConfig;
import io.gem.api.ConnectionConfig;
import io.gem.params.GemParameterOptions;
import io.sbk.exception.HelpException;
import io.sbk.params.impl.SbkDriversParameters;
import io.sbk.system.Printer;
import io.time.Time;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class SbkGemParameters.
 */
@Slf4j
public final class SbkGemParameters extends SbkDriversParameters implements GemParameterOptions {

    final private GemConfig config;

    @Getter
    final private int timeoutMS;

    @Getter
    final private String[] optionsArgs;

    @Getter
    private String[] parsedArgs;

    @Getter
    private ConnectionConfig[] connections;

    @Getter
    private String localHost;

    @Getter
    private int sbmPort;

    /**
     * This Constructor is responsible for initializing all values.
     *
     * @param name    String
     * @param drivers String[]
     * @param loggers
     * @param config  NotNull GemConfig
     * @param sbmPort int
     */
    public SbkGemParameters(String name, String[] drivers, String[] loggers, @NotNull GemConfig config, int sbmPort) {
        super(name, GemConfig.DESC, drivers, loggers);
        this.config = config;
        this.timeoutMS = config.timeoutSeconds * Time.MS_PER_SEC;
        this.sbmPort = sbmPort;
        try {
            this.localHost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Printer.log.error(ex.toString());
            this.localHost = GemConfig.LOCAL_HOST;
        }
        addOption("nodes", true, """
                remote hostnames separated by ',';
                default:""" + config.nodes);
        addOption("gemuser", true, "ssh user name of the remote hosts, default: " + config.gemuser);
        addOption("gempass", true, "ssh user password of the remote hosts, default: " + config.gempass);
        addOption("gemport", true, "ssh port of the remote hosts, default: " + config.gemport);
        addOption("sbkdir", true, "directory path of sbk application, default: " + config.sbkdir);
        addOption("sbkcommand", true,
                "remote sbk command; command path is relative to 'sbkdir', default: " + config.sbkcommand);
        addOption("copy", true, "Copy the SBK package to remote hosts; default: " + config.copy);
        addOption("delete", true, "Delete SBK package after benchmark; default: " + config.delete);
        addOption("localhost", true, "this local SBM host name, default: " + localHost);
        addOption("sbmPort", true, "SBM port number; default: " + this.sbmPort);
        this.optionsArgs = new String[]{"-nodes", "-gemuser", "-gempass", "-gemport", "-sbkdir", "-sbkcommand",
                "-copy", "-delete", "-localhost", "-sbmPort"};
        this.parsedArgs = null;
    }


    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        super.parseArgs(args);
        final String nodeString = getOptionValue("nodes", config.nodes);
        String[] nodes = nodeString.replace("[ ]+", " ")
                .replace("[,]+", ",")
                .split("[ ,\n]+");
        config.gemuser = getOptionValue("gemuser", config.gemuser);
        config.gempass = getOptionValue("gempass", config.gempass);
        config.gemport = Integer.parseInt(getOptionValue("gemport", Integer.toString(config.gemport)));
        config.sbkdir = getOptionValue("sbkdir", config.sbkdir);
        config.sbkcommand = getOptionValue("sbkcommand", config.sbkcommand);
        localHost = getOptionValue("localhost", localHost);
        sbmPort = Integer.parseInt(getOptionValue("ramport", Integer.toString(sbmPort)));
        config.copy = Boolean.parseBoolean(getOptionValue("copy", Boolean.toString(config.copy)));
        config.delete = Boolean.parseBoolean(getOptionValue("delete", Boolean.toString(config.delete)));

        parsedArgs = new String[]{"-nodes", nodeString, "-gemuser", config.sbkcommand, "-gempass",
                config.gempass, "-gemport", Integer.toString(config.gemport), "-sbkdir", config.sbkdir,
                "-sbkcommand", config.sbkcommand, "-copy", Boolean.toString(config.copy),
                "-delete", Boolean.toString(config.delete), "-localhost", localHost, "-ramport",
                Integer.toString(sbmPort)};

        connections = new ConnectionConfig[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            connections[i] = new ConnectionConfig(nodes[i], config.gemuser, config.gempass, config.gemport,
                    config.remoteDir);
        }

        if (StringUtils.isEmpty(config.sbkdir)) {
            String errMsg = "The SBK application directory not supplied!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isDirectory(Paths.get(config.sbkdir))) {
            String errMsg = "The SBK application directory: " + config.sbkdir + " not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (StringUtils.isEmpty(config.sbkcommand)) {
            String errMsg = "The SBK application/command not supplied!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        final String sbkFullCommand = config.sbkdir + File.separator + config.sbkcommand;
        Path sbkCommandPath = Paths.get(sbkFullCommand);

        if (!Files.exists(sbkCommandPath)) {
            String errMsg = "The sbk executable command: " + sbkFullCommand + " not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isExecutable(sbkCommandPath)) {
            String errMsg = "The executable permissions are not found for command: " + sbkFullCommand;
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

    }

    @Override
    public String getSbkDir() {
        return config.sbkdir;
    }

    @Override
    public String getSbkCommand() {
        return config.sbkcommand;
    }

    @Override
    public boolean isCopy() {
        return config.copy;
    }

    @Override
    public boolean isDelete() {
        return config.delete;
    }
}

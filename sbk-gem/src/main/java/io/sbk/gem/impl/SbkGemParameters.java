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

import io.sbk.api.HelpException;
import io.sbk.api.impl.SbkParameters;
import io.sbk.gem.GemConfig;
import io.sbk.gem.GemParameterOptions;
import io.sbk.gem.SshConnection;
import io.sbk.perl.PerlConfig;
import io.sbk.system.Printer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class SbkGemParameters extends SbkParameters implements GemParameterOptions {

    final private GemConfig config;

    @Getter
    final private int timeoutMS;

    @Getter
    final private String[] optionsArgs;

    @Getter
    private String[] parsedArgs;

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

    @Getter
    private String hostName;

    @Getter
    private int ramPort;

    private boolean isCopy;


    public SbkGemParameters(String name, List<String> driversList, GemConfig config, int ramport) {
        super(name, GemConfig.DESC, driversList);
        this.config = config;
        this.timeoutMS = config.timeoutSeconds * PerlConfig.MS_PER_SEC;
        this.ramPort = ramport;
        try {
            this.hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Printer.log.error(ex.toString());
            this.hostName = GemConfig.LOCAL_HOST;
        }
        addOption("nodes", true, "remote hostnames separated by `,` , default: "+config.nodes);
        addOption("gemuser", true, "ssh user name of the remote hosts, default: " + config.user);
        addOption("gempass", true, "ssh user password of the remote hosts, default: " + config.password);
        addOption("gemport", true, "ssh port of the remote hosts, default: " + config.port);
        addOption("sbkdir", true, "directory path of sbk application, default: " + config.sbkPath);
        addOption("sbkcommand", true, "sbk command for remote run, default: " + config.sbkCommand);
        addOption("hostname", true, "this RAM host name, default: " + hostName);
        addOption("ramport", true, "RAM port number; default: "+ramPort);
        addOption("copy", true, "Copy the SBK package to remote hosts; default: true");
        this.optionsArgs = new String[]{"-nodes", "-gemuser", "-gempass", "-gemport", "-sbkdir", "-sbkcommand",
                "-hostname", "ramport", "-copy"};
        this.parsedArgs = null;
        this.isCopy = true;
    }

    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        super.parseArgs(args);
        final String nodeString = getOptionValue("nodes", config.nodes);
        String[] nodes = nodeString.split(",");
        user = getOptionValue("gemuser", config.user);
        password = getOptionValue("gempass", config.password);
        port = Integer.parseInt(getOptionValue("gemport", Integer.toString(config.port)));
        sbkDir = getOptionValue("sbkdir", config.sbkPath);
        sbkCommand = getOptionValue("sbkcommand", config.sbkCommand);
        ramPort = Integer.parseInt(getOptionValue("ramport", Integer.toString(ramPort)));
        isCopy = Boolean.parseBoolean(getOptionValue("copy", Boolean.toString(isCopy)));

        parsedArgs = new String[]{"-nodes", nodeString, "-gemuser", user, "-gempass", password, "-gemport",
                Integer.toString(port), "-sbkdir", sbkDir, "-sbkcommand", sbkCommand, "-hostname", hostName,
                "-ramport", Integer.toString(ramPort), "-copy", Boolean.toString(isCopy)};

        connections = new SshConnection[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            connections[i] = new SshConnection(nodes[i], user, password, port, config.remoteDir);
        }

        if (StringUtils.isEmpty(sbkDir)) {
            String errMsg = "The SBK application directory not supplied!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isDirectory(Paths.get(sbkDir))) {
            String errMsg = "The SBK application directory: "+sbkDir +" not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (StringUtils.isEmpty(sbkCommand)) {
            String errMsg = "The SBK application/command not supplied!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        final String sbkFullCommand = sbkDir + File.separator + GemConfig.BIN_DIR + File.separator + sbkCommand;
        Path sbkCommandPath = Paths.get(sbkFullCommand);

        if (!Files.exists(sbkCommandPath)) {
            String errMsg = "The sbk executable command: "+sbkFullCommand+" not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isExecutable(sbkCommandPath)) {
            String errMsg = "The executable permissions are not found for command: "+sbkFullCommand;
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

    }

    @Override
    public boolean isCopy() {
        return isCopy;
    }
}

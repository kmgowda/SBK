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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * GEM (Group Execution Monitor) parameters and argument parsing.
 *
 * <p>Extends {@link SbkDriversParameters} to include SBK driver/logger help, and adds GEM-specific
 * options for remote orchestration (nodes, SSH creds/port, SBK and Java deployment,
 * copy/delete/deleteafter,
 * local SBM host/port/idle sleep). Populates typed getters and constructs {@link ConnectionConfig}
 * instances for each target node.
 *
 * <p>Supported options (help text shows defaults from {@link GemConfig}):
 * - -nodes: comma/space/newline-separated hostnames
 * - -gemuser, -gempass, -gemport
 * - -sbkdir, -sbkcommand
 * - -copy, -delete, -deleteafter, -javacopy, -javaversion, -javadir
 * - -localhost
 * - -sbmport, -sbmsleepms
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

    @Getter
    private int sbmIdleSleepMilliSeconds;

    /**
     * Construct GEM parameters with defaults and register GEM options.
     *
     * @param name   benchmark/application name used in help
     * @param drivers storage driver class names for help listing
     * @param loggers logger class names for help listing
     * @param config configuration backing defaults and parsed values
     * @param sbmPort SBM port default
     * @param sbmIdleSleepMilliSeconds SBM idle sleep default (ms)
     */
    public SbkGemParameters(String name, String[] drivers, String[] loggers, @NotNull GemConfig config, int sbmPort,
                            int sbmIdleSleepMilliSeconds) {
        super(name, GemConfig.DESC, drivers, loggers);
        this.config = config;
        this.timeoutMS = config.timeoutSeconds * Time.MS_PER_SEC;
        this.sbmPort = sbmPort;
        this.sbmIdleSleepMilliSeconds = sbmIdleSleepMilliSeconds;
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
        addOption(GemConfig.GEM_PASS_OPTION, true, "ssh user password of the remote hosts, default: " +
                (StringUtils.isEmpty(config.gempass) ? "not set" : "******"));
        addOption("hostkeycheck", true, "Verify remote SSH host keys against known_hosts; default: " +
                config.hostkeycheck);
        addOption("knownhosts", true, "Known-hosts file; an empty value uses ~/.ssh/known_hosts; default: " +
                (StringUtils.isEmpty(config.knownhosts) ? "default" : config.knownhosts));
        addOption("gemport", true, "ssh port of the remote hosts, default: " + config.gemport);
        addOption("sbkdir", true, "directory path of sbk application, default: " + config.sbkdir);
        addOption("sbkcommand", true,
                "remote sbk command; command path is relative to 'sbkdir', default: " + config.sbkcommand);
        addOption("copy", true, "Copy SBK when missing or mismatched; default: " + config.copy);
        addOption("javacopy", true, "Copy the local Java runtime when the expected remote Java is unavailable; " +
                "default: " + config.javacopy);
        addOption("javaversion", true, "Required remote Java major version; default: " + config.javaversion);
        addOption("javadir", true, "Remote Java home containing bin/java; default: " +
                (StringUtils.isEmpty(config.javadir) ? "null" : config.javadir));
        addOption("delete", true, "Delete a mismatched remote SBK before copying; default: " + config.delete);
        addOption("deleteafter", true, "Delete remote SBK after benchmarking; default: " + config.deleteafter);
        addOption("localhost", true, "this local SBM host name, default: " + localHost);
        addOption("sbmport", true, "SBM port number; default: " + this.sbmPort);
        addOption("sbmsleepms", true, "SBM idle milliseconds to sleep; default: " + this.sbmIdleSleepMilliSeconds +
                " ms");
        this.optionsArgs = new String[]{"-nodes", "-gemuser", "-gempass", "-hostkeycheck", "-knownhosts",
                "-gemport", "-sbkdir", "-sbkcommand", "-copy", "-javacopy", "-javaversion", "-javadir",
                "-delete", "-deleteafter", "-localhost", "-sbmport", "-sbmsleepms"};
        this.parsedArgs = null;
    }


    /**
     * Parse GEM options, validate SBK directory/command, and build connection set.
     *
     * <p>Derives {@link #parsedArgs} and {@link #connections}. Validates that SBK directory exists,
     * command exists and is executable.
     *
     * @param args command-line arguments to parse
     * @throws ParseException            if parsing of arguments fails or required values are invalid
     * @throws IllegalArgumentException  if SBK directory/command checks fail or other validation errors occur
     * @throws HelpException             if help text needs to be displayed by upstream handling
     */
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        super.parseArgs(args);
        final String nodeString = getOptionValue("nodes", config.nodes);
        String[] nodes = nodeString.replace("[ ]+", " ")
                .replace("[,] +", ",")
                .split("[ ,\n]+");
        config.gemuser = getOptionValue("gemuser", config.gemuser);
        config.gempass = getOptionValue(GemConfig.GEM_PASS_OPTION, config.gempass);
        config.hostkeycheck = Boolean.parseBoolean(getOptionValue("hostkeycheck",
                Boolean.toString(config.hostkeycheck)));
        config.knownhosts = getOptionValue("knownhosts", Objects.requireNonNullElse(config.knownhosts, ""));
        config.gemport = Integer.parseInt(getOptionValue("gemport", Integer.toString(config.gemport)));
        config.sbkdir = getOptionValue("sbkdir", config.sbkdir);
        config.sbkcommand = getOptionValue("sbkcommand", config.sbkcommand);
        localHost = getOptionValue("localhost", localHost);
        sbmPort = Integer.parseInt(getOptionValue("sbmport", Integer.toString(sbmPort)));
        sbmIdleSleepMilliSeconds = Integer.parseInt(getOptionValue("sbmsleepms", Integer.toString(sbmIdleSleepMilliSeconds)));
        config.copy = Boolean.parseBoolean(getOptionValue("copy", Boolean.toString(config.copy)));
        config.javacopy = Boolean.parseBoolean(getOptionValue("javacopy", Boolean.toString(config.javacopy)));
        config.javaversion = Integer.parseInt(getOptionValue("javaversion", Integer.toString(config.javaversion)));
        config.javadir = getOptionValue("javadir", Objects.requireNonNullElse(config.javadir, ""));
        config.delete = Boolean.parseBoolean(getOptionValue("delete", Boolean.toString(config.delete)));
        config.deleteafter = Boolean.parseBoolean(getOptionValue("deleteafter",
                Boolean.toString(config.deleteafter)));

        if (config.javaversion <= 0) {
            throw new IllegalArgumentException("The Java major version must be greater than zero");
        }

        parsedArgs = new String[]{"-nodes", nodeString, "-gemuser", config.gemuser,
                "-hostkeycheck", Boolean.toString(config.hostkeycheck), "-knownhosts", config.knownhosts,
                "-gemport", Integer.toString(config.gemport), "-sbkdir", config.sbkdir,
                "-sbkcommand", config.sbkcommand, "-copy", Boolean.toString(config.copy),
                "-javacopy", Boolean.toString(config.javacopy), "-javaversion",
                Integer.toString(config.javaversion), "-javadir", config.javadir, "-delete",
                Boolean.toString(config.delete), "-deleteafter", Boolean.toString(config.deleteafter),
                "-localhost", localHost, "-sbmport", Integer.toString(sbmPort)};

        connections = new ConnectionConfig[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            connections[i] = new ConnectionConfig(nodes[i], config.gemuser, config.gempass, config.gemport,
                    config.remoteDir, config.hostkeycheck, config.knownhosts);
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
    public boolean isJavaCopy() {
        return config.javacopy;
    }

    @Override
    public int getJavaVersion() {
        return config.javaversion;
    }

    @Override
    public String getJavaDir() {
        return config.javadir;
    }

    @Override
    public boolean isDelete() {
        return config.delete;
    }

    @Override
    public boolean isDeleteAfter() {
        return config.deleteafter;
    }
}

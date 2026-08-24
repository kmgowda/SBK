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

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

/**
 * GEM (Group Execution Monitor) parameters and argument parsing.
 *
 * <p>Extends {@link SbkDriversParameters} to include SBK driver/logger help, and adds GEM-specific
 * options for remote orchestration (nodes, SSH creds/port, SBK and Java deployment,
 * automatic runtime deployment/cleanup,
 * local SBM host/port/idle sleep). Populates typed getters and constructs {@link ConnectionConfig}
 * instances for each target node.
 *
 * <p>Supported options (help text shows defaults from {@link GemConfig}):
 * - -nodes: comma/space/newline-separated hostnames or host:port endpoints
 * - -gemuser, -gempass, -gemport
 * - -sbkdir
 * - -runtimecleanup, -delete, -javaversion, -javadir
 * - -localhost
 * - -sbmport, -sbmsleepms
 * - -totalrecords
 * - -totalthroughput
 */
@Slf4j
public final class SbkGemParameters extends SbkDriversParameters implements GemParameterOptions {

    private static final int MINIMUM_PORT = 1;
    private static final int MAXIMUM_PORT = 65_535;

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

    @Getter
    private boolean totalRecordsOption;

    private long distributedTotalRecords;

    @Getter
    private boolean totalThroughputOption;

    @Getter
    private BigDecimal totalThroughput;

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
                remote hostnames or host:port endpoints separated by ',';
                default:""" + config.nodes);
        addOption("gemuser", true, "ssh user name of the remote hosts, default: " + config.gemuser);
        addOption(GemConfig.GEM_PASS_OPTION, true, "ssh user password of the remote hosts, default: " +
                (StringUtils.isEmpty(config.gempass) ? "not set" : "******"));
        addOption("hostkeycheck", true, "Verify remote SSH host keys against known_hosts; default: " +
                config.hostkeycheck);
        addOption("knownhosts", true, "Known-hosts file; an empty value uses ~/.ssh/known_hosts; default: " +
                (StringUtils.isEmpty(config.knownhosts) ? "default" : config.knownhosts));
        addOption("gemport", true, "ssh port of the remote hosts, default: " + config.gemport);
        addOption("sbkdir", true, "directory path of the SBK application containing the standard "
                + GemConfig.SBK_COMMAND + " launcher; default: " + config.sbkdir);
        addOption("javaversion", true, "Required remote Java major version; default: " + config.javaversion);
        addOption("javadir", true, "Remote Java home containing bin/java; default: " +
                (StringUtils.isEmpty(config.javadir) ? "null" : config.javadir));
        addOption("delete", true, "Delete a mismatched remote SBK before copying; default: " + config.delete);
        addOption("runtimecleanup", true, "Remove every inactive non-current SBK-GEM-managed runtime and "
                + "local cached bundle, regardless of version ordering, while retaining the current verified "
                + "identity; default: " + config.runtimecleanup);
        addOption("localhost", true, "this local SBM host name, default: " + localHost);
        addOption("sbmport", true, "SBM port number; default: " + this.sbmPort);
        addOption("sbmsleepms", true, "SBM idle milliseconds to sleep; default: " + this.sbmIdleSleepMilliSeconds +
                " ms");
        addOption("totalrecords", true, "Total records across all remote SBK clients; without -seconds this is " +
                "a fixed record count, and with -seconds this is the aggregate records/second rate; mutually " +
                "exclusive with -records and -throughput");
        addOption("totalthroughput", true, "Total throughput in MB/s across all remote SBK clients; mutually " +
                "exclusive with -throughput");
        this.optionsArgs = new String[]{"-nodes", "-gemuser", "-gempass", "-hostkeycheck", "-knownhosts",
                "-gemport", "-sbkdir", "-javaversion", "-javadir",
                "-delete", "-runtimecleanup", "-localhost", "-sbmport", "-sbmsleepms", "-totalrecords",
                "--totalrecords", "-totalthroughput", "--totalthroughput"};
        this.parsedArgs = null;
        this.totalThroughput = BigDecimal.ZERO;
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
        rejectRemovedDeploymentOptions(args);
        totalRecordsOption = hasCommandLineOption(args, "totalrecords");
        totalThroughputOption = hasCommandLineOption(args, "totalthroughput");
        if (totalRecordsOption && hasCommandLineOption(args, "records")) {
            throw new IllegalArgumentException("The '-totalrecords' and '-records' options are mutually exclusive");
        }
        if (totalRecordsOption && hasCommandLineOption(args, "throughput")) {
            throw new IllegalArgumentException(
                    "The '-totalrecords' and '-throughput' options are mutually exclusive");
        }
        if (totalThroughputOption && hasCommandLineOption(args, "throughput")) {
            throw new IllegalArgumentException(
                    "The '-totalthroughput' and '-throughput' options are mutually exclusive");
        }
        if (totalRecordsOption) {
            distributedTotalRecords = parseTotalRecords(args);
        }
        if (totalThroughputOption) {
            totalThroughput = parseTotalThroughput(args);
        }
        super.parseArgs(normalizeAggregateOptions(args));
        final String nodeString = getOptionValue("nodes", config.nodes);
        String[] nodes = nodeString.split("[ ,\n]+");
        config.gemuser = getOptionValue("gemuser", config.gemuser);
        config.gempass = getOptionValue(GemConfig.GEM_PASS_OPTION, config.gempass);
        config.hostkeycheck = Boolean.parseBoolean(getOptionValue("hostkeycheck",
                Boolean.toString(config.hostkeycheck)));
        config.knownhosts = getOptionValue("knownhosts", Objects.requireNonNullElse(config.knownhosts, ""));
        config.gemport = Integer.parseInt(getOptionValue("gemport", Integer.toString(config.gemport)));
        validatePort(config.gemport, "-gemport");
        config.sbkdir = getOptionValue("sbkdir", config.sbkdir);
        localHost = getOptionValue("localhost", localHost);
        sbmPort = Integer.parseInt(getOptionValue("sbmport", Integer.toString(sbmPort)));
        sbmIdleSleepMilliSeconds = Integer.parseInt(getOptionValue("sbmsleepms", Integer.toString(sbmIdleSleepMilliSeconds)));
        config.javaversion = Integer.parseInt(getOptionValue("javaversion", Integer.toString(config.javaversion)));
        config.javadir = getOptionValue("javadir", Objects.requireNonNullElse(config.javadir, ""));
        config.delete = Boolean.parseBoolean(getOptionValue("delete", Boolean.toString(config.delete)));
        config.runtimecleanup = Boolean.parseBoolean(getOptionValue("runtimecleanup",
                Boolean.toString(config.runtimecleanup)));

        if (config.javaversion <= 0) {
            throw new IllegalArgumentException("The Java major version must be greater than zero");
        }

        parsedArgs = new String[]{"-nodes", nodeString, "-gemuser", config.gemuser,
                "-hostkeycheck", Boolean.toString(config.hostkeycheck), "-knownhosts", config.knownhosts,
                "-gemport", Integer.toString(config.gemport), "-sbkdir", config.sbkdir, "-javaversion",
                Integer.toString(config.javaversion), "-javadir", config.javadir, "-delete",
                Boolean.toString(config.delete), "-runtimecleanup", Boolean.toString(config.runtimecleanup),
                "-localhost", localHost, "-sbmport", Integer.toString(sbmPort)};

        connections = new ConnectionConfig[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final NodeEndpoint endpoint = parseNodeEndpoint(nodes[i], config.gemport);
            connections[i] = new ConnectionConfig(endpoint.host(), config.gemuser, config.gempass, endpoint.port(),
                    config.remoteDir, config.hostkeycheck, config.knownhosts);
        }

        validateAggregateOptions(nodes.length);

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

        final Path sbkCommandPath = Paths.get(config.sbkdir).resolve(GemConfig.SBK_COMMAND);

        if (!Files.exists(sbkCommandPath)) {
            String errMsg = "The SBK executable command: " + sbkCommandPath + " not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isExecutable(sbkCommandPath)) {
            String errMsg = "The executable permissions are not found for command: " + sbkCommandPath;
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

    }

    /**
     * Get the configured record value. The GEM aggregate value is returned when {@code -totalrecords} is supplied;
     * otherwise, this returns the standard SBK record value. The aggregate value is a fixed count without
     * {@code -seconds} and a records/second rate with {@code -seconds}.
     *
     * @return aggregate or standard record value, according to the selected option
     */
    @Override
    public long getTotalRecords() {
        return totalRecordsOption ? distributedTotalRecords : super.getTotalRecords();
    }

    private void validateAggregateOptions(int nodeCount) {
        if (totalRecordsOption) {
            validateTotalRecords(nodeCount);
        }
        if (totalThroughputOption && totalThroughput.signum() <= 0) {
            throw new IllegalArgumentException("The '-totalthroughput' value must be greater than zero");
        }
        if (totalThroughputOption) {
            validateMatchingMixedWorkerCounts("-totalthroughput");
        }
        if (totalRecordsOption && totalThroughputOption && getTotalSecondsToRun() > 0) {
            throw new IllegalArgumentException("The '-totalrecords' and '-totalthroughput' options cannot be " +
                    "combined with '-seconds' because both would define the benchmark rate");
        }
    }

    private void validateTotalRecords(int nodeCount) {
        if (distributedTotalRecords <= 0) {
            throw new IllegalArgumentException("The '-totalrecords' value must be greater than zero");
        }
        if (getTotalSecondsToRun() > 0) {
            validateMatchingMixedWorkerCounts("-totalrecords");
            final int workers = getWritersCount() > 0 ? getWritersCount() : getReadersCount();
            if (distributedTotalRecords % workers != 0) {
                throw new IllegalArgumentException("The '-totalrecords' records/second value must be divisible by " +
                        "the active worker count " + workers);
            }
            if (distributedTotalRecords / workers < nodeCount) {
                throw new IllegalArgumentException("The '-totalrecords' records/second value must provide at " +
                        "least one record/second per active worker on each of the " + nodeCount + " nodes");
            }
        } else if (distributedTotalRecords < nodeCount) {
            throw new IllegalArgumentException("The '-totalrecords' value must be at least the number of nodes: " +
                    nodeCount);
        }
    }

    private void validateMatchingMixedWorkerCounts(String option) {
        final int writers = getWritersCount();
        final int readers = getReadersCount();
        if (writers > 0 && readers > 0 && writers != readers) {
            throw new IllegalArgumentException("The '" + option + "' aggregate rate requires equal writer and " +
                    "reader counts for mixed workloads; configured writers: " + writers + ", readers: " + readers);
        }
    }

    private static long parseTotalRecords(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (isOption(args[i], "totalrecords")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("The '-totalrecords' option requires a value");
                }
                try {
                    return Long.parseLong(args[i + 1]);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid '-totalrecords' value: " + args[i + 1], ex);
                }
            }
        }
        throw new IllegalArgumentException("The '-totalrecords' option requires a value");
    }

    private static BigDecimal parseTotalThroughput(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (isOption(args[i], "totalthroughput")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("The '-totalthroughput' option requires a value");
                }
                try {
                    final BigDecimal value = new BigDecimal(args[i + 1]);
                    if (!Double.isFinite(value.doubleValue())) {
                        throw new IllegalArgumentException("The '-totalthroughput' value is outside the supported " +
                                "SBK throughput range: " + args[i + 1]);
                    }
                    return value;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid '-totalthroughput' value: " + args[i + 1], ex);
                }
            }
        }
        throw new IllegalArgumentException("The '-totalthroughput' option requires a value");
    }

    private static String[] normalizeAggregateOptions(String[] args) {
        final String[] normalized = Arrays.copyOf(args, args.length);
        for (int i = 0; i < normalized.length; i++) {
            if (isOption(normalized[i], "totalrecords")) {
                normalized[i] = "-records";
                if (i + 1 < normalized.length) {
                    normalized[i + 1] = "1";
                }
            } else if (isOption(normalized[i], "totalthroughput")) {
                normalized[i] = "-throughput";
                if (i + 1 < normalized.length) {
                    normalized[i + 1] = "1";
                }
            }
        }
        return normalized;
    }

    private static boolean hasCommandLineOption(String[] args, String option) {
        return Arrays.stream(args).anyMatch(argument -> isOption(argument, option));
    }

    private static boolean isOption(String argument, String option) {
        return ("-" + option).equals(argument) || ("--" + option).equals(argument);
    }

    private static void rejectRemovedDeploymentOptions(String[] args) {
        if (hasCommandLineOption(args, "copy")) {
            throw new IllegalArgumentException("The '-copy' option was removed: missing exact SBK/JDK runtime "
                    + "content is now copied automatically; use '-runtimecleanup' to control stale versions");
        }
        if (hasCommandLineOption(args, "deleteafter")) {
            throw new IllegalArgumentException("The '-deleteafter' option was removed: the current verified "
                    + "runtime is retained and '-runtimecleanup' controls inactive non-current versions");
        }
        if (hasCommandLineOption(args, "sbkcommand")) {
            throw new IllegalArgumentException("The '-sbkcommand' option was removed: SBK-GEM always validates "
                    + "and deploys the standard '" + GemConfig.SBK_COMMAND + "' launcher under '-sbkdir'");
        }
        if (hasCommandLineOption(args, "javacopy")) {
            throw new IllegalArgumentException("The '-javacopy' option was removed: SBK-GEM now reuses a "
                    + "matching remote JDK or provisions the controller JDK automatically and separately from SBK");
        }
    }

    private static NodeEndpoint parseNodeEndpoint(String value, int defaultPort) {
        if (value.startsWith("[")) {
            final int bracket = value.indexOf(']');
            if (bracket < 0) {
                throw new IllegalArgumentException("Invalid bracketed node endpoint: " + value);
            }
            final String host = value.substring(1, bracket);
            if (host.isBlank()) {
                throw new IllegalArgumentException("The node host must not be empty: " + value);
            }
            if (bracket == value.length() - 1) {
                return new NodeEndpoint(host, defaultPort);
            }
            if (value.charAt(bracket + 1) != ':' || bracket + 2 >= value.length()) {
                throw new IllegalArgumentException("Invalid bracketed node endpoint: " + value);
            }
            return new NodeEndpoint(host, parsePort(value.substring(bracket + 2), value));
        }

        final int firstColon = value.indexOf(':');
        final int lastColon = value.lastIndexOf(':');
        if (firstColon > 0 && firstColon == lastColon) {
            return new NodeEndpoint(value.substring(0, firstColon),
                    parsePort(value.substring(firstColon + 1), value));
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("The node host must not be empty");
        }
        return new NodeEndpoint(value, defaultPort);
    }

    private static int parsePort(String value, String endpoint) {
        final int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid SSH port in node endpoint: " + endpoint, ex);
        }
        validatePort(port, "node endpoint " + endpoint);
        return port;
    }

    private static void validatePort(int port, String option) {
        if (port < MINIMUM_PORT || port > MAXIMUM_PORT) {
            throw new IllegalArgumentException("The SSH port for " + option + " must be between "
                    + MINIMUM_PORT + " and " + MAXIMUM_PORT + ": " + port);
        }
    }

    private record NodeEndpoint(String host, int port) {
    }

    @Override
    public String getSbkDir() {
        return config.sbkdir;
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
    public boolean isRuntimeCleanup() {
        return config.runtimecleanup;
    }
}

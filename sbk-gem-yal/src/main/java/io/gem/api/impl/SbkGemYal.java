/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;


import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import io.gem.params.impl.SbkGemYmlMap;
import io.gem.exception.SbkGemParameterException;
import io.micrometer.core.instrument.util.IOUtils;
import io.sbk.config.Config;
import io.sbk.config.YalConfig;
import io.sbk.exception.HelpException;
import io.gem.api.RemoteResponse;
import io.sbk.system.Printer;
import io.sbk.params.YmlMap;
import io.sbk.params.impl.SbkYalParameters;
import io.sbk.utils.SbkUtils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Class SbkGemYal.
 */
final public class SbkGemYal {
    final static String CONFIG_FILE = "gem-yal.properties";
    final static String NAME = "sbk-gem-yal";

    final static String DESC = "Storage Benchmark Kit-Group Execution Monitor-YML Arguments Loader";
    final static String BANNER_FILE = "gem-yal-banner.txt";

    /**
     * Creates an SBK-GEM YAML launcher.
     */
    public SbkGemYal() {
    }

    /**
     * Run the Performance Benchmarking .
     *
     * @param args               command line arguments.
     * @param applicationName    Name of the application. will be used in the 'help' message. if it is 'null' ,
     *                           SbkServer is used by default.
     * @param storagePackageName Name of the package where storage class is available.
     *                           If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param loggerPackageName  Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                           logger will be used.
     * @return Array of remote responses
     * @throws ParseException           If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException              If an exception occurred due to write or read failures.
     * @throws InterruptedException     If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException       If an exception occurred.
     * @throws TimeoutException         If an exception occurred if an I/O operation is timed out.
     * @throws InstantiationException    if the exception occurred due to initiation failures.
     * @throws ClassNotFoundException    If the storage class driver is not found.
     * @throws InvocationTargetException if the exception occurs.
     * @throws NoSuchMethodException     if the exception occurs.
     * @throws IllegalAccessException    if the exception occurs.
     * @throws HelpException            if '-help' is used or yaml file is missing.
     */
    public static RemoteResponse[] run(final String[] args, final String applicationName, final String storagePackageName,
                                       String loggerPackageName) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException, HelpException,
            ClassNotFoundException, InvocationTargetException, InstantiationException,
            NoSuchMethodException, IllegalAccessException {
        return runBenchmark(args, applicationName, storagePackageName, loggerPackageName);
    }


    private static RemoteResponse[] runBenchmark(final String[] args, final String applicationName,
                                                 final String storagePackageName, final String loggerPackageName)
            throws ParseException, IllegalArgumentException, IOException, InterruptedException,
            ExecutionException, TimeoutException, HelpException, ClassNotFoundException, InvocationTargetException,
            InstantiationException, NoSuchMethodException, IllegalAccessException {
        final String version = SbkGemYal.class.getPackage().getImplementationVersion();
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName : SbkGemYal.NAME;
        final String[] gemArgs;
        final SbkYalParameters params;
        final YalConfig yalConfig;
        String yalFileName;

        Printer.log.info(IOUtils.toString(SbkGemYal.class.getClassLoader().getResourceAsStream(BANNER_FILE)));
        Printer.log.info(SbkGemYal.DESC);
        Printer.log.info(SbkGemYal.NAME.toUpperCase() + " Version: " + Objects.requireNonNullElse(version, ""));
        Printer.log.info("Arguments List: " + Arrays.toString(SbkUtils.redactSensitiveOptionValues(args)));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());

        yalConfig = mapper.readValue(SbkGemYal.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                YalConfig.class);
        params = new SbkGemYalParameters(appName, SbkGemYal.DESC, yalConfig);

        final boolean isPrintOption =  SbkUtils.hasArg(args, YalConfig.PRINT_OPTION_ARG);
        String[] nextArgs = SbkUtils.removeOptionArgs(args, new String[]{YalConfig.PRINT_OPTION_ARG});
        nextArgs = SbkUtils.removeOptionArgsAndValues(nextArgs, new String[]{YalConfig.FILE_OPTION_ARG});
        try {
            params.parseArgs(args);
            yalFileName = params.getFileName();
        } catch (HelpException ex) {
            params.printHelp();
            throw ex;
        } catch (ParseException | IllegalArgumentException ex) {
            if (ex instanceof UnrecognizedOptionException unrecognized &&
                    unrecognized.getOption() != null && unrecognized.getOption().startsWith("--")) {
                Printer.log.error(unrecognized.toString());
                params.printHelp();
                throw unrecognized;
            }
            Printer.log.warn("SBK-GEM-YAL: Overriding options are supplied!");
            if (SbkUtils.hasHelp(args)) {
                params.printHelp();
                throw new HelpException(params.getHelpText());
            }
            final String fileName = SbkUtils.getArgValue(args, YalConfig.FILE_OPTION_ARG);
            yalFileName = StringUtils.isNotEmpty(fileName) ? fileName : yalConfig.yamlFileName;
        }

        try {
            gemArgs = YmlMap.getYmlArgs(yalFileName, SbkGemYmlMap.class);
        } catch (IOException | JacksonException | InvalidPathException ex) {
            final String configuredFile = StringUtils.isEmpty(yalFileName) ? "<empty>" : yalFileName;
            final String resolvedFile;
            try {
                resolvedFile = Path.of(configuredFile).toAbsolutePath().normalize().toString();
            } catch (InvalidPathException invalidPath) {
                throw reportedYamlFailure(configuredFile, configuredFile, invalidPath, params);
            }
            Printer.log.error("SBK-GEM-YAL: Unable to read YAML configuration file: {}", resolvedFile);
            Printer.log.error("SBK-GEM-YAL: {}", rootMessage(ex));
            printYamlUsage();
            if (isPrintOption) {
                SbkGem.run(new String[]{Config.HELP_OPTION_ARG}, applicationName, storagePackageName, loggerPackageName);
            }
            params.printHelp();
            throw new SbkGemParameterException(new IllegalArgumentException(
                    "Unable to read SBK-GEM YAML configuration file: " + resolvedFile, ex));
        }

        final String[] mergeArgs = SbkUtils.mergeArgs(gemArgs, nextArgs);
        Printer.log.info("SBK-GEM-YAL: Merged YAML and command-line arguments: "
                + Arrays.toString(SbkUtils.redactSensitiveOptionValues(mergeArgs)));
        String[] sbkGemArgs = mergeArgs;
        if (isPrintOption) {
            sbkGemArgs = Arrays.copyOf(mergeArgs, mergeArgs.length + 1);
            sbkGemArgs[mergeArgs.length] = Config.HELP_OPTION_ARG;
        }

        return SbkGem.run(sbkGemArgs, applicationName, storagePackageName, loggerPackageName);
    }

    private static SbkGemParameterException reportedYamlFailure(String configuredFile, String resolvedFile,
                                                                 RuntimeException failure,
                                                                 SbkYalParameters params) {
        Printer.log.error("SBK-GEM-YAL: Invalid YAML configuration path '{}': {}",
                configuredFile, failure.getMessage());
        printYamlUsage();
        params.printHelp();
        return new SbkGemParameterException(new IllegalArgumentException(
                "Invalid SBK-GEM YAML configuration path: " + resolvedFile, failure));
    }

    private static String rootMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return Objects.requireNonNullElse(cause.getMessage(), cause.getClass().getSimpleName());
    }

    private static void printYamlUsage() {
        Printer.log.info("Usage: sbk-gem-yal -file <yaml-file> [SBK-GEM option overrides]");
        Printer.log.info("Example: ./build/install/sbk/bin/sbk-gem-yal -file /path/to/sbk-gem.yml");
        Printer.log.info("The YAML document must start with 'sbkGemArgs:'; see "
                + "sbk-gem-yal/sbk-gem.yml in an SBK source checkout for a template");
    }
}

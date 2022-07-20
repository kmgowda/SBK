/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.micrometer.core.instrument.util.IOUtils;
import io.sbk.config.Config;
import io.sbk.config.YalConfig;
import io.sbk.exception.HelpException;
import io.sbk.logger.RWLogger;
import io.sbk.system.Printer;
import io.sbk.params.YmlMap;
import io.sbk.params.impl.SbkYalParameters;
import io.sbk.params.impl.SbkYmlMap;
import io.sbk.utils.SbkUtils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Class SbkGemYal.
 */
public final class SbkYal {
    final static String CONFIG_FILE = "sbk-yal.properties";
    final static String NAME = "sbk-yal";
    final static String DESC = "Storage Benchmark Kit-YML Arguments Loader";
    final static String BANNER_FILE = "sbk-yal-banner.txt";

    /**
     * Run the Performance Benchmarking .
     *
     * @param args            command line arguments.
     * @param packageName     Name of the package where storage class is available.
     *                        If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param applicationName Name of the application. will be used in the 'help' message. if it is 'null' ,
     *                        SbkServer is used by default.
     * @param outLogger       Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                        logger will be used.
     * @throws ParseException            If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException  If an exception occurred due to invalid arguments.
     * @throws IOException               If an exception occurred due to write or read failures.
     * @throws InterruptedException      If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException        If an exception occurred.
     * @throws InvocationTargetException If an exception occurred.
     * @throws InstantiationException    If an exception occurred.
     * @throws IllegalAccessException    If an exception occurred.
     * @throws NoSuchMethodException     If an exception occurred.
     * @throws ClassNotFoundException    If an exception occurred.
     * @throws TimeoutException          If an exception occurred if an I/O operation is timed out.
     * @throws HelpException             if '-help' is used or yaml file is missing.
     */
    public static void run(final String[] args, final String packageName, final String applicationName,
                           RWLogger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException, HelpException, ClassNotFoundException, InvocationTargetException, InstantiationException, NoSuchMethodException, IllegalAccessException {
        runBenchmark(args, packageName, applicationName, outLogger);
    }

    private static void runBenchmark(final String[] args, final String packageName,
                                     final String applicationName, RWLogger outLogger)
            throws ParseException, IllegalArgumentException, IOException, InterruptedException,
            ExecutionException, TimeoutException, HelpException, ClassNotFoundException, InvocationTargetException, InstantiationException, NoSuchMethodException, IllegalAccessException {
        final String version = io.sbk.api.impl.SbkYal.class.getPackage().getImplementationVersion();
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName : SbkYal.NAME;
        final String[] yalArgs;
        final SbkYalParameters params;
        final YalConfig yalConfig;
        String yalFileName;

        Printer.log.info(IOUtils.toString(io.sbk.api.impl.SbkYal.class.getClassLoader().getResourceAsStream(BANNER_FILE)));
        Printer.log.info(SbkYal.DESC);
        Printer.log.info(SbkYal.NAME.toUpperCase() + " Version: " + Objects.requireNonNullElse(version, ""));
        Printer.log.info("Arguments List: " + Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        yalConfig = mapper.readValue(io.sbk.api.impl.SbkYal.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                YalConfig.class);
        params = new SbkYalParameters(appName, SbkYal.DESC, yalConfig);
        String[] nextArgs = SbkUtils.removeOptionArgs(args, new String[]{YalConfig.PRINT_OPTION_ARG});
        nextArgs = SbkUtils.removeOptionArgsAndValues(nextArgs, new String[]{YalConfig.FILE_OPTION_ARG});
        try {
            params.parseArgs(args);
            yalFileName = params.getFileName();
        } catch (HelpException ex) {
            params.printHelp();
            throw ex;
        } catch (ParseException | IllegalArgumentException ignored) {
            Printer.log.warn("SBK-YAL: Overriding options are supplied!");
            if (SbkUtils.hasHelp(args)) {
                params.printHelp();
                throw new HelpException(params.getHelpText());
            }
            final String fileName = SbkUtils.getArgValue(args, YalConfig.FILE_OPTION_ARG);
            yalFileName = StringUtils.isNotEmpty(fileName) ? fileName : yalConfig.yamlFileName;
        }

        try {
            yalArgs = YmlMap.getYmlArgs(yalFileName, SbkYmlMap.class);
        } catch (FileNotFoundException ex) {
            Printer.log.error(ex.toString());
            if (params.isPrintOption()) {
                Sbk.run(new String[]{Config.HELP_OPTION_ARG}, packageName, applicationName, outLogger);
                throw new HelpException(ex.toString());
            }
            params.printHelp();
            throw new HelpException(ex.toString());
        }
        final String[] mergeArgs = SbkUtils.mergeArgs(yalArgs, nextArgs);
        String[] sbkArgs = mergeArgs;
        if (params.isPrintOption()) {
            sbkArgs = Arrays.copyOf(mergeArgs, mergeArgs.length + 1);
            sbkArgs[mergeArgs.length] = Config.HELP_OPTION_ARG;
        }
        Sbk.run(sbkArgs, packageName, applicationName, outLogger);
    }
}

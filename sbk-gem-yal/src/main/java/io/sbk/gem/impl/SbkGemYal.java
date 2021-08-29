/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.micrometer.core.instrument.util.IOUtils;
import io.sbk.config.YalConfig;
import io.sbk.exception.HelpException;
import io.sbk.gem.RemoteResponse;
import io.sbk.logger.GemLogger;
import io.sbk.system.Printer;
import io.sbk.yal.YmlMap;
import io.sbk.yal.impl.SbkGemYmlMap;
import io.sbk.yal.impl.SbkYalParameters;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class SbkGemYal {
    final static String CONFIG_FILE = "gem-yal.properties";
    final static String NAME = "sbk-gem-yal";
    final static String DESC = "SBK-GEM-YML Arguments Loader";
    final static String BANNER_FILE = "gem-yal-banner.txt";

    /**
     * Run the Performance Benchmarking .
     * @param args command line arguments.
     * @param packageName  Name of the package where storage class is available.
     *                     If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param applicationName Name of the application. will be used in the 'help' message. if it is 'null' ,
     *                        SbkServer is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InterruptedException If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException If an exception occurred.
     * @throws TimeoutException If an exception occurred if an I/O operation is timed out.
     * @throws HelpException if '-help' is used or yaml file is missing.
     * @return Array of remote responses
     */
    public static RemoteResponse[] run(final String[] args, final String packageName, final String applicationName,
                                       GemLogger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException, HelpException {
        return runBenchmark(args, packageName, applicationName, outLogger);
    }


    private static RemoteResponse[] runBenchmark(final String[] args, final String packageName,
                                                 final String applicationName, GemLogger outLogger)
            throws ParseException, IllegalArgumentException, IOException, InterruptedException,
            ExecutionException, TimeoutException, HelpException {
        final String version = io.sbk.gem.impl.SbkGemYal.class.getPackage().getImplementationVersion();
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName : SbkGemYal.NAME;
        final String[] gemArgs;
        final SbkYalParameters params;
        final YalConfig yalConfig;

        Printer.log.info(IOUtils.toString(io.sbk.gem.impl.SbkGemYal.class.getClassLoader().getResourceAsStream(BANNER_FILE)));
        Printer.log.info(SbkGemYal.DESC);
        Printer.log.info(SbkGemYal.NAME.toUpperCase() + " Version: " + Objects.requireNonNullElse(version, ""));
        Printer.log.info("Arguments List: " + Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        yalConfig = mapper.readValue(io.sbk.gem.impl.SbkGemYal.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                YalConfig.class);
        params = new SbkGemYalParameters(appName, SbkGemYal.DESC, yalConfig);

        try {
            params.parseArgs(args);
        } catch (HelpException ex) {
            params.printHelp();
            throw ex;
        }

        try {
            gemArgs = YmlMap.getYmlArgs(params.getFileName(), SbkGemYmlMap.class);
        } catch (FileNotFoundException ex) {
            Printer.log.error(ex.toString());
            params.printHelp();
            throw new HelpException(ex.toString());
        }
        return SbkGem.run(gemArgs, packageName, applicationName, outLogger);
    }
}

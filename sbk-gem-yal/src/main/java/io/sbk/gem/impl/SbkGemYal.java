/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.sbk.config.YalConfig;
import io.sbk.exception.HelpException;
import io.sbk.gem.RemoteResponse;
import io.sbk.gem.YalMap;
import io.sbk.logger.GemLogger;
import io.sbk.system.Printer;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class SbkGemYal {
    final static String CONFIG_FILE = "gem-yal.properties";

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
     * @return Array of remote responses
     */
    public static RemoteResponse[] run(final String[] args, final String packageName, final String applicationName,
                                       GemLogger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException {
            return runBenchmark(args, packageName, applicationName, outLogger);
    }


    private static RemoteResponse[] runBenchmark(final String[] args, final String packageName,
                                             final String applicationName, GemLogger outLogger)
            throws ParseException, IllegalArgumentException, IOException, InterruptedException,
            ExecutionException, TimeoutException {
        final SbkGemYalParameters params;
        final YalConfig yalConfig;
        final String version = io.sbk.gem.impl.SbkGem.class.getPackage().getImplementationVersion();
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName : YalConfig.NAME;
        Printer.log.info(YalConfig.DESC);
        Printer.log.info(YalConfig.NAME.toUpperCase() +" Version: "+ Objects.requireNonNullElse(version, ""));
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        yalConfig = mapper.readValue(io.sbk.gem.impl.SbkGemYal.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                YalConfig.class);
        params = new SbkGemYalParameters(appName, YalConfig.DESC, yalConfig);

        try {
            params.parseArgs(args);
        } catch (HelpException e) {
            params.printHelp();
            return null;
        }

        return SbkGem.run(getYamlArgs(params.getFileName()), packageName, applicationName, outLogger);
    }

    private static String[] getYamlArgs(String yamlFileName) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        final YalMap yap = mapper.readValue(new File(yamlFileName), YalMap.class);
        final List<String> lt = new ArrayList<>();
        yap.args.forEach((k, v) -> {
            lt.add("-"+k.strip());
            lt.add(v.replaceAll("\\n+", " ").strip());
        });
       return lt.toArray(new String[0]);
    }

}

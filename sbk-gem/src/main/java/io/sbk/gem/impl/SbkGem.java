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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.micrometer.core.instrument.util.IOUtils;
import io.sbk.config.Config;
import io.sbk.data.DataType;
import io.sbk.exception.HelpException;
import io.sbk.api.Storage;
import io.sbk.api.StoragePackage;
import io.sbk.api.impl.SbkUtils;
import io.sbk.gem.GemBenchmark;
import io.sbk.config.GemConfig;
import io.sbk.logger.GemLogger;
import io.sbk.gem.GemParameterOptions;
import io.sbk.gem.RemoteResponse;
import io.sbk.logger.impl.GemRamPrometheusLogger;
import io.sbk.time.Time;
import io.sbk.config.RamConfig;
import io.sbk.ram.RamParameterOptions;
import io.sbk.ram.impl.SbkRamBenchmark;
import io.sbk.ram.impl.SbkRamParameters;
import io.sbk.system.Printer;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class SbkGem {
    final static String CONFIG_FILE = "gem.properties";
    final static String RAM_CONFIG_FILE = "ram.properties";
    final static String BANNER_FILE = "gem-banner.txt";

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
        final GemBenchmark benchmark;
        try {
            benchmark = buildBenchmark(args, packageName, applicationName, outLogger);
        } catch (HelpException ex) {
           return null;
        }
        final CompletableFuture<RemoteResponse[]> ret = benchmark.start();
        ret.thenAccept(results -> printRemoteResults(results, false));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            benchmark.stop();
        }));
        return ret.get();
    }


    /**
     * Build the Benchmark Object.
     *
     * @param args command line arguments.
     * @param packageName  Name of the package where storage class is available.
     *                     If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param applicationName Name of the application. will be used in the 'help' message.
     *                       if it is 'null' , storage name is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws HelpException if '-help' option is supplied.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @return Benchmark Interface
     */
    public static GemBenchmark buildBenchmark(final String[] args, final String packageName,
                                              final String applicationName, GemLogger outLogger)
            throws ParseException, IOException, HelpException {
        final GemParameterOptions params;
        final RamParameterOptions ramParams;
        final GemConfig gemConfig;
        final GemLogger logger;
        final RamConfig ramConfig;
        final Time time;
        final String version = io.sbk.ram.impl.SbkRam.class.getPackage().getImplementationVersion();
        final String sbkGemAppName = System.getProperty(GemConfig.SBK_GEM_APP_NAME);
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName :
                StringUtils.isNotEmpty(sbkGemAppName) ? sbkGemAppName : GemConfig.NAME;
        final String sbkAppName = System.getProperty(Config.SBK_APP_NAME);
        final String sbkCommand = StringUtils.isNotEmpty(sbkAppName) ? sbkAppName : Config.NAME;
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        final String argsClassName = SbkUtils.getClassName(args);
        final String className = StringUtils.isNotEmpty(argsClassName) ? argsClassName : sbkClassName;
        final String storagePackageName = StringUtils.isNotEmpty(packageName) ? packageName : Config.SBK_PACKAGE_NAME;
        final StoragePackage packageStore = new StoragePackage(storagePackageName);
        final Storage storageDevice;
        final String usageLine;
        final String[] storageDrivers;
        final String[] nextArgs;

        Printer.log.info(IOUtils.toString(io.sbk.gem.impl.SbkGem.class.getClassLoader().getResourceAsStream(BANNER_FILE)));
        Printer.log.info(GemConfig.DESC);
        Printer.log.info(GemConfig.NAME.toUpperCase() +" Version: "+ Objects.requireNonNullElse(version, ""));
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info("Storage Drivers Package: "+ storagePackageName);
        Printer.log.info(Config.SBK_APP_NAME + ": "+ Objects.requireNonNullElse(sbkAppName, ""));
        Printer.log.info(Config.SBK_CLASS_NAME + ": "+ Objects.requireNonNullElse(sbkClassName, ""));
        Printer.log.info(Config.SBK_APP_HOME+": "+ Objects.requireNonNullElse(sbkAppHome, ""));
        Printer.log.info("'"+Config.CLASS_OPTION_ARG +"': "+ Objects.requireNonNullElse(argsClassName, ""));
        packageStore.printDrivers();

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ramConfig = mapper.readValue(io.sbk.ram.impl.SbkRam.class.getClassLoader().getResourceAsStream(RAM_CONFIG_FILE),
                RamConfig.class);
        gemConfig = mapper.readValue(io.sbk.gem.impl.SbkGem.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                GemConfig.class);
        nextArgs = SbkUtils.removeOptionArgsAndValues(args, new String[]{Config.CLASS_OPTION_ARG});

        if (StringUtils.isNotEmpty(sbkCommand)) {
            gemConfig.sbkcommand = GemConfig.BIN_DIR + File.separator + sbkCommand;
        }

        if (StringUtils.isNotEmpty(sbkAppHome)) {
            gemConfig.sbkdir = sbkAppHome;
        }
        gemConfig.remoteDir = appName;
        if (StringUtils.isNotEmpty(version)) {
            gemConfig.remoteDir += "-"+version;
        }

        logger = Objects.requireNonNullElseGet(outLogger, GemRamPrometheusLogger::new);

        if (StringUtils.isEmpty(className)) {
            storageDevice = null;
        } else {
            Storage<?> device = null;
            try {
                device = packageStore.getStorage(className);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                    IllegalAccessException | InstantiationException ex) {
                Printer.log.warn("Instantiation of storage class '"+className+ "' from the package '" +
                        storagePackageName + "' failed!, " + "error: " + ex);
            }
            storageDevice = device;
        }

        usageLine = StringUtils.isNotEmpty(argsClassName) ? appName + " "+Config.CLASS_OPTION_ARG +" "+argsClassName :
                appName;
        storageDrivers = storageDevice == null ? packageStore.getDrivers() : null;

        params = new SbkGemParameters(usageLine, storageDrivers, gemConfig, ramConfig.port);
        logger.addArgs(params);
        if (storageDevice != null) {
            storageDevice.addArgs(params);
        }

        if (nextArgs.length == 0 || SbkUtils.hasHelp(args)) {
            final String helpText = params.getHelpText();
            System.out.println("\n" + helpText);
            throw new HelpException(helpText);
        }

        String[] processArgs = nextArgs;
        int i = 1;

        while (true) {
            Printer.log.info("SBK-GEM ["+i+ "]: Arguments to process : "+Arrays.toString(processArgs));
            i++;
            try {
                params.parseArgs(processArgs);
                logger.parseArgs(params);
                if (storageDevice != null) {
                    storageDevice.parseArgs(params);
                }
            } catch (UnrecognizedOptionException ex) {
                if (storageDevice != null) {
                    Printer.log.error(ex.toString());
                    params.printHelp();
                    throw ex;
                }
                Printer.log.warn(ex.toString());
                processArgs = SbkUtils.removeOptionArgsAndValues(processArgs, new String[]{ex.getOption()});
                if (processArgs == null) {
                    params.printHelp();
                    throw new ParseException("SBK-GEM: Insufficient command line arguments");
                }
                continue;
            } catch (HelpException ex) {
                System.out.println("\n"+ex.getHelpText());
                throw ex;
            }
            break;
        }

        if (storageDevice != null) {
            final DataType<?> dType = storageDevice.getDataType();
            if (dType == null) {
                String errMsg = "No storage Data type of Remote Storage device: "+ className;
                Printer.log.error(errMsg);
                throw new ParseException(errMsg);
            }

            int minSize = dType.getWriteReadMinSize();
            if (params.isWriteAndRead() && params.getRecordSize() < minSize) {
                String errMsg =
                        "Invalid record size: "+ params.getRecordSize() +
                                ", For both Writers and Readers, minimum data size should be "+ minSize +
                                " for data type: " +dType.getClass().getName();
                Printer.log.error(errMsg);
                throw new ParseException(errMsg);
            }
        }

        String actionString = "r";
        if (params.isWriteAndRead()) {
            actionString = "wr";
        } else if (params.getWritersCount() > 0) {
            actionString = "w";
        }

        // remove GEM and logger parameter options
        final String[] sbkArgsList = SbkUtils.removeOptionArgsAndValues(
                SbkUtils.removeOptionArgsAndValues(nextArgs, params.getOptionsArgs()), logger.getOptionsArgs());
        final StringBuilder sbkArgsBuilder = new StringBuilder(Config.CLASS_OPTION_ARG + " " + className);
        for (String arg: sbkArgsList) {
            sbkArgsBuilder.append(" ");
            sbkArgsBuilder.append(arg);
        }
        time = SbkUtils.getTime(logger);
        sbkArgsBuilder.append(" -time ").append(time.getTimeUnit().name());
        sbkArgsBuilder.append(" -minlatency " + logger.getMinLatency());
        sbkArgsBuilder.append(" -maxlatency " + logger.getMaxLatency());
        sbkArgsBuilder.append(" -context no");
        sbkArgsBuilder.append(" -ram " + params.getLocalHost());
        sbkArgsBuilder.append(" -ramport " + params.getRamPort());

        Printer.log.info("SBK dir: "+params.getSbkDir());
        Printer.log.info("SBK command: "+params.getSbkCommand());
        Printer.log.info("Arguments to remote SBK command: "+ sbkArgsBuilder);
        Printer.log.info("SBK-GEM: Arguments to remote SBK command verification Success..");

        ramConfig.maxConnections = params.getConnections().length;
        final List<String> ramArgsList = new ArrayList<>();
        ramArgsList.add(Config.CLASS_OPTION_ARG);
        ramArgsList.add(className);
        ramArgsList.add("-action");
        ramArgsList.add(actionString);
        ramArgsList.add("-max");
        ramArgsList.add(Integer.toString(params.getConnections().length));

        final String[] ramArgs = ramArgsList.toArray(new String[0]);
        Printer.log.info("Arguments to  SBK-RAM: "+ Arrays.toString(ramArgs));

        ramParams = new SbkRamParameters(appName, params.getRamPort(), params.getConnections().length);
        try {
            ramParams.parseArgs(ramArgs);
        } catch (UnrecognizedOptionException ex) {
            Printer.log.error(ex.toString());
            ramParams.printHelp();
            throw ex;
        }
        Printer.log.info("SBK-GEM: Arguments to SBK-RAM command verification Success..");
        return new SbkGemBenchmark(new SbkRamBenchmark(ramConfig, ramParams, logger, time), gemConfig, params,
                sbkArgsBuilder.toString());
    }


    public static void printRemoteResults(RemoteResponse[] results, boolean all) {
        final String separatorText = "-".repeat(80);
        System.out.println();
        System.out.println("SBK-GEM Remote Results");
        for (int i = 0; i < results.length; i++) {
            System.out.println(separatorText);
            System.out.println("Host "+ (i+1) +": "+results[i].host +", return code: "+results[i].returnCode);
            if (all || results[i].returnCode != 0) {
                System.out.println();
                System.out.println(" : stdout : \n");
                System.out.println(results[i].stdOutput);
                System.out.println(" : stderr : ");
                System.out.println(results[i].errOutput);
            }
        }
        System.out.println(separatorText);
    }


}

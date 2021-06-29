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
import io.sbk.api.Config;
import io.sbk.api.DataType;
import io.sbk.api.HelpException;
import io.sbk.api.Logger;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.api.impl.SbkGrpcPrometheusLogger;
import io.sbk.api.impl.SbkParameters;
import io.sbk.api.impl.SbkUtils;
import io.sbk.gem.GemBenchmark;
import io.sbk.gem.GemConfig;
import io.sbk.gem.GemLogger;
import io.sbk.gem.GemParameterOptions;
import io.sbk.gem.RemoteResponse;
import io.sbk.perl.Time;
import io.sbk.ram.RamConfig;
import io.sbk.ram.RamParameterOptions;
import io.sbk.ram.impl.SbkRamBenchmark;
import io.sbk.ram.impl.SbkRamParameters;
import io.sbk.system.Printer;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang.StringUtils;
import org.reflections.ReflectionsException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , SbkServer is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InterruptedException If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException If an exception occurred.
     * @throws TimeoutException If an exception occurred if an I/O operation is timed out.
     */
    public static RemoteResponse[] run(final String[] args, final String applicationName,
                           GemLogger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException {
        final GemBenchmark benchmark;
        try {
            benchmark = buildBenchmark(args, applicationName, outLogger);
        } catch (HelpException | UnrecognizedOptionException ex) {
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
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , storage name is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws HelpException if '-help' option is supplied.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     */
    public static GemBenchmark buildBenchmark(final String[] args, final String applicationName,
                                              GemLogger outLogger) throws ParseException, IllegalArgumentException,
            IOException, HelpException {
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
        final Storage storageDevice;
        final String usageLine;
        String driverName = null;
        List<String> driversList;

        Printer.log.info(IOUtils.toString(io.sbk.gem.impl.SbkGem.class.getClassLoader().getResourceAsStream(BANNER_FILE)));
        Printer.log.info(GemConfig.DESC);
        Printer.log.info(GemConfig.NAME.toUpperCase() +" Version: "+ Objects.requireNonNullElse(version, ""));
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info(Config.SBK_APP_NAME + ": "+ Objects.requireNonNullElse(sbkAppName, ""));
        Printer.log.info(Config.SBK_CLASS_NAME + ": "+ Objects.requireNonNullElse(sbkClassName, ""));
        Printer.log.info(Config.SBK_APP_HOME+": "+ Objects.requireNonNullElse(sbkAppHome, ""));

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ramConfig = mapper.readValue(io.sbk.ram.impl.SbkRam.class.getClassLoader().getResourceAsStream(RAM_CONFIG_FILE),
                RamConfig.class);
        gemConfig = mapper.readValue(io.sbk.gem.impl.SbkGem.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                GemConfig.class);

        if (StringUtils.isNotEmpty(sbkCommand)) {
            gemConfig.sbkCommand = sbkCommand;
        }

        if (StringUtils.isNotEmpty(sbkAppHome)) {
            gemConfig.sbkPath = sbkAppHome;
        }
        gemConfig.remoteDir = appName;
        if (StringUtils.isNotEmpty(version)) {
            gemConfig.remoteDir += "-"+version;
        }

        logger = Objects.requireNonNullElseGet(outLogger, SbkGemRamPrometheusLogger::new);

        try {
            driversList = SbkUtils.getAvailableStorageClassNames(Config.SBK_PACKAGE_NAME);
            Printer.log.info("Available Drivers: "+ driversList.size());
        } catch (ReflectionsException ex) {
            Printer.log.warn(ex.toString());
            driversList = new LinkedList<>();
        }

        if (StringUtils.isEmpty(className)) {
            Printer.log.warn("SBK-GEM: Storage class not found!");
            storageDevice = null;
        } else {
            if (driversList.size() > 0) {
                driverName = SbkUtils.searchList(driversList, className);
                if (driverName == null) {
                    String msg = "storage driver: " + className + " not found in the SBK";
                    Printer.log.warn(msg);
                    driverName = className;
                }
            } else {
                driverName = className;
            }

            Storage<?> tmp = null;

            final String  packageClassPath =  SbkUtils.getStorageClassPath(Config.SBK_PACKAGE_NAME, driverName);
            if (packageClassPath == null) {
                String errMsg = "The Package class Path not found for the storage driver: "+driverName;
                Printer.log.error(errMsg);
            } else {
                try {
                    tmp = (Storage<?>) Class.forName(packageClassPath).getConstructor().newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                        NoSuchMethodException | InvocationTargetException ex) {
                    String errMsg = "SBK-GEM: storage driver '" + driverName + "' Not available in the package: "
                            + Config.SBK_PACKAGE_NAME;
                    Printer.log.warn(errMsg);
                }
            }
            storageDevice = tmp;
        }

        usageLine = storageDevice != null ? appName + " " + SbkUtils.CLASS_OPTION + " " + driverName : appName;

        params = new SbkGemParameters(usageLine, null, gemConfig, ramConfig.port);
        logger.addArgs(params);
        if (storageDevice != null) {
            storageDevice.addArgs(params);
        }

        final String[] nextArgs = SbkUtils.removeOptionsAndValues(args, new String[]{SbkUtils.CLASS_OPTION});

        if (nextArgs == null) {
            params.printHelp();
            throw new ParseException("SBK-GEM: Insufficient command line arguments");
        }

        String[] processArgs = nextArgs;
        int i = 1;
        boolean checkRemoteArgs = true;

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
                processArgs = SbkUtils.removeOptionsAndValues(processArgs, new String[]{ex.getOption()});
                if (processArgs == null) {
                    params.printHelp();
                    throw new ParseException("SBK-GEM: Insufficient command line arguments");
                }
                checkRemoteArgs = false;
                continue;
            } catch (HelpException ex) {
                System.out.println("\n"+ex.getHelpText());
                throw ex;
            }
            break;
        }

        String actionString = "r";
        if (params.isWriteAndRead()) {
            actionString = "wr";
        } else if (params.getWritersCount() > 0) {
            actionString = "w";
        }

        // remove GEM and logger parameter options
        final String[] sbkArgsList = SbkUtils.removeOptionsAndValues(
                SbkUtils.removeOptionsAndValues(nextArgs, params.getOptionsArgs()), logger.getOptionsArgs());
        final StringBuilder sbkArgsBuilder = new StringBuilder(SbkUtils.CLASS_OPTION + " " + driverName);
        for (String arg: sbkArgsList) {
            sbkArgsBuilder.append(" ");
            sbkArgsBuilder.append(arg);
        }
        time = SbkUtils.getTime(logger);
        sbkArgsBuilder.append(" -time ").append(time.getTimeUnit().name());
        sbkArgsBuilder.append(" -context no");
        sbkArgsBuilder.append(" -ram " + params.getHostName());
        sbkArgsBuilder.append(" -ramport " + params.getRamPort());

        Printer.log.info("SBK dir: "+params.getSbkDir());
        Printer.log.info("SBK command: "+params.getSbkCommand());
        Printer.log.info("Arguments to remote SBK command: "+ sbkArgsBuilder);
        if (checkRemoteArgs && StringUtils.isNotEmpty(driverName)) {
            checkRemoteSbkArgs(sbkAppName, driverName, driversList, sbkArgsBuilder.toString().split(" "));
        }
        Printer.log.info("SBK-GEM: Arguments to remote SBK command verification success..");

        ramConfig.maxConnections = params.getConnections().length;
        final List<String> ramArgsList = new ArrayList<>();
        ramArgsList.add(SbkUtils.CLASS_OPTION);
        ramArgsList.add(driverName);
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
        Printer.log.info("SBK-GEM: Arguments to SBK-RAM command verification success..");
        return new SbkGemBenchmark(new SbkRamBenchmark(ramConfig, ramParams, logger, time), gemConfig, params,
                sbkArgsBuilder.toString());
    }


    private static void checkRemoteSbkArgs(String sbkAppName, String storageName, List<String> driversList,
                                           String[] args) throws  ParseException, HelpException {
        final String remoteUsageLine = sbkAppName + " " + SbkUtils.CLASS_OPTION + " " + storageName;
        final ParameterOptions sbkParams = new SbkParameters(remoteUsageLine, driversList);
        final Logger grpcLogger = new SbkGrpcPrometheusLogger();
        Storage<?> tmp = null;

        final String  packageClassPath =  SbkUtils.getStorageClassPath(Config.SBK_PACKAGE_NAME, storageName);
        if (packageClassPath == null) {
            String errMsg = "The Package class Path not found for the storage driver: "+storageName;
            Printer.log.error(errMsg);
        } else {
            try {
                tmp = (Storage<?>) Class.forName(packageClassPath).getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
                    | ClassNotFoundException ex) {
                Printer.log.warn("SBK-GEM: storage class '"+storageName+"' not found in the package "+Config.SBK_PACKAGE_NAME);
            }
        }

        final Storage<?> remoteStorage = tmp;

        grpcLogger.addArgs(sbkParams);

        if (remoteStorage != null) {
            remoteStorage.addArgs(sbkParams);
        }

        sbkParams.parseArgs(args);
        grpcLogger.parseArgs(sbkParams);

        if (remoteStorage != null) {
            remoteStorage.parseArgs(sbkParams);
            final DataType<?> dType = remoteStorage.getDataType();
            if (dType == null) {
                String errMsg = "No storage Data type of Remote Storage device: "+ storageName;
                Printer.log.error(errMsg);
                throw new ParseException(errMsg);
            }

            int minSize = dType.getWriteReadMinSize();
            if (sbkParams.isWriteAndRead() && sbkParams.getRecordSize() < minSize) {
                String errMsg =
                        "Invalid record size: "+ sbkParams.getRecordSize() +
                                ", For both Writers and Readers, minimum data size should be "+ minSize +
                                " for data type: " +dType.getClass().getName();
                Printer.log.error(errMsg);
                throw new ParseException(errMsg);
            }
        }
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

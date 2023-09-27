/**
 * Copyright (c) KMG. All Rights Reserved..
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
import io.perl.api.impl.PerlBuilder;
import io.sbk.action.Action;
import io.sbk.api.Storage;
import io.sbk.api.StoragePackage;
import io.sbk.gem.GemLoggerPackage;
import io.sbk.logger.impl.GemSbmPrometheusLogger;
import io.sbk.params.InputParameterOptions;
import io.sbk.params.impl.SbkDriversParameters;
import io.sbm.logger.RamLogger;
import io.sbm.logger.impl.SbmPrometheusLogger;
import io.sbk.utils.SbkUtils;
import io.sbk.config.Config;
import io.sbk.config.GemConfig;
import io.sbm.config.SbmConfig;
import io.sbk.data.DataType;
import io.sbk.exception.HelpException;
import io.sbk.gem.GemBenchmark;
import io.sbk.params.GemParameterOptions;
import io.sbk.gem.RemoteResponse;
import io.sbk.logger.GemLogger;
import io.sbm.params.RamParameterOptions;
import io.sbk.params.impl.SbkGemParameters;
import io.sbm.api.impl.Sbm;
import io.sbm.api.impl.SbmBenchmark;
import io.sbm.params.impl.SbmParameters;
import io.sbk.system.Printer;
import io.sbp.api.Sbp;
import io.sbp.config.SbpVersion;
import io.time.Time;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

/**
 * Class SbkGem.
 */
final public class SbkGem {
    final static String CONFIG_FILE = "gem.properties";
    final static String SBM_CONFIG_FILE = "sbm.properties";
    final static String BANNER_FILE = "gem-banner.txt";

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
     */
    public static RemoteResponse[] run(final String[] args, final String applicationName, final String storagePackageName,
                                       String loggerPackageName) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, InstantiationException, ExecutionException, TimeoutException,
            ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final GemBenchmark benchmark;
        try {
            benchmark = buildBenchmark(args, applicationName, storagePackageName, loggerPackageName);
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
     * @param args               command line arguments.
     * @param applicationName    Name of the application. will be used in the 'help' message.
     *                           if it is 'null' , storage name is used by default.
     * @param storagePackageName Name of the package where storage class is available.
     *                           If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param loggerPackageName  Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                           logger will be used.
     * @return Benchmark Interface
     * @throws HelpException  if '-help' option is supplied.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IOException    If an exception occurred due to write or read failures.
     */
    @Contract("_, _, _, _ -> new")
    public static @NotNull GemBenchmark buildBenchmark(final String[] args, final String applicationName,
                                                       final String storagePackageName, final String loggerPackageName)
            throws ParseException, IOException, HelpException, ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException, InstantiationException {
        final GemParameterOptions params;
        final RamParameterOptions ramParams;
        final GemConfig gemConfig;
        final GemLogger logger;
        final RamLogger ramLogger;
        final SbmConfig sbmConfig;
        final Time time;
        final String version = io.sbk.gem.impl.SbkGem.class.getPackage().getImplementationVersion();
        final String sbkGemAppName = System.getProperty(GemConfig.SBK_GEM_APP_NAME);
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName :
                StringUtils.isNotEmpty(sbkGemAppName) ? sbkGemAppName : GemConfig.NAME;
        final String sbkAppName = System.getProperty(Config.SBK_APP_NAME);
        final String sbkCommand = StringUtils.isNotEmpty(sbkAppName) ? sbkAppName : Config.NAME;
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        final String argsClassName = SbkUtils.getClassName(args);
        final String argsLoggerName = SbkUtils.getLoggerName(args);
        final String className = StringUtils.isNotEmpty(argsClassName) ? argsClassName : sbkClassName;
        final String sbkStoragePackageName = StringUtils.isNotEmpty(storagePackageName) ?
                storagePackageName : Config.SBK_STORAGE_PACKAGE_NAME;
        final String gemLoggerPackageName = StringUtils.isNotEmpty(loggerPackageName) ?
                loggerPackageName : GemConfig.SBK_GEM_LOGGER_PACKAGE_NAME;
        final StoragePackage packageStore = new StoragePackage(sbkStoragePackageName);
        final GemLoggerPackage loggerStore = new GemLoggerPackage(gemLoggerPackageName);
        final SbpVersion sbpVersion = Sbp.getVersion();
        final Storage storageDevice;
        final String usageLine;
        final String[] storageDrivers;
        final String[] nextArgs;
        final String[] loggerNames;

        Printer.log.info(IOUtils.toString(io.sbk.gem.impl.SbkGem.class.getClassLoader().getResourceAsStream(BANNER_FILE)));
        Printer.log.info(GemConfig.DESC);
        Printer.log.info(GemConfig.NAME.toUpperCase() + " Version: " + Objects.requireNonNullElse(version, ""));
        Printer.log.info(GemConfig.NAME.toUpperCase() + " Website: " + Config.SBK_WEBSITE_NAME);
        Printer.log.info("Arguments List: " + Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info("SBP Version Major: " + sbpVersion.major+", Minor: "+sbpVersion.minor);
        Printer.log.info("Storage Drivers Package: " + sbkStoragePackageName);
        Printer.log.info(Config.SBK_APP_NAME + ": " + Objects.requireNonNullElse(sbkAppName, ""));
        Printer.log.info(Config.SBK_CLASS_NAME + ": " + Objects.requireNonNullElse(sbkClassName, ""));
        Printer.log.info(Config.SBK_APP_HOME + ": " + Objects.requireNonNullElse(sbkAppHome, ""));
        Printer.log.info("'" + Config.CLASS_OPTION_ARG + "': " + Objects.requireNonNullElse(argsClassName, ""));
        packageStore.printClasses("Storage");
        loggerStore.printClasses("Gem Logger");

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        sbmConfig = mapper.readValue(Sbm.class.getClassLoader().getResourceAsStream(SBM_CONFIG_FILE),
                SbmConfig.class);
        gemConfig = mapper.readValue(io.sbk.gem.impl.SbkGem.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                GemConfig.class);

        usageLine = StringUtils.isNotEmpty(argsClassName) ? appName + " " + Config.CLASS_OPTION_ARG + " " + argsClassName :
                appName;

        if (StringUtils.isEmpty(argsLoggerName)) {
            logger = new GemSbmPrometheusLogger();
            ramLogger = new GemSbmPrometheusLogger();
            String[] loggers = loggerStore.getClassNames();
            if(loggers != null && loggers.length > 0) {
                loggerNames = loggers;
            } else {
                loggerNames = new String[]{logger.getClass().getSimpleName()};
                Printer.log.error("No logger classes found from the package : "+gemLoggerPackageName +
                        " default logger "+ Arrays.toString(loggerNames));
            }
        } else {
            loggerNames = loggerStore.getClassNames();
            try {
                logger = loggerStore.getClass(argsLoggerName);
                ramLogger = loggerStore.getClass(argsLoggerName);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                     | IllegalAccessException | InstantiationException ex) {
                Printer.log.error("Instantiation of Logger class '" + argsLoggerName + "' from the package '" +
                        gemLoggerPackageName + "' failed!, " + "error: " + ex);
                final InputParameterOptions helpParams = new SbkDriversParameters(usageLine,
                        packageStore.getClassNames(), loggerNames);
                helpParams.printHelp();
                throw ex;
            }
        }


        nextArgs = SbkUtils.removeOptionArgsAndValues(args, new String[]{Config.CLASS_OPTION_ARG});

        if (StringUtils.isNotEmpty(sbkCommand)) {
            gemConfig.sbkcommand = GemConfig.BIN_DIR + File.separator + sbkCommand;
        }

        if (StringUtils.isNotEmpty(sbkAppHome)) {
            gemConfig.sbkdir = sbkAppHome;
        }
        gemConfig.remoteDir = appName;
        if (StringUtils.isNotEmpty(version)) {
            gemConfig.remoteDir += "-" + version;
        }

        if (StringUtils.isEmpty(className)) {
            storageDevice = null;
        } else {
            Storage<?> device = null;
            try {
                device = packageStore.getClass(className);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                    IllegalAccessException | InstantiationException ex) {
                Printer.log.warn("Instantiation of storage class '" + className + "' from the package '" +
                        sbkStoragePackageName + "' failed!, " + "error: " + ex);
            }
            storageDevice = device;
        }

        storageDrivers = storageDevice == null ? packageStore.getClassNames() : null;

        params = new SbkGemParameters(usageLine, storageDrivers, loggerNames, gemConfig, sbmConfig.port);
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
            Printer.log.info("SBK-GEM [" + i + "]: Arguments to process : " + Arrays.toString(processArgs));
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
                System.out.println("\n" + ex.getHelpText());
                throw ex;
            }
            break;
        }

        if (storageDevice != null) {
            final DataType<?> dType = storageDevice.getDataType();
            if (dType == null) {
                String errMsg = "No storage Data type of Remote Storage device: " + className;
                Printer.log.error(errMsg);
                throw new ParseException(errMsg);
            }

            int minSize = dType.getWriteReadMinSize();
            if (params.getAction() == Action.Write_Reading && params.getRecordSize() < minSize) {
                String errMsg =
                        "Invalid record size: " + params.getRecordSize() +
                                ", For both Writers and Readers, minimum data size should be " + minSize +
                                " for data type: " + dType.getClass().getName();
                Printer.log.error(errMsg);
                throw new ParseException(errMsg);
            }
        }

        final String actionString = switch (params.getAction()) {
            case Writing -> "w";
            case Write_Reading -> "wr";
            case Write_OnlyReading -> "wro";
            default -> "r";
        };

        // remove GEM and logger parameter options
        final String[] sbkArgsList = SbkUtils.removeOptionArgsAndValues(
                SbkUtils.removeOptionArgsAndValues(nextArgs, params.getOptionsArgs()), logger.getOptionsArgs());
        final StringBuilder sbkArgsBuilder = new StringBuilder(Config.CLASS_OPTION_ARG + " " + className);
        for (String arg : sbkArgsList) {
            sbkArgsBuilder.append(" ");
            sbkArgsBuilder.append(arg);
        }
        time = PerlBuilder.buildTime(logger);
        sbkArgsBuilder.append(" -time ").append(time.getTimeUnit().name());
        sbkArgsBuilder.append(" -minlatency ").append(logger.getMinLatency());
        sbkArgsBuilder.append(" -maxlatency ").append(logger.getMaxLatency());
        sbkArgsBuilder.append(" -wq ").append(logger.getMaxWriterIDs() > 0);
        sbkArgsBuilder.append(" -rq ").append(logger.getMaxReaderIDs() > 0);
        sbkArgsBuilder.append(" -context no");
        sbkArgsBuilder.append(" -sbm ").append(params.getLocalHost());
        sbkArgsBuilder.append(" -sbmport ").append(params.getSbmPort());

        Printer.log.info("SBK dir: " + params.getSbkDir());
        Printer.log.info("SBK command: " + params.getSbkCommand());
        Printer.log.info("Arguments to remote SBK command: " + sbkArgsBuilder);
        Printer.log.info("SBK-GEM: Arguments to remote SBK command verification Success..");

        sbmConfig.maxConnections = params.getConnections().length;
        final List<String> ramArgsList = new ArrayList<>();
        ramArgsList.add(Config.CLASS_OPTION_ARG);
        ramArgsList.add(className);
        ramArgsList.add("-action");
        ramArgsList.add(actionString);
        ramArgsList.add("-time");
        ramArgsList.add(time.getTimeUnit().name());
        ramArgsList.add("-minlatency");
        ramArgsList.add(String.valueOf(logger.getMinLatency()));
        ramArgsList.add("-maxlatency");
        ramArgsList.add(String.valueOf(logger.getMaxLatency()));
        ramArgsList.add("-port");
        ramArgsList.add(String.valueOf(params.getSbmPort()));
        ramArgsList.add("-wq");
        ramArgsList.add(String.valueOf(logger.getMaxWriterIDs() > 0));
        ramArgsList.add("-rq");
        ramArgsList.add(String.valueOf(logger.getMaxReaderIDs() > 0));
        ramArgsList.add("-max");
        ramArgsList.add(Integer.toString(params.getConnections().length));

        final String[] ramArgs = ramArgsList.toArray(new String[0]);
        Printer.log.info("Arguments to SBM: " + Arrays.toString(ramArgs));

        ramParams = new SbmParameters(appName, params.getSbmPort(), params.getConnections().length,
                new String[]{ramLogger.getClass().getSimpleName()});
        ramLogger.addArgs(ramParams);
        try {
            ramParams.parseArgs(ramArgs);
            ramLogger.parseArgs(ramParams);
        } catch (UnrecognizedOptionException ex) {
            Printer.log.error(ex.toString());
            ramParams.printHelp();
            throw ex;
        }
        Printer.log.info("SBK-GEM: Arguments to SBM command verification Success..");
        return new SbkGemBenchmark(new SbmBenchmark(sbmConfig, ramParams, ramLogger, time), gemConfig, params,
                sbkArgsBuilder.toString());
    }


    /**
     * This method prints Remote Results.
     *
     * @param results NotNull RemoteResponse[]
     * @param all     boolean
     */
    public static void printRemoteResults(@NotNull RemoteResponse[] results, boolean all) {
        final String separatorText = "-".repeat(80);
        System.out.println();
        System.out.println("SBK-GEM Remote Results");
        for (int i = 0; i < results.length; i++) {
            System.out.println(separatorText);
            System.out.println("Host " + (i + 1) + ": " + results[i].host + ", return code: " + results[i].returnCode);
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

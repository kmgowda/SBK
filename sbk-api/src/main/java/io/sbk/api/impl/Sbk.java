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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.util.IOUtils;
import io.perl.api.impl.PerlBuilder;
import io.sbk.action.Action;
import io.sbk.api.Benchmark;
import io.sbk.api.RWLoggerPackage;
import io.sbk.logger.impl.SystemLogger;
import io.sbk.params.InputParameterOptions;
import io.sbk.api.Storage;
import io.sbk.api.StoragePackage;
import io.sbk.config.Config;
import io.sbk.data.DataType;
import io.sbk.exception.HelpException;
import io.sbk.logger.RWLogger;
import io.sbk.params.impl.SbkDriversParameters;
import io.sbk.params.impl.SbkParameters;
import io.sbk.system.Printer;
import io.sbk.utils.SbkUtils;
import io.time.Time;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


/**
 * Main class of SBK.
 */
final public class Sbk {
    final static String BANNERFILE = "banner.txt";

    /**
     * Run the Performance Benchmarking .
     *
     * @param args               command line arguments.
     * @param applicationName    name of the application. will be used in the 'help' message. if it is 'null' , name
     *                           'sbk' used as default.
     * @param storagePackageName the name of the package where storage class is available.
     *                           If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param loggerPackageName  Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                           logger will be used.
     * @throws ParseException            If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException  If an exception occurred due to invalid arguments.
     * @throws IOException               If an exception occurred due to write or read failures.
     * @throws InstantiationException    if the exception occurred due to initiation failures.
     * @throws InterruptedException      If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException        If an exception occurred.
     * @throws TimeoutException          If an exception occurred if an I/O operation is timed out.
     * @throws ClassNotFoundException    if the supplied storage class is not found.
     * @throws InvocationTargetException if the exception occurs.
     * @throws NoSuchMethodException     if the exception occurs.
     * @throws IllegalAccessException    if the exception occurs.
     */
    public static void run(final String[] args, final String applicationName, final String storagePackageName,
                           String loggerPackageName) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException, InstantiationException,
            ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final Benchmark benchmark;
        try {
            benchmark = buildBenchmark(args, applicationName, storagePackageName, loggerPackageName);
        } catch (HelpException ex) {
            return;
        }

        final CompletableFuture<Void> ret = benchmark.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            benchmark.stop();
        }));
        ret.get();
    }


    /**
     * Build the Benchmark Object.
     *
     * @param args               command line arguments.
     * @param applicationName    name of the application. will be used in the 'help' message. if it is 'null' , name
     *                           'sbk' used as default.
     * @param storagePackageName Name of the package where storage class is available.
     *                           If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param loggerPackageName  Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                           logger will be used.
     * @return Benchmark interface
     * @throws HelpException             if '-help' option is supplied.
     * @throws ParseException            If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException  If an exception occurred due to invalid arguments.
     * @throws IOException               If an exception occurred due to write or read failures.
     * @throws InstantiationException    if the exception occurred due to initiation failures.
     * @throws ClassNotFoundException    If the storage class driver is not found.
     * @throws InvocationTargetException if the exception occurs.
     * @throws NoSuchMethodException     if the exception occurs.
     * @throws IllegalAccessException    if the exception occurs.
     */
    @Contract("_, _, _, _ -> new")
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public static @NotNull Benchmark buildBenchmark(final String[] args, final String applicationName,
                                                    final String storagePackageName,
                                                    final String loggerPackageName) throws ParseException,
            IllegalArgumentException, IOException, InstantiationException, HelpException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final String version = io.sbk.api.impl.Sbk.class.getPackage().getImplementationVersion();
        final String sbkApplicationName = System.getProperty(Config.SBK_APP_NAME);
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName :
                StringUtils.isNotEmpty(sbkApplicationName) ? sbkApplicationName : Config.NAME;
        final String sbkStoragePackageName = StringUtils.isNotEmpty(storagePackageName) ?
                storagePackageName : Config.SBK_STORAGE_PACKAGE_NAME;
        final String sbkLoggerPackageName = StringUtils.isNotEmpty(loggerPackageName) ?
                loggerPackageName : Config.SBK_LOGGER_PACKAGE_NAME;
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        final String argsClassName = SbkUtils.getClassName(args);
        final String argsLoggerName = SbkUtils.getLoggerName(args);
        final String className = StringUtils.isNotEmpty(argsClassName) ? argsClassName :
                Objects.requireNonNullElse(sbkClassName, "");
        final StoragePackage packageStore = new StoragePackage(sbkStoragePackageName);
        final RWLoggerPackage loggerStore = new RWLoggerPackage(sbkLoggerPackageName);
        final Storage storageDevice;
        final InputParameterOptions params;
        final RWLogger rwLogger;
        final Time time;
        final String[] nextArgs;
        final String[] loggerNames;
        String usageLine;

        Printer.log.info(IOUtils.toString(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(BANNERFILE)));
        Printer.log.info(Config.DESC);
        Printer.log.info(Config.NAME.toUpperCase() + " Version: " + version);
        Printer.log.info(Config.NAME.toUpperCase() + " Website: " + Config.SBK_WEBSITE_NAME);
        Printer.log.info("Arguments List: " + Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info("Storage Drivers Package: " + sbkStoragePackageName);
        Printer.log.info("Logger Package: " + sbkLoggerPackageName);
        Printer.log.info(Config.SBK_APP_NAME + ": " + Objects.requireNonNullElse(sbkApplicationName, ""));
        Printer.log.info(Config.SBK_APP_HOME + ": " + Objects.requireNonNullElse(sbkAppHome, ""));
        Printer.log.info(Config.SBK_CLASS_NAME + ": " + Objects.requireNonNullElse(sbkClassName, ""));
        Printer.log.info("'" + Config.CLASS_OPTION_ARG + "': " + argsClassName);
        Printer.log.info("'" + Config.LOGGER_OPTION_ARG + "': " + argsLoggerName);
        packageStore.printClasses("Storage");
        loggerStore.printClasses("Logger");

        usageLine = StringUtils.isNotEmpty(argsClassName) ?
                appName + " " + Config.CLASS_OPTION_ARG + " " + argsClassName : appName;

        if (StringUtils.isEmpty(argsLoggerName)) {
            rwLogger = new SystemLogger();
            usageLine = usageLine+" "+Config.LOGGER_OPTION_ARG+ " "+rwLogger.getClass().getSimpleName();
            String[] loggers = loggerStore.getClassNames();
            if (loggers != null && loggers.length > 0) {
                loggerNames = loggers;
                Printer.log.warn("The option '-"+ Config.LOGGER_OPTION +"' is not supplied;"+
                        " default logger: "+ rwLogger.getClass().getSimpleName());
            } else {
                loggerNames = new String[]{rwLogger.getClass().getSimpleName()};
                Printer.log.error("No logger class found from the package: "+sbkLoggerPackageName +
                        "; default logger: "+ rwLogger.getClass().getSimpleName());
            }
        } else {
            usageLine = usageLine+" "+Config.LOGGER_OPTION_ARG+ " "+argsLoggerName;
            loggerNames = loggerStore.getClassNames();
            try {
                rwLogger = loggerStore.getClass(argsLoggerName);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                     | IllegalAccessException | InstantiationException ex) {
                Printer.log.error("Instantiation of Logger class '" + argsLoggerName + "' from the package '" +
                        sbkLoggerPackageName + "' failed!, " + "error: " + ex);
                final InputParameterOptions helpParams = new SbkDriversParameters(usageLine,
                        packageStore.getClassNames(), loggerNames);
                helpParams.printHelp();
                throw ex;
            }
        }

        nextArgs = SbkUtils.removeOptionArgsAndValues(args, new String[]{Config.CLASS_OPTION_ARG, Config.LOGGER_OPTION_ARG});
        if (StringUtils.isEmpty(className)) {
            final InputParameterOptions helpParams = new SbkDriversParameters(usageLine,
                    packageStore.getClassNames(), loggerNames);
            rwLogger.addArgs(helpParams);
            final String helpText = helpParams.getHelpText();
            System.out.println("\n" + helpText);
            if (nextArgs.length == 0 || SbkUtils.hasHelp(nextArgs)) {
                throw new HelpException(helpText);
            }
            throw new ParseException("The option '-"+Config.CLASS_OPTION+"' is not supplied");
        } else {
            try {
                storageDevice = packageStore.getClass(className);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                    | IllegalAccessException ex) {
                Printer.log.error("Instantiation of storage class '" + className + "' from the package '" +
                        sbkStoragePackageName + "' failed!, " + "error: " + ex);
                final InputParameterOptions helpParams = new SbkDriversParameters(usageLine,
                        packageStore.getClassNames(), loggerNames);
                rwLogger.addArgs(helpParams);
                helpParams.printHelp();
                throw ex;
            }
        }

        Printer.log.info("Arguments to Driver '" + storageDevice.getClass().getSimpleName() + "' : " +
                Arrays.toString(nextArgs));

        params = new SbkParameters(usageLine);
        rwLogger.addArgs(params);
        storageDevice.addArgs(params);

        if (nextArgs.length == 0 || SbkUtils.hasHelp(nextArgs)) {
            final String helpText = params.getHelpText();
            System.out.println("\n" + helpText);
            throw new HelpException(helpText);
        }

        try {
            params.parseArgs(nextArgs);
            rwLogger.parseArgs(params);
            storageDevice.parseArgs(params);
        } catch (UnrecognizedOptionException ex) {
            params.printHelp();
            Printer.log.error(ex.toString());
            throw ex;
        } catch (HelpException ex) {
            System.out.println("\n" + ex.getHelpText());
            throw ex;
        }

        final DataType dType = storageDevice.getDataType();
        if (dType == null) {
            String errMsg = "No storage Data type";
            Printer.log.error(errMsg);
            throw new InstantiationException(errMsg);
        }

        int minSize = dType.getWriteReadMinSize();
        if (params.getAction() == Action.Write_Reading && params.getRecordSize() < minSize) {
            String errMsg =
                    "Invalid record size: " + params.getRecordSize() +
                            ", For both Writers and Readers, minimum data size should be " + minSize +
                            " for data type: " + dType.getClass().getName();
            Printer.log.error(errMsg);
            throw new InstantiationException(errMsg);
        }
        time = PerlBuilder.buildTime(rwLogger);

        return new SbkBenchmark(params, storageDevice, dType, rwLogger, time);
    }

}

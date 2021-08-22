/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.util.IOUtils;
import io.sbk.action.Action;
import io.sbk.api.Benchmark;
import io.sbk.config.Config;
import io.sbk.data.DataType;
import io.sbk.exception.HelpException;
import io.sbk.api.ParameterOptions;
import io.sbk.logger.Logger;
import io.sbk.api.StoragePackage;
import io.sbk.logger.impl.GrpcPrometheusLogger;
import io.sbk.config.PerlConfig;
import io.sbk.api.Storage;
import io.sbk.time.Time;
import io.sbk.system.Printer;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang.StringUtils;

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
public class Sbk {
    final static String CONFIGFILE = "sbk.properties";
    final static String BANNERFILE = "banner.txt";

    /**
     * Run the Performance Benchmarking .
     * @param args command line arguments.
     * @param packageName  the name of the package where storage class is available.
     *                     If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , name
     *                        'sbk' used as default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InstantiationException if the exception occurred due to initiation failures.
     * @throws InterruptedException If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException If an exception occurred.
     * @throws TimeoutException If an exception occurred if an I/O operation is timed out.
     * @throws ClassNotFoundException if the supplied storage class is not found.
     * @throws InvocationTargetException if the exception occurs.
     * @throws NoSuchMethodException if the exception occurs.
     * @throws IllegalAccessException if the exception occurs.
     */
    public static void run(final String[] args, final String packageName,
                           final String applicationName, Logger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException, InstantiationException,
            ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final Benchmark benchmark;
        try {
            benchmark = buildBenchmark(args, packageName, applicationName, outLogger);
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
     * @param args command line arguments.
     * @param packageName  Name of the package where storage class is available.
     *                     If you pass null to this parameter, then default package name "io.sbk" is used.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , name
     *                        'sbk' used as default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws HelpException if '-help' option is supplied.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InstantiationException if the exception occurred due to initiation failures.
     * @throws ClassNotFoundException If the storage class driver is not found.
     * @throws InvocationTargetException if the exception occurs.
     * @throws NoSuchMethodException if the exception occurs.
     * @throws IllegalAccessException if the exception occurs.
     * @return Benchmark interface
     */
    public static Benchmark buildBenchmark(final String[] args, final String packageName,
                                           final String applicationName, final Logger outLogger) throws ParseException,
            IllegalArgumentException, IOException, InstantiationException, HelpException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final String version = io.sbk.api.impl.Sbk.class.getPackage().getImplementationVersion();
        final String sbkApplicationName = System.getProperty(Config.SBK_APP_NAME);
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName :
                StringUtils.isNotEmpty(sbkApplicationName) ? sbkApplicationName : Config.NAME;
        final String storagePackageName =  StringUtils.isNotEmpty(packageName) ? packageName : Config.SBK_PACKAGE_NAME;
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        final String argsClassName = SbkUtils.getClassName(args);
        final String className = StringUtils.isNotEmpty(argsClassName) ? argsClassName :
                Objects.requireNonNullElse(sbkClassName, "");
        final StoragePackage packageStore = new StoragePackage(storagePackageName);
        final Storage storageDevice;
        final Action action;
        final ParameterOptions params;
        final Logger logger;
        final PerlConfig perlConfig;
        final Time time;
        final String[] nextArgs;
        final String usageLine;

        Printer.log.info(IOUtils.toString(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(BANNERFILE)));
        Printer.log.info(Config.DESC);
        Printer.log.info(Config.NAME.toUpperCase() +" Version: "+version);
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info("Storage Drivers Package: "+ storagePackageName);
        Printer.log.info(Config.SBK_APP_NAME + ": "+   Objects.requireNonNullElse(sbkApplicationName, ""));
        Printer.log.info(Config.SBK_APP_HOME+": "+ Objects.requireNonNullElse(sbkAppHome, ""));
        Printer.log.info(Config.SBK_CLASS_NAME + ": "+ Objects.requireNonNullElse(sbkClassName, ""));
        Printer.log.info("'"+Config.CLASS_OPTION_ARG +"': "+ argsClassName);
        packageStore.printDrivers();

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        perlConfig = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                PerlConfig.class);

        // No CSV backup
        perlConfig.csv = false;

        logger = Objects.requireNonNullElseGet(outLogger, GrpcPrometheusLogger::new);
        usageLine = StringUtils.isNotEmpty(argsClassName) ?
                appName + " " + Config.CLASS_OPTION_ARG + " " + argsClassName : appName;
        nextArgs = SbkUtils.removeOptionArgsAndValues(args, new String[]{Config.CLASS_OPTION_ARG});

        if (StringUtils.isEmpty(className)) {
            final ParameterOptions helpParams = new SbkDriversParameters(usageLine, packageStore.getDrivers());
            logger.addArgs(helpParams);
            final String helpText = helpParams.getHelpText();
            System.out.println("\n" + helpText);
            if (nextArgs.length == 0 || SbkUtils.hasHelp(nextArgs)) {
                throw new HelpException(helpText);
            }
            throw new ParseException("The option '-class' is not supplied");
        } else {
            try {
                storageDevice = packageStore.getStorage(className);
            } catch (ClassNotFoundException | NoSuchMethodException |  InvocationTargetException
                    | IllegalAccessException ex) {
                Printer.log.error("Instantiation of storage class '"+className+ "' from the package '" +
                        storagePackageName + "' failed!, " + "error: " + ex);
                final ParameterOptions helpParams = new SbkDriversParameters(usageLine, packageStore.getDrivers());
                logger.addArgs(helpParams);
                helpParams.printHelp();
                throw ex;
            }
        }

        Printer.log.info("Arguments to Driver '" + storageDevice.getClass().getSimpleName() + "' : " +
                Arrays.toString(nextArgs));

        params = new SbkParameters(usageLine);
        logger.addArgs(params);
        storageDevice.addArgs(params);

        if (nextArgs.length == 0 || SbkUtils.hasHelp(nextArgs)) {
            final String helpText = params.getHelpText();
            System.out.println("\n" + helpText);
            throw new HelpException(helpText);
        }

        try {
            params.parseArgs(nextArgs);
            logger.parseArgs(params);
            storageDevice.parseArgs(params);
        } catch (UnrecognizedOptionException ex) {
            params.printHelp();
            Printer.log.error(ex.toString());
            throw ex;
        } catch (HelpException ex) {
            System.out.println("\n"+ex.getHelpText());
            throw ex;
        }

        final DataType dType = storageDevice.getDataType();
        if (dType == null) {
            String errMsg = "No storage Data type";
            Printer.log.error(errMsg);
            throw new InstantiationException(errMsg);
        }

        int minSize = dType.getWriteReadMinSize();
        if (params.isWriteAndRead() && params.getRecordSize() < minSize) {
            String errMsg =
                    "Invalid record size: "+ params.getRecordSize() +
                            ", For both Writers and Readers, minimum data size should be "+ minSize +
                            " for data type: " +dType.getClass().getName();
            Printer.log.error(errMsg);
            throw new InstantiationException(errMsg);
        }
        time = SbkUtils.getTime(logger);
        if (params.getReadersCount() > 0) {
            if (params.isWriteAndRead()) {
                action = Action.Write_Reading;
            } else {
                action = Action.Reading;
            }
        } else {
            action = Action.Writing;
        }
        return new SbkBenchmark(action, perlConfig, params, storageDevice, dType, logger, time);
    }

}

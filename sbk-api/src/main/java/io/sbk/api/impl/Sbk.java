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
import io.sbk.api.Action;
import io.sbk.api.Benchmark;
import io.sbk.api.Config;
import io.sbk.api.DataType;
import io.sbk.api.HelpException;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Logger;
import io.sbk.api.StoragePackage;
import io.sbk.perl.PerlConfig;
import io.sbk.api.Storage;
import io.sbk.perl.Time;
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
     * @param storage storage object on which performance benchmarking will be conducted.
     *                if you pass 'null', then name of the storage should be in args '-class' arguments
     *                and storage object should be available in the package 'packageName'.
     * @param packageName  the name of the package where storage object is located. if you are passing null as
     *                     'storage' parameter, then you can specify where the storage class/object is available using
     *                     this parameter. If you pass null to this parameter, then default package name "io.sbk" is
     *                     used.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , storage name is used by default.
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
     */
    public static void run(final String[] args, final Storage<Object> storage, final String packageName,
                           final String applicationName, Logger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException, InstantiationException,
            ClassNotFoundException {
        final Benchmark benchmark;
        try {
            benchmark = buildBenchmark(args, storage, packageName, applicationName, outLogger);
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
     * @param storage storage object on which performance benchmarking will be conducted.
     *                if you pass 'null', then name of the storage should be in args '-class' arguments
     *                and storage object should be available in the package 'packageName'.
     * @param packageName  the name of the package where storage object is located. if you are passing null as
     *                     'storage' parameter, then you can specify where the storage class/object is available using
     *                     this parameter. If you pass null to this parameter, then default package name "io.sbk" is
     *                     used.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , storage name is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws HelpException if '-help' option is supplied.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InstantiationException if the exception occurred due to initiation failures.
     * @throws ClassNotFoundException If the storage class driver is not found.
     */
    public static Benchmark buildBenchmark(final String[] args, final Storage<Object> storage,
                           final String packageName, final String applicationName, final Logger outLogger)
            throws ParseException, IllegalArgumentException, IOException, InstantiationException,
            HelpException, ClassNotFoundException {
        final Storage storageDevice;
        final Action action;
        final ParameterOptions params;
        final Logger logger;
        final PerlConfig perlConfig;
        final Time time;
        final String version = io.sbk.api.impl.Sbk.class.getPackage().getImplementationVersion();
        final String sbkApplicationName = System.getProperty(Config.SBK_APP_NAME);
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName :
                StringUtils.isNotEmpty(sbkApplicationName) ? sbkApplicationName : Config.NAME;
        final String storagePackageName =  StringUtils.isNotEmpty(packageName) ? packageName : Config.SBK_PACKAGE_NAME;
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        final String argsClassName = SbkUtils.getClassName(args);
        final String className = StringUtils.isNotEmpty(argsClassName) ? argsClassName : sbkClassName;
        final StoragePackage packageStore = new StoragePackage(storagePackageName);
        final String usageLine;
        final String[] storageDrivers;

        Printer.log.info(IOUtils.toString(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(BANNERFILE)));
        Printer.log.info(Config.DESC);
        Printer.log.info(Config.NAME.toUpperCase() +" Version: "+version);
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info(Config.SBK_APP_NAME + ": "+   Objects.requireNonNullElse(sbkApplicationName, ""));
        Printer.log.info(Config.SBK_CLASS_NAME + ": "+ Objects.requireNonNullElse(sbkClassName, ""));
        Printer.log.info(Config.SBK_APP_HOME+": "+ Objects.requireNonNullElse(sbkAppHome, ""));
        packageStore.printDrivers();

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        perlConfig = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                PerlConfig.class);
        logger = Objects.requireNonNullElseGet(outLogger, SbkGrpcPrometheusLogger::new);

        if (storage == null) {
            if (StringUtils.isEmpty(className)) {
                storageDevice = null;
            } else {
                Storage<?> device = null;
                try {
                    device = packageStore.getStorage(className);
                } catch (ClassNotFoundException | NoSuchMethodException |  InvocationTargetException
                        | IllegalAccessException ex) {
                    Printer.log.warn("Instantiation of storage class '"+className+ "' from the package '" +
                            storagePackageName + "' failed!, " + "error: " + ex);
                }
                storageDevice = device;
            }
        } else {
            storageDevice = storage;
        }

        usageLine = StringUtils.isNotEmpty(argsClassName) ?
                appName + " "+SbkUtils.CLASS_OPTION +" "+argsClassName : appName;
        storageDrivers = storageDevice == null ? packageStore.getDrivers() : null;
        params = new SbkParameters(usageLine, storageDrivers);
        logger.addArgs(params);
        if (storageDevice != null) {
            storageDevice.addArgs(params);
        }
        final String[] nextArgs = SbkUtils.removeOptionsAndValues(args, new String[]{SbkUtils.CLASS_OPTION});
        if (nextArgs == null || nextArgs.length == 0) {
            final String helpText = params.getHelpText();
            System.out.println("\n" + helpText);
            throw new HelpException(helpText);
        }

        try {
            params.parseArgs(nextArgs);
            logger.parseArgs(params);
            if (storageDevice != null) {
                storageDevice.parseArgs(params);
            }
        } catch (UnrecognizedOptionException ex) {
            if (storageDevice == null) {
                params.printHelp();
                final String errStr = "The option '-class' is not supplied";
                throw new ParseException(errStr);
            }
            Printer.log.error(ex.toString());
            params.printHelp();
            throw ex;
        } catch (HelpException ex) {
            System.out.println("\n"+ex.getHelpText());
            throw  ex;
        }
        if (storageDevice == null) {
            final String errStr;
            params.printHelp();
            if (StringUtils.isEmpty(className)) {
                errStr = "The option '-class' is not supplied";
                throw new ParseException(errStr);
            } else {
                errStr = "The storage class implementation for the driver: " + className + " not found!";
                throw new ClassNotFoundException(errStr);
            }
        }

        Printer.log.info("Arguments to Driver '"+ storageDevice.getClass().getSimpleName() + "' : "+Arrays.toString(nextArgs));

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

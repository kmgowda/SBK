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
import io.sbk.api.ParameterOptions;
import io.sbk.api.Logger;
import io.sbk.api.PerformanceLogger;
import io.sbk.perl.PerlConfig;
import io.sbk.api.Storage;
import io.sbk.perl.Time;
import io.sbk.system.Printer;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang.StringUtils;
import org.reflections.ReflectionsException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
     *                and storage object should be available in the package 'io.sbk.storage'.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , storage name is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InterruptedException If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException If an exception occurred.
     * @throws TimeoutException If an exception occurred if an I/O operation is timed out.
     */
    public static void run(final String[] args, final Storage<Object> storage,
                           final String applicationName, Logger outLogger) throws ParseException, IllegalArgumentException,
             IOException, InterruptedException, ExecutionException, TimeoutException {

        final CompletableFuture<Void> ret = runAsync(args, storage, applicationName, outLogger);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            ret.complete(null);
        }));
        ret.get();
    }

    /**
     * Asynchronously Run the Performance Benchmarking .
     * @param args command line arguments.
     * @param storage storage object on which performance benchmarking will be conducted.
     *                if you pass 'null', then name of the storage should be in args '-class' arguments
     *                and storage object should be available in the package 'io.sbk.storage'.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , storage name is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @return CompletableFuture instance.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InterruptedException If an exception occurred due to writers or readers interrupted.
     * @throws ExecutionException If an exception occurred due to writers or readers exceptions.
     */
    public static CompletableFuture<Void> runAsync(final String[] args, final Storage<Object> storage,
                           final String applicationName, Logger outLogger) throws ParseException,
            IllegalArgumentException, IOException, InterruptedException, ExecutionException {
        CompletableFuture<Void> ret;
        try {
            ret = new SbkCompletableFutureAsync(args, storage, applicationName, outLogger);
        } catch (InstantiationException ex) {
                ret = new CompletableFuture<>();
                ret.complete(null);
                return ret;
        }
        return ret;
    }

    private static class SbkCompletableFutureAsync extends CompletableFuture<Void> {
        private final Benchmark benchmark;
        private final CompletableFuture<Void> ret;

        public SbkCompletableFutureAsync(final String[] args, final Storage<Object> storage,
                                    final String applicationName, Logger outLogger) throws ParseException,
                IllegalArgumentException, IOException, InterruptedException, ExecutionException,
                InstantiationException {
            super();
            benchmark = createBenchmark(args, storage, applicationName, outLogger);
            ret = benchmark.start();
        }

        @Override
        public Void get() throws InterruptedException,
                ExecutionException {
            return ret.get();
        }


        @Override
        public Void get(long timeout, java.util.concurrent.TimeUnit  unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            return ret.get(timeout, unit);
        }

        @Override
        public Void join() {
            return ret.join();
        }

        @Override
        public boolean complete(Void val) {
            benchmark.stop();
            return super.complete(val);
        }
    }


    private static Benchmark createBenchmark(final String[] args, final Storage<Object> storage,
                           final String applicationName, Logger outLogger) throws ParseException,
            IllegalArgumentException,
            IOException, InstantiationException  {
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
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        final String argsClassName = SbkUtils.getClassName(args);
        final String driverName;
        final String usageLine;

        Printer.log.info(IOUtils.toString(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(BANNERFILE)));
        Printer.log.info( "Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info(Config.NAME.toUpperCase() +" Version: "+version);
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info(Config.SBK_APP_NAME + ": "+   Objects.requireNonNullElse(sbkApplicationName, ""));
        Printer.log.info(Config.SBK_CLASS_NAME + ": "+ Objects.requireNonNullElse(sbkClassName, ""));
        Printer.log.info(Config.SBK_APP_HOME+": "+ Objects.requireNonNullElse(sbkAppHome, ""));

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        perlConfig = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                PerlConfig.class);
        logger = Objects.requireNonNullElseGet(outLogger, SbkGrpcPrometheusLogger::new);

        storageDevice = Objects.requireNonNullElse(storage, getStorageDevice( argsClassName, appName, logger));
        driverName =  storageDevice.getClass().getSimpleName();
        usageLine = StringUtils.isNotEmpty(argsClassName) ?
                appName + " " + SbkUtils.CLASS_OPTION + " " + driverName : appName;

        params = new SbkParameters(usageLine, null);
        logger.addArgs(params);
        storageDevice.addArgs(params);
        final String[] nextArgs = SbkUtils.removeOptionsAndValues(args, new String[]{SbkUtils.CLASS_OPTION});

        if (nextArgs == null) {
            params.printHelp();
            throw new InstantiationException("Insufficient command line arguments");
        }
        Printer.log.info("Arguments to Driver '"+driverName + "' : "+Arrays.toString(nextArgs));
        try {
            params.parseArgs(nextArgs);
            logger.parseArgs(params);
            storageDevice.parseArgs(params);
        } catch (UnrecognizedOptionException ex) {
            Printer.log.error(ex.toString());
            params.printHelp();
            throw new InstantiationException("print help !");
        }

        if (params.hasOption("help")) {
            params.printHelp();
            throw new InstantiationException("print help !");
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
        return new SbkBenchmark(driverName, action, perlConfig, params, storageDevice, dType, logger, time);
    }


    private static Storage<?> getStorageDevice(final String argsClassName, final String appName,
                                               final PerformanceLogger logger) throws  InstantiationException {
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final Storage<?> storageDevice;
        final String className;
        List<String> driversList;
        String driverName;

        try {
            driversList = SbkUtils.getAvailableClassNames(Config.PACKAGE_NAME);
            Printer.log.info("Available Drivers : "+ driversList.size());
        } catch (ReflectionsException ex) {
            Printer.log.warn(ex.toString());
            driversList = new LinkedList<>();
        }
        if (argsClassName == null) {
            if (StringUtils.isNotEmpty(sbkClassName)) {
                className = sbkClassName;
            } else {
                final ParameterOptions paramsHelp = new SbkParameters(appName, driversList);
                logger.addArgs(paramsHelp);
                paramsHelp.printHelp();
                final String errMsg = "SBK Benchmark class driver not found! check the option '"+ SbkUtils.CLASS_OPTION +"'";
                throw new InstantiationException(errMsg);
            }
        } else {
            className = argsClassName;
        }
        driverName = null;
        if (driversList.size() > 0) {
            driverName = SbkUtils.searchDriver(driversList, className);
            if (driverName == null) {
                String msg = "storage driver: " + className+ " not found in the SBK";
                Printer.log.warn(msg);
            }
        }
        if (driverName == null) {
            driverName = className;
        }
        if (driverName.length() == 0) {
            String errMsg = "No storage driver name supplied/found";
            Printer.log.error(errMsg);
            throw new InstantiationException(errMsg);
        }

        try {
            storageDevice = (Storage<?>) Class.forName(Config.PACKAGE_NAME + "." + driverName + "." + driverName)
                    .getConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException ex) {
            final ParameterOptions paramsHelp = new SbkParameters(appName, driversList);
            logger.addArgs(paramsHelp);
            paramsHelp.printHelp();
            String errMsg = "storage driver: " + driverName+ " Instantiation failed";
            throw new InstantiationException(errMsg);
        }
        return storageDevice;
    }



}

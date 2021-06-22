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
import io.sbk.api.Benchmark;
import io.sbk.api.Config;
import io.sbk.api.DataType;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.api.impl.SbkUtils;
import io.sbk.gem.GemConfig;
import io.sbk.gem.GemParameterOptions;
import io.sbk.perl.Time;
import io.sbk.ram.RamConfig;
import io.sbk.ram.RamLogger;

import io.sbk.ram.impl.SbkRamPrometheusLogger;
import io.sbk.system.Printer;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang.StringUtils;
import org.reflections.ReflectionsException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    final static String BIN_EXT_PATH = "bin";
    final static String LIB_EXT_PATH = "lib";

    /**
     * Run the Performance Benchmarking .
     * @param args command line arguments.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , SbkServer is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @param sbkCommand sbk command name , if null, then env variable sbk.applicationName is used.
     * @param sbkDir    directory where sbk command exists; if null, env variable sbk.appHome is used.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InterruptedException If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException If an exception occurred.
     * @throws TimeoutException If an exception occurred if an I/O operation is timed out.
     */
    public static void run(final String[] args, final String applicationName,
                           RamLogger outLogger,  String sbkCommand,
                           String sbkDir) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException {

        final CompletableFuture<Void> ret = runAsync(args, applicationName, outLogger,
                sbkCommand, sbkDir);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            ret.complete(null);
        }));
        ret.get();
    }

    /**
     * Asynchronously Run the Performance Benchmarking .
     * @param args command line arguments.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null', SbkServer
     *                       is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @param sbkCommand sbk command name , if null, then env variable sbk.applicationName is used.
     * @param sbkDir    directory where sbk command exists; if null, env variable sbk.appHome is used.
     *
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @return CompletableFuture instance.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InterruptedException If an exception occurred due to writers or readers interrupted.
     * @throws ExecutionException If an exception occurred due to writers or readers exceptions.
     */
    public static CompletableFuture<Void> runAsync(final String[] args, final String applicationName,
                                                   RamLogger outLogger, String sbkCommand,
                                                   String sbkDir) throws ParseException,
            IllegalArgumentException, IOException, InterruptedException, ExecutionException {
        CompletableFuture<Void> ret;
        try {
            ret = new SbkGem.SbkGemCompletableFutureAsync(args, applicationName, outLogger,
                    sbkCommand, sbkDir);
        } catch (InstantiationException ex) {
            ret = new CompletableFuture<>();
            ret.complete(null);
            return ret;
        }
        return ret;
    }

    private static class SbkGemCompletableFutureAsync extends CompletableFuture<Void> {
        private final Benchmark benchmark;
        private final CompletableFuture<Void> ret;

        public SbkGemCompletableFutureAsync(final String[] args, final String applicationName,
                                            RamLogger outLogger, String sbkCommand,
                                            String sbkDir) throws ParseException,
                IllegalArgumentException, IOException, InterruptedException, ExecutionException,
                InstantiationException {
            super();
            benchmark = createBenchmark(args, applicationName, outLogger, sbkCommand, sbkDir);
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


    private static Benchmark createBenchmark(final String[] args, final String applicationName,
                                             RamLogger outLogger, final String sbkCommand,
                                             final String sbkDir)
            throws ParseException, IllegalArgumentException, IOException, InstantiationException  {
        final GemParameterOptions params;
        final GemConfig gemConfig;
        final RamLogger logger;
        final RamConfig ramConfig;
        final Time time;
        final String version = io.sbk.ram.impl.SbkRam.class.getPackage().getImplementationVersion();
        final String sbkGemAppName = System.getProperty(GemConfig.SBK_GEM_APP_NAME);
        final String appName = StringUtils.isNotEmpty(applicationName) ? applicationName :
                StringUtils.isNotEmpty(sbkGemAppName) ? sbkGemAppName : GemConfig.NAME;
        final String sbkAppName = System.getProperty(Config.SBK_APP_NAME);
        final String sbkAppCommand = StringUtils.isNotEmpty(sbkCommand) ? sbkCommand :
                StringUtils.isNotEmpty(sbkAppName) ? sbkAppName : Config.NAME;
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        final String sbkAppDir = StringUtils.isNotEmpty(sbkDir) ? sbkDir : sbkAppHome;
        final String argsClassName = SbkUtils.getClassName(args);
        final String className = StringUtils.isNotEmpty(argsClassName) ? argsClassName : sbkClassName;
        final String sbkFullCommand = sbkAppDir+"/"+BIN_EXT_PATH+"/"+sbkCommand;
        final Storage storageDevice;
        final String usageLine;
        String driverName = null;
        List<String> driversList;

        Printer.log.info(IOUtils.toString(io.sbk.gem.impl.SbkGem.class.getClassLoader().getResourceAsStream(BANNER_FILE)));
        Printer.log.info( "Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info(GemConfig.NAME.toUpperCase() +" Version: "+version);
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info(Config.SBK_APP_NAME + ": "+ Objects.requireNonNullElse(sbkAppName, ""));
        Printer.log.info(Config.SBK_CLASS_NAME + ": "+ Objects.requireNonNullElse(sbkClassName, ""));
        Printer.log.info(Config.SBK_APP_HOME+": "+ Objects.requireNonNullElse(sbkAppHome, ""));

        if (!Files.isDirectory(Paths.get(sbkAppDir))) {
            String errMsg = "The SBK application directory: "+sbkAppDir +" not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Path sbkCommandPath = Paths.get(sbkFullCommand);

        if (!Files.exists(sbkCommandPath)) {
            String errMsg = "The sbk executable command: "+sbkFullCommand+" not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isExecutable(sbkCommandPath)) {
            String errMsg = "The executable permissions are not found for command: "+sbkFullCommand;
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ramConfig = mapper.readValue(io.sbk.ram.impl.SbkRam.class.getClassLoader().getResourceAsStream(RAM_CONFIG_FILE),
                RamConfig.class);
        gemConfig = mapper.readValue(io.sbk.gem.impl.SbkGem.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                GemConfig.class);

        logger = Objects.requireNonNullElseGet(outLogger, SbkRamPrometheusLogger::new);

        try {
            driversList = SbkUtils.getAvailableClassNames(Config.PACKAGE_NAME);
            Printer.log.info("Available Drivers : "+ driversList.size());
        } catch (ReflectionsException ex) {
            Printer.log.warn(ex.toString());
            driversList = new LinkedList<>();
        }
        if (StringUtils.isEmpty(className)) {
            final ParameterOptions paramsHelp = new SbkGemParameters(appName, driversList, gemConfig);
            logger.addArgs(paramsHelp);
            paramsHelp.printHelp();
            final String errMsg = "SBK Benchmark class driver not found! check the option '"+ SbkUtils.CLASS_OPTION +"'";
            throw new InstantiationException(errMsg);
        }
        if (driversList.size() > 0) {
            driverName = SbkUtils.searchDriver(driversList, className);
            if (driverName == null) {
                String msg = "storage driver: " + className+ " not found in the SBK";
                Printer.log.warn(msg);
            } else {
                driverName = className;
            }
        } else {
            driverName = className;
        }
        try {
            storageDevice = (Storage<?>) Class.forName(Config.PACKAGE_NAME + "." + driverName + "." + driverName)
                    .getConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException ex) {
            final ParameterOptions paramsHelp = new SbkGemParameters(appName, driversList, gemConfig);
            logger.addArgs(paramsHelp);
            paramsHelp.printHelp();
            String errMsg = "storage driver: " + driverName+ " Instantiation failed";
            throw new InstantiationException(errMsg);
        }
        usageLine = StringUtils.isNotEmpty(argsClassName) ?
                appName + " " + SbkUtils.CLASS_OPTION + " " + driverName : appName;

        params = new SbkGemParameters(usageLine, null, gemConfig);
        logger.addArgs(params);
        storageDevice.addArgs(params);
        final String[] nextArgs = SbkUtils.removeOptionsAndValues(args, new String[]{SbkUtils.CLASS_OPTION});

        if (nextArgs == null) {
            params.printHelp();
            throw new InstantiationException("Insufficient command line arguments");
        }
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

        return null;
    }




}

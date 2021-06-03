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
import io.sbk.perl.PerlConfig;
import io.sbk.api.Storage;
import io.sbk.perl.Time;
import io.sbk.perl.TimeUnit;
import io.sbk.perl.impl.MicroSeconds;
import io.sbk.perl.impl.MilliSeconds;
import io.sbk.perl.impl.NanoSeconds;
import io.sbk.system.Printer;
import org.apache.commons.cli.ParseException;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Main class of SBK.
 */
public class Sbk {
    final static String CONFIGFILE = "sbk.properties";
    final static String BANNERFILE = "banner.txt";
    final static String CLASS_OPTION = "-class";


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
        final String className;
        final Storage storageDevice;
        final Action action;
        final ParameterOptions params;
        final Logger logger;
        final PerlConfig perlConfig;
        final Time time;
        final String version = io.sbk.api.impl.Sbk.class.getPackage().getImplementationVersion();
        final String sbkApplicationName = System.getProperty(Config.SBK_APP_NAME);
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        String driverName;
        String usageLine;

        Printer.log.info(IOUtils.toString(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(BANNERFILE)));
        Printer.log.info( "Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info(Config.NAME.toUpperCase() +" Version: "+version);
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info(Config.SBK_APP_NAME + ": "+ sbkApplicationName);
        Printer.log.info(Config.SBK_CLASS_NAME + ": "+ sbkClassName);
        Printer.log.info(Config.SBK_APP_HOME+": "+sbkAppHome);

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        perlConfig = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                PerlConfig.class);

        logger = Objects.requireNonNullElseGet(outLogger, SbkPrometheusLogger::new);

        if (storage == null) {
            List<String> driversList;
            try {
                driversList = getAvailableClassNames(Config.PACKAGE_NAME);
                Printer.log.info("Available Drivers : "+ driversList.size());
            } catch (ReflectionsException ex) {
                Printer.log.warn(ex.toString());
                driversList = new LinkedList<>();
            }
            if (sbkApplicationName != null && sbkApplicationName.length() > 0) {
                usageLine = sbkApplicationName;
            } else {
                usageLine = Config.NAME;
            }
            String name  = getClassName(args);
            if (name == null) {
                if (sbkClassName != null && sbkClassName.length() > 0) {
                    className = sbkClassName;
                } else {
                    final ParameterOptions paramsHelp = new SbkParameters(usageLine, driversList);
                    logger.addArgs(paramsHelp);
                    paramsHelp.printHelp();
                    throw new InstantiationException("SBK Benchmark class driver not found!");
                }
            } else {
                className = name;
                usageLine += " "+CLASS_OPTION+" " + className;
            }
            driverName = null;
            if (driversList.size() > 0) {
                driverName = searchDriver(driversList, className);
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
                storageDevice =
                        (Storage<?>) Class.forName(Config.PACKAGE_NAME + "." + driverName + "." + driverName).getConstructor().newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                    NoSuchMethodException | InvocationTargetException ex) {
                final ParameterOptions paramsHelp = new SbkParameters(usageLine, driversList);
                logger.addArgs(paramsHelp);
                paramsHelp.printHelp();
                String errMsg = "storage driver: " + driverName+ " Instantiation failed";
                throw new InstantiationException(errMsg);
            }

        } else {
            storageDevice = storage;
            usageLine = Objects.requireNonNullElseGet(applicationName, () -> storageDevice.getClass().getSimpleName());
            driverName = usageLine;
        }
        params = new SbkParameters(usageLine, null);
        logger.addArgs(params);
        storageDevice.addArgs(params);
        final String[] nextArgs = removeClassName(args);

        if (nextArgs == null) {
            params.printHelp();
            throw new InstantiationException("Insufficient command line arguments");
        }
        Printer.log.info("Arguments to Driver '"+driverName + "' : "+Arrays.toString(nextArgs));
        params.parseArgs(nextArgs);
        if (params.hasOption("help")) {
            throw new InstantiationException("print help !");
        }

        logger.parseArgs(params);
        storageDevice.parseArgs(params);
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
        TimeUnit timeUnit = logger.getTimeUnit();
        if (timeUnit == TimeUnit.mcs) {
            time = new MicroSeconds();
        } else if (timeUnit == TimeUnit.ns) {
            time = new NanoSeconds();
        } else {
            time = new MilliSeconds();
        }
        Printer.log.info("Time Unit: "+ timeUnit.toString());
        Printer.log.info("Minimum Latency: "+logger.getMinLatency()+" "+timeUnit.name());
        Printer.log.info("Maximum Latency: "+logger.getMaxLatency()+" "+timeUnit.name());
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

    private static String[] removeClassName(String[] args) {
        if (args.length < 3) {
            return null;
        }
        List<String> ret = new ArrayList<>(args.length);
        int i = 0;
        while (i < args.length) {
            if (args[i].equals(CLASS_OPTION)) {
                i += 1;
            } else {
                ret.add(args[i]);
            }
            i += 1;
        }
        return ret.toArray(new String[0]);
    }

    private static String getClassName(String[] args) {
        if (args == null || args.length < 2) {
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(CLASS_OPTION)) {
                if (i+1 < args.length) {
                    return args[i+1];
                } else {
                    return null;
                }
            }
        }
        return null;
    }


    private static List<String> getAvailableClassNames(String pkgName) throws ReflectionsException {
        Reflections reflections = new Reflections(pkgName);
        Set<Class<? extends Storage>> subTypes = reflections.getSubTypesOf(Storage.class);
        return subTypes.stream().map(i -> i.toString().substring(i.toString().lastIndexOf(".") + 1))
                .sorted().collect(Collectors.toList());
    }

    private static String searchDriver(List<String> list, String name) {
        for (String st: list) {
            if (st.equalsIgnoreCase(name)) {
                return st;
            }
        }
        return null;
    }
}

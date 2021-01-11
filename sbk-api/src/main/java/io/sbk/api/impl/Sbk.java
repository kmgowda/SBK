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
import io.sbk.api.Logger;
import io.sbk.api.Parameters;
import io.sbk.api.Config;
import io.sbk.api.Storage;
import io.sbk.api.Time;
import io.sbk.api.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
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

    public static void run(final String[] args, final Storage<Object> storage,
                           final String applicationName, Logger outLogger) throws ParseException, IllegalArgumentException,
             IOException, InterruptedException, ExecutionException, TimeoutException {
        runAsync(args, storage, applicationName, outLogger).get();
    }

    public static CompletableFuture<Void> runAsync(final String[] args, final Storage<Object> storage,
                           final String applicationName, Logger outLogger) throws ParseException, IllegalArgumentException,
            IOException {
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
                                    final String applicationName, Logger outLogger) throws ParseException, IllegalArgumentException,
                IOException, InstantiationException {
            super();
            benchmark = createBenchmark(args, storage, applicationName, outLogger);
            ret = benchmark.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println();
                benchmark.stop();
            }));
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
        public boolean complete(Void val) {
            benchmark.stop();
            return super.complete(val);
        }
    }


    private static Benchmark createBenchmark(final String[] args, final Storage<Object> storage,
                           final String applicationName, Logger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InstantiationException  {
        List<String> driversList;
        final CommandLine commandline;
        final String className;
        final String driverName;
        final Storage storageDevice;
        final String usageLine;
        final Action action;
        final Parameters params;
        final Logger logger;
        final Config config;
        final Time time;
        final String version = io.sbk.api.impl.Sbk.class.getPackage().getImplementationVersion();
        final String sbkApplicationName = System.getProperty(Config.SBK_APP_NAME);
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);

        SbkLogger.log.info(IOUtils.toString(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(BANNERFILE)));
        SbkLogger.log.info(Config.NAME.toUpperCase() +" version: "+version);
        SbkLogger.log.info("Argument List: "+Arrays.toString(args));
        SbkLogger.log.info(Config.SBK_APP_NAME + ": "+ sbkApplicationName);
        SbkLogger.log.info(Config.SBK_CLASS_NAME + ": "+ sbkClassName);

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        config = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                Config.class);

        commandline = new DefaultParser().parse(new Options()
                        .addOption("class", true, "Benchmark Class"),
                args, true);

        if (outLogger == null) {
            logger = new PrometheusLogger();
        } else {
            logger = outLogger;
        }

        if (storage == null) {
            driversList =  getClassNames(config.packageName);
            SbkLogger.log.info("Available Drivers : "+ driversList.size());
            String name  = commandline.getOptionValue("class", null);
            if (name == null) {
                if (sbkClassName != null && sbkClassName.length() > 0) {
                    className = sbkClassName;
                } else {
                    final Parameters paramsHelp;
                    if (sbkApplicationName != null && sbkApplicationName.length() > 0) {
                        paramsHelp = new SbkParameters(sbkApplicationName,  driversList);
                    } else {
                        paramsHelp = new SbkParameters(Config.NAME, driversList);
                    }
                    logger.addArgs(paramsHelp);
                    paramsHelp.printHelp();
                    throw new InstantiationException("SBK Benchmark class driver not found!");
                }
            } else {
                className = name;
            }
            driverName = searchDriver(driversList, className);
            if (driverName == null) {
                String errMsg = "storage driver: " + className+ " not found in the SBK, run with -help to see the supported drivers";
                SbkLogger.log.error(errMsg);
                throw new InstantiationException(errMsg);
            }
            try {
                storageDevice = (Storage<?>) Class.forName(config.packageName + "." + driverName + "." + driverName).getConstructor().newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                    NoSuchMethodException | InvocationTargetException ex) {
                throw new IllegalArgumentException(ex);
            }
            if (sbkApplicationName != null && sbkApplicationName.length() > 0) {
                usageLine = sbkApplicationName;
            } else {
                usageLine = Config.NAME + " -class "+ driverName;
            }
        } else {
            storageDevice = storage;
            driversList = null;
            if (applicationName != null) {
                usageLine = applicationName;
            } else {
                usageLine = storageDevice.getClass().getSimpleName();
            }
            driverName = usageLine;
        }

        params = new SbkParameters(usageLine, driversList);
        logger.addArgs(params);
        storageDevice.addArgs(params);

        params.parseArgs(args);
        if (params.hasOption("help")) {
            throw new InstantiationException("print only help!");
        }

        logger.parseArgs(params);
        storageDevice.parseArgs(params);
        TimeUnit timeUnit = logger.getTimeUnit();
        SbkLogger.log.info("Time Unit: "+ timeUnit.toString());
        if (timeUnit == TimeUnit.ms) {
            time = new MilliSeconds();
        } else if (timeUnit == TimeUnit.ns) {
            time = new NanoSeconds();
        } else {
            SbkLogger.log.error("storage driver: " + driverName+ " using invalid TimeUnit , Use either TimeUnit.ms or TimeUnit.ns");
            throw new IllegalArgumentException();
        }
        if (params.getReadersCount() > 0) {
            if (params.isWriteAndRead()) {
                action = Action.Write_Reading;
            } else {
                action = Action.Reading;
            }
        } else {
            action = Action.Writing;
        }
        return new SbkBenchmark(driverName, action, config, params, storageDevice, logger, time);
    }

    private static List<String> getClassNames(String pkgName) {
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

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
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.util.IOUtils;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import io.sbk.api.Benchmark;
import io.sbk.api.Metric;
import io.sbk.api.Parameters;
import io.sbk.api.ResultLogger;
import io.sbk.api.Config;
import io.sbk.api.Storage;
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
import java.util.stream.Collectors;

/**
 * Main class of SBK.
 */
public class Sbk {
    final static String CONFIGFILE = "sbk.properties";
    final static String BANNERFILE = "banner.txt";

    public static void run(final String[] args) throws ParseException, IllegalArgumentException,
             IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();
        CommandLine commandline = null;
        String className = null;
        Storage obj = null;
        String usageLine = null;
        MeterRegistry metricRegistry = null;
        final String action;
        final Parameters params;
        final List<String> driversList;
        final ResultLogger metricsLogger;
        final String version = io.sbk.main.SbkMain.class.getPackage().getImplementationVersion();
        final String sbkApplicationName = System.getProperty(Config.SBK_APP_NAME);
        final String sbkClassName = System.getProperty(Config.SBK_CLASS_NAME);
        Config config = null;
        CompletableFuture<Void> ret = null;

        SbkLogger.log.info(IOUtils.toString(io.sbk.main.SbkMain.class.getClassLoader().getResourceAsStream(BANNERFILE)));
        SbkLogger.log.info(Config.NAME.toUpperCase() +" version: "+version);
        SbkLogger.log.info("Argument List: "+Arrays.toString(args));
        SbkLogger.log.info(Config.SBK_APP_NAME + ": "+ sbkApplicationName);
        SbkLogger.log.info(Config.SBK_CLASS_NAME + ": "+ sbkClassName);

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        config = mapper.readValue(io.sbk.main.SbkMain.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                Config.class);

        commandline = new DefaultParser().parse(new Options()
                        .addOption("class", true, "Benchmark Class"),
                args, true);

        if (sbkApplicationName != null && sbkApplicationName.length() > 0) {
            usageLine = sbkApplicationName;
        } else {
            usageLine = Config.NAME;
        }

        final Metric metric = new MetricImpl();
        driversList =  getClassNames(config.packageName);
        className = commandline.getOptionValue("class", null);
        if (className == null) {
            if (sbkClassName != null && sbkClassName.length() > 0) {
                className = sbkClassName;
            } else {
                Parameters paramsHelp = new SbkParameters(usageLine, config.DESC,  driversList, startTime);
                metric.addArgs(paramsHelp);
                paramsHelp.printHelp();
                return;
            }
        }
        final String name = searchDriver(driversList, className);
        if (name == null) {
            SbkLogger.log.error("storage driver: " + className+ " not found in the SBK, run with -help to see the supported drivers");
            return;
        }
        try {
            obj = (Storage) Class.forName(config.packageName + "." + name + "." + name).getConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException ex) {
            throw new IllegalArgumentException(ex);
        }

        final Storage storage = obj;
        if (storage == null) {
            SbkLogger.log.error("Failure to create Benchmark object");
            return;
        }

        if (usageLine.equals(Config.NAME)) {
            usageLine = usageLine + " -class "+ name;
        }

        params = new SbkParameters(usageLine, config.DESC, driversList,  startTime);
        storage.addArgs(params);
        metric.addArgs(params);
        params.parseArgs(args);
        if (params.hasOption("help")) {
            params.printHelp();
            return;
        }
        metric.parseArgs(params);
        storage.parseArgs(params);
        metricRegistry = metric.createMetric(params);

        if (params.getReadersCount() > 0) {
            if (params.isWriteAndRead()) {
                action = "Write/Reading";
            } else {
                action = "Reading";
            }
        } else {
            action = "Writing";
        }

        final String prefix = name +" "+action;
        if (metricRegistry == null) {
            metricsLogger = new SystemResultLogger(prefix);
        } else {
            final CompositeMeterRegistry compositeLogger = Metrics.globalRegistry;
            compositeLogger.add(new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM));
            compositeLogger.add(metricRegistry);
            metricsLogger = new MetricsLogger(Config.NAME, prefix, params.getWritersCount(), params.getReadersCount(),
                    config.reportingMS, compositeLogger);
        }

        final Benchmark benchmark = new SbkBenchmark(config, params,
                storage, metricsLogger);
        ret = benchmark.start(System.currentTimeMillis());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            benchmark.stop(System.currentTimeMillis());
        }));

        ret.get();
        benchmark.stop(System.currentTimeMillis());
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

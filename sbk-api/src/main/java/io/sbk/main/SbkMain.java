/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.main;

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
import io.sbk.api.impl.MetricImpl;
import io.sbk.api.impl.MetricsLogger;
import io.sbk.api.impl.SbkBenchmark;
import io.sbk.api.impl.SbkLogger;
import io.sbk.api.impl.SbkParameters;
import io.sbk.api.impl.SystemResultLogger;
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
public class SbkMain {
    final static String CONFIGFILE = "sbk.properties";
    final static String BANNERFILE = "banner.txt";

    public static void main(final String[] args) {
        long startTime = System.currentTimeMillis();
        CommandLine commandline = null;
        String className = null;
        Storage obj = null;
        MeterRegistry metricRegistry = null;
        final String action;
        final Parameters params;
        final List<String> driversList;
        final ResultLogger metricsLogger;
        final String version = SbkMain.class.getPackage().getImplementationVersion();
        Config config = null;
        CompletableFuture<Void> ret = null;

        SbkLogger.log.info(IOUtils.toString(SbkMain.class.getClassLoader().getResourceAsStream(BANNERFILE)));
        SbkLogger.log.info(Config.NAME.toUpperCase() +" version: "+version);
        SbkLogger.log.info("Argument List: "+Arrays.toString(args));

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            config = mapper.readValue(SbkMain.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                    Config.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        try {
            commandline = new DefaultParser().parse(new Options()
                            .addOption("class", true, "Benchmark Class"),
                    args, true);
        } catch (ParseException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        final Metric metric = new MetricImpl();
        driversList =  getClassNames(config.packageName);
        className = commandline.getOptionValue("class", null);
        if (className == null) {
            Parameters paramsHelp = new SbkParameters(config.NAME, config.DESC, "", driversList,  startTime);
            metric.addArgs(paramsHelp);
            paramsHelp.printHelp();
            System.exit(0);
        }
        final String name = searchDriver(driversList, className);
        if (name == null) {
            SbkLogger.log.error("storage driver: " + className+ " not found in the SBK, run with -help to see the supported drivers");
            System.exit(0);
        }
        try {
            obj = (Storage) Class.forName(config.packageName + "." + name + "." + name).getConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        final Storage storage = obj;
        if (storage == null) {
            SbkLogger.log.error("Failure to create Benchmark object");
            System.exit(0);
        }
        params = new SbkParameters(config.NAME, config.DESC, name, driversList,  startTime);
        storage.addArgs(params);
        metric.addArgs(params);
        try {
            params.parseArgs(args);
        }  catch (ParseException | IllegalArgumentException ex) {
            ex.printStackTrace();
            params.printHelp();
            System.exit(0);
        }
        if (params.hasOption("help")) {
            System.exit(0);
        }
        try {
            metric.parseArgs(params);
            storage.parseArgs(params);
            metricRegistry = metric.createMetric(params);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        if (params.getReadersCount() > 0) {
            if (params.isWriteAndRead()) {
                action = "Write/Reading";
            } else {
                action = "Reading";
            }
        } else {
            action = "Writing";
        }
        final ResultLogger logger = new SystemResultLogger();
        if (metricRegistry == null) {
            metricsLogger = logger;
        } else {
            final CompositeMeterRegistry compositeLogger = Metrics.globalRegistry;
            final String prefix = config.NAME.toUpperCase() + "_" + name + "_" + action + "_";
            compositeLogger.add(new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM));
            compositeLogger.add(metricRegistry);
            metricsLogger = new MetricsLogger(
                    prefix, params.getWritersCount(), params.getReadersCount(),
                    config.reportingMS, logger, compositeLogger);
        }

        final Benchmark benchmark = new SbkBenchmark(action, config, params,
                storage, logger, metricsLogger);
        try {
            ret = benchmark.start(System.currentTimeMillis());
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println();
                benchmark.stop(System.currentTimeMillis());
        }));
        try {
            ret.get();
        } catch (ExecutionException | InterruptedException ex ) {
            ex.printStackTrace();
        }
        benchmark.stop(System.currentTimeMillis());
        System.exit(0);
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

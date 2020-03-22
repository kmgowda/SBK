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

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import io.sbk.api.Benchmark;
import io.sbk.api.Metric;
import io.sbk.api.Parameters;
import io.sbk.api.ResultLogger;
import io.sbk.api.Storage;
import io.sbk.api.impl.MetricImpl;
import io.sbk.api.impl.MetricsLogger;
import io.sbk.api.impl.SbkBenchmark;
import io.sbk.api.impl.SbkParameters;
import io.sbk.api.impl.SystemResultLogger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Main class of SBK.
 */
public class SbkMain {
    final static String BENCHMARKNAME = "sbk";
    final static String DESC = "Storage Benchmark Kit";
    final static String PKGNAME = "io.sbk";
    final static int REPORTINGINTERVAL = 5000;

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
        CompletableFuture ret = null;

        try {
            commandline = new DefaultParser().parse(new Options()
                    .addOption("version", false, "Version"),
                    args, true);
        } catch (ParseException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        if (commandline.hasOption("version")) {
            System.out.println(DESC + ", " + BENCHMARKNAME + " version: " + version);
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
        driversList =  getClassNames(PKGNAME);
        className = commandline.getOptionValue("class", null);
        if (className == null) {
            Parameters paramsHelp = new SbkParameters(BENCHMARKNAME, DESC, version, "", driversList,  startTime);
            metric.addArgs(paramsHelp);
            paramsHelp.printHelp();
            System.exit(0);
        }
        final String name = searchDriver(driversList, className);
        if (name == null) {
            System.out.printf("storage driver : %s not found in the SBK, run with -help to see the supported drivers\n", className);
            System.exit(0);
        }
        try {
            obj = (Storage) Class.forName(PKGNAME + "." + name + "." + name).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        final Storage storage = obj;
        if (storage == null) {
            System.out.println("Failure to create Benchmark object");
            System.exit(0);
        }
        params = new SbkParameters(BENCHMARKNAME, DESC, version, name, driversList,  startTime);
        storage.addArgs(params);
        metric.addArgs(params);
        try {
            params.parseArgs(args);
        }  catch (ParseException | IllegalArgumentException ex) {
            ex.printStackTrace();
            params.printHelp();
            System.exit(0);
        }
        if (params.hasOption("version")) {
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
            final String prefix = BENCHMARKNAME.toUpperCase() + "_" + name + "_" + action + "_";
            compositeLogger.add(new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM));
            compositeLogger.add(metricRegistry);
            metricsLogger = new MetricsLogger(
                    prefix, params.getWritersCount(), params.getReadersCount(),
                    REPORTINGINTERVAL, logger, compositeLogger);
        }

        final Benchmark benchmark = new SbkBenchmark(action, params, storage, logger,
                metricsLogger, REPORTINGINTERVAL);
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
            if (ret != null) {
                ret.get();
            } else {
                benchmark.stop(System.currentTimeMillis());
            }
        } catch (ExecutionException | InterruptedException ex ) {
            ex.printStackTrace();
        }
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

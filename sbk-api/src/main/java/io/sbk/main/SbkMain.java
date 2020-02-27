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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.sbk.api.Benchmark;
import io.sbk.api.DataType;
import io.sbk.api.Metric;
import io.sbk.api.Parameters;
import io.sbk.api.Performance;
import io.sbk.api.QuadConsumer;
import io.sbk.api.Reader;
import io.sbk.api.ResultLogger;
import io.sbk.api.Writer;
import io.sbk.api.impl.MetricImpl;
import io.sbk.api.impl.MetricsLogger;
import io.sbk.api.impl.SbkParameters;
import io.sbk.api.impl.SbkPerformance;
import io.sbk.api.impl.SbkReader;
import io.sbk.api.impl.SbkWriter;
import io.sbk.api.impl.SystemResultLogger;

import java.io.IOException;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.reflections.Reflections;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.core.instrument.Clock;

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
        final boolean fork = true;
        CommandLine commandline = null;
        String className = null;
        Benchmark obj = null;
        MeterRegistry metricRegistry = null;
        final String action;
        final Parameters params;
        final ExecutorService executor;
        final Performance writeStats;
        final Performance readStats;
        final QuadConsumer writeTime;
        final QuadConsumer readTime;
        final List<String> driversList;
        final ResultLogger metricsLogger;
        final String version = SbkMain.class.getPackage().getImplementationVersion();

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
            obj = (Benchmark) Class.forName(PKGNAME + "." + name + "." + name).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        final Benchmark benchmark = obj;
        if (benchmark == null) {
            System.out.println("Failure to create Benchmark object");
            System.exit(0);
        }
        params = new SbkParameters(BENCHMARKNAME, DESC, version, className, driversList,  startTime);
        benchmark.addArgs(params);
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
            benchmark.parseArgs(params);
            metricRegistry = metric.createMetric(params);
            benchmark.openStorage(params);
        } catch (RuntimeException | IOException ex) {
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
            final String prefix = BENCHMARKNAME.toUpperCase() + "_" + className + "_" + action + "_";
            compositeLogger.add(new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM));
            compositeLogger.add(metricRegistry);
            metricsLogger = new MetricsLogger(
                    prefix, params.getWritersCount(), params.getReadersCount(),
                    REPORTINGINTERVAL, logger, compositeLogger);
        }

        final int threadCount = params.getWritersCount() + params.getReadersCount() + 6;
        if (fork) {
            executor = new ForkJoinPool(threadCount);
        } else {
            executor = Executors.newFixedThreadPool(threadCount);
        }

        if (params.getWritersCount() > 0 && !params.isWriteAndRead()) {
            writeStats = new SbkPerformance(action, REPORTINGINTERVAL, params.getRecordSize(),
                                params.getCsvFile(), metricsLogger, logger, executor);
            writeTime = writeStats::recordTime;
        } else {
            writeStats = null;
            writeTime = null;
        }

        if (params.getReadersCount() > 0) {
            readStats = new SbkPerformance(action, REPORTINGINTERVAL, params.getRecordSize(),
                            params.getCsvFile(), metricsLogger, logger, executor);
            readTime = readStats::recordTime;
        } else {
            readStats = null;
            readTime = null;
        }
        final DataType data = benchmark.getDataType();
        try {
            final List<Writer> writers = IntStream.range(0, params.getWritersCount())
                                                .boxed()
                                                .map(i -> benchmark.createWriter(i, params))
                                                .collect(Collectors.toList());

            final List<Reader> readers = IntStream.range(0, params.getReadersCount())
                                                .boxed()
                                                .map(i -> benchmark.createReader(i, params))
                                                .collect(Collectors.toList());

            final List<SbkWriter> sbkWriters =  IntStream.range(0, params.getWritersCount())
                                            .boxed()
                                            .map(i -> new SbkWriter(i, params, writeTime, data, writers.get(i)))
                                            .collect(Collectors.toList());

            final List<SbkReader> sbkReaders = IntStream.range(0, params.getReadersCount())
                                            .boxed()
                                            .map(i -> new SbkReader(i, params, readTime, data, readers.get(i)))
                                            .collect(Collectors.toList());

            final List<Callable<Void>> workers = Stream.of(sbkReaders, sbkWriters)
                    .filter(x -> x != null)
                    .flatMap(x -> x.stream())
                    .collect(Collectors.toList());

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        System.out.println();
                        executor.shutdown();
                        executor.awaitTermination(1, TimeUnit.SECONDS);
                        if (writeStats != null && !params.isWriteAndRead()) {
                            writeStats.shutdown(System.currentTimeMillis());
                        }
                        if (readStats != null) {
                            readStats.shutdown(System.currentTimeMillis());
                        }
                        if (readers != null) {
                            readers.forEach(c -> {
                                try {
                                    c.close();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }
                        if (writers != null) {
                            writers.forEach(c -> {
                                try {
                                    c.close();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }
                        benchmark.closeStorage(params);
                    } catch (ExecutionException | InterruptedException | IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            startTime = System.currentTimeMillis();
            if (writeStats != null && !params.isWriteAndRead()) {
                writeStats.start(startTime);
            }
            if (readStats != null) {
                readStats.start(startTime);
            }

            executor.invokeAll(workers);
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
            if (writeStats != null && !params.isWriteAndRead()) {
                writeStats.shutdown(System.currentTimeMillis());
            }
            if (readStats != null) {
                readStats.shutdown(System.currentTimeMillis());
            }
            if (readers != null) {
                readers.forEach(c -> {
                    try {
                       c.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            if (writers != null) {
                writers.forEach(c -> {
                    try {
                        c.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            benchmark.closeStorage(params);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }


    private static List<String> getClassNames(String pkgName) {
        Reflections reflections = new Reflections(pkgName);
        Set<Class<? extends Benchmark>> subTypes = reflections.getSubTypesOf(Benchmark.class);
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

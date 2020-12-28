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
import io.sbk.api.Time;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main class of SBK.
 */
public class Sbk {
    final static String CONFIGFILE = "sbk.properties";
    final static String BANNERFILE = "banner.txt";

    public static void run(final String[] args, final Storage<Object> storage,
                           final String applicationName, ResultLogger outLogger) throws ParseException, IllegalArgumentException,
             IOException, InterruptedException, ExecutionException {
        List<String> driversList;
        final CommandLine commandline;
        final String className;
        final String driverName;
        final Storage storageDevice;
        final String usageLine;
        final MeterRegistry metricRegistry;
        final String action;
        final Parameters params;
        final ResultLogger metricsLogger;
        final Config config;
        final CompletableFuture<Void> ret;
        final Time time;
        final String timeUnitName;
        final double[] percentiles;
        final Metric metric;
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
            metric = new MetricImpl();
        } else {
            metric = null;
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
                    if (metric !=  null) {
                        metric.addArgs(paramsHelp);
                    }
                    paramsHelp.printHelp();
                    return;
                }
            } else {
                className = name;
            }
            driverName = searchDriver(driversList, className);
            if (driverName == null) {
                SbkLogger.log.error("storage driver: " + className+ " not found in the SBK, run with -help to see the supported drivers");
                return;
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
        storageDevice.addArgs(params);
        if (metric != null) {
            metric.addArgs(params);
        }
        params.parseArgs(args);
        if (params.hasOption("help")) {
            return;
        }
        if (metric != null) {
            metric.parseArgs(params);
        }
        storageDevice.parseArgs(params);
        TimeUnit timeUnit = storageDevice.getTimeUnit();
        SbkLogger.log.info("Time Unit: "+ timeUnit.toString());
        if (timeUnit == TimeUnit.MILLISECONDS) {
            time = new MilliSeconds();
        } else if (timeUnit == TimeUnit.NANOSECONDS) {
            time = new NanoSeconds();
        } else {
            SbkLogger.log.error("storage driver: " + driverName+ " using invalid TimeUnit , Use either TimeUnit.MILLISECONDS or TimeUnit.NANOSECONDS");
            throw new IllegalArgumentException();
        }
        percentiles = storageDevice.getPercentileIndices();
        for (double p: percentiles) {
            if (p < 0 || p > 100) {
                SbkLogger.log.error("storage driver: " + driverName+ " Invalid percentiles indices : "+percentiles.toString());
                SbkLogger.log.error("Percentile indices should be greater than 0 and less than 100");
                throw new IllegalArgumentException();
            }
        }
        Arrays.sort(percentiles);
        if (params.getReadersCount() > 0) {
            if (params.isWriteAndRead()) {
                action = "Write/Reading";
            } else {
                action = "Reading";
            }
        } else {
            action = "Writing";
        }
        timeUnitName = Config.timeUnitToString(timeUnit);
        //For metrics the driver name should be in Uppercase
        final String prefix = driverName.toUpperCase() +" "+action;
        if (metric != null) {
            metricRegistry = metric.createMetric(params);
            if (metricRegistry == null) {
                metricsLogger = new SystemResultLogger(prefix, timeUnitName, percentiles);
            } else {
                final CompositeMeterRegistry compositeLogger = Metrics.globalRegistry;
                compositeLogger.add(new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM));
                compositeLogger.add(metricRegistry);
                metricsLogger = new MetricsLogger(Config.NAME, prefix, timeUnitName, percentiles,
                        params.getWritersCount(), params.getReadersCount(), compositeLogger);
            }
        } else {
            metricsLogger = outLogger;
        }
        final Benchmark benchmark = new SbkBenchmark(config, params,
                storageDevice, time, storageDevice.getMinLatency(), storageDevice.getMaxWindowLatency(),
                storageDevice.getMaxLatency(), percentiles, metricsLogger);
        ret = benchmark.start(time.getCurrentTime());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            benchmark.stop(time.getCurrentTime());
        }));

        ret.get();
        benchmark.stop(time.getCurrentTime());
    }

    public static CompletableFuture<Void> runAsync(final String[] args, final Storage<Object> storage,
                           final String applicationName, ResultLogger outLogger) {
        return CompletableFuture.runAsync(() -> {
            try {
                 run(args, storage, applicationName, outLogger);
            } catch (ParseException | IllegalArgumentException | IOException |
                    InterruptedException | ExecutionException ex) {
                throw new CompletionException(ex);
            }
        });
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

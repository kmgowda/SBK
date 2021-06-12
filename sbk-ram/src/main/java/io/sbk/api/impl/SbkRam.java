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
import io.sbk.api.Benchmark;
import io.sbk.api.Config;
import io.sbk.api.RamConfig;
import io.sbk.api.RamLogger;
import io.sbk.api.RamParameterOptions;
import io.sbk.perl.Time;
import io.sbk.perl.TimeUnit;
import io.sbk.perl.impl.MicroSeconds;
import io.sbk.perl.impl.MilliSeconds;
import io.sbk.perl.impl.NanoSeconds;
import io.sbk.system.Printer;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Main class of SBK Server.
 */
public class SbkRam {
    final static String CONFIG_FILE = "ram.properties";
    final static String BANNER_FILE = "ram-banner.txt";
    final static String APP_NAME = "sbk-ram";

    /**
     * Run the Performance Benchmarking .
     * @param args command line arguments.
     * @param applicationName name of the application. will be used in the 'help' message. if it is 'null' , SbkServer is used by default.
     * @param outLogger Logger object to write the benchmarking results; if it is 'null' , the default Prometheus
     *                  logger will be used.
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InterruptedException If an exception occurred if the writers and readers are interrupted.
     * @throws ExecutionException If an exception occurred.
     * @throws TimeoutException If an exception occurred if an I/O operation is timed out.
     */
    public static void run(final String[] args, final String applicationName,
                           RamLogger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InterruptedException, ExecutionException, TimeoutException {

        final CompletableFuture<Void> ret = runAsync(args, applicationName, outLogger);

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
     * @throws ParseException If an exception occurred while parsing command line arguments.
     * @return CompletableFuture instance.
     * @throws IllegalArgumentException If an exception occurred due to invalid arguments.
     * @throws IOException If an exception occurred due to write or read failures.
     * @throws InterruptedException If an exception occurred due to writers or readers interrupted.
     * @throws ExecutionException If an exception occurred due to writers or readers exceptions.
     */
    public static CompletableFuture<Void> runAsync(final String[] args, final String applicationName,
                                                   RamLogger outLogger) throws ParseException,
            IllegalArgumentException, IOException, InterruptedException, ExecutionException {
        CompletableFuture<Void> ret;
        try {
            ret = new SbkServerCompletableFutureAsync(args, applicationName, outLogger);
        } catch (InstantiationException ex) {
            ret = new CompletableFuture<>();
            ret.complete(null);
            return ret;
        }
        return ret;
    }

    private static class SbkServerCompletableFutureAsync extends CompletableFuture<Void> {
        private final Benchmark benchmark;
        private final CompletableFuture<Void> ret;

        public SbkServerCompletableFutureAsync(final String[] args, final String applicationName,
                                               RamLogger outLogger) throws ParseException,
                IllegalArgumentException, IOException, InterruptedException, ExecutionException,
                InstantiationException {
            super();
            benchmark = createBenchmark(args, applicationName, outLogger);
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
                                             RamLogger outLogger) throws ParseException, IllegalArgumentException,
            IOException, InstantiationException  {
        final RamParameterOptions params;
        final RamLogger logger;
        final RamConfig ramConfig;
        final Time time;
        final String version = io.sbk.api.impl.Sbk.class.getPackage().getImplementationVersion();
        final String sbkServerName = System.getProperty(Config.SBK_APP_NAME);
        final String sbkAppHome = System.getProperty(Config.SBK_APP_HOME);
        String appName = applicationName;

        if (appName == null) {
            appName = Objects.requireNonNullElse(sbkServerName, APP_NAME);
        }
        Printer.log.info(IOUtils.toString(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(BANNER_FILE)));
        Printer.log.info("Java Runtime Version: " + System.getProperty("java.runtime.version"));
        Printer.log.info("Arguments List: "+Arrays.toString(args));
        Printer.log.info(appName +" Version: "+version);
        Printer.log.info(Config.SBK_APP_HOME+": "+sbkAppHome);

        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ramConfig = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                RamConfig.class);

        logger = Objects.requireNonNullElseGet(outLogger, SbkRamPrometheusLogger::new);

        params = new SbkRamParameters(appName, ramConfig.maxConnections);
        logger.addArgs(params);
        params.parseArgs(args);
        if (params.hasOption("help")) {
            throw new InstantiationException("print help !");
        }
        logger.parseArgs(params);
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

        return new SbkRamBenchmark(ramConfig, params, logger, time);
    }


}

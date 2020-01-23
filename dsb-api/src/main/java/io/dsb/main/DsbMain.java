/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.dsb.main;

import io.dsb.api.Benchmark;
import io.dsb.api.Parameters;
import io.dsb.api.QuadConsumer;
import io.dsb.api.Performance;
import io.dsb.api.Dsb;
import io.dsb.api.Writer;
import io.dsb.api.Reader;
import io.dsb.api.ResultLogger;
import io.dsb.api.SystemResultLogger;

import java.io.IOException;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

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

/**
 * Performance benchmark for Pravega.
 * Data format is in comma separated format as following: {TimeStamp, Sensor Id, Location, TempValue }.
 */
public class DsbMain {
    final static String BENCHMARKNAME = "DSB";
    final static int REPORTINGINTERVAL = 5000;

    public static void main(final String[] args) {
        final long startTime = System.currentTimeMillis();
        final Options options = new Options();
        CommandLine commandline = null;
        String className = null;
        Benchmark obj = null;
        final Parameters params;
        final ExecutorService executor;
        final Performance writeStats;
        final Performance readStats;
        final QuadConsumer writeTime;
        final QuadConsumer readTime;

        options.addOption("class", true, "Benchmark class");
        try {
            commandline = new DefaultParser().parse(options, args, true);
        } catch (ParseException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        className = commandline.getOptionValue("class", null);
        if (className == null) {
            new Parameters(BENCHMARKNAME, startTime).printHelp();
            System.exit(0);
        }

        try {
            obj = (Benchmark) Class.forName("io.dsb." + className+"."+className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        final Benchmark benchmark = obj;
        if (benchmark == null) {
            System.out.println("Failure to create Benchmark object");
            System.exit(0);
        }
        params = new Parameters(BENCHMARKNAME +" "+ className, startTime);
        benchmark.addArgs(params);
        try {
            params.parseArgs(args);
        }  catch (ParseException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        try {
            benchmark.parseArgs(params);
        } catch (IllegalArgumentException ex) {
            if (!params.hasOption("help")) {
                ex.printStackTrace();
            }
            System.exit(0);
        }

        if (params.hasOption("help")) {
            params.printHelp();
            System.exit(0);
        }
        try {
            benchmark.openStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        final ResultLogger logger = new SystemResultLogger();

        final int threadCount = params.writersCount + params.readersCount + 6;
        if (params.fork) {
            executor = new ForkJoinPool(threadCount);
        } else {
            executor = Executors.newFixedThreadPool(threadCount);
        }

        if (params.writersCount > 0 && !params.writeAndRead) {
            writeStats = new Dsb("Writing", REPORTINGINTERVAL, params.recordSize,
                                params.csvFile, logger, executor);
            writeTime = writeStats::recordTime;
        } else {
            writeStats = null;
            writeTime = null;
        }

        if (params.readersCount > 0) {
            String action;
            if (params.writeAndRead) {
                action = "Write/Reading";
              } else {
                action = "Reading";
            }
            readStats = new Dsb(action, REPORTINGINTERVAL, params.recordSize,
                            params.csvFile, logger, executor);
            readTime = readStats::recordTime;
        } else {
            readStats = null;
            readTime = null;
        }

        try {
            final List<Writer> writers =  IntStream.range(0, params.writersCount)
                                            .boxed()
                                            .map(i -> benchmark.createWriter(i, params, writeTime))
                                            .collect(Collectors.toList());

            final List<Reader> readers = IntStream.range(0, params.readersCount)
                                            .boxed()
                                            .map(i -> benchmark.createReader(i, params, readTime))
                                            .collect(Collectors.toList());

            final List<Callable<Void>> workers = Stream.of(readers, writers)
                    .filter(x -> x != null)
                    .flatMap(x -> x.stream())
                    .collect(Collectors.toList());

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        System.out.println();
                        executor.shutdown();
                        executor.awaitTermination(1, TimeUnit.SECONDS);
                        if (writeStats != null && !params.writeAndRead) {
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

            if (writeStats != null && !params.writeAndRead) {
                writeStats.start(startTime);
            }
            if (readStats != null) {
                readStats.start(startTime);
            }

            executor.invokeAll(workers);
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
            if (writeStats != null && !params.writeAndRead) {
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
}

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

import io.sbk.api.Benchmark;
import io.sbk.api.Parameters;
import io.sbk.api.Performance;
import io.sbk.api.SbkPerformance;
import io.sbk.api.Writer;
import io.sbk.api.Reader;
import io.sbk.api.ResultLogger;
import io.sbk.api.SystemResultLogger;

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
 * Main class of SBK.
 */
public class SbkMain {
    final static String BENCHMARKNAME = "sbk";
    final static int REPORTINGINTERVAL = 5000;

    public static void main(final String[] args) {
        long startTime = System.currentTimeMillis();
        final Options options = new Options();
        CommandLine commandline = null;
        String className = null;
        Benchmark obj = null;
        final Parameters params;
        final ExecutorService executor;
        final Performance writeStats;
        final Performance readStats;

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
            obj = (Benchmark) Class.forName("io.sbk." + className + "." + className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        final Benchmark benchmark = obj;
        if (benchmark == null) {
            System.out.println("Failure to create Benchmark object");
            System.exit(0);
        }
        params = new Parameters(BENCHMARKNAME +" -class "+ className, startTime);
        benchmark.addArgs(params);
        try {
            params.parseArgs(args);
        }  catch (ParseException ex) {
            ex.printStackTrace();
            params.printHelp();
            System.exit(0);
        }
        if (params.hasOption("help")) {
            System.exit(0);
        }

        try {
            benchmark.parseArgs(params);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
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
            writeStats = new SbkPerformance("Writing", REPORTINGINTERVAL, params.recordSize,
                                params.csvFile, logger, executor);
            params.recordWrite = writeStats::recordTime;
        } else {
            writeStats = null;
            params.recordWrite = null;
        }

        if (params.readersCount > 0) {
            String action;
            if (params.writeAndRead) {
                action = "Write/Reading";
              } else {
                action = "Reading";
            }
            readStats = new SbkPerformance(action, REPORTINGINTERVAL, params.recordSize,
                            params.csvFile, logger, executor);
            params.recordRead = readStats::recordTime;
        } else {
            readStats = null;
            params.recordRead = null;
        }

        try {
            final List<Writer> writers =  IntStream.range(0, params.writersCount)
                                            .boxed()
                                            .map(i -> benchmark.createWriter(i, params))
                                            .collect(Collectors.toList());

            final List<Reader> readers = IntStream.range(0, params.readersCount)
                                            .boxed()
                                            .map(i -> benchmark.createReader(i, params))
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
            startTime = System.currentTimeMillis();
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

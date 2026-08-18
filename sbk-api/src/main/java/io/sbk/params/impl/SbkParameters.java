/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.params.impl;

import io.perl.config.PerlConfig;
import io.sbk.action.Action;
import io.sbk.config.SbkRuntimeConfig;
import io.sbk.params.InputParameterOptions;
import io.sbk.config.Config;
import io.sbk.exception.HelpException;
import io.sbk.thread.ThreadType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses and exposes common SBK benchmark parameters.
 *
 * <p>Builds the CLI schema for core options (writers/readers, size, records, seconds,
 * throughput, step controls, read-only, idle sleep) and maps parsed values into
 * typed getters via Lombok {@link Getter} annotations.
 *
 * <p>Semantics (high-level):
 * - **writers/readers**: concurrency configuration; at least one must be > 0.
 * - **size**: record size (bytes); required if workers > 0.
 * - **records/seconds/throughput**: determine rate control and runtime.
 * - **wstep/wsec, rstep/rsec**: step ramping configuration.
 * - **ro**: read-only when both writers and readers are set.
 * - **millisecsleep**: idle sleep in milliseconds between operations.
 */
@Slf4j
public sealed class SbkParameters extends SbkInputOptions implements InputParameterOptions
        permits SbkDriversParameters {

    /** CLI option selecting the intrusive or JDK timestamp queue. */
    public static final String MPSC_QUEUE_OPTION = "mpscqueue";

    private static final String CONFIG_FILE = "sbk.properties";

    @Getter
    final private int timeoutMS;

    @Getter
    private int recordSize;

    @Getter
    private int writersCount;

    @Getter
    private int readersCount;

    @Getter
    private int recordsPerSec;

    @Getter
    private int recordsPerSync;

    @Getter
    private long totalRecords;

    @Getter
    private long totalSecondsToRun;

    @Getter
    private int writersStep;

    @Getter
    private int writersStepSeconds;

    @Getter
    private int readersStep;

    @Getter
    private int readersStepSeconds;

    @Getter
    private Action action;

    @Getter
    private ThreadType threadType;

    @Getter
    private int idleSleepMilliSeconds;

    @Getter
    private boolean mpscQueueEnabled;

    /**
     * Construct parameters with the given benchmark name and description.
     * Registers the standard SBK options and default values.
     *
     * @param name benchmark name
     * @param desc help description
     */
    public SbkParameters(String name, String desc) {
        super(name, desc);
        this.timeoutMS = SbkRuntimeConfig.get().defaultOperationTimeoutMillis;
        this.action = Action.Reading;
        final PerlConfig perlDefaults = loadPerlDefaults();
        this.mpscQueueEnabled = perlDefaults.mpscQueueEnable;
        validatePerlQueueTopology(perlDefaults);

        addOption("writers", true, "Number of writers");
        addOption("readers", true, "Number of readers");
        addOption("size", true, "Size of each message (event or record)");
        addOption("records", true,
                """
                        Number of records(events) if 'seconds' not specified;
                        otherwise, Maximum records per second by writer(s); and/or
                        Number of records per second by reader(s)""");
        addOption("sync", true,
                """
                        Each Writer calls flush/sync after writing <arg> number of of events(records); and/or
                        <arg> number of events(records) per Write or Read Transaction""");
        addOption("seconds", true,
                """
                        Number of seconds to run
                        if not specified, runs forever""");
        addOption("throughput", true,
                """
                        If > 0, throughput in MB/s
                        If 0, writes/reads 'records'
                        If -1, get the maximum throughput (default: -1)""");
        addOption("wstep", true,
                "Number of writers/step, default: 1");
        addOption("wsec", true,
                "Number of seconds/step for writers, default: 0");
        addOption("rstep", true,
                "Number of readers/step, default: 1");
        addOption("rsec", true,
                "Number of seconds/step for readers, default: 0");
        addOption("ro", true,
                """
                           Readonly Benchmarking,
                           Applicable only if both writers and readers are set; default: false""");
        addOption("millisecsleep", true, "Idle sleep in milliseconds; default: 0 ms");
        addOption("thread", true,
                "Thread Type [p: platform, f: fork-join, v: virtual], default: v");
        addOption(MPSC_QUEUE_OPTION, true,
                ("PerL timestamp queue [true: TimeStampMpscQueue,%n"
                        + "false: JDK ConcurrentLinkedQueue];%n"
                        + "default: %s").formatted(this.mpscQueueEnabled));
    }

    /**
     * Construct parameters using the default description.
     *
     * @param name benchmark name
     */
    public SbkParameters(String name) {
        this(name, Config.DESC);
    }

    /**
     * Parse SBK core options and compute derived values (e.g., recordsPerSec, totalSecondsToRun).
     * Validates required combinations (e.g., at least one of writers/readers must be > 0).
     * May throw {@link HelpException} via the superclass when help is requested.
     *
     * @param args command-line arguments to parse
     * @throws ParseException if an option cannot be parsed
     * @throws IllegalArgumentException if an option value or combination is invalid
     * @throws HelpException if help was requested
     */
    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        super.parseArgs(args);
        final boolean writeReadOnly = Boolean.parseBoolean(getOptionValue("ro", "false"));
        writersCount = Integer.parseInt(getOptionValue("writers", "0"));
        readersCount = Integer.parseInt(getOptionValue("readers", "0"));

        if (writersCount == 0 && readersCount == 0) {
            throw new IllegalArgumentException("Error: Must specify the number of writers or readers");
        }

        totalRecords = Long.parseLong(getOptionValue("records", "0"));
        recordSize = Integer.parseInt(getOptionValue("size", "0"));
        if (recordSize <= 0) {
            throw new IllegalArgumentException(
                    "Error: The record 'size' must be greater than zero");
        }
        int syncRecords = Integer.parseInt(getOptionValue("sync", "0"));
        if (syncRecords > 0) {
            recordsPerSync = syncRecords;
        } else {
            recordsPerSync = Integer.MAX_VALUE;
        }

        if (hasOptionValue("seconds")) {
            totalSecondsToRun = Long.parseLong(getOptionValue("seconds"));
        } else if (totalRecords > 0) {
            totalSecondsToRun = 0;
        } else {
            totalSecondsToRun = PerlConfig.DEFAULT_RUNTIME_SECONDS;
        }

        final double throughput;
        if (hasOptionValue("throughput")) {
            throughput = Double.parseDouble(getOptionValue("throughput"));
        } else {
            throughput = -1;
        }

        writersStep = Integer.parseInt(getOptionValue("wstep", "1"));
        writersStepSeconds = Integer.parseInt(getOptionValue("wsec", "0"));
        readersStep = Integer.parseInt(getOptionValue("rstep", "1"));
        readersStepSeconds = Integer.parseInt(getOptionValue("rsec", "0"));
        idleSleepMilliSeconds = Integer.parseInt(getOptionValue("millisecsleep", "0"));
        parseMpscQueueOption();

        int workersCnt = writersCount;
        if (workersCnt == 0) {
            workersCnt = readersCount;
        }

        if (throughput < 0 && totalSecondsToRun > 0) {
            long recsPerSec = totalRecords / workersCnt;
            if (recsPerSec > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Error: The Records per Second value :" + recsPerSec + "is more than " + Integer.MAX_VALUE);
            }
            recordsPerSec = (int) recsPerSec;
        } else if (throughput > 0) {
            recordsPerSec = (int) (((throughput * 1024 * 1024) / recordSize) / workersCnt);
        } else {
            recordsPerSec = 0;
        }

        if (writersCount > 0 && readersCount > 0) {
            if (writeReadOnly) {
                action = Action.Write_OnlyReading;
            } else {
                action = Action.Write_Reading;
            }
        } else if (writersCount > 0) {
            action = Action.Writing;
        }

        String threadString = getOptionValue("thread", "v");
        threadType = switch (threadString.toLowerCase()) {
            case "f" -> ThreadType.ForkJoin;
            case "v" -> ThreadType.Virtual;
            default -> ThreadType.Platform;
        };

    }

    private void parseMpscQueueOption() {
        final String queueEnabled = getOptionValue(MPSC_QUEUE_OPTION,
                Boolean.toString(mpscQueueEnabled));
        if (!"true".equalsIgnoreCase(queueEnabled)
                && !"false".equalsIgnoreCase(queueEnabled)) {
            throw new IllegalArgumentException("Error: The option '-"
                    + MPSC_QUEUE_OPTION + "' must be true or false");
        }
        mpscQueueEnabled = Boolean.parseBoolean(queueEnabled);
    }

    private static void validatePerlQueueTopology(PerlConfig config) {
        if (config.maxQs < 0) {
            throw new IllegalArgumentException(
                    "Error: sbk.properties maxQs must be zero or greater");
        }

        if (config.qPerWorker < PerlConfig.MIN_Q_PER_WORKER) {
            throw new IllegalArgumentException(
                    "Error: sbk.properties qPerWorker must be at least "
                    + PerlConfig.MIN_Q_PER_WORKER);
        }
    }

    private static PerlConfig loadPerlDefaults() {
        try {
            return loadPerlConfig();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load PerL defaults from " + CONFIG_FILE,
                    exception);
        }
    }

    /**
     * Load the PerL defaults bundled with the SBK application.
     *
     * @return property-backed PerL configuration
     * @throws IOException if {@code sbk.properties} is missing or invalid
     */
    public static PerlConfig loadPerlConfig() throws IOException {
        final InputStream inputStream = SbkParameters.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE);
        if (inputStream == null) {
            throw new IOException("Missing classpath resource: " + CONFIG_FILE);
        }
        try (inputStream) {
            return PerlConfig.build(inputStream);
        }
    }
}

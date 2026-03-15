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
import io.sbk.params.InputParameterOptions;
import io.sbk.config.Config;
import io.sbk.exception.HelpException;
import io.sbk.thread.ThreadType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;

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


    /**
     * Construct parameters with the given benchmark name and description.
     * Registers the standard SBK options and default values.
     *
     * @param name benchmark name
     * @param desc help description
     */
    public SbkParameters(String name, String desc) {
        super(name, desc);
        this.timeoutMS = PerlConfig.DEFAULT_TIMEOUT_MS;
        this.action = Action.Reading;

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
                "Thread Type [p: platform, f: fork-join, v:virtual], default: p");
    }

    /**
     * Construct parameters using the default description.
     *
     * @param name benchmark name
     */
    public SbkParameters(String name) {
        this(name, Config.DESC);
    }


    @Override
    /**
     * Parse SBK core options and compute derived values (e.g., recordsPerSec, totalSecondsToRun).
     * Validates required combinations (e.g., at least one of writers/readers must be > 0).
     * May throw {@link HelpException} via the superclass when help is requested.
     */
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

        if (workersCnt > 0) {
            if (recordSize == 0) {
                throw new IllegalArgumentException("Error: Must specify the record 'size'");
            }
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

        String threadString = getOptionValue("thread", "p");
        threadType = switch (threadString.toLowerCase()) {
            case "f" -> ThreadType.ForkJoin;
            case "v" -> ThreadType.Virtual;
            default -> ThreadType.Platform;
        };

    }
}

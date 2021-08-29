/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.sbk.config.Config;
import io.sbk.logger.PerformanceLogger;
import io.sbk.system.Printer;
import io.sbk.time.Time;
import io.sbk.time.TimeUnit;
import io.sbk.time.impl.MicroSeconds;
import io.sbk.time.impl.MilliSeconds;
import io.sbk.time.impl.NanoSeconds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SbkUtils {

    public static Time getTime(PerformanceLogger logger) {
        final TimeUnit timeUnit = logger.getTimeUnit();
        final Time ret;
        if (timeUnit == TimeUnit.mcs) {
            ret = new MicroSeconds();
        } else if (timeUnit == TimeUnit.ns) {
            ret = new NanoSeconds();
        } else {
            ret = new MilliSeconds();
        }
        Printer.log.info("Time Unit: " + ret.getTimeUnit().toString());
        Printer.log.info("Minimum Latency: " + logger.getMinLatency() + " " + ret.getTimeUnit().name());
        Printer.log.info("Maximum Latency: " + logger.getMaxLatency() + " " + ret.getTimeUnit().name());
        return ret;
    }

    public static String[] removeOptionArgsAndValues(String[] args, String[] opts) {
        if (args == null) {
            return new String[0];
        }
        if (args.length < 2) {
            return args;
        }
        final List<String> optsList = Arrays.asList(opts);
        final List<String> ret = new ArrayList<>(args.length);
        int i = 0;
        while (i < args.length) {
            if (optsList.contains(args[i])) {
                i += 1;
                optsList.remove(args[i]);
            } else {
                ret.add(args[i]);
            }
            i += 1;
        }
        return ret.toArray(new String[0]);
    }


    public static String getClassName(String[] args) {
        if (args == null || args.length < 2) {
            return "";
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(Config.CLASS_OPTION_ARG)) {
                if (i + 1 < args.length) {
                    return args[i + 1];
                } else {
                    return "";
                }
            }
        }
        return "";
    }

    public static boolean hasHelp(String[] args) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (arg.equals(Config.HELP_OPTION_ARG)) {
                return true;
            }
        }
        return false;
    }

}


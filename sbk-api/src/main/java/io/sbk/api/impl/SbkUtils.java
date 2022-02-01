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
import io.sbk.logger.PerformancePrinter;
import io.sbk.system.Printer;
import io.time.MicroSeconds;
import io.time.MilliSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import io.time.TimeUnit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final public class SbkUtils {

    public static @NotNull Time getTime(@NotNull PerformancePrinter printer) {
        final TimeUnit timeUnit = printer.getTimeUnit();
        final Time ret = switch (timeUnit) {
            case mcs -> new MicroSeconds();
            case ns -> new NanoSeconds();
            default -> new MilliSeconds();
        };
        Printer.log.info("Time Unit: " + ret.getTimeUnit().toString());
        Printer.log.info("Minimum Latency: " + printer.getMinLatency() + " " + ret.getTimeUnit().name());
        Printer.log.info("Maximum Latency: " + printer.getMaxLatency() + " " + ret.getTimeUnit().name());
        return ret;
    }

    @Contract("null, _ -> new")
    public static @NotNull String[] removeOptionArgsAndValues(String[] args, String[] opts) {
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


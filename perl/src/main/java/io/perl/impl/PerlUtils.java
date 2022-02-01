/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.impl;

import io.perl.PerformanceLogger;
import io.perl.PerlPrinter;
import io.time.MicroSeconds;
import io.time.MilliSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import io.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

final public class PerlUtils {

    public static @NotNull Time getTime(@NotNull PerformanceLogger printer) {
        final TimeUnit timeUnit = printer.getTimeUnit();
        final Time ret = switch (timeUnit) {
            case mcs -> new MicroSeconds();
            case ns -> new NanoSeconds();
            default -> new MilliSeconds();
        };
        PerlPrinter.log.info("Time Unit: " + ret.getTimeUnit().toString());
        PerlPrinter.log.info("Minimum Latency: " + printer.getMinLatency() + " " + ret.getTimeUnit().name());
        PerlPrinter.log.info("Maximum Latency: " + printer.getMaxLatency() + " " + ret.getTimeUnit().name());
        return ret;
    }

}

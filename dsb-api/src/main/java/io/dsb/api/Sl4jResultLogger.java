/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.dsb.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sl4jResultLogger implements ResultLogger {
    final private Logger log;

    public Sl4jResultLogger() {
        log = LoggerFactory.getLogger("DSB");
    }

    public void print(String action, long records, double recsPerSec, double mbPerSec, double avglatency, double maxlatency) {
        log.info(String.format("%s %10d records, %9.1f records/sec, %7.2f MB/sec, %7.1f ms avg latency, %7.1f ms max latency",
                action, records, recsPerSec, mbPerSec, avglatency, maxlatency));
    }

    public void printLatencies(String action, int one, int two, int three, int four, int five, int six) {
        log.info(String.format("%s %d ms 50th, %d ms 75th, %d ms 95th, %d ms 99th, %d ms 99.9th, %d ms 99.99th.",
                action, one, two, three, four, five, six));
    }

    public void printDiscardedLatencies(String action, int discard) {
        if (discard > 0) {
            log.info(String.format("%s %d\n", action, discard));
        }
    }
}

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

import io.sbk.api.ResultLogger;

/**
 * Class for recoding/printing results on System.out.
 */
public class SystemResultLogger implements ResultLogger {

    public SystemResultLogger() {
        return;
    }

    @Override
    public void print(String action, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      int maxLatency, long discard, int one, int two, int three, int four, int five, int six,
                      int seven, int eight) {
        System.out.printf("%s %10d records, %9.1f records/sec, %8.2f MB/sec, %8.1f ms avg latency, %7d ms max latency;" +
                        "%8d discarded latencies; " +
                        "Percentiles: %7d ms 10th, %7d ms 25th, %7d ms 50th, %7d ms 75th, %7d ms 95th, %7d ms 99th, %7d ms 99.9th, %7d ms 99.99th.\n",
                action, records, recsPerSec, mbPerSec, avgLatency, maxLatency, discard, one, two, three,
                four, five, six, seven, eight);
    }
}

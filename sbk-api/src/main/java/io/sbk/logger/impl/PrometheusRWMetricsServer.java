/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tags;
import io.perl.logger.impl.PrometheusMetricsServer;
import io.sbk.config.Config;
import io.sbk.logger.MetricsConfig;
import io.sbk.logger.RWPrint;
import io.time.Time;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public class PrometheusRWMetricsServer extends PrometheusMetricsServer implements RWPrint {
    final private AtomicInteger writers;
    final private AtomicInteger readers;
    final private AtomicInteger maxWriters;
    final private AtomicInteger maxReaders;

    final private Counter writeRequestBytes;
    final private Counter readRequestBytes;
    final private Counter writeRequests;
    final private Counter readRequests;
    final private AtomicDouble writeRequestsMbPerSec;
    final private AtomicDouble writeRequestsPerSec;
    final private AtomicDouble readRequestsMbPerSec;
    final private AtomicDouble readRequestsPerSec;

    public  PrometheusRWMetricsServer(String header, String action, String className, double[] percentiles, Time time,
                                        MetricsConfig config) throws IOException {
        super(header.toUpperCase()+" "+action, percentiles, time,
                config.latencyTimeUnit, config.port, config.context, Tags.of(Config.CLASS_OPTION, className));
        final String writersName = metricPrefix + "_Writers";
        final String readersName = metricPrefix + "_Readers";
        final String maxWritersName = metricPrefix + "_Max_Writers";
        final String maxReadersName = metricPrefix + "_Max_Readers";
        final String writeRequestBytesName = metricPrefix + "_Write_RequestBytes";
        final String writeRequestsName = metricPrefix + "_Write_Requests";
        final String writeRequestsMbPerSecName = metricPrefix + "_Write_RequestBytes_MBPerSec";
        final String writeRequestsPerSecName =  metricPrefix + "_Write_Requests_MBPerSec";
        final String readRequestBytesName = metricPrefix + "_Read_RequestBytes";
        final String readRequestsName = metricPrefix + "_Read_Requests";
        final String readRequestsMbPerSecName = metricPrefix + "_Read_RequestBytes_MBPerSec";
        final String readRequestsPerSecName =  metricPrefix + "_Read_Requests_MBPerSec";

        this.writers = this.registry.gauge(writersName, new AtomicInteger());
        this.readers = this.registry.gauge(readersName, new AtomicInteger());
        this.maxWriters = this.registry.gauge(maxWritersName, new AtomicInteger());
        this.maxReaders = this.registry.gauge(maxReadersName, new AtomicInteger());
        this.writeRequestBytes = this.registry.counter(writeRequestBytesName);
        this.writeRequests = this.registry.counter(writeRequestsName);
        this.writeRequestsMbPerSec = this.registry.gauge(writeRequestsMbPerSecName, new AtomicDouble());
        this.writeRequestsPerSec = this.registry.gauge(writeRequestsPerSecName, new AtomicDouble());
        this.readRequestBytes = this.registry.counter(readRequestBytesName);
        this.readRequests = this.registry.counter(readRequestsName);
        this.readRequestsMbPerSec = this.registry.gauge(readRequestsMbPerSecName, new AtomicDouble());
        this.readRequestsPerSec = this.registry.gauge(readRequestsPerSecName, new AtomicDouble());

    }


    @Override
    public void print(int writers, int maxWriters, int readers, int maxReaders, long writeRequestBytes,
                      double writeRequestsMbPerSec, long writeRequests, double writeRequestsPerSec,
                      long readRequestBytes, double readRequestsMbPerSec, long readRequests,
                      double readRequestsPerSec, double seconds, long bytes, long records, double recsPerSec,
                      double mbPerSec, double avgLatency, long minLatency, long maxLatency, long invalid,
                      long lowerDiscard, long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        this.writers.set(writers);
        this.maxWriters.set(maxWriters);
        this.readers.set(readers);
        this.maxReaders.set(maxReaders);
        this.writeRequestBytes.increment(writeRequestBytes);
        this.writeRequests.increment(writeRequests);
        this.writeRequestsMbPerSec.set(writeRequestsMbPerSec);
        this.writeRequestsPerSec.set(writeRequestsPerSec);
        this.readRequestBytes.increment(readRequestBytes);
        this.readRequests.increment(readRequests);
        this.readRequestsMbPerSec.set(readRequestsMbPerSec);
        this.readRequestsPerSec.set(readRequestsPerSec);

    }
}

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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus metrics server adapter for SBK read/write benchmarks.
 *
 * <p>This class bridges SBK's {@link RWPrint} reporting callbacks to Micrometer counters and gauges
 * and exposes them via the embedded HTTP server implemented in {@link PrometheusMetricsServer} so
 * that Prometheus can scrape them. It publishes per-interval throughput, request counts, pending
 * records/bytes, timeout events, as well as statistical latency metrics (avg/min/max/discards and
 * percentiles).
 *
 * <p>Metrics are tagged with the SBK storage class name (tag key: {@code class}) and the action
 * (tag key: {@code action}). Metric names are prefixed using an upper-cased header (for example,
 * {@code SBK_READ}) and follow a stable schema, such as
 * {@code <prefix>_Write_Request_Bytes}, {@code <prefix>_Read_Request_RecordsPerSec}, etc.
 */
public class SbkPrometheusServer extends PrometheusMetricsServer implements RWPrint {
    final private static String ACTION_TEXT = "action";
    /** Metric name prefix derived from the header (upper-cased and '_' separated). */
    final protected String rwMetricPrefix;
    /** Gauges tracking current and maximum active writers/readers. */
    final private AtomicInteger writers;
    final private AtomicInteger readers;
    final private AtomicInteger maxWriters;
    final private AtomicInteger maxReaders;
    /** Cumulative counters for request bytes and records. */
    final private Counter writeRequestBytes;
    final private Counter readRequestBytes;
    final private Counter writeRequestRecords;
    final private Counter readRequestRecords;
    /** Per-interval rates exposed as gauges. */
    final private AtomicDouble writeRequestsMbPerSec;
    final private AtomicDouble writeRequestRecordsPerSec;
    final private AtomicDouble readRequestsMbPerSec;
    final private AtomicDouble readRequestRecordsPerSec;

    final private AtomicLong writeResponsePendingRecords;

    final private AtomicLong writeResponsePendingBytes;

    final private AtomicLong readResponsePendingRecords;

    final private AtomicLong readResponsePendingBytes;

    final private AtomicLong writeReadRequestPendingRecords;

    final private AtomicLong writeReadRequestPendingBytes;

    final private Counter writeTimeoutEvents;

    final private AtomicDouble writeTimeoutEventsPerSec;

    final private Counter readTimeoutEvents;

    final private AtomicDouble readTimeoutEventsPerSec;

    /**
     * Construct the SBK Prometheus metrics server.
     *
     * @param header       SBK header (used to derive the metric name prefix)
     * @param action       SBK action (READ/WRITE)
     * @param className    storage implementation class name (exposed as a tag)
     * @param percentiles  percentile configuration to publish
     * @param time         time provider for latency measurements
     * @param config       metrics endpoint configuration (port, context, latency time unit)
     * @throws IOException if the underlying server cannot be initialized
     */
    public SbkPrometheusServer(String header, String action, String className, double[] percentiles, Time time,
                               MetricsConfig config) throws IOException {
        super(header.toUpperCase()+" "+action, percentiles, time,
                config.latencyTimeUnit, config.port, config.context,
                Tags.of(Config.CLASS_OPTION, className, ACTION_TEXT, action));
        rwMetricPrefix =   header.toUpperCase().replace(" ", "_");
        final String writersName = rwMetricPrefix + "_Writers";
        final String readersName = rwMetricPrefix + "_Readers";
        final String maxWritersName = rwMetricPrefix + "_Max_Writers";
        final String maxReadersName = rwMetricPrefix + "_Max_Readers";
        final String writeRequestBytesName = rwMetricPrefix + "_Write_Request_Bytes";
        final String writeRequestRecordsName = rwMetricPrefix + "_Write_Request_Records";
        final String writeRequestsMbPerSecName = rwMetricPrefix + "_Write_Request_Bytes_MBPerSec";
        final String writeRequestRecordsPerSecName =  rwMetricPrefix + "_Write_Request_RecordsPerSec";
        final String readRequestBytesName = rwMetricPrefix + "_Read_Request_Bytes";
        final String readRequestRecordsName = rwMetricPrefix + "_Read_Request_Records";
        final String readRequestsMbPerSecName = rwMetricPrefix + "_Read_Request_MBPerSec";
        final String readRequestRecordsPerSecName =  rwMetricPrefix + "_Read_Request_RecordsPerSec";
        final String writeResponsePendingRecordsName = rwMetricPrefix+"_Write_Response_Pending_Records";
        final String writeResponsePendingBytesName = rwMetricPrefix+"_Write_Response_Pending_Bytes";
        final String readResponsePendingRecordsName = rwMetricPrefix+"_Read_Response_Pending_Records";
        final String readResponsePendingBytesName = rwMetricPrefix+"_Read_Response_Pending_Bytes";
        final String writeReadPendingRecordsName = rwMetricPrefix+"_Write_Read_Request_Pending_Records";
        final String writeReadPendingBytesName = rwMetricPrefix+"_Write_Read_Request_Pending_Bytes";
        final String writeTimeoutEventsName = rwMetricPrefix+"_Write_Timeout_Events";
        final String writeTimeoutEventsPerSecName = rwMetricPrefix+"_Write_Timeout_Events_PerSec";
        final String readTimeoutEventsName = rwMetricPrefix+"_Read_Timeout_Events";
        final String readTimeoutEventsPerSecName = rwMetricPrefix+"_Read_Timeout_Events_PerSec";

        this.writers = this.registry.gauge(writersName, new AtomicInteger());
        this.readers = this.registry.gauge(readersName, new AtomicInteger());
        this.maxWriters = this.registry.gauge(maxWritersName, new AtomicInteger());
        this.maxReaders = this.registry.gauge(maxReadersName, new AtomicInteger());
        this.writeRequestBytes = this.registry.counter(writeRequestBytesName);
        this.writeRequestRecords = this.registry.counter(writeRequestRecordsName);
        this.writeRequestsMbPerSec = this.registry.gauge(writeRequestsMbPerSecName, new AtomicDouble());
        this.writeRequestRecordsPerSec = this.registry.gauge(writeRequestRecordsPerSecName, new AtomicDouble());
        this.readRequestBytes = this.registry.counter(readRequestBytesName);
        this.readRequestRecords = this.registry.counter(readRequestRecordsName);
        this.readRequestsMbPerSec = this.registry.gauge(readRequestsMbPerSecName, new AtomicDouble());
        this.readRequestRecordsPerSec = this.registry.gauge(readRequestRecordsPerSecName, new AtomicDouble());
        this.writeResponsePendingRecords = this.registry.gauge(writeResponsePendingRecordsName, new AtomicLong());
        this.writeResponsePendingBytes = this.registry.gauge(writeResponsePendingBytesName, new AtomicLong());
        this.readResponsePendingRecords = this.registry.gauge(readResponsePendingRecordsName, new AtomicLong());
        this.readResponsePendingBytes = this.registry.gauge(readResponsePendingBytesName, new AtomicLong());
        this.writeReadRequestPendingRecords = this.registry.gauge(writeReadPendingRecordsName, new AtomicLong());
        this.writeReadRequestPendingBytes = this.registry.gauge(writeReadPendingBytesName, new AtomicLong());
        this.writeTimeoutEvents = this.registry.counter(writeTimeoutEventsName);
        this.writeTimeoutEventsPerSec = this.registry.gauge(writeTimeoutEventsPerSecName, new AtomicDouble());
        this.readTimeoutEvents = this.registry.counter(readTimeoutEventsName);
        this.readTimeoutEventsPerSec = this.registry.gauge(readTimeoutEventsPerSecName, new AtomicDouble());
    }


    /**
     * Update Micrometer counters/gauges and delegate latency stats to the base server.
     *
     * @param writers                        current writers
     * @param maxWriters                     max writers observed
     * @param readers                        current readers
     * @param maxReaders                     max readers observed
     * @param writeRequestBytes              write request bytes in this interval
     * @param writeRequestMbPerSec           write throughput MB/sec
     * @param writeRequestRecords            write request count
     * @param writeRequestRecordsPerSec      write requests/sec
     * @param readRequestBytes               read request bytes in this interval
     * @param readRequestMbPerSec            read throughput MB/sec
     * @param readRequestRecords             read request count
     * @param readRequestRecordsPerSec       read requests/sec
     * @param writeResponsePendingRecords    pending write response records
     * @param writeResponsePendingBytes      pending write response bytes
     * @param readResponsePendingRecords     pending read response records
     * @param readResponsePendingBytes       pending read response bytes
     * @param writeReadRequestPendingRecords write-read pending records
     * @param writeReadRequestPendingBytes   write-read pending bytes
     * @param writeTimeoutEvents             write timeout events
     * @param writeTimeoutEventsPerSec       write timeout events/sec
     * @param readTimeoutEvents              read timeout events
     * @param readTimeoutEventsPerSec        read timeout events/sec
     * @param seconds                        reporting period seconds
     * @param bytes                          total bytes in period
     * @param records                        total records in period
     * @param recsPerSec                     records/sec
     * @param mbPerSec                       MB/sec
     * @param avgLatency                     average latency
     * @param minLatency                     minimum latency
     * @param maxLatency                     maximum latency
     * @param invalid                        invalid/negative latency count
     * @param lowerDiscard                   discarded below min latency
     * @param higherDiscard                  discarded above max latency
     * @param slc1                           sliding latency coverage 1
     * @param slc2                           sliding latency coverage 2
     * @param percentileLatencies            percentile latency values
     * @param percentileLatencyCounts        percentile latency counts
     */
    @Override
    public final void print(int writers, int maxWriters, int readers, int maxReaders, long writeRequestBytes,
                            double writeRequestMbPerSec, long writeRequestRecords, double writeRequestRecordsPerSec,
                            long readRequestBytes, double readRequestMbPerSec, long readRequestRecords,
                            double readRequestRecordsPerSec, long writeResponsePendingRecords,
                            long writeResponsePendingBytes, long readResponsePendingRecords,
                            long readResponsePendingBytes, long writeReadRequestPendingRecords,
                            long writeReadRequestPendingBytes,
                            long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                            long readTimeoutEvents, double readTimeoutEventsPerSec,
                            double seconds, long bytes, long records, double recsPerSec,
                            double mbPerSec, double avgLatency, long minLatency, long maxLatency, long invalid,
                            long lowerDiscard, long higherDiscard, long slc1, long slc2, long[] percentileLatencies,
                            long[] percentileLatencyCounts) {
        this.writers.set(writers);
        this.maxWriters.set(maxWriters);
        this.readers.set(readers);
        this.maxReaders.set(maxReaders);
        this.writeRequestBytes.increment(writeRequestBytes);
        this.writeRequestRecords.increment(writeRequestRecords);
        this.writeRequestsMbPerSec.set(writeRequestMbPerSec);
        this.writeRequestRecordsPerSec.set(writeRequestRecordsPerSec);
        this.readRequestBytes.increment(readRequestBytes);
        this.readRequestRecords.increment(readRequestRecords);
        this.readRequestsMbPerSec.set(readRequestMbPerSec);
        this.readRequestRecordsPerSec.set(readRequestRecordsPerSec);
        this.writeResponsePendingRecords.set(writeResponsePendingRecords);
        this.writeResponsePendingBytes.set(writeResponsePendingBytes);
        this.readResponsePendingRecords.set(readResponsePendingRecords);
        this.readResponsePendingBytes.set(readResponsePendingBytes);
        this.writeReadRequestPendingRecords.set(writeReadRequestPendingRecords);
        this.writeReadRequestPendingBytes.set(writeReadRequestPendingBytes);
        this.writeTimeoutEvents.increment(writeTimeoutEvents);
        this.writeTimeoutEventsPerSec.set(writeTimeoutEventsPerSec);
        this.readTimeoutEvents.increment(readTimeoutEvents);
        this.readTimeoutEventsPerSec.set(readTimeoutEventsPerSec);
        super.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                                higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
    }
}

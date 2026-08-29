/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.api.impl;

import io.perl.api.LatencyPercentiles;
import io.perl.api.ReportLatencies;
import io.perl.api.impl.HybridPagedLatencyRecorder;
import io.perl.logger.Print;
import io.sbk.logger.ReadRequestsLogger;
import io.sbk.logger.SetRW;
import io.sbk.logger.WriteRequestsLogger;
import io.sbp.grpc.MessageLatenciesRecord;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.mock;

/** Exercises exact periodic aggregation with several thousand registered-client identities. */
final class SbmExactAggregationScaleTest {
    private static final int CLIENTS = 5_000;
    private static final int LATENCIES_PER_CLIENT = 64;
    private static final double[] PERCENTILES = new double[]{0.5, 0.9, 0.99};

    @Test
    void aggregatesFiveThousandClientBatchesWithinOneReportingInterval() {
        final HybridPagedLatencyRecorder window = recorder(64);
        final HybridPagedLatencyRecorder totalWindow = recorder(128);
        final SbmTotalWindowLatencyPeriodicRecorder recorder =
                new SbmTotalWindowLatencyPeriodicRecorder(window, totalWindow,
                        mock(Print.class), mock(Print.class), mock(ReportLatencies.class), mock(SetRW.class),
                        mock(WriteRequestsLogger.class), mock(ReadRequestsLogger.class), CLIENTS);
        window.reset(0);
        totalWindow.reset(0);

        assertTimeout(Duration.ofSeconds(5), () -> {
            for (int client = 0; client < CLIENTS; client++) {
                recorder.addLatenciesRecord(clientRecord(client + 1L));
            }
            recorder.flush(5_000);
        });

        final long expectedRecords = (long) CLIENTS * LATENCIES_PER_CLIENT;
        assertEquals(expectedRecords, window.getTotalRecords());
        assertEquals(expectedRecords, totalWindow.getTotalRecords());
        final LatencyPercentiles totalPercentiles = new LatencyPercentiles(PERCENTILES);
        totalWindow.copyPercentiles(totalPercentiles, null);
        assertEquals(32, totalPercentiles.medianLatency);
        assertEquals(57, totalPercentiles.latencies[1]);
        assertEquals(63, totalPercentiles.latencies[2]);
    }

    private HybridPagedLatencyRecorder recorder(int maximumMemoryMiB) {
        return new HybridPagedLatencyRecorder(0, 180_000_000_000L,
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, PERCENTILES,
                new NanoSeconds(), maximumMemoryMiB, 8, 32);
    }

    private MessageLatenciesRecord clientRecord(long clientId) {
        final MessageLatenciesRecord.Builder builder = MessageLatenciesRecord.newBuilder()
                .setClientID(clientId)
                .setSequenceNumber(1)
                .setTotalRecords(LATENCIES_PER_CLIENT)
                .setValidLatencyRecords(LATENCIES_PER_CLIENT)
                .setTotalBytes(LATENCIES_PER_CLIENT * 100L)
                .setTotalLatency((LATENCIES_PER_CLIENT - 1L) * LATENCIES_PER_CLIENT / 2)
                .setMinLatency(0)
                .setMaxLatency(LATENCIES_PER_CLIENT - 1);
        for (int latency = 0; latency < LATENCIES_PER_CLIENT; latency++) {
            builder.addLatencyValues(latency);
            builder.addLatencyCounts(1);
        }
        return builder.build();
    }
}

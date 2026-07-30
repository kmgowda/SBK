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

import io.perl.api.LatencyRecordWindow;
import io.perl.api.ReportLatencies;
import io.perl.logger.Print;
import io.sbk.logger.ReadRequestsLogger;
import io.sbk.logger.SetRW;
import io.sbk.logger.WriteRequestsLogger;
import io.sbp.grpc.MessageLatenciesRecord;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies packed SBP latency aggregation without a boxed protobuf map.
 */
final class SbmTotalWindowLatencyPeriodicRecorderTest {
    @Test
    void aggregatesPackedLatencyPairsAndExactSummaryBounds() {
        final LatencyRecordWindow window = mock(LatencyRecordWindow.class);
        final LatencyRecordWindow totalWindow = mock(LatencyRecordWindow.class);
        final SetRW setRW = mock(SetRW.class);
        final SbmTotalWindowLatencyPeriodicRecorder recorder =
                new SbmTotalWindowLatencyPeriodicRecorder(
                        window,
                        totalWindow,
                        mock(Print.class),
                        mock(Print.class),
                        mock(ReportLatencies.class),
                        setRW,
                        mock(WriteRequestsLogger.class),
                        mock(ReadRequestsLogger.class),
                        4);
        final MessageLatenciesRecord record = MessageLatenciesRecord.newBuilder()
                .setClientID(1)
                .setSequenceNumber(1)
                .setTotalRecords(5)
                .setValidLatencyRecords(5)
                .setTotalBytes(500)
                .setTotalLatency(80)
                .setMinLatency(10)
                .setMaxLatency(20)
                .setReaders(1)
                .setWriters(2)
                .setMaxReaders(3)
                .setMaxWriters(4)
                .addLatencyValues(10)
                .addLatencyCounts(2)
                .addLatencyValues(20)
                .addLatencyCounts(3)
                .build();
        final MessageLatenciesRecord secondRecord = MessageLatenciesRecord.newBuilder()
                .setClientID(2)
                .setSequenceNumber(1)
                .setReaders(5)
                .setWriters(6)
                .setMaxReaders(7)
                .setMaxWriters(8)
                .build();

        recorder.addLatenciesRecord(record);
        recorder.addLatenciesRecord(secondRecord);
        recorder.flush(100);
        recorder.flush(200);

        verify(window).update(5, 80, 500, 0, 0, 0, 5, 10, 20);
        verify(window).reportLatency(10, 2);
        verify(window).reportLatency(20, 3);
        final InOrder setRwOrder = inOrder(setRW);
        setRwOrder.verify(setRW).setReaders(6);
        setRwOrder.verify(setRW).setWriters(8);
        setRwOrder.verify(setRW).setMaxReaders(10);
        setRwOrder.verify(setRW).setMaxWriters(12);
        setRwOrder.verify(setRW).setReaders(0);
        setRwOrder.verify(setRW).setWriters(0);
        setRwOrder.verify(setRW).setMaxReaders(0);
        setRwOrder.verify(setRW).setMaxWriters(0);
    }
}

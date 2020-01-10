/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perf.drivers.Pravega;
import io.perf.core.WriterWorker;
import io.perf.core.PerfStats;
import io.perf.core.TriConsumer;

import io.pravega.client.ClientFactory;
import io.pravega.client.stream.Transaction;
import io.pravega.client.stream.TxnFailedException;

import javax.annotation.concurrent.GuardedBy;

public class PravegaTransactionWriterWorker extends PravegaWriterWorker {
    private final int transactionsPerCommit;

    @GuardedBy("this")
    private int eventCount;

    @GuardedBy("this")
    private Transaction<byte[]> transaction;

    public PravegaTransactionWriterWorker(int sensorId, int events,
                                   int secondsToRun, boolean isRandomKey,
                                   int messageSize, long start,
                                   PerfStats stats, String streamName, int timeout,
                                   int eventsPerSec, boolean writeAndRead,
                                   ClientFactory factory, int transactionsPerCommit) {

        super(sensorId, events, Integer.MAX_VALUE, secondsToRun, isRandomKey,
                messageSize, start, stats, streamName, timeout, eventsPerSec, writeAndRead, factory);

        this.transactionsPerCommit = transactionsPerCommit;
        eventCount = 0;
        transaction = producer.beginTxn();
    }

    @Override
    public long recordWrite(byte[] data, TriConsumer record) {
        long time = 0;
        try {
            synchronized (this) {
                time = System.currentTimeMillis();
                transaction.writeEvent(data);
                record.accept(time, System.currentTimeMillis(), messageSize);
                eventCount++;
                if (eventCount >= transactionsPerCommit) {
                    eventCount = 0;
                    transaction.commit();
                    transaction = producer.beginTxn();
                }
            }
        } catch (TxnFailedException e) {
            throw new RuntimeException("Transaction Write data failed ", e);
        }
        return time;
    }
}
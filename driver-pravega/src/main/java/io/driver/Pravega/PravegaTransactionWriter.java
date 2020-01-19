/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.driver.Pravega;
import io.dsb.api.Parameters;
import io.dsb.api.QuadConsumer;

import io.pravega.client.ClientFactory;
import io.pravega.client.stream.Transaction;
import io.pravega.client.stream.TxnFailedException;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;

public class PravegaTransactionWriter extends PravegaWriter {
    private final int transactionsPerCommit;

    @GuardedBy("this")
    private int eventCount;

    @GuardedBy("this")
    private Transaction<byte[]> transaction;

    public PravegaTransactionWriter(int writerID, QuadConsumer recordTime, Parameters params,
                                          String streamName, int transactionsPerCommit, ClientFactory factory) throws IOException {

        super(writerID, recordTime, params, streamName, factory);

        this.transactionsPerCommit = transactionsPerCommit;
        eventCount = 0;
        transaction = producer.beginTxn();
    }

    @Override
    public long recordWrite(byte[] data, QuadConsumer record) {
        long time = 0;
        try {
            synchronized (this) {
                time = System.currentTimeMillis();
                transaction.writeEvent(data);
                record.accept(time, System.currentTimeMillis(), params.recordSize, 1);
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
/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Ignite;

import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.perl.SendChannel;
import io.sbk.api.Status;
import io.sbk.perl.Time;
import io.sbk.api.Writer;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.IgniteClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class IgniteClientTransactionWriter implements Writer<byte[]> {
    private final Parameters params;
    private final ClientCache<Long, byte[]> cache;
    private final IgniteClient client;
    private long key;
    private long cnt;

    public IgniteClientTransactionWriter(int id, Parameters params, ClientCache<Long, byte[]> cache,
                                         IgniteClient client) throws IOException {
        this.params = params;
        this.cache = cache;
        this.client = client;
        this.key = Ignite.generateStartKey(id);
        this.cnt = 0;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        cache.put(key++, data);
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws  IOException {
    }

    @Override
    public void writeSetTime(DataType<byte[]> dType, byte[] data, int size, Time time, Status status) throws IOException {
        final int recs;
        if (params.getRecordsPerWriter() > 0 && params.getRecordsPerWriter() > cnt) {
            recs = (int) Math.min(params.getRecordsPerWriter() - cnt, params.getRecordsPerSync());
        } else {
            recs = params.getRecordsPerSync();
        }
        final long ctime = time.getCurrentTime();
        status.bytes = size * recs;
        status.records =  recs;
        status.startTime = ctime;
        ClientTransaction tx = client.transactions().txStart();
        long keyCnt = key;
        for (int i = 0; i < recs; i++) {
            cache.put(keyCnt++, data);
        }
        tx.commit();
        key += recs;
        cnt += recs;
    }

    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Time time,
                            Status status, SendChannel sendChannel, int id) throws IOException {
        final int recs;
        if (params.getRecordsPerWriter() > 0 && params.getRecordsPerWriter() > cnt) {
            recs = (int) Math.min(params.getRecordsPerWriter() - cnt, params.getRecordsPerSync());
        } else {
            recs = params.getRecordsPerSync();
        }
        status.bytes = size * recs;
        status.records =  recs;
        status.startTime = time.getCurrentTime();
        ClientTransaction tx = client.transactions().txStart();
        long keyCnt = key;
        for (int i = 0; i < recs; i++) {
            cache.put(keyCnt++, data);
        }
        tx.commit();
        status.endTime = time.getCurrentTime();
        sendChannel.send(id, status.startTime, status.endTime, status.bytes, status.records);
        key += recs;
        cnt += recs;
    }

}
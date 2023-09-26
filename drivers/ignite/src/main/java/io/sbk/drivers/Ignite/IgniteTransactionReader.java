/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.drivers.Ignite;

import io.perl.api.PerlChannel;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;
import io.sbk.api.Status;
import io.sbk.data.DataType;
import io.time.Time;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.transactions.Transaction;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class IgniteTransactionReader implements Reader<byte[]> {
    private final ParameterOptions params;
    private final IgniteCache<Long, byte[]> cache;
    private final org.apache.ignite.Ignite ignite;
    private long key;
    private long cnt;

    public IgniteTransactionReader(int id, ParameterOptions params, IgniteCache<Long, byte[]> cache,
                                   org.apache.ignite.Ignite ignite) throws IOException {
        this.params = params;
        this.cache = cache;
        this.ignite = ignite;
        this.key = Ignite.generateStartKey(id);
        this.cnt = 0;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        byte[] ret;
        ret = cache.get(key);
        if (ret != null) {
            key++;
        }
        return ret;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void recordRead(DataType<byte[]> dType, int size, Time time, Status status, PerlChannel perlChannel)
            throws EOFException, IOException {
        final int recs = params.getRecordsPerSync();
        status.startTime = time.getCurrentTime();
        Transaction tx = ignite.transactions().txStart();
        long startKey = key;
        Status stat = new Status();
        for (int i = 0; i < recs; i++) {
            byte[] result = cache.get(startKey++);
            if (result != null) {
                stat.bytes += result.length;
                stat.records += 1;
            }
        }
        tx.commit();
        if (stat.records == 0) {
            throw new EOFException();
        }
        status.records = stat.records;
        status.bytes = stat.bytes;
        status.endTime = time.getCurrentTime();
        key += recs;
        cnt += recs;
        perlChannel.send(status.startTime, status.endTime, status.records, status.bytes);
    }


    @Override
    public void recordReadTime(DataType<byte[]> dType, int size, Time time, Status status, PerlChannel perlChannel)
            throws EOFException, IOException {
        final int recs = params.getRecordsPerSync();
        Transaction tx = ignite.transactions().txStart();
        long startKey = key;
        Status stat = new Status();
        for (int i = 0; i < recs; i++) {
            byte[] result = cache.get(startKey++);
            if (result != null) {
                stat.bytes += result.length;
                stat.records += 1;
                if (stat.startTime == 0) {
                    stat.startTime = dType.getTime(result);
                }
            } else {
                break;
            }
        }
        tx.commit();
        status.records = stat.records;
        status.bytes = stat.bytes;
        status.startTime = stat.startTime;
        status.endTime = time.getCurrentTime();
        key += status.records;
        cnt += status.records;
        perlChannel.send(status.startTime, status.endTime, status.records, status.bytes);
    }
}
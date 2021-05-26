/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FoundationDB;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.tuple.Tuple;
import io.sbk.api.DataType;
import io.sbk.api.ParameterOptions;
import io.sbk.perl.SendChannel;
import io.sbk.api.Status;
import io.sbk.perl.Time;
import io.sbk.api.Writer;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Multi Key Writer.
 */
public class FoundationDBMultiKeyWriter implements Writer<byte[]> {
    final private ParameterOptions params;
    final private FoundationDBConfig config;
    final private Database db;
    private long key;
    private int cnt;

    public FoundationDBMultiKeyWriter(int id, ParameterOptions params, FoundationDBConfig config, FDB fdb, Database db) throws IOException {
        this.params = params;
        this.config = config;
        this.key = FoundationDB.generateStartKey(id);
        this.cnt = 0;
        if (config.multiClient) {
            this.db = fdb.open(config.cFile);
        } else {
            this.db = db;
        }
        this.db.run(tr -> {
            tr.clear(Tuple.from(key + 1).pack(), Tuple.from(key + 1 + Integer.MAX_VALUE).pack());
            return null;
        });
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        final long startKey = key++;
        return db.run(tr -> {
            tr.set(Tuple.from(startKey).pack(), data);
            return null;
        });
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws  IOException {
        if (config.multiClient && this.db != null) {
            this.db.close();
        }
    }

    @Override
    public void writeSetTime(DataType<byte[]> dType, byte[] data, int size, Time time, Status status) throws IOException {
        final int recs =  params.getRecordsPerSync();
        final long ctime = time.getCurrentTime();
        status.bytes = size * recs;
        status.records =  recs;
        status.startTime = ctime;
        db.run(tr -> {
            long keyCnt = key;
            for (int i = 0; i < recs; i++) {
                tr.set(Tuple.from(keyCnt++).pack(), dType.setTime(data, ctime));
            }
            return null;
        });
        key += recs;
        cnt += recs;
    }

    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Time time,
                            Status status, SendChannel sendChannel, int id) throws IOException {
        final int recs =  params.getRecordsPerSync();
        status.bytes = size * recs;
        status.records =  recs;
        status.startTime = time.getCurrentTime();
        db.run(tr -> {
            long keyCnt = key;
            for (int i = 0; i < recs; i++) {
                tr.set(Tuple.from(keyCnt++).pack(), data);
            }
            return null;
        });
        status.endTime = time.getCurrentTime();
        sendChannel.send(id, status.startTime, status.endTime, status.bytes, status.records);
        key += recs;
        cnt += recs;
    }

}

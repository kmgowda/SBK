/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.FdbRecord;

import com.apple.foundationdb.record.provider.foundationdb.FDBDatabase;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordContext;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStore;
import com.google.protobuf.ByteString;
import io.perl.api.PerlChannel;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.data.DataType;
import io.time.Time;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Class for Writer.
 */
public class FdbRecordMultiWriter implements Writer<ByteString> {
    final private ParameterOptions params;
    final private FDBDatabase db;
    final private Function<FDBRecordContext, FDBRecordStore> recordStoreProvider;
    private long key;
    private int cnt;

    public FdbRecordMultiWriter(int id, ParameterOptions params, FDBDatabase db,
                                Function<FDBRecordContext, FDBRecordStore> recordStoreProvider) throws IOException {
        this.params = params;
        this.key = FdbRecord.generateStartKey(id);
        this.db = db;
        this.recordStoreProvider = recordStoreProvider;
    }

    @Override
    public CompletableFuture writeAsync(ByteString data) throws IOException {
        key++;
        return db.run(context -> {
            FDBRecordStore recordStore = recordStoreProvider.apply(context);
            recordStore.saveRecord(FdbRecordLayerProto.Record.newBuilder()
                    .setRecordID(key)
                    .setData(data)
                    .build());
            return null;
        });
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void writeSetTime(DataType<ByteString> dType, ByteString data, int size, Time time, Status status) throws IOException {
        final int recs = params.getRecordsPerSync();
        final long ctime = time.getCurrentTime();
        status.bytes = size * recs;
        status.records = recs;
        status.startTime = ctime;
        db.run(context -> {
            long keyCnt = key;
            FDBRecordStore recordStore = recordStoreProvider.apply(context);
            for (int i = 0; i < recs; i++) {
                recordStore.saveRecord(FdbRecordLayerProto.Record.newBuilder()
                        .setRecordID(keyCnt++)
                        .setData(data)
                        .build());

            }
            return null;
        });
        key += recs;
        cnt += recs;
    }

    @Override
    public void recordWrite(DataType<ByteString> dType, ByteString data, int size, Time time, Status status, PerlChannel perlChannel) throws IOException {
        final int recs = params.getRecordsPerSync();
        status.bytes = size * recs;
        status.records = recs;
        status.startTime = time.getCurrentTime();
        db.run(context -> {
            long keyCnt = key;
            FDBRecordStore recordStore = recordStoreProvider.apply(context);
            for (int i = 0; i < recs; i++) {
                recordStore.saveRecord(FdbRecordLayerProto.Record.newBuilder()
                        .setRecordID(keyCnt++)
                        .setData(data)
                        .build());
            }
            return null;
        });
        status.endTime = time.getCurrentTime();
        perlChannel.send(status.startTime, status.endTime, status.records, status.bytes);
        key += recs;
        cnt += recs;
    }


}

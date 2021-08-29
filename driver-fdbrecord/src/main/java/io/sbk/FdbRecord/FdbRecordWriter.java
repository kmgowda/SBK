/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FdbRecord;

import com.apple.foundationdb.record.provider.foundationdb.FDBDatabase;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordContext;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStore;
import com.google.protobuf.ByteString;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Class for Writer.
 */
public class FdbRecordWriter implements Writer<ByteString> {
    final private FDBDatabase db;
    final private Function<FDBRecordContext, FDBRecordStore> recordStoreProvider;
    private long key;

    public FdbRecordWriter(int id, ParameterOptions params, FDBDatabase db,
                           Function<FDBRecordContext, FDBRecordStore> recordStoreProvider) throws IOException {
        this.key = FdbRecord.generateStartKey(id);
        this.db = db;
        this.recordStoreProvider = recordStoreProvider;
    }

    @Override
    public CompletableFuture writeAsync(ByteString data) throws IOException {
        final long startKey = key++;
        return db.run(context -> {
            FDBRecordStore recordStore = recordStoreProvider.apply(context);
            recordStore.saveRecord(FdbRecordLayerProto.Record.newBuilder()
                    .setRecordID(startKey)
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
}

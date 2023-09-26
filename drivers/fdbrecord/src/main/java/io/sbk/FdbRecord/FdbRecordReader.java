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
import com.apple.foundationdb.record.provider.foundationdb.FDBStoredRecord;
import com.apple.foundationdb.tuple.Tuple;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;

import java.io.EOFException;
import java.io.IOException;
import java.util.function.Function;

/**
 * Class for Reader.
 */
public class FdbRecordReader implements Reader<ByteString> {
    final private FDBDatabase db;
    final private Function<FDBRecordContext, FDBRecordStore> recordStoreProvider;
    private long key;

    public FdbRecordReader(int id, ParameterOptions params, FDBDatabase db,
                           Function<FDBRecordContext, FDBRecordStore> recordStoreProvider) throws IOException {
        this.key = FdbRecord.generateStartKey(id);
        this.db = db;
        this.recordStoreProvider = recordStoreProvider;
    }

    @Override
    public ByteString read() throws EOFException, IOException {
        ByteString ret = null;
        FDBStoredRecord<Message> storedRecord = db.run(context ->
                // load the record
                recordStoreProvider.apply(context).loadRecord(Tuple.from(key))
        );

        if (storedRecord != null) {
            key++;
            FdbRecordLayerProto.Record record = FdbRecordLayerProto.Record.newBuilder()
                    .mergeFrom(storedRecord.getRecord())
                    .build();
            ret = record.getData();
        }
        return ret;
    }

    @Override
    public void close() throws IOException {
    }
}
/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FdbRecord;

import com.apple.foundationdb.record.provider.foundationdb.FDBDatabase;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordContext;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStore;
import com.apple.foundationdb.record.provider.foundationdb.FDBStoredRecord;
import com.apple.foundationdb.tuple.Tuple;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.sbk.data.DataType;
import io.sbk.parameters.ParameterOptions;
import io.sbk.api.Reader;
import io.sbk.perl.SendChannel;
import io.sbk.api.Status;
import io.sbk.time.Time;

import java.io.EOFException;
import java.io.IOException;
import java.util.function.Function;

/**
 * Class for Reader.
 */
public class FdbRecordMultiReader implements Reader<ByteString> {
    final private ParameterOptions params;
    final private FDBDatabase db;
    final private Function<FDBRecordContext, FDBRecordStore> recordStoreProvider;
    private long key;
    private long cnt;

    public FdbRecordMultiReader(int id, ParameterOptions params, FDBDatabase db,
                                Function<FDBRecordContext, FDBRecordStore> recordStoreProvider ) throws IOException {
        this.params = params;
        this.key = FdbRecord.generateStartKey(id);
        this.cnt = 0;
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
    public void close() throws  IOException {
    }

    @Override
    public void recordRead(DataType<ByteString> dType, int size, Time time, Status status, SendChannel sendChannel, int id)
            throws EOFException, IOException {
        final int recs =  params.getRecordsPerSync();
        status.startTime = time.getCurrentTime();
        final Status ret = db.run(context -> {
            long startKey = key;
            Status stat = new Status();
            FDBRecordStore recordStore = recordStoreProvider.apply(context);
            FDBStoredRecord<Message> storedRecord;
            for (int i = 0; i < recs; i++) {
                storedRecord = recordStore.loadRecord(Tuple.from(startKey++));
                if (storedRecord != null) {
                    FdbRecordLayerProto.Record record = FdbRecordLayerProto.Record.newBuilder()
                            .mergeFrom(storedRecord.getRecord())
                            .build();
                    stat.bytes += record.getData().size();
                    stat.records += 1;
                }
            }
            return stat;
        });
        if (ret.records == 0) {
            throw new EOFException();
        }
        status.records = ret.records;
        status.bytes = ret.bytes;
        status.endTime = time.getCurrentTime();
        key += recs;
        cnt += recs;
        sendChannel.send(id, status.startTime, status.endTime, status.bytes, status.records);
    }


    @Override
    public void recordReadTime(DataType<ByteString> dType, int size, Time time, Status status, SendChannel sendChannel, int id)
            throws EOFException, IOException {
        final int recs =  params.getRecordsPerSync();
        final Status ret = db.run(context -> {
            long startKey = key;
            Status stat = new Status();
            FDBRecordStore recordStore = recordStoreProvider.apply(context);
            FDBStoredRecord<Message> storedRecord;
            ByteString data;
            for (int i = 0; i < recs; i++) {
                storedRecord = recordStore.loadRecord(Tuple.from(startKey++));
                if (storedRecord != null) {
                    FdbRecordLayerProto.Record record = FdbRecordLayerProto.Record.newBuilder()
                            .mergeFrom(storedRecord.getRecord())
                            .build();
                    data = record.getData();
                    stat.bytes += data.size();
                    stat.records += 1;
                    if (stat.startTime == 0) {
                        stat.startTime = dType.getTime(data);
                    }
                } else {
                    break;
                }
            }
            return stat;
        });
        status.startTime = ret.startTime;
        status.records = ret.records;
        status.bytes = ret.bytes;
        status.endTime = time.getCurrentTime();
        key += status.records;
        cnt += status.records;
        sendChannel.send(id, status.startTime, status.endTime, status.bytes, status.records);
    }
}
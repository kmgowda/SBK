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
import com.apple.foundationdb.tuple.Tuple;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.Reader;
import io.sbk.api.RecordTime;
import io.sbk.api.Status;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class FoundationDBMultiKeyReader implements Reader<byte[]> {
    final Parameters params;
    final private Database db;
    private long key;

    public FoundationDBMultiKeyReader(int id, Parameters params, Database db) throws IOException {
        this.params = params;
        this.key = (id * Integer.MAX_VALUE) + 1;
        this.db = db;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        byte[] ret;
        ret = db.read(tr -> {
            byte[] result = tr.get(Tuple.from(key).pack()).join();
            return result;
        });
        if (ret != null) {
            key++;
        }
        return ret;
    }

    @Override
    public void close() throws  IOException {
    }

    @Override
    public void recordRead(DataType<byte[]> dType, Status status, RecordTime recordTime, int id)
            throws EOFException, IOException {
        final int recs;
        if (params.getRecordsPerReader() > key) {
            recs = Math.min(params.getRecordsPerReader(), params.getRecordsPerSync());
        } else {
            recs =  params.getRecordsPerSync();
        }
        status.startTime = System.currentTimeMillis();
        final Status ret = db.read(tr -> {
            long startKey = key;
            Status stat = new Status();

            for (int i = 0; i < recs; i++) {
                byte[] result = tr.get(Tuple.from(startKey++).pack()).join();
                if (result != null) {
                    stat.bytes = result.length;
                    stat.records += 1;
                }
            }
            return stat;
        });
        status.records = ret.records;
        status.bytes = ret.bytes;
        status.endTime = System.currentTimeMillis();
        key += recs;
        recordTime.accept(id, status.startTime, status.endTime, status.bytes, status.records);
    }


    @Override
    public void recordReadTime(DataType<byte[]> dType, Status status, RecordTime recordTime, int id)
            throws EOFException, IOException {
        final int recs;
        if (params.getRecordsPerReader() > key) {
            recs = Math.min(params.getRecordsPerReader(), params.getRecordsPerSync());
        } else {
            recs =  params.getRecordsPerSync();
        }
        status.startTime = System.currentTimeMillis();
        final Status ret = db.read(tr -> {
            long startKey = key;
            Status stat = new Status();

            for (int i = 0; i < recs; i++) {
                byte[] result = tr.get(Tuple.from(startKey++).pack()).join();
                if (result != null) {
                    stat.bytes = result.length;
                    stat.records += 1;
                } else {
                    break;
                }
            }
            return stat;
        });
        status.records = ret.records;
        status.bytes = ret.bytes;
        status.endTime = System.currentTimeMillis();
        key += status.records;
        recordTime.accept(id, status.startTime, status.endTime, status.bytes, status.records);
    }

}
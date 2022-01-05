/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.FoundationDB;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.tuple.Tuple;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;
import io.sbk.api.Status;
import io.sbk.data.DataType;
import io.perl.PerlChannel;
import io.time.Time;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class FoundationDBMultiKeyReader implements Reader<byte[]> {
    final private ParameterOptions params;
    final private FoundationDBConfig config;
    final private Database db;
    private long key;
    private int cnt;

    public FoundationDBMultiKeyReader(int id, ParameterOptions params, FoundationDBConfig config, FDB fdb, Database db) throws IOException {
        this.params = params;
        this.config = config;
        this.key = FoundationDB.generateStartKey(id);
        this.cnt = 0;
        if (config.multiClient) {
            this.db = fdb.open(config.cFile);
        } else {
            this.db = db;
        }
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
    public void close() throws IOException {
        if (config.multiClient && this.db != null) {
            this.db.close();
        }
    }

    @Override
    public void recordRead(DataType<byte[]> dType, int size, Time time, Status status, PerlChannel perlChannel, int id)
            throws EOFException, IOException {
        final int recs = params.getRecordsPerSync();
        status.startTime = time.getCurrentTime();
        final Status ret = db.read(tr -> {
            long startKey = key;
            Status stat = new Status();

            for (int i = 0; i < recs; i++) {
                byte[] result = tr.get(Tuple.from(startKey++).pack()).join();
                if (result != null) {
                    stat.bytes += result.length;
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
        perlChannel.send(id, status.startTime, status.endTime, status.bytes, status.records);
    }


    @Override
    public void recordReadTime(DataType<byte[]> dType, int size, Time time, Status status, PerlChannel perlChannel, int id)
            throws EOFException, IOException {
        final int recs = params.getRecordsPerSync();
        final Status ret = db.read(tr -> {
            long startKey = key;
            Status stat = new Status();

            for (int i = 0; i < recs; i++) {
                byte[] result = tr.get(Tuple.from(startKey++).pack()).join();
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
            return stat;
        });
        status.records = ret.records;
        status.bytes = ret.bytes;
        status.startTime = ret.startTime;
        status.endTime = time.getCurrentTime();
        key += status.records;
        cnt += status.records;
        perlChannel.send(id, status.startTime, status.endTime, status.bytes, status.records);
    }

}
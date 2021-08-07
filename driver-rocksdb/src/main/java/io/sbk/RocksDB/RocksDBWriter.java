/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.RocksDB;

import io.sbk.parameters.ParameterOptions;
import io.sbk.api.Writer;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class RocksDBWriter implements Writer<byte[]> {
    final private RocksDB db;
    private long key;

    public RocksDBWriter(int id, ParameterOptions params, RocksDB db) throws IOException {
        this.key = io.sbk.RocksDB.RocksDB.generateStartKey(id);
        this.db = db;
        try {
            this.db.deleteRange(String.valueOf(this.key).getBytes(),
                    String.valueOf(this.key + (long) Integer.MAX_VALUE).getBytes());
        } catch (RocksDBException ex) {
            //
        }
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        try {
            db.put(String.valueOf(this.key++).getBytes(), data);
        } catch (RocksDBException ex) {
            throw  new IOException(ex);
        }
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws  IOException {
    }
}

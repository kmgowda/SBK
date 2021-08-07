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

import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class RocksDBReader implements Reader<byte[]> {
    final ParameterOptions params;
    final private RocksDB db;
    private long key;

    public RocksDBReader(int id, ParameterOptions params, RocksDB db) throws IOException {
        this.key = io.sbk.RocksDB.RocksDB.generateStartKey(id);
        this.params = params;
        this.db = db;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        byte[] ret;
        try {
            ret = db.get(String.valueOf(this.key).getBytes());
        } catch (RocksDBException ex) {
            throw new IOException(ex);
        }
        if (ret != null) {
            key++;
        }
        return ret;
    }

    @Override
    public void close() throws  IOException {
    }
}
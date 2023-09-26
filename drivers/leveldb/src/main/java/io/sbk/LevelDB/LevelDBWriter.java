/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.LevelDB;

import io.sbk.params.ParameterOptions;
import io.sbk.api.Writer;
import org.iq80.leveldb.DB;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class LevelDBWriter implements Writer<byte[]> {
    final private DB db;
    private long key;

    public LevelDBWriter(int id, ParameterOptions params, DB db) throws IOException {
        this.key = io.sbk.LevelDB.LevelDB.generateStartKey(id);
        this.db = db;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        db.put(String.valueOf(this.key++).getBytes(), data);
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}

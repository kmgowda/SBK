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
import io.sbk.parameters.ParameterOptions;
import io.sbk.api.Writer;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class FoundationDBWriter implements Writer<byte[]> {
    final private FoundationDBConfig config;
    final private Database db;
    private long key;

    public FoundationDBWriter(int id, ParameterOptions params, FoundationDBConfig config, FDB fdb, Database db) throws IOException {
        this.key = FoundationDB.generateStartKey(id);
        this.config = config;
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
}

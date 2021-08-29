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

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class FoundationDBReader implements Reader<byte[]> {
    final private FoundationDBConfig config;
    final private Database db;
    private long key;

    public FoundationDBReader(int id, ParameterOptions params, FoundationDBConfig config, FDB fdb, Database db) throws IOException {
        this.key = FoundationDB.generateStartKey(id);
        this.config = config;
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
}
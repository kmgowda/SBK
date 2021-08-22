/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.LevelDB;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;
import org.iq80.leveldb.DB;
import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class LevelDBReader implements Reader<byte[]> {
    final ParameterOptions params;
    final private DB db;
    private long key;

    public LevelDBReader(int id, ParameterOptions params, DB db) throws IOException {
        this.key = io.sbk.LevelDB.LevelDB.generateStartKey(id);
        this.params = params;
        this.db = db;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        byte[] ret;
        ret = db.get(String.valueOf(this.key).getBytes());
        if (ret != null) {
            key++;
        }
        return ret;
    }

    @Override
    public void close() throws  IOException {
    }
}
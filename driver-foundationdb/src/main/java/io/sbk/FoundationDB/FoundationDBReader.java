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
import io.sbk.api.Parameters;
import io.sbk.api.Reader;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for File Reader.
 */
public class FoundationDBReader implements Reader<byte[]> {
    final private Parameters params;
    final private Database db;
    private long key;

    public FoundationDBReader(int id, Parameters params, Database db) throws IOException {
        this.key = (id * Integer.MAX_VALUE) + 1;
        this.params = params;
        this.db = db;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        byte[] ret;
        ret = db.run(tr -> {
            byte[] result = tr.get(Tuple.from(key).pack()).join();
            return result;
        });
        if (ret != null || !params.isWriteAndRead()) {
            key++;
        }
        return ret;
    }

    @Override
    public void close() throws  IOException {
    }
}
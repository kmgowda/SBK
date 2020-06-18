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
import io.sbk.api.Parameters;
import io.sbk.api.Reader;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for File Reader.
 */
public class FoundationDBReader implements Reader<byte[]> {
    final private Database db;
    private long key;

    public FoundationDBReader(int id, Parameters params, Database db) throws IOException {
        this.key = (id * Integer.MAX_VALUE) - 1;
        this.db = db;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        key++;
        return db.run(tx -> tx.get(FoundationDB.longToBytes(key)).join());
    }

    @Override
    public void close() throws  IOException {
    }
}
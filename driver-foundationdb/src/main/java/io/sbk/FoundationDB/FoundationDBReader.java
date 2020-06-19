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

import com.apple.foundationdb.Transaction;
import io.sbk.api.Parameters;
import io.sbk.api.Reader;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for File Reader.
 */
public class FoundationDBReader implements Reader<byte[]> {
    final private Parameters params;
    final private Transaction tx;
    private long key;

    public FoundationDBReader(int id, Parameters params, Transaction tx) throws IOException {
        this.key = (id * Integer.MAX_VALUE) + 1;
        this.params = params;
        this.tx = tx;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        CompletableFuture<byte[]> cf = tx.get(FoundationDB.longToBytes(key));
        if (cf == null) {
            if (!params.isWriteAndRead()) {
               throw new EOFException();
            }
            return null;
        }
        key++;
        return cf.join();
    }

    @Override
    public void close() throws  IOException {
    }
}
/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.HaloDB;

import com.oath.halodb.HaloDB;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


/**
 * Class for HaloDB Writer.
 */
public class HaloDBWriter implements Writer<byte[]> {
    private final HaloDB db;
    private long key;

    public HaloDBWriter(int writerID, ParameterOptions params, HaloDBConfig config, HaloDB db) throws IOException {
        this.key = io.sbk.driver.HaloDB.HaloDB.generateStartKey(writerID);
        this.db = db;
    }

    @Override
    public CompletableFuture<?> writeAsync(byte[] data) throws IOException {
        try {
            db.put(String.valueOf(this.key++).getBytes(), data);
        } catch (Exception ex) {
            throw new IOException("Write operation failed", ex);
        }
        return null;
    }

    @Override
    public void sync() throws IOException {
        // HaloDB automatically handles flushing based on configuration
        // No explicit sync needed as HaloDB manages durability internally
    }

    @Override
    public void close() throws IOException {
        // Database is closed by the main HaloDB class
        // No need to close individual writers
    }
}
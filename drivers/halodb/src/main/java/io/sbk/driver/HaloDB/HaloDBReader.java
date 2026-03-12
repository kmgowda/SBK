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
import io.sbk.api.Reader;

import java.io.IOException;
import java.io.EOFException;

/**
 * Class for HaloDB Reader.
 */
public class HaloDBReader implements Reader<byte[]> {
    private final HaloDB db;
    private long key;

    public HaloDBReader(int readerId, ParameterOptions params, HaloDBConfig config, HaloDB db) throws IOException {
        this.key = io.sbk.driver.HaloDB.HaloDB.generateStartKey(readerId);
        this.db = db;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        byte[] ret;
        try {
            ret = db.get(String.valueOf(this.key).getBytes());
        } catch (Exception ex) {
            throw new IOException("Read operation failed", ex);
        }
        if (ret != null) {
            key++;
        }
        return ret;
    }

    @Override
    public void close() throws IOException {
        // Database is closed by the main HaloDB class
        // No need to close individual readers
    }
}
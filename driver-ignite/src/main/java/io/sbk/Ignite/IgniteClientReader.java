/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Ignite;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;
import org.apache.ignite.client.ClientCache;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class IgniteClientReader implements Reader<byte[]> {
    private long key;
    private ClientCache<Long, byte[]> cache;

    public IgniteClientReader(int id, ParameterOptions params, ClientCache<Long, byte[]> cache) throws IOException {
        this.key = Ignite.generateStartKey(id);
        this.cache = cache;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        byte[] ret;
        ret = cache.get(key);
        if (ret != null) {
            key++;
        }
        return ret;
    }

    @Override
    public void close() throws IOException {
    }
}
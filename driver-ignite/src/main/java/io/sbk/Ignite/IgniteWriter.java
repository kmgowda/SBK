/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Ignite;

import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import org.apache.ignite.IgniteCache;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class IgniteWriter implements Writer<byte[]> {
    private long key;
    private IgniteCache<Long, byte[]> cache;

    public IgniteWriter(int id, Parameters params, IgniteCache<Long, byte[]> cache) throws IOException {
        this.key = Ignite.generateStartKey(id);
        this.cache = cache;
        /*
        for (long i = this.key; i < this.key + Integer.MAX_VALUE + 1; i++) {
            if (cache.containsKey(i)) {
                cache.clear(i);
            }
        }
        */
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        cache.put(key++, data);
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws  IOException {
    }
}
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
import io.sbk.api.Writer;
import org.apache.ignite.client.ClientCache;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class IgniteClientWriter implements Writer<byte[]> {
    private long key;
    private ClientCache<Long, byte[]> cache;

    public IgniteClientWriter(int id, ParameterOptions params, ClientCache<Long, byte[]> cache) throws IOException {
        this.key = Ignite.generateStartKey(id);
        this.cache = cache;
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
    public void close() throws IOException {
    }
}
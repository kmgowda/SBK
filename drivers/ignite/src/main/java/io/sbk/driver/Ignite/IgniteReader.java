/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.Ignite;

import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;
import org.apache.ignite.IgniteCache;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class IgniteReader implements Reader<byte[]> {
    private long key;
    private IgniteCache<Long, byte[]> cache;

    public IgniteReader(int id, ParameterOptions params, org.apache.ignite.Ignite ignite, IgniteConfig config) throws IOException {
        this.key = Ignite.generateStartKey(id);
        this.cache = ignite.getOrCreateCache(config.cacheName);
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
        cache.close();
    }
}
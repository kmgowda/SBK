/**
 * Copyright (c) KMG. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Memcached;

import io.sbk.api.Writer;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;


/**
 * Class for Memcached Writer.
 */
public class MemcachedWriter implements Writer<String> {

    private XMemcachedClient xMemcachedClient;
    private long key;

    public MemcachedWriter(int writerID, XMemcachedClient client) {
        this.key = Memcached.generateStartKey(writerID);
        this.xMemcachedClient = client;
    }

    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        try {
            xMemcachedClient.set(Long.toString(key++), 3600, data);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (MemcachedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
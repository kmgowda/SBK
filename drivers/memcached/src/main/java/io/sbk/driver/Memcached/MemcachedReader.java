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

import io.sbk.api.Reader;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Class for Memcached Reader.
 */
public class MemcachedReader implements Reader<String> {

    private XMemcachedClient xMemcachedClient;
    private long key;

    public MemcachedReader(int readerId, XMemcachedClient client) {
        this.key = Memcached.generateStartKey(readerId);
        this.xMemcachedClient = client;
    }

    @Override
    public String read() throws IOException {
        try {
            return xMemcachedClient.get(Long.toString(key++));
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (MemcachedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
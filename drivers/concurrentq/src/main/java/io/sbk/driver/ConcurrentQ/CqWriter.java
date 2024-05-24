/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.ConcurrentQ;

import io.perl.api.Queue;
import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


/**
 * Class for Concurrent Queue Writer.
 */
public class CqWriter implements Writer<byte[]> {
    private Queue<byte[]> queue;

    public CqWriter(Queue queue) throws IOException {
        this.queue = queue;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        queue.add(data);
        return null;
    }

    @Override
    public void sync() throws IOException {

    }

    @Override
    public void close() throws IOException {
    }
}
/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.ConcurrentQ;

import io.sbk.api.Benchmark;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class for Concurrent Queue Benchmarking.
 */
public class ConcurrentQ implements Benchmark<byte[]> {
    private ConcurrentLinkedQueue<byte[]> queue;

    @Override
    public void addArgs(final Parameters params) {
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        if (params.getReadersCount() < 1 || params.getWritersCount() < 1) {
            throw new IllegalArgumentException("Specify both Writer or readers for Java Concurrent Linked Queue");
        }

    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        this.queue.clear();
    }

    @Override
    public Writer createWriter(final int id, final Parameters params) {
        try {
            return new CqWriter(queue);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader createReader(final int id, final Parameters params) {
        try {
            return new CqReader(queue);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
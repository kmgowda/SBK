/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.ConcurrentQ;

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class for Concurrent Queue Benchmarking.
 */
public class ConcurrentQ implements Storage<byte[]> {
    private ConcurrentLinkedQueue<byte[]> queue;

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        if (params.getReadersCount() < 1 || params.getWritersCount() < 1) {
            throw new IllegalArgumentException("Specify both Writer or readers for Java Concurrent Linked Queue");
        }
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        this.queue.clear();
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        try {
            return new CqWriter(queue);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            return new CqReader(queue);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
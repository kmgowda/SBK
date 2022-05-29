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


import io.sbk.api.Reader;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Class for File Reader.
 */
public class CqReader implements Reader<byte[]> {
    final private static int NS_PER_MICRO = 1000;
    final private static int MICROS_PER_MS = 1000;
    final private static int NS_PER_MS = NS_PER_MICRO * MICROS_PER_MS;
    final private static int PARK_NS = NS_PER_MS;
    private final ConcurrentLinkedQueue<byte[]> queue;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CqReader(ConcurrentLinkedQueue queue) throws IOException {
        this.queue = queue;
    }

    @Override
    public byte[] read() throws IOException {
        boolean found = true;
        byte[] ret;
        while (true) {
            ret = queue.poll();
            if (ret != null || !found) {
                return ret;
            }
            found = false;
            LockSupport.parkNanos(PARK_NS);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
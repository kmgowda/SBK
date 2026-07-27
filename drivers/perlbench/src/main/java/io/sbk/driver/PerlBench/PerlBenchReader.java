/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.PerlBench;

import io.sbk.api.Reader;

import java.io.IOException;

/**
 * Synthetic reader returning a preallocated worker-owned payload.
 */
public final class PerlBenchReader implements Reader<byte[]> {
    private final byte[] payload;

    /**
     * Create a reader payload once, outside the per-record path.
     *
     * @param recordSize configured record size in bytes
     */
    public PerlBenchReader(int recordSize) {
        this.payload = new byte[recordSize];
    }

    /**
     * Return the preallocated payload.
     *
     * @return the same worker-owned payload on every invocation
     * @throws IOException retained by the reader contract
     */
    @Override
    public byte[] read() throws IOException {
        return payload;
    }

    /**
     * Close the reader.
     *
     * @throws IOException retained by the reader contract
     */
    @Override
    public void close() throws IOException {
    }
}

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

import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Synchronous no-operation writer used to expose measurement-pipeline cost.
 */
public final class PerlBenchWriter implements Writer<byte[]> {

    /**
     * Complete one synthetic operation synchronously.
     *
     * @param data worker-owned payload; intentionally unused
     * @return {@code null}, indicating synchronous completion
     * @throws IOException retained by the writer contract
     */
    @Override
    public CompletableFuture<?> writeAsync(byte[] data) throws IOException {
        return null;
    }

    /**
     * Close the stateless writer.
     *
     * @throws IOException retained by the writer contract
     */
    @Override
    public void close() throws IOException {
    }
}

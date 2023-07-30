/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Null;

import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class for Writer.
 */
public class NullWriter implements Writer<byte[]> {
    private final long n;
    private final int timeoutMS;


    public NullWriter(int timeoutMS, long n) {
        this.n = n;
        this.timeoutMS = timeoutMS;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        if (n > 0) {
            for (long i = 0; i < n; i++) {
                //no op
            }
            return null;
        } else {
            return new CompletableFuture<>().orTimeout(timeoutMS, TimeUnit.MILLISECONDS);
        }

    }


    @Override
    public void sync() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
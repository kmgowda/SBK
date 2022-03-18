/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Couchbase;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


/**
 * Class for Couchbase Writer.
 */
public class CouchbaseWriter implements Writer<byte[]> {

    public CouchbaseWriter(int writerID, ParameterOptions params, CouchbaseConfig config) {
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        throw new IOException("The Couchbase Writer Driver not defined");
    }

    @Override
    public void sync() throws IOException {
        throw new IOException("The Couchbase Writer Driver not defined");
    }

    @Override
    public void close() throws IOException {
        throw new IOException("The Couchbase Writer Driver not defined");
    }
}
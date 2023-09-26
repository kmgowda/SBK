/**
 * Copyright (c) KMG. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.drivers.Couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import io.sbk.api.Writer;
import io.sbk.params.ParameterOptions;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


/**
 * Class for Couchbase Writer.
 */
public class CouchbaseWriter implements Writer<String> {

    private Collection collection;
    final private ParameterOptions params;

    private long key;

    public CouchbaseWriter(int writerID, ParameterOptions params, CouchbaseConfig config, Bucket bucket) {
        this.key = Couchbase.generateStartKey(writerID);
        this.params = params;
        collection = bucket.defaultCollection();
    }

    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        collection.upsert(Long.toString(key++), data);
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
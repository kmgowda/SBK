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

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import io.sbk.api.Reader;
import io.sbk.params.ParameterOptions;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Couchbase Reader.
 */
public class CouchbaseReader implements Reader<String> {

    private Collection collection;
    final private ParameterOptions params;

    private long key;

    public CouchbaseReader(int readerId, ParameterOptions params, CouchbaseConfig config, Bucket bucket) {
        this.key = Couchbase.generateStartKey(readerId);
        this.params = params;
        collection = bucket.defaultCollection();
    }

    @Override
    public String read() throws IOException {
        String k = Long.toString(key);
        try {
            GetResult result = collection.get(k);
            if (result != null) {
                key++;
                return result.contentAs(String.class);
            }
        } catch (DocumentNotFoundException ex) {
            throw new EOFException("Key : " + k + "not found");
        }
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}
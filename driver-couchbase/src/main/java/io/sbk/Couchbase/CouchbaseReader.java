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

import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;

import java.io.IOException;

/**
 * Class for Couchbase Reader.
 */
public class CouchbaseReader implements Reader<byte[]> {

    public CouchbaseReader(int readerId, ParameterOptions params, CouchbaseConfig config) {
    }

    @Override
    public byte[] read() throws IOException {
        throw new IOException("The Couchbase Reader Driver not defined");
    }

    @Override
    public void close() throws IOException {
        throw new IOException("The Couchbase Reader Driver not defined");
    }
}
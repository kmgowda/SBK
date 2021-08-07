/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.CouchDB;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Writer;
import org.ektorp.CouchDbConnector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class CouchDBWriter implements Writer<String> {
    final private CouchDbConnector db;
    final private ParameterOptions params;
    private long key;

    public CouchDBWriter(int id, ParameterOptions params, CouchDBConfig config, CouchDbConnector db) throws IOException {
        this.key = CouchDB.generateStartKey(id);
        this.params = params;
        this.db = db;
    }

    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        // fill the map
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("data", data);
        db.create(Long.toString(key++), map);
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws  IOException {
    }


}
/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.drivers.MongoDB;

import com.mongodb.client.MongoCollection;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Writer;
import org.bson.Document;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class MongoDBWriter implements Writer<byte[]> {
    final private MongoCollection<Document> databaseCollection;
    final private ParameterOptions params;
    private long key;

    public MongoDBWriter(int id, ParameterOptions params, MongoDBConfig config, MongoCollection<Document> databaseCollection) throws IOException {
        this.key = MongoDB.generateStartKey(id);
        this.params = params;
        this.databaseCollection = databaseCollection;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        Document document = new Document();
        document.put("index", Long.toString(key++));
        document.put("data", data);
        databaseCollection.insertOne(document);
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }


}



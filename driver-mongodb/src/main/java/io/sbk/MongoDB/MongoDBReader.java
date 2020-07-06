/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.MongoDB;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import io.sbk.api.Parameters;
import io.sbk.api.Reader;
import org.bson.Document;
import org.bson.types.Binary;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Reader.
 */
public class MongoDBReader implements Reader<byte[]> {
    final private  MongoCollection<Document> databaseCollection;
    final private Parameters params;
    private long key;
    private int cnt;

    public MongoDBReader(int id, Parameters params, MongoDBConfig config,  MongoCollection<Document> databaseCollection) throws IOException {
        this.key = MongoDB.generateStartKey(id);
        this.cnt = 0;
        this.params = params;
        this.databaseCollection = databaseCollection;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        MongoCursor<Document> cursor = databaseCollection.find(Filters.eq("index", Long.toString(key))).iterator();
        if (cursor.hasNext()) {
            key++;
            Binary bin = cursor.next().get("data", org.bson.types.Binary.class);
            return bin.getData();
        }
        return null;
    }

    @Override
    public void close() throws  IOException {
    }
}
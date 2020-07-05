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
import io.sbk.api.Parameters;
import io.sbk.api.Reader;
import org.bson.Document;

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
        this.key = (id * Integer.MAX_VALUE) + 1;
        this.cnt = 0;
        this.params = params;
        this.databaseCollection = databaseCollection;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        Document query = new Document();
        query.put("index",  Long.toString(key));
        MongoCursor<Document> cursor = databaseCollection.find(query).iterator();

        while (cursor.hasNext()) {
            key++;
            System.out.println(cursor.next());
            return (byte[]) cursor.next().get("data");
        }
        return null;
    }

    @Override
    public void close() throws  IOException {
    }
}
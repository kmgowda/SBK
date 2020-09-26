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
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import org.bson.Document;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Multi key Writer.
 */
public class MongoDBMultiWriter implements Writer<byte[]> {
    final private MongoCollection<Document> databaseCollection;
    final private Parameters params;
    private long key;
    private int cnt;

    public MongoDBMultiWriter(int id, Parameters params, MongoDBConfig config, MongoCollection<Document> databaseCollection) throws IOException {
        this.key = MongoDB.generateStartKey(id);
        this.cnt = 0;
        this.params = params;
        this.databaseCollection = databaseCollection;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        Document document = new Document();
        document.put("index",  Long.toString(key++));
        document.put("data", data);
        databaseCollection.insertOne(document);
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws  IOException {
    }

    @Override
    public void writeAsyncTime(DataType<byte[]> dType, byte[] data, int size, Status status) throws IOException {
        final int recs;
        if (params.getRecordsPerWriter() > 0 && params.getRecordsPerWriter() > cnt) {
            recs = Math.min(params.getRecordsPerWriter() - cnt, params.getRecordsPerSync());
        } else {
            recs = params.getRecordsPerSync();
        }
        final long time = System.currentTimeMillis();
        status.bytes = size * recs;
        status.records =  recs;
        status.startTime = time;
        final LinkedList<Document> lt = new LinkedList<>();
        for (int i = 0; i < recs; i++) {
            Document document = new Document();
            document.put("index", Long.toString(key++));
            document.put("data", data);
            lt.add(document);
        }
        databaseCollection.insertMany(lt);
        cnt += recs;
    }

    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Status status, RecordTime recordTime, int id) throws IOException {
        final int recs;
        if (params.getRecordsPerWriter() > 0 && params.getRecordsPerWriter() > cnt) {
            recs = Math.min(params.getRecordsPerWriter() - cnt, params.getRecordsPerSync());
        } else {
            recs = params.getRecordsPerSync();
        }
        final LinkedList<Document> lt = new LinkedList<>();
        status.bytes = size * recs;
        status.records =  recs;
        status.startTime = System.currentTimeMillis();
        for (int i = 0; i < recs; i++) {
            Document document = new Document();
            document.put("index", Long.toString(key++));
            document.put("data", data);
            lt.add(document);
        }
        databaseCollection.insertMany(lt);
        status.endTime = System.currentTimeMillis();
        recordTime.send(id, status.startTime, status.endTime, status.bytes, status.records);
        cnt += recs;
    }
}
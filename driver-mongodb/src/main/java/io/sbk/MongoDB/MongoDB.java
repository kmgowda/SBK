/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.MongoDB;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import org.bson.Document;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for MongoDB Benchmarking.
 */
public class MongoDB implements Storage<byte[]> {
    private final static String CONFIGFILE = "mongodb.properties";
    private MongoDBConfig config;
    private MongoClient client;
    private MongoDatabase database;
    private MongoCollection<Document> mCollection;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(MongoDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    MongoDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("url", true, "Database url, default url: " + config.url);
        params.addOption("db", true, "Database Name, default : " + config.dbName);
        params.addOption("collection", true, "collection Name, default: " + config.collection);
        /*
        params.addOption("user", true, "User Name, default : "+config.user);
        params.addOption("password", true, "password, default : "+config.password);
        */
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.url = params.getOptionValue("url", config.url);
        config.dbName = params.getOptionValue("db", config.dbName);
        config.collection = params.getOptionValue("collection", config.collection);
        /*
        config.user =  params.getOptionValue("user", config.user);
        config.password =  params.getOptionValue("password", config.password);
         */
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        client = MongoClients.create(config.url);
        database = client.getDatabase(config.dbName);
        mCollection = database.getCollection(config.collection);
        if (params.getWritersCount() > 0) {
            mCollection.drop();
            database.createCollection(config.collection);
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        try {
            if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                return new MongoDBMultiWriter(id, params, config, mCollection);
            } else {
                return new MongoDBWriter(id, params, config, mCollection);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                return new MongoDBMultiReader(id, params, config, mCollection);
            } else {
                return new MongoDBReader(id, params, config, mCollection);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

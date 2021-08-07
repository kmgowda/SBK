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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.data.DataType;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;

import io.sbk.api.Storage;
import io.sbk.data.impl.SbkString;
import io.sbk.system.Printer;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for CouchDB Benchmarking.
 */
public class CouchDB implements Storage<String> {
    private final static String CONFIGFILE = "couchdb.properties";
    private CouchDBConfig config;
    private CouchDbConnector db;

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(CouchDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    CouchDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("url", true, "Database url, default url: "+config.url);
        params.addOption("db", true, "Database Name, default : "+config.dbName);
        params.addOption("user", true, "User Name, default : "+config.user);
        params.addOption("password", true, "password, default : "+config.password);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.url =  params.getOptionValue("url", config.url);
        config.dbName =  params.getOptionValue("db", config.dbName);
        config.user =  params.getOptionValue("user", config.user);
        config.password =  params.getOptionValue("password", config.password);
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        HttpClient httpClient = new StdHttpClient.Builder()
                .url(config.url).username(config.user)
                .password(config.password)
                .build();

        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);

        try {
            if (params.getWritersCount() > 0) {
                dbInstance.deleteDatabase(config.dbName);
            }
        } catch (DocumentNotFoundException ex) {
            Printer.log.info("The data base : " + config.dbName + " not found");
        }
        // if the second parameter is true, the database will be created if it
        // doesn't exists
        db = dbInstance.createConnector(config.dbName, true);
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (db != null) {
            db.cleanupViews();
        }
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        try {
            return new CouchDBWriter(id, params, config, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        try {
            return new CouchDBReader(id, params, config, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType<String> getDataType() {
        return new SbkString();
    }

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }
}

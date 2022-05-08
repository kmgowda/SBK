/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.FoundationDB;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.params.InputOptions;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for FoundationDB Benchmarking.
 */
public class FoundationDB implements Storage<byte[]> {
    private final static String CONFIGFILE = "foundationdb.properties";
    private FoundationDBConfig config;
    private FDB fdb;
    private Database db;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(FoundationDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    FoundationDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("cfile", true, "cluster file, default : " + config.cFile);
        params.addOption("multiclient", true, "client connection per Writer/Reader, default : " + config.multiClient);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.cFile = params.getOptionValue("cfile", config.cFile);
        config.multiClient = Boolean.parseBoolean(params.getOptionValue("multiclient", Boolean.toString(config.multiClient)));
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        fdb = FDB.selectAPIVersion(config.version);
        if (config.multiClient) {
            db = null;
        } else {
            db = fdb.open(config.cFile);
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (db != null) {
            db.close();
        }
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        try {
            if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                return new FoundationDBMultiKeyWriter(id, params, config, fdb, db);
            } else {
                return new FoundationDBWriter(id, params, config, fdb, db);
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
                return new FoundationDBMultiKeyReader(id, params, config, fdb, db);
            } else {
                return new FoundationDBReader(id, params, config, fdb, db);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

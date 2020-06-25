/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.FoundationDB;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

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

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(FoundationDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    FoundationDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("cfile", true, "cluster file, default : "+ config.cFile);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        config.cFile =  params.getOptionValue("cfile", config.cFile);
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        fdb = FDB.selectAPIVersion(config.version);
        db = fdb.open(config.cFile);
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        db.close();
    }

    @Override
    public Writer<byte[]> createWriter(final int id, final Parameters params) {
        try {
            if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 0) {
                return new FoundationDBMultiKeyWriter(id, params, db);
            } else {
                return new FoundationDBWriter(id, params, db);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader<byte[]> createReader(final int id, final Parameters params) {
        try {
            if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 0) {
                return new FoundationDBMultiKeyReader(id, params, db);
            } else {
                return new FoundationDBReader(id, params, db);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

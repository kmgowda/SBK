/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.drivers.RocksDB;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.params.InputOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for RocksDB Benchmarking.
 */
public class RocksDB implements Storage<byte[]> {
    private final static String CONFIGFILE = "rocksdb.properties";
    private RocksDBConfig config;
    private org.rocksdb.RocksDB db;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(RocksDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    RocksDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("rfile", true, "RocksDB file, default : " + config.rFile);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.rFile = params.getOptionValue("rfile", config.rFile);
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            // a static method that loads the RocksDB C++ library.
            org.rocksdb.RocksDB.loadLibrary();
            if (params.getWritersCount() > 0) {
                org.rocksdb.RocksDB.destroyDB(config.rFile, new Options());
            }
            db = org.rocksdb.RocksDB.open(new Options().setCreateIfMissing(true), config.rFile);
        } catch (RocksDBException ex) {
            throw new IOException(ex);
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
            return new RocksDBWriter(id, params, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            return new RocksDBReader(id, params, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}



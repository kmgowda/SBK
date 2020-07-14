/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.RocksDB;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;
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

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(RocksDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    RocksDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("rfile", true, "RocksDB file, default : "+ config.rFile);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        config.rFile =  params.getOptionValue("rfile", config.rFile);
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        if (params.getWritersCount() > 0) {
            java.io.File file = new java.io.File(config.rFile);
            file.delete();
        }
        final Options options = new Options().setCreateIfMissing(true);
        try {
            // a static method that loads the RocksDB C++ library.
            org.rocksdb.RocksDB.loadLibrary();
            db = org.rocksdb.RocksDB.open(options, config.rFile);
        } catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        if (db != null) {
            db.close();
        }
    }

    @Override
    public Writer<byte[]> createWriter(final int id, final Parameters params) {
        try {
            return new RocksDBWriter(id, params, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader<byte[]> createReader(final int id, final Parameters params) {
        try {
            return new RocksDBReader(id, params, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }
}



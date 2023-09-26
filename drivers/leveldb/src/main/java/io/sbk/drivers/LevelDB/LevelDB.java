/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.drivers.LevelDB;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.params.InputOptions;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

/**
 * Class for RocksDB Benchmarking.
 */
public class LevelDB implements Storage<byte[]> {
    private final static String CONFIGFILE = "leveldb.properties";
    private LevelDBConfig config;
    private DB db;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(LevelDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    LevelDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("lfile", true, "LevelDB file, default : " + config.lFile);
        params.addOption("cache", true, "LevelDB cache size in bytes ");
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.lFile = params.getOptionValue("lfile", config.lFile);
        config.cache = Long.parseLong(params.getOptionValue("cache", String.valueOf(params.getRecordSize())));
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        Options options = new Options();
        options.createIfMissing(true);
        options.cacheSize(config.cache);
        options.compressionType(CompressionType.NONE);
        db = factory.open(new File(config.lFile), options);
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
            return new LevelDBWriter(id, params, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            return new LevelDBReader(id, params, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}



/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.HaloDB;


import com.oath.halodb.HaloDBOptions;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.ByteArray;
import io.sbk.params.InputOptions;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for HaloDB storage driver.
 *
 * Incase if your data type in other than byte[] (Byte Array)
 * then change the datatype and getDataType.
 */
public class HaloDB implements Storage<byte[]> {
    private final static String CONFIGFILE = "HaloDB.properties";
    private HaloDBConfig config;
    private com.oath.halodb.HaloDB db;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(HaloDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    HaloDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("storageDirectory", true, "HaloDB storage directory, default: " + config.storageDirectory);
        params.addOption("maxFileSize", true, "HaloDB max file size in bytes, default: " + config.maxFileSize);
        params.addOption("buildIndexThreads", true, "HaloDB build index threads, default: " + config.buildIndexThreads);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.storageDirectory = params.getOptionValue("storageDirectory", config.storageDirectory);
        config.maxFileSize = Long.parseLong(params.getOptionValue("maxFileSize", String.valueOf(config.maxFileSize)));
        config.buildIndexThreads = Integer.parseInt(params.getOptionValue("buildIndexThreads", String.valueOf(config.buildIndexThreads)));
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            HaloDBOptions options = new HaloDBOptions();
            options.setMaxFileSize((int) config.maxFileSize);
            options.setMaxTombstoneFileSize((int) config.maxTombstoneFileSize);
            options.setBuildIndexThreads(config.buildIndexThreads);
            options.setFlushDataSizeBytes((int) config.flushDataSizeBytes);
            options.setCompactionThresholdPerFile(config.compactionThresholdPerFile);
            options.setCompactionJobRate((int) config.compactionJobRate);
            options.setNumberOfRecords((int) config.numberOfRecords);
            options.setCleanUpTombstonesDuringOpen(config.cleanUpTombstonesDuringOpen);
            options.setCleanUpInMemoryIndexOnClose(config.cleanUpInMemoryIndexOnClose);
            options.setUseMemoryPool(config.useMemoryPool);
            options.setMemoryPoolChunkSize((int) config.memoryPoolChunkSize);
            options.setFixedKeySize(config.fixedKeySize);
            
            db = com.oath.halodb.HaloDB.open(config.storageDirectory, options);
        } catch (Exception ex) {
            throw new IOException("Failed to open HaloDB", ex);
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (db != null) {
            try {
                db.close();
            } catch (Exception ex) {
                throw new IOException("Failed to close HaloDB", ex);
            }
        }
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        try {
            return new HaloDBWriter(id, params, config, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            return new HaloDBReader(id, params, config, db);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType<byte[]> getDataType() {
        return new ByteArray();
    }
}

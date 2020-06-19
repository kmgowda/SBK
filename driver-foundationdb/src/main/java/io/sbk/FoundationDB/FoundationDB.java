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
import com.apple.foundationdb.Transaction;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Class for File System Benchmarking.
 */
public class FoundationDB implements Storage<byte[]> {
    private final static String CONFIGFILE = "foundationdb.properties";
    private final static String CLUSTERFILE = "fdb.cluster";
    private String cFile;
    private FoundationDBConfig config;
    private FDB fdb;
    private Database db;
    private Transaction tx;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(FoundationDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    FoundationDBConfig.class);
            cFile  = FoundationDB.class.getClassLoader().getResource(CLUSTERFILE).getPath();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("cfile", true, "cluster file, default : "+ cFile);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        cFile =  params.getOptionValue("cfile", cFile);
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        fdb = FDB.selectAPIVersion(config.version);
        db = fdb.open(cFile);
        tx = db.createTransaction();
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        tx.close();
        db.close();
    }

    @Override
    public Writer<byte[]> createWriter(final int id, final Parameters params) {
        try {
            return new FoundationDBWriter(id, params, tx);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader<byte[]> createReader(final int id, final Parameters params) {
        try {
            return new FoundationDBReader(id, params, tx);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }
}

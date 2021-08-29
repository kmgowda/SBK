/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FdbRecord;

import com.apple.foundationdb.record.RecordMetaData;
import com.apple.foundationdb.record.RecordMetaDataBuilder;
import com.apple.foundationdb.record.metadata.Key;
import com.apple.foundationdb.record.provider.foundationdb.FDBDatabase;
import com.apple.foundationdb.record.provider.foundationdb.FDBDatabaseFactory;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordContext;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStore;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpace;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpaceDirectory;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpacePath;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.google.protobuf.ByteString;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.ProtoBufByteString;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Class for FdbRecord Benchmarking.
 */
public class FdbRecord implements Storage<ByteString> {
    private final static String CONFIGFILE = "fdbrecord.properties";
    private FdbRecordConfig config;
    private FDBDatabase db;
    private Function<FDBRecordContext, FDBRecordStore> recordStoreProvider;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(FdbRecord.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    FdbRecordConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("cfile", true, "cluster file, default : " + config.cFile);
        params.addOption("keyspace", true, "Key space name, default : " + config.keySpace);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        if (params.getReadersCount() > 0 && params.getWritersCount() > 0) {
            throw new IllegalArgumentException("Specify either Writer or readers ; both are not allowed");
        }

        config.cFile = params.getOptionValue("cfile", config.cFile);
        config.keySpace = params.getOptionValue("keyspace", config.keySpace);
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        db = FDBDatabaseFactory.instance().getDatabase(config.cFile);

        // Define the keyspace for our application
        KeySpace keySpace = new KeySpace(new KeySpaceDirectory(config.keySpace, KeySpaceDirectory.KeyType.STRING, config.keySpace));

        // Get the path where our record store will be rooted
        KeySpacePath path = keySpace.path(config.keySpace);

        RecordMetaDataBuilder metaDataBuilder = RecordMetaData.newBuilder()
                .setRecords(FdbRecordLayerProto.getDescriptor());
        metaDataBuilder.getRecordType("Record")
                .setPrimaryKey(Key.Expressions.field("recordID"));
        recordStoreProvider = context -> FDBRecordStore.newBuilder()
                .setMetaDataProvider(metaDataBuilder)
                .setContext(context)
                .setKeySpacePath(path)
                .createOrOpen();
        if (params.getWritersCount() > 0) {
            db.run(context -> {
                FDBRecordStore recordStore = recordStoreProvider.apply(context);
                recordStore.deleteAllRecords();
                return null;
            });
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        db.close();
    }

    @Override
    public DataWriter<ByteString> createWriter(final int id, final ParameterOptions params) {
        try {
            if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                return new FdbRecordMultiWriter(id, params, db, recordStoreProvider);
            } else {
                return new FdbRecordWriter(id, params, db, recordStoreProvider);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<ByteString> createReader(final int id, final ParameterOptions params) {
        try {
            if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                return new FdbRecordMultiReader(id, params, db, recordStoreProvider);
            } else {
                return new FdbRecordReader(id, params, db, recordStoreProvider);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType<ByteString> getDataType() {
        return new ProtoBufByteString();
    }

}
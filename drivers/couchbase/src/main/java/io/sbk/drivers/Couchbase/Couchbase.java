/**
 * Copyright (c) KMG. All Rights Reserved..
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.drivers.Couchbase;

import com.couchbase.client.java.manager.bucket.BucketType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.couchbase.client.core.error.BucketNotFoundException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.manager.bucket.BucketManager;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.SbkString;
import io.sbk.params.InputOptions;
import io.sbk.params.ParameterOptions;
import io.sbk.system.Printer;


import java.io.IOException;
import java.time.Duration;
import java.util.Objects;


/**
 * Class for Couchbase storage driver.
 */
public class Couchbase implements Storage<String> {
    private final static String CONFIGFILE = "couchbase.properties";
    private CouchbaseConfig config;

    private Cluster cluster;

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Couchbase.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    CouchbaseConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("url", true, "Database Cluster url, default url: " + config.url);
        params.addOption("user", true, "User Name, default : " + config.user);
        params.addOption("password", true, "Bucket password, default : " + config.password);
        params.addOption("bucket", true, "Bucket Name, default : " + config.bucket);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.url = params.getOptionValue("url", config.url);
        config.user = params.getOptionValue("user", config.user);
        config.password = params.getOptionValue("password", config.password);
        config.bucket = params.getOptionValue("bucket", config.bucket);
    }

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        cluster = Cluster.connect(config.url, config.user, config.password);
        BucketManager bucketManager = cluster.buckets();
        try {
            if (params.getWritersCount() > 0) {
                bucketManager.dropBucket(config.bucket);
            }
        } catch (BucketNotFoundException ex) {
            Printer.log.info("The bucket : " + config.bucket + " not found");
        }
        Printer.log.info("Bucket List : " +bucketManager.getAllBuckets().keySet());
        if (bucketManager.getAllBuckets().keySet().contains(config.bucket)) {
            return;
        }
        bucketManager.createBucket(BucketSettings.create(config.bucket)
                .bucketType(BucketType.COUCHBASE)
                .ramQuotaMB(120)
                .numReplicas(1)
                .replicaIndexes(true)
                .flushEnabled(true));
        cluster.bucket(config.bucket).waitUntilReady(Duration.ofSeconds(30));

    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
            cluster.disconnect();
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        return new CouchbaseWriter(id, params, config, cluster.bucket(config.bucket));
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        return new CouchbaseReader(id, params, config, cluster.bucket(config.bucket));
    }

    @Override
    public DataType<String> getDataType() {
        return new SbkString();
    }
}

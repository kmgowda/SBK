/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.MinIO;

import io.minio.Result;
import io.minio.errors.InvalidArgumentException;
import io.minio.messages.Item;
import io.sbk.api.DataType;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;
import io.sbk.api.impl.ByteArray;

import java.io.IOException;


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;

import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.NoResponseException;
import io.minio.errors.RegionConflictException;
import io.sbk.api.impl.SbkLogger;

/**
 * Class for MinIO.
 */
public class MinIO implements Storage<byte[]> {
    private final static String CONFIGFILE = "minio.properties";
    private MinIOConfig config;
    private MinioClient mclient;
    private DataType<byte[]> dType;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(MinIO.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    MinIOConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("bucket", true, "Bucket name, default bucket name: "+config.bucketName);
        params.addOption("url", true, "Database url, default url: "+config.url);
        params.addOption("key", true, "Access Key, default User name: "+config.accessKey);
        params.addOption("secret", true, "secret key, default password: "+config.secretKey);
        params.addOption("recreate", true,
                "If the table is already existing, delete and recreate the same, default: "+config.reCreate);

    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        config.bucketName =  params.getOptionValue("bucket", config.bucketName);
        config.url = params.getOptionValue("url", config.url);
        config.accessKey = params.getOptionValue("key", config.accessKey);
        config.secretKey = params.getOptionValue("secret", config.secretKey);
        if (params.getWritersCount() > 0 && params.getReadersCount() > 0) {
            config.reCreate = true;
        } else {
            config.reCreate = Boolean.parseBoolean(params.getOptionValue("recreate", String.valueOf(config.reCreate)));
        }
        dType = new ByteArray();
    }

    @Override
    public void openStorage(final Parameters params) throws IOException {
        try {
            // Create a minioClient with the MinIO Server name, Port, Access key and Secret key.
            mclient = new MinioClient(config.url, config.accessKey, config.secretKey);

            // Check if the bucket already exists.
            boolean isExist = mclient.bucketExists(config.bucketName);
            if (isExist) {
                SbkLogger.log.info("Bucket '" + config.bucketName +"' already exists.");
                if (config.reCreate && params.getWritersCount() > 0) {
                    SbkLogger.log.info("Deleting the Bucket: " + config.bucketName );
                    //Delete all existing objects first
                    final Iterable<Result<Item>> results =
                            mclient.listObjects(config.bucketName, config.bucketName, false);
                    Item item;
                    for (Result<Item> result : results) {
                        item = result.get();
                        SbkLogger.log.info("Deleting the object: " + item.objectName() );
                        mclient.removeObject(config.bucketName, item.objectName());
                    }
                    mclient.removeBucket(config.bucketName);
                    isExist = false;
                }
            } else if (params.getWritersCount() < 1) {
                throw  new IOException("Bucket '" + config.bucketName +"' does not exist.");
            } else {
                SbkLogger.log.info("Bucket '" + config.bucketName +"' does not exist.");
            }

            if (!isExist && params.getWritersCount() > 0) {
                SbkLogger.log.info("Creating the Bucket: " + config.bucketName );
                mclient.makeBucket(config.bucketName);
            }
        } catch (InvalidPortException | InvalidEndpointException | org.xmlpull.v1.XmlPullParserException |
                InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException |
                InvalidKeyException | ErrorResponseException | RegionConflictException | NoResponseException |
                InternalException | InvalidArgumentException ex) {
            throw new IOException(ex);
        }
    }


    @Override
    public void closeStorage(final Parameters params) throws IOException {
    }

    @Override
    public Writer<byte[]> createWriter(final int id, final Parameters params) {
        return new MinIOWriter(id, params, config, mclient, dType);
    }

    @Override
    public Reader<byte[]> createReader(final int id, final Parameters params) {
        return  new MinIOReader(id, params, config, mclient);
    }

    @Override
    public DataType<byte[]> getDataType() throws IllegalArgumentException {
        return dType;
    }

}

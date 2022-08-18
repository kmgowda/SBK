/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.MinIO;

import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.NoResponseException;
import io.minio.messages.Item;
import io.perl.api.PerlChannel;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;
import io.sbk.api.Status;
import io.sbk.data.DataType;
import io.time.Time;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Class for MinIO Reader.
 */
public class MinIOReader implements Reader<byte[]> {
    final private ParameterOptions params;
    final private MinIOConfig config;
    final private MinioClient client;

    public MinIOReader(int id, ParameterOptions params, MinIOConfig config, MinioClient client) {
        this.params = params;
        this.config = config;
        this.client = client;
    }

    @Override
    public void recordRead(DataType dType, int size, Time time, Status status, PerlChannel perlChannel) throws IOException {
        final Iterable<Result<Item>> results =
                client.listObjects(config.bucketName, config.bucketName, false);
        Item item;
        InputStream inStream;
        try {
            for (Result<Item> result : results) {
                status.startTime = time.getCurrentTime();
                item = result.get();
                status.bytes = (int) client.statObject(config.bucketName, item.objectName()).length();
                inStream = client.getObject(config.bucketName, item.objectName());
                status.endTime = time.getCurrentTime();
                perlChannel.send(status.startTime, status.endTime, 1, status.bytes);
                inStream.close();
            }
        } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException |
                InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException |
                InternalException | InvalidArgumentException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void recordReadTime(DataType dType, int size, Time time, Status status, PerlChannel perlChannel) throws IOException {
        final Iterable<Result<Item>> results =
                client.listObjects(config.bucketName, config.bucketName, false);
        Item item;
        int ret;
        InputStream inStream;
        try {
            for (Result<Item> result : results) {
                status.startTime = time.getCurrentTime();
                item = result.get();
                status.bytes = (int) client.statObject(config.bucketName, item.objectName()).length();
                byte[] inData = new byte[status.bytes];
                inStream = client.getObject(config.bucketName, item.objectName());
                ret = inStream.read(inData);
                if (ret > 0) {
                    status.endTime = dType.getTime(inData);
                    perlChannel.send(status.startTime, status.endTime, 1, status.bytes);
                }
                inStream.close();
            }
        } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException |
                InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException |
                InternalException | InvalidArgumentException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public byte[] read() throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}

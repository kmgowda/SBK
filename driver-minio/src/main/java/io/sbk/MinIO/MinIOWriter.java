/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.MinIO;
import io.minio.MinioClient;
import io.minio.ServerSideEncryption;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.NoResponseException;
import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;
import io.sbk.api.Writer;
import io.sbk.api.DataType;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Class for MinIO Writer.
 */
public class MinIOWriter implements Writer<byte[]> {
    final private MinIOConfig config;
    final private MinioClient client;
    final private InputStream dataStream;

    public MinIOWriter(int id, Parameters params, MinIOConfig config, MinioClient client, DataType<byte[]> dType) {
        this.config = config;
        this.client = client;
        dataStream = new ByteArrayInputStream(dType.create(params.getRecordSize()), 0, params.getRecordSize());
    }

    @Override
    public long recordWrite(byte[] data, int size, RecordTime record, int id) throws IOException {
        final long time = System.currentTimeMillis();
        dataStream.reset();
        try {
            client.putObject(config.bucketName, config.bucketName + "-" + UUID.randomUUID().toString(),
                    dataStream, (long) size, null, (ServerSideEncryption) null, null);
        } catch (InvalidBucketNameException | NoSuchAlgorithmException | InvalidKeyException | NoResponseException |
                XmlPullParserException | ErrorResponseException | InternalException | InvalidArgumentException |
                InsufficientDataException e) {
           throw new IOException(e);
        }
        record.accept(id, time, System.currentTimeMillis(), size, 1);
        return time;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        try {
            client.putObject(config.bucketName, config.bucketName + "-" + UUID.randomUUID().toString(),
                    new ByteArrayInputStream(data), (long) data.length, null, (ServerSideEncryption) null,
                    null);
        } catch (InvalidBucketNameException | NoSuchAlgorithmException | InvalidKeyException | NoResponseException |
                XmlPullParserException | ErrorResponseException | InternalException | InvalidArgumentException |
                InsufficientDataException e) {
            throw new IOException(e);
        }
        return null;
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws  IOException {
       dataStream.close();
    }
}
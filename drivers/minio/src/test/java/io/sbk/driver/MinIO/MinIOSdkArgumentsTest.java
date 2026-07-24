/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.MinIO;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetObjectTagsArgs;
import io.sbk.params.InputParameterOptions;
import io.sbk.params.ParameterOptions;
import io.sbk.params.impl.SbkDriversParameters;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifies the actual MinIO SDK argument objects emitted by writer operations.
 *
 * <p>These tests stop at the SDK boundary: no S3 service or hand-written S3
 * protocol implementation is involved.
 */
public class MinIOSdkArgumentsTest {

    @Test
    public void putEmbedsTagsInTheSingleSdkRequest() throws Exception {
        MinioClient client = mock(MinioClient.class);
        MinIOConfig config = baseConfig();
        config.taggingEnabled = true;
        config.taggingTags = "team=storage,scenario=put";
        MinIOWriter writer = writer(config, S3Operation.PUT, client,
                new S3ObjectCatalog(List.of()));

        writer.writeAsync(new byte[128]);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client, times(1)).putObject(captor.capture());
        verify(client, times(0)).setObjectTags(any());
        PutObjectArgs args = captor.getValue();
        assertEquals("sbk-test", args.bucket());
        assertTrue(args.object().startsWith("objects/"));
        assertEquals("storage", args.tags().get().get("team"));
        assertEquals("put", args.tags().get().get("scenario"));
    }

    @Test
    public void tagSetTargetsTheCatalogVersion() throws Exception {
        MinioClient client = mock(MinioClient.class);
        MinIOConfig config = baseConfig();
        config.taggingTags = "phase=verification";
        S3ObjectRef versioned = new S3ObjectRef("objects/versioned", "version-42", 16, 0);
        MinIOWriter writer = writer(config, S3Operation.TAG_SET, client,
                new S3ObjectCatalog(List.of(versioned)));

        writer.writeAsync(new byte[1]);

        ArgumentCaptor<SetObjectTagsArgs> captor =
                ArgumentCaptor.forClass(SetObjectTagsArgs.class);
        verify(client).setObjectTags(captor.capture());
        SetObjectTagsArgs args = captor.getValue();
        assertEquals("objects/versioned", args.object());
        assertEquals("version-42", args.versionId());
        assertEquals("verification", args.tags().get().get("phase"));
    }

    private static MinIOWriter writer(MinIOConfig config, S3Operation operation,
                                      MinioClient client, S3ObjectCatalog catalog) throws Exception {
        InputParameterOptions parsed = new SbkDriversParameters(
                "SBK MinIO SDK argument test", new String[]{"MinIO"}, new String[]{});
        parsed.parseArgs(new String[]{"-writers", "1", "-size", "128", "-seconds", "1"});
        ParameterOptions params = parsed;
        Queue<String> createdBuckets = new ConcurrentLinkedQueue<>();
        return new MinIOWriter(0, params, config, operation, client, null, catalog,
                List.of(), createdBuckets, "test-run", null);
    }

    private static MinIOConfig baseConfig() {
        MinIOConfig config = new MinIOConfig();
        config.bucketName = "sbk-test";
        config.prefix = "objects";
        config.copyPrefix = "copies";
        config.bucketPrefix = "sbk-bucket";
        config.async = false;
        config.asyncDepth = 1;
        config.partSize = 0;
        config.taggingEnabled = false;
        config.taggingTags = "";
        config.checksumAlgorithm = "";
        config.dataCompressibility = 0;
        config.dataDedupable = true;
        config.retryMaxAttempts = 1;
        return config;
    }
}

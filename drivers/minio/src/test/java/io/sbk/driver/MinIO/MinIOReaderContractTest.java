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

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.perl.api.PerlChannel;
import io.sbk.api.Status;
import io.sbk.data.impl.ByteArray;
import io.sbk.params.InputParameterOptions;
import io.sbk.params.ParameterOptions;
import io.sbk.params.impl.SbkDriversParameters;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins reader operation contracts at the MinIO SDK boundary.
 */
public class MinIOReaderContractTest {

    @Test
    public void bucketStatFailsWhenTheTargetDoesNotExist() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        MinIOConfig config = baseConfig();
        MinIOReader reader = new MinIOReader(0, parameters(), config,
                S3Operation.BUCKET_STAT, client, null, new S3ObjectCatalog(List.of()),
                List.of("missing-bucket"), null, null,
                new java.util.concurrent.atomic.LongAdder());

        IOException failure = assertThrows(IOException.class, () -> reader.recordRead(
                new ByteArray(), 1, new NanoSeconds(), new Status(), mock(PerlChannel.class)));

        assertTrue(failure.getMessage().contains("does not exist"));
    }

    private static ParameterOptions parameters() throws Exception {
        InputParameterOptions parsed = new SbkDriversParameters(
                "SBK MinIO reader contract test", new String[]{"MinIO"}, new String[]{});
        parsed.parseArgs(new String[]{"-readers", "1", "-size", "1", "-seconds", "1"});
        return parsed;
    }

    private static MinIOConfig baseConfig() {
        MinIOConfig config = new MinIOConfig();
        config.bucketName = "sbk-test";
        config.async = false;
        config.asyncDepth = 1;
        config.writeOperation = "put";
        config.writeMix = "";
        config.readMix = "";
        config.listPrefixes = "";
        config.retryMaxAttempts = 1;
        return config;
    }
}

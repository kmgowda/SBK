/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.PerlBench;

import io.sbk.api.Reader;
import io.sbk.api.Writer;
import io.sbk.params.impl.SbkParameters;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests the allocation-free synthetic storage operations.
 */
public final class PerlBenchTest {

    /**
     * Verify synchronous writer completion without a future allocation.
     *
     * @throws Exception if driver setup or writing fails
     */
    @Test
    public void writerCompletesSynchronously() throws Exception {
        final SbkParameters parameters = writerParameters();
        final PerlBench storage = new PerlBench();
        storage.openStorage(parameters);
        final Writer<byte[]> writer =
                (Writer<byte[]>) storage.createWriter(0, parameters);

        try {
            final CompletableFuture<?> result =
                    writer.writeAsync(new byte[8]);
            assertNull(result,
                    "null denotes allocation-free synchronous completion");
        } finally {
            writer.close();
            storage.closeStorage(parameters);
        }
    }

    /**
     * Verify that a reader reuses one correctly sized worker-owned payload.
     *
     * @throws Exception if driver setup or reading fails
     */
    @Test
    public void readerReusesPreallocatedPayload() throws Exception {
        final SbkParameters parameters =
                new SbkParameters("perlbench-reader-test");
        parameters.parseArgs(new String[]{
                "-readers", "1", "-size", "64", "-records", "2"
        });
        final PerlBench storage = new PerlBench();
        storage.openStorage(parameters);
        final Reader<byte[]> reader =
                (Reader<byte[]>) storage.createReader(0, parameters);

        try {
            final byte[] first = reader.read();
            final byte[] second = reader.read();
            assertEquals(64, first.length);
            assertSame(first, second,
                    "the read hot path must not allocate a payload");
        } finally {
            reader.close();
            storage.closeStorage(parameters);
        }
    }

    private static SbkParameters writerParameters() throws Exception {
        final SbkParameters parameters =
                new SbkParameters("perlbench-writer-test");
        parameters.parseArgs(new String[]{
                "-writers", "1", "-size", "8", "-records", "1"
        });
        return parameters;
    }
}

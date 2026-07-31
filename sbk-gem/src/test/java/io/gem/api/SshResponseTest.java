/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests bounded SSH command diagnostics.
 */
final class SshResponseTest {
    @Test
    void retainsOnlyTheMostRecentDiagnosticBytes() throws IOException {
        final SshResponse response = new SshResponse(true, 8);

        response.stdOutputStream.write("012345".getBytes(StandardCharsets.UTF_8));
        response.stdOutputStream.write("6789".getBytes(StandardCharsets.UTF_8));
        response.errOutputStream.write("abcdefghijk".getBytes(StandardCharsets.UTF_8));

        assertEquals("23456789", response.stdOutputStream.toString());
        assertEquals("defghijk", response.errOutputStream.toString());
    }

    @Test
    void handlesWrappedWritesWithoutChangingByteOrder() throws IOException {
        final SshResponse response = new SshResponse(true, 5);

        response.stdOutputStream.write("abc".getBytes(StandardCharsets.UTF_8));
        response.stdOutputStream.write("de".getBytes(StandardCharsets.UTF_8));
        response.stdOutputStream.write("f".getBytes(StandardCharsets.UTF_8));

        assertEquals("bcdef", response.stdOutputStream.toString());
    }

    @Test
    void safelyRetainsOutputWrittenByConcurrentSshCallbacks() throws IOException {
        final int capacity = 4096;
        try (BoundedTailOutputStream output = new BoundedTailOutputStream(capacity)) {
            final byte[] stdout = "stdout\n".getBytes(StandardCharsets.UTF_8);
            final byte[] stderr = "stderr\n".getBytes(StandardCharsets.UTF_8);
            final CountDownLatch start = new CountDownLatch(1);

            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                try (var executor = Executors.newFixedThreadPool(2)) {
                    executor.submit(() -> writeRepeatedly(output, stdout, start));
                    executor.submit(() -> writeRepeatedly(output, stderr, start));
                    start.countDown();
                    executor.shutdown();
                    assertTrue(executor.awaitTermination(4, TimeUnit.SECONDS));
                }
            });

            final byte[] retained = output.toByteArray();
            assertEquals(capacity, retained.length);
            assertTrue(output.toString().contains("stdout") || output.toString().contains("stderr"));
        }
    }

    private static void writeRepeatedly(BoundedTailOutputStream output, byte[] value, CountDownLatch start) {
        try {
            start.await();
            for (int index = 0; index < 10_000; index++) {
                output.write(value);
            }
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}

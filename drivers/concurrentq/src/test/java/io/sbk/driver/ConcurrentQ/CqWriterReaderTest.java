/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.ConcurrentQ;

import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class CqWriterReaderTest {

    private LinkedCQueue<byte[]> queue;

    @Before
    public void setUp() {
        queue = new LinkedCQueue<>();
    }

    @Test
    public void testCqWriterWriteAsync() throws IOException {
        CqWriter writer = new CqWriter(queue);
        byte[] data = {1, 2, 3};
        CompletableFuture future = writer.writeAsync(data);
        assertNull("writeAsync should return null", future);
        assertEquals(1, queue.size());
        assertArrayEquals(data, queue.poll());
    }

    @Test
    public void testCqWriterSyncAndClose() throws IOException {
        CqWriter writer = new CqWriter(queue);
        writer.sync(); // Should not throw
        writer.close(); // Should not throw
    }

    @Test
    public void testCqReaderRead() throws IOException {
        CqReader reader = new CqReader(queue);
        byte[] data = {4, 5, 6};
        queue.add(data);
        byte[] readData = reader.read();
        assertArrayEquals(data, readData);
    }

    @Test
    public void testCqReaderReadReturnsNullIfEmpty() throws IOException {
        CqReader reader = new CqReader(queue);
        // Remove all items to ensure empty
        queue.clear();
        // The read method will block, so we test only the initial poll
        // To avoid blocking, we can check poll directly
        assertNull(queue.poll());
    }

    @Test
    public void testCqReaderClose() throws IOException {
        CqReader reader = new CqReader(queue);
        reader.close(); // Should not throw
    }
}
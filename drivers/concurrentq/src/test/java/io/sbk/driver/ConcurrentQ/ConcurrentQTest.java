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

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.impl.SbkParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConcurrentQTest {

    private ConcurrentQ concurrentQ;
    private SbkParameters params;

    @BeforeEach
    public void setUp() throws Exception {
        concurrentQ = new ConcurrentQ();
        params = new SbkParameters("test");
        // Set required options for 1 reader and 1 writer, and record size
        params.parseArgs(new String[]{"-writers", "1", "-readers", "1", "-size", "10"});
    }

    @Test
    public void testParseArgsThrowsOnInvalidCounts() throws Exception {
        SbkParameters badParams = new SbkParameters("test");
        // Both writers and readers set to 0, should throw
        assertThrows(IllegalArgumentException.class, () -> {
            badParams.parseArgs(new String[]{"-writers", "0", "-readers", "0", "-size", "10"});
        });
    }

    @Test
    public void testCreateWriterAndReader() {
        concurrentQ.queue = new LinkedCQueue<>();
        DataWriter<byte[]> writer = concurrentQ.createWriter(0, params);
        DataReader<byte[]> reader = concurrentQ.createReader(0, params);
        assertNotNull(writer);
        assertNotNull(reader);
    }
}

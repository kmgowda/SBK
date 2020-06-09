/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FileStream;
import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;
import io.sbk.api.Writer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for File Buffered Writer.
 */
public class FileBufferedWriter implements Writer<byte[]> {
    final private BufferedOutputStream out;

    public FileBufferedWriter(int id, Parameters params, FileStreamConfig config) throws IOException {
        this.out = new BufferedOutputStream(new FileOutputStream(config.fileName, config.isAppend), config.bufferSize);
    }

    @Override
    public long recordWrite(byte[] data, int size, RecordTime record, int id) throws IOException {
        final long time = System.currentTimeMillis();
        out.write(data);
        record.accept(id, time, System.currentTimeMillis(), size, 1);
        return time;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        out.write(data);
        return null;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws  IOException {
        out.close();
    }
}
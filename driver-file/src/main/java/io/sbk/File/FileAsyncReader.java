/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.File;

import io.sbk.api.AsyncReader;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;


import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

/**
 * Class for File Asynchronous Reader.
 */
public class FileAsyncReader implements AsyncReader<ByteBuffer> {
    final private FileChannel in;
    final private DataType<ByteBuffer> dType;
    final private ByteBuffer readBuffer;
    final private int size;

    public FileAsyncReader(int id, Parameters params, DataType<ByteBuffer> dType, FileConfig config) throws IOException {
        this.in = FileChannel.open(Paths.get(config.fileName), StandardOpenOption.READ);
        this.size = params.getRecordSize();
        this.dType = dType;
        this.readBuffer = dType.create(params.getRecordSize());
    }

    @Override
    public CompletableFuture<ByteBuffer> readAsync() {
        final CompletableFuture<ByteBuffer> ret = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            final ByteBuffer readBuffer = dType.create(size);
            try {
                final int readSize = in.read(readBuffer);
                if (readSize <= 0) {
                    ret.completeExceptionally(new EOFException());
                }
                ret.complete(readBuffer);
            } catch (IOException ex) {
                ret.completeExceptionally(ex);
            }
        });
        return ret;
    }

    @Override
    public void close() throws  IOException {
        in.close();
    }
}
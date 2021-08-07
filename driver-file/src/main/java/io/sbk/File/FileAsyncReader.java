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
import io.sbk.data.DataType;
import io.sbk.api.ParameterOptions;


import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class for File Asynchronous Reader.
 */
public class FileAsyncReader implements AsyncReader<ByteBuffer> {
    final private FileChannel in;
    final private DataType<ByteBuffer> dType;
    final private ExecutorService executor;

    public FileAsyncReader(int id, ParameterOptions params, DataType<ByteBuffer> dType, FileConfig config) throws IOException {
        this.in = FileChannel.open(Paths.get(config.fileName), StandardOpenOption.READ);
        this.dType = dType;
        this.executor = Executors.newFixedThreadPool(config.asyncThreads);
    }

    @Override
    public CompletableFuture<ByteBuffer> readAsync(int size) {
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
        }, executor);
        return ret;
    }

    @Override
    public void close() throws  IOException {
        in.close();
    }
}
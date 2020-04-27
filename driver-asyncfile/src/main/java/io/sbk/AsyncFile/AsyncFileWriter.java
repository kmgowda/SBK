/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.AsyncFile;
import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;
import io.sbk.api.Writer;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * Class for Async File Writer.
 */
public class AsyncFileWriter implements Writer<ByteBuffer> {
    final private String fileName;
    final private AsynchronousFileChannel out;
    private long pos;

    public AsyncFileWriter(int id, Parameters params, String fileName) throws IOException {
        this.fileName = fileName;
        this.out = AsynchronousFileChannel.open(Paths.get(fileName), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        this.pos = 0;
    }

    @Override
    public long recordWrite(ByteBuffer data, int size, RecordTime record, int id) throws IOException {
        final long time = System.currentTimeMillis();
        final ByteBuffer buffer = data.asReadOnlyBuffer();
        out.write(buffer, pos, buffer,
        new CompletionHandler<Integer, ByteBuffer>() {

            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                final long endTime = System.currentTimeMillis();
                record.accept(id, time, endTime, result, 1);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                System.out.println("AsyncFileWriter Write failed");
                exc.printStackTrace();
            }
        });
        pos += data.capacity();
        return time;
    }


    @Override
    public CompletableFuture writeAsync(ByteBuffer data) throws IOException {
        try {
            out.write(data.asReadOnlyBuffer(), pos).get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IOException(ex);
        }
        pos += data.capacity();
        return null;
    }

    @Override
    public void flush() throws IOException {
        out.force(true);
    }

    @Override
    public void close() throws  IOException {
        out.close();
    }
}
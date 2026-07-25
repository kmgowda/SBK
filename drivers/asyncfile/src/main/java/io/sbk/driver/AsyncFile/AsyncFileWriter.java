/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.AsyncFile;

import io.perl.api.PerlChannel;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.data.DataType;
import io.time.Time;

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
    final private AsyncFileOperations operations;
    private long pos;

    public AsyncFileWriter(int id, ParameterOptions params, String fileName) throws IOException {
        this.fileName = fileName;
        this.out = AsynchronousFileChannel.open(Paths.get(fileName), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        this.operations = new AsyncFileOperations();
        this.pos = 0;
    }

    @Override
    public void recordWrite(DataType<ByteBuffer> dType, ByteBuffer data, int size, Time time,
                            Status status, PerlChannel record) throws IOException {
        final ByteBuffer buffer = data.asReadOnlyBuffer();
        final long ctime = time.getCurrentTime();

        status.startTime = ctime;
        status.bytes = size;
        status.records = 1;
        operations.submitted();
        try {
            out.write(buffer, pos, buffer,
                    new CompletionHandler<Integer, ByteBuffer>() {

                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            try {
                                final long endTime = time.getCurrentTime();
                                record.send(ctime, endTime, 1, result);
                            } finally {
                                operations.completed();
                            }
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            operations.failed(exc);
                            record.throwException(exc);
                        }
                    });
        } catch (RuntimeException ex) {
            operations.failed(ex);
            throw ex;
        }
        pos += data.capacity();
    }


    @Override
    public CompletableFuture<?> writeAsync(ByteBuffer data) throws IOException {
        try {
            out.write(data.asReadOnlyBuffer(), pos).get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IOException(ex);
        }
        pos += data.capacity();
        return null;
    }

    @Override
    public void sync() throws IOException {
        operations.awaitCompletion();
        out.force(true);
    }

    @Override
    public void close() throws IOException {
        operations.awaitCompletion();
        out.close();
    }
}

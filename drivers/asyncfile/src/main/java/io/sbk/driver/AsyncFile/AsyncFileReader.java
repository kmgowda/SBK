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
import io.sbk.api.Reader;
import io.sbk.api.Status;
import io.sbk.data.DataType;
import io.time.Time;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class for File Reader.
 */
public class AsyncFileReader implements Reader<ByteBuffer> {
    final private String fileName;
    final private ParameterOptions params;
    final private AsynchronousFileChannel in;
    final private AtomicBoolean isEOF;
    final private AsyncFileOperations operations;
    private long pos;

    public AsyncFileReader(int id, ParameterOptions params, String fileName) throws IOException {
        this.fileName = fileName;
        this.params = params;
        this.in = AsynchronousFileChannel.open(Paths.get(fileName), StandardOpenOption.READ);
        this.pos = 0;
        this.isEOF = new AtomicBoolean(false);
        this.operations = new AsyncFileOperations();
    }

    @Override
    public ByteBuffer read() throws IOException {
        return null;
    }


    @Override
    public void recordRead(DataType<ByteBuffer> dType, int size, Time time, Status status, PerlChannel perlChannel) throws IOException {
        final long ctime = time.getCurrentTime();
        final ByteBuffer buffer = dType.allocate(params.getRecordSize());
        status.startTime = ctime;
        status.endTime = ctime;
        status.bytes = size;
        status.records = 1;
        operations.submitted();
        try {
            in.read(buffer, pos, buffer,
                    new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        try {
                            final long endTime = time.getCurrentTime();
                            if (result <= 0 && !isEOF.get()) {
                                isEOF.set(true);
                                perlChannel.throwException(new EOFException());
                            } else {
                                perlChannel.send(ctime, endTime, 1, result);
                            }
                        } finally {
                            operations.completed();
                        }
                    }

                    @Override
                    public void failed(Throwable ex, ByteBuffer attachment) {
                        operations.failed(ex);
                        if (!isEOF.get()) {
                            perlChannel.throwException(ex);
                        }
                    }
                    });
        } catch (RuntimeException ex) {
            operations.failed(ex);
            throw ex;
        }
        pos += dType.length(buffer);
    }


    @Override
    public void recordReadTime(DataType<ByteBuffer> dType, int size, Time time, Status status, PerlChannel perlChannel) throws IOException {
        final ByteBuffer buffer = dType.allocate(params.getRecordSize());
        status.startTime = time.getCurrentTime();
        status.endTime = status.startTime;
        status.bytes = size;
        status.records = 1;
        operations.submitted();
        try {
            in.read(buffer, pos, buffer,
                    new CompletionHandler<Integer, ByteBuffer>() {

                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        try {
                            final long endTime = time.getCurrentTime();
                            if (result <= 0 && !isEOF.get()) {
                                isEOF.set(true);
                                perlChannel.throwException(new EOFException());
                            } else {
                                perlChannel.send(dType.getTime(attachment), endTime, 1, result);
                            }
                        } finally {
                            operations.completed();
                        }
                    }

                    @Override
                    public void failed(Throwable ex, ByteBuffer attachment) {
                        operations.failed(ex);
                        if (!isEOF.get()) {
                            perlChannel.throwException(ex);
                        }
                    }
                    });
        } catch (RuntimeException ex) {
            operations.failed(ex);
            throw ex;
        }
        pos += dType.length(buffer);
    }

    @Override
    public void close() throws IOException {
        operations.awaitCompletion();
        in.close();
    }
}

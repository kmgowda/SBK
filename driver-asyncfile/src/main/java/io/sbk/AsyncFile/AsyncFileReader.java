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

import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.Reader;
import io.sbk.api.RecordTime;
import io.sbk.api.TimeStamp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Class for File Reader.
 */
public class AsyncFileReader implements Reader<ByteBuffer> {
    final private String fileName;
    final private Parameters params;
    final private AsynchronousFileChannel in;
    private long pos;

    public AsyncFileReader(int id, Parameters params, String fileName) throws IOException {
        this.fileName = fileName;
        this.params = params;
        this.in = AsynchronousFileChannel.open(Paths.get(fileName), StandardOpenOption.READ);
        this.pos = 0;
    }

    @Override
    public ByteBuffer read() throws IOException {
            return null;
    }


    @Override
    public void recordRead(DataType dType, TimeStamp status, RecordTime recordTime, int id) throws IOException {
        final long time = System.currentTimeMillis();
        final ByteBuffer buffer = (ByteBuffer) dType.allocate(params.getRecordSize());
        in.read(buffer, pos, buffer,
                new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        final long endTime = System.currentTimeMillis();
                        recordTime.accept(id, time, endTime, result, 1);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        System.out.println("AsyncFileWriter Write failed");
                        exc.printStackTrace();
                    }
                });
        pos += dType.length(buffer);
    }


    @Override
    public void recordReadTime(DataType dType, TimeStamp status, RecordTime recordTime, int id) throws IOException {
        final ByteBuffer buffer = (ByteBuffer) dType.allocate(params.getRecordSize());
        in.read(buffer, pos, buffer,
                new CompletionHandler<Integer, ByteBuffer>() {

                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        final long endTime = System.currentTimeMillis();
                        recordTime.accept(id, dType.getTime(attachment), endTime, result, 1);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        System.out.println("AsyncFileReader read failed");
                        exc.printStackTrace();
                    }
                });
        pos += dType.length(buffer);
    }

    @Override
    public void close() throws  IOException {
        in.close();
    }
}
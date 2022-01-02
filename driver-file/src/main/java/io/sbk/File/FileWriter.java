/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.File;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.data.DataType;
import io.sbk.perl.SendChannel;
import io.time.Time;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;


/**
 * Class for File Channel Writer.
 */
public class FileWriter implements Writer<ByteBuffer> {
    final private FileChannel out;
    final private FileConfig config;

    public FileWriter(int id, ParameterOptions params, FileConfig config) throws IOException {
        this.config = config;
        if (config.isAppend) {
            this.out = FileChannel.open(Paths.get(config.fileName), StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            this.out = FileChannel.open(Paths.get(config.fileName), StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    @Override
    public void recordWrite(DataType<ByteBuffer> dType, ByteBuffer data, int size, Time time,
                            Status status, SendChannel record, int id) throws IOException {
        final ByteBuffer buffer = data.asReadOnlyBuffer();
        status.startTime = time.getCurrentTime();
        out.write(buffer);
        status.endTime = time.getCurrentTime();
        status.bytes = size;
        status.records = 1;
        record.send(id, status.startTime, status.endTime, size, 1);
    }


    @Override
    public CompletableFuture writeAsync(ByteBuffer data) throws IOException {
        final ByteBuffer buffer = data.asReadOnlyBuffer();
        out.write(buffer);
        return null;
    }

    @Override
    public void sync() throws IOException {
        out.force(config.metaUpdate);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
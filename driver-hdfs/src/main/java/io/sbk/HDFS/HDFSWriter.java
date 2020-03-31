/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.HDFS;
import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;
import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataOutputStream;

/**
 * Class for HDFS Writer.
 */
public class HDFSWriter implements Writer<byte[]> {
    final private FSDataOutputStream out;

    public HDFSWriter(int id, Parameters params, FileSystem fileSystem, Path filePath,
                      boolean recreate) throws IOException {
        if (recreate) {
            out = fileSystem.create(filePath, true, params.getRecordSize());
        } else {
            fileSystem.createNewFile(filePath);
            FSDataOutputStream outStream;
            try {
                outStream = fileSystem.append(filePath, params.getRecordSize());
            } catch (UnsupportedOperationException ex) {
                ex.printStackTrace();
                outStream = fileSystem.create(filePath, true, params.getRecordSize());
            }
            out = outStream;
        }
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
        out.hflush();
        out.hsync();
    }

    @Override
    public void close() throws  IOException {
        out.close();
    }
}
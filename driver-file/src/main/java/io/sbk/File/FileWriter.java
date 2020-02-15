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
import io.sbk.api.Parameters;
import io.sbk.api.Writer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for File Writer.
 */
public class FileWriter implements Writer<byte[]> {
    final private String fileName;
    final private FileOutputStream out;
    final private boolean sync;

    public FileWriter(int id, Parameters params, String fileName, boolean sync) throws IOException {
        this.fileName = fileName;
        this.out = new FileOutputStream(fileName, false);
        this.sync = sync;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        out.write(data);
        if (sync) {
            out.flush();
            out.getFD().sync();
        }
        return null;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
        if (sync) {
            out.getFD().sync();
        }
    }

    @Override
    public void close() throws  IOException {
        out.close();
    }
}
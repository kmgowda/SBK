/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.BookKeeper;
import io.sbk.api.Writer;
import org.apache.distributedlog.DLSN;
import org.apache.distributedlog.LogRecord;
import org.apache.distributedlog.api.AsyncLogWriter;
import org.apache.distributedlog.api.DistributedLogManager;
import org.apache.distributedlog.util.TimeSequencer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Class for BookKeeper Writer.
 */
public class BookKeeperWriter implements Writer<byte[]> {
    private final AsyncLogWriter writer;
    private final TimeSequencer sequencer;
    private CompletableFuture<DLSN> ret;

    public BookKeeperWriter(DistributedLogManager dlm) throws IOException {
        try {
            writer = dlm.openAsyncLogWriter().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IOException(ex);
        }
        sequencer = new TimeSequencer();
        sequencer.setLastId(writer.getLastTxId());
    }

    @Override
    public CompletableFuture<DLSN> writeAsync(byte[] data) throws IOException {
        LogRecord record = new LogRecord(
                sequencer.nextId(), data);
        ret =  writer.write(record);
        return ret;
    }

    @Override
    public void sync() throws IOException {
        try {
            ret.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void close() throws  IOException {
        try {
            writer.asyncClose().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IOException(ex);
        }
    }
}
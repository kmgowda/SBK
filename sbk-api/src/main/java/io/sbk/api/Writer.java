/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Writers.
 */
public interface Writer {

    /**
     * Asynchronously Writes the data .
     * @param data data to write
     * @return CompletableFuture completable future. null if the write completed synchronously .
     * @throws IOException If an exception occurred
     */
    CompletableFuture writeAsync(byte[] data) throws IOException;

    /**
     * Flush the  data.
     * @throws IOException If an exception occurred.
     */
    void flush() throws IOException;

    /**
     * Close the  Writer.
     * @throws IOException If an exception occurred.
     */
    void close() throws IOException;

    /**
     * Default implementation for writing data using {@link io.sbk.api.Writer#writeAsync(byte[])}
     * and recording the benchmark statistics.
     * if you are intend to not use the CompletableFuture returned by {@link io.sbk.api.Writer#writeAsync(byte[])}
     * then you can override this method. otherwise, use the default implementation and don't override this method.
     *
     * @param data   data to write
     * @param recordTime to call for benchmarking
     * @return time return the data sent time
     * @throws IOException If an exception occurred.
     */
    default long recordWrite(byte[] data, QuadConsumer recordTime) throws IOException {
        CompletableFuture ret;
        final long time = System.currentTimeMillis();
        ret = writeAsync(data);
        if (ret == null) {
            final long endTime = System.currentTimeMillis();
            recordTime.accept(time, endTime, data.length, 1);
        } else {
            ret.thenAccept(d -> {
                final long endTime = System.currentTimeMillis();
                recordTime.accept(time, endTime, data.length, 1);
            });
        }
        return time;
    }
}

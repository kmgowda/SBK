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

public interface ReaderCallback {

    /**
     * Consume the data.
     * @param data data read from storage client/device.
     */
    void consume(final Object data);

    /**
     * Accept the benchmarking data.
     * if your storage driver is not interested in passing the data read;
     * then below method can be overridden.
     *
     * @param startTime Start time
     * @param endTime End Time.
     * @param dataSize  size of the data in bytes.
     * @param records  number of records/events/messages.
     */
    default void record(long startTime, long endTime, int dataSize, int records) {

    }
}

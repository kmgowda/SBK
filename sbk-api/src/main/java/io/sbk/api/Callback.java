/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

/**
 * Callback interface used by callback-driven drivers to receive data and
 * optionally report per-record benchmarking metrics.
 *
 * <p>Drivers that provide a callback-style API may implement this interface
 * to bridge into SBK's metric reporting. The {@code record} method is a
 * convenience to report pre-computed timing and size information; it has a
 * default no-op implementation so callback-only drivers can ignore it.
 *
 * @param <T> Flexible parameter
 */
public interface Callback<T> {

    /**
     * Consume the data.
     *
     * @param data data read from storage client/device.
     */
    void consume(final T data);

    /**
     * Accept the benchmarking data.
     * if your storage driver is not interested in passing the data read;
     * then below method can be overridden.
     *
     * @param startTime Start time
     * @param endTime   End Time.
     * @param dataSize  size of the data in bytes.
     * @param records   number of records/events/messages.
     */
    default void record(long startTime, long endTime, int dataSize, int records) {

    }
}

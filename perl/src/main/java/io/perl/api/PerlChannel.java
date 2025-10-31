/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api;

import io.perl.exception.ExceptionHandler;

/**
 * A dedicated channel used by a single thread to submit benchmarking events to
 * a PerL instance. The channel collects start/end timestamps and the number of
 * records and bytes and forwards them to the PerL internals for aggregation.
 *
 * <p><b>Thread-safety</b>: Instances of {@code PerlChannel} are NOT thread-safe
 * and each worker thread should obtain its own channel via
 * {@link GetPerlChannel#getPerlChannel()}.
 *
 * <p>Implementations may throw/handle exceptions via {@link ExceptionHandler}.
 *
 * Example usage:
 * <pre>
 * {@code
 * PerlChannel channel = perl.getPerlChannel();
 * long start = System.nanoTime();
 * // perform work
 * long end = System.nanoTime();
 * channel.send(start, end, 1, payloadSize);
 * }
 * </pre>
 */
public interface PerlChannel extends ExceptionHandler {

    /**
     * Send benchmarking data for aggregation.
     *
     * @param startTime Start time (caller-defined time unit; typically nanoseconds)
     * @param endTime   End time (caller-defined time unit)
     * @param records   number of records/events/messages represented by this entry
     * @param bytes     size of the data in bytes
     */
    void send(long startTime, long endTime, int records, int bytes);
}

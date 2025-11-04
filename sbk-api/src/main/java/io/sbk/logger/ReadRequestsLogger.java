/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger;

/**
 * Read-side request logging hooks used by SBK loggers.
 *
 * Implementations can optionally aggregate per-reader metrics (counts, bytes,
 * timeouts) to be emitted periodically or exported to external systems.
 */
public interface ReadRequestsLogger {

    /**
     * Record one or more read requests attributed to a reader.
     *
     * @param readerId   logical reader identifier (0..N-1)
     * @param startTime  read start time in the active {@code TimeUnit}
     * @param bytes      bytes read for these events
     * @param events     number of read events
     */
    void recordReadRequests(int readerId, long startTime, long bytes, long events);

    /**
     * Record read timeout events for a reader.
     *
     * @param readerId       logical reader identifier (0..N-1)
     * @param startTime      time when timeouts were observed
     * @param timeoutEvents  number of timeout events
     */
    void recordReadTimeoutEvents(int readerId, long startTime, long timeoutEvents);

    /**
     * Maximum number of reader IDs the logger expects to track.
     * Returning 0 disables per-reader request logging.
     *
     * @return max reader IDs
     */
    int getMaxReaderIDs();
}

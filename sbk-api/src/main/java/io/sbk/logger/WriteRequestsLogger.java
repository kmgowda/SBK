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
 * Write-side request logging hooks used by SBK loggers.
 *
 * Implementations can optionally aggregate per-writer metrics (counts, bytes,
 * timeouts) to be emitted periodically or exported to external systems.
 */
public interface WriteRequestsLogger {

    /**
     * Record one or more write requests attributed to a writer.
     *
     * @param writerId   logical writer identifier (0..N-1)
     * @param startTime  write start time in the active {@code TimeUnit}
     * @param bytes      bytes written for these events
     * @param events     number of write events
     */
    void recordWriteRequests(int writerId, long startTime, long bytes, long events);

    /**
     * Record write timeout events for a writer.
     *
     * @param writerId       logical writer identifier (0..N-1)
     * @param startTime      time when timeouts were observed
     * @param timeoutEvents  number of timeout events
     */
    void recordWriteTimeoutEvents(int writerId, long startTime, long timeoutEvents);

    /**
     * Maximum number of writer IDs the logger expects to track.
     * Returning 0 disables per-writer request logging.
     *
     * @return max writer IDs
     */
    int getMaxWriterIDs();
}
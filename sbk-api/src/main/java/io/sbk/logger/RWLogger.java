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

import io.perl.api.ReportLatency;

/**
 * Interface for recoding/printing results.
 */
public interface RWLogger extends Logger, CountRW, ReportLatency, WriteRequestsLogger, ReadRequestsLogger {

    /**
     * Default method to record latency of every/multiple event(s).
     */
    @Override
    default void recordLatency(long startTime, int bytes, int events, long latency) {

    }

    /**
     * Default method to record every/multiple write event(s).
     */
    @Override
    default void recordWriteRequests(int writerId, long startTime, long bytes, long events) {

    }

    /**
     * Default method to record every/multiple read event(s).
     */
    @Override
    default void recordReadRequests(int readerId, long startTime, long bytes, long events) {

    }

    /**
     * Default method to indicate to record write requests or not.
     */
    @Override
    default int getMaxWriterIDs() {
        return 0;
    }

    /**
     * Default method to indicate to record read requests or not.
     */
    @Override
    default int getMaxReaderIDs() {
        return 0;
    }

}

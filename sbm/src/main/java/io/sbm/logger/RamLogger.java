/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.logger;

import io.perl.api.ReportLatencies;
import io.sbk.logger.Logger;
import io.sbk.logger.ReadRequestsLogger;
import io.sbk.logger.SetRW;
import io.sbk.logger.WriteRequestsLogger;

/**
 * Interface RamLogger.
 */
public interface RamLogger extends Logger, ReportLatencies, SetRW, CountConnections, WriteRequestsLogger,
        ReadRequestsLogger {

    void print(int connections, int maxConnections, int writers, int maxWriters, int readers, int maxReaders,
               long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
               double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
               long readRequestRecords, double readRequestsRecordsPerSec, long writeResponsePendingRecords,
               long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
               long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
               long writeTimeoutEvents, double writeTimeoutEventsPerSec,
               long readTimeoutEvents, double readTimeoutEventsPerSec,
               double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
               double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
               long higherDiscard, long slc1, long slc2, long[] percentileValues);

    void printTotal(int connections, int maxConnections, int writers, int maxWriters, int readers, int maxReaders,
               long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
               double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
               long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
               long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
               long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
               long writeTimeoutEvents, double writeTimeoutEventsPerSec,
               long readTimeoutEvents, double readTimeoutEventsPerSec,
               double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
               double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
               long higherDiscard, long slc1, long slc2, long[] percentileValues);


}

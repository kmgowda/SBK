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

/**
 * Interface used by latency windows to retrieve percentile values from a
 * backing latency store. Implementers provide methods to report aggregated
 * latency records or individual latency counts.
 */
public interface ReportLatencies {

    /**
     * Report an aggregated latency record (bucket summary) to the consumer.
     *
     * @param record Latency Record
     */
    void reportLatencyRecord(LatencyRecord record);


    /**
     * Report a single latency value observed a number of times.
     *
     * @param latency Latency value
     * @param count   Number of times the latency value is observed
     */
    void reportLatency(long latency, long count);
}

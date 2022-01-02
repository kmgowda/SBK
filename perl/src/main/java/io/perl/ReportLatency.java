/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl;

public interface ReportLatency {


    /**
     * Record the latency.
     *
     * @param startTime start time.
     * @param bytes     number of bytes.
     * @param events    number of events(records).
     * @param latency   latency value in milliseconds.
     */
    void recordLatency(long startTime, int bytes, int events, long latency);
}

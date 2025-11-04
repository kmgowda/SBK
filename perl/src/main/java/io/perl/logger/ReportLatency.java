/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.logger;

/**
 * Callback interface used by the PerL framework to report individual latency
 * samples or aggregated latency events. Implementations typically forward
 * these events to recorders or histogram data structures.
 */
public interface ReportLatency {


    /**
     * Record the latency for an event or batch.
     *
     * @param startTime start time.
     * @param events    number of events(records).
     * @param bytes     number of bytes.
     * @param latency   latency value in milliseconds (or caller-defined unit)
     */
    void recordLatency(long startTime, int events, int bytes, long latency);
}

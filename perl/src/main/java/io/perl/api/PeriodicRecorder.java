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
 * Interface for recording latencies.
 */
public interface PeriodicRecorder extends TotalPeriodicWindow {

    /**
     * Record the Event/record.
     *
     * @param startTime start time
     * @param endTime   end time
     * @param events    number of events (records)
     * @param bytes     number of bytes
     */
    void record(long startTime, long endTime, int events, int bytes);

}

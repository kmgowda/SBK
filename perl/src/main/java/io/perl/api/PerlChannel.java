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
 * Interface for recording benchmarking data.
 */
public interface PerlChannel extends ExceptionHandler {

    /**
     * send the benchmarking data.
     *
     * @param startTime Start time
     * @param endTime   End Time.
     * @param dataSize  size of the data in bytes.
     * @param records   number of records/events/messages.
     */
    void send(long startTime, long endTime, int dataSize, int records);
}

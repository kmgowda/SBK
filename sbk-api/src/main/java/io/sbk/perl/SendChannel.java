/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl;

/**
 * Interface for recording benchmarking data.
 */
public sealed interface SendChannel permits Channel {
    /**
     * send the benchmarking data.
     *
     * @param id        Channel identifier
     * @param startTime Start time
     * @param endTime   End Time.
     * @param dataSize  size of the data in bytes.
     * @param records   number of records/events/messages.
     */
    void send(int id, long startTime, long endTime, int dataSize, int records);

    /**
     * send the Exception.
     *
     * @param id Channel identifier
     * @param ex Exception
     */
    void sendException(int id, Throwable ex);
}

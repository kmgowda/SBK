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

public non-sealed interface Channel extends GetPerlChannel {

    /**
     * Receive the benchmarking timestamp.
     *
     * @param timeout maximum time in milliseconds to wait for the data.
     * @return TimeStamp    Time stamp data
     * If there is no data, the this method will return null data after timeout
     * or it can return before the timeout with null value.
     * return TimeStamp  Benchmarking Data. return null if there is no data.
     */
    TimeStamp receive(int timeout);

    /**
     * Send end time indication.
     *
     * @param endTime End Time.
     */
    void sendEndTime(long endTime);

    /**
     * Clear the channel.
     */
    void clear();
}

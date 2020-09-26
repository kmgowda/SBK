/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api;

public interface Channel extends SendChannel {

    /**
     * Receive the benchmarking timestamp.
     * return TimeStamp  Benchmarking Data.
     */
    TimeStamp receive();

    /**
     * Send end time indication.
     * @param  endTime End Time.
     */
    void sendEndTime(long endTime);

    /**
     * Clear the channel.
     */
    void clear();
}

/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

public interface AsyncReader<T> {

    /**
     * set the callback for AsyncReader.
     * @param callback reader callback .
     */
    void setCallback(ReaderCallback callback);

    /**
     * start the AsyncReader.
     * @param startTime startTime.
     */
    void start(long startTime);
}

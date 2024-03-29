/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger;

public interface ReadRequestsLogger {

    void recordReadRequests(int readerId, long startTime, long bytes, long events);

    void recordReadTimeoutEvents(int readerId, long startTime, long timeoutEvents);

    int getMaxReaderIDs();
}

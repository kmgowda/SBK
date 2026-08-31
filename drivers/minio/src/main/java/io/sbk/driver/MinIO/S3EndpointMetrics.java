/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.MinIO;

import java.util.concurrent.atomic.LongAdder;

/** Optional per-endpoint operation attribution aggregated outside reporting windows. */
final class S3EndpointMetrics {
    private final String endpoint;
    private final LongAdder operations = new LongAdder();
    private final LongAdder bytes = new LongAdder();
    private final LongAdder retries = new LongAdder();
    private final LongAdder failures = new LongAdder();

    S3EndpointMetrics(String endpoint) {
        this.endpoint = endpoint;
    }

    void success(long transferredBytes) {
        operations.increment();
        bytes.add(transferredBytes);
    }

    void retry() {
        retries.increment();
    }

    void failure() {
        failures.increment();
    }

    String summary() {
        return "endpoint=" + endpoint + ", operations=" + operations.sum()
                + ", bytes=" + bytes.sum() + ", retries=" + retries.sum()
                + ", failures=" + failures.sum();
    }
}

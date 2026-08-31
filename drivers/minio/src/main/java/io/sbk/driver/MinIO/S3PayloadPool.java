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

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Reuses async request payloads after their SDK futures complete.
 *
 * <p>Capacity is bounded by the owning worker's already-acquired async slots;
 * the pool itself never creates speculative buffers.
 */
final class S3PayloadPool {
    private final ConcurrentLinkedQueue<byte[]> available = new ConcurrentLinkedQueue<>();

    byte[] acquire(int size, S3DataGenerator generator) {
        byte[] payload = null;
        int retained = available.size();
        for (int index = 0; index < retained; index++) {
            byte[] candidate = available.poll();
            if (candidate == null) {
                break;
            }
            if (candidate.length == size) {
                payload = candidate;
                break;
            }
            available.offer(candidate);
        }
        if (payload == null) {
            available.poll();
            payload = new byte[size];
        }
        generator.newObject();
        generator.fill(payload, 0, payload.length);
        return payload;
    }

    void release(byte[] payload) {
        if (payload != null) {
            available.offer(payload);
        }
    }

    int retainedCount() {
        return available.size();
    }
}

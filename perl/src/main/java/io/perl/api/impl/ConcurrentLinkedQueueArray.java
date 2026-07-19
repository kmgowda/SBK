/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api.impl;

import io.perl.api.QueueArray;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Array of concurrent linked queues used to isolate producer channels.
 *
 * @param <T> queued element type
 */
@SuppressWarnings("unchecked")
public class ConcurrentLinkedQueueArray<T> implements QueueArray<T> {
    final private ConcurrentLinkedQueue<T>[] cQueues;

    /**
     * Creates the requested number of queues.
     *
     * @param size number of queues
     */
    public ConcurrentLinkedQueueArray(int size) {
        this.cQueues = new ConcurrentLinkedQueue[size];
        for (int i = 0; i < cQueues.length; i++) {
            cQueues[i] = new ConcurrentLinkedQueue<>();
        }
    }

    @Override
    final public T poll(int index) {
        return this.cQueues[index].poll();
    }

    @Override
    final public boolean add(int index, T data) {
        return this.cQueues[index].add(data);
    }

    @Override
    final public void clear(int index) {
        this.cQueues[index].clear();
    }

    @Override
    final public void clear() {
        for (ConcurrentLinkedQueue<T> q : cQueues) {
            q.clear();
        }
    }
}

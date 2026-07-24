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

/**
 * Array of MPSC queues backed by {@link CQueue}.
 *
 * <p>This implementation is retained for controlled performance experiments.
 * Production PerL uses {@link ConcurrentLinkedQueueArray}: although both
 * implementations allocate one node per enqueue and both recover from stale
 * retired heads, the JDK queue also covers multiple consumers, iterators, and
 * interior dead-node removal. {@code CQueue} instead amortizes self-linking over
 * {@value CQueue#RETIRE_BATCH_SIZE} dequeues for its narrower MPSC contract.
 * Each index in this array must still have exactly one consumer.</p>
 *
 * @param <T> queued element type
 */
@SuppressWarnings("unchecked")
public class CQueueArray<T> implements QueueArray<T> {
    final private CQueue<T>[] cQueues;

    /**
     * Creates the requested number of queues.
     *
     * @param size number of queues
     */
    public CQueueArray(int size) {
        this.cQueues = new CQueue[size];
        for (int i = 0; i < cQueues.length; i++) {
            cQueues[i] = new CQueue<>();
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
        for (CQueue<T> q : cQueues) {
            q.clear();
        }
    }

}

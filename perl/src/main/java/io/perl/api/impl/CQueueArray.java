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

/*
 * Concurrent Queue Array based on CQueue Implementation.
 * DON'T USE THIS CLASS.
 * Use ConcurrentLinkedQueueArray, because the ConcurrentLinkedQueue does better Garbage collection.
 */
@SuppressWarnings("unchecked")
public class CQueueArray<T> implements QueueArray<T> {
    final private CQueue<T>[] cQueues;

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

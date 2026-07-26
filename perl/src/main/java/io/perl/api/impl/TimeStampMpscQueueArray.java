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
import io.perl.api.TimeStamp;
import io.perl.api.TimeStampNode;

/**
 * Array of intrusive MPSC queues used by optimized PerL channels.
 *
 * <p>Every element supplied to {@link #add(int, TimeStamp)} must be a
 * single-use {@link TimeStampNode}. Each indexed queue accepts multiple
 * producers and has exactly one consumer.</p>
 */
public class TimeStampMpscQueueArray implements QueueArray<TimeStamp> {
    private final TimeStampMpscQueue[] queues;

    /**
     * Creates the requested number of timestamp queues.
     *
     * @param size number of queues
     */
    public TimeStampMpscQueueArray(int size) {
        this.queues = new TimeStampMpscQueue[size];
        for (int index = 0; index < queues.length; index++) {
            queues[index] = new TimeStampMpscQueue();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param index index of the intrusive queue to poll
     * @return the next timestamp node, or {@code null} when the queue is empty
     */
    @Override
    public TimeStamp poll(int index) {
        return queues[index].poll();
    }

    /**
     * Adds a timestamp node to an indexed intrusive queue.
     *
     * @param index index of the queue receiving the node
     * @param data single-use {@link TimeStampNode} to enqueue
     * @return {@code true} after the node is linked
     * @throws IllegalArgumentException if {@code data} is not a
     *         {@link TimeStampNode}
     */
    @Override
    public boolean add(int index, TimeStamp data) {
        if (!(data instanceof TimeStampNode node)) {
            throw new IllegalArgumentException(
                    "TimeStampMpscQueueArray accepts only TimeStampNode");
        }
        return queues[index].add(node);
    }

    /**
     * {@inheritDoc}
     *
     * @param index index of the queue to drain
     */
    @Override
    public void clear(int index) {
        queues[index].clear();
    }

    /**
     * Drains every intrusive queue in this array.
     */
    @Override
    public void clear() {
        for (TimeStampMpscQueue queue : queues) {
            queue.clear();
        }
    }
}

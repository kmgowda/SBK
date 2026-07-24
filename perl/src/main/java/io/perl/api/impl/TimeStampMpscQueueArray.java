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

/**
 * Array of intrusive MPSC queues used by optimized PerL channels.
 *
 * <p>Every element supplied to {@link #add(int, TimeStamp)} must be a
 * single-use {@link TimeStampNode}. Each indexed queue accepts multiple
 * producers and has exactly one consumer.</p>
 */
public final class TimeStampMpscQueueArray implements QueueArray<TimeStamp> {
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

    @Override
    public TimeStamp poll(int index) {
        return queues[index].poll();
    }

    @Override
    public boolean add(int index, TimeStamp data) {
        if (!(data instanceof TimeStampNode node)) {
            throw new IllegalArgumentException(
                    "TimeStampMpscQueueArray accepts only TimeStampNode");
        }
        return queues[index].add(node);
    }

    @Override
    public void clear(int index) {
        queues[index].clear();
    }

    @Override
    public void clear() {
        for (TimeStampMpscQueue queue : queues) {
            queue.clear();
        }
    }
}

/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api;

/**
 * A minimal queue abstraction used by PerL to decouple producers and consumers.
 * Implementations may be backed by any concurrent queue or custom ring-buffer.
 *
 * <p>Contract:
 * <ul>
 *     <li>{@link #poll()} should return the next element or {@code null} if
 *     none is available at the time of the call.</li>
 *     <li>{@link #add(Object)} should enqueue an element and return {@code true}
 *     on success.</li>
 *     <li>{@link #clear()} should remove any pending elements and reset state.</li>
 * </ul>
 *
 * <p>Thread-safety is implementation-specific; callers should prefer
 * implementations that match their concurrency requirements.</p>
 */
public interface Queue<T> {
    /**
     * Return data of type T from queue, or {@code null} if none is available.
     */
    T poll();

    /**
     * Add data of type T to queue.
     *
     * @param data element to add to the queue
     * @return true if the element was added successfully
     */
    boolean add(T data);

    /**
     * Clear queue and reset internal state.
     */
    void clear();

}

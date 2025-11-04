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
 * Represents an array of {@link Queue} instances. This abstraction is used when
 * the PerL framework allocates one queue per worker thread or IO channel.
 */
public interface QueueArray<T> {

    /**
     * Poll an element from the queue at the given index, or {@code null} if none.
     *
     * @param index index of the queue to poll
     * @return element from the queue or {@code null}
     */
    T poll(int index);

    /**
     * Add an element to the queue identified by index.
     *
     * @param index index of the queue to add the element to
     * @param data  element to add
     * @return true if added successfully
     */
    boolean add(int index, T data);

    /**
     * Clear the queue at the specified index.
     *
     * @param index index of the queue to clear
     */
    void clear(int index);

    /**
     * Clear all queues in the array.
     */
    void clear();
}

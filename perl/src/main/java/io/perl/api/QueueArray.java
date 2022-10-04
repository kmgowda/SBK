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

/*
 * Array of queues
 */
public interface QueueArray<T> {

    /*
     * Return data of type T from queue at 'index', if it's available. Return Null otherwise.
     */
    T poll(int index);

    /*
     * Add data of type T to queue at 'index'.
     */
    boolean add(int index, T data);

    /*
     * Clear queue at 'index'.
     */
    void clear(int index);

    /*
     * Clear all queues.
     */
    void clear();
}

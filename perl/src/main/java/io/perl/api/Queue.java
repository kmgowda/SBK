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

public interface Queue<T> {
    /*
     * Return data of type T from queue , if it's available. Return Null otherwise.
     */
    T poll();

    /*
     * Add data of type T to queue.
     */
    boolean add( T data);

    /*
     * Clear queue.
     */
    void clear();

}

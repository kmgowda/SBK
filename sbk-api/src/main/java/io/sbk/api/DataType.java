/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api;

/**
 * Interface for Data Type operations.
 */
public interface DataType<T> {

    /**
     * allocate the data.
     * @param size size (number of bytes) of the data to create.
     * @return T return the data.
     */
    T allocate(int size);

    /**
     * Create the data with payload content.
     * @param size size (number of bytes) of the data to create.
     * @return T return the data.
     */
    T create(int size);

    /**
     * Get the size of the given data in terms of number of bytes.
     * @param  data data
     * @return return size of the data.
     */
    int length(T data);

    /**
     * Set the time for data.
     * @param  data data
     * @param  time time to set
     * @return T return the data.
     */
    T setTime(T data, long time);

    /**
     * Get the time of data.
     * @param  data data
     * @return long return the time set by last {@link DataType#setTime(Object, long)}}.
     */
    long getTime(T data);
}

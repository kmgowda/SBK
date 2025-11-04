/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger;

/**
 * Interface SetRW.
 *
 * Contract for loggers that track and update the number of active and maximum
 * writers/readers in a benchmark run.
 */
public interface SetRW {


    /**
     * Set current number of active writers.
     *
     * @param val new value.
     */
    void setWriters(int val);


    /**
     * Update the max number of writers seen so far.
     *
     * @param val new value.
     */
    void setMaxWriters(int val);

    /**
     * Set current number of active readers.
     *
     * @param val new value.
     */
    void setReaders(int val);

    /**
     * Update the max number of readers seen so far.
     *
     * @param val new value.
     */
    void setMaxReaders(int val);

}

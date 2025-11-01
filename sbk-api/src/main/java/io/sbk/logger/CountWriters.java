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
 * Interface for tracking the count of active writers in the benchmarking system.
 * This sealed interface is part of the writer tracking mechanism in SBK.
 * It provides methods to increment and decrement the count of active writers
 * during benchmark operations.
 * 
 * <p>This interface is sealed and only permits implementation by CountRW.</p>
 */
public sealed interface CountWriters permits CountRW {

    /**
     * Increments the count of active writers.
     * This method should be called when a new writer becomes active.
     */
    void incrementWriters();

    /**
     * Decrements the count of active writers.
     * This method should be called when a writer completes its operation or becomes inactive.
     */
    void decrementWriters();

}

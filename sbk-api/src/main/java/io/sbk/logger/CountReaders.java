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
 * Interface for tracking the count of active readers in the benchmarking system.
 * This sealed interface is part of the reader tracking mechanism in SBK.
 * It provides methods to increment and decrement the count of active readers
 * during benchmark operations.
 * 
 * <p>This interface is sealed and only permits implementation by CountRW.</p>
 */
public sealed interface CountReaders permits CountRW {

    /**
     * Increments the count of active readers.
     * This method should be called when a new reader becomes active.
     */
    void incrementReaders();

    /**
     * Decrements the count of active readers.
     * This method should be called when a reader completes its operation or becomes inactive.
     */
    void decrementReaders();
}

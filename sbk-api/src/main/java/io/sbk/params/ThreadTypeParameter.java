/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.params;

import io.sbk.thread.ThreadType;

/**
 * Interface for parameters that provide thread type configuration.
 * This interface allows access to the thread type setting used for
 * executing benchmark operations, which can be platform threads,
 * fork-join threads, or virtual threads.
 */
public interface ThreadTypeParameter {

    /**
     * Gets the thread type to be used for benchmark execution.
     *
     * @return the ThreadType enum value indicating the type of threads to use
     */
    ThreadType getThreadType();
}

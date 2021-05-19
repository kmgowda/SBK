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

public interface RWCount {

    /**
     * Increment Writers.
     * @param val increment value.
     */
    void incrementWriters(int val);

    /**
     * Decrement Writers.
     * @param val decrement value.
     */
    void decrementWriters(int val);

    /**
     * Increment Readers.
     * @param val increment value.
     */
    void incrementReaders(int val);

    /**
     * Decrement Readers.
     * @param val decrement value.
     */
    void decrementReaders(int val);
}

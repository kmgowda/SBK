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

import java.io.IOException;

/**
 * Interface for Readers.
 */
public interface Reader {
    /**
     * read the data.
     * @return byte[] return the data.
     * @throws IOException If an exception occurred.
     */
    public abstract byte[] read() throws IOException;

    /**
     * close the consumer/reader.
     * @throws IOException If an exception occurred.
     */
    public abstract void close() throws IOException;
}

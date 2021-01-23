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
 * Interface for IO.
 */
public interface IO {

    /**
     * Open the IOr.
     *
     * @throws IOException If an exception occurred.
     */
    void open() throws IOException;

    /**
     * close the IO.
     *
     * @throws IOException If an exception occurred.
     */
    void close() throws IOException;
}
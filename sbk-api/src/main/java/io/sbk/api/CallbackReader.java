/**
 * Copyright (c) KMG. All Rights Reserved..
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
 * Call back Reader Interface.
 * Deprecated
 * This interface is replaced by Abstract class AbstractCallbackReader.
 */
public interface CallbackReader<T> {

    /**
     * set the callback and start the AsyncReader.
     * @param callback Reader callback.
     * @throws IOException If an exception occurred.
     */
    void start(Callback<T> callback) throws IOException;

    /**
     * Close the AsyncReader.
     * @throws IOException If an exception occurred.
     */
    void close() throws IOException;
}

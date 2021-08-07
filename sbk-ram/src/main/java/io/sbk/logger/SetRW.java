/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger;

public interface SetRW {


    /**
     * Set Writers.
     * @param val new value.
     */
    void setWriters(int val);


    /**
     * Set Max Writers.
     * @param val new value.
     */
    void setMaxWriters(int val);

    /**
     * Set Readers.
     * @param val new value.
     */
    void setReaders(int val);

    /**
     * Set Max Readers.
     * @param val new value.
     */
    void setMaxReaders(int val);

}

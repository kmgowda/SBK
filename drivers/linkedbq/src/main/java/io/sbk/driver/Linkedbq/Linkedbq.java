/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Linkedbq;


import io.sbk.driver.ConcurrentQ.ConcurrentQ;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;

import java.io.IOException;

/**
 * Class for Linkedbq storage driver.
 *
 * Incase if your data type in other than byte[] (Byte Array)
 * then change the datatype and getDataType.
 */
public class Linkedbq extends ConcurrentQ implements Storage<byte[]> {

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        this.queue = new LinkedBQueue<>();
    }

}

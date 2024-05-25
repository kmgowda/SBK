/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Atomicq;


import io.perl.api.impl.AtomicQueue;
import io.sbk.driver.ConcurrentQ.ConcurrentQ;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;


import java.io.IOException;


/**
 * Class for Atomicq storage driver.
 *
 * Incase if your data type in other than byte[] (Byte Array)
 * then change the datatype and getDataType.
 */
public class Atomicq extends ConcurrentQ implements Storage<byte[]> {

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        this.queue = new AtomicQueue<>();
    }

}

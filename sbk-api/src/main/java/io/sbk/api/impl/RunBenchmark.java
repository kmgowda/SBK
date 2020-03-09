/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import java.io.IOException;

/**
 * Interface for executing writers/readers benchmarks.
 */
public interface RunBenchmark {

     /**
      * Run writers/readers benchmarks.
      *
      * @throws IOException If an exception occurred.
      * @throws InterruptedException If an exception occurred.
      */
     void run() throws InterruptedException, IOException;
}

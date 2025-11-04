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
 * Combined interface for tracking both readers and writers in the benchmarking system.
 * This interface extends both CountReaders and CountWriters interfaces to provide
 * a unified way to track both types of operations.
 * 
 * <p>This interface is sealed and only permits implementation by RWLogger.</p>
 */
public sealed interface CountRW extends CountReaders, CountWriters permits RWLogger {

}

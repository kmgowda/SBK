/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.thread;

/**
 * Thread types available in Java 25 for SBK benchmarking.
 * 
 * @since Java 25
 */
public enum ThreadType {
    /**
     * Platform threads - Traditional OS threads for CPU-intensive workloads.
     * Memory: ~1MB per thread, Max: ~1000 concurrent.
     */
    Platform,
    
    /**
     * ForkJoin threads - Work-stealing threads for parallel algorithms.
     * Memory: ~1MB per thread, Max: ~1000 concurrent.
     */
    ForkJoin,
    
    /**
     * Virtual threads - Lightweight threads for I/O-bound operations.
     * Memory: ~1KB per thread, Max: 1M+ concurrent.
     */
    Virtual
}

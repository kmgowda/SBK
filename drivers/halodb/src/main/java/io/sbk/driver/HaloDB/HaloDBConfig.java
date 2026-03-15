/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.HaloDB;

public class HaloDBConfig {
    public String storageDirectory = "/tmp/halodb";
    public long maxFileSize = 1024 * 1024 * 1024L;
    public long maxTombstoneFileSize = 64 * 1024 * 1024L;
    public int buildIndexThreads = 8;
    public long flushDataSizeBytes = 10 * 1024 * 1024L;
    public double compactionThresholdPerFile = 0.7;
    public long compactionJobRate = 50 * 1024 * 1024L;
    public long numberOfRecords = 100_000_000L;
    public boolean cleanUpTombstonesDuringOpen = true;
    public boolean cleanUpInMemoryIndexOnClose = false;
    public boolean useMemoryPool = true;
    public long memoryPoolChunkSize = 2 * 1024 * 1024L;
    public int fixedKeySize = 8;
}
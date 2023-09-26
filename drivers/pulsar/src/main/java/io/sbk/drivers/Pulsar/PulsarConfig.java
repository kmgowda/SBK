/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.drivers.Pulsar;

public class PulsarConfig {
    public String topicName;
    public String brokerUri;
    public String adminUri;
    public String nameSpace;
    public String cluster;
    public String tenant;
    public int partitions;
    public int ioThreads;
    public int ensembleSize;
    public int writeQuorum;
    public int ackQuorum;
    public boolean deduplicationEnabled;
}

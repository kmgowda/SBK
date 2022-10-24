/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Kafka;

public class KafkaConfig {
    public String brokerUri;
    public String topicName;
    public int pollTimeoutMS;
    public int partitions;
    public short replica;
    public short sync;
    public boolean create;
    public boolean idempotence;
    public int lingerMS;
    public int batchSize;
    public boolean autoCommit;
    public int maxPartitionFetchBytes;

}

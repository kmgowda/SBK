/**
 * Copyright (c) 2017 Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perf.drivers.kafka;
import io.perf.core.ReaderWorker;
import io.perf.core.PerfStats;

import java.util.Properties;
import java.util.Arrays;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Class for Kafka reader/consumer.
 */
public class KafkaReaderWorker extends ReaderWorker {
    final private KafkaConsumer<byte[], byte[]> consumer;

    public KafkaReaderWorker(int readerId, int events, int secondsToRun,
                      long start, PerfStats stats, String partition,
                      int timeout, boolean writeAndRead, Properties consumerProps) {
        super(readerId, events, secondsToRun, start, stats, partition, timeout, writeAndRead);

        this.consumer = new KafkaConsumer<>(consumerProps);
        this.consumer.subscribe(Arrays.asList(partition));
    }

    @Override
    public byte[] readData() {
        final ConsumerRecords<byte[], byte[]> records = consumer.poll(timeout);
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }

    @Override
    public void close() {
        consumer.close();
    }
}
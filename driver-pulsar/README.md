<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Apache Pulsar Benchmarking with SBK 
The Apache pulsar driver for SBK supports single / multiple Readers and Writers performance benchmarking.
SBK supports the End to End latency for benchmarking too.

## Apache pulsar Docker and SBK
Refer to this page : https://hub.docker.com/r/apachepulsar/pulsar for Apache pulsar docker images and steps to roll out the cluster.

Just to make it simple, just the below command to start the standalone pulsar broker.
```
docker run -it -p 6650:6650 -p 8080:8080  apachepulsar/pulsar:latest bin/pulsar standalone
```

An Example SBK command for single pulsar writer  is as follows

```
./build/install/sbk/bin/sbk -class Pulsar -broker tcp://localhost:6650 -topic topic-k-10  -partitions 10   -writers 1  -size 100  -time 60 -context 8085
```
In the above example, Note that Prometheus context port is 8085 (not the default 8080) because the default port is used by pulsar standalone admin .
The output is as follows. 
```
kmg@W10GRDN6H2:~/kmg-linux/SBK$ ./build/install/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-10  -partitions 10   -writers 1  -size 100  -time 60 -context 8085
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-07-23 11:24:16 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-07-23 11:24:16 INFO SBK version: 0.81
2020-07-23 11:24:16 INFO Argument List: [-class, Pulsar, -admin, http://localhost:8080, -broker, tcp://localhost:6650, -topic, topic-k-10, -partitions, 10, -writers, 1, -size, 100, -time, 60, -context, 8085]
2020-07-23 11:24:17 INFO Reflections took 122 ms to scan 23 urls, producing 31 keys and 123 values
2020-07-23 11:24:19 WARN [http://localhost:8080/admin/v2/persistent/public/default/topic-k-10/partitions?force=false] Failed to perform http delete request: org.apache.pulsar.shade.javax.ws.rs.NotFoundException: HTTP 404 Not Found
2020-07-23 11:24:19 INFO [[id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]] Connected to server
2020-07-23 11:24:19 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:20 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:20 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:20 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:20 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:20 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:20 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:21 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:21 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:21 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:21 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:21 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:21 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:22 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:22 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:22 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:22 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:22 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:22 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-10",
  "producerName" : null,
  "sendTimeoutMs" : 30000,
  "blockIfQueueFull" : true,
  "maxPendingMessages" : 1000,
  "maxPendingMessagesAcrossPartitions" : 50000,
  "messageRoutingMode" : "RoundRobinPartition",
  "hashingScheme" : "JavaStringHash",
  "cryptoFailureAction" : "FAIL",
  "batchingMaxPublishDelayMicros" : 1000,
  "batchingMaxMessages" : 1000,
  "batchingEnabled" : true,
  "compressionType" : "NONE",
  "initialSequenceId" : null,
  "autoUpdatePartitions" : true,
  "properties" : { }
}
2020-07-23 11:24:22 INFO Pulsar client config: {
  "serviceUrl" : "tcp://localhost:6650",
  "authPluginClassName" : null,
  "authParams" : null,
  "operationTimeoutMs" : 30000,
  "statsIntervalSeconds" : 60,
  "numIoThreads" : 1,
  "numListenerThreads" : 1,
  "connectionsPerBroker" : 1,
  "useTcpNoDelay" : true,
  "useTls" : false,
  "tlsTrustCertsFilePath" : "",
  "tlsAllowInsecureConnection" : false,
  "tlsHostnameVerificationEnable" : false,
  "concurrentLookupRequest" : 5000,
  "maxLookupRequest" : 50000,
  "maxNumberOfRejectedRequestPerConnection" : 50,
  "keepAliveIntervalSeconds" : 30,
  "connectionTimeoutMs" : 10000,
  "requestTimeoutMs" : 60000,
  "initialBackoffIntervalNanos" : 100000000,
  "maxBackoffIntervalNanos" : 60000000000
}
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-0] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-1] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-2] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-3] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-4] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-5] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-6] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-7] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-8] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-9] [null] Creating producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-0] [standalone-0-0] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-1] [standalone-0-1] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-3] [standalone-0-3] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-2] [standalone-0-2] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-4] [standalone-0-4] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-5] [standalone-0-5] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-7] [standalone-0-7] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-6] [standalone-0-6] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-8] [standalone-0-8] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [persistent://public/default/topic-k-10-partition-9] [standalone-0-9] Created producer on cnx [id: 0xaf759f00, L:/127.0.0.1:52116 - R:localhost/127.0.0.1:6650]
2020-07-23 11:24:23 INFO [topic-k-10] Created partitioned producer
2020-07-23 11:24:23 INFO SSE4.2 CRC32C provider initialized
Writing     369218 records,   73784.6 records/sec,     7.04 MB/sec,     79.5 ms avg latency,     503 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      21 ms 10th,      30 ms 25th,      60 ms 50th,     107 ms 75th,     188 ms 95th,     457 ms 99th,     496 ms 99.9th,     498 ms 99.99th.
Writing     564620 records,  112788.7 records/sec,    10.76 MB/sec,     32.2 ms avg latency,     104 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      12 ms 10th,      17 ms 25th,      27 ms 50th,      43 ms 75th,      72 ms 95th,      81 ms 99th,      89 ms 99.9th,     103 ms 99.99th.
Writing     579801 records,  115937.0 records/sec,    11.06 MB/sec,     28.7 ms avg latency,      99 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      10 ms 10th,      14 ms 25th,      24 ms 50th,      37 ms 75th,      73 ms 95th,      87 ms 99th,      93 ms 99.9th,      99 ms 99.99th.
Writing     560707 records,  112119.0 records/sec,    10.69 MB/sec,     27.7 ms avg latency,     111 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      10 ms 10th,      14 ms 25th,      22 ms 50th,      35 ms 75th,      67 ms 95th,      97 ms 99th,     105 ms 99.9th,     110 ms 99.99th.
Writing     583656 records,  116707.9 records/sec,    11.13 MB/sec,     18.6 ms avg latency,      95 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       8 ms 10th,      11 ms 25th,      16 ms 50th,      25 ms 75th,      38 ms 95th,      49 ms 99th,      76 ms 99.9th,      95 ms 99.99th.
Writing     578439 records,  115664.7 records/sec,    11.03 MB/sec,     17.1 ms avg latency,      79 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       8 ms 10th,      10 ms 25th,      13 ms 50th,      21 ms 75th,      45 ms 95th,      55 ms 99th,      66 ms 99.9th,      79 ms 99.99th.
Writing     583100 records,  116596.7 records/sec,    11.12 MB/sec,     14.9 ms avg latency,      77 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       7 ms 10th,       9 ms 25th,      12 ms 50th,      17 ms 75th,      36 ms 95th,      51 ms 99th,      70 ms 99.9th,      76 ms 99.99th.
Writing     575301 records,  115037.2 records/sec,    10.97 MB/sec,     13.2 ms avg latency,      65 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       6 ms 10th,       8 ms 25th,      10 ms 50th,      16 ms 75th,      29 ms 95th,      43 ms 99th,      57 ms 99.9th,      64 ms 99.99th.
Writing     523027 records,  104584.5 records/sec,     9.97 MB/sec,     33.3 ms avg latency,     195 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      12 ms 10th,      17 ms 25th,      26 ms 50th,      42 ms 75th,      85 ms 95th,     124 ms 99th,     166 ms 99.9th,     195 ms 99.99th.
Writing     425499 records,   85082.8 records/sec,     8.11 MB/sec,     58.6 ms avg latency,     669 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      10 ms 10th,      18 ms 25th,      39 ms 50th,      64 ms 75th,     117 ms 95th,     611 ms 99th,     656 ms 99.9th,     668 ms 99.99th.
2020-07-23 11:25:20 INFO [persistent://public/default/topic-k-10-partition-0] [standalone-0-0] Pending messages: 0 --- Publish throughput: 9253.61 msg/s --- 7.06 Mbit/s --- Latency: med: 19.388 ms - 95pct: 80.283 ms - 99pct: 189.930 ms - 99.9pct: 605.765 ms - max: 654.066 ms --- Ack received rate: 9253.61 ack/s --- Failed messages: 0
2020-07-23 11:25:20 INFO [persistent://public/default/topic-k-10-partition-1] [standalone-0-1] Pending messages: 0 --- Publish throughput: 9285.30 msg/s --- 7.08 Mbit/s --- Latency: med: 19.243 ms - 95pct: 79.590 ms - 99pct: 174.734 ms - 99.9pct: 637.426 ms - max: 666.470 ms --- Ack received rate: 9285.30 ack/s --- Failed messages: 0
2020-07-23 11:25:20 INFO [persistent://public/default/topic-k-10-partition-2] [standalone-0-2] Pending messages: 0 --- Publish throughput: 9409.67 msg/s --- 7.18 Mbit/s --- Latency: med: 19.985 ms - 95pct: 80.055 ms - 99pct: 181.098 ms - 99.9pct: 645.829 ms - max: 655.667 ms --- Ack received rate: 9409.67 ack/s --- Failed messages: 0
2020-07-23 11:25:21 INFO [persistent://public/default/topic-k-10-partition-3] [standalone-0-3] Pending messages: 0 --- Publish throughput: 9241.22 msg/s --- 7.05 Mbit/s --- Latency: med: 18.362 ms - 95pct: 75.584 ms - 99pct: 139.005 ms - 99.9pct: 295.813 ms - max: 648.538 ms --- Ack received rate: 9241.22 ack/s --- Failed messages: 0
2020-07-23 11:25:21 INFO [persistent://public/default/topic-k-10-partition-4] [standalone-0-4] Pending messages: 0 --- Publish throughput: 9247.92 msg/s --- 7.06 Mbit/s --- Latency: med: 18.909 ms - 95pct: 78.579 ms - 99pct: 161.524 ms - 99.9pct: 493.930 ms - max: 668.672 ms --- Ack received rate: 9247.92 ack/s --- Failed messages: 0
2020-07-23 11:25:21 INFO [persistent://public/default/topic-k-10-partition-5] [standalone-0-5] Pending messages: 0 --- Publish throughput: 9417.03 msg/s --- 7.18 Mbit/s --- Latency: med: 19.934 ms - 95pct: 80.675 ms - 99pct: 172.244 ms - 99.9pct: 583.475 ms - max: 647.056 ms --- Ack received rate: 9417.03 ack/s --- Failed messages: 0
2020-07-23 11:25:22 INFO [persistent://public/default/topic-k-10-partition-6] [standalone-0-6] Pending messages: 0 --- Publish throughput: 9470.01 msg/s --- 7.23 Mbit/s --- Latency: med: 19.937 ms - 95pct: 83.882 ms - 99pct: 180.091 ms - 99.9pct: 637.648 ms - max: 657.969 ms --- Ack received rate: 9470.01 ack/s --- Failed messages: 0
2020-07-23 11:25:22 INFO [persistent://public/default/topic-k-10-partition-7] [standalone-0-7] Pending messages: 0 --- Publish throughput: 9352.58 msg/s --- 7.14 Mbit/s --- Latency: med: 19.375 ms - 95pct: 80.386 ms - 99pct: 159.996 ms - 99.9pct: 467.303 ms - max: 654.489 ms --- Ack received rate: 9352.58 ack/s --- Failed messages: 0
Writing     252828 records,   27478.3 records/sec,     2.62 MB/sec,     11.6 ms avg latency,      48 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       6 ms 10th,       7 ms 25th,       9 ms 50th,      14 ms 75th,      27 ms 95th,      34 ms 99th,      43 ms 99.9th,      48 ms 99.99th.
2020-07-23 11:25:22 INFO [persistent://public/default/topic-k-10-partition-8] [standalone-0-8] Pending messages: 0 --- Publish throughput: 9364.33 msg/s --- 7.14 Mbit/s --- Latency: med: 19.265 ms - 95pct: 78.531 ms - 99pct: 132.946 ms - 99.9pct: 254.799 ms - max: 649.766 ms --- Ack received rate: 9364.33 ack/s --- Failed messages: 0
2020-07-23 11:25:23 INFO [persistent://public/default/topic-k-10-partition-9] [standalone-0-9] Pending messages: 0 --- Publish throughput: 9225.88 msg/s --- 7.04 Mbit/s --- Latency: med: 19.277 ms - 95pct: 78.015 ms - 99pct: 161.525 ms - 99.9pct: 466.467 ms - max: 653.508 ms --- Ack received rate: 9225.88 ack/s --- Failed messages: 0
Writing(Total)    5596196 records,   91682.3 records/sec,     8.74 MB/sec,     29.0 ms avg latency,     669 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       8 ms 10th,      11 ms 25th,      19 ms 50th,      34 ms 75th,      79 ms 95th,     156 ms 99th,     600 ms 99.9th,     655 ms 99.99th.
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-0] [standalone-0-0] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-1] [standalone-0-1] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-2] [standalone-0-2] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-3] [standalone-0-3] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-4] [standalone-0-4] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-5] [standalone-0-5] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-6] [standalone-0-6] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-7] [standalone-0-7] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-8] [standalone-0-8] Closed Producer
2020-07-23 11:25:24 INFO [persistent://public/default/topic-k-10-partition-9] [standalone-0-9] Closed Producer
2020-07-23 11:25:24 INFO [topic-k-10] Closed Partitioned Producer
```

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
./build/install/sbk/bin/sbk -class Pulsar -broker tcp://localhost:6650 -topic topic-k-1  -partitions 1   -writers 1  -size 100  -seconds 60
```

The output is as follows. 
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class Pulsar -broker tcp://localhost:6650 -topic topic-k-1  -partitions 1   -writers 1  -size 100  -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-02-23 12:12:55 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-02-23 12:12:55 INFO Java Runtime Version: 11.0.8+11
2021-02-23 12:12:55 INFO SBK Version: 0.86
2021-02-23 12:12:55 INFO Arguments List: [-class, Pulsar, -broker, tcp://localhost:6650, -topic, topic-k-1, -partitions, 1, -writers, 1, -size, 100, -seconds, 60]
2021-02-23 12:12:55 INFO sbk.applicationName: sbk
2021-02-23 12:12:55 INFO sbk.className: 
2021-02-23 12:12:55 INFO Reflections took 66 ms to scan 34 urls, producing 53 keys and 173 values 
2021-02-23 12:12:55 INFO Available Drivers : 33
2021-02-23 12:12:55 INFO Arguments to Driver 'Pulsar' : [-broker, tcp://localhost:6650, -topic, topic-k-1, -partitions, 1, -writers, 1, -size, 100, -seconds, 60]
2021-02-23 12:12:55 INFO Time Unit: MILLISECONDS
2021-02-23 12:12:55 INFO Minimum Latency: 0 ms
2021-02-23 12:12:55 INFO Maximum Latency: 180000 ms
2021-02-23 12:12:55 INFO Window Latency Store: Array
2021-02-23 12:12:55 INFO Total Window Latency Store: HashMap
2021-02-23 12:12:55 INFO PrometheusLogger Started
2021-02-23 12:12:56 INFO [[id: 0x64de4cd2, L:/127.0.0.1:51428 - R:localhost/127.0.0.1:6650]] Connected to server
2021-02-23 12:12:56 INFO Starting Pulsar producer perf with config: {
  "topicName" : "topic-k-1",
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
2021-02-23 12:12:56 INFO Pulsar client config: {
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
2021-02-23 12:12:56 INFO [topic-k-1] [null] Creating producer on cnx [id: 0x64de4cd2, L:/127.0.0.1:51428 - R:localhost/127.0.0.1:6650]
2021-02-23 12:12:56 INFO [topic-k-1] [standalone-0-0] Created producer on cnx [id: 0x64de4cd2, L:/127.0.0.1:51428 - R:localhost/127.0.0.1:6650]
2021-02-23 12:12:57 WARN Failed to load Circe JNI library. Falling back to Java based CRC32c provider
Pulsar Writing     809876 records,  161910.4 records/sec,    15.44 MB/sec,      6.0 ms avg latency,     106 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      5 ms 10th,       5 ms 25th,       6 ms 50th,       6 ms 75th,       8 ms 90th,       9 ms 95th,      13 ms 99th,      26 ms 99.9th,      45 ms 99.99th.
Pulsar Writing     909903 records,  181835.1 records/sec,    17.34 MB/sec,      5.4 ms avg latency,      32 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      4 ms 10th,       5 ms 25th,       5 ms 50th,       6 ms 75th,       6 ms 90th,       7 ms 95th,      12 ms 99th,      26 ms 99.9th,      26 ms 99.99th.
Pulsar Writing     925885 records,  184992.0 records/sec,    17.64 MB/sec,      5.4 ms avg latency,      25 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      4 ms 10th,       5 ms 25th,       5 ms 50th,       6 ms 75th,       6 ms 90th,       7 ms 95th,      11 ms 99th,      16 ms 99.9th,      16 ms 99.99th.
Pulsar Writing     813448 records,  162559.6 records/sec,    15.50 MB/sec,      6.1 ms avg latency,     449 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      4 ms 10th,       5 ms 25th,       5 ms 50th,       6 ms 75th,       7 ms 90th,       8 ms 95th,      12 ms 99th,     444 ms 99.9th,     445 ms 99.99th.
Pulsar Writing     895616 records,  178980.0 records/sec,    17.07 MB/sec,      5.5 ms avg latency,      86 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      4 ms 10th,       5 ms 25th,       5 ms 50th,       6 ms 75th,       7 ms 90th,       8 ms 95th,      11 ms 99th,      83 ms 99.9th,      83 ms 99.99th.
Pulsar Writing     722486 records,  144410.6 records/sec,    13.77 MB/sec,      6.9 ms avg latency,     473 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      5 ms 10th,       5 ms 25th,       5 ms 50th,       6 ms 75th,       8 ms 90th,       9 ms 95th,      14 ms 99th,     467 ms 99.9th,     469 ms 99.99th.
Pulsar Writing     931596 records,  186244.7 records/sec,    17.76 MB/sec,      5.3 ms avg latency,      18 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      4 ms 10th,       5 ms 25th,       5 ms 50th,       6 ms 75th,       6 ms 90th,       7 ms 95th,       9 ms 99th,      12 ms 99.9th,      16 ms 99.99th.
Total : Pulsar Writing    6484499 records,  106289.3 records/sec,    10.14 MB/sec,      5.8 ms avg latency,     473 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      4 ms 10th,       5 ms 25th,       5 ms 50th,       6 ms 75th,       7 ms 90th,       8 ms 95th,      12 ms 99th,      37 ms 99.9th,     467 ms 99.99th.
```

<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# MongoDB performance benchmarking with SBK
The MongoDB driver for SBK supports single/multiple Writer , single/multiple reader performance benchmarking.

## setting up  MongoDB local Docker server
To make simple demo/test, you can run the local MongoDB server docker image as follows:

```
docker run -p 127.0.0.1:27017:27017/tcp  --name kmg-mongo -d mongo:latest
```

make sure that you have redirected the port 27017 from docker to local system

Now, you can run the MongoDB writer benchmarking as follows. 
```
kmg@W10GRDN6H2:~/kmg-linux/SBK$ ./build/install/sbk/bin/sbk -class mongodb -writers 1 -size 100 -time 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-07-06 09:56:54 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-07-06 09:56:54 INFO SBK version: 0.79
2020-07-06 09:56:54 INFO Reflections took 101 ms to scan 21 urls, producing 30 keys and 110 values
2020-07-06 09:56:55 INFO Cluster created with settings {hosts=[localhost:27017], mode=SINGLE, requiredClusterType=UNKNOWN, serverSelectionTimeout='30000 ms', maxWaitQueueSize=500}
2020-07-06 09:56:55 INFO Cluster description not yet available. Waiting for 30000 ms before timing out
2020-07-06 09:56:55 INFO Opened connection [connectionId{localValue:1, serverValue:5}] to localhost:27017
2020-07-06 09:56:55 INFO Monitor thread successfully connected to server with description ServerDescription{address=localhost:27017, type=STANDALONE, state=CONNECTED, ok=true, version=ServerVersion{versionList=[4, 2, 8]}, minWireVersion=0, maxWireVersion=8, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=5900700}
2020-07-06 09:56:55 INFO Opened connection [connectionId{localValue:2, serverValue:6}] to localhost:27017
Writing       3164 records,     632.5 records/sec,     0.06 MB/sec,      1.6 ms avg latency,      22 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       2 ms 50th,       2 ms 75th,       2 ms 95th,       3 ms 99th,       7 ms 99.9th,      22 ms 99.99th.
Writing       3498 records,     699.5 records/sec,     0.07 MB/sec,      1.4 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,       6 ms 99.99th.
Writing       3735 records,     746.6 records/sec,     0.07 MB/sec,      1.3 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,       5 ms 99.99th.
Writing       4040 records,     807.8 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,       7 ms 99.99th.
Writing       4062 records,     812.2 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       5 ms 99.99th.
Writing       4060 records,     811.8 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,       5 ms 99.99th.
Writing       4011 records,     801.9 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       6 ms 99.99th.
Writing       3769 records,     753.5 records/sec,     0.07 MB/sec,      1.3 ms avg latency,       9 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,       9 ms 99.99th.
Writing       4068 records,     813.4 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,       7 ms 99.99th.
Writing       4084 records,     816.5 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,       5 ms 99.99th.
Writing       3969 records,     793.6 records/sec,     0.08 MB/sec,      1.3 ms avg latency,       9 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,       9 ms 99.99th.
Writing(Total)      44954 records,     773.0 records/sec,     0.07 MB/sec,      1.3 ms avg latency,      22 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,       8 ms 99.99th.
2020-07-06 09:57:53 INFO Closed connection [connectionId{localValue:2, serverValue:6}] to localhost:27017 because the pool has been closed.
```

## FoundationDB Document layer performance benchmarking
The SBK MongoDB driver can be used conduct the performance benchmarking of FoundationDB Document layer.
Note that default port for Foundation DB document layer is 27016 (not 27017).
Refer to this document: https://foundationdb.github.io/fdb-document-layer/getting-started-linux.html for setting for FoundationDB document layer cluster
<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Ignite Benchmarking with SBK 
The SBK supports the benchmarking of Ignite key value store with multiple writers and readers.
It also supports End to End latency benchmarking. The SBK supports the thin client and complete cluster key value store benchmarking. 

# Ignite thin client benchmarking with Docker
Use the below command start the ignite docker

```
docker run -p 127.0.0.1:10800:10800/tcp  apacheignite/ignite
```

The port is redirect the local host "27.0.0.1:10800"

An Example SBK command for benchmarking single writer is as follows

```
./build/install/sbk/bin/sbk  -class ignite  -size 100 -writers 1 -seconds 60
```
In the above example, the data size is 100 bytes and writer generates the linearly incrementing keys.
example output:

```
./build/install/sbk/bin/sbk  -class ignite  -size 100 -writers 1 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-07-19 07:22:38 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-07-19 07:22:38 INFO SBK version: 0.81
2020-07-19 07:22:38 INFO Reflections took 109 ms to scan 23 urls, producing 31 keys and 119 values
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/mnt/c/KMG/kmg-linux/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
Writing       4290 records,     857.8 records/sec,     0.08 MB/sec,      1.1 ms avg latency,       9 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       9 ms 99.99th.
Writing       4215 records,     841.1 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       3 ms 99.99th.
Writing       4084 records,     816.5 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       3 ms 99.99th.
Writing       4005 records,     800.4 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       4 ms 99.99th.
Writing       4295 records,     858.8 records/sec,     0.08 MB/sec,      1.2 ms avg latency,      15 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,      15 ms 99.99th.
Writing       4087 records,     817.1 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       8 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       8 ms 99.99th.
Writing       3855 records,     770.4 records/sec,     0.07 MB/sec,      1.3 ms avg latency,      11 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       3 ms 99th,       7 ms 99.9th,      11 ms 99.99th.
Writing       4185 records,     836.7 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       5 ms 99.99th.
Writing       4196 records,     838.9 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       9 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       9 ms 99.99th.
Writing       4161 records,     831.9 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       3 ms 99.99th.
Writing       4228 records,     845.1 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       3 ms 99.99th.
Writing(Total)      48229 records,     790.1 records/sec,     0.08 MB/sec,      1.2 ms avg latency,      15 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       8 ms 99.99th.
```

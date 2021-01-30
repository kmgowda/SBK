<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# FoundationDB Benchmarking with SBK 
The SBK supports the benchmarking of foundationDB key value store with mulitple writers and readers.
It also supports End to End latency benchmarking. The SBK uses the fdb-java client library; 
so, before using this FoundationDB driver with SBK make sure you install the foundationdb-clients-6.XX client library.
Refer to this page : https://www.foundationdb.org/download/ for required foundationDB client libs.
The default path : fdb.cluster file is : /etc/foundationdb/fdb.cluster,
but you can change the path by passing arguments for the **-cfile** option.


An Example SBK command for benchmarking single writer is as follows

```
./build/install/sbk/bin/sbk -class foundationdb -size 100 -writers 1 -seconds 30
```
In the above example, the data size is 100 bytes and writer generates the linearly incrementing keys.
example output:

```
 ./build/install/sbk/bin/sbk -class foundationdb -size 100 -writers 1 -seconds 30
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-06-21 03:09:08 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-06-21 03:09:08 INFO SBK version: 0.78
2020-06-21 03:09:08 INFO Reflections took 59 ms to scan 19 urls, producing 22 keys and 90 values
Writing       4935 records,     986.8 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       9 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       9 ms 99.99th.
Writing       4924 records,     984.6 records/sec,     0.09 MB/sec,      1.0 ms avg latency,      17 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,      17 ms 99.99th.
Writing       4803 records,     960.2 records/sec,     0.09 MB/sec,      1.0 ms avg latency,     102 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,     102 ms 99.99th.
Writing       4938 records,     987.4 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       3 ms 99.99th.
Writing       4925 records,     984.0 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       4 ms 99.99th.
Writing(Total)      28846 records,     981.5 records/sec,     0.09 MB/sec,      1.0 ms avg latency,     102 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,      17 ms 99.99th.
```

An SBK command for End to End latency benchmarking is as follows:
```
./build/install/sbk/bin/sbk -class foundationdb -size 100 -writers 1 -readers 1 -seconds 30
```
The output is as follows:
```
./build/install/sbk/bin/sbk -class foundationdb -size 100 -writers 1 -readers 1 -seconds 30
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-06-21 03:12:06 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-06-21 03:12:06 INFO SBK version: 0.78
2020-06-21 03:12:06 INFO Reflections took 59 ms to scan 19 urls, producing 22 keys and 90 values
Write/Reading       4924 records,     984.4 records/sec,     0.09 MB/sec,      1.2 ms avg latency,      21 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,      21 ms 99.99th.
Write/Reading       4932 records,     986.2 records/sec,     0.09 MB/sec,      1.3 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       6 ms 99.99th.
Write/Reading       4943 records,     988.4 records/sec,     0.09 MB/sec,      1.2 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       3 ms 99.99th.
Write/Reading       4897 records,     979.2 records/sec,     0.09 MB/sec,      1.3 ms avg latency,      43 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,      43 ms 99.99th.
Write/Reading       4944 records,     988.2 records/sec,     0.09 MB/sec,      1.3 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       4 ms 99.99th.
Write/Reading(Total)      28927 records,     984.3 records/sec,     0.09 MB/sec,      1.3 ms avg latency,      43 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,      21 ms 99.99th.

```

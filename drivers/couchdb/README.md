<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# CouchDB performance benchmarking with SBK
The CouchDB driver for SBK supports single/multiple Writer , single/multiple reader performance benchmarking.

## setting up CouchDB local Docker server
To make simple demo/test, you can run the local CouchDB server docker image as follows:

```
docker run -p 5984:5984 -e COUCHDB_USER=admin -e COUCHDB_PASSWORD=admin -d couchdb
```

make sure you have redirected the port 5984 from docker to local system

Now, you can run the CouchDB writer benchmarking as follows. 
```
kmg@kmg:~/kmg-linux/SBK$ ./build/install/sbk/bin/sbk -class couchdb  -size 100 -writers 1 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/mnt/d/kmg-linux/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/d/kmg-linux/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/d/kmg-linux/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-07-30 13:27:36 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-07-30 13:27:36 INFO SBK version: 0.81
2020-07-30 13:27:36 INFO Argument List: [-class, couchdb, -size, 100, -writers, 1, -seconds, 60]
2020-07-30 13:27:37 INFO Reflections took 114 ms to scan 24 urls, producing 31 keys and 126 values
2020-07-30 13:27:38 INFO The data base :demo not found
Writing        267 records,      53.4 records/sec,     0.01 MB/sec,     18.6 ms avg latency,     104 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      14 ms 10th,      16 ms 25th,      17 ms 50th,      19 ms 75th,      26 ms 95th,      68 ms 99th,     104 ms 99.9th,     104 ms 99.99th.
Writing        280 records,      55.9 records/sec,     0.01 MB/sec,     17.9 ms avg latency,      40 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      14 ms 10th,      16 ms 25th,      17 ms 50th,      19 ms 75th,      27 ms 95th,      36 ms 99th,      40 ms 99.9th,      40 ms 99.99th.
Writing        237 records,      47.3 records/sec,     0.00 MB/sec,     21.1 ms avg latency,     123 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      15 ms 10th,      17 ms 25th,      19 ms 50th,      23 ms 75th,      33 ms 95th,      61 ms 99th,     123 ms 99.9th,     123 ms 99.99th.
Writing        222 records,      44.2 records/sec,     0.00 MB/sec,     22.6 ms avg latency,      83 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      15 ms 10th,      17 ms 25th,      20 ms 50th,      25 ms 75th,      42 ms 95th,      64 ms 99th,      83 ms 99.9th,      83 ms 99.99th.
Writing        187 records,      37.0 records/sec,     0.00 MB/sec,     27.0 ms avg latency,     156 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      16 ms 10th,      19 ms 25th,      24 ms 50th,      30 ms 75th,      49 ms 95th,     112 ms 99th,     156 ms 99.9th,     156 ms 99.99th.
Writing        211 records,      42.1 records/sec,     0.00 MB/sec,     23.7 ms avg latency,     186 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      14 ms 10th,      16 ms 25th,      19 ms 50th,      26 ms 75th,      45 ms 95th,     108 ms 99th,     186 ms 99.9th,     186 ms 99.99th.
Writing        276 records,      55.1 records/sec,     0.01 MB/sec,     18.1 ms avg latency,      38 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      13 ms 10th,      15 ms 25th,      17 ms 50th,      20 ms 75th,      28 ms 95th,      36 ms 99th,      38 ms 99.9th,      38 ms 99.99th.
Writing        246 records,      49.1 records/sec,     0.00 MB/sec,     20.3 ms avg latency,      75 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      14 ms 10th,      16 ms 25th,      18 ms 50th,      21 ms 75th,      37 ms 95th,      57 ms 99th,      75 ms 99.9th,      75 ms 99.99th.
Writing        315 records,      62.9 records/sec,     0.01 MB/sec,     15.9 ms avg latency,      36 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      13 ms 10th,      14 ms 25th,      15 ms 50th,      18 ms 75th,      22 ms 95th,      23 ms 99th,      36 ms 99.9th,      36 ms 99.99th.
Writing        271 records,      54.1 records/sec,     0.01 MB/sec,     18.5 ms avg latency,      66 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      14 ms 10th,      15 ms 25th,      17 ms 50th,      19 ms 75th,      32 ms 95th,      49 ms 99th,      66 ms 99.9th,      66 ms 99.99th.
Writing        264 records,      52.7 records/sec,     0.01 MB/sec,     19.0 ms avg latency,      91 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      14 ms 10th,      15 ms 25th,      17 ms 50th,      20 ms 75th,      31 ms 95th,      46 ms 99th,      91 ms 99.9th,      91 ms 99.99th.
Writing(Total)       2900 records,      47.5 records/sec,     0.00 MB/sec,     20.2 ms avg latency,     186 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:      14 ms 10th,      15 ms 25th,      17 ms 50th,      21 ms 75th,      35 ms 95th,      63 ms 99th,     127 ms 99.9th,     186 ms 99.99th.
```

The sample output for the CouchDB read is as follows:

```
kmg@kmg:~/kmg-linux/SBK$ ./build/install/sbk/bin/sbk -class couchdb  -size 100 -readers 1 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/mnt/d/kmg-linux/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/d/kmg-linux/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/mnt/d/kmg-linux/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-07-30 13:29:37 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-07-30 13:29:37 INFO SBK version: 0.81
2020-07-30 13:29:37 INFO Argument List: [-class, couchdb, -size, 100, -readers, 1, -seconds, 60]
2020-07-30 13:29:37 INFO Reflections took 115 ms to scan 24 urls, producing 31 keys and 126 values
Reading        623 records,     124.6 records/sec,     0.05 MB/sec,      7.9 ms avg latency,      48 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       6 ms 10th,       6 ms 25th,       7 ms 50th,       8 ms 75th,      13 ms 95th,      25 ms 99th,      48 ms 99.9th,      48 ms 99.99th.
Reading        697 records,     139.2 records/sec,     0.05 MB/sec,      7.2 ms avg latency,      25 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       5 ms 10th,       6 ms 25th,       6 ms 50th,       7 ms 75th,      13 ms 95th,      22 ms 99th,      25 ms 99.9th,      25 ms 99.99th.
Reading        786 records,     157.2 records/sec,     0.06 MB/sec,      6.4 ms avg latency,      28 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       5 ms 10th,       5 ms 25th,       6 ms 50th,       6 ms 75th,      11 ms 95th,      17 ms 99th,      28 ms 99.9th,      28 ms 99.99th.
2020-07-30 13:29:57 INFO Reader 0 exited with EOF
Reading(Total)       2900 records,     146.5 records/sec,     0.06 MB/sec,      6.8 ms avg latency,      48 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       5 ms 10th,       5 ms 25th,       6 ms 50th,       7 ms 75th,      11 ms 95th,      21 ms 99th,      28 ms 99.9th,      48 ms 99.99th.

```
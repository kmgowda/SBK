<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Redis Performance benchmarking using SBK
The Redis driver for SBK supports multi writers and readers performance benchmarking.


An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class redis -size 10 -writers 1 -seconds 60
```

by default, the SBK uses the local Redis Server: 127.0.0.1:6379, and default list name is 'list-1'


Sample SBK Redis write benchmarking output

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class redis -size 10 -writers 1 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-03-07 12:18:44 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-03-07 12:18:44 INFO Java Runtime Version: 11.0.8+11
2021-03-07 12:18:44 INFO SBK Version: 0.861
2021-03-07 12:18:44 INFO Arguments List: [-class, redis, -size, 10, -writers, 1, -seconds, 60]
2021-03-07 12:18:44 INFO sbk.applicationName: sbk
2021-03-07 12:18:44 INFO sbk.className: sbk
2021-03-07 12:18:45 INFO Reflections took 57 ms to scan 35 urls, producing 52 keys and 175 values 
2021-03-07 12:18:45 INFO Available Drivers : 34
2021-03-07 12:18:45 INFO Arguments to Driver 'Redis' : [-size, 10, -writers, 1, -seconds, 60]
2021-03-07 12:18:45 INFO Time Unit: MILLISECONDS
2021-03-07 12:18:45 INFO Minimum Latency: 0 ms
2021-03-07 12:18:45 INFO Maximum Latency: 180000 ms
2021-03-07 12:18:45 INFO Window Latency Store: Array
2021-03-07 12:18:45 INFO Total Window Latency Store: HashMap
2021-03-07 12:18:45 INFO PrometheusLogger Started
Redis Writing       6942 records,    1388.1 records/sec,     0.01 MB/sec,      0.7 ms avg latency,      41 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      41 ms 99.99th.
Redis Writing       6550 records,    1309.5 records/sec,     0.01 MB/sec,      0.8 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       4 ms 99.99th.
Redis Writing       6932 records,    1386.1 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       5 ms 99.99th.
Redis Writing       6708 records,    1341.3 records/sec,     0.01 MB/sec,      0.7 ms avg latency,      58 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      58 ms 99.99th.
Redis Writing       4402 records,     878.3 records/sec,     0.01 MB/sec,      1.1 ms avg latency,     827 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,     827 ms 99.99th.
Redis Writing       6082 records,    1215.9 records/sec,     0.01 MB/sec,      0.8 ms avg latency,     341 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,     341 ms 99.99th.
Redis Writing       6976 records,    1394.6 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       3 ms 99.99th.
Redis Writing       6735 records,    1346.7 records/sec,     0.01 MB/sec,      0.7 ms avg latency,      15 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      15 ms 99.99th.
Redis Writing       6861 records,    1371.9 records/sec,     0.01 MB/sec,      0.7 ms avg latency,      95 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      95 ms 99.99th.
Redis Writing       6779 records,    1355.5 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Redis Writing       6830 records,    1365.7 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Redis Writing       6381 records,    1282.6 records/sec,     0.01 MB/sec,      0.8 ms avg latency,     292 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,     292 ms 99.99th.
Total : Redis Writing      78178 records,    1303.0 records/sec,     0.01 MB/sec,      0.8 ms avg latency,     827 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      58 ms 99.99th.
2021-03-07 12:19:45 INFO SBK Performance Shutdown
2021-03-07 12:19:45 INFO PrometheusLogger Stopped
2021-03-07 12:19:46 INFO SBK Benchmark Shutdown

```

The sample SBK Redis read benchmarking output is below
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class redis -size 10 -readers 1 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-03-07 12:20:04 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-03-07 12:20:04 INFO Java Runtime Version: 11.0.8+11
2021-03-07 12:20:04 INFO SBK Version: 0.861
2021-03-07 12:20:04 INFO Arguments List: [-class, redis, -size, 10, -readers, 1, -seconds, 60]
2021-03-07 12:20:04 INFO sbk.applicationName: sbk
2021-03-07 12:20:04 INFO sbk.className: sbk
2021-03-07 12:20:04 INFO Reflections took 54 ms to scan 35 urls, producing 52 keys and 175 values 
2021-03-07 12:20:04 INFO Available Drivers : 34
2021-03-07 12:20:04 INFO Arguments to Driver 'Redis' : [-size, 10, -readers, 1, -seconds, 60]
2021-03-07 12:20:04 INFO Time Unit: MILLISECONDS
2021-03-07 12:20:04 INFO Minimum Latency: 0 ms
2021-03-07 12:20:04 INFO Maximum Latency: 180000 ms
2021-03-07 12:20:04 INFO Window Latency Store: Array
2021-03-07 12:20:04 INFO Total Window Latency Store: HashMap
2021-03-07 12:20:04 INFO PrometheusLogger Started
Redis Reading       6867 records,    1372.9 records/sec,     0.01 MB/sec,      0.7 ms avg latency,      18 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      18 ms 99.99th.
Redis Reading       6901 records,    1379.9 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       7 ms 99.99th.
Redis Reading       6703 records,    1340.1 records/sec,     0.01 MB/sec,      0.7 ms avg latency,      62 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      62 ms 99.99th.
Redis Reading       6820 records,    1363.7 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       7 ms 99.99th.
Redis Reading       6605 records,    1319.9 records/sec,     0.01 MB/sec,      0.8 ms avg latency,       8 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       8 ms 99.99th.
Redis Reading       6481 records,    1295.9 records/sec,     0.01 MB/sec,      0.8 ms avg latency,      38 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      38 ms 99.99th.
Redis Reading       6889 records,    1377.5 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       5 ms 99.99th.
Redis Reading       6851 records,    1369.9 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       3 ms 99.99th.
Redis Reading       6790 records,    1357.7 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       3 ms 99.99th.
Redis Reading       6687 records,    1337.1 records/sec,     0.01 MB/sec,      0.7 ms avg latency,      44 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      44 ms 99.99th.
Redis Reading       6657 records,    1330.9 records/sec,     0.01 MB/sec,      0.8 ms avg latency,      11 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,      11 ms 99.99th.
2021-03-07 12:21:02 INFO Reader 0 exited with EOF
Redis Reading       3936 records,    1409.7 records/sec,     0.01 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Total : Redis Reading      78187 records,    1352.5 records/sec,     0.01 MB/sec,      0.7 ms avg latency,      62 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       8 ms 99.99th.
2021-03-07 12:21:02 INFO SBK Performance Shutdown
2021-03-07 12:21:02 INFO PrometheusLogger Stopped
2021-03-07 12:21:03 INFO SBK Benchmark Shutdown

```


## Redis Dockers

Below docker command can be used launch the single Redis Server container.

```
docker run  -p 127.0.0.1:6379:6379/tcp   --name some-redis -d redis

```
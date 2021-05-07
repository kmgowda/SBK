<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Cassandra Driver for SBK
The Cassandra driver for SBK supports multi writers,readers performance benchmarking. But, the End to End Latency benchmarking is not supported.

## Cassandra Docker image
Refer this page : https://cassandra.apache.org/quickstart/ to quickly start with Cassandra docker

or run the below command to launch the single node cassandra data center.
```
docker run -p 9042:9042  cassandra
```

The example SBK command for data writing as follows:
```
./build/install/sbk/bin/sbk -class cassandra -writers 1 -size 100 -seconds 120
```

by default node is "localhost", port: 9042 , keyspace : sbk, table : tmp

sample output
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class cassandra -writers 1 -size 100 -seconds 120 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-05-07 17:57:52 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-05-07 17:57:52 INFO Java Runtime Version: 11.0.8+11
2021-05-07 17:57:52 INFO SBK Version: 0.88
2021-05-07 17:57:52 INFO Arguments List: [-class, cassandra, -writers, 1, -size, 100, -seconds, 120]
2021-05-07 17:57:52 INFO sbk.applicationName: sbk
2021-05-07 17:57:52 INFO sbk.className: sbk
2021-05-07 17:57:52 INFO Reflections took 70 ms to scan 37 urls, producing 57 keys and 185 values 
2021-05-07 17:57:52 INFO Available Drivers : 36
2021-05-07 17:57:52 INFO KMG.. read the config value
2021-05-07 17:57:52 INFO Arguments to Driver 'Cassandra' : [-writers, 1, -size, 100, -seconds, 120]
2021-05-07 17:57:52 INFO Time Unit: MILLISECONDS
2021-05-07 17:57:52 INFO Minimum Latency: 0 ms
2021-05-07 17:57:52 INFO Maximum Latency: 180000 ms
2021-05-07 17:57:52 INFO Window Latency Store: Array
2021-05-07 17:57:52 INFO Total Window Latency Store: HashMap
2021-05-07 17:57:53 INFO SBK PrometheusLogger Started
2021-05-07 17:57:53 INFO DataStax Java driver for Apache Cassandra(R) (com.datastax.oss:java-driver-core) version 4.11.1
2021-05-07 17:57:53 INFO Using native clock for microsecond precision
2021-05-07 17:57:58 INFO Performance Logger Started
2021-05-07 17:57:58 INFO SBK Benchmark Started
Cassandra Writing       4183 records,     836.4 records/sec,     0.08 MB/sec,      1.2 ms avg latency,     257 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,     257 ms 99.99th.
Cassandra Writing       4528 records,     904.0 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      11 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,      11 ms 99.99th.
Cassandra Writing       4534 records,     906.4 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      11 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       4 ms 99.9th,      11 ms 99.99th.
Cassandra Writing       4353 records,     870.4 records/sec,     0.08 MB/sec,      1.1 ms avg latency,     229 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,      14 ms 99.9th,     229 ms 99.99th.
Cassandra Writing       4603 records,     920.4 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      11 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,      11 ms 99.99th.
Cassandra Writing       4717 records,     943.2 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Cassandra Writing       5074 records,    1013.2 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      15 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,      15 ms 99.99th.
Cassandra Writing       4566 records,     913.0 records/sec,     0.09 MB/sec,      1.1 ms avg latency,     239 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,     239 ms 99.99th.
Cassandra Writing       4725 records,     944.8 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      12 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,      12 ms 99.99th.
Cassandra Writing       4540 records,     907.8 records/sec,     0.09 MB/sec,      1.1 ms avg latency,     120 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,     120 ms 99.99th.
Cassandra Writing       4530 records,     905.8 records/sec,     0.09 MB/sec,      1.1 ms avg latency,     289 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       5 ms 99.9th,     289 ms 99.99th.
Cassandra Writing       5135 records,    1004.9 records/sec,     0.10 MB/sec,      1.0 ms avg latency,     129 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,     129 ms 99.99th.
Cassandra Writing       5118 records,    1023.4 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       9 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       9 ms 99.99th.
Cassandra Writing       4841 records,     968.0 records/sec,     0.09 MB/sec,      1.0 ms avg latency,     273 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       8 ms 99.9th,     273 ms 99.99th.
Cassandra Writing       5200 records,    1039.0 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,       5 ms 99.99th.
Cassandra Writing       4985 records,     996.8 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      42 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,      42 ms 99.99th.
Cassandra Writing       4058 records,     811.4 records/sec,     0.08 MB/sec,      1.2 ms avg latency,    1002 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       5 ms 99.9th,    1002 ms 99.99th.
Cassandra Writing       4972 records,     993.6 records/sec,     0.09 MB/sec,      1.0 ms avg latency,      45 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,      45 ms 99.99th.
Cassandra Writing       5080 records,    1015.2 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      81 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       4 ms 99.9th,      81 ms 99.99th.
Cassandra Writing       4923 records,     984.2 records/sec,     0.09 MB/sec,      1.0 ms avg latency,      15 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,      15 ms 99.99th.
Cassandra Writing       3893 records,     778.0 records/sec,     0.07 MB/sec,      1.3 ms avg latency,    1007 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,    1007 ms 99.99th.
Cassandra Writing       5190 records,    1037.8 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      12 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,      12 ms 99.99th.
Cassandra Writing       5014 records,    1002.0 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      58 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,      58 ms 99.99th.
Cassandra Writing       4621 records,     955.1 records/sec,     0.09 MB/sec,      1.0 ms avg latency,     206 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,     206 ms 99.99th.
Total : Cassandra Writing     113383 records,     944.8 records/sec,     0.09 MB/sec,      1.1 ms avg latency,    1007 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,     120 ms 99.99th.
2021-05-07 17:59:58 INFO Performance Logger Shutdown
2021-05-07 18:00:00 INFO SBK PrometheusLogger Shutdown
2021-05-07 18:00:01 INFO SBK Benchmark Shutdown

```

For read performance benchmarking , the example command is 
```
./build/install/sbk/bin/sbk -class cassandra -readers 1 -size 100 -seconds 60 
```
The sample output is as follows:
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class cassandra -readers 1 -size 100 -seconds 60  
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-05-07 18:00:44 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-05-07 18:00:44 INFO Java Runtime Version: 11.0.8+11
2021-05-07 18:00:44 INFO SBK Version: 0.88
2021-05-07 18:00:44 INFO Arguments List: [-class, cassandra, -readers, 1, -size, 100, -seconds, 60]
2021-05-07 18:00:44 INFO sbk.applicationName: sbk
2021-05-07 18:00:44 INFO sbk.className: sbk
2021-05-07 18:00:44 INFO Reflections took 65 ms to scan 37 urls, producing 57 keys and 185 values 
2021-05-07 18:00:44 INFO Available Drivers : 36
2021-05-07 18:00:44 INFO KMG.. read the config value
2021-05-07 18:00:44 INFO Arguments to Driver 'Cassandra' : [-readers, 1, -size, 100, -seconds, 60]
2021-05-07 18:00:44 INFO Time Unit: MILLISECONDS
2021-05-07 18:00:44 INFO Minimum Latency: 0 ms
2021-05-07 18:00:44 INFO Maximum Latency: 180000 ms
2021-05-07 18:00:44 INFO Window Latency Store: Array
2021-05-07 18:00:44 INFO Total Window Latency Store: HashMap
2021-05-07 18:00:44 INFO SBK PrometheusLogger Started
2021-05-07 18:00:44 INFO DataStax Java driver for Apache Cassandra(R) (com.datastax.oss:java-driver-core) version 4.11.1
2021-05-07 18:00:45 INFO Using native clock for microsecond precision
2021-05-07 18:00:45 INFO Performance Logger Started
2021-05-07 18:00:45 INFO SBK Benchmark Started
2021-05-07 18:00:46 INFO Reader 0 exited with EOF
Cassandra Reading     113398 records,  113398.0 records/sec,    10.81 MB/sec,      0.0 ms avg latency,      45 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 90th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,      28 ms 99.99th.
Total : Cassandra Reading     113398 records,  113398.0 records/sec,    10.81 MB/sec,      0.0 ms avg latency,      45 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 90th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,      28 ms 99.99th.
2021-05-07 18:00:46 INFO Performance Logger Shutdown
2021-05-07 18:00:48 INFO SBK PrometheusLogger Shutdown
2021-05-07 18:00:49 INFO SBK Benchmark Shutdown
```

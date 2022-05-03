<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# H2 Performance Benchmarking using SBK
The H2 driver can be used to get the performance benchmarking of the 
[H2 database](https://h2database.com/html/main.html). 
See [H2 downloads](https://h2database.com/html/download.html) for more information.

The H2 driver uses JDBC for IO operations. 
By default, the H2 driver uses the embedded database `db`(db.mv.db) with driver `org.h2.Driver`and url `jdbc:h2:~/db` with username `sa` and password `""`(blank). The default table is `test`

An example command for write benchmarking is:
```
build\install\sbk\bin\sbk -class h2 -writers 1 -size 100 -seconds 30
```

Note: The above command runs on Windows and Linux, on MacOS use the command `./build/install/sbk/bin/sbk -class h2 -writers 1 -size 100 -seconds 30`

The output for write benchmarking is:
```
D:\Sushruth-Nagaraj\SBK>build\install\sbk\bin\sbk -class h2 -writers 1 -size 100 -seconds 30
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/D:/Sushruth-Nagaraj/SBK/build/install/sbk/lib/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/D:/Sushruth-Nagaraj/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/D:/Sushruth-Nagaraj/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-04-02 15:57:28 INFO Reflections took 186 ms to scan 46 urls, producing 98 keys and 210 values
2022-04-02 15:57:28 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-04-02 15:57:28 INFO Storage Benchmark Kit
2022-04-02 15:57:28 INFO SBK Version: 0.98
2022-04-02 15:57:28 INFO SBK Website: https://github.com/kmgowda/SBK
2022-04-02 15:57:28 INFO Arguments List: [-class, h2, -writers, 1, -size, 100, -seconds, 30]
2022-04-02 15:57:28 INFO Java Runtime Version: 17.0.2+8-LTS-86
2022-04-02 15:57:28 INFO Storage Drivers Package: io.sbk
2022-04-02 15:57:28 INFO sbk.applicationName: sbk
2022-04-02 15:57:28 INFO sbk.appHome: D:\Sushruth-Nagaraj\SBK\build\install\sbk\bin\..
2022-04-02 15:57:28 INFO sbk.className:
2022-04-02 15:57:28 INFO '-class': h2
2022-04-02 15:57:28 INFO Available Storage Drivers in package 'io.sbk': 40 [Artemis,
AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, Derby,
FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive, Jdbc, Kafka, LevelDB,
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL,
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-04-02 15:57:28 INFO Arguments to Driver 'H2' : [-writers, 1, -size, 100, -seconds, 30]
2022-04-02 15:57:28 INFO Time Unit: MILLISECONDS
2022-04-02 15:57:28 INFO Minimum Latency: 0 ms
2022-04-02 15:57:28 INFO Maximum Latency: 180000 ms
2022-04-02 15:57:28 INFO Window Latency Store: Array, Size: 1 MB
2022-04-02 15:57:28 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-04-02 15:57:28 INFO Total Window Extension: None, Size: 0 MB
2022-04-02 15:57:29 INFO SBK Benchmark Started
2022-04-02 15:57:29 INFO SBK PrometheusLogger Started
2022-04-02 15:57:29 INFO JDBC Url: jdbc:h2:~/db
2022-04-02 15:57:30 INFO JDBC Driver Type: h2
2022-04-02 15:57:30 INFO JDBC Driver Name: H2 JDBC Driver
2022-04-02 15:57:30 INFO JDBC Driver Version: 2.1.210 (2022-01-17)
2022-04-02 15:57:30 INFO Deleting the Table: test
2022-04-02 15:57:33 INFO Creating the Table: test
2022-04-02 15:57:33 INFO CQueuePerl Start
2022-04-02 15:57:33 INFO Performance Recorder Started
2022-04-02 15:57:33 INFO Writer 0 started , run seconds: 30
H2 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        66.1 MB,           693532 records,    138567.8 records/sec,    13.21 MB/sec,      0.0 ms avg latency,     118 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       5 ms 70th.
H2 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       227.9 MB,          2389519 records,    477808.2 records/sec,    45.57 MB/sec,      0.0 ms avg latency,      31 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
H2 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       205.8 MB,          2158015 records,    431516.7 records/sec,    41.15 MB/sec,      0.0 ms avg latency,      56 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
H2 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       246.3 MB,          2582242 records,    516345.1 records/sec,    49.24 MB/sec,      0.0 ms avg latency,      45 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
H2 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       234.9 MB,          2463223 records,    492546.1 records/sec,    46.97 MB/sec,      0.0 ms avg latency,      32 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
H2 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        4 seconds,       199.3 MB,          2089501 records,    418653.8 records/sec,    39.93 MB/sec,      0.0 ms avg latency,      51 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
Total : H2 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,       30 seconds,      1180.3 MB,         12376032 records,    412534.4 records/sec,    39.34 MB/sec,      0.0 ms avg latency,     118 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
2022-04-02 15:58:03 INFO Performance Recorder Exited
2022-04-02 15:58:03 INFO CQueuePerl Shutdown
2022-04-02 15:58:32 INFO Writer 0 exited
2022-04-02 15:58:32 INFO SBK Benchmark initiated Writers
2022-04-02 15:58:33 INFO SBK PrometheusLogger Shutdown
2022-04-02 15:58:34 INFO SBK Benchmark Shutdown
```

An example command for read benchmarking is:
```
build\install\sbk\bin\sbk -class h2 -readers 1 -size 100 -seconds 30
```

Note: The above command runs on Windows and Linux, on MacOS use the command `./build/install/sbk/bin/sbk -class h2 -readers 1 -size 100 -seconds 30`

The output for read benchmarking is:
```
D:\Sushruth-Nagaraj\SBK>build\install\sbk\bin\sbk -class h2 -readers 1 -size 100 -seconds 30
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/D:/Sushruth-Nagaraj/SBK/build/install/sbk/lib/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/D:/Sushruth-Nagaraj/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/D:/Sushruth-Nagaraj/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-04-02 15:59:53 INFO Reflections took 165 ms to scan 46 urls, producing 98 keys and 210 values
2022-04-02 15:59:53 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-04-02 15:59:53 INFO Storage Benchmark Kit
2022-04-02 15:59:53 INFO SBK Version: 0.98
2022-04-02 15:59:53 INFO SBK Website: https://github.com/kmgowda/SBK
2022-04-02 15:59:53 INFO Arguments List: [-class, h2, -readers, 1, -size, 100, -seconds, 30]
2022-04-02 15:59:53 INFO Java Runtime Version: 17.0.2+8-LTS-86
2022-04-02 15:59:53 INFO Storage Drivers Package: io.sbk
2022-04-02 15:59:53 INFO sbk.applicationName: sbk
2022-04-02 15:59:53 INFO sbk.appHome: D:\Sushruth-Nagaraj\SBK\build\install\sbk\bin\..
2022-04-02 15:59:53 INFO sbk.className:
2022-04-02 15:59:53 INFO '-class': h2
2022-04-02 15:59:53 INFO Available Storage Drivers in package 'io.sbk': 40 [Artemis,
AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, Derby,
FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive, Jdbc, Kafka, LevelDB,
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL,
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-04-02 15:59:53 INFO Arguments to Driver 'H2' : [-readers, 1, -size, 100, -seconds, 30]
2022-04-02 15:59:53 INFO Time Unit: MILLISECONDS
2022-04-02 15:59:53 INFO Minimum Latency: 0 ms
2022-04-02 15:59:53 INFO Maximum Latency: 180000 ms
2022-04-02 15:59:53 INFO Window Latency Store: Array, Size: 1 MB
2022-04-02 15:59:53 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-04-02 15:59:53 INFO Total Window Extension: None, Size: 0 MB
2022-04-02 15:59:53 INFO SBK Benchmark Started
2022-04-02 15:59:54 INFO SBK PrometheusLogger Started
2022-04-02 15:59:54 INFO JDBC Url: jdbc:h2:~/db
2022-04-02 15:59:54 INFO JDBC Driver Type: h2
2022-04-02 15:59:54 INFO JDBC Driver Name: H2 JDBC Driver
2022-04-02 15:59:54 INFO JDBC Driver Version: 2.1.210 (2022-01-17)
2022-04-02 15:59:54 INFO CQueuePerl Start
2022-04-02 15:59:54 INFO SBK Benchmark initiated Readers
2022-04-02 15:59:55 INFO Performance Recorder Started
2022-04-02 15:59:55 INFO Reader 0 started , run seconds: 30
H2 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,       15 seconds,         0.0 MB,                0 records,         0.0 records/sec,     0.00 MB/sec,      0.0 ms avg latency,       0 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       0 ms 60th,       0 ms 70th.
H2 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,                0 records,         0.0 records/sec,     0.00 MB/sec,      0.0 ms avg latency,       0 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       0 ms 60th,       0 ms 70th.
H2 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       223.1 MB,          2338943 records,    467695.1 records/sec,    44.60 MB/sec,      0.0 ms avg latency,      68 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
H2 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        3 seconds,       178.5 MB,          1872186 records,    470280.3 records/sec,    44.85 MB/sec,      0.0 ms avg latency,      61 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
2022-04-02 16:00:25 INFO Reader 0 exited
Total : H2 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,       30 seconds,       401.6 MB,          4211129 records,    140371.0 records/sec,    13.39 MB/sec,      0.0 ms avg latency,      68 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       1 ms 60th,       1 ms 70th.
2022-04-02 16:00:25 INFO Performance Recorder Exited
2022-04-02 16:00:25 INFO CQueuePerl Shutdown
2022-04-02 16:00:25 INFO SBK PrometheusLogger Shutdown
2022-04-02 16:00:26 INFO SBK Benchmark Shutdown
```
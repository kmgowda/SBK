<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Memcached performance benchmarking with SBK
The Memcached driver for SBK supports single/multiple Writer , single/multiple reader performance benchmarking.

## Setting up Memcached local Docker server
To make simple demo/test, you can run the local memcached server docker image as follows:

```
docker run --name memcached-server -p 11211:11211 memcached
```

make sure you have redirected the port 11211 from docker to local system

Now, you can run the Memcached benchmarking as follows. 
```
sanjaynv@DESKTOP-VMPKLNN MINGW64  - SBK Driver/SBK (master)
$ ./build/install/sbk/bin/sbk.bat -class memcached -writers 1 -size 100 -seconds 60 -url localhost -port 11211
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-09-05 21:37:21 INFO Reflections took 234 ms to scan 50 urls, producing 103 keys and 224 values
2022-09-05 21:37:21 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-09-13 21:37:21 INFO Storage Benchmark Kit
2022-09-13 21:37:21 INFO SBK Version: 1.0
2022-09-13 21:37:21 INFO SBK Website: https://github.com/kmgowda/SBK
2022-09-13 21:37:21 INFO Arguments List: [-class, memcached, -writers, 1, -size, 100, -seconds, 60, -url, localhost, -port, 11211]
2022-09-13 21:37:21 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-09-13 21:37:21 INFO Storage Drivers Package: io.sbk
2022-09-13 21:37:21 INFO sbk.applicationName: sbk
2022-09-13 21:37:21 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-09-13 21:37:21 INFO sbk.className:
2022-09-13 21:37:21 INFO '-class': memcached
2022-09-13 21:37:21 INFO Available Storage Drivers in package 'io.sbk': 43 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive, Jdbc,
Kafka, LevelDB, MariaDB, Memcached, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream,
Nsq, Null, OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ,
RocksDB, SeaweedS3, SQLite]
2022-09-13 21:37:21 INFO Arguments to Driver 'Memcached' : [-writers, 1, -size, 100, -seconds, 60, -url, localhost, -port, 11211]
2022-09-13 21:37:21 INFO Time Unit: MILLISECONDS
2022-09-13 21:37:21 INFO Minimum Latency: 0 ms
2022-09-13 21:37:21 INFO Maximum Latency: 180000 ms
2022-09-13 21:37:21 INFO Window Latency Store: Array, Size: 1 MB
2022-09-13 21:37:21 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-09-13 21:37:21 INFO Total Window Extension: None, Size: 0 MB
2022-09-13 21:37:21 INFO SBK Benchmark Started
2022-09-13 21:37:22 INFO SBK PrometheusLogger Started
2022-09-13 21:37:22 INFO XMemcachedClient is using Text protocol
2022-09-13 21:37:22 INFO Creating 8 reactors...
2022-09-13 21:37:22 INFO The Controller started at localhost/127.0.0.1:0 ...
2022-09-13 21:37:22 INFO Add a session: 127.0.0.1:11211
2022-09-13 21:37:22 INFO CQueuePerl Start
2022-09-13 21:37:22 INFO Performance Recorder Started
2022-09-13 21:37:22 INFO SBK Benchmark initiated Writers
2022-09-13 21:37:22 INFO Writer 0 started , run seconds: 60
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.6 MB,             5987 records,      1197.2 records/sec,     0.11 MB/sec,      0.8 ms avg latency,     165 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   9; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       3 ms 99.5th,       3 ms 99.75th,       4 ms 99.9th,       5 ms 99.95th,     165 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.7 MB,             7093 records,      1418.3 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.7 MB,             7198 records,      1438.4 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       4 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.7 MB,             6887 records,      1377.1 records/sec,     0.13 MB/sec,      0.7 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       4 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.6 MB,             6301 records,      1259.7 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       5 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.6 MB,             6393 records,      1278.3 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.6 MB,             6444 records,      1287.5 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       5 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.6 MB,             6549 records,      1309.5 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       2 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.6 MB,             6626 records,      1324.9 records/sec,     0.13 MB/sec,      0.8 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.6 MB,             6698 records,      1339.3 records/sec,     0.13 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.6 MB,             6788 records,      1356.2 records/sec,     0.13 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        4 seconds,         0.6 MB,             6784 records,      1363.1 records/sec,     0.13 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       2 ms 99.99th.
2022-09-13 21:38:22 INFO Writer 0 exited
Total : Memcached Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,       60 seconds,         7.6 MB,            79748 records,      1329.1 records/sec,     0.13 MB/sec,      0.8 ms avg latency,     165 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       3 ms 99.95th,       4 ms 99.99th.
2022-09-13 21:38:22 INFO Performance Recorder Exited
2022-09-13 21:38:22 INFO CQueuePerl Shutdown
2022-09-13 21:38:22 INFO Remove a session: 127.0.0.1:11211
2022-09-13 21:38:22 INFO Controller has been stopped.
2022-09-13 21:38:22 INFO SBK PrometheusLogger Shutdown
2022-09-13 21:38:23 INFO SBK Benchmark Shutdown

sanjaynv@DESKTOP-VMPKLNN MINGW64  - SBK Driver/SBK (master)
$ ./build/install/sbk/bin/sbk.bat -class memcached -readers 1 -size 100 -seconds 60 -url localhost -port 11211
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-09-05 21:39:57 INFO Reflections took 141 ms to scan 50 urls, producing 103 keys and 224 values
2022-09-05 21:39:57 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-09-13 21:39:57 INFO Storage Benchmark Kit
2022-09-13 21:39:58 INFO SBK Version: 1.0
2022-09-13 21:39:58 INFO SBK Website: https://github.com/kmgowda/SBK
2022-09-13 21:39:58 INFO Arguments List: [-class, memcached, -readers, 1, -size, 100, -seconds, 60, -url, localhost, -port, 11211]
2022-09-13 21:39:58 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-09-13 21:39:58 INFO Storage Drivers Package: io.sbk
2022-09-13 21:39:58 INFO sbk.applicationName: sbk
2022-09-13 21:39:58 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-09-13 21:39:58 INFO sbk.className:
2022-09-13 21:39:58 INFO '-class': memcached
2022-09-13 21:39:58 INFO Available Storage Drivers in package 'io.sbk': 43 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive, Jdbc,
Kafka, LevelDB, MariaDB, Memcached, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream,
Nsq, Null, OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ,
RocksDB, SeaweedS3, SQLite]
2022-09-13 21:39:58 INFO Arguments to Driver 'Memcached' : [-readers, 1, -size, 100, -seconds, 60, -url, localhost, -port, 11211]
2022-09-13 21:39:58 INFO Time Unit: MILLISECONDS
2022-09-13 21:39:58 INFO Minimum Latency: 0 ms
2022-09-13 21:39:58 INFO Maximum Latency: 180000 ms
2022-09-13 21:39:58 INFO Window Latency Store: Array, Size: 1 MB
2022-09-13 21:39:58 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-09-13 21:39:58 INFO Total Window Extension: None, Size: 0 MB
2022-09-13 21:39:58 INFO SBK Benchmark Started
2022-09-13 21:39:58 INFO SBK PrometheusLogger Started
2022-09-13 21:39:58 INFO XMemcachedClient is using Text protocol
2022-09-13 21:39:58 INFO Creating 8 reactors...
2022-09-13 21:39:58 INFO The Controller started at localhost/127.0.0.1:0 ...
2022-09-13 21:39:58 INFO Add a session: 127.0.0.1:11211
2022-09-13 21:39:58 INFO CQueuePerl Start
2022-09-13 21:39:58 INFO Performance Recorder Started
2022-09-13 21:39:58 INFO SBK Benchmark initiated Readers
2022-09-13 21:39:58 INFO Reader 0 started , run seconds: 60
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.6 MB,             6073 records,      1214.4 records/sec,     0.12 MB/sec,      0.8 ms avg latency,      17 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   2; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       3 ms 99.75th,       3 ms 99.9th,       4 ms 99.95th,      17 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7146 records,      1428.9 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       2 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7192 records,      1437.8 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       2 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7209 records,      1441.5 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       2 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7233 records,      1445.4 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       4 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7234 records,      1446.5 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7234 records,      1446.5 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7223 records,      1444.3 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       2 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7254 records,      1450.2 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       4 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7181 records,      1435.9 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
Memcached Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.7 MB,             7230 records,      1445.7 records/sec,     0.14 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       2 ms 99.99th.
2022-09-13 21:40:58 INFO Reader 0 exited
Memcached Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.1 MB,             1565 records,       312.9 records/sec,     0.03 MB/sec,      0.7 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       2 ms 99.99th.
Total : Memcached Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,       60 seconds,         7.6 MB,            79774 records,      1329.2 records/sec,     0.13 MB/sec,      0.7 ms avg latency,      17 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       1 ms 92.5th,       1 ms 95th,       1 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       2 ms 99.75th,       2 ms 99.9th,       2 ms 99.95th,       3 ms 99.99th.
2022-09-13 21:40:58 INFO Performance Recorder Exited
2022-09-13 21:40:58 INFO CQueuePerl Shutdown
2022-09-13 21:40:58 INFO Remove a session: 127.0.0.1:11211
2022-09-13 21:40:58 INFO Controller has been stopped.
2022-09-13 21:40:58 INFO SBK PrometheusLogger Shutdown
2022-09-13 21:40:59 INFO SBK Benchmark Shutdown


```
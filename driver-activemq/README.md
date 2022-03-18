<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Apache ActiveMQ Benchmarking with SBK
The SBK supports the end to end latency benchmarking of Apache ActiveMQ  distributed message queue with multiple writers and readers.

# Apache ActiveMQ benchmarking with Docker
Use the below command start the Apache ActiveMQ docker

```
docker run -p 61616:61616 -p 8161:8161 rmohr/activemq
```

The port is redirect the local host "27.0.0.1:61616"

you can access the ActiveMQ console via http://localhost:8161

An Example SBK command for end to end benchmarking single writer and single reader is as follows

```
 ./build/install/sbk/bin/sbk -class activemq -writers 1 -readers 1 -size 100  -seconds 60   
```
In the above example, the data size is 100 bytes.

example output:

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class activemq -writers 1 -readers 1 -size 100  -seconds 60        
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-03-18 15:42:14 INFO Reflections took 68 ms to scan 47 urls, producing 98 keys and 221 values 
2022-03-18 15:42:14 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-03-18 15:42:14 INFO Storage Benchmark Kit
2022-03-18 15:42:14 INFO SBK Version: 0.98
2022-03-18 15:42:14 INFO SBK Website: https://github.com/kmgowda/SBK
2022-03-18 15:42:14 INFO Arguments List: [-class, activemq, -writers, 1, -readers, 1, -size, 100, -seconds, 60]
2022-03-18 15:42:14 INFO Java Runtime Version: 17+35-LTS-2724
2022-03-18 15:42:14 INFO Storage Drivers Package: io.sbk
2022-03-18 15:42:14 INFO sbk.applicationName: sbk
2022-03-18 15:42:14 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2022-03-18 15:42:14 INFO sbk.className: 
2022-03-18 15:42:14 INFO '-class': activemq
2022-03-18 15:42:14 INFO Available Storage Drivers in package 'io.sbk': 41 [Activemq, 
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, 
Derby, FdbRecord, File, FileStream, FoundationDB, HDFS, Hive, Ignite, Jdbc, Kafka, 
LevelDB, MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, 
PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, 
SQLite]
2022-03-18 15:42:14 INFO Arguments to Driver 'Activemq' : [-writers, 1, -readers, 1, -size, 100, -seconds, 60]
2022-03-18 15:42:14 INFO Time Unit: MILLISECONDS
2022-03-18 15:42:14 INFO Minimum Latency: 0 ms
2022-03-18 15:42:14 INFO Maximum Latency: 180000 ms
2022-03-18 15:42:14 INFO Window Latency Store: Array, Size: 1 MB
2022-03-18 15:42:14 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-03-18 15:42:14 INFO Total Window Extension: None, Size: 0 MB
2022-03-18 15:42:14 INFO SBK Benchmark Started
2022-03-18 15:42:14 INFO SBK PrometheusLogger Started
2022-03-18 15:42:14 INFO CQueuePerl Start
2022-03-18 15:42:14 INFO Performance Recorder Started
2022-03-18 15:42:14 INFO SBK Benchmark initiated Writers
2022-03-18 15:42:14 INFO SBK Benchmark initiated Readers
2022-03-18 15:42:14 INFO Writer 0 started , run seconds: 60
2022-03-18 15:42:14 INFO Reader 0 started , run seconds: 60
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2556 records,       511.1 records/sec,     0.05 MB/sec,      1.1 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       5 ms 60th,       7 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2423 records,       484.4 records/sec,     0.05 MB/sec,      1.1 ms avg latency,      42 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  12; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       3 ms 60th,      42 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2378 records,       475.5 records/sec,     0.05 MB/sec,      1.1 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       4 ms 60th,       5 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2497 records,       499.1 records/sec,     0.05 MB/sec,      1.0 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       2 ms 50th,       4 ms 60th,       5 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2480 records,       495.9 records/sec,     0.05 MB/sec,      1.1 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   2; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       4 ms 60th,       4 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2594 records,       518.6 records/sec,     0.05 MB/sec,      1.1 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   2; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       3 ms 60th,       5 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2512 records,       502.3 records/sec,     0.05 MB/sec,      1.1 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   2; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       3 ms 60th,       4 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2434 records,       486.6 records/sec,     0.05 MB/sec,      1.1 ms avg latency,      43 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  12; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       4 ms 60th,      43 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2501 records,       500.0 records/sec,     0.05 MB/sec,      1.0 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       4 ms 60th,       5 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2487 records,       497.3 records/sec,     0.05 MB/sec,      1.1 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       5 ms 60th,      13 ms 70th.
Activemq Write_Reading     1 Writers,     1 Readers,      1 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             2527 records,       505.3 records/sec,     0.05 MB/sec,      1.1 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       4 ms 60th,       5 ms 70th.
2022-03-18 15:43:14 INFO Reader 0 exited
Activemq Write_Reading     1 Writers,     0 Readers,      1 Max Writers,     1 Max Readers,        4 seconds,         0.2 MB,             2491 records,       499.9 records/sec,     0.05 MB/sec,      1.0 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       4 ms 60th,       5 ms 70th.
2022-03-18 15:43:14 INFO Writer 0 exited
Total : Activemq Write_Reading     1 Writers,     0 Readers,      1 Max Writers,     1 Max Readers,       60 seconds,         2.8 MB,            29880 records,       498.0 records/sec,     0.05 MB/sec,      1.1 ms avg latency,      43 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       2 ms 40th,       2 ms 50th,       4 ms 60th,      33 ms 70th.
2022-03-18 15:43:14 INFO Performance Recorder Exited
2022-03-18 15:43:14 INFO CQueuePerl Shutdown
2022-03-18 15:43:14 INFO SBK PrometheusLogger Shutdown
2022-03-18 15:43:15 INFO SBK Benchmark Shutdown
```

To write the driver for ActiveMQ following examples are referred:
1. https://metamug.com/article/distributed-systems/jms-java-activemq-example-usage.html
2. https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/amazon-mq-working-java-example.html
3. https://examples.javacodegeeks.com/enterprise-java/jms/apache-activemq-hello-world-example

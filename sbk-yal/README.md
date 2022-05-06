<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SBK-YAL : YML Arguments Loader

[![Api](https://img.shields.io/badge/SBK--YAL-API-brightgreen)](https://kmgowda.github.io/SBK/sbk-yal/javadoc/index.html)

The SBK-YAL (YML Arguments Loader) is a wrapper for [SBK](https://github.com/kmgowda/SBK/tree/master/sbk).
The SBK-YAL extracts arguments from the YML file and invokes the SBK command with the same arguments as command
line parameters. you can store the SBK arguments in a YML file and can be given as input to SBK-YAL
application/command.
Use this example YML file [sbk-yal-file-read.yml](https://github.com/kmgowda/SBK/blob/master/sbk-yal/sbk-yal-file-read.yml)
to build your own YML file for SBK-YAL command/application.

## Build SBK-YAL
SBK-YAL is a submodule/project of the SBK framework. If you  [build SBK](./../README.md#build-sbk), it builds
the SBK-YAL package too.

## Running SBK-YAL locally
The standard help output with SBK-YAL parameters as follows

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk-yal                                    
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-05-06 14:42:45 INFO 
   _____   ____    _  __          __     __             _
  / ____| |  _ \  | |/ /          \ \   / /     /\     | |
 | (___   | |_) | | ' /   ______   \ \_/ /     /  \    | |
  \___ \  |  _ <  |  <   |______|   \   /     / /\ \   | |
  ____) | | |_) | | . \              | |     / ____ \  | |____
 |_____/  |____/  |_|\_\             |_|    /_/    \_\ |______|

2022-05-06 14:42:45 INFO Storage Benchmark Kit-YML Arguments Loader
2022-05-06 14:42:45 INFO SBK-YAL Version: 0.99
2022-05-06 14:42:45 INFO Arguments List: []
2022-05-06 14:42:45 INFO Java Runtime Version: 17.0.2+8
2022-05-06 14:42:45 ERROR java.io.FileNotFoundException: ./sbk.yml (No such file or directory)

usage: sbk-yal
Storage Benchmark Kit-YML Arguments Loader

 -f <arg>   SBK YAML file, default: ./sbk.yml
 -help      Help message

Please report issues at https://github.com/kmgowda/SBK

```
An Example output of SBK-YAL with 1 SBK file system benchmarking instances is as follows:

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk-yal -f ./sbk-yal/sbk-yal-file-read.yml 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-05-06 14:43:49 INFO 
   _____   ____    _  __          __     __             _
  / ____| |  _ \  | |/ /          \ \   / /     /\     | |
 | (___   | |_) | | ' /   ______   \ \_/ /     /  \    | |
  \___ \  |  _ <  |  <   |______|   \   /     / /\ \   | |
  ____) | | |_) | | . \              | |     / ____ \  | |____
 |_____/  |____/  |_|\_\             |_|    /_/    \_\ |______|

2022-05-06 14:43:49 INFO Storage Benchmark Kit-YML Arguments Loader
2022-05-06 14:43:49 INFO SBK-YAL Version: 0.99
2022-05-06 14:43:49 INFO Arguments List: [-f, ./sbk-yal/sbk-yal-file-read.yml]
2022-05-06 14:43:49 INFO Java Runtime Version: 17.0.2+8
2022-05-06 14:43:49 INFO Reflections took 73 ms to scan 48 urls, producing 98 keys and 214 values 
2022-05-06 14:43:49 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-05-06 14:43:49 INFO Storage Benchmark Kit
2022-05-06 14:43:49 INFO SBK Version: 0.99
2022-05-06 14:43:49 INFO SBK Website: https://github.com/kmgowda/SBK
2022-05-06 14:43:49 INFO Arguments List: [-class, file, -readers, 1, -size, 10, -time, ns, -seconds, 60]
2022-05-06 14:43:49 INFO Java Runtime Version: 17.0.2+8
2022-05-06 14:43:49 INFO Storage Drivers Package: io.sbk
2022-05-06 14:43:49 INFO sbk.applicationName: sbk
2022-05-06 14:43:49 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2022-05-06 14:43:49 INFO sbk.className: 
2022-05-06 14:43:49 INFO '-class': file
2022-05-06 14:43:49 INFO Available Storage Drivers in package 'io.sbk': 41 [Activemq, 
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, 
Derby, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive, Jdbc, Kafka, LevelDB, 
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, 
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-05-06 14:43:49 INFO Arguments to Driver 'File' : [-readers, 1, -size, 10, -time, ns, -seconds, 60]
2022-05-06 14:43:50 INFO Time Unit: NANOSECONDS
2022-05-06 14:43:50 INFO Minimum Latency: 0 ns
2022-05-06 14:43:50 INFO Maximum Latency: 180000000000 ns
2022-05-06 14:43:50 INFO Window Latency Store: HashMap, Size: 192 MB
2022-05-06 14:43:50 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-05-06 14:43:50 INFO Total Window Extension: None, Size: 0 MB
2022-05-06 14:43:50 INFO SBK Benchmark Started
2022-05-06 14:43:50 INFO SBK PrometheusLogger Started
2022-05-06 14:43:50 INFO Synchronous File Reader initiated !
2022-05-06 14:43:50 INFO CQueuePerl Start
2022-05-06 14:43:50 INFO Performance Recorder Started
2022-05-06 14:43:50 INFO SBK Benchmark initiated Readers
2022-05-06 14:43:50 INFO Reader 0 started , run seconds: 60
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        54.2 MB,          5686694 records,   1137078.8 records/sec,    10.84 MB/sec,    801.9 ns avg latency, 2511775 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  16; Latency Percentiles:     696 ns 10th,     706 ns 20th,     720 ns 25th,     728 ns 30th,     844 ns 40th,    1043 ns 50th,   22521 ns 60th,   35694 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        55.1 MB,          5782374 records,   1156014.7 records/sec,    11.02 MB/sec,    797.1 ns avg latency, 3077230 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  17; Latency Percentiles:     714 ns 10th,     718 ns 20th,     722 ns 25th,     727 ns 30th,     782 ns 40th,     902 ns 50th,   22961 ns 60th,   37340 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        55.6 MB,          5831593 records,   1166085.2 records/sec,    11.12 MB/sec,    790.1 ns avg latency, 2281053 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  16; Latency Percentiles:     688 ns 10th,     704 ns 20th,     721 ns 25th,     729 ns 30th,     775 ns 40th,     889 ns 50th,   22259 ns 60th,   34524 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        55.2 MB,          5792752 records,   1158317.9 records/sec,    11.05 MB/sec,    794.3 ns avg latency, 2818543 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  16; Latency Percentiles:     701 ns 10th,     716 ns 20th,     724 ns 25th,     731 ns 30th,     772 ns 40th,    1014 ns 50th,   22429 ns 60th,   35704 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.1 MB,          5982293 records,   1196111.3 records/sec,    11.41 MB/sec,    768.3 ns avg latency, 2312232 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  16; Latency Percentiles:     679 ns 10th,     693 ns 20th,     708 ns 25th,     724 ns 30th,     743 ns 40th,     863 ns 50th,   21082 ns 60th,   34642 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        55.0 MB,          5766808 records,   1153130.5 records/sec,    11.00 MB/sec,    797.3 ns avg latency, 2549265 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  16; Latency Percentiles:     702 ns 10th,     712 ns 20th,     724 ns 25th,     732 ns 30th,     778 ns 40th,     884 ns 50th,   22433 ns 60th,   35431 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.0 MB,          5978686 records,   1195497.2 records/sec,    11.40 MB/sec,    769.2 ns avg latency, 2331869 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  16; Latency Percentiles:     688 ns 10th,     695 ns 20th,     709 ns 25th,     724 ns 30th,     746 ns 40th,     858 ns 50th,   21133 ns 60th,   33998 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.7 MB,          6048825 records,   1209523.0 records/sec,    11.53 MB/sec,    760.8 ns avg latency, 2182350 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  15; Latency Percentiles:     676 ns 10th,     683 ns 20th,     701 ns 25th,     722 ns 30th,     741 ns 40th,     859 ns 50th,   20690 ns 60th,   33683 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        59.4 MB,          6231198 records,   1245990.3 records/sec,    11.88 MB/sec,    737.5 ns avg latency, 1778275 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  15; Latency Percentiles:     675 ns 10th,     678 ns 20th,     688 ns 25th,     700 ns 30th,     721 ns 40th,     864 ns 50th,   20172 ns 60th,   30119 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        59.2 MB,          6205478 records,   1240846.4 records/sec,    11.83 MB/sec,    741.3 ns avg latency, 2013790 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  15; Latency Percentiles:     675 ns 10th,     678 ns 20th,     685 ns 25th,     698 ns 30th,     724 ns 40th,     824 ns 50th,   20324 ns 60th,   30547 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        59.6 MB,          6247690 records,   1249287.6 records/sec,    11.91 MB/sec,    736.2 ns avg latency, 1999974 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  14; Latency Percentiles:     675 ns 10th,     678 ns 20th,     683 ns 25th,     699 ns 30th,     719 ns 40th,     799 ns 50th,   20242 ns 60th,   29033 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        4 seconds,        59.3 MB,          6222522 records,   1247648.8 records/sec,    11.90 MB/sec,    737.4 ns avg latency, 2248868 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  15; Latency Percentiles:     675 ns 10th,     678 ns 20th,     685 ns 25th,     696 ns 30th,     718 ns 40th,     802 ns 50th,   20239 ns 60th,   29988 ns 70th.
2022-05-06 14:44:50 INFO Reader 0 exited
Total : File Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,       60 seconds,       684.5 MB,         71776913 records,   1196281.9 records/sec,    11.41 MB/sec,    768.4 ns avg latency, 3077230 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  15; Latency Percentiles:     677 ns 10th,     687 ns 20th,     708 ns 25th,     724 ns 30th,     757 ns 40th,     884 ns 50th,   20863 ns 60th,   33595 ns 70th.
2022-05-06 14:44:50 INFO Performance Recorder Exited
2022-05-06 14:44:50 INFO CQueuePerl Shutdown
2022-05-06 14:44:50 INFO SBK PrometheusLogger Shutdown
2022-05-06 14:44:51 INFO SBK Benchmark Shutdown


```


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
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-07-21 11:56:00 INFO 
   _____   ____    _  __          __     __             _
  / ____| |  _ \  | |/ /          \ \   / /     /\     | |
 | (___   | |_) | | ' /   ______   \ \_/ /     /  \    | |
  \___ \  |  _ <  |  <   |______|   \   /     / /\ \   | |
  ____) | | |_) | | . \              | |     / ____ \  | |____
 |_____/  |____/  |_|\_\             |_|    /_/    \_\ |______|

2022-07-21 11:56:00 INFO Storage Benchmark Kit-YML Arguments Loader
2022-07-21 11:56:00 INFO SBK-YAL Version: 0.992
2022-07-21 11:56:00 INFO Arguments List: []
2022-07-21 11:56:00 INFO Java Runtime Version: 17.0.2+8
2022-07-21 11:56:00 ERROR java.io.FileNotFoundException: ./sbk.yml (No such file or directory)

usage: sbk-yal
Storage Benchmark Kit-YML Arguments Loader

 -f <arg>   SBK YAML file, default: ./sbk.yml
 -help      Help message
 -p         Print SBK Options Help Text

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
2022-05-07 21:27:30 INFO 
   _____   ____    _  __          __     __             _
  / ____| |  _ \  | |/ /          \ \   / /     /\     | |
 | (___   | |_) | | ' /   ______   \ \_/ /     /  \    | |
  \___ \  |  _ <  |  <   |______|   \   /     / /\ \   | |
  ____) | | |_) | | . \              | |     / ____ \  | |____
 |_____/  |____/  |_|\_\             |_|    /_/    \_\ |______|

2022-05-07 21:27:30 INFO Storage Benchmark Kit-YML Arguments Loader
2022-05-07 21:27:30 INFO SBK-YAL Version: 0.99
2022-05-07 21:27:30 INFO Arguments List: [-f, ./sbk-yal/sbk-yal-file-read.yml]
2022-05-07 21:27:30 INFO Java Runtime Version: 17.0.2+8
2022-05-07 21:27:30 INFO Reflections took 72 ms to scan 48 urls, producing 101 keys and 216 values 
2022-05-07 21:27:30 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-05-07 21:27:30 INFO Storage Benchmark Kit
2022-05-07 21:27:30 INFO SBK Version: 0.99
2022-05-07 21:27:30 INFO SBK Website: https://github.com/kmgowda/SBK
2022-05-07 21:27:30 INFO Arguments List: [-class, file, -readers, 1, -size, 10, -time, ns, -seconds, 60]
2022-05-07 21:27:30 INFO Java Runtime Version: 17.0.2+8
2022-05-07 21:27:30 INFO Storage Drivers Package: io.sbk
2022-05-07 21:27:30 INFO sbk.applicationName: sbk
2022-05-07 21:27:30 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2022-05-07 21:27:30 INFO sbk.className: 
2022-05-07 21:27:30 INFO '-class': file
2022-05-07 21:27:30 INFO Available Storage Drivers in package 'io.sbk': 41 [Activemq, 
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, 
Derby, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive, Jdbc, Kafka, LevelDB, 
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, 
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-05-07 21:27:30 INFO Arguments to Driver 'File' : [-readers, 1, -size, 10, -time, ns, -seconds, 60]
2022-05-07 21:27:30 INFO Time Unit: NANOSECONDS
2022-05-07 21:27:30 INFO Minimum Latency: 0 ns
2022-05-07 21:27:30 INFO Maximum Latency: 180000000000 ns
2022-05-07 21:27:30 INFO Window Latency Store: HashMap, Size: 192 MB
2022-05-07 21:27:30 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-05-07 21:27:30 INFO Total Window Extension: None, Size: 0 MB
2022-05-07 21:27:30 INFO SBK Benchmark Started
2022-05-07 21:27:31 INFO SBK PrometheusLogger Started
2022-05-07 21:27:31 INFO Synchronous File Reader initiated !
2022-05-07 21:27:31 INFO CQueuePerl Start
2022-05-07 21:27:31 INFO Performance Recorder Started
2022-05-07 21:27:31 INFO SBK Benchmark initiated Readers
2022-05-07 21:27:31 INFO Reader 0 started , run seconds: 60
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        49.4 MB,          5182144 records,   1035974.0 records/sec,     9.88 MB/sec,    884.7 ns avg latency, 2931253 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     709 ns 10th,     718 ns 20th,     721 ns 25th,     722 ns 30th,     725 ns 40th,     728 ns 50th,     740 ns 60th,     762 ns 70th,     789 ns 75th,     863 ns 80th,    1188 ns 90th,    1240 ns 92.5th,    1283 ns 95th,    1344 ns 97.5th,    1659 ns 99th,    1751 ns 99.25th,    2074 ns 99.5th,    6821 ns 99.75th,   22481 ns 99.9th,   26599 ns 99.95th,   40410 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        54.1 MB,          5673612 records,   1134495.2 records/sec,    10.82 MB/sec,    818.7 ns avg latency, 2372472 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     708 ns 10th,     711 ns 20th,     714 ns 25th,     719 ns 30th,     723 ns 40th,     725 ns 50th,     728 ns 60th,     732 ns 70th,     739 ns 75th,     757 ns 80th,     797 ns 90th,     846 ns 92.5th,     925 ns 95th,    1225 ns 97.5th,    1312 ns 99th,    1337 ns 99.25th,    1396 ns 99.5th,   14969 ns 99.75th,   23392 ns 99.9th,   28415 ns 99.95th,   40855 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        52.7 MB,          5527823 records,   1105204.7 records/sec,    10.54 MB/sec,    839.2 ns avg latency, 2347559 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     713 ns 10th,     723 ns 20th,     725 ns 25th,     726 ns 30th,     729 ns 40th,     732 ns 50th,     744 ns 60th,     762 ns 70th,     766 ns 75th,     777 ns 80th,     854 ns 90th,     868 ns 92.5th,     876 ns 95th,     922 ns 97.5th,    1211 ns 99th,    1281 ns 99.25th,    1421 ns 99.5th,   17191 ns 99.75th,   23938 ns 99.9th,   28252 ns 99.95th,   37437 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        56.6 MB,          5935858 records,   1186933.9 records/sec,    11.32 MB/sec,    781.3 ns avg latency, 1960600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:     709 ns 10th,     711 ns 20th,     712 ns 25th,     713 ns 30th,     717 ns 40th,     724 ns 50th,     727 ns 60th,     729 ns 70th,     730 ns 75th,     732 ns 80th,     744 ns 90th,     750 ns 92.5th,     769 ns 95th,     810 ns 97.5th,     885 ns 99th,     928 ns 99.25th,    1089 ns 99.5th,    2557 ns 99.75th,   22134 ns 99.9th,   27407 ns 99.95th,   37431 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        54.7 MB,          5736284 records,   1147027.2 records/sec,    10.94 MB/sec,    808.8 ns avg latency, 2249077 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     713 ns 10th,     723 ns 20th,     724 ns 25th,     725 ns 30th,     727 ns 40th,     729 ns 50th,     731 ns 60th,     735 ns 70th,     739 ns 75th,     744 ns 80th,     766 ns 90th,     775 ns 92.5th,     795 ns 95th,     848 ns 97.5th,     934 ns 99th,    1053 ns 99.25th,    1239 ns 99.5th,   15767 ns 99.75th,   23712 ns 99.9th,   28540 ns 99.95th,   39064 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        55.1 MB,          5772913 records,   1154351.7 records/sec,    11.01 MB/sec,    803.1 ns avg latency, 2010629 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     712 ns 10th,     722 ns 20th,     724 ns 25th,     725 ns 30th,     727 ns 40th,     729 ns 50th,     730 ns 60th,     733 ns 70th,     736 ns 75th,     741 ns 80th,     764 ns 90th,     771 ns 92.5th,     788 ns 95th,     842 ns 97.5th,     912 ns 99th,     977 ns 99.25th,    1184 ns 99.5th,   15371 ns 99.75th,   23617 ns 99.9th,   28755 ns 99.95th,   39139 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        55.3 MB,          5798350 records,   1159437.9 records/sec,    11.06 MB/sec,    799.6 ns avg latency, 2111391 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     714 ns 10th,     724 ns 20th,     725 ns 25th,     726 ns 30th,     727 ns 40th,     729 ns 50th,     731 ns 60th,     734 ns 70th,     737 ns 75th,     741 ns 80th,     764 ns 90th,     773 ns 92.5th,     794 ns 95th,     852 ns 97.5th,     922 ns 99th,     984 ns 99.25th,    1183 ns 99.5th,   14967 ns 99.75th,   23165 ns 99.9th,   28220 ns 99.95th,   38596 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        53.6 MB,          5617297 records,   1123233.2 records/sec,    10.71 MB/sec,    826.5 ns avg latency, 2804847 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     721 ns 10th,     725 ns 20th,     726 ns 25th,     727 ns 30th,     729 ns 40th,     731 ns 50th,     735 ns 60th,     747 ns 70th,     760 ns 75th,     764 ns 80th,     791 ns 90th,     824 ns 92.5th,     853 ns 95th,     879 ns 97.5th,    1083 ns 99th,    1212 ns 99.25th,    1356 ns 99.5th,   16755 ns 99.75th,   24086 ns 99.9th,   28512 ns 99.95th,   38912 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        53.6 MB,          5620125 records,   1123800.2 records/sec,    10.72 MB/sec,    825.0 ns avg latency, 2235390 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     724 ns 10th,     726 ns 20th,     727 ns 25th,     727 ns 30th,     729 ns 40th,     731 ns 50th,     734 ns 60th,     741 ns 70th,     746 ns 75th,     758 ns 80th,     780 ns 90th,     795 ns 92.5th,     831 ns 95th,     871 ns 97.5th,     967 ns 99th,    1114 ns 99.25th,    1324 ns 99.5th,   16893 ns 99.75th,   24330 ns 99.9th,   28601 ns 99.95th,   39121 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        54.6 MB,          5722638 records,   1144298.7 records/sec,    10.91 MB/sec,    810.4 ns avg latency, 2483173 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     711 ns 10th,     718 ns 20th,     723 ns 25th,     725 ns 30th,     727 ns 40th,     729 ns 50th,     731 ns 60th,     737 ns 70th,     743 ns 75th,     757 ns 80th,     781 ns 90th,     805 ns 92.5th,     858 ns 95th,     876 ns 97.5th,     956 ns 99th,    1098 ns 99.25th,    1262 ns 99.5th,   15407 ns 99.75th,   23387 ns 99.9th,   28073 ns 99.95th,   38128 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        56.7 MB,          5944861 records,   1188734.3 records/sec,    11.34 MB/sec,    779.3 ns avg latency, 1858225 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:     709 ns 10th,     711 ns 20th,     712 ns 25th,     714 ns 30th,     718 ns 40th,     724 ns 50th,     727 ns 60th,     729 ns 70th,     730 ns 75th,     732 ns 80th,     744 ns 90th,     750 ns 92.5th,     768 ns 95th,     808 ns 97.5th,     887 ns 99th,     930 ns 99.25th,    1095 ns 99.5th,    2209 ns 99.75th,   21848 ns 99.9th,   27159 ns 99.95th,   38170 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        4 seconds,        56.5 MB,          5920837 records,   1187215.2 records/sec,    11.32 MB/sec,    781.4 ns avg latency, 1839054 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:     709 ns 10th,     711 ns 20th,     713 ns 25th,     714 ns 30th,     719 ns 40th,     724 ns 50th,     727 ns 60th,     729 ns 70th,     731 ns 75th,     733 ns 80th,     743 ns 90th,     749 ns 92.5th,     766 ns 95th,     800 ns 97.5th,     880 ns 99th,     925 ns 99.25th,    1087 ns 99.5th,    2337 ns 99.75th,   22000 ns 99.9th,   27545 ns 99.95th,   37277 ns 99.99th.
2022-05-07 21:28:31 INFO Reader 0 exited
Total : File Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,       60 seconds,       652.8 MB,         68452742 records,   1140879.0 records/sec,    10.88 MB/sec,    812.1 ns avg latency, 2931253 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     710 ns 10th,     716 ns 20th,     721 ns 25th,     724 ns 30th,     726 ns 40th,     728 ns 50th,     730 ns 60th,     735 ns 70th,     740 ns 75th,     748 ns 80th,     778 ns 90th,     806 ns 92.5th,     853 ns 95th,     935 ns 97.5th,    1267 ns 99th,    1305 ns 99.25th,    1419 ns 99.5th,   15241 ns 99.75th,   23322 ns 99.9th,   28060 ns 99.95th,   38688 ns 99.99th.
2022-05-07 21:28:31 INFO Performance Recorder Exited
2022-05-07 21:28:31 INFO CQueuePerl Shutdown
2022-05-07 21:28:31 INFO SBK PrometheusLogger Shutdown
2022-05-07 21:28:32 INFO SBK Benchmark Shutdown



```


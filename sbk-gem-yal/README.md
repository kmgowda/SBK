<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SBK-GEM-YAL : YML Arguments Loader

[![Api](https://img.shields.io/badge/SBK--GEM--YAL-API-brightgreen)](https://kmgowda.github.io/SBK/sbk-gem-yal/javadoc/index.html)

The SBK-GEM-YAL (YML Arguments Loader) is a wrapper for [SBK-GEM](https://github.com/kmgowda/SBK/tree/master/sbk-gem).
The SBK-GEM-YAL extracts arguments from the YML file and invokes the SBK-GEM command with the same arguments as command 
line parameters. you can store the SBK-GEM arguments in a YML file and can be given as input to SBK-GEM-YAL 
application/command. 
Use this example YML file [sbk-gem.yml](https://github.com/kmgowda/SBK/blob/master/sbk-gem-yal/sbk-gem.yml) to build 
your own YML file for SBK-GEM-YAL command/application.

## Build SBK-GEM-YAL
SBK-GEM-YAL is a submodule/project of the SBK framework. If you  [build SBK](./../README.md#build-sbk), it builds 
the SBK-GEM-YAL package too.

## Running SBK-GEM-YAL locally
The standard help output with SBK-GEM-YAL parameters as follows

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk-gem-yal                     
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-09-07 15:24:31 INFO 
   _____   ____    _  __            _____   ______   __  __           __     __             _
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |          \ \   / /     /\     | |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |  ______   \ \_/ /     /  \    | |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| | |______|   \   /     / /\ \   | |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |             | |     / ____ \  | |____
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|             |_|    /_/    \_\ |______|


2021-09-07 15:24:31 INFO SBK-GEM-YML Arguments Loader
2021-09-07 15:24:31 INFO SBK-GEM-YAL Version: 0.92
2021-09-07 15:24:31 INFO Arguments List: []
2021-09-07 15:24:31 INFO Java Runtime Version: 11.0.8+11
2021-09-07 15:24:31 ERROR java.io.FileNotFoundException: ./sbk-gem.yml (No such file or directory)

usage: sbk-gem-yal
SBK-GEM-YML Arguments Loader

 -f <arg>   SBK-GEM YAML file, default: ./sbk-gem.yml
 -help      Help message

Please report issues at https://github.com/kmgowda/SBK

```
An Example output of SBK-GEM-YAL with 1 SBK file system benchmarking instances is as follows:

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk-gem-yal  -f ./sbk-gem-1.yaml
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-09-07 15:25:22 INFO 
   _____   ____    _  __            _____   ______   __  __           __     __             _
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |          \ \   / /     /\     | |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |  ______   \ \_/ /     /  \    | |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| | |______|   \   /     / /\ \   | |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |             | |     / ____ \  | |____
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|             |_|    /_/    \_\ |______|


2021-09-07 15:25:22 INFO SBK-GEM-YML Arguments Loader
2021-09-07 15:25:22 INFO SBK-GEM-YAL Version: 0.92
2021-09-07 15:25:22 INFO Arguments List: [-f, ./sbk-gem-1.yaml]
2021-09-07 15:25:22 INFO Java Runtime Version: 11.0.8+11
2021-09-07 15:25:22 INFO Reflections took 75 ms to scan 44 urls, producing 109 keys and 251 values 
2021-09-07 15:25:22 INFO 
   _____   ____    _  __            _____   ______   __  __
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|

2021-09-07 15:25:22 INFO Storage Benchmark Kit - Group Execution Monitor
2021-09-07 15:25:22 INFO SBK-GEM Version: 0.92
2021-09-07 15:25:22 INFO SBK-GEM Website: https://github.com/kmgowda/SBK
2021-09-07 15:25:22 INFO Arguments List: [-nodes, localhost, -class, file, -readers, 1, -size, 10, -time, ns, -seconds, 60, -gemuser, kmg, -gempass, Laki@2322, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk]
2021-09-07 15:25:22 INFO Java Runtime Version: 11.0.8+11
2021-09-07 15:25:22 INFO Storage Drivers Package: io.sbk
2021-09-07 15:25:22 INFO sbk.applicationName: sbk
2021-09-07 15:25:22 INFO sbk.className: 
2021-09-07 15:25:22 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2021-09-07 15:25:22 INFO '-class': file
2021-09-07 15:25:22 INFO Available Storage Drivers in package 'io.sbk': 40 [Artemis, 
AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, Derby, 
FdbRecord, File, FileStream, FoundationDB, HDFS, Hive, Ignite, Jdbc, Kafka, LevelDB, 
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, 
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2021-09-07 15:25:22 INFO SBK-GEM [1]: Arguments to process : [-nodes, localhost, -readers, 1, -size, 10, -time, ns, -seconds, 60, -gemuser, kmg, -gempass, Laki@2322, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk]
2021-09-07 15:25:22 INFO Time Unit: NANOSECONDS
2021-09-07 15:25:22 INFO Minimum Latency: 0 ns
2021-09-07 15:25:22 INFO Maximum Latency: 180000000000 ns
2021-09-07 15:25:22 INFO SBK dir: /Users/kmg/projects/SBK/build/install/sbk
2021-09-07 15:25:22 INFO SBK command: bin/sbk
2021-09-07 15:25:22 INFO Arguments to remote SBK command: -class file -readers 1 -size 10 -seconds 60 -time ns -minlatency 0 -maxlatency 180000000000 -context no -ram kmgs-MBP.Dlink -ramport 9716
2021-09-07 15:25:22 INFO SBK-GEM: Arguments to remote SBK command verification Success..
2021-09-07 15:25:22 INFO Arguments to  SBK-RAM: [-class, file, -action, r, -max, 1]
2021-09-07 15:25:22 INFO SBK-GEM: Arguments to SBK-RAM command verification Success..
2021-09-07 15:25:22 INFO Window Latency Store: HashMap, Size: 512 MB
2021-09-07 15:25:22 INFO Total Window Latency Store: HashMap, Size: 1024 MB
2021-09-07 15:25:22 INFO Total Window Extension: None, Size: 0 MB
2021-09-07 15:25:22 INFO getOrCreateProvider(BC) created instance of org.bouncycastle.jce.provider.BouncyCastleProvider
2021-09-07 15:25:22 INFO getOrCreateProvider(EdDSA) created instance of net.i2p.crypto.eddsa.EdDSASecurityProvider
2021-09-07 15:25:23 INFO SBK GEM Benchmark Started
2021-09-07 15:25:23 INFO SBK-GEM: Ssh Connection to host 'localhost' starting...
2021-09-07 15:25:23 INFO Using MinaServiceFactoryFactory
2021-09-07 15:25:23 WARN Server at localhost/127.0.0.1:22 presented unverified EC key: SHA256:6kTtY0SqflZ/04wvcClPuoe9ZRxaVyeakHBYuVfTkSg
2021-09-07 15:25:23 INFO SBK-GEM: Ssh Connection to host 'localhost' Success.
2021-09-07 15:25:23 INFO SBK-GEM: Ssh session establishment Success..
2021-09-07 15:25:23 INFO SBK-GEM: Matching Java Major Version: 11 Success..
2021-09-07 15:25:23 INFO SBK-GEM: Removing the remote directory: 'sbk'  Success..
2021-09-07 15:25:23 INFO SBK-GEM: Creating remote directory: 'sbk'  Success..
2021-09-07 15:25:28 INFO Copy SBK application: 'bin/sbk' to remote nodes Success..
2021-09-07 15:25:28 INFO SBK RAM Benchmark Started
2021-09-07 15:25:28 INFO SBK PrometheusLogger Started
2021-09-07 15:25:28 INFO SBK Connections PrometheusLogger Started
2021-09-07 15:25:28 INFO LatenciesRecord Benchmark Started
2021-09-07 15:25:28 INFO SBK-GEM: Remote SBK command: sbk/bin/sbk -class file -readers 1 -size 10 -seconds 60 -time ns -minlatency 0 -maxlatency 180000000000 -context no -ram kmgs-MBP.Dlink -ramport 9716
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     0 Readers,      0 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,                0 records,         0.0 records/sec,     0.00 MB/sec,      0.0 ns avg latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ns 10th,       0 ns 20th,       0 ns 25th,       0 ns 30th,       0 ns 40th,       0 ns 50th,       0 ns 60th,       0 ns 70th,       0 ns 75th,       0 ns 80th,       0 ns 90th,       0 ns 92.5th,       0 ns 95th,       0 ns 97.5th,       0 ns 99th,       0 ns 99.25th,       0 ns 99.5th,       0 ns 99.75th,       0 ns 99.9th,       0 ns 99.95th,       0 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        58.4 MB,          6125815 records,   1222144.8 records/sec,    11.66 MB/sec,    740.6 ns avg latency, 3792770 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:     644 ns 10th,     653 ns 20th,     656 ns 25th,     659 ns 30th,     665 ns 40th,     670 ns 50th,     676 ns 60th,     685 ns 70th,     690 ns 75th,     697 ns 80th,     721 ns 90th,     742 ns 92.5th,    1026 ns 95th,    1120 ns 97.5th,    1211 ns 99th,    1256 ns 99.25th,    1414 ns 99.5th,    2642 ns 99.75th,   19067 ns 99.9th,   21171 ns 99.95th,   28638 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        61.7 MB,          6473299 records,   1291649.7 records/sec,    12.32 MB/sec,    709.0 ns avg latency, 2626021 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     643 ns 10th,     650 ns 20th,     653 ns 25th,     656 ns 30th,     661 ns 40th,     667 ns 50th,     672 ns 60th,     678 ns 70th,     682 ns 75th,     686 ns 80th,     700 ns 90th,     705 ns 92.5th,     717 ns 95th,     751 ns 97.5th,     801 ns 99th,     828 ns 99.25th,     906 ns 99.5th,    1198 ns 99.75th,   17228 ns 99.9th,   20582 ns 99.95th,   27895 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        62.0 MB,          6503578 records,   1299929.2 records/sec,    12.40 MB/sec,    704.9 ns avg latency, 1856897 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     652 ns 20th,     656 ns 25th,     658 ns 30th,     663 ns 40th,     669 ns 50th,     673 ns 60th,     678 ns 70th,     681 ns 75th,     684 ns 80th,     696 ns 90th,     701 ns 92.5th,     708 ns 95th,     721 ns 97.5th,     785 ns 99th,     812 ns 99.25th,     895 ns 99.5th,    1139 ns 99.75th,   15787 ns 99.9th,   20478 ns 99.95th,   28087 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        61.7 MB,          6470860 records,   1291107.7 records/sec,    12.31 MB/sec,    708.4 ns avg latency, 1453392 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     646 ns 10th,     655 ns 20th,     658 ns 25th,     660 ns 30th,     665 ns 40th,     670 ns 50th,     675 ns 60th,     680 ns 70th,     683 ns 75th,     686 ns 80th,     696 ns 90th,     700 ns 92.5th,     705 ns 95th,     715 ns 97.5th,     781 ns 99th,     811 ns 99.25th,     922 ns 99.5th,    1182 ns 99.75th,   16817 ns 99.9th,   20696 ns 99.95th,   30629 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        62.2 MB,          6525371 records,   1303205.3 records/sec,    12.43 MB/sec,    702.6 ns avg latency, 1265571 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     652 ns 20th,     656 ns 25th,     659 ns 30th,     664 ns 40th,     669 ns 50th,     673 ns 60th,     678 ns 70th,     682 ns 75th,     685 ns 80th,     697 ns 90th,     701 ns 92.5th,     707 ns 95th,     717 ns 97.5th,     764 ns 99th,     794 ns 99.25th,     885 ns 99.5th,    1110 ns 99.75th,   15237 ns 99.9th,   20392 ns 99.95th,   27489 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        62.3 MB,          6530842 records,   1303674.0 records/sec,    12.43 MB/sec,    702.6 ns avg latency, 1250411 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     653 ns 20th,     656 ns 25th,     659 ns 30th,     664 ns 40th,     669 ns 50th,     674 ns 60th,     679 ns 70th,     682 ns 75th,     686 ns 80th,     696 ns 90th,     700 ns 92.5th,     705 ns 95th,     715 ns 97.5th,     755 ns 99th,     788 ns 99.25th,     875 ns 99.5th,    1113 ns 99.75th,   15102 ns 99.9th,   20377 ns 99.95th,   27847 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        62.1 MB,          6509308 records,   1299069.4 records/sec,    12.39 MB/sec,    704.7 ns avg latency, 1199798 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     653 ns 20th,     657 ns 25th,     659 ns 30th,     664 ns 40th,     670 ns 50th,     674 ns 60th,     679 ns 70th,     683 ns 75th,     686 ns 80th,     697 ns 90th,     702 ns 92.5th,     707 ns 95th,     730 ns 97.5th,     853 ns 99th,     865 ns 99.25th,     939 ns 99.5th,    1163 ns 99.75th,   15270 ns 99.9th,   20413 ns 99.95th,   27873 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        61.9 MB,          6493877 records,   1297288.7 records/sec,    12.37 MB/sec,    705.6 ns avg latency, 1302912 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     652 ns 20th,     656 ns 25th,     658 ns 30th,     663 ns 40th,     669 ns 50th,     673 ns 60th,     678 ns 70th,     681 ns 75th,     685 ns 80th,     695 ns 90th,     700 ns 92.5th,     704 ns 95th,     713 ns 97.5th,     763 ns 99th,     795 ns 99.25th,     884 ns 99.5th,    1150 ns 99.75th,   17035 ns 99.9th,   20490 ns 99.95th,   27988 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        62.1 MB,          6512990 records,   1302191.1 records/sec,    12.42 MB/sec,    704.2 ns avg latency, 1265876 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     652 ns 20th,     656 ns 25th,     659 ns 30th,     663 ns 40th,     669 ns 50th,     673 ns 60th,     679 ns 70th,     682 ns 75th,     686 ns 80th,     697 ns 90th,     701 ns 92.5th,     706 ns 95th,     716 ns 97.5th,     766 ns 99th,     799 ns 99.25th,     878 ns 99.5th,    1124 ns 99.75th,   15429 ns 99.9th,   20456 ns 99.95th,   28095 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        61.6 MB,          6459316 records,   1291471.8 records/sec,    12.32 MB/sec,    709.8 ns avg latency, 1167192 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     646 ns 10th,     653 ns 20th,     657 ns 25th,     660 ns 30th,     666 ns 40th,     671 ns 50th,     676 ns 60th,     683 ns 70th,     687 ns 75th,     691 ns 80th,     703 ns 90th,     706 ns 92.5th,     710 ns 95th,     720 ns 97.5th,     788 ns 99th,     816 ns 99.25th,     954 ns 99.5th,    1225 ns 99.75th,   17047 ns 99.9th,   20564 ns 99.95th,   27861 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        61.9 MB,          6493556 records,   1296092.4 records/sec,    12.36 MB/sec,    705.9 ns avg latency, 1251746 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     653 ns 20th,     657 ns 25th,     659 ns 30th,     664 ns 40th,     670 ns 50th,     674 ns 60th,     680 ns 70th,     684 ns 75th,     688 ns 80th,     700 ns 90th,     703 ns 92.5th,     708 ns 95th,     717 ns 97.5th,     768 ns 99th,     798 ns 99.25th,     880 ns 99.5th,    1134 ns 99.75th,   16251 ns 99.9th,   20455 ns 99.95th,   27837 ns 99.99th.
Sbk-Ram     0 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        2 seconds,        61.7 MB,          6473163 records,   2439372.0 records/sec,    23.26 MB/sec,    706.2 ns avg latency, 1201935 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     650 ns 20th,     654 ns 25th,     657 ns 30th,     662 ns 40th,     668 ns 50th,     672 ns 60th,     676 ns 70th,     679 ns 75th,     683 ns 80th,     695 ns 90th,     700 ns 92.5th,     706 ns 95th,     717 ns 97.5th,     783 ns 99th,     810 ns 99.25th,     915 ns 99.5th,    1151 ns 99.75th,   17301 ns 99.9th,   20577 ns 99.95th,   27801 ns 99.99th.
Total : Sbk-Ram     0 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,       62 seconds,       739.8 MB,         77571975 records,   1236234.1 records/sec,    11.79 MB/sec,    708.6 ns avg latency, 3792770 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     645 ns 10th,     652 ns 20th,     656 ns 25th,     659 ns 30th,     664 ns 40th,     669 ns 50th,     674 ns 60th,     679 ns 70th,     683 ns 75th,     687 ns 80th,     699 ns 90th,     703 ns 92.5th,     710 ns 95th,     728 ns 97.5th,     853 ns 99th,    1019 ns 99.25th,    1108 ns 99.5th,    1284 ns 99.75th,   16554 ns 99.9th,   20504 ns 99.95th,   28025 ns 99.99th.
2021-09-07 15:26:31 INFO LatenciesRecord Benchmark Shutdown
2021-09-07 15:26:31 INFO SBK PrometheusLogger Shutdown
2021-09-07 15:26:31 INFO SBK RAM Benchmark Shutdown
2021-09-07 15:26:31 INFO SBK GEM Benchmark Shutdown

SBK-GEM Remote Results
--------------------------------------------------------------------------------

Host 1: localhost, return code: 0
--------------------------------------------------------------------------------

```
Note that at the results, The SBK-GEM prints the return code of each of the remote host.

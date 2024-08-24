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
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk-gem-yal                                                                  
2024-08-24 17:06:31 INFO 
   _____   ____    _  __            _____   ______   __  __           __     __             _
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |          \ \   / /     /\     | |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |  ______   \ \_/ /     /  \    | |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| | |______|   \   /     / /\ \   | |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |             | |     / ____ \  | |____
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|             |_|    /_/    \_\ |______|


2024-08-24 17:06:31 INFO Storage Benchmark Kit-Group Execution Monitor-YML Arguments Loader
2024-08-24 17:06:31 INFO SBK-GEM-YAL Version: 5.3
2024-08-24 17:06:31 INFO Arguments List: []
2024-08-24 17:06:31 INFO Java Runtime Version: 17.0.2+8
2024-08-24 17:06:31 ERROR java.io.FileNotFoundException: ./sbk-gem.yml (No such file or directory)

usage: sbk-gem-yal
Storage Benchmark Kit-Group Execution Monitor-YML Arguments Loader

 -f <arg>   SBK-GEM YAML file, default: ./sbk-gem.yml
 -help      Help message
 -p         Print SBK-GEM Options Help Text

Please report issues at https://github.com/kmgowda/SBK
```
An Example output of SBK-GEM-YAL with 1 SBK file system benchmarking instances is as follows:

```
kmg@kmgs-MacBook-Pro SBK % ./gradlew installDist
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk-gem-yal -f ./sbk-gem-file-reader.yml                                     
2024-08-24 17:08:07 INFO 
   _____   ____    _  __            _____   ______   __  __           __     __             _
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |          \ \   / /     /\     | |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |  ______   \ \_/ /     /  \    | |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| | |______|   \   /     / /\ \   | |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |             | |     / ____ \  | |____
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|             |_|    /_/    \_\ |______|


2024-08-24 17:08:07 INFO Storage Benchmark Kit-Group Execution Monitor-YML Arguments Loader
2024-08-24 17:08:07 INFO SBK-GEM-YAL Version: 5.3
2024-08-24 17:08:07 INFO Arguments List: [-f, ./sbk-gem-file-reader.yml]
2024-08-24 17:08:07 INFO Java Runtime Version: 17.0.2+8
2024-08-24 17:08:07 INFO Reflections took 63 ms to scan 51 urls, producing 26 keys and 156 values
2024-08-24 17:08:07 INFO Reflections took 0 ms to scan 1 urls, producing 4 keys and 6 values
2024-08-24 17:08:07 INFO 
   _____   ____    _  __            _____   ______   __  __
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|

2024-08-24 17:08:07 INFO Storage Benchmark Kit - Group Execution Monitor
2024-08-24 17:08:07 INFO SBK-GEM Version: 5.3
2024-08-24 17:08:07 INFO SBK-GEM Website: https://github.com/kmgowda/SBK
2024-08-24 17:08:07 INFO Arguments List: [-seconds, 60, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk, -class, file, -sbmsleepms, 100, -gempass, Laki@2322, -nodes, localhost 127.0.0.1, -readers, 1, -time, ns, -gemuser, kmg, -size, 10]
2024-08-24 17:08:07 INFO Java Runtime Version: 17.0.2+8
2024-08-24 17:08:07 INFO SBP Version Major: 3, Minor: 0
2024-08-24 17:08:07 INFO Storage Drivers Package: io.sbk.driver
2024-08-24 17:08:07 INFO sbk.applicationName: sbk
2024-08-24 17:08:07 INFO sbk.className: 
2024-08-24 17:08:07 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2024-08-24 17:08:07 INFO '-class': file
2024-08-24 17:08:07 INFO Storage Classes in package 'io.sbk.driver': 49 [Activemq, 
Artemis, AsyncFile, Atomicq, BookKeeper, Cassandra, CephS3, ConcurrentQ, Conqueue, 
Couchbase, CouchDB, CSV, Db2, Derby, Dynamodb, Exasol, FdbRecord, File, FileStream, 
FoundationDB, H2, HDFS, Hive, Jdbc, Kafka, LevelDB, Linkedbq, MariaDB, Memcached, 
MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, Pravega, 
Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite, Syncq]
2024-08-24 17:08:07 INFO Gem Logger Classes in package 'io.gem.logger': 1 [GemPrometheusLogger]
2024-08-24 17:08:07 INFO SBK-GEM [1]: Arguments to process : [-seconds, 60, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk, -sbmsleepms, 100, -gempass, Laki@2322, -nodes, localhost 127.0.0.1, -readers, 1, -time, ns, -gemuser, kmg, -size, 10]
2024-08-24 17:08:07 INFO Time Unit: NANOSECONDS
2024-08-24 17:08:07 INFO Minimum Latency: 0 ns
2024-08-24 17:08:07 INFO Maximum Latency: 180000000000 ns
2024-08-24 17:08:07 INFO SBK dir: /Users/kmg/projects/SBK/build/install/sbk
2024-08-24 17:08:07 INFO SBK command: bin/sbk
2024-08-24 17:08:07 INFO Arguments to remote SBK command: -class file -seconds 60 -readers 1 -size 10 -out GrpcLogger -time ns -minlatency 0 -maxlatency 180000000000 -wq true -rq true -context no -sbm kmgs-MacBook-Pro.local -sbmport 9717
2024-08-24 17:08:07 INFO SBK-GEM: Arguments to remote SBK command verification Success..
2024-08-24 17:08:07 INFO Arguments to SBM: [-class, file, -action, r, -time, ns, -minlatency, 0, -maxlatency, 180000000000, -port, 9717, -wq, true, -rq, true, -max, 2, -millisecsleep, 100]
2024-08-24 17:08:07 INFO Logger for SBM: GemPrometheusLogger
2024-08-24 17:08:07 INFO SBK-GEM: Arguments to SBM command verification Success..
2024-08-24 17:08:07 INFO Window Latency Store: HashMap, Size: 512 MB
2024-08-24 17:08:07 INFO Total Window Latency Store: HashMap, Size: 1024 MB
2024-08-24 17:08:07 INFO Total Window Extension: None, Size: 0 MB
2024-08-24 17:08:07 INFO getOrCreateProvider(BC) created instance of org.bouncycastle.jce.provider.BouncyCastleProvider
2024-08-24 17:08:07 INFO getOrCreateProvider(EdDSA) created instance of net.i2p.crypto.eddsa.EdDSASecurityProvider
2024-08-24 17:08:07 INFO SBK GEM Benchmark Started
2024-08-24 17:08:07 INFO SBK-GEM: Ssh Connection to host '127.0.0.1' starting...
2024-08-24 17:08:07 INFO SBK-GEM: Ssh Connection to host 'localhost' starting...
2024-08-24 17:08:07 INFO Using MinaServiceFactoryFactory
2024-08-24 17:08:07 INFO resolveEffectiveResolver(kmg@localhost:22/null) no configuration file at /Users/kmg/.ssh/config
2024-08-24 17:08:07 INFO resolveEffectiveResolver(kmg@127.0.0.1:22/null) no configuration file at /Users/kmg/.ssh/config
2024-08-24 17:08:08 WARN Server at /127.0.0.1:22 presented unverified EC key: SHA256:6kTtY0SqflZ/04wvcClPuoe9ZRxaVyeakHBYuVfTkSg
2024-08-24 17:08:08 WARN Server at localhost/127.0.0.1:22 presented unverified EC key: SHA256:6kTtY0SqflZ/04wvcClPuoe9ZRxaVyeakHBYuVfTkSg
2024-08-24 17:08:08 INFO Server announced support for publickey-hostbound@openssh.com version 0
2024-08-24 17:08:08 INFO Server announced support for publickey-hostbound@openssh.com version 0
2024-08-24 17:08:08 INFO SBK-GEM: Ssh Connection to host '127.0.0.1' Success.
2024-08-24 17:08:08 INFO SBK-GEM: Ssh Connection to host 'localhost' Success.
2024-08-24 17:08:08 INFO SBK-GEM: Ssh session establishment Success..
2024-08-24 17:08:08 INFO SBK-GEM: Matching Java Major Version: 17 Success..
2024-08-24 17:08:08 INFO SBK-GEM: Removing the remote directory: 'sbk'  Success..
2024-08-24 17:08:08 INFO SBK-GEM: Creating remote directory: 'sbk'  Success..
2024-08-24 17:08:08 WARN validateCommandStatusCode(ScpHelper[ClientSessionImpl[kmg@/127.0.0.1:22]])[/Users/kmg/projects/SBK/build/install/sbk] advisory ACK=1: scp: sbk-gem-5.3/sbk: File exists for command=D0755 0 sbk
2024-08-24 17:08:12 WARN handleCommandExitStatus(ClientSessionImpl[kmg@/127.0.0.1:22]) cmd='scp -d -r -p -t -- sbk-gem-5.3' may have terminated with some problems
2024-08-24 17:08:12 INFO Copy SBK application: 'bin/sbk' to remote nodes Success..
2024-08-24 17:08:12 INFO SBM Started
2024-08-24 17:08:12 INFO SBK PrometheusLogger Started
2024-08-24 17:08:12 INFO SBK Connections PrometheusLogger Started
2024-08-24 17:08:12 INFO SbmLatencyBenchmark Started : 100 milliseconds idle sleep
2024-08-24 17:08:12 INFO SBK-GEM: Remote SBK command: sbk/bin/sbk -class file -seconds 60 -readers 1 -size 10 -out GrpcLogger -time ns -minlatency 0 -maxlatency 180000000000 -wq true -rq true -context no -sbm kmgs-MacBook-Pro.local -sbmport 9717
SBM     2 connections,     2 max connections: File Reading     0 writers,     0 readers,      0 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,         0.0 MB,                0 records,         0.0 records/sec,     0.00 MB/sec,      0.0 ns avg latency,       0 ns min latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ns 5th,       0 ns 10th,       0 ns 20th,       0 ns 25th,       0 ns 30th,       0 ns 40th,       0 ns 50th,       0 ns 60th,       0 ns 70th,       0 ns 75th,       0 ns 80th,       0 ns 90th,       0 ns 92.5th,       0 ns 95th,       0 ns 97.5th,       0 ns 99th,       0 ns 99.25th,       0 ns 99.5th,       0 ns 99.75th,       0 ns 99.9th,       0 ns 99.95th,       0 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        58.5 read request MB,           6136372 read request records,   1209993.7 read request records/sec,    11.54 read request MB/sec,   -58.22 write response pending MB,      -6104620 write response pending records,      0.03 read response pending MB,        317540 read response pending records,    -58.52 write read request pending MB,      -6136372 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        58.2 MB,          6104620 records,   1203732.7 records/sec,    11.48 MB/sec,   1545.6 ns avg latency,       0 ns min latency, 4367581 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    1164 ns 5th,    1200 ns 10th,    1242 ns 20th,    1258 ns 25th,    1274 ns 30th,    1304 ns 40th,    1336 ns 50th,    1375 ns 60th,    1427 ns 70th,    1465 ns 75th,    1518 ns 80th,    1674 ns 90th,    1742 ns 92.5th,    1855 ns 95th,    2083 ns 97.5th,    3412 ns 99th,    5277 ns 99.25th,   16875 ns 99.5th,   22976 ns 99.75th,   28811 ns 99.9th,   32885 ns 99.95th,   57091 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        57.4 read request MB,           6015129 read request records,   1178774.3 read request records/sec,    11.24 read request MB/sec,  -115.72 write response pending MB,     -12134282 write response pending records,      0.02 read response pending MB,        172210 read response pending records,   -115.89 write read request pending MB,     -12151501 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        57.5 MB,          6029662 records,   1181622.3 records/sec,    11.27 MB/sec,   1577.7 ns avg latency,       0 ns min latency, 5064563 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    1182 ns 5th,    1215 ns 10th,    1259 ns 20th,    1277 ns 25th,    1295 ns 30th,    1329 ns 40th,    1367 ns 50th,    1415 ns 60th,    1478 ns 70th,    1517 ns 75th,    1564 ns 80th,    1691 ns 90th,    1750 ns 92.5th,    1846 ns 95th,    2019 ns 97.5th,    2501 ns 99th,    4845 ns 99.25th,   18016 ns 99.5th,   23654 ns 99.75th,   28541 ns 99.9th,   32483 ns 99.95th,   52431 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        54.9 read request MB,           5756510 read request records,   1150727.1 read request records/sec,    10.97 read request MB/sec,  -170.61 write response pending MB,     -17889907 write response pending records,      0.02 read response pending MB,        181040 read response pending records,   -170.78 write read request pending MB,     -17908011 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        54.9 MB,          5755625 records,   1150550.2 records/sec,    10.97 MB/sec,   1652.1 ns avg latency,       0 ns min latency, 4496183 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   9; Latency Percentiles:    1222 ns 5th,    1258 ns 10th,    1300 ns 20th,    1317 ns 25th,    1333 ns 30th,    1367 ns 40th,    1403 ns 50th,    1445 ns 60th,    1502 ns 70th,    1542 ns 75th,    1592 ns 80th,    1743 ns 90th,    1804 ns 92.5th,    1889 ns 95th,    2045 ns 97.5th,    3193 ns 99th,   17150 ns 99.25th,   19281 ns 99.5th,   24512 ns 99.75th,   30756 ns 99.9th,   34726 ns 99.95th,   64815 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        57.8 read request MB,           6055685 read request records,   1197523.8 read request records/sec,    11.42 read request MB/sec,  -228.43 write response pending MB,     -23952820 write response pending records,      0.01 read response pending MB,        108770 read response pending records,   -228.54 write read request pending MB,     -23963696 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        57.8 MB,          6062913 records,   1198953.1 records/sec,    11.43 MB/sec,   1569.9 ns avg latency,       0 ns min latency, 4337114 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    1185 ns 5th,    1219 ns 10th,    1264 ns 20th,    1283 ns 25th,    1300 ns 30th,    1335 ns 40th,    1373 ns 50th,    1420 ns 60th,    1483 ns 70th,    1522 ns 75th,    1566 ns 80th,    1676 ns 90th,    1721 ns 92.5th,    1796 ns 95th,    1969 ns 97.5th,    2322 ns 99th,    3724 ns 99.25th,   17251 ns 99.5th,   23487 ns 99.75th,   29570 ns 99.9th,   33969 ns 99.95th,   57784 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        61.3 read request MB,           6426688 read request records,   1276022.4 read request records/sec,    12.17 read request MB/sec,  -289.76 write response pending MB,     -30383234 write response pending records,      0.01 read response pending MB,         71500 read response pending records,   -289.83 write read request pending MB,     -30390384 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        61.3 MB,          6430414 records,   1276762.2 records/sec,    12.18 MB/sec,   1478.5 ns avg latency,       0 ns min latency, 2983951 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    1163 ns 5th,    1190 ns 10th,    1224 ns 20th,    1238 ns 25th,    1252 ns 30th,    1278 ns 40th,    1306 ns 50th,    1339 ns 60th,    1384 ns 70th,    1418 ns 75th,    1470 ns 80th,    1614 ns 90th,    1665 ns 92.5th,    1745 ns 95th,    1904 ns 97.5th,    2231 ns 99th,    2466 ns 99.25th,   15733 ns 99.5th,   21781 ns 99.75th,   27536 ns 99.9th,   31533 ns 99.95th,   46802 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        63.2 read request MB,           6631129 read request records,   1312869.7 read request records/sec,    12.52 read request MB/sec,  -353.01 write response pending MB,     -37015775 write response pending records,      0.01 read response pending MB,         57390 read response pending records,   -353.06 write read request pending MB,     -37021513 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        63.3 MB,          6632541 records,   1313149.2 records/sec,    12.52 MB/sec,   1433.5 ns avg latency,       0 ns min latency, 2186353 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    1156 ns 5th,    1182 ns 10th,    1215 ns 20th,    1229 ns 25th,    1241 ns 30th,    1266 ns 40th,    1292 ns 50th,    1321 ns 60th,    1358 ns 70th,    1383 ns 75th,    1418 ns 80th,    1543 ns 90th,    1585 ns 92.5th,    1635 ns 95th,    1714 ns 97.5th,    1862 ns 99th,    1962 ns 99.25th,   10326 ns 99.5th,   21022 ns 99.75th,   26637 ns 99.9th,   31113 ns 99.95th,   45639 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        62.1 read request MB,           6509061 read request records,   1288450.7 read request records/sec,    12.29 read request MB/sec,  -415.08 write response pending MB,     -43523985 write response pending records,      0.01 read response pending MB,         65900 read response pending records,   -415.14 write read request pending MB,     -43530574 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        62.1 MB,          6508210 records,   1288282.2 records/sec,    12.29 MB/sec,   1462.2 ns avg latency,       0 ns min latency, 2075656 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    1158 ns 5th,    1185 ns 10th,    1219 ns 20th,    1234 ns 25th,    1248 ns 30th,    1275 ns 40th,    1304 ns 50th,    1338 ns 60th,    1386 ns 70th,    1423 ns 75th,    1474 ns 80th,    1617 ns 90th,    1671 ns 92.5th,    1757 ns 95th,    1920 ns 97.5th,    2197 ns 99th,    2386 ns 99.25th,    5062 ns 99.5th,   20592 ns 99.75th,   26256 ns 99.9th,   30592 ns 99.95th,   41419 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        58.6 read request MB,           6145478 read request records,   1214671.7 read request records/sec,    11.58 read request MB/sec,  -473.69 write response pending MB,     -49669712 write response pending records,      0.01 read response pending MB,         63400 read response pending records,   -473.75 write read request pending MB,     -49676052 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        58.6 MB,          6145727 records,   1214720.9 records/sec,    11.58 MB/sec,   1547.4 ns avg latency,       0 ns min latency, 3223832 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    1169 ns 5th,    1200 ns 10th,    1241 ns 20th,    1258 ns 25th,    1274 ns 30th,    1306 ns 40th,    1341 ns 50th,    1384 ns 60th,    1444 ns 70th,    1483 ns 75th,    1528 ns 80th,    1644 ns 90th,    1686 ns 92.5th,    1751 ns 95th,    1904 ns 97.5th,    2264 ns 99th,    4960 ns 99.25th,   17721 ns 99.5th,   23695 ns 99.75th,   29230 ns 99.9th,   33269 ns 99.95th,   57421 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        63.8 read request MB,           6693929 read request records,   1318847.2 read request records/sec,    12.58 read request MB/sec,  -537.54 write response pending MB,     -56365549 write response pending records,      0.00 read response pending MB,         44320 read response pending records,   -537.59 write read request pending MB,     -56369981 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        63.9 MB,          6695837 records,   1319223.1 records/sec,    12.58 MB/sec,   1421.1 ns avg latency,       0 ns min latency, 2691380 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:    1147 ns 5th,    1173 ns 10th,    1207 ns 20th,    1221 ns 25th,    1235 ns 30th,    1261 ns 40th,    1289 ns 50th,    1320 ns 60th,    1361 ns 70th,    1389 ns 75th,    1427 ns 80th,    1548 ns 90th,    1589 ns 92.5th,    1640 ns 95th,    1722 ns 97.5th,    1878 ns 99th,    1967 ns 99.25th,    2493 ns 99.5th,   18648 ns 99.75th,   25551 ns 99.9th,   29979 ns 99.95th,   38500 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        59.6 read request MB,           6246758 read request records,   1235696.8 read request records/sec,    11.78 read request MB/sec,  -597.11 write response pending MB,     -62611482 write response pending records,      0.01 read response pending MB,         52570 read response pending records,   -597.16 write read request pending MB,     -62616739 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        59.6 MB,          6245933 records,   1235533.6 records/sec,    11.78 MB/sec,   1525.0 ns avg latency,       0 ns min latency, 7159150 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    1154 ns 5th,    1185 ns 10th,    1225 ns 20th,    1243 ns 25th,    1260 ns 30th,    1295 ns 40th,    1334 ns 50th,    1383 ns 60th,    1452 ns 70th,    1496 ns 75th,    1546 ns 80th,    1686 ns 90th,    1749 ns 92.5th,    1848 ns 95th,    1997 ns 97.5th,    2336 ns 99th,    2604 ns 99.25th,   15734 ns 99.5th,   21852 ns 99.75th,   26907 ns 99.9th,   31336 ns 99.95th,   51186 ns 99.99th

SBM     2 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        60.6 read request MB,           6355906 read request records,   1259785.7 read request records/sec,    12.01 read request MB/sec,  -657.72 write response pending MB,     -68967100 write response pending records,      0.01 read response pending MB,         55450 read response pending records,   -657.77 write read request pending MB,     -68972645 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        60.6 MB,          6355618 records,   1259728.6 records/sec,    12.01 MB/sec,   1497.6 ns avg latency,       0 ns min latency, 2772632 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    1174 ns 5th,    1204 ns 10th,    1241 ns 20th,    1256 ns 25th,    1270 ns 30th,    1298 ns 40th,    1327 ns 50th,    1361 ns 60th,    1405 ns 70th,    1435 ns 75th,    1476 ns 80th,    1598 ns 90th,    1640 ns 92.5th,    1695 ns 95th,    1798 ns 97.5th,    2036 ns 99th,    2315 ns 99.25th,   16348 ns 99.5th,   22621 ns 99.75th,   28343 ns 99.9th,   32541 ns 99.95th,   52774 ns 99.99th

SBM     0 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        64.7 read request MB,           6786707 read request records,   3006698.0 read request records/sec,    28.67 read request MB/sec,  -722.46 write response pending MB,     -75755380 write response pending records,      0.00 read response pending MB,         39720 read response pending records,   -722.50 write read request pending MB,     -75759352 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       2 seconds,        64.7 MB,          6788280 records,   3007394.8 records/sec,    28.68 MB/sec,   1398.4 ns avg latency,       0 ns min latency, 4776571 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:    1141 ns 5th,    1167 ns 10th,    1199 ns 20th,    1212 ns 25th,    1224 ns 30th,    1249 ns 40th,    1275 ns 50th,    1305 ns 60th,    1347 ns 70th,    1377 ns 75th,    1424 ns 80th,    1567 ns 90th,    1615 ns 92.5th,    1687 ns 95th,    1876 ns 97.5th,    2070 ns 99th,    2188 ns 99.25th,    2402 ns 99.5th,   16151 ns 99.75th,   23230 ns 99.9th,   27521 ns 99.95th,   34559 ns 99.99th

Total : SBM     0 connections,     2 max connections: File Reading     0 writers,     2 readers,      0 max writers,     2 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,       722.5 read request MB,          75759352 read request records,   1204673.5 read request records/sec,    11.49 read request MB/sec,  -722.46 write response pending MB,     -75755380 write response pending records,      0.00 read response pending MB,         39720 read response pending records,   -722.50 write read request pending MB,     -75759352 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,      62 seconds,       722.5 MB,         75755380 records,   1204610.3 records/sec,    11.49 MB/sec,   1505.7 ns avg latency,       0 ns min latency, 7159150 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    1162 ns 5th,    1192 ns 10th,    1230 ns 20th,    1247 ns 25th,    1262 ns 30th,    1293 ns 40th,    1325 ns 50th,    1364 ns 60th,    1419 ns 70th,    1457 ns 75th,    1504 ns 80th,    1634 ns 90th,    1684 ns 92.5th,    1761 ns 95th,    1924 ns 97.5th,    2267 ns 99th,    2640 ns 99.25th,   16146 ns 99.5th,   22332 ns 99.75th,   27722 ns 99.9th,   31891 ns 99.95th,   51440 ns 99.99th

2024-08-24 17:09:15 INFO SbmLatencyBenchmark Shutdown
2024-08-24 17:09:15 INFO SBK PrometheusLogger Shutdown
2024-08-24 17:09:15 INFO SBM Shutdown
2024-08-24 17:09:15 INFO SBK GEM Benchmark Shutdown

SBK-GEM Remote Results
--------------------------------------------------------------------------------

Host 1: localhost, return code: 0
--------------------------------------------------------------------------------
Host 2: 127.0.0.1, return code: 0
--------------------------------------------------------------------------------
```
Note that at the results, The SBK-GEM prints the return code of each of the remote host.

# LevelDB Performance benchmarking using SBK

The java version of leveldb storage driver of SBK supports the on disk key value store single and multi writers and readers.


## Writer Example


```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class leveldb -writers 1 -size 100 -time ns -seconds 60      
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-09-03 17:23:47 INFO Reflections took 113 ms to scan 44 urls, producing 109 keys and 251 values 
2021-09-03 17:23:48 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-09-03 17:23:48 INFO Storage Benchmark Kit
2021-09-03 17:23:48 INFO SBK Version: 0.911
2021-09-03 17:23:48 INFO Arguments List: [-class, leveldb, -writers, 1, -size, 100, -time, ns, -seconds, 60]
2021-09-03 17:23:48 INFO Java Runtime Version: 11.0.8+11
2021-09-03 17:23:48 INFO Storage Drivers Package: io.sbk
2021-09-03 17:23:48 INFO sbk.applicationName: sbk
2021-09-03 17:23:48 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2021-09-03 17:23:48 INFO sbk.className: 
2021-09-03 17:23:48 INFO '-class': leveldb
2021-09-03 17:23:48 INFO Available Storage Drivers in package 'io.sbk': 40 [Artemis, 
AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, Derby, 
FdbRecord, File, FileStream, FoundationDB, HDFS, Hive, Ignite, Jdbc, Kafka, LevelDB, 
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, 
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2021-09-03 17:23:48 INFO Arguments to Driver 'LevelDB' : [-writers, 1, -size, 100, -time, ns, -seconds, 60]
2021-09-03 17:23:48 INFO Time Unit: NANOSECONDS
2021-09-03 17:23:48 INFO Minimum Latency: 0 ns
2021-09-03 17:23:48 INFO Maximum Latency: 180000000000 ns
2021-09-03 17:23:48 INFO Window Latency Store: HashMap, Size: 192 MB
2021-09-03 17:23:48 INFO Total Window Latency Store: HashMap, Size: 256 MB
2021-09-03 17:23:48 INFO Total Window Extension: None, Size: 0 MB
2021-09-03 17:23:48 INFO SBK Benchmark Started
2021-09-03 17:23:48 INFO SBK PrometheusLogger Started
2021-09-03 17:23:48 INFO Performance Logger Started
2021-09-03 17:23:48 INFO SBK Benchmark initiated Writers
2021-09-03 17:23:48 INFO Writer 0 started , run seconds: 60
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        73.5 MB,           770647 records,    154075.4 records/sec,    14.69 MB/sec,   6362.3 ns avg latency, 33273243 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2: 439; Latency Percentiles:     539 ns 10th,     611 ns 20th,     640 ns 25th,     666 ns 30th,     721 ns 40th,     800 ns 50th,     934 ns 60th,    1074 ns 70th,    1231 ns 75th,    1514 ns 80th,    2368 ns 90th,    2584 ns 92.5th,    2941 ns 95th,    4038 ns 97.5th,   14218 ns 99th,   20430 ns 99.25th,   28932 ns 99.5th, 1244930 ns 99.75th, 1287565 ns 99.9th, 1304094 ns 99.95th, 1358296 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        28.0 MB,           293590 records,     58702.3 records/sec,     5.60 MB/sec,  16942.5 ns avg latency, 17482661 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 1277; Latency Percentiles:     555 ns 10th,     612 ns 20th,     633 ns 25th,     652 ns 30th,     689 ns 40th,     731 ns 50th,     787 ns 60th,     861 ns 70th,     908 ns 75th,     971 ns 80th,    1244 ns 90th,    1515 ns 92.5th,    2455 ns 95th,    3116 ns 97.5th, 1149788 ns 99th, 1265585 ns 99.25th, 1282793 ns 99.5th, 1304165 ns 99.75th, 1324662 ns 99.9th, 1341732 ns 99.95th, 6328313 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        55.7 MB,           583770 records,    116715.7 records/sec,    11.13 MB/sec,   8476.0 ns avg latency, 32361354 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 907; Latency Percentiles:     573 ns 10th,     619 ns 20th,     635 ns 25th,     651 ns 30th,     681 ns 40th,     715 ns 50th,     758 ns 60th,     824 ns 70th,     871 ns 75th,     928 ns 80th,    1111 ns 90th,    1231 ns 92.5th,    2082 ns 95th,    2678 ns 97.5th,    4947 ns 99th,   13453 ns 99.25th, 1072057 ns 99.5th, 1281669 ns 99.75th, 1308986 ns 99.9th, 1325594 ns 99.95th, 4719808 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        76.4 MB,           801513 records,    160246.6 records/sec,    15.28 MB/sec,   6144.1 ns avg latency, 43356141 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 486; Latency Percentiles:     578 ns 10th,     622 ns 20th,     639 ns 25th,     654 ns 30th,     685 ns 40th,     720 ns 50th,     764 ns 60th,     829 ns 70th,     874 ns 75th,     931 ns 80th,    1112 ns 90th,    1220 ns 92.5th,    1963 ns 95th,    2639 ns 97.5th,    3744 ns 99th,    5217 ns 99.25th,   16488 ns 99.5th, 1233396 ns 99.75th, 1295286 ns 99.9th, 1310757 ns 99.95th, 1383174 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        20.9 MB,           219036 records,     43793.5 records/sec,     4.18 MB/sec,  22737.6 ns avg latency, 15938492 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 1342; Latency Percentiles:     597 ns 10th,     630 ns 20th,     644 ns 25th,     657 ns 30th,     685 ns 40th,     718 ns 50th,     762 ns 60th,     831 ns 70th,     878 ns 75th,     932 ns 80th,    1113 ns 90th,    1245 ns 92.5th,    2338 ns 95th,    3057 ns 97.5th, 1262814 ns 99th, 1277696 ns 99.25th, 1292599 ns 99.5th, 1309514 ns 99.75th, 1330498 ns 99.9th, 1345814 ns 99.95th, 6630084 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        10.5 MB,           110188 records,     22027.9 records/sec,     2.10 MB/sec,  45292.9 ns avg latency, 30625772 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 1545; Latency Percentiles:     517 ns 10th,     570 ns 20th,     590 ns 25th,     608 ns 30th,     644 ns 40th,     683 ns 50th,     731 ns 60th,     797 ns 70th,     842 ns 75th,     903 ns 80th,    1163 ns 90th,    1693 ns 92.5th,    2611 ns 95th, 1202193 ns 97.5th, 1294784 ns 99th, 1300956 ns 99.25th, 1309327 ns 99.5th, 1323920 ns 99.75th, 1341577 ns 99.9th, 1363726 ns 99.95th, 6685133 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        13.9 MB,           145684 records,     29130.9 records/sec,     2.78 MB/sec,  34231.2 ns avg latency, 15175225 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 1531; Latency Percentiles:     505 ns 10th,     552 ns 20th,     573 ns 25th,     593 ns 30th,     632 ns 40th,     670 ns 50th,     712 ns 60th,     769 ns 70th,     809 ns 75th,     867 ns 80th,    1078 ns 90th,    1245 ns 92.5th,    2224 ns 95th, 1050221 ns 97.5th, 1284250 ns 99th, 1295419 ns 99.25th, 1304882 ns 99.5th, 1318681 ns 99.75th, 1335709 ns 99.9th, 1351881 ns 99.95th, 6442289 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        13.9 MB,           145620 records,     29118.0 records/sec,     2.78 MB/sec,  34244.8 ns avg latency, 15301490 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 1514; Latency Percentiles:     532 ns 10th,     579 ns 20th,     599 ns 25th,     616 ns 30th,     648 ns 40th,     683 ns 50th,     725 ns 60th,     789 ns 70th,     834 ns 75th,     893 ns 80th,    1092 ns 90th,    1281 ns 92.5th,    2313 ns 95th, 1049285 ns 97.5th, 1283919 ns 99th, 1294678 ns 99.25th, 1302491 ns 99.5th, 1316885 ns 99.75th, 1334757 ns 99.9th, 1349292 ns 99.95th, 6575925 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        10.9 MB,           114091 records,     22811.9 records/sec,     2.18 MB/sec,  43739.4 ns avg latency, 22831313 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 1546; Latency Percentiles:     548 ns 10th,     579 ns 20th,     592 ns 25th,     606 ns 30th,     633 ns 40th,     665 ns 50th,     707 ns 60th,     772 ns 70th,     817 ns 75th,     873 ns 80th,    1070 ns 90th,    1303 ns 92.5th,    2416 ns 95th, 1179813 ns 97.5th, 1292490 ns 99th, 1298507 ns 99.25th, 1307935 ns 99.5th, 1322184 ns 99.75th, 1341136 ns 99.9th, 1358038 ns 99.95th, 6322663 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        23.9 MB,           250119 records,     50002.2 records/sec,     4.77 MB/sec,  19909.9 ns avg latency, 20922013 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 1326; Latency Percentiles:     516 ns 10th,     557 ns 20th,     574 ns 25th,     589 ns 30th,     620 ns 40th,     657 ns 50th,     705 ns 60th,     776 ns 70th,     823 ns 75th,     882 ns 80th,    1165 ns 90th,    1831 ns 92.5th,    2272 ns 95th,    3209 ns 97.5th, 1216145 ns 99th, 1270332 ns 99.25th, 1288106 ns 99.5th, 1305940 ns 99.75th, 1325841 ns 99.9th, 1337809 ns 99.95th, 5320101 ns 99.99th.
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        55.7 MB,           583715 records,    116713.4 records/sec,    11.13 MB/sec,   8478.9 ns avg latency, 19331438 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 618; Latency Percentiles:     566 ns 10th,     608 ns 20th,     625 ns 25th,     642 ns 30th,     674 ns 40th,     709 ns 50th,     753 ns 60th,     816 ns 70th,     860 ns 75th,     916 ns 80th,    1093 ns 90th,    1200 ns 92.5th,    2004 ns 95th,    2620 ns 97.5th,    5041 ns 99th,   14169 ns 99.25th, 1071836 ns 99.5th, 1267402 ns 99.75th, 1300200 ns 99.9th, 1315019 ns 99.95th, 1598727 ns 99.99th.
2021-09-03 17:24:48 INFO Writer 0 exited
LevelDB Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        4 seconds,        69.6 MB,           729346 records,    146348.7 records/sec,    13.96 MB/sec,   6732.7 ns avg latency, 17158701 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 505; Latency Percentiles:     571 ns 10th,     612 ns 20th,     628 ns 25th,     645 ns 30th,     677 ns 40th,     712 ns 50th,     757 ns 60th,     821 ns 70th,     865 ns 75th,     920 ns 80th,    1104 ns 90th,    1214 ns 92.5th,    1993 ns 95th,    2620 ns 97.5th,    4317 ns 99th,    6676 ns 99.25th,   24620 ns 99.5th, 1259729 ns 99.75th, 1299755 ns 99.9th, 1316515 ns 99.95th, 1475063 ns 99.99th.
Total : LevelDB Writing     0 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,       60 seconds,       452.7 MB,          4747319 records,     79121.0 records/sec,     7.55 MB/sec,  12538.5 ns avg latency, 43356141 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2: 1007; Latency Percentiles:     558 ns 10th,     606 ns 20th,     625 ns 25th,     643 ns 30th,     677 ns 40th,     716 ns 50th,     768 ns 60th,     846 ns 70th,     901 ns 75th,     968 ns 80th,    1276 ns 90th,    1824 ns 92.5th,    2397 ns 95th,    3001 ns 97.5th,   23311 ns 99th, 1105100 ns 99.25th, 1264147 ns 99.5th, 1294517 ns 99.75th, 1314581 ns 99.9th, 1329899 ns 99.95th, 4479638 ns 99.99th.
2021-09-03 17:24:48 INFO Performance Logger Shutdown
2021-09-03 17:24:48 INFO SBK PrometheusLogger Shutdown
2021-09-03 17:24:49 INFO SBK Benchmark Shutdown

```

## Reader Example

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class leveldb -readers 1 -size 100 -time ns -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-09-03 17:27:42 INFO Reflections took 102 ms to scan 44 urls, producing 109 keys and 251 values 
2021-09-03 17:27:42 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-09-03 17:27:42 INFO Storage Benchmark Kit
2021-09-03 17:27:42 INFO SBK Version: 0.911
2021-09-03 17:27:42 INFO Arguments List: [-class, leveldb, -readers, 1, -size, 100, -time, ns, -seconds, 60]
2021-09-03 17:27:42 INFO Java Runtime Version: 11.0.8+11
2021-09-03 17:27:42 INFO Storage Drivers Package: io.sbk
2021-09-03 17:27:42 INFO sbk.applicationName: sbk
2021-09-03 17:27:42 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2021-09-03 17:27:42 INFO sbk.className: 
2021-09-03 17:27:42 INFO '-class': leveldb
2021-09-03 17:27:42 INFO Available Storage Drivers in package 'io.sbk': 40 [Artemis, 
AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, Derby, 
FdbRecord, File, FileStream, FoundationDB, HDFS, Hive, Ignite, Jdbc, Kafka, LevelDB, 
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, 
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2021-09-03 17:27:42 INFO Arguments to Driver 'LevelDB' : [-readers, 1, -size, 100, -time, ns, -seconds, 60]
2021-09-03 17:27:42 INFO Time Unit: NANOSECONDS
2021-09-03 17:27:42 INFO Minimum Latency: 0 ns
2021-09-03 17:27:42 INFO Maximum Latency: 180000000000 ns
2021-09-03 17:27:43 INFO Window Latency Store: HashMap, Size: 192 MB
2021-09-03 17:27:43 INFO Total Window Latency Store: HashMap, Size: 256 MB
2021-09-03 17:27:43 INFO Total Window Extension: None, Size: 0 MB
2021-09-03 17:27:43 INFO SBK Benchmark Started
2021-09-03 17:27:43 INFO SBK PrometheusLogger Started
2021-09-03 17:27:43 INFO Performance Logger Started
2021-09-03 17:27:43 INFO SBK Benchmark initiated Readers
2021-09-03 17:27:43 INFO Reader 0 started , run seconds: 60
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         6.1 MB,            63646 records,     12726.5 records/sec,     1.21 MB/sec,  78067.9 ns avg latency, 33564061 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2: 235; Latency Percentiles:    4942 ns 10th,    5372 ns 20th,    5563 ns 25th,    5771 ns 30th,    6669 ns 40th,    9651 ns 50th,   10894 ns 60th,   37519 ns 70th,  103225 ns 75th,  137251 ns 80th,  200727 ns 90th,  226011 ns 92.5th,  264675 ns 95th,  375998 ns 97.5th,  774610 ns 99th,  893076 ns 99.25th, 1094418 ns 99.5th, 1722181 ns 99.75th, 2479993 ns 99.9th, 6003549 ns 99.95th, 19809164 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        38.2 MB,           400775 records,     80137.7 records/sec,     7.64 MB/sec,  12381.1 ns avg latency, 11373780 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:  82; Latency Percentiles:    2221 ns 10th,    2441 ns 20th,    2537 ns 25th,    2630 ns 30th,    2861 ns 40th,    3448 ns 50th,    3955 ns 60th,    4384 ns 70th,    4715 ns 75th,    5196 ns 80th,    8271 ns 90th,   33045 ns 92.5th,   83984 ns 95th,  121410 ns 97.5th,  179270 ns 99th,  192795 ns 99.25th,  212675 ns 99.5th,  242520 ns 99.75th,  321419 ns 99.9th,  580723 ns 99.95th, 2295381 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        55.2 MB,           578927 records,    115761.7 records/sec,    11.04 MB/sec,   8542.1 ns avg latency, 4441152 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  40; Latency Percentiles:    2021 ns 10th,    2163 ns 20th,    2230 ns 25th,    2293 ns 30th,    2405 ns 40th,    2507 ns 50th,    2617 ns 60th,    2776 ns 70th,    2909 ns 75th,    3108 ns 80th,    5495 ns 90th,   17409 ns 92.5th,   55507 ns 95th,   90369 ns 97.5th,  121382 ns 99th,  138991 ns 99.25th,  156342 ns 99.5th,  177511 ns 99.75th,  212459 ns 99.9th,  233514 ns 99.95th,  320221 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       135.4 MB,          1419673 records,    283877.8 records/sec,    27.07 MB/sec,   3440.8 ns avg latency, 2762924 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  21; Latency Percentiles:    1981 ns 10th,    2121 ns 20th,    2185 ns 25th,    2248 ns 30th,    2369 ns 40th,    2477 ns 50th,    2581 ns 60th,    2707 ns 70th,    2790 ns 75th,    2898 ns 80th,    3229 ns 90th,    3356 ns 92.5th,    3865 ns 95th,    6670 ns 97.5th,   21996 ns 99th,   32675 ns 99.25th,   77915 ns 99.5th,   94710 ns 99.75th,  137114 ns 99.9th,  162788 ns 99.95th,  233511 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       174.9 MB,          1833859 records,    366698.3 records/sec,    34.97 MB/sec,   2647.9 ns avg latency, 1718450 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   4; Latency Percentiles:    1943 ns 10th,    2095 ns 20th,    2153 ns 25th,    2209 ns 30th,    2324 ns 40th,    2442 ns 50th,    2560 ns 60th,    2686 ns 70th,    2755 ns 75th,    2836 ns 80th,    3108 ns 90th,    3217 ns 92.5th,    3364 ns 95th,    4408 ns 97.5th,    6863 ns 99th,    7296 ns 99.25th,    7998 ns 99.5th,   17907 ns 99.75th,   25181 ns 99.9th,   29969 ns 99.95th,   41007 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       185.3 MB,          1943323 records,    388586.8 records/sec,    37.06 MB/sec,   2497.2 ns avg latency, 1452947 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   4; Latency Percentiles:    1864 ns 10th,    1980 ns 20th,    2033 ns 25th,    2086 ns 30th,    2190 ns 40th,    2288 ns 50th,    2386 ns 60th,    2494 ns 70th,    2557 ns 75th,    2632 ns 80th,    2888 ns 90th,    2989 ns 92.5th,    3124 ns 95th,    4142 ns 97.5th,    6408 ns 99th,    6882 ns 99.25th,    7656 ns 99.5th,   18196 ns 99.75th,   25563 ns 99.9th,   30321 ns 99.95th,   42960 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       184.8 MB,          1937447 records,    387411.0 records/sec,    36.95 MB/sec,   2506.1 ns avg latency, 1505327 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   4; Latency Percentiles:    1905 ns 10th,    2000 ns 20th,    2047 ns 25th,    2095 ns 30th,    2194 ns 40th,    2290 ns 50th,    2385 ns 60th,    2484 ns 70th,    2538 ns 75th,    2601 ns 80th,    2829 ns 90th,    2937 ns 92.5th,    3087 ns 95th,    4112 ns 97.5th,    6283 ns 99th,    6697 ns 99.25th,    7761 ns 99.5th,   18986 ns 99.75th,   26745 ns 99.9th,   30997 ns 99.95th,   43139 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       180.8 MB,          1896079 records,    379139.7 records/sec,    36.16 MB/sec,   2565.1 ns avg latency, 2276983 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  12; Latency Percentiles:    1879 ns 10th,    1964 ns 20th,    2005 ns 25th,    2048 ns 30th,    2142 ns 40th,    2242 ns 50th,    2342 ns 60th,    2443 ns 70th,    2495 ns 75th,    2555 ns 80th,    2825 ns 90th,    3106 ns 92.5th,    4301 ns 95th,    5286 ns 97.5th,    6322 ns 99th,    6789 ns 99.25th,    8440 ns 99.5th,   18260 ns 99.75th,   26707 ns 99.9th,   31880 ns 99.95th,  285796 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        91.0 MB,           953855 records,    190732.4 records/sec,    18.19 MB/sec,   5155.6 ns avg latency, 2237255 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:  27; Latency Percentiles:    2158 ns 10th,    2541 ns 20th,    3031 ns 25th,    3752 ns 30th,    4117 ns 40th,    4359 ns 50th,    4587 ns 60th,    4824 ns 70th,    4951 ns 75th,    5102 ns 80th,    5619 ns 90th,    5968 ns 92.5th,    6878 ns 95th,    8349 ns 97.5th,   10934 ns 99th,   17048 ns 99.25th,   22431 ns 99.5th,  153116 ns 99.75th,  337862 ns 99.9th,  404806 ns 99.95th,  788413 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        75.8 MB,           795254 records,    159018.9 records/sec,    15.17 MB/sec,   6194.9 ns avg latency, 1963664 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  28; Latency Percentiles:    3920 ns 10th,    4156 ns 20th,    4249 ns 25th,    4338 ns 30th,    4510 ns 40th,    4679 ns 50th,    4853 ns 60th,    5051 ns 70th,    5173 ns 75th,    5328 ns 80th,    6026 ns 90th,    6708 ns 92.5th,    7637 ns 95th,    9053 ns 97.5th,   12262 ns 99th,   18643 ns 99.25th,   29548 ns 99.5th,  190756 ns 99.75th,  375608 ns 99.9th,  422601 ns 99.95th,  922438 ns 99.99th.
LevelDB Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        73.1 MB,           766723 records,    153313.8 records/sec,    14.62 MB/sec,   6420.2 ns avg latency, 2559625 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  26; Latency Percentiles:    4223 ns 10th,    4483 ns 20th,    4592 ns 25th,    4692 ns 30th,    4873 ns 40th,    5053 ns 50th,    5259 ns 60th,    5519 ns 70th,    5680 ns 75th,    5881 ns 80th,    6658 ns 90th,    7184 ns 92.5th,    8157 ns 95th,    9578 ns 97.5th,   11629 ns 99th,   12765 ns 99.25th,   19634 ns 99.5th,  161340 ns 99.75th,  385786 ns 99.9th,  432953 ns 99.95th,  928986 ns 99.99th.
2021-09-03 17:28:43 INFO Reader 0 exited
LevelDB Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,        4 seconds,        92.4 MB,           969308 records,    193967.9 records/sec,    18.50 MB/sec,   3374.4 ns avg latency, 1428639 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  17; Latency Percentiles:    2208 ns 10th,    2323 ns 20th,    2377 ns 25th,    2435 ns 30th,    2566 ns 40th,    2712 ns 50th,    2870 ns 60th,    3036 ns 70th,    3142 ns 75th,    3539 ns 80th,    4859 ns 90th,    5075 ns 92.5th,    5391 ns 95th,    6706 ns 97.5th,    8280 ns 99th,    8756 ns 99.25th,    9542 ns 99.5th,   15247 ns 99.75th,   33075 ns 99.9th,  176520 ns 99.95th,  409703 ns 99.99th.
Total : LevelDB Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,       60 seconds,      1293.1 MB,         13558869 records,    225949.2 records/sec,    21.55 MB/sec,   4220.6 ns avg latency, 33564061 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  44; Latency Percentiles:    1949 ns 10th,    2097 ns 20th,    2166 ns 25th,    2234 ns 30th,    2363 ns 40th,    2494 ns 50th,    2652 ns 60th,    2943 ns 70th,    3262 ns 75th,    4115 ns 80th,    5036 ns 90th,    5340 ns 92.5th,    5854 ns 95th,    7552 ns 97.5th,   21452 ns 99th,   35662 ns 99.25th,   86345 ns 99.5th,  141650 ns 99.75th,  213951 ns 99.9th,  319915 ns 99.95th,  797849 ns 99.99th.
2021-09-03 17:28:43 INFO Performance Logger Shutdown
2021-09-03 17:28:43 INFO SBK PrometheusLogger Shutdown
2021-09-03 17:28:44 INFO SBK Benchmark Shutdown

```
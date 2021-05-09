<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# IBM DB2 Performance using SBK
The IBM DB2 driver for SBK supports single writer and multiple readers' performance benchmarking. But, the End to End Latency benchmarking is not supported.
Using this JDBC support of SBK, you can conduct the performance benchmarking of any database which provides JDBC driver.
The Apache Hive driver uses the JDBC for IO operations.

An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class db2 -writers 1 -size 100 -seconds 120
```

by default, the SBK uses the local hive cluster url: jdbc:db2://localhost:50000/testdb
if you want to use remote cluster you specify the same using '-url' option


you can use the below docker image to setup the standalone Hive cluster.
```
docker run -itd --name db2inst1  --privileged=true -p 50000:50000 -e LICENSE=accept -e DB2INST1_PASSWORD=pass -e DBNAME=testdb   ibmcom/db2
```
docker environment variable DB2INST1_PASSWORD  set the pass word , --name sets the name of the DB2 instance.
Note that, even if you change the name of the docker instance using --name parameter, the username remains as "db2inst1"

Sample SBK Db2 write output

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class db2 -size 100 -writers 1 -seconds 60 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-05-09 13:22:46 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-05-09 13:22:46 INFO Java Runtime Version: 11.0.8+11
2021-05-09 13:22:46 INFO SBK Version: 0.88
2021-05-09 13:22:46 INFO Arguments List: [-class, db2, -size, 100, -writers, 1, -seconds, 60]
2021-05-09 13:22:46 INFO sbk.applicationName: sbk
2021-05-09 13:22:46 INFO sbk.className: sbk
2021-05-09 13:22:46 INFO Reflections took 60 ms to scan 38 urls, producing 57 keys and 186 values 
2021-05-09 13:22:46 INFO Available Drivers : 37
2021-05-09 13:22:46 INFO Arguments to Driver 'Db2' : [-size, 100, -writers, 1, -seconds, 60]
2021-05-09 13:22:46 INFO Time Unit: MILLISECONDS
2021-05-09 13:22:46 INFO Minimum Latency: 0 ms
2021-05-09 13:22:46 INFO Maximum Latency: 180000 ms
2021-05-09 13:22:46 INFO Window Latency Store: Array
2021-05-09 13:22:46 INFO Total Window Latency Store: HashMap
2021-05-09 13:22:46 INFO SBK PrometheusLogger Started
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-05-09 13:22:47 INFO JDBC Driver Type: db2
2021-05-09 13:22:47 INFO JDBC Driver Name: IBM Data Server Driver for JDBC and SQLJ
2021-05-09 13:22:47 INFO JDBC Driver Version: 4.25.13
2021-05-09 13:22:47 INFO Deleting the Table: test
2021-05-09 13:22:47 INFO Creating the Table: test
2021-05-09 13:22:48 INFO Performance Logger Started
2021-05-09 13:22:48 INFO SBK Benchmark Started
Db2 Writing       4839 records,     967.6 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       8 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,       8 ms 99.99th.
Db2 Writing       4954 records,     990.6 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Db2 Writing       5006 records,    1000.2 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Db2 Writing       4971 records,     994.0 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       6 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       6 ms 99.99th.
Db2 Writing       4913 records,     981.6 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Db2 Writing       4931 records,     986.0 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       7 ms 99.99th.
Db2 Writing       4940 records,     987.8 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       5 ms 99.99th.
Db2 Writing       4943 records,     988.4 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Db2 Writing       4936 records,     987.0 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Db2 Writing       4886 records,     977.0 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       8 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       8 ms 99.99th.
Db2 Writing       4918 records,     983.4 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       2 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Db2 Writing       4863 records,     976.3 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       4 ms 99.99th.
Total : Db2 Writing      59100 records,     985.0 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       8 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       5 ms 99.99th.
2021-05-09 13:23:48 INFO Performance Logger Shutdown
2021-05-09 13:23:49 INFO SBK PrometheusLogger Shutdown
2021-05-09 13:23:50 INFO SBK Benchmark Shutdown

```


The sample SBK DB2 read output is below
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class db2 -size 100 -readers 1 -seconds 60 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-05-09 13:24:31 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-05-09 13:24:31 INFO Java Runtime Version: 11.0.8+11
2021-05-09 13:24:31 INFO SBK Version: 0.88
2021-05-09 13:24:31 INFO Arguments List: [-class, db2, -size, 100, -readers, 1, -seconds, 60]
2021-05-09 13:24:31 INFO sbk.applicationName: sbk
2021-05-09 13:24:31 INFO sbk.className: sbk
2021-05-09 13:24:31 INFO Reflections took 66 ms to scan 38 urls, producing 57 keys and 186 values 
2021-05-09 13:24:31 INFO Available Drivers : 37
2021-05-09 13:24:31 INFO Arguments to Driver 'Db2' : [-size, 100, -readers, 1, -seconds, 60]
2021-05-09 13:24:31 INFO Time Unit: MILLISECONDS
2021-05-09 13:24:31 INFO Minimum Latency: 0 ms
2021-05-09 13:24:31 INFO Maximum Latency: 180000 ms
2021-05-09 13:24:31 INFO Window Latency Store: Array
2021-05-09 13:24:31 INFO Total Window Latency Store: HashMap
2021-05-09 13:24:31 INFO SBK PrometheusLogger Started
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-05-09 13:24:32 INFO JDBC Driver Type: db2
2021-05-09 13:24:32 INFO JDBC Driver Name: IBM Data Server Driver for JDBC and SQLJ
2021-05-09 13:24:32 INFO JDBC Driver Version: 4.25.13
2021-05-09 13:24:32 INFO Performance Logger Started
2021-05-09 13:24:32 INFO SBK Benchmark Started
2021-05-09 13:24:32 INFO Reader 0 exited with EOF
Db2 Reading      59102 records,   59102.0 records/sec,     5.64 MB/sec,      0.0 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 90th,       0 ms 95th,       0 ms 99th,       2 ms 99.9th,       3 ms 99.99th.
Total : Db2 Reading      59102 records,   59102.0 records/sec,     5.64 MB/sec,      0.0 ms avg latency,       4 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 90th,       0 ms 95th,       0 ms 99th,       2 ms 99.9th,       3 ms 99.99th.
2021-05-09 13:24:32 INFO Performance Logger Shutdown
2021-05-09 13:24:33 INFO SBK PrometheusLogger Shutdown
2021-05-09 13:24:34 INFO SBK Benchmark Shutdown

```

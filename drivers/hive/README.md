<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Apache Hive Performance using SBK
The Apache Hive driver for SBK supports single writer and multiple readers performance benchmarking. But, the End to End Latency benchmarking is not supported.
Using this JDBC support of SBK, you can conduct the performance benchmarking of any data base which provides JDBC driver.
The Apache Hive driver uses the JDBC for IO operations.

An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class hive -table table1 -writers 1 -size 100 -seconds 120
```

by default, the SBK uses the local hive cluster url: jdbc:hive2://localhost:10000/default;auth=noSasl
if you want to use remote cluster you specify the same using '-url' option


you can use the below docker image to setup the standalone Hive cluster. 
```
docker run -it -p10000:10000 -p8088:8088 jqcoffey/hive-standalone
```

you check your local host : http://localhost:8088 for to observe the IO operations and addtional metrics.

Sample SBK Hive write output

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class hive -table table1 -writers 1 -size 100 -seconds 120                                                       
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-10-18 15:52:30 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-10-18 15:52:30 INFO SBK version: 0.83
2020-10-18 15:52:30 INFO Argument List: [-class, hive, -table, table1, -writers, 1, -size, 100, -seconds, 120]
2020-10-18 15:52:30 INFO sbk.applicationName: sbk
2020-10-18 15:52:30 INFO sbk.className: 
2020-10-18 15:52:30 INFO Reflections took 56 ms to scan 25 urls, producing 34 keys and 130 values 
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2020-10-18 15:52:30 INFO Supplied authorities: localhost:10000
2020-10-18 15:52:30 INFO Resolved authority: localhost:10000
2020-10-18 15:52:30 INFO Will try to open client transport with JDBC Uri: jdbc:hive2://localhost:10000/default;auth=noSasl
2020-10-18 15:52:31 INFO JDBC Driver Type: hive2
2020-10-18 15:52:31 INFO JDBC Driver Name: Hive JDBC
2020-10-18 15:52:31 INFO JDBC Driver Version: 1.1.0
2020-10-18 15:52:31 INFO Deleting the Table: table1
2020-10-18 15:52:31 INFO Creating the Table: table1
2020-10-18 15:52:31 INFO Supplied authorities: localhost:10000
2020-10-18 15:52:31 INFO Resolved authority: localhost:10000
2020-10-18 15:52:31 INFO Will try to open client transport with JDBC Uri: jdbc:hive2://localhost:10000/default;auth=noSasl
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          1 records,       0.2 records/sec,     0.00 MB/sec,  19162.0 ms avg latency,   19162 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:   19162 ms 10th,   19162 ms 25th,   19162 ms 50th,   19162 ms 75th,   19162 ms 95th,   19162 ms 99th,   19162 ms 99.9th,   19162 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          1 records,       0.2 records/sec,     0.00 MB/sec,  16052.0 ms avg latency,   16052 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:   16052 ms 10th,   16052 ms 25th,   16052 ms 50th,   16052 ms 75th,   16052 ms 95th,   16052 ms 99th,   16052 ms 99.9th,   16052 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          1 records,       0.2 records/sec,     0.00 MB/sec,  17065.0 ms avg latency,   17065 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:   17065 ms 10th,   17065 ms 25th,   17065 ms 50th,   17065 ms 75th,   17065 ms 95th,   17065 ms 99th,   17065 ms 99.9th,   17065 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          1 records,       0.2 records/sec,     0.00 MB/sec,  15782.0 ms avg latency,   15782 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:   15782 ms 10th,   15782 ms 25th,   15782 ms 50th,   15782 ms 75th,   15782 ms 95th,   15782 ms 99th,   15782 ms 99.9th,   15782 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          1 records,       0.2 records/sec,     0.00 MB/sec,  15536.0 ms avg latency,   15536 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:   15536 ms 10th,   15536 ms 25th,   15536 ms 50th,   15536 ms 75th,   15536 ms 95th,   15536 ms 99th,   15536 ms 99.9th,   15536 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          1 records,       0.2 records/sec,     0.00 MB/sec,  16197.0 ms avg latency,   16197 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:   16197 ms 10th,   16197 ms 25th,   16197 ms 50th,   16197 ms 75th,   16197 ms 95th,   16197 ms 99th,   16197 ms 99.9th,   16197 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
Writing          1 records,       0.2 records/sec,     0.00 MB/sec,  15379.0 ms avg latency,   15379 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:   15379 ms 10th,   15379 ms 25th,   15379 ms 50th,   15379 ms 75th,   15379 ms 95th,   15379 ms 99th,   15379 ms 99.9th,   15379 ms 99.99th.
Writing(Total)          7 records,       0.1 records/sec,     0.00 MB/sec,  16453.3 ms avg latency,   19162 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:   15379 ms 10th,   15536 ms 25th,   16052 ms 50th,   17065 ms 75th,   19162 ms 95th,   19162 ms 99th,   19162 ms 99.9th,   19162 ms 99.99th.

```
The sample SBK Hive read output is below
```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class hive -table table1 -readers 1 -size 100 -seconds 120
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-10-18 15:57:08 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-10-18 15:57:08 INFO SBK version: 0.83
2020-10-18 15:57:08 INFO Argument List: [-class, hive, -table, table1, -readers, 1, -size, 100, -seconds, 120]
2020-10-18 15:57:08 INFO sbk.applicationName: sbk
2020-10-18 15:57:08 INFO sbk.className: 
2020-10-18 15:57:08 INFO Reflections took 57 ms to scan 25 urls, producing 34 keys and 130 values 
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2020-10-18 15:57:09 INFO Supplied authorities: localhost:10000
2020-10-18 15:57:09 INFO Resolved authority: localhost:10000
2020-10-18 15:57:09 INFO Will try to open client transport with JDBC Uri: jdbc:hive2://localhost:10000/default;auth=noSasl
2020-10-18 15:57:09 INFO JDBC Driver Type: hive2
2020-10-18 15:57:09 INFO JDBC Driver Name: Hive JDBC
2020-10-18 15:57:09 INFO JDBC Driver Version: 1.1.0
2020-10-18 15:57:09 INFO Supplied authorities: localhost:10000
2020-10-18 15:57:09 INFO Resolved authority: localhost:10000
2020-10-18 15:57:09 INFO Will try to open client transport with JDBC Uri: jdbc:hive2://localhost:10000/default;auth=noSasl
2020-10-18 15:57:09 INFO Reader 0 exited with EOF
Reading(Total)          5 records,      14.6 records/sec,     0.00 MB/sec,      0.0 ms avg latency,       0 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 95th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th.
2020-10-18 15:57:09 INFO Supplied authorities: localhost:10000
2020-10-18 15:57:09 INFO Resolved authority: localhost:10000
2020-10-18 15:57:09 INFO Will try to open client transport with JDBC Uri: jdbc:hive2://localhost:10000/default;auth=noSasl

```

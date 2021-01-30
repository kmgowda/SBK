<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SQLite Performance using SBK
The SQLite driver for SBK supports single writer and multiple readers performance benchmarking. But, the End to End Latency benchmarking is not supported.
The SQLite driver uses the JDBC for IO operations.

An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class sqlite -size 100 -writers 1 -seconds 60 
```

by default, the SBK uses the local sqlite database: jdbc:sqlite:test.db, and default table name is 'test'


Sample SBK SQLte write output

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class sqlite -size 100 -writers 1 -seconds 60 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-17 15:07:31 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-17 15:07:31 INFO SBK version: 0.841
2021-01-17 15:07:31 INFO Argument List: [-class, sqlite, -size, 100, -writers, 1, -seconds, 60]
2021-01-17 15:07:31 INFO sbk.applicationName: sbk
2021-01-17 15:07:31 INFO sbk.className: 
2021-01-17 15:07:32 INFO Reflections took 57 ms to scan 27 urls, producing 40 keys and 144 values 
2021-01-17 15:07:32 INFO Available Drivers : 26
2021-01-17 15:07:32 INFO Time Unit: MILLISECONDS
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-01-17 15:07:32 INFO JDBC Driver Type: sqlite
2021-01-17 15:07:32 INFO JDBC Driver Name: SQLite JDBC
2021-01-17 15:07:32 INFO JDBC Driver Version: 3.31.1
2021-01-17 15:07:32 INFO Creating the Table: test
SQLite Writing     1750159 records,  349961.8 records/sec,    33.37 MB/sec,      0.0 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1798136 records,  359555.3 records/sec,    34.29 MB/sec,      0.0 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1759190 records,  351767.6 records/sec,    33.55 MB/sec,      0.0 ms avg latency,      23 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1774780 records,  354885.0 records/sec,    33.84 MB/sec,      0.0 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1714255 records,  342782.4 records/sec,    32.69 MB/sec,      0.0 ms avg latency,     145 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1799376 records,  359803.2 records/sec,    34.31 MB/sec,      0.0 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1778270 records,  355582.9 records/sec,    33.91 MB/sec,      0.0 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1782146 records,  356357.9 records/sec,    33.98 MB/sec,      0.0 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1765531 records,  353035.6 records/sec,    33.67 MB/sec,      0.0 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1777216 records,  355372.1 records/sec,    33.89 MB/sec,      0.0 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1608245 records,  321584.7 records/sec,    30.67 MB/sec,      0.0 ms avg latency,     371 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing     1792273 records,  359244.9 records/sec,    34.26 MB/sec,      0.0 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
SQLite Writing(Total)     21099577 records,  351659.6 records/sec,    33.54 MB/sec,      0.0 ms avg latency,     371 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 


```
The sample SBK SQLite read output is below
```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class sqlite -size 100 -readers 1 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-17 15:24:23 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-17 15:24:23 INFO SBK version: 0.841
2021-01-17 15:24:23 INFO Argument List: [-class, sqlite, -size, 100, -readers, 1, -seconds, 60]
2021-01-17 15:24:23 INFO sbk.applicationName: sbk
2021-01-17 15:24:23 INFO sbk.className: 
2021-01-17 15:24:24 INFO Reflections took 59 ms to scan 27 urls, producing 40 keys and 144 values 
2021-01-17 15:24:24 INFO Available Drivers : 26
2021-01-17 15:24:24 INFO Time Unit: MILLISECONDS
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-01-17 15:24:25 INFO JDBC Driver Type: sqlite
2021-01-17 15:24:25 INFO JDBC Driver Name: SQLite JDBC
2021-01-17 15:24:25 INFO JDBC Driver Version: 3.31.1
SQLite Reading     4853861 records,  970578.1 records/sec,    92.56 MB/sec,      0.0 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 
SQLite Reading     5118366 records, 1023468.5 records/sec,    97.61 MB/sec,      0.0 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 
SQLite Reading     4824856 records,  964778.2 records/sec,    92.01 MB/sec,      0.0 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 
SQLite Reading     5415187 records, 1082820.8 records/sec,   103.27 MB/sec,      0.0 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 
2021-01-17 15:24:46 INFO Reader 0 exited with EOF
SQLite Reading      888353 records,  888353.0 records/sec,    84.72 MB/sec,      0.0 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 
SQLite Reading(Total)     21100623 records, 1009309.4 records/sec,    96.26 MB/sec,      0.0 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 

```

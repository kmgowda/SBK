<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# PostgreSQL Performance using SBK
The PostgreSQL driver for SBK supports multiple writers and multiple readers performance benchmarking. But, the End to End Latency benchmarking is not supported.
The PostgreSQL driver uses the auto incrementing index are primary key to support multiple writers.
The PostgreSQL driver uses the JDBC for IO operations.

As an example, to start the mysql storage server as a container is as follows:

```
docker run  -p 127.0.0.1:5432:5432/tcp  --name kmg-postgres -e POSTGRES_USER=root -e POSTGRES_PASSWORD=root -d postgres
```

An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class postgresql  -size 100 -writers 1 -time 60 
```

by default, the SBK uses the url: jdbc:postgresql://localhost:5432/postgres, and default table name is 'test'
the default username is 'root' and the default password is 'root'.

Sample SBK PostgeSQL write output is follows

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class postgresql  -size 100 -writers 1 -time 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-18 11:27:56 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-18 11:27:56 INFO SBK version: 0.841
2021-01-18 11:27:56 INFO Argument List: [-class, postgresql, -size, 100, -writers, 1, -time, 60]
2021-01-18 11:27:56 INFO sbk.applicationName: sbk
2021-01-18 11:27:56 INFO sbk.className: 
2021-01-18 11:27:56 INFO Reflections took 55 ms to scan 30 urls, producing 41 keys and 147 values 
2021-01-18 11:27:56 INFO Available Drivers : 29
2021-01-18 11:27:56 INFO Time Unit: MILLISECONDS
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-01-18 11:27:56 INFO JDBC Driver Type: postgresql
2021-01-18 11:27:56 INFO JDBC Driver Name: PostgreSQL JDBC Driver
2021-01-18 11:27:56 INFO JDBC Driver Version: 42.1.4
2021-01-18 11:27:56 INFO Deleting the Table: test
2021-01-18 11:27:56 INFO ERROR: table "test" does not exist
2021-01-18 11:27:56 INFO Creating the Table: test
PostgreSQL Writing        6389 records,    1277.3 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th. 
PostgreSQL Writing        6458 records,    1291.3 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       1 ms 99.9th,       6 ms 99.99th. 
PostgreSQL Writing        6044 records,    1207.8 records/sec,     0.12 MB/sec,      0.8 ms avg latency,      32 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,      32 ms 99.99th. 
PostgreSQL Writing        6176 records,    1235.0 records/sec,     0.12 MB/sec,      0.8 ms avg latency,      43 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,      43 ms 99.99th. 
PostgreSQL Writing        6412 records,    1281.6 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       1 ms 99.9th,       2 ms 99.99th. 
PostgreSQL Writing        6644 records,    1328.5 records/sec,     0.13 MB/sec,      0.8 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       1 ms 99.9th,       2 ms 99.99th. 
PostgreSQL Writing        6018 records,    1202.4 records/sec,     0.11 MB/sec,      0.8 ms avg latency,     124 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,     124 ms 99.99th. 
PostgreSQL Writing        6467 records,    1293.1 records/sec,     0.12 MB/sec,      0.8 ms avg latency,      17 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,      17 ms 99.99th. 
PostgreSQL Writing        6434 records,    1286.5 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       1 ms 99.9th,       3 ms 99.99th. 
PostgreSQL Writing        6197 records,    1238.7 records/sec,     0.12 MB/sec,      0.8 ms avg latency,      10 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,      10 ms 99.99th. 
PostgreSQL Writing        6192 records,    1238.2 records/sec,     0.12 MB/sec,      0.8 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       1 ms 99.9th,       5 ms 99.99th. 
PostgreSQL Writing        5629 records,    1131.0 records/sec,     0.11 MB/sec,      0.9 ms avg latency,     427 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,     427 ms 99.99th. 
PostgreSQL Writing(Total)        75060 records,    1251.0 records/sec,     0.12 MB/sec,      0.8 ms avg latency,     427 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,       6 ms 99.99th. 

```
The sample SBK PostgeSQL read output is below

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class postgresql  -size 100 -readers 1 -time 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-18 11:29:19 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-18 11:29:19 INFO SBK version: 0.841
2021-01-18 11:29:19 INFO Argument List: [-class, postgresql, -size, 100, -readers, 1, -time, 60]
2021-01-18 11:29:19 INFO sbk.applicationName: sbk
2021-01-18 11:29:19 INFO sbk.className: 
2021-01-18 11:29:19 INFO Reflections took 58 ms to scan 30 urls, producing 41 keys and 147 values 
2021-01-18 11:29:19 INFO Available Drivers : 29
2021-01-18 11:29:19 INFO Time Unit: MILLISECONDS
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-01-18 11:29:20 INFO JDBC Driver Type: postgresql
2021-01-18 11:29:20 INFO JDBC Driver Name: PostgreSQL JDBC Driver
2021-01-18 11:29:20 INFO JDBC Driver Version: 42.1.4
2021-01-18 11:29:20 INFO Reader 0 exited with EOF
PostgreSQL Reading       75066 records,   75066.0 records/sec,     7.16 MB/sec,      0.0 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 
PostgreSQL Reading(Total)        75066 records,   75066.0 records/sec,     7.16 MB/sec,      0.0 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 

```


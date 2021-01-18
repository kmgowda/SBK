<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# MicrosoftSQL Performance Benchmarking using SBK
The MicrosoftSQL driver for SBK supports multiple writers and multiple readers performance benchmarking. But, the End to End Latency benchmarking is not supported.
The MicrosoftSQL driver uses the auto incrementing index are primary key to support multiple writers.
The MicrosoftSQL driver uses the JDBC for IO operations.

As an example, to start the MSSQL storage server as a container is as follows:

```
docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=root@1234' -p 1433:1433 -d mcr.microsoft.com/mssql/server:2017-latest
```

An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class mssql  -size 100 -writers 1 -time 60 
```

by default, the SBK uses the url: jdbc:sqlserver://localhost:1433, and default table name is 'test'
the default username is 'sa' and the default password is 'root@1234'

Sample SBK MicrosoftSQL write output is follows

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class mssql  -size 100 -writers 1 -time 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-18 15:55:04 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-18 15:55:04 INFO SBK version: 0.841
2021-01-18 15:55:04 INFO Argument List: [-class, mssql, -size, 100, -writers, 1, -time, 60]
2021-01-18 15:55:04 INFO sbk.applicationName: sbk
2021-01-18 15:55:04 INFO sbk.className: 
2021-01-18 15:55:04 INFO Reflections took 54 ms to scan 32 urls, producing 41 keys and 149 values 
2021-01-18 15:55:04 INFO Available Drivers : 31
2021-01-18 15:55:04 INFO Time Unit: MILLISECONDS
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-01-18 15:55:09 INFO JDBC Driver Type: sqlserver
2021-01-18 15:55:09 INFO JDBC Driver Name: Microsoft JDBC Driver 8.2 for SQL Server
2021-01-18 15:55:09 INFO JDBC Driver Version: 8.2.2.0
2021-01-18 15:55:10 INFO Deleting the Table: test
2021-01-18 15:55:10 INFO Cannot drop the table 'test', because it does not exist or you do not have permission.
2021-01-18 15:55:10 INFO Creating the Table: test
MsSql Writing        4475 records,     894.8 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       4 ms 99.9th,       7 ms 99.99th. 
MsSql Writing        4563 records,     912.4 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      18 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       6 ms 99.9th,      18 ms 99.99th. 
MsSql Writing        4448 records,     889.4 records/sec,     0.08 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       5 ms 99.9th,       7 ms 99.99th. 
MsSql Writing        4450 records,     889.8 records/sec,     0.08 MB/sec,      1.1 ms avg latency,       9 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       4 ms 99.9th,       9 ms 99.99th. 
MsSql Writing        4438 records,     887.4 records/sec,     0.08 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       4 ms 99.9th,       7 ms 99.99th. 
MsSql Writing        4502 records,     899.3 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       7 ms 99.99th. 
MsSql Writing        4483 records,     896.4 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      15 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,      15 ms 99.99th. 
MsSql Writing        4563 records,     912.2 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       7 ms 99.99th. 
MsSql Writing        4638 records,     927.4 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       7 ms 99.99th. 
MsSql Writing        4519 records,     903.6 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       4 ms 99.9th,       7 ms 99.99th. 
MsSql Writing        4528 records,     905.4 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       9 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       9 ms 99.99th. 
MsSql Writing        4569 records,     916.9 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      10 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,      10 ms 99.99th. 
MsSql Writing(Total)        54176 records,     902.9 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      18 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       3 ms 99.9th,       8 ms 99.99th. 

```
The sample SBK MicrosoftSQL read output is below

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class mssql  -size 100 -readers 1 -time 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-18 15:57:50 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-18 15:57:50 INFO SBK version: 0.841
2021-01-18 15:57:50 INFO Argument List: [-class, mssql, -size, 100, -readers, 1, -time, 60]
2021-01-18 15:57:50 INFO sbk.applicationName: sbk
2021-01-18 15:57:50 INFO sbk.className: 
2021-01-18 15:57:50 INFO Reflections took 55 ms to scan 32 urls, producing 41 keys and 149 values 
2021-01-18 15:57:50 INFO Available Drivers : 31
2021-01-18 15:57:50 INFO Time Unit: MILLISECONDS
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-01-18 15:57:55 INFO JDBC Driver Type: sqlserver
2021-01-18 15:57:55 INFO JDBC Driver Name: Microsoft JDBC Driver 8.2 for SQL Server
2021-01-18 15:57:55 INFO JDBC Driver Version: 8.2.2.0
2021-01-18 15:57:56 INFO Reader 0 exited with EOF
MsSql Reading       54180 records,   54180.0 records/sec,     5.17 MB/sec,      0.0 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 
MsSql Reading(Total)        54180 records,   54180.0 records/sec,     5.17 MB/sec,      0.0 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       1 ms 99.9th,       1 ms 99.99th. 

```

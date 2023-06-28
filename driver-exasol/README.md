<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Exasol Performance benchmarking using SBK
The Exasol driver for SBK supports multiple writers and multiple readers performance benchmarking. But, the End to End Latency benchmarking is not supported.
The Exasol driver uses the auto incrementing index are primary key to support multiple writers.
The Exasol driver uses the JDBC for IO operations.

As an example, to start the Exasol storage server as a container is as follows:

https://github.com/exasol/docker-db#creating-a-stand-alone-exasol-container

<b>Exasol currently only support Docker on Linux. If you are using a Windows host you'd have to create a Linux VM.</b>
```
$ docker run --name exasoldb -p 127.0.0.1:9563:8563 --detach --privileged --stop-timeout 120  exasol/docker-db:<version>
```

Here we have used a free trial offering of Exasol db . You can follow the step and register.It will spin up
the database and mail the username and password along with the host. We can start benchmarking on that database.

```
https://docs.exasol.com/db/latest/get_started/trial/publicdemosystem.htm
https://docs.exasol.com/db/latest/connect_exasol/sql_clients/dbeaver.htm
```



An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk.bat -class exasol -writers 1 -size 100 -seconds 60 -url jdbc:exa://localhost:8563/SYS -user <USER_NAME> -password <PASSWORD>
./build/install/sbk/bin/sbk.bat -class exasol -readers 1 -size 100 -seconds 60 -url jdbc:exa://localhost:8563/SYS -user <USER_NAME> -password <PASSWORD> 
```

by default, the SBK uses the url: jdbc:exa:localhost:8563;schema=SYS, and default table name is 'test'
the default username is 'sys' and the default password is 'exasol'.
Here we are using the credential and host of our free trial server .
Host: DEMODB.EXASOL.COM

Sample SBK Exasol write output is follows: 

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk.bat -class exasol -writers 1 -size 100 -seconds 60 -url "jdbc:exa:DEMODB.EXASOL.COM:8563;schema=PUBD4459" -user PUBD4459 -password  '"v+aR<Cj~Z{;i7.~w"'
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-12-14 10:52:52 INFO Reflections took 153 ms to scan 51 urls, producing 76 keys and 191 values
2022-12-14 10:52:53 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-12-14 10:52:53 INFO Storage Benchmark Kit
2022-12-14 10:52:53 INFO SBK Version: 3.0-pre1
2022-12-14 10:52:53 INFO SBK Website: https://github.com/kmgowda/SBK
2022-12-14 10:52:53 INFO Arguments List: [-class, exasol, -writers, 1, -size, 100, -seconds, 60, -url, jdbc:exa:DEMODB.EXASOL.COM:8563;schema=PUBD4459, -user, PUBD4459, -password, v+aR<Cj~Z{;i7.~w]
2022-12-14 10:52:53 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-12-14 10:52:53 INFO SBP Version Major: 2, Minor: 0
2022-12-14 10:52:53 INFO Storage Drivers Package: io.sbk
2022-12-14 10:52:53 INFO sbk.applicationName: sbk
2022-12-14 10:52:53 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-12-14 10:52:53 INFO sbk.className:
2022-12-14 10:52:53 INFO '-class': exasol
2022-12-14 10:52:53 INFO Available Storage Drivers in package 'io.sbk': 45 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, Dynamodb, Exasol, FdbRecord, File, FileStream, FoundationDB, H2,
HDFS, Hive, Jdbc, Kafka, LevelDB, MariaDB, Memcached, MinIO, MongoDB, MsSql, MySQL,
Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis,
RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-12-14 10:52:53 INFO Arguments to Driver 'Exasol' : [-writers, 1, -size, 100, -seconds, 60, -url, jdbc:exa:DEMODB.EXASOL.COM:8563;schema=PUBD4459, -user, PUBD4459, -password, v+aR<Cj~Z{;i7.~w]
2022-12-14 10:52:53 INFO Time Unit: MILLISECONDS
2022-12-14 10:52:53 INFO Minimum Latency: 0 ms
2022-12-14 10:52:53 INFO Maximum Latency: 180000 ms
2022-12-14 10:52:53 INFO Window Latency Store: Array, Size: 1 MB
2022-12-14 10:52:53 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-12-14 10:52:53 INFO Total Window Extension: None, Size: 0 MB
2022-12-14 10:52:53 INFO SBK Benchmark Started
2022-12-14 10:52:53 INFO SBK PrometheusLogger Started
2022-12-14 10:52:53 INFO JDBC Url: jdbc:exa:DEMODB.EXASOL.COM:8563;schema=PUBD4459
2022-12-14 10:52:59 INFO JDBC Driver Type: exa
2022-12-14 10:52:59 INFO JDBC Driver Name: EXASolution JDBC Driver
2022-12-14 10:52:59 INFO JDBC Driver Version: 7.1.16
2022-12-14 10:53:02 INFO Deleting the Table: test
2022-12-14 10:53:03 INFO Creating the Table: test
2022-12-14 10:53:09 INFO CQueuePerl Start
2022-12-14 10:53:09 INFO Performance Recorder Started
2022-12-14 10:53:09 INFO SBK Benchmark initiated Writers
2022-12-14 10:53:09 INFO Writer 0 started , run seconds: 60
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               11 records,         2.2 records/sec,     0.00 MB/sec,    455.9 ms avg latency,     270 ms min latency,     879 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     270 ms 5th,     270 ms 10th,     283 ms 20th,     283 ms 25th,     297 ms 30th,     362 ms 40th,     407 ms 50th,     475 ms 60th,     487 ms 70th,     565 ms 75th,     565 ms 80th,     720 ms 90th,     879 ms 92.5th,     879 ms 95th,     879 ms 97.5th,     879 ms 99th,     879 ms 99.25th,     879 ms 99.5th,     879 ms 99.75th,     879 ms 99.9th,     879 ms 99.95th,     879 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               12 records,         2.3 records/sec,     0.00 MB/sec,    431.1 ms avg latency,     333 ms min latency,     549 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     333 ms 5th,     363 ms 10th,     377 ms 20th,     379 ms 25th,     379 ms 30th,     390 ms 40th,     442 ms 50th,     442 ms 60th,     454 ms 70th,     500 ms 75th,     500 ms 80th,     536 ms 90th,     549 ms 92.5th,     549 ms 95th,     549 ms 97.5th,     549 ms 99th,     549 ms 99.25th,     549 ms 99.5th,     549 ms 99.75th,     549 ms 99.9th,     549 ms 99.95th,     549 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               12 records,         2.2 records/sec,     0.00 MB/sec,    456.5 ms avg latency,     330 ms min latency,     740 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     330 ms 5th,     331 ms 10th,     377 ms 20th,     408 ms 25th,     408 ms 30th,     423 ms 40th,     456 ms 50th,     456 ms 60th,     469 ms 70th,     514 ms 75th,     514 ms 80th,     550 ms 90th,     740 ms 92.5th,     740 ms 95th,     740 ms 97.5th,     740 ms 99th,     740 ms 99.25th,     740 ms 99.5th,     740 ms 99.75th,     740 ms 99.9th,     740 ms 99.95th,     740 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               10 records,         2.0 records/sec,     0.00 MB/sec,    512.8 ms avg latency,     284 ms min latency,     786 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     284 ms 5th,     362 ms 10th,     377 ms 20th,     377 ms 25th,     426 ms 30th,     504 ms 40th,     532 ms 50th,     534 ms 60th,     567 ms 70th,     567 ms 75th,     756 ms 80th,     786 ms 90th,     786 ms 92.5th,     786 ms 95th,     786 ms 97.5th,     786 ms 99th,     786 ms 99.25th,     786 ms 99.5th,     786 ms 99.75th,     786 ms 99.9th,     786 ms 99.95th,     786 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               12 records,         2.3 records/sec,     0.00 MB/sec,    441.5 ms avg latency,     332 ms min latency,     864 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     332 ms 5th,     332 ms 10th,     357 ms 20th,     360 ms 25th,     360 ms 30th,     392 ms 40th,     413 ms 50th,     436 ms 60th,     440 ms 70th,     456 ms 75th,     456 ms 80th,     504 ms 90th,     864 ms 92.5th,     864 ms 95th,     864 ms 97.5th,     864 ms 99th,     864 ms 99.25th,     864 ms 99.5th,     864 ms 99.75th,     864 ms 99.9th,     864 ms 99.95th,     864 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               10 records,         1.9 records/sec,     0.00 MB/sec,    523.2 ms avg latency,     340 ms min latency,     790 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     340 ms 5th,     369 ms 10th,     393 ms 20th,     393 ms 25th,     424 ms 30th,     473 ms 40th,     556 ms 50th,     621 ms 60th,     623 ms 70th,     623 ms 75th,     643 ms 80th,     790 ms 90th,     790 ms 92.5th,     790 ms 95th,     790 ms 97.5th,     790 ms 99th,     790 ms 99.25th,     790 ms 99.5th,     790 ms 99.75th,     790 ms 99.9th,     790 ms 99.95th,     790 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               11 records,         2.2 records/sec,     0.00 MB/sec,    455.9 ms avg latency,     275 ms min latency,     719 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     275 ms 5th,     285 ms 10th,     343 ms 20th,     343 ms 25th,     362 ms 30th,     380 ms 40th,     439 ms 50th,     439 ms 60th,     472 ms 70th,     582 ms 75th,     582 ms 80th,     719 ms 90th,     719 ms 92.5th,     719 ms 95th,     719 ms 97.5th,     719 ms 99th,     719 ms 99.25th,     719 ms 99.5th,     719 ms 99.75th,     719 ms 99.9th,     719 ms 99.95th,     719 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               14 records,         2.6 records/sec,     0.00 MB/sec,    378.9 ms avg latency,     282 ms min latency,     517 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     282 ms 5th,     282 ms 10th,     312 ms 20th,     313 ms 25th,     344 ms 30th,     345 ms 40th,     390 ms 50th,     393 ms 60th,     422 ms 70th,     422 ms 75th,     454 ms 80th,     469 ms 90th,     469 ms 92.5th,     517 ms 95th,     517 ms 97.5th,     517 ms 99th,     517 ms 99.25th,     517 ms 99.5th,     517 ms 99.75th,     517 ms 99.9th,     517 ms 99.95th,     517 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               13 records,         2.4 records/sec,     0.00 MB/sec,    411.7 ms avg latency,     313 ms min latency,     547 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     313 ms 5th,     343 ms 10th,     360 ms 20th,     375 ms 25th,     375 ms 30th,     391 ms 40th,     394 ms 50th,     406 ms 60th,     468 ms 70th,     468 ms 75th,     470 ms 80th,     470 ms 90th,     547 ms 92.5th,     547 ms 95th,     547 ms 97.5th,     547 ms 99th,     547 ms 99.25th,     547 ms 99.5th,     547 ms 99.75th,     547 ms 99.9th,     547 ms 99.95th,     547 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               11 records,         2.2 records/sec,     0.00 MB/sec,    462.2 ms avg latency,     282 ms min latency,     861 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:     282 ms 5th,     298 ms 10th,     359 ms 20th,     359 ms 25th,     376 ms 30th,     405 ms 40th,     423 ms 50th,     454 ms 60th,     502 ms 70th,     562 ms 75th,     562 ms 80th,     562 ms 90th,     861 ms 92.5th,     861 ms 95th,     861 ms 97.5th,     861 ms 99th,     861 ms 99.25th,     861 ms 99.5th,     861 ms 99.75th,     861 ms 99.9th,     861 ms 99.95th,     861 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        5 seconds,         0.0 MB,               11 records,         2.1 records/sec,     0.00 MB/sec,    484.7 ms avg latency,     267 ms min latency,    1412 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   2; Latency Percentiles:     267 ms 5th,     281 ms 10th,     286 ms 20th,     286 ms 25th,     297 ms 30th,     314 ms 40th,     394 ms 50th,     406 ms 60th,     423 ms 70th,     486 ms 75th,     486 ms 80th,     766 ms 90th,    1412 ms 92.5th,    1412 ms 95th,    1412 ms 97.5th,    1412 ms 99th,    1412 ms 99.25th,    1412 ms 99.5th,    1412 ms 99.75th,    1412 ms 99.9th,    1412 ms 99.95th,    1412 ms 99.99th
Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        2 seconds,         0.0 MB,                8 records,         2.8 records/sec,     0.00 MB/sec,    362.3 ms avg latency,     297 ms min latency,     410 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   0; Latency Percentiles:     297 ms 5th,     297 ms 10th,     312 ms 20th,     344 ms 25th,     344 ms 30th,     346 ms 40th,     390 ms 50th,     390 ms 60th,     391 ms 70th,     408 ms 75th,     408 ms 80th,     410 ms 90th,     410 ms 92.5th,     410 ms 95th,     410 ms 97.5th,     410 ms 99th,     410 ms 99.25th,     410 ms 99.5th,     410 ms 99.75th,     410 ms 99.9th,     410 ms 99.95th,     410 ms 99.99th
Total Exasol Writing     1 writers,     0 readers,      1 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,
0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,       60 seconds,         0.0 MB,              135 records,         2.2 records/sec,     0.00 MB/sec,    446.7 ms avg latency,     267 ms min latency,    1412 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:     282 ms 5th,     297 ms 10th,     343 ms 20th,     357 ms 25th,     362 ms 30th,     391 ms 40th,     408 ms 50th,     439 ms 60th,     469 ms 70th,     487 ms 75th,     532 ms 80th,     623 ms 90th,     719 ms 92.5th,     766 ms 95th,     861 ms 97.5th,     879 ms 99th,     879 ms 99.25th,    1412 ms 99.5th,    1412 ms 99.75th,    1412 ms 99.9th,    1412 ms 99.95th,    1412 ms 99.99th


2022-12-14 10:54:09 INFO Performance Recorder Exited
2022-12-14 10:54:09 INFO CQueuePerl Shutdown
2022-12-14 10:54:11 INFO Writer 0 exited
2022-12-14 10:54:16 INFO SBK PrometheusLogger Shutdown
2022-12-14 10:54:17 INFO SBK Benchmark Shutdown
```
The sample SBK Exasol read output is below

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk.bat -class exasol -readers 1 -size 100 -seconds 60 -url "jdbc:exa:DEMODB.EXASOL.COM:8563;schema=PUBD4459" -user PUBD4459 -password  '"v+aR<Cj~Z{;i7.~w"'
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-12-14 10:56:21 INFO Reflections took 153 ms to scan 51 urls, producing 76 keys and 191 values
2022-12-14 10:56:21 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-12-14 10:56:21 INFO Storage Benchmark Kit
2022-12-14 10:56:21 INFO SBK Version: 3.0-pre1
2022-12-14 10:56:21 INFO SBK Website: https://github.com/kmgowda/SBK
2022-12-14 10:56:21 INFO Arguments List: [-class, exasol, -readers, 1, -size, 100, -seconds, 60, -url, jdbc:exa:DEMODB.EXASOL.COM:8563;schema=PUBD4459, -user, PUBD4459, -password, v+aR<Cj~Z{;i7.~w]
2022-12-14 10:56:21 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-12-14 10:56:21 INFO SBP Version Major: 2, Minor: 0
2022-12-14 10:56:21 INFO Storage Drivers Package: io.sbk
2022-12-14 10:56:21 INFO sbk.applicationName: sbk
2022-12-14 10:56:21 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-12-14 10:56:21 INFO sbk.className:
2022-12-14 10:56:21 INFO '-class': exasol
2022-12-14 10:56:21 INFO Available Storage Drivers in package 'io.sbk': 45 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, Dynamodb, Exasol, FdbRecord, File, FileStream, FoundationDB, H2,
HDFS, Hive, Jdbc, Kafka, LevelDB, MariaDB, Memcached, MinIO, MongoDB, MsSql, MySQL,
Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis,
RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-12-14 10:56:21 INFO Arguments to Driver 'Exasol' : [-readers, 1, -size, 100, -seconds, 60, -url, jdbc:exa:DEMODB.EXASOL.COM:8563;schema=PUBD4459, -user, PUBD4459, -password, v+aR<Cj~Z{;i7.~w]
2022-12-14 10:56:22 INFO Time Unit: MILLISECONDS
2022-12-14 10:56:22 INFO Minimum Latency: 0 ms
2022-12-14 10:56:22 INFO Maximum Latency: 180000 ms
2022-12-14 10:56:22 INFO Window Latency Store: Array, Size: 1 MB
2022-12-14 10:56:22 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-12-14 10:56:22 INFO Total Window Extension: None, Size: 0 MB
2022-12-14 10:56:22 INFO SBK Benchmark Started
2022-12-14 10:56:22 INFO SBK PrometheusLogger Started
2022-12-14 10:56:22 INFO JDBC Url: jdbc:exa:DEMODB.EXASOL.COM:8563;schema=PUBD4459
2022-12-14 10:56:25 INFO JDBC Driver Type: exa
2022-12-14 10:56:25 INFO JDBC Driver Name: EXASolution JDBC Driver
2022-12-14 10:56:25 INFO JDBC Driver Version: 7.1.16
2022-12-14 10:56:28 INFO CQueuePerl Start
2022-12-14 10:56:28 INFO Performance Recorder Started
2022-12-14 10:56:28 INFO SBK Benchmark initiated Readers
2022-12-14 10:56:28 INFO Reader 0 started , run seconds: 60
2022-12-14 10:56:29 INFO Reader 0 exited with EOF
Exasol Reading     0 writers,     0 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        0 seconds,         0.0 MB,              133 records,       157.6 records/sec,     0.02 MB/sec,      0.0 ms avg latency,       0 ms min latency,       0 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 5th,       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       0 ms 60th,       0 ms 70th,       0 ms 75th,       0 ms 80th,       0 ms 90th,       0 ms 92.5th,       0 ms 95th,       0 ms 97.5th,       0 ms 99th,       0 ms 99.25th,       0 ms 99.5th,       0 ms 99.75th,       0 ms 99.9th,       0 ms 99.95th,       0 ms 99.99th

Total Exasol Reading     0 writers,     0 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,
0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,        0 seconds,         0.0 MB,              133 records,       157.6 records/sec,     0.02 MB/sec,      0.0 ms avg latency,       0 ms min latency,       0 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ms 5th,       0 ms 10th,       0 ms 20th,       0 ms 25th,       0 ms 30th,       0 ms 40th,       0 ms 50th,       0 ms 60th,       0 ms 70th,       0 ms 75th,       0 ms 80th,       0 ms 90th,       0 ms 92.5th,       0 ms 95th,       0 ms 97.5th,       0 ms 99th,       0 ms 99.25th,       0 ms 99.5th,       0 ms 99.75th,       0 ms 99.9th,       0 ms 99.95th,       0 ms 99.99th


2022-12-14 10:56:29 INFO Performance Recorder Exited
2022-12-14 10:56:29 INFO CQueuePerl Shutdown
2022-12-14 10:56:34 INFO SBK PrometheusLogger Shutdown
2022-12-14 10:56:35 INFO SBK Benchmark Shutdown
```


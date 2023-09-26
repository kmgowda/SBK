<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# DynamoDb performance benchmarking with SBK
The DynamoDb driver for SBK supports single/multiple Writer , single/multiple reader performance benchmarking.
It supports both local Dynamo db and AWS dynamoDb through config parameters.

## Config Parameters Supported

| Parameter Name | Default Value         | Description                          |
|----------------|-----------------------|--------------------------------------|
| endpoint       | http://localhost:8000 | Endpoint for DynamoDb                |
| table          | test                  | Table Name                           |
| region         | us-east-1             | Region where Dynamo table is created |
| readCapacity   | 5                     | Provisioned Read Capacity            |
| writeCapacity  | 5                     | Provisioned Write Capacity           |
| awsAccessKey   | Dummy                 | AWS Access key                       |
| awsSecretKey   | Dummy                 | AWS Secret Key                       |

We can set up either local or remote AWS connection to measure the performance.


## Setting up DynamoDb local Docker server
To make simple demo/test, you can run the local dynamodb server docker image as follows:

```
docker run -d -p 8000:8000 amazon/dynamodb-local
```

Make sure you have redirected the port 8000 from docker to local system

Now, you can run the DynamoDb benchmarking as follows. 

```
sanja@DESKTOP-VMPKLNN MINGW64 - SBK Driver/SBK (master)
$ ./build/install/sbk/bin/sbk.bat -class dynamodb -writers 1 -size 100 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-10-16 17:49:30 INFO Reflections took 727 ms to scan 51 urls, producing 103 keys and 227 values
2022-10-16 17:49:30 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-10-16 17:49:30 INFO Storage Benchmark Kit
2022-10-16 17:49:31 INFO SBK Version: 1.0
2022-10-16 17:49:31 INFO SBK Website: https://github.com/kmgowda/SBK
2022-10-16 17:49:31 INFO Arguments List: [-class, dynamodb, -writers, 1, -size, 100, -seconds, 60]
2022-10-16 17:49:31 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-10-16 17:49:31 INFO Storage Drivers Package: io.sbk
2022-10-16 17:49:31 INFO sbk.applicationName: sbk
2022-10-16 17:49:31 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-10-16 17:49:31 INFO sbk.className:
2022-10-16 17:49:31 INFO '-class': dynamodb
2022-10-16 17:49:31 INFO Available Storage Drivers in package 'io.sbk': 44 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, Dynamodb, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive,
Jdbc, Kafka, LevelDB, MariaDB, Memcached, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream,
Nsq, Null, OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ,
RocksDB, SeaweedS3, SQLite]
2022-10-16 17:49:31 INFO Arguments to Driver 'Dynamodb' : [-writers, 1, -size, 100, -seconds, 60]
2022-10-16 17:49:32 INFO Time Unit: MILLISECONDS
2022-10-16 17:49:32 INFO Minimum Latency: 0 ms
2022-10-16 17:49:32 INFO Maximum Latency: 180000 ms
2022-10-16 17:49:32 INFO Window Latency Store: Array, Size: 1 MB
2022-10-16 17:49:32 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-10-16 17:49:32 INFO Total Window Extension: None, Size: 0 MB
2022-10-16 17:49:32 INFO SBK Benchmark Started
2022-10-22 17:49:34 INFO SBK PrometheusLogger Started
2022-10-22 17:49:36 INFO Local DynamoDb client created.
DescribeTableResponse(Table=TableDescription(AttributeDefinitions=[AttributeDefinition(AttributeName=key, AttributeType=S)], TableName=test, KeySchema=[KeySchemaElement(AttributeName=key, KeyType=HASH)], TableStatus=ACTIVE, CreationDateTime=2022-09-22T12:19:38.784Z, ProvisionedThroughput=ProvisionedThroughputDescription(LastIncreaseDateTime=1970-01-01T00:00:00Z, LastDecreaseDateTime=1970-01-01T00:00:00Z, NumberOfDecreasesToday=0, ReadCapacityUnits=5, WriteCapacityUnits=5), TableSizeBytes=0, ItemCount=0, TableArn=arn:aws:dynamodb:ddblocal:000000000000:table/test))
2022-10-16 17:49:39 INFO Table created:test
2022-10-16 17:49:39 INFO Storage opened successfully.
2022-10-16 17:49:39 INFO CQueuePerl Start
2022-10-16 17:49:39 INFO Performance Recorder Started
2022-10-16 17:49:39 INFO SBK Benchmark initiated Writers
2022-10-16 17:49:39 INFO Writer 0 started , run seconds: 60
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,              328 records,        65.5 records/sec,     0.01 MB/sec,     15.2 ms avg latency,     183 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:      10 ms 10th,      11 ms 20th,      11 ms 25th,      11 ms 30th,      12 ms 40th,      13 ms 50th,      14 ms 60th,      16 ms 70th,      17 ms 75th,      18 ms 80th,      21 ms 90th,      22 ms 92.5th,      24 ms 95th,      26 ms 97.5th,      29 ms 99th,      47 ms 99.25th,      88 ms 99.5th,     183 ms 99.75th,     183 ms 99.9th,     183 ms 99.95th,     183 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,              510 records,       101.9 records/sec,     0.01 MB/sec,      9.7 ms avg latency,      46 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:       7 ms 10th,       8 ms 20th,       8 ms 25th,       8 ms 30th,       8 ms 40th,       9 ms 50th,       9 ms 60th,      10 ms 70th,      10 ms 75th,      11 ms 80th,      12 ms 90th,      13 ms 92.5th,      14 ms 95th,      17 ms 97.5th,      32 ms 99th,      39 ms 99.25th,      43 ms 99.5th,      45 ms 99.75th,      46 ms 99.9th,      46 ms 99.95th,      46 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,              592 records,       118.3 records/sec,     0.01 MB/sec,      8.5 ms avg latency,      43 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:       6 ms 10th,       7 ms 20th,       7 ms 25th,       7 ms 30th,       7 ms 40th,       8 ms 50th,       8 ms 60th,       9 ms 70th,       9 ms 75th,      10 ms 80th,      11 ms 90th,      12 ms 92.5th,      13 ms 95th,      16 ms 97.5th,      25 ms 99th,      26 ms 99.25th,      29 ms 99.5th,      41 ms 99.75th,      43 ms 99.9th,      43 ms 99.95th,      43 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,              631 records,       125.9 records/sec,     0.01 MB/sec,      7.9 ms avg latency,      85 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   4; Latency Percentiles:       5 ms 10th,       6 ms 20th,       6 ms 25th,       6 ms 30th,       7 ms 40th,       7 ms 50th,       8 ms 60th,       8 ms 70th,       9 ms 75th,      10 ms 80th,      11 ms 90th,      12 ms 92.5th,      13 ms 95th,      15 ms 97.5th,      22 ms 99th,      26 ms 99.25th,      27 ms 99.5th,      31 ms 99.75th,      85 ms 99.9th,      85 ms 99.95th,      85 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,              715 records,       142.8 records/sec,     0.01 MB/sec,      7.0 ms avg latency,      71 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   4; Latency Percentiles:       5 ms 10th,       5 ms 20th,       5 ms 25th,       5 ms 30th,       6 ms 40th,       6 ms 50th,       7 ms 60th,       7 ms 70th,       7 ms 75th,       8 ms 80th,       9 ms 90th,      10 ms 92.5th,      13 ms 95th,      18 ms 97.5th,      23 ms 99th,      26 ms 99.25th,      30 ms 99.5th,      38 ms 99.75th,      71 ms 99.9th,      71 ms 99.95th,      71 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,             1107 records,       221.3 records/sec,     0.02 MB/sec,      4.5 ms avg latency,      27 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:       3 ms 10th,       4 ms 20th,       4 ms 25th,       4 ms 30th,       4 ms 40th,       4 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       7 ms 97.5th,       8 ms 99th,       9 ms 99.25th,      11 ms 99.5th,      14 ms 99.75th,      25 ms 99.9th,      27 ms 99.95th,      27 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,             1227 records,       244.9 records/sec,     0.02 MB/sec,      4.1 ms avg latency,      24 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:       3 ms 10th,       3 ms 20th,       3 ms 25th,       4 ms 30th,       4 ms 40th,       4 ms 50th,       4 ms 60th,       4 ms 70th,       4 ms 75th,       5 ms 80th,       5 ms 90th,       5 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       8 ms 99.5th,      11 ms 99.75th,      14 ms 99.9th,      24 ms 99.95th,      24 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,             1306 records,       261.0 records/sec,     0.02 MB/sec,      3.8 ms avg latency,      25 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:       3 ms 10th,       3 ms 20th,       3 ms 25th,       3 ms 30th,       4 ms 40th,       4 ms 50th,       4 ms 60th,       4 ms 70th,       4 ms 75th,       4 ms 80th,       5 ms 90th,       5 ms 92.5th,       5 ms 95th,       5 ms 97.5th,       6 ms 99th,       6 ms 99.25th,       8 ms 99.5th,      11 ms 99.75th,      18 ms 99.9th,      25 ms 99.95th,      25 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,             1075 records,       214.4 records/sec,     0.02 MB/sec,      4.6 ms avg latency,     289 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:  12; Latency Percentiles:       3 ms 10th,       3 ms 20th,       3 ms 25th,       4 ms 30th,       4 ms 40th,       4 ms 50th,       4 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       7 ms 97.5th,      10 ms 99th,      10 ms 99.25th,      13 ms 99.5th,      24 ms 99.75th,      29 ms 99.9th,     289 ms 99.95th,     289 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,             1239 records,       247.5 records/sec,     0.02 MB/sec,      4.1 ms avg latency,      70 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   5; Latency Percentiles:       3 ms 10th,       3 ms 20th,       3 ms 25th,       3 ms 30th,       3 ms 40th,       4 ms 50th,       4 ms 60th,       4 ms 70th,       4 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       7 ms 97.5th,       9 ms 99th,      10 ms 99.25th,      12 ms 99.5th,      19 ms 99.75th,      25 ms 99.9th,      70 ms 99.95th,      70 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.1 MB,             1279 records,       255.4 records/sec,     0.02 MB/sec,      3.9 ms avg latency,      25 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:       3 ms 10th,       3 ms 20th,       3 ms 25th,       3 ms 30th,       4 ms 40th,       4 ms 50th,       4 ms 60th,       4 ms 70th,       4 ms 75th,       5 ms 80th,       5 ms 90th,       5 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       8 ms 99.5th,       8 ms 99.75th,      21 ms 99.9th,      25 ms 99.95th,      25 ms 99.99th.
2022-10-16 17:50:39 INFO Writer 0 exited
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        4 seconds,         0.1 MB,             1096 records,       222.6 records/sec,     0.02 MB/sec,      4.5 ms avg latency,      29 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   3; Latency Percentiles:       3 ms 10th,       3 ms 20th,       4 ms 25th,       4 ms 30th,       4 ms 40th,       4 ms 50th,       4 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       8 ms 97.5th,      12 ms 99th,      13 ms 99.25th,      16 ms 99.5th,      25 ms 99.75th,      25 ms 99.9th,      29 ms 99.95th,      29 ms 99.99th.
Total : Dynamodb Writing     0 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,       60 seconds,         1.1 MB,            11105 records,       185.1 records/sec,     0.02 MB/sec,      5.4 ms avg latency,     289 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   7; Latency Percentiles:       3 ms 10th,       3 ms 20th,       4 ms 25th,       4 ms 30th,       4 ms 40th,       4 ms 50th,       5 ms 60th,       5 ms 70th,       6 ms 75th,       7 ms 80th,       9 ms 90th,      10 ms 92.5th,      11 ms 95th,      14 ms 97.5th,      19 ms 99th,      21 ms 99.25th,      24 ms 99.5th,      27 ms 99.75th,      41 ms 99.9th,      70 ms 99.95th,     183 ms 99.99th.
2022-10-16 17:50:39 INFO Performance Recorder Exited
2022-10-16 17:50:39 INFO CQueuePerl Shutdown
2022-10-16 17:50:39 INFO SBK PrometheusLogger Shutdown
2022-10-16 17:50:40 INFO SBK Benchmark Shutdown


sanja@DESKTOP-VMPKLNN MINGW64 - SBK Driver/SBK (master)
$ ./build/install/sbk/bin/sbk.bat -class dynamodb -readers 1 -size 100 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-10-16 17:50:52 INFO Reflections took 170 ms to scan 51 urls, producing 103 keys and 227 values
2022-10-16 17:50:53 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-10-16 17:50:53 INFO Storage Benchmark Kit
2022-10-16 17:50:53 INFO SBK Version: 1.0
2022-10-16 17:50:53 INFO SBK Website: https://github.com/kmgowda/SBK
2022-10-16 17:50:53 INFO Arguments List: [-class, dynamodb, -readers, 1, -size, 100, -seconds, 60]
2022-10-16 17:50:53 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-10-16 17:50:53 INFO Storage Drivers Package: io.sbk
2022-10-16 17:50:53 INFO sbk.applicationName: sbk
2022-10-16 17:50:53 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-10-16 17:50:53 INFO sbk.className:
2022-10-16 17:50:53 INFO '-class': dynamodb
2022-10-16 17:50:53 INFO Available Storage Drivers in package 'io.sbk': 44 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, Dynamodb, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive,
Jdbc, Kafka, LevelDB, MariaDB, Memcached, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream,
Nsq, Null, OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ,
RocksDB, SeaweedS3, SQLite]
2022-10-16 17:50:53 INFO Arguments to Driver 'Dynamodb' : [-readers, 1, -size, 100, -seconds, 60]
2022-10-16 17:50:53 INFO Time Unit: MILLISECONDS
2022-10-16 17:50:53 INFO Minimum Latency: 0 ms
2022-10-16 17:50:53 INFO Maximum Latency: 180000 ms
2022-10-16 17:50:53 INFO Window Latency Store: Array, Size: 1 MB
2022-10-16 17:50:53 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-10-16 17:50:53 INFO Total Window Extension: None, Size: 0 MB
2022-10-16 17:50:53 INFO SBK Benchmark Started
2022-10-16 17:50:54 INFO SBK PrometheusLogger Started
2022-10-16 17:50:55 INFO Local DynamoDb client created.
2022-10-16 17:50:56 ERROR Cannot create preexisting table (Service: DynamoDb, Status Code: 400, Request ID: b902a62b-7096-4e40-bcff-f1efe5bb6039)
2022-10-16 17:50:56 ERROR No table created.
2022-10-16 17:50:56 INFO Storage opened successfully.
2022-10-16 17:50:56 INFO CQueuePerl Start
2022-10-16 17:50:56 INFO Performance Recorder Started
2022-10-16 17:50:56 INFO SBK Benchmark initiated Readers
2022-10-16 17:50:56 INFO Reader 0 started , run seconds: 60
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.1 MB,              635 records,       127.0 records/sec,     0.01 MB/sec,      7.8 ms avg latency,     235 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:  10; Latency Percentiles:       4 ms 10th,       5 ms 20th,       5 ms 25th,       5 ms 30th,       6 ms 40th,       6 ms 50th,       7 ms 60th,       8 ms 70th,       9 ms 75th,      10 ms 80th,      12 ms 90th,      13 ms 92.5th,      14 ms 95th,      17 ms 97.5th,      25 ms 99th,      26 ms 99.25th,      30 ms 99.5th,      51 ms 99.75th,     235 ms 99.9th,     235 ms 99.95th,     235 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.1 MB,              953 records,       190.6 records/sec,     0.02 MB/sec,      5.2 ms avg latency,      68 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   4; Latency Percentiles:       4 ms 10th,       4 ms 20th,       4 ms 25th,       4 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       6 ms 75th,       6 ms 80th,       7 ms 90th,       7 ms 92.5th,       8 ms 95th,      10 ms 97.5th,      13 ms 99th,      14 ms 99.25th,      20 ms 99.5th,      22 ms 99.75th,      68 ms 99.9th,      68 ms 99.95th,      68 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.1 MB,             1213 records,       242.6 records/sec,     0.02 MB/sec,      4.1 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       3 ms 10th,       3 ms 20th,       4 ms 25th,       4 ms 30th,       4 ms 40th,       4 ms 50th,       4 ms 60th,       4 ms 70th,       5 ms 75th,       5 ms 80th,       5 ms 90th,       5 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       7 ms 99.5th,       8 ms 99.75th,      11 ms 99.9th,      13 ms 99.95th,      13 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.1 MB,             1298 records,       259.5 records/sec,     0.02 MB/sec,      3.8 ms avg latency,      18 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:       3 ms 10th,       3 ms 20th,       3 ms 25th,       3 ms 30th,       4 ms 40th,       4 ms 50th,       4 ms 60th,       4 ms 70th,       4 ms 75th,       4 ms 80th,       5 ms 90th,       5 ms 92.5th,       5 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       7 ms 99.5th,      10 ms 99.75th,      10 ms 99.9th,      18 ms 99.95th,      18 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.1 MB,             1425 records,       284.6 records/sec,     0.03 MB/sec,      3.5 ms avg latency,       9 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       3 ms 10th,       3 ms 20th,       3 ms 25th,       3 ms 30th,       3 ms 40th,       3 ms 50th,       4 ms 60th,       4 ms 70th,       4 ms 75th,       4 ms 80th,       4 ms 90th,       5 ms 92.5th,       5 ms 95th,       5 ms 97.5th,       5 ms 99th,       6 ms 99.25th,       6 ms 99.5th,       7 ms 99.75th,       7 ms 99.9th,       9 ms 99.95th,       9 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.1 MB,             1415 records,       282.7 records/sec,     0.03 MB/sec,      3.5 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       3 ms 10th,       3 ms 20th,       3 ms 25th,       3 ms 30th,       3 ms 40th,       3 ms 50th,       4 ms 60th,       4 ms 70th,       4 ms 75th,       4 ms 80th,       4 ms 90th,       4 ms 92.5th,       5 ms 95th,       5 ms 97.5th,       6 ms 99th,       6 ms 99.25th,       6 ms 99.5th,       6 ms 99.75th,       6 ms 99.9th,       7 ms 99.95th,       7 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.1 MB,             1532 records,       306.3 records/sec,     0.03 MB/sec,      3.3 ms avg latency,      11 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       2 ms 10th,       3 ms 20th,       3 ms 25th,       3 ms 30th,       3 ms 40th,       3 ms 50th,       3 ms 60th,       4 ms 70th,       4 ms 75th,       4 ms 80th,       4 ms 90th,       4 ms 92.5th,       4 ms 95th,       5 ms 97.5th,       5 ms 99th,       5 ms 99.25th,       6 ms 99.5th,       6 ms 99.75th,       6 ms 99.9th,      11 ms 99.95th,      11 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             1604 records,       320.1 records/sec,     0.03 MB/sec,      3.1 ms avg latency,      11 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       2 ms 10th,       2 ms 20th,       3 ms 25th,       3 ms 30th,       3 ms 40th,       3 ms 50th,       3 ms 60th,       3 ms 70th,       4 ms 75th,       4 ms 80th,       4 ms 90th,       4 ms 92.5th,       5 ms 95th,       5 ms 97.5th,       6 ms 99th,       6 ms 99.25th,       6 ms 99.5th,       6 ms 99.75th,       8 ms 99.9th,      11 ms 99.95th,      11 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             1654 records,       330.7 records/sec,     0.03 MB/sec,      3.0 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       2 ms 10th,       2 ms 20th,       3 ms 25th,       3 ms 30th,       3 ms 40th,       3 ms 50th,       3 ms 60th,       3 ms 70th,       3 ms 75th,       4 ms 80th,       4 ms 90th,       4 ms 92.5th,       4 ms 95th,       5 ms 97.5th,       5 ms 99th,       5 ms 99.25th,       5 ms 99.5th,       6 ms 99.75th,       7 ms 99.9th,       7 ms 99.95th,       7 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             1889 records,       377.7 records/sec,     0.04 MB/sec,      2.6 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       2 ms 10th,       2 ms 20th,       2 ms 25th,       2 ms 30th,       2 ms 40th,       3 ms 50th,       3 ms 60th,       3 ms 70th,       3 ms 75th,       3 ms 80th,       3 ms 90th,       4 ms 92.5th,       4 ms 95th,       4 ms 97.5th,       4 ms 99th,       4 ms 99.25th,       5 ms 99.5th,       5 ms 99.75th,       7 ms 99.9th,       7 ms 99.95th,       7 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.2 MB,             1827 records,       365.2 records/sec,     0.03 MB/sec,      2.7 ms avg latency,       6 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       2 ms 10th,       2 ms 20th,       2 ms 25th,       2 ms 30th,       3 ms 40th,       3 ms 50th,       3 ms 60th,       3 ms 70th,       3 ms 75th,       3 ms 80th,       3 ms 90th,       4 ms 92.5th,       4 ms 95th,       4 ms 97.5th,       4 ms 99th,       5 ms 99.25th,       5 ms 99.5th,       5 ms 99.75th,       6 ms 99.9th,       6 ms 99.95th,       6 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        4 seconds,         0.2 MB,             1722 records,       346.5 records/sec,     0.03 MB/sec,      2.9 ms avg latency,       8 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       2 ms 10th,       2 ms 20th,       2 ms 25th,       3 ms 30th,       3 ms 40th,       3 ms 50th,       3 ms 60th,       3 ms 70th,       3 ms 75th,       3 ms 80th,       4 ms 90th,       4 ms 92.5th,       4 ms 95th,       4 ms 97.5th,       4 ms 99th,       5 ms 99.25th,       5 ms 99.5th,       5 ms 99.75th,       7 ms 99.9th,       8 ms 99.95th,       8 ms 99.99th.
Total : Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,       60 seconds,         1.6 MB,            17167 records,       286.1 records/sec,     0.03 MB/sec,      3.5 ms avg latency,     235 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   4; Latency Percentiles:       2 ms 10th,       3 ms 20th,       3 ms 25th,       3 ms 30th,       3 ms 40th,       3 ms 50th,       3 ms 60th,       4 ms 70th,       4 ms 75th,       4 ms 80th,       5 ms 90th,       5 ms 92.5th,       5 ms 95th,       7 ms 97.5th,       9 ms 99th,      10 ms 99.25th,      12 ms 99.5th,      14 ms 99.75th,      18 ms 99.9th,      25 ms 99.95th,      68 ms 99.99th.
2022-10-16 17:51:56 INFO Performance Recorder Exited
2022-10-16 17:51:56 INFO CQueuePerl Shutdown
2022-10-16 17:51:56 INFO Reader 0 exited
2022-10-16 17:51:56 INFO SBK PrometheusLogger Shutdown
2022-10-16 17:51:57 INFO SBK Benchmark Shutdown

```

## Connecting to AWS Dynamo DB

```
sanja@DESKTOP-VMPKLNN MINGW64 - SBK Driver/SBK (master)
$ ./build/install/sbk/bin/sbk.bat -class dynamodb -writers 1 -size 100 -seconds 60 -awsAccessKey AKIAZPGQQHMFYHIA4V6H -awsSecretKey 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  -readCapacity 3 -writeCapacity 3
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-10-16 17:53:37 INFO Reflections took 405 ms to scan 51 urls, producing 103 keys and 227 values
2022-10-16 17:53:37 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-10-16 17:53:37 INFO Storage Benchmark Kit
2022-10-16 17:53:37 INFO SBK Version: 1.0
2022-10-16 17:53:37 INFO SBK Website: https://github.com/kmgowda/SBK
2022-10-16 17:53:37 INFO Arguments List: [-class, dynamodb, -writers, 1, -size, 100, -seconds, 60, -awsAccessKey, AKIAZPGQQHMFYHIA4V6H, -awsSecretKey, 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, -readCapacity, 3, -writeCapacity, 3]
2022-10-16 17:53:37 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-10-16 17:53:37 INFO Storage Drivers Package: io.sbk
2022-10-16 17:53:37 INFO sbk.applicationName: sbk
2022-10-16 17:53:37 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-10-16 17:53:37 INFO sbk.className:
2022-10-16 17:53:37 INFO '-class': dynamodb
2022-10-1609-22 17:53:37 INFO Available Storage Drivers in package 'io.sbk': 44 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, Dynamodb, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive,
Jdbc, Kafka, LevelDB, MariaDB, Memcached, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream,
Nsq, Null, OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ,
RocksDB, SeaweedS3, SQLite]
2022-10-16 17:53:37 INFO Arguments to Driver 'Dynamodb' : [-writers, 1, -size, 100, -seconds, 60, -awsAccessKey, AKIAZPGQQHMFYHIA4V6H, -awsSecretKey, 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, -readCapacity, 3, -writeCapacity, 3]
2022-10-16 17:53:38 INFO Time Unit: MILLISECONDS
2022-10-16 17:53:38 INFO Minimum Latency: 0 ms
2022-10-16 17:53:38 INFO Maximum Latency: 180000 ms
2022-10-16 17:53:38 INFO Window Latency Store: Array, Size: 1 MB
2022-10-16 17:53:38 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-10-16 17:53:38 INFO Total Window Extension: None, Size: 0 MB
2022-10-16 17:53:38 INFO SBK Benchmark Started
2022-10-16 17:53:39 INFO SBK PrometheusLogger Started
2022-10-16 17:53:42 INFO AWS DynamoDb client created.
DescribeTableResponse(Table=TableDescription(AttributeDefinitions=[AttributeDefinition(AttributeName=key, AttributeType=S)], TableName=test, KeySchema=[KeySchemaElement(AttributeName=key, KeyType=HASH)], TableStatus=ACTIVE, CreationDateTime=2022-09-22T12:23:46.683Z, ProvisionedThroughput=ProvisionedThroughputDescription(NumberOfDecreasesToday=0, ReadCapacityUnits=3, WriteCapacityUnits=3), TableSizeBytes=0, ItemCount=0, TableArn=arn:aws:dynamodb:us-east-1:651124816651:table/test, TableId=f941c982-0a1c-43f5-992e-a3a6d4efe202))
2022-10-16 17:54:04 INFO Table created:test
2022-10-16 17:54:04 INFO Storage opened successfully.
2022-10-16 17:54:04 INFO CQueuePerl Start
2022-10-16 17:54:04 INFO Performance Recorder Started
2022-10-16 17:54:04 INFO SBK Benchmark initiated Writers
2022-10-16 17:54:04 INFO Writer 0 started , run seconds: 60
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               20 records,         4.0 records/sec,     0.00 MB/sec,    250.5 ms avg latency,     839 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   2; Latency Percentiles:     215 ms 10th,     216 ms 20th,     216 ms 25th,     216 ms 30th,     217 ms 40th,     218 ms 50th,     219 ms 60th,     222 ms 70th,     223 ms 75th,     229 ms 80th,     236 ms 90th,     236 ms 92.5th,     839 ms 95th,     839 ms 97.5th,     839 ms 99th,     839 ms 99.25th,     839 ms 99.5th,     839 ms 99.75th,     839 ms 99.9th,     839 ms 99.95th,     839 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               24 records,         4.6 records/sec,     0.00 MB/sec,    216.1 ms avg latency,     219 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     214 ms 10th,     215 ms 20th,     215 ms 25th,     215 ms 30th,     215 ms 40th,     216 ms 50th,     217 ms 60th,     217 ms 70th,     218 ms 75th,     218 ms 80th,     219 ms 90th,     219 ms 92.5th,     219 ms 95th,     219 ms 97.5th,     219 ms 99th,     219 ms 99.25th,     219 ms 99.5th,     219 ms 99.75th,     219 ms 99.9th,     219 ms 99.95th,     219 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               23 records,         4.5 records/sec,     0.00 MB/sec,    221.0 ms avg latency,     276 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     213 ms 10th,     213 ms 20th,     214 ms 25th,     214 ms 30th,     215 ms 40th,     215 ms 50th,     217 ms 60th,     218 ms 70th,     219 ms 75th,     222 ms 80th,     226 ms 90th,     264 ms 92.5th,     264 ms 95th,     276 ms 97.5th,     276 ms 99th,     276 ms 99.25th,     276 ms 99.5th,     276 ms 99.75th,     276 ms 99.9th,     276 ms 99.95th,     276 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               24 records,         4.6 records/sec,     0.00 MB/sec,    215.5 ms avg latency,     222 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     213 ms 10th,     214 ms 20th,     214 ms 25th,     215 ms 30th,     215 ms 40th,     215 ms 50th,     215 ms 60th,     216 ms 70th,     216 ms 75th,     216 ms 80th,     218 ms 90th,     221 ms 92.5th,     221 ms 95th,     222 ms 97.5th,     222 ms 99th,     222 ms 99.25th,     222 ms 99.5th,     222 ms 99.75th,     222 ms 99.9th,     222 ms 99.95th,     222 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               24 records,         4.6 records/sec,     0.00 MB/sec,    215.8 ms avg latency,     227 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     213 ms 10th,     214 ms 20th,     214 ms 25th,     214 ms 30th,     215 ms 40th,     215 ms 50th,     216 ms 60th,     217 ms 70th,     217 ms 75th,     217 ms 80th,     218 ms 90th,     219 ms 92.5th,     219 ms 95th,     227 ms 97.5th,     227 ms 99th,     227 ms 99.25th,     227 ms 99.5th,     227 ms 99.75th,     227 ms 99.9th,     227 ms 99.95th,     227 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               24 records,         4.7 records/sec,     0.00 MB/sec,    215.0 ms avg latency,     220 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     213 ms 10th,     214 ms 20th,     214 ms 25th,     214 ms 30th,     214 ms 40th,     215 ms 50th,     215 ms 60th,     216 ms 70th,     216 ms 75th,     216 ms 80th,     217 ms 90th,     218 ms 92.5th,     218 ms 95th,     220 ms 97.5th,     220 ms 99th,     220 ms 99.25th,     220 ms 99.5th,     220 ms 99.75th,     220 ms 99.9th,     220 ms 99.95th,     220 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               23 records,         4.6 records/sec,     0.00 MB/sec,    219.7 ms avg latency,     295 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     214 ms 10th,     214 ms 20th,     214 ms 25th,     215 ms 30th,     215 ms 40th,     215 ms 50th,     215 ms 60th,     216 ms 70th,     217 ms 75th,     219 ms 80th,     221 ms 90th,     230 ms 92.5th,     230 ms 95th,     295 ms 97.5th,     295 ms 99th,     295 ms 99.25th,     295 ms 99.5th,     295 ms 99.75th,     295 ms 99.9th,     295 ms 99.95th,     295 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               24 records,         4.6 records/sec,     0.00 MB/sec,    215.4 ms avg latency,     219 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     213 ms 10th,     213 ms 20th,     214 ms 25th,     214 ms 30th,     215 ms 40th,     215 ms 50th,     215 ms 60th,     216 ms 70th,     218 ms 75th,     218 ms 80th,     219 ms 90th,     219 ms 92.5th,     219 ms 95th,     219 ms 97.5th,     219 ms 99th,     219 ms 99.25th,     219 ms 99.5th,     219 ms 99.75th,     219 ms 99.9th,     219 ms 99.95th,     219 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               24 records,         4.7 records/sec,     0.00 MB/sec,    215.0 ms avg latency,     219 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     213 ms 10th,     213 ms 20th,     214 ms 25th,     214 ms 30th,     214 ms 40th,     215 ms 50th,     215 ms 60th,     215 ms 70th,     216 ms 75th,     216 ms 80th,     218 ms 90th,     218 ms 92.5th,     218 ms 95th,     219 ms 97.5th,     219 ms 99th,     219 ms 99.25th,     219 ms 99.5th,     219 ms 99.75th,     219 ms 99.9th,     219 ms 99.95th,     219 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               24 records,         4.6 records/sec,     0.00 MB/sec,    215.2 ms avg latency,     221 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     214 ms 10th,     214 ms 20th,     214 ms 25th,     214 ms 30th,     214 ms 40th,     215 ms 50th,     215 ms 60th,     215 ms 70th,     216 ms 75th,     217 ms 80th,     217 ms 90th,     218 ms 92.5th,     218 ms 95th,     221 ms 97.5th,     221 ms 99th,     221 ms 99.25th,     221 ms 99.5th,     221 ms 99.75th,     221 ms 99.9th,     221 ms 99.95th,     221 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,               23 records,         4.5 records/sec,     0.00 MB/sec,    220.5 ms avg latency,     271 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     214 ms 10th,     214 ms 20th,     214 ms 25th,     214 ms 30th,     215 ms 40th,     215 ms 50th,     216 ms 60th,     218 ms 70th,     223 ms 75th,     224 ms 80th,     229 ms 90th,     240 ms 92.5th,     240 ms 95th,     271 ms 97.5th,     271 ms 99th,     271 ms 99.25th,     271 ms 99.5th,     271 ms 99.75th,     271 ms 99.9th,     271 ms 99.95th,     271 ms 99.99th.
Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        3 seconds,         0.0 MB,               17 records,         4.6 records/sec,     0.00 MB/sec,    216.6 ms avg latency,     227 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     213 ms 10th,     213 ms 20th,     214 ms 25th,     214 ms 30th,     214 ms 40th,     215 ms 50th,     217 ms 60th,     217 ms 70th,     218 ms 75th,     220 ms 80th,     225 ms 90th,     225 ms 92.5th,     227 ms 95th,     227 ms 97.5th,     227 ms 99th,     227 ms 99.25th,     227 ms 99.5th,     227 ms 99.75th,     227 ms 99.9th,     227 ms 99.95th,     227 ms 99.99th.
Total : Dynamodb Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,       60 seconds,         0.0 MB,              274 records,         4.6 records/sec,     0.00 MB/sec,    219.3 ms avg latency,     839 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     213 ms 10th,     214 ms 20th,     214 ms 25th,     214 ms 30th,     215 ms 40th,     215 ms 50th,     216 ms 60th,     217 ms 70th,     217 ms 75th,     218 ms 80th,     220 ms 90th,     222 ms 92.5th,     227 ms 95th,     236 ms 97.5th,     276 ms 99th,     276 ms 99.25th,     295 ms 99.5th,     839 ms 99.75th,     839 ms 99.9th,     839 ms 99.95th,     839 ms 99.99th.
2022-10-16 17:55:04 INFO Performance Recorder Exited
2022-10-16 17:55:04 INFO CQueuePerl Shutdown
2022-10-16 17:55:05 INFO Writer 0 exited
2022-10-16 17:55:05 INFO SBK PrometheusLogger Shutdown
2022-10-16 17:55:06 INFO SBK Benchmark Shutdown


sanja@DESKTOP-VMPKLNN MINGW64 - SBK Driver/SBK (master)
$ ./build/install/sbk/bin/sbk.bat -class dynamodb -readers 1 -size 100 -seconds 60 -awsAccessKey AKIAZPGQQHMFYHIA4V6H -awsSecretKey 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  -readCapacity 3 -writeCapacity 3
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-10-16 17:55:30 INFO Reflections took 248 ms to scan 51 urls, producing 103 keys and 227 values
2022-10-16 17:55:30 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-10-16 17:55:30 INFO Storage Benchmark Kit
2022-10-16 17:55:30 INFO SBK Version: 1.0
2022-10-16 17:55:30 INFO SBK Website: https://github.com/kmgowda/SBK
2022-10-16 17:55:30 INFO Arguments List: [-class, dynamodb, -readers, 1, -size, 100, -seconds, 60, -awsAccessKey, AKIAZPGQQHMFYHIA4V6H, -awsSecretKey, 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, -readCapacity, 3, -writeCapacity, 3]
2022-10-16 17:55:30 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-10-16 17:55:30 INFO Storage Drivers Package: io.sbk
2022-10-16 17:55:30 INFO sbk.applicationName: sbk
2022-10-16 17:55:30 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-10-16 17:55:30 INFO sbk.className:
2022-10-16 17:55:30 INFO '-class': dynamodb
2022-10-16 17:55:30 INFO Available Storage Drivers in package 'io.sbk': 44 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, Dynamodb, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive,
Jdbc, Kafka, LevelDB, MariaDB, Memcached, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream,
Nsq, Null, OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ,
RocksDB, SeaweedS3, SQLite]
2022-10-16 17:55:30 INFO Arguments to Driver 'Dynamodb' : [-readers, 1, -size, 100, -seconds, 60, -awsAccessKey, AKIAZPGQQHMFYHIA4V6H, -awsSecretKey, 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, -readCapacity, 3, -writeCapacity, 3]
2022-10-16 17:55:30 INFO Time Unit: MILLISECONDS
2022-10-16 17:55:30 INFO Minimum Latency: 0 ms
2022-10-16 17:55:30 INFO Maximum Latency: 180000 ms
2022-10-16 17:55:30 INFO Window Latency Store: Array, Size: 1 MB
2022-10-16 17:55:30 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-10-16 17:55:30 INFO Total Window Extension: None, Size: 0 MB
2022-10-16 17:55:30 INFO SBK Benchmark Started
2022-10-16 17:55:31 INFO SBK PrometheusLogger Started
2022-10-16 17:55:31 INFO AWS DynamoDb client created.
2022-10-16 17:55:33 ERROR Table already exists: test (Service: DynamoDb, Status Code: 400, Request ID: R8QIGCBC7J2343TSF7IIJSBH43VV4KQNSO5AEMVJF66Q9ASUAAJG)
2022-10-16 17:55:33 ERROR No table created.
2022-10-16 17:55:33 INFO Storage opened successfully.
2022-10-16 17:55:33 INFO CQueuePerl Start
2022-10-16 17:55:33 INFO Performance Recorder Started
2022-10-16 17:55:33 INFO SBK Benchmark initiated Readers
2022-10-16 17:55:33 INFO Reader 0 started , run seconds: 60
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               21 records,         4.2 records/sec,     0.00 MB/sec,    237.3 ms avg latency,     398 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     228 ms 10th,     228 ms 20th,     228 ms 25th,     228 ms 30th,     229 ms 40th,     229 ms 50th,     229 ms 60th,     230 ms 70th,     230 ms 75th,     230 ms 80th,     233 ms 90th,     237 ms 92.5th,     237 ms 95th,     398 ms 97.5th,     398 ms 99th,     398 ms 99.25th,     398 ms 99.5th,     398 ms 99.75th,     398 ms 99.9th,     398 ms 99.95th,     398 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.3 records/sec,     0.00 MB/sec,    230.0 ms avg latency,     253 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     226 ms 10th,     227 ms 20th,     227 ms 25th,     227 ms 30th,     228 ms 40th,     229 ms 50th,     229 ms 60th,     230 ms 70th,     231 ms 75th,     232 ms 80th,     233 ms 90th,     234 ms 92.5th,     234 ms 95th,     253 ms 97.5th,     253 ms 99th,     253 ms 99.25th,     253 ms 99.5th,     253 ms 99.75th,     253 ms 99.9th,     253 ms 99.95th,     253 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.4 records/sec,     0.00 MB/sec,    229.0 ms avg latency,     242 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     226 ms 10th,     227 ms 20th,     227 ms 25th,     227 ms 30th,     227 ms 40th,     228 ms 50th,     228 ms 60th,     229 ms 70th,     230 ms 75th,     231 ms 80th,     233 ms 90th,     235 ms 92.5th,     235 ms 95th,     242 ms 97.5th,     242 ms 99th,     242 ms 99.25th,     242 ms 99.5th,     242 ms 99.75th,     242 ms 99.9th,     242 ms 99.95th,     242 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.4 records/sec,     0.00 MB/sec,    228.7 ms avg latency,     236 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     226 ms 10th,     227 ms 20th,     227 ms 25th,     227 ms 30th,     227 ms 40th,     228 ms 50th,     229 ms 60th,     229 ms 70th,     230 ms 75th,     231 ms 80th,     233 ms 90th,     233 ms 92.5th,     233 ms 95th,     236 ms 97.5th,     236 ms 99th,     236 ms 99.25th,     236 ms 99.5th,     236 ms 99.75th,     236 ms 99.9th,     236 ms 99.95th,     236 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.3 records/sec,     0.00 MB/sec,    232.4 ms avg latency,     282 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     226 ms 10th,     226 ms 20th,     226 ms 25th,     227 ms 30th,     227 ms 40th,     227 ms 50th,     228 ms 60th,     228 ms 70th,     229 ms 75th,     230 ms 80th,     242 ms 90th,     272 ms 92.5th,     272 ms 95th,     282 ms 97.5th,     282 ms 99th,     282 ms 99.25th,     282 ms 99.5th,     282 ms 99.75th,     282 ms 99.9th,     282 ms 99.95th,     282 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.4 records/sec,     0.00 MB/sec,    227.9 ms avg latency,     247 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     225 ms 10th,     225 ms 20th,     225 ms 25th,     226 ms 30th,     226 ms 40th,     227 ms 50th,     227 ms 60th,     227 ms 70th,     229 ms 75th,     229 ms 80th,     230 ms 90th,     232 ms 92.5th,     232 ms 95th,     247 ms 97.5th,     247 ms 99th,     247 ms 99.25th,     247 ms 99.5th,     247 ms 99.75th,     247 ms 99.9th,     247 ms 99.95th,     247 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.4 records/sec,     0.00 MB/sec,    228.0 ms avg latency,     235 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     226 ms 10th,     226 ms 20th,     226 ms 25th,     226 ms 30th,     227 ms 40th,     227 ms 50th,     228 ms 60th,     229 ms 70th,     229 ms 75th,     231 ms 80th,     232 ms 90th,     232 ms 92.5th,     232 ms 95th,     235 ms 97.5th,     235 ms 99th,     235 ms 99.25th,     235 ms 99.5th,     235 ms 99.75th,     235 ms 99.9th,     235 ms 99.95th,     235 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.4 records/sec,     0.00 MB/sec,    229.8 ms avg latency,     260 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     225 ms 10th,     226 ms 20th,     226 ms 25th,     226 ms 30th,     227 ms 40th,     228 ms 50th,     228 ms 60th,     229 ms 70th,     230 ms 75th,     230 ms 80th,     232 ms 90th,     244 ms 92.5th,     244 ms 95th,     260 ms 97.5th,     260 ms 99th,     260 ms 99.25th,     260 ms 99.5th,     260 ms 99.75th,     260 ms 99.9th,     260 ms 99.95th,     260 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.3 records/sec,     0.00 MB/sec,    231.0 ms avg latency,     284 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     226 ms 10th,     226 ms 20th,     226 ms 25th,     226 ms 30th,     227 ms 40th,     228 ms 50th,     228 ms 60th,     230 ms 70th,     230 ms 75th,     231 ms 80th,     234 ms 90th,     238 ms 92.5th,     238 ms 95th,     284 ms 97.5th,     284 ms 99th,     284 ms 99.25th,     284 ms 99.5th,     284 ms 99.75th,     284 ms 99.9th,     284 ms 99.95th,     284 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.3 records/sec,     0.00 MB/sec,    231.9 ms avg latency,     244 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:     228 ms 10th,     229 ms 20th,     229 ms 25th,     229 ms 30th,     230 ms 40th,     231 ms 50th,     233 ms 60th,     234 ms 70th,     234 ms 75th,     234 ms 80th,     236 ms 90th,     237 ms 92.5th,     237 ms 95th,     244 ms 97.5th,     244 ms 99th,     244 ms 99.25th,     244 ms 99.5th,     244 ms 99.75th,     244 ms 99.9th,     244 ms 99.95th,     244 ms 99.99th.
Dynamodb Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.0 MB,               22 records,         4.4 records/sec,     0.00 MB/sec,    228.5 ms avg latency,     257 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     225 ms 10th,     225 ms 20th,     226 ms 25th,     226 ms 30th,     226 ms 40th,     227 ms 50th,     227 ms 60th,     229 ms 70th,     229 ms 75th,     230 ms 80th,     230 ms 90th,     232 ms 92.5th,     232 ms 95th,     257 ms 97.5th,     257 ms 99th,     257 ms 99.25th,     257 ms 99.5th,     257 ms 99.75th,     257 ms 99.9th,     257 ms 99.95th,     257 ms 99.99th.
2022-10-16 17:56:33 INFO Reader 0 exited
Dynamodb Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,        4 seconds,         0.0 MB,               19 records,         4.1 records/sec,     0.00 MB/sec,    229.3 ms avg latency,     271 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     225 ms 10th,     225 ms 20th,     226 ms 25th,     226 ms 30th,     227 ms 40th,     227 ms 50th,     227 ms 60th,     228 ms 70th,     228 ms 75th,     229 ms 80th,     232 ms 90th,     232 ms 92.5th,     271 ms 95th,     271 ms 97.5th,     271 ms 99th,     271 ms 99.25th,     271 ms 99.5th,     271 ms 99.75th,     271 ms 99.9th,     271 ms 99.95th,     271 ms 99.99th.
Total : Dynamodb Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,       60 seconds,         0.0 MB,              260 records,         4.3 records/sec,     0.00 MB/sec,    230.3 ms avg latency,     398 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:     226 ms 10th,     226 ms 20th,     227 ms 25th,     227 ms 30th,     227 ms 40th,     228 ms 50th,     229 ms 60th,     230 ms 70th,     230 ms 75th,     231 ms 80th,     234 ms 90th,     235 ms 92.5th,     242 ms 95th,     257 ms 97.5th,     282 ms 99th,     284 ms 99.25th,     284 ms 99.5th,     398 ms 99.75th,     398 ms 99.9th,     398 ms 99.95th,     398 ms 99.99th.
2022-10-16 17:56:33 INFO Performance Recorder Exited
2022-10-16 17:56:33 INFO CQueuePerl Shutdown
2022-10-16 17:56:33 INFO SBK PrometheusLogger Shutdown
2022-10-16 17:56:34 INFO SBK Benchmark Shutdown

```
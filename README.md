<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Storage Benchmark Kit  ![SBK](images/sbk-logo-small-1.png)

[![Build Status](https://travis-ci.org/kmgowda/SBK.svg?branch=master)](https://travis-ci.org/kmgowda/SBK) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)  [![Api](https://img.shields.io/badge/SBK-API-brightgreen)](https://kmgowda.github.io/SBK/sbk-api/javadoc/index.html) [![Version](https://img.shields.io/github/v/release/kmgowda/sbk)](https://github.com/kmgowda/SBK/releases) [![](https://jitpack.io/v/kmgowda/SBK.svg)](https://jitpack.io/#kmgowda/SBK) [![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4606/badge)](https://bestpractices.coreinfrastructure.org/projects/4606)

>  **_Any Storage System_...  _Any Payload_...  _Any Time Stamp_...**

[![SBK-RAM](https://img.shields.io/badge/SBK-RAM-red)](sbk-ram)
[![SBK-GEM](https://img.shields.io/badge/SBK-GEM-orange)](sbk-gem)
[![SBK-GEM](https://img.shields.io/badge/SBK-GEM--YAL-orange)](sbk-gem-yal)

The SBK (Storage Benchmark Kit) is an open source software framework for the performance benchmarking of any storage system. If you are curious to measure the maximum throughput performance of your storage device/system, then SBK is the right software for you. The SBK itself is a very high-performance benchmark tool/framework.
It massively writes the data to the storage system and reads the data from the storage system. The SBK supports multi writers and readers and also the End to End latency benchmarking.
The latency quartiles and percentiles are calculated for complete data written/read without any sampling; hence the 
percentiles are 100% accurate.

The design principle of SBK is the **Performance Benchmarking of _'Any Storage System'_ with _'Any Type of data payload'_ and _'Any Time Stamp'_**,
because, the SBK is not specific to particular type of storage system,
it can be used for performance benchmarking of any storage system, let it be file system, databases , any distributed storage systems or message queues by adding SBK driver which specifies the IO operations of storage system.
you can find the list of supported drivers below.
The SBK supports a variety of payloads too, such as byte array, byte buffer, string, and you can add your own payload type.
The Latency values can be measured either in milliseconds, microseconds or nanoseconds using SBK.

___

<p align="center">
    <a href="https://kmgowda.github.io/SBK/?from=SBK">
        <img src="images/sbk-logo-wide.png" alt="Storage Benchmark Kit" width="750" height="550">
    </a>
</p>

___


*SBK supports performance benchmarking of following storage systems*

| **#** |             **Driver**              | **#** |           **Driver**            |
|:-----:|:-----------------------------------:|:-----:|:-------------------------------:|
|   1   |      [Artemis](driver-artemis)      |  21   |    [Mariadb](driver-mariadb)    |
|   2   |    [Asyncfile](driver-asyncfile)    |  22   |      [Minio](driver-minio)      |
|   3   |   [Bookkeeper](driver-bookkeeper)   |  23   |    [Mongodb](driver-mongodb)    |
|   4   |    [Cassandra](driver-cassandra)    |  24   |      [Mssql](driver-mssql)      |
|   5   |       [Cephs3](driver-cephs3)       |  25   |      [Mysql](driver-mysql)      |
|   6   |  [Concurrentq](driver-concurrentq)  |  26   |       [Nats](driver-nats)       |
|   7   |      [Couchdb](driver-couchdb)      |  27   | [NatsStream](driver-natsStream) |
|   8   |          [Csv](driver-csv)          |  28   |        [Nsq](driver-nsq)        |
|   9   |          [Db2](driver-db2)          |  29   |       [Null](driver-null)       |
|  10   |        [Derby](driver-derby)        |  30   |     [Openio](driver-openio)     |
|  11   |    [Fdbrecord](driver-fdbrecord)    |  31   | [Postgresql](driver-postgresql) |
|  12   |         [File](driver-file)         |  32   |    [Pravega](driver-pravega)    |
|  13   |   [Filestream](driver-filestream)   |  33   |     [Pulsar](driver-pulsar)     |
|  14   | [Foundationdb](driver-foundationdb) |  34   |   [Rabbitmq](driver-rabbitmq)   |
|  15   |         [Hdfs](driver-hdfs)         |  35   |      [Redis](driver-redis)      |
|  16   |         [Hive](driver-hive)         |  36   |   [Redpanda](driver-redpanda)   |
|  17   |       [Ignite](driver-ignite)       |  37   |   [Rocketmq](driver-rocketmq)   |
|  18   |         [Jdbc](driver-jdbc)         |  38   |    [Rocksdb](driver-rocksdb)    |
|  19   |        [Kafka](driver-kafka)        |  39   |  [Seaweeds3](driver-seaweeds3)  |
|  20   |      [Leveldb](driver-leveldb)      |  40   |     [Sqlite](driver-sqlite)     |

*In the future, many more storage systems drivers will be plugged in* 

We welcome open source developers to contribute to this project by adding a driver for your storage device and any features to SBK. Refer to : 
* [Contributing to SBK](#contributing-to-sbk) for the Contributing guidelines.
* [Add your storage driver to SBK](#add-your-driver-to-sbk) to know how to add your driver (storage device driver or 
  client) for performance benchmarking.

___

<p align="center">
   Supported By
</p>

<p align="center">
    <a href="https://www.jetbrains.com/?from=SBK">
        <img src="images/jetbrains.png" alt="Jet Brains" width="100" height="100">
    </a>
</p>

___

## Build SBK

**Prerequisites**

- Java 17+
- Gradle 7+

**Building**

Checkout the source code:

```
git clone https://github.com/kmgowda/SBK.git
cd SBK
```

Build the SBK:

```
./gradlew build
```

untar the SBK  to local folder

```
tar -xvf ./build/distributions/sbk.tar -C ./build/distributions/.
```

Running SBK locally:

```
<SBK directory>/./build/distributions/sbk/bin/sbk -help
...
usage: sbk
Storage Benchmark Kit

 -class <arg>        Storage Driver Class,
                     Available Drivers [Artemis, AsyncFile, BookKeeper,
                     Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2,
                     Derby, FdbRecord, File, FileStream, FoundationDB,
                     HDFS, Hive, Ignite, Jdbc, Kafka, MariaDB, MinIO,
                     MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null,
                     OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis,
                     RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
 -context <arg>      Prometheus Metric context; 'no' disables this option;
                     default: 9718/metrics
 -csvfile <arg>      CSV file to record results; 'no' disables this
                     option, default: no
 -help               Help message
 -maxlatency <arg>   Maximum latency; use '-time' for time unit; default:
                     180000 ms
 -minlatency <arg>   Minimum latency; use '-time' for time unit; default:
                     0 ms
 -ram <arg>          SBK RAM host; 'no' disables this option, default: no
 -ramport <arg>      SBK RAM Port; default: 9717
 -readers <arg>      Number of readers
 -records <arg>      Number of records(events) if 'seconds' not specified;
                     otherwise, Maximum records per second by writer(s)
                     and/or Number of records per second by reader(s)
 -rsec <arg>         Number of seconds/step for readers, default: 0
 -rstep <arg>        Number of readers/step, default: 1
 -seconds <arg>      Number of seconds to run; if not specified, runs
                     forever
 -size <arg>         Size of each message (event or record)
 -sync <arg>         Each Writer calls flush/sync after writing <arg>
                     number of of events(records) ; <arg> number of
                     events(records) per Write or Read Transaction
 -throughput <arg>   If > 0, throughput in MB/s
                     If 0, writes/reads 'records'
                     If -1, get the maximum throughput (default: -1)
 -time <arg>         Latency Time Unit [ms:MILLISECONDS, mcs:MICROSECONDS,
                     ns:NANOSECONDS]; default: ms
 -writers <arg>      Number of writers
 -wsec <arg>         Number of seconds/step for writers, default: 0
 -wstep <arg>        Number of writers/step, default: 1

Please report issues at https://github.com/kmgowda/SBK

```


Just to check the SBK build issue the command

```
./gradlew check
```

Build only the SBK install binary

```
./gradlew installDist
```
executable binary will be available at :  [SBK directory]/./build/install/sbk/bin/sbk


## Running Performance benchmarking
The SBK  can be executed to
 - write/read a specific amount of events/records to/from the storage driver (device/cluster)
 - write/read the events/records for the specified amount of time
 
SBK outputs the data written/read , average throughput and latency, maximum latency  and the latency 
percentiles 10th, 20th, 25th, 30th, 40th, 50th, 60th, 75th, 80th, 90th, 92.5th, 95th, 97.5th, 99th, 99.25th, 99.5th, 99.75th, 99.9th, 99.95th and 99.99th for every 5 seconds time interval as show below.

```
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        81.9 MB,           858754 records,    171682.1 records/sec,    16.37 MB/sec,      5.6 ms avg latency,      51 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   2; Latency Percentiles:       4 ms 10th,       5 ms 20th,       5 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       6 ms 60th,       6 ms 70th,       6 ms 75th,       6 ms 80th,       7 ms 90th,       8 ms 92.5th,       8 ms 95th,       9 ms 97.5th,      10 ms 99th,      11 ms 99.25th,      11 ms 99.5th,      13 ms 99.75th,      21 ms 99.9th,      21 ms 99.95th,      21 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        95.9 MB,          1005395 records,    200958.4 records/sec,    19.16 MB/sec,      4.9 ms avg latency,      24 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       4 ms 10th,       4 ms 20th,       5 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       8 ms 99.5th,       9 ms 99.75th,      11 ms 99.9th,      21 ms 99.95th,      21 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        97.2 MB,          1018748 records,    203627.4 records/sec,    19.42 MB/sec,      4.9 ms avg latency,      14 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       4 ms 10th,       4 ms 20th,       4 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       8 ms 99th,       8 ms 99.25th,       9 ms 99.5th,       9 ms 99.75th,       9 ms 99.9th,       9 ms 99.95th,      10 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        96.9 MB,          1016051 records,    203169.6 records/sec,    19.38 MB/sec,      4.9 ms avg latency,      26 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       4 ms 10th,       4 ms 20th,       4 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       8 ms 99.5th,       8 ms 99.75th,      10 ms 99.9th,      22 ms 99.95th,      22 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        95.4 MB,          1000720 records,    199984.0 records/sec,    19.07 MB/sec,      4.9 ms avg latency,      21 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       4 ms 10th,       4 ms 20th,       5 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       7 ms 97.5th,       7 ms 99th,       8 ms 99.25th,       8 ms 99.5th,       9 ms 99.75th,      16 ms 99.9th,      17 ms 99.95th,      17 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        82.1 MB,           860890 records,    172040.4 records/sec,    16.41 MB/sec,      5.8 ms avg latency,     249 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:  13; Latency Percentiles:       4 ms 10th,       5 ms 20th,       5 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       6 ms 80th,       6 ms 90th,       6 ms 92.5th,       7 ms 95th,       7 ms 97.5th,      11 ms 99th,      14 ms 99.25th,      23 ms 99.5th,      37 ms 99.75th,     239 ms 99.9th,     239 ms 99.95th,     240 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        87.5 MB,           917480 records,    183422.6 records/sec,    17.49 MB/sec,      5.4 ms avg latency,     226 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:  12; Latency Percentiles:       4 ms 10th,       5 ms 20th,       5 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       6 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       7 ms 97.5th,       8 ms 99th,      10 ms 99.25th,      11 ms 99.5th,      24 ms 99.75th,     219 ms 99.9th,     220 ms 99.95th,     220 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        94.8 MB,           994183 records,    198796.8 records/sec,    18.96 MB/sec,      5.0 ms avg latency,      21 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       4 ms 10th,       4 ms 20th,       5 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       8 ms 99.25th,       8 ms 99.5th,      10 ms 99.75th,      16 ms 99.9th,      16 ms 99.95th,      16 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        90.6 MB,           950128 records,    189987.6 records/sec,    18.12 MB/sec,      5.2 ms avg latency,     228 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:  13; Latency Percentiles:       4 ms 10th,       4 ms 20th,       4 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       7 ms 97.5th,       9 ms 99th,       9 ms 99.25th,      10 ms 99.5th,      12 ms 99.75th,     222 ms 99.9th,     222 ms 99.95th,     222 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        96.6 MB,          1013110 records,    202581.5 records/sec,    19.32 MB/sec,      4.9 ms avg latency,      25 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       4 ms 10th,       4 ms 20th,       4 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       7 ms 99.5th,       8 ms 99.75th,      15 ms 99.9th,      21 ms 99.95th,      21 ms 99.99th.
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,        96.3 MB,          1010276 records,    202014.8 records/sec,    19.27 MB/sec,      4.9 ms avg latency,      16 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       4 ms 10th,       4 ms 20th,       5 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       7 ms 99.5th,      10 ms 99.75th,      12 ms 99.9th,      13 ms 99.95th,      14 ms 99.99th.
2021-08-20 14:22:45 INFO [topic-k-1] [standalone-0-0] Pending messages: 1 --- Publish throughput: 194235.46 msg/s --- 148.19 Mbit/s --- Latency: med: 8.565 ms - 95pct: 10.435 ms - 99pct: 14.061 ms - 99.9pct: 212.614 ms - max: 249.145 ms --- Ack received rate: 194218.80 ack/s --- Failed messages: 0
Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        4 seconds,        97.2 MB,          1019666 records,    204875.6 records/sec,    19.54 MB/sec,      4.8 ms avg latency,      16 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   1; Latency Percentiles:       4 ms 10th,       4 ms 20th,       4 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       6 ms 97.5th,       7 ms 99th,       7 ms 99.25th,       7 ms 99.5th,       8 ms 99.75th,      11 ms 99.9th,      12 ms 99.95th,      12 ms 99.99th.

```

At the end of the benchmarking session, SBK outputs the total data written/read , average throughput and latency , 
maximum latency  and the latency percentiles 10th, 20th, 25th, 30th, 40th, 50th, 60th, 75th, 80th, 90th, 92.5th, 95th, 97.5th, 99th, 99.25th, 99.5th, 99.75th, 99.9th, 99.95th and 99.99th for the complete data records written/read.
An example  final output is show as below:

```
Total : Pulsar Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,       60 seconds,      1112.5 MB,         11665401 records,    194423.4 records/sec,    18.54 MB/sec,      5.1 ms avg latency,     249 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   1, SLC-2:   5; Latency Percentiles:       4 ms 10th,       4 ms 20th,       5 ms 25th,       5 ms 30th,       5 ms 40th,       5 ms 50th,       5 ms 60th,       5 ms 70th,       5 ms 75th,       5 ms 80th,       6 ms 90th,       6 ms 92.5th,       6 ms 95th,       7 ms 97.5th,       8 ms 99th,       9 ms 99.25th,       9 ms 99.5th,      11 ms 99.75th,      21 ms 99.9th,      26 ms 99.95th,     222 ms 99.99th.

```

**Sliding Latency Coverage (SLC)  factors**

The SBK yields latency data points in the form of quartiles and percentiles. For the performance analysis, these 
quartiles and percentile latencies can be combined into two factors : Sliding Latency Coverage 1 (SLC 1)
 and Sliding Latency Coverage 2 (SLC 2).

The SLC1 indicates the coefficient of dispersion from lower latency percentile to median percentile. This indicates 
the range between all lower latencies percentiles to median latency and also dispersion from all latency values 
which are below median latency. The SLC2 indicates the coefficient of dispersion from median latency percentile and 
all other percentile values to the last (maximum) percentile (99.99th percentile). If you are comparing two or more 
storage systems which are having similar / approximate median latency percentiles then SLC2 gives which storage system is 
doing better. Lower SLC2 factor means higher the performance of the system. If you are observing too many 
variations of SLC 2 factor that means you have an opportunity to improve the stability of the storage system too.

**Performance results to CSV file**
you can use option "-csvfile" to specify the csv file to log all the performance results. Further you can use [sbk 
charts](#sbk-charts) for generating xlsx files with graphs.

### Grafana Dashboards of SBK
When you run the SBK, by default it starts the http server and all the output benchmark data is directed to the default port number: **9718** and **metrics** context.
If you want to change the port number and context, you can use the command line argument **-context** to change the same.
you have to run the prometheus monitoring system (server [default port number is 9090] cum client) which pulls/fetches the benchmark data from the local/remote http server.
If you want to include additional SBK nodes/instances to fetch the performance data or from port number other than 
9718, you need to extend or update [targets.json](grafana/prometheus/targets.json)
In case, if you are fetching metrics/benchmark data from remote http server , or from the context other than **metrics** then you need to change the [default prometheus server configuration](grafana/prometheus/prometheus.yml) too.
Run the grafana server (cum a client) to fetch the benchmark data from prometheus.
For example, if you are running a local grafana server then by default it fetches the data from the prometheus server at the local port 9090.
You can access the local grafana server at localhost:3000 in your browser using **admin/admin** as default username / password.
You can import the grafana dashboards to fetch the SBK benchmark data of the existing supported storage drivers from 
[grafana dashboards](https://github.com/kmgowda/SBK/tree/master/grafana/dashboards).

The sample output of Standalone Pulsar benchmark data with grafana is below

[![Pulsar Grafana Dashboard](images/pulsar-grafana.png)](images/pulsar-grafana.png)

**Port conflicts between storage servers and grafana/prometheus**
* If you are running Pravega server in standalone/local mode or if you are running SBK in the same system in which Pravega controller is also running, then Prometheus port 9090 conflicts with the Pravega controller. So, either you change the Pravega controller port number or change the Prometheus port number in the [Prometheus targets file](grafana/prometheus/prometheus.yml) before deploying the prometheus. 
* If you find that using the local port 9718 conflicts with a storage server or any other application. Then, you can change the SBK's http port using **-metrics** option, and you need change the [Prometheus targets.json](grafana/prometheus/targets.json) too


### SBK with JMX exporter and Grafana
The SBK can start the java agent to export the JVM metrics to Grafana via Prometheus. you just have build with 
parameter **-PjmxExport=true** while building SBK.

the command is below
```
./gradlew installDist -PjmxExport=true
```
All the SBK JVM metrics will be available at http://localhost:8718/metrics The network port **8718** to used to 
expose the metrics. use [SBK-JMX grafana dashboard](grafana/dashboards/sbk-jmx-metrics.json) to analyse the SBK-JVM 
metrics.


## Distributed SBK
SBK can be deployed in a distributed clusters using [**SBK-RAM**](./sbk-ram), [**SBK-GEM**](./sbk-gem) and 
[**SBK-GEM-YAL**](./sbk-gem-yal)

## SBK Docker Containers
The SBK Docker images are available at [SBK Docker](https://hub.docker.com/r/kmgowda/sbk)

The SBK docker image pull command is 
```
docker pull kmgowda/sbk
```

you can straightaway run the docker image too, For example
```
docker run  -p 127.0.0.1:9718:9718/tcp  kmgowda/sbk:latest -class  rabbitmq  -broker 192.168.0.192 -topic kmg-topic-11  -writers 5  -readers 1 -size 100 -seconds 60
```
* Note that the option **-p 127.0.0.1:9718:9718/tcp** redirects the 9718 port to local port to fetch the performance 
  metric data for Prometheus.  
* Avoid using the **--network host** option , because this option overrides the port redirection.

**SBK Docker Compose**

The SBK docker compose consists of SBK docker image, Grafana and prometheus docker images. 
The [grafana image](grafana) contains the dashboards which can be directly deployed for the performance analytics.

As an example, just follow the below steps to see the performance graphs

1. In the SBK directory build the 'SBK' service of the [docker compose](docker-compose.yml) file as follows.

   ```
   <SBK dir>% docker-compose build 

   ```

1. Run the 'SBK' service as follows.

   ```
   <SBK dir>% docker-compose run sbk  -class concurrentq -writers 1  -readers 5 -size 1000 -seconds 120 

   ```

1. login to [grafana local host port 3000](http://localhost:3000) with username **admin** and password **sbk**
1. go to dashboard menu and pick the dashboard of the storage device on which you are running the performance benchmarking.
   in the above example, you can choose the [Concurrent Queue dashboard](grafana/dashboards/sbk-concurrentq.json).
1. The SBK docker compose runs the SBK image as docker container. 
   In case, if you are running SBK as an application, and you want to see the SBK performance graphs using Grafana,
   then use [Grafana Docker compose](grafana)
   
   
## SBK Kubernetes
check these [SBK Kubernetes Deployments samples](kubernetes) for details 
on SBK as kubernetes pod.
If you want to run the Grafana and prometheus as Kubernetes pods, then use [Grafana Kubernetes deployment](grafana/README.md#grafana-with-kubernetes)


## SBK Metrics Network Ports

| Network Port 	 | Description                               	 |
|----------------|---------------------------------------------|
| 9717         	 | SBK-RAM GRPC server Port                  	 |
| 9718         	 | SBK performance metrics to Prometheus     	 |
| 9719         	 | SBK-RAM performance metrics to Prometheus 	 |
| 8718         	 | SBK JVM/JMX metrics to Prometheus         	 |
| 8719         	 | SBK-RAM JVM/JMX metrics to Prometheus     	 |


## SBK Execution Modes

The SBK can be executed in the following modes:
```
1. Burst Mode (Max rate mode)
2. Throughput Mode
3. Rate limiter Mode
4. End to End Latency Mode
```

**1 - Burst Mode / Max Rate Mode**

In this mode, the SBK pushes/pulls the messages to/from the storage client(device/driver) as much as possible.
This mode is used to find the maximum and throughput that can be obtained from the storage device or storage cluster (server).
This mode can be used for both writers and readers.
By default, the SBK runs in Burst mode.

```
For example: The Burst mode for pulsar single writer as follows

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-223  -partitions 1  -writers 1 -size 1000  -seconds 60 -throughput -1


The -throughput -1 indicates the burst mode. Note that, you don't supply the parameter -throughput then also its burst mode.
This test will be executed for 60 seconds because option -seconds 60 is used.
This test tries to write and read events of size 1000 bytes to/from the topic 'topic-k-223'.
The option '-broker tcp://localhost:6650' specifies the Pulsar broker IP address and port number for write operations.
The option '-admin http://localhost:8080' specifies the Pulsar admin IP and port number for topic creation and deletion.
Note that -producers 1 indicates 1 producer/writers.

in the case you want to write/read the certain number of records.events use the -records option without -seconds option as follows

<SBK directory>/build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-223  -partitions 1  -writers 1 -size 1000  -records 100000 -throughput -1

-records <number> indicates that total <number> of records to write/read
```

**2 - Throughput Mode**

In this mode, the SBK  pushes/pulls/from the messages to the storage client(device/driver) with specified 
approximate maximum throughput in terms of Mega Bytes/second (MB/s).
This mode is used to find the least latency that can be obtained from the storage device or storage cluster (server) for given throughput.


```
For example: The throughput mode for pulsar 5 writers as follows
<SBK directory> ./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-223 -partitions 1 -writers 5 -size 1000 -seconds 120 -throughput 10

The -throughput <positive number> indicates the Throughput mode.

This test will be executed with approximate max throughput of 10MB/sec.
This test will be executed for 120 seconds (2 minutes) because option -seconds 120 is used.
This test tries to write and read events of size 1000 bytes to/from the topic 'topic-k-223' of 1 partition.
If the topic 'topic-k-223' is not existing , then it will be created with 1 segment.
if the steam is already existing then it will be deleted and recreated with 1 segment.
Note that -writers 5 indicates 5 producers/writers .

in the case you want to write/read the certain number of events use the -records option without -seconds option as follows

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-223 -partitions 1 -writers 5 -size 1000 -records 1000000 -throughput 10

-records 1000000 indicates that total 1000000 (1 million) of events will be written at the throughput speed of 10MB/sec
```

**3 - Rate limiter Mode**

This mode is another form of controlling writers/readers throughput by limiting the number of records per second.
In this mode, the SBK pushes/pulls the messages to/from the storage client (device/driver) with specified approximate maximum records per sec.
This mode is used to find the least latency that can be obtained from the storage device or storage cluster (server) for events rate.

```
For example: The Rate limiter Mode for pulsar 5 writers as follows

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broke
r tcp://localhost:6650 -topic topic-k-225 -partitions 10 -writers 5 -size 100 -seconds 60 -records 1000

The -records <records number> (1000) specifies the records per second to write.
Note that the option "-throughput" SHOULD NOT be supplied for this Rate limiter Mode.

This test will be executed with approximately 1000 events per second by 5 writers.
The topic "topic-k-225" with 10 partitions is created to run this test.
This test will be executed for 60seconds (1 minutes) because option -seconds 60 is used.
Note that in this mode, there is 'NO total number of events' to specify hence the user must supply the time to run using the -seconds option.
```

**4 - End to End Latency Mode**

In this mode, the SBK writes and reads the messages to the storage client (device/driver) and records the end to end latency.
End to end latency means the time duration between the beginning of the writing event/record to stream, and the time after reading the event/record.
in this mode user must specify both the number of writers and readers.
The -throughput option (Throughput mode) or -records (late limiter) can be used to limit the writer's throughput or records rate.

```
For example: The End to End latency of between single writer and single reader of pulsar is as follows:

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-km-1 -partitions 1 -writers 1 -readers 1 -size 1000 -throughput -1 -seconds 60 

The user should specify both writers and readers count for write to read or End to End latency mode.
The -throughput -1 specifies the writer tries to write the events at the maximum possible speed.
```

## Contributing to SBK
* All submissions must be adhering to [Apache licence](LICENSE)

All submissions to the master are done through pull requests. If you'd like to make a change:

1. Create a new Github issue ([SBK issues](https://github.com/kmgowda/sbk/issues)) describing the problem / feature.
2. Fork a branch.
3. Make your changes.
   * you can refer ([Oracle Java Coding Style](https://www.oracle.com/technetwork/java/codeconvtoc-136057.html)) for 
     coding style; however, Running the Gradle build helps you to fix the Coding style issues too.
4. Verify all changes are working and Gradle build checkstyle is good.
5. Submit a pull request with Issue number, Description and your Sign-off.

Make sure that you update the issue with all details of testing you have done; it will be helpful for me to review and merge.

Another important point to consider is how to keep up with changes against the base branch (the one your pull request is comparing against). Let's assume that the base branch is master. To make sure that your changes reflect the recent commits, I recommend that you rebase frequently. The command I suggest you use is:

```
git pull --rebase upstream master
git push --force origin <pr-branch-name>
```
in the above, I'm assuming that:

* upstream is https://github.com/kmgowda/SBK.git
* origin is 'your github account/SBK.git'

The rebase might introduce conflicts, so you better do it frequently to avoid outrageous sessions of conflict resolving.

**Lombok**

SBK uses [[Lombok](https://projectlombok.org)] for code optimizations; I suggest the same for all the contributors too.
If you use an IDE you'll need to install a plugin to make the IDE understand it. Using IntelliJ is recommended.

To import the source into IntelliJ:

1. Import the project directory into IntelliJ IDE. It will automatically detect the gradle project and import things correctly.
2. Enable `Annotation Processing` by going to `Build, Execution, Deployment` -> `Compiler` > `Annotation Processors` and checking 'Enable annotation processing'.
3. Install the `Lombok Plugin`. This can be found in `Preferences` -> `Plugins`. Restart your IDE.
4. SBK should now compile properly.

For eclipse, you can generate eclipse project files by running `./gradlew eclipse`.


## Add your driver to SBK

### Add your driver to SBK using Gradle command and template driver
1. Run the command **./gradlew addDriver -Pdriver="your driver name"**
   1. This command create the new subproject under the SBK framework with <driver name>.java and <driver name>Reader.
      java and <driver name>Writer.java files filled with required classes definitions
   
2. You have to implement the following methods of Benchmark Interface:
   a). Add the Additional parameters (Command line Parameters) for your driver :[[addArgs](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#addArgs(io.sbk.api.ParameterOptions))]
   * The default command line parameters are listed in the help output here : [[Building SBK](https://github.com/kmgowda/sbk#building)]

   b). Parse your driver specific parameters: [[parseArgs](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#parseArgs(io.sbk.api.ParameterOptions))]

   c). Open the storage: [[openStorage](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#openStorage(io.sbk.api.ParameterOptions))]

   d). Close the storage:[[closeStorage](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#closeStorage(io.sbk.api.ParameterOptions))]

   e). Create a single writer instance:[[createWriter](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#createWriter(int,io.sbk.api.ParameterOptions))]
   * Create Writer will be called multiple times by SBK in case of Multi writers are specified in the command line.

   f). Create a single Reader instance:[[createReader](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#createReader(int,io.sbk.api.ParameterOptions))]
   * Create Reader will be called multiple times by SBK in case of Multi readers are specified in the command line.

   g). Get the Data Type :[[getDataType](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#getDataType())]
   * In case your data type is byte[] (Byte Array), No need to override this method. see the example:   [[Pulsar class](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/Pulsar.java)]
   * If your Benchmark,  Reader and Writer classes operates on different data type such as String or custom data type, then you have to override this default implementation.

3. You have to implement the following methods of Writer class
   a). Writer Data [Async or Sync]: [[writeAsync](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html#writeAsync(T))]

   b). Flush the data: [[sync](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html#sync())]

   c). Close the Writer: [[close](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html#close())]

   d). In case , if you want to have your own recordWrite implementation to write data and record the start and end time, then you can override: [[recordWrite](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html#recordWrite(io.sbk.data.DataType,T,int,io.time.Time,io.sbk.api.Status,io.perl.SendChannel,int))]

   1. You have to implement the following methods of Reader class

      i). Read Data
      1. for synchronous reads: [[read](hhttps://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Reader.html#read())]
         * Example: [[Pulsar Reader](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/PulsarReader.java)]
      2. for Asynchronous reads: [[AsyncRead](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/AsyncReader.html)]
         * create a new class
         * Example: [[File Async Reader](https://github.com/kmgowda/SBK/blob/master/driver-file/src/main/java/io/sbk/File/FileAsyncReader.java)]
      3. for call-back reads extend the abstract class: [[Abstract callback Reader](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/AbstractCallbackReader.html)]
         * Create a new class
         * Example: [[RabbitMQ Reader](https://github.com/kmgowda/SBK/blob/master/driver-rabbitmq/src/main/java/io/sbk/RabbitMQ/RabbitMQCallbackReader.java)]
      
      ii). Close the Reader:[[close](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Reader.html#close()) ] 

4. That's all ; Now, Build the SBK included your driver with the command:

```
./gradlew build
```

untar the SBK  to local folder

```
tar -xvf ./build/distributions/sbk.tar -C ./build/distributions/.
```

6. To invoke the benchmarking of the driver you have to issue the parameters "-class < your driver name>"
   Example: For pulsar driver

```
<SBK directory>./build/distributions/sbk/bin/sbk  -class pulsar -help

usage: sbk -class pulsar
Storage Benchmark Kit

 -ackQuorum <arg>       AckQuorum default: 1
 -admin <arg>           Admin URI, required to create the partitioned
                        topic, default: null
 -broker <arg>          Broker URI, default: tcp://localhost:6650
 -cluster <arg>         Cluster name (optional parameter)
 -context <arg>         Prometheus Metric context; 'no' disables this
                        option; default: 9718/metrics
 -csvfile <arg>         CSV file to record results; 'no' disables this
                        option, default: no
 -deduplication <arg>   Enable or Disable Deduplication; default: false
 -ensembleSize <arg>    EnsembleSize default: 1
 -help                  Help message
 -maxlatency <arg>      Maximum latency; use '-time' for time unit;
                        default: 180000 ms
 -minlatency <arg>      Minimum latency; use '-time' for time unit;
                        default: 0 ms
 -partitions <arg>      Number of partitions of the topic, default: 1
 -ram <arg>             SBK RAM host; 'no' disables this option, default:
                        no
 -ramport <arg>         SBK RAM Port; default: 9717
 -readers <arg>         Number of readers
 -records <arg>         Number of records(events) if 'seconds' not
                        specified;
                        otherwise, Maximum records per second by writer(s)
                        and/or Number of records per second by reader(s)
 -rsec <arg>            Number of seconds/step for readers, default: 0
 -rstep <arg>           Number of readers/step, default: 1
 -seconds <arg>         Number of seconds to run; if not specified, runs
                        forever
 -size <arg>            Size of each message (event or record)
 -sync <arg>            Each Writer calls flush/sync after writing <arg>
                        number of of events(records) ; <arg> number of
                        events(records) per Write or Read Transaction
 -threads <arg>         io threads per Topic, default: 1
 -throughput <arg>      If > 0, throughput in MB/s
                        If 0, writes/reads 'records'
                        If -1, get the maximum throughput (default: -1)
 -time <arg>            Latency Time Unit [ms:MILLISECONDS,
                        mcs:MICROSECONDS, ns:NANOSECONDS]; default: ms
 -topic <arg>           Topic name, default : test
 -writeQuorum <arg>     WriteQuorum default: 1
 -writers <arg>         Number of writers
 -wsec <arg>            Number of seconds/step for writers, default: 0
 -wstep <arg>           Number of writers/step, default: 1

Please report issues at https://github.com/kmgowda/SBK

```

### Add your driver to SBK Manually
1. Create the gradle subproject preferable with the name **driver-<your driver(storage device) name>**.

    * See the Example:[[Pulsar driver](https://github.com/kmgowda/sbk/tree/master/driver-pulsar)]   


2. Create the package **io.sbk.< your driver name>** 

    * See the Example: [[Pulsar driver package](https://github.com/kmgowda/sbk/tree/master/driver-pulsar/src/main/java/io/sbk/Pulsar)]   
    

3. In your driver package you have to implement the Interface: [[Storage](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html)]

    * See the Example:  [[Pulsar class](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/Pulsar.java)]
    
    * you have to implement the following methods of Benchmark Interface:
        
      a). Add the Additional parameters (Command line Parameters) for your driver :[[addArgs](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#addArgs(io.sbk.api.ParameterOptions))]
      * The default command line parameters are listed in the help output here : [[Building SBK](https://github.com/kmgowda/sbk#building)]
        
      b). Parse your driver specific parameters: [[parseArgs](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#parseArgs(io.sbk.api.ParameterOptions))]
        
      c). Open the storage: [[openStorage](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#openStorage(io.sbk.api.ParameterOptions))]
        
      d). Close the storage:[[closeStorage](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#closeStorage(io.sbk.api.ParameterOptions))]
        
      e). Create a single writer instance:[[createWriter](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#createWriter(int,io.sbk.api.ParameterOptions))]
        * Create Writer will be called multiple times by SBK in case of Multi writers are specified in the command line.   
        
      f). Create a single Reader instance:[[createReader](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#createReader(int,io.sbk.api.ParameterOptions))]
        * Create Reader will be called multiple times by SBK in case of Multi readers are specified in the command line. 
        
      g). Get the Data Type :[[getDataType](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Storage.html#getDataType())]
        * In case your data type is byte[] (Byte Array), No need to override this method. see the example:   [[Pulsar class](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/Pulsar.java)]
        * If your Benchmark,  Reader and Writer classes operates on different data type such as String or custom data type, then you have to override this default implementation.

    
4. Implement the Writer Interface: [[Writer](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html)]

    * See the Example: [[Pulsar Writer](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/PulsarWriter.java)]
    
    * you have to implement the following methods of Writer class:
        
      a). Writer Data [Async or Sync]: [[writeAsync](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html#writeAsync(T))]
        
      b). Flush the data: [[sync](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html#sync())]
        
      c). Close the Writer: [[close](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html#close())]
        
      d). In case , if you want to have your own recordWrite implementation to write data and record the start and end time, then you can override: [[recordWrite](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Writer.html#recordWrite(io.sbk.data.DataType,T,int,io.time.Time,io.sbk.api.Status,io.perl.SendChannel,int))]


5. Implement the Reader Interface: [[Reader](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Reader.html)]

    * you have to implement the following methods of Reader class:
        
      i). Read Data 
      1. for synchronous reads: [[read](hhttps://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Reader.html#read())]
         * Example: [[Pulsar Reader](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/PulsarReader.java)]
      2. for Asynchronous reads: [[AsyncRead](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/AsyncReader.html)]
         * Example: [[File Async Reader](https://github.com/kmgowda/SBK/blob/master/driver-file/src/main/java/io/sbk/File/FileAsyncReader.java)]
      3. for call-back reads extend the abstract class: [[Abstract callback Reader](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/AbstractCallbackReader.html)]
         * Example: [[RabbitMQ Reader](https://github.com/kmgowda/SBK/blob/master/driver-rabbitmq/src/main/java/io/sbk/RabbitMQ/RabbitMQCallbackReader.java)]
         
      ii). Close the Reader: [[close](https://kmgowda.github.io/SBK/sbk-api/javadoc/io/sbk/api/Reader.html#close()) ] 


6.  Add the Gradle dependency [ compile project(":sbk-api")] to your sub-project (driver)

    * see the Example:[[Pulsar Gradle Build](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/build.gradle)]


7. Add your subproject to the main gradle as dependency.

    * see the Example: [[SBK Gradle](build-drivers.gradle)]
    
    * make sure that gradle settings file: [[SBK Gradle Settings](settings-drivers.gradle)] has your Storage driver subproject name


8. That's all ; Now, Build the SBK included your driver with the command:

```
./gradlew build
```

untar the SBK  to local folder

```
tar -xvf ./build/distributions/sbk.tar -C ./build/distributions/.
```

9.  To invoke the benchmarking of the driver you have to issue the parameters "-class < your driver name>"


## Use SBK git hub packages
Instead of using entire SBK framework, if you just want to use the [SBK framework API](https://github.com/kmgowda?tab=packages&repo_name=SBK) packages to measure the performance benchmarking of your storage device/software, then follow the below simple and easy steps.

1. Add the SBK git hub package repository and dependency in gradle build file of your project as follows
    
   ```
    repositories {
        mavenCentral()

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kmgowda/SBK")

   /*
            credentials {
                username = project.findProperty("github.user") ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("github.token") ?: System.getenv("GITHUB_TOKEN")
            }
   */   
            credentials {
                username = "sbk-public"
                password = "\u0067hp_FBqmGRV6KLTcFjwnDTvozvlhs3VNja4F67B5"
            }   
   
       }
    }

    dependencies {
        implementation 'sbk:sbk-api:0.952'
    }

   ```
   few points to remember here
   
    *  you need to authenticate with your git hub username (GITHUB_USERNAME) and git hub token (GITHUB_TOKEN) 
    *  mavenCentral() repository is required to fetch the SBK's dependencies too.
    *  check this example: [File system benchmarking git hub build](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/gitpackage-build.gradle)
  
2. Extend the storage interface [Storage](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Storage.html) by following steps 1 to 5 described in [Add your storage driver](https://github.com/kmgowda/SBK#add-your-driver-to-sbk)

    *  check this example: [File system benchmarking](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/src/main/java/io.file/File.java)

3. Create a Main method to supply your storage class object to SBK to run/conduct the performance benchmarking

   ```
    public static void main(final String[] args) {
        Storage device = new <your storage class, extending the Storage interface>;
        try {
        
            //Start the File system benchmarking here
            
            Sbk.run(args  /* Command line Arguments, use '-class ' option specify the you storage class name  */ , 
                  packageNmae /* the name of the package where your storage device object exists */ , 
                  null /* Name of the your performance benchmarking application, by default , storage class name will be used */ ,
                  null /* Logger, if you don't have your own logger, then prometheus logger will be used by default */ );
            
            
        } catch (ParseException | IllegalArgumentException | IOException |
                InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

   ```
   *  check this example: [Start File system benchmarking](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/src/main/java/io.file/Main.java#L25)
   
4. That's all! Run your main method (your java application ) with "-help" to see the benchmarking options.    



## Use SBK from JitPack
The SBK API package is available in [JitPack Repository](https://jitpack.io/#kmgowda/SBK) too. To use the SBK-API package from Jitpack, follow the below simple and easy steps

1. Add the SBK git hub package repository and dependency in gradle build file of your project as follows

   ```
    repositories {
        mavenCentral()
        maven {
            url 'https://jitpack.io'
        }
    }

    dependencies {
        implementation 'com.github.kmgowda.SBK:sbk-api:0.952'
    }
   
   ```
   few points to remember here     
    *  mavenCentral() repository is required to fetch the SBK's dependencies too.
    *  check this example: [File system benchmarking jit pack build](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/jitpack-build.gradle)

2. Extend the storage interface [Storage](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Storage.html) by following steps 1 to 5 described in [Add your storage driver](https://github.com/kmgowda/SBK#add-your-driver-to-sbk)
    *  check this example: [File system benchmarking](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/src/main/java/io.file/File.java)

3. Create a Main method to supply your storage class object to SBK to run/conduct the performance benchmarking

   ```
    public static void main(final String[] args) {
        Storage device = new <your storage class, extending the Storage interface>;
        try {
        
            //Start the File system benchmarking here
            
            Sbk.run(args  /* Command line Arguments, use '-class ' option specify the you storage class name  */ , 
                  packageNmae /* the name of the package where your storage device object exists */ , 
                  null /* Name of the your performance benchmarking application, by default , storage class name will be used */ ,
                  null /* Logger, if you don't have your own logger, then prometheus logger will be used by default */ );
            
            
        } catch (ParseException | IllegalArgumentException | IOException |
                InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

   ```
   * check this example: [Start File system benchmarking](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/src/main/java/io.file/Main.java#L25)
   
4. That's all! Run your main method (your java application ) with "-help" to see the benchmarking options.   


## Use SBK from Maven Central
The SBK APIs Package is available at [maven central](https://search.maven.org/classic/#artifactdetails%7Cio.github.kmgowda%7Csbk-api%7C0.84%7Cjar) too. to use the sbk-api package, follow below steps

1. Add the SBK git hub package repository and dependency in gradle build file of your project as follows

   ```
    repositories {
        mavenCentral()
    }

    dependencies {
        implementation 'io.github.kmgowda:sbk-api:0.952'
    }
   ```
   few points to remember here  
    *  mavenCentral() repository is required to fetch the SBK APIs package and its dependencies.
    *  check this example: [File system benchmarking maven build](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/mavencentral-build.gradle)

2. Extend the storage interface [Storage](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Storage.html) by following steps 1 to 5 described in [Add your storage driver](https://github.com/kmgowda/SBK#add-your-driver-to-sbk)
    *  check this example: [File system benchmarking](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/src/main/java/io.file/File.java)

3. Create a Main method to supply your storage class object to SBK to run/conduct the performance benchmarking

   ```
     public static void main(final String[] args) {
        Storage device = new <your storage class, extending the Storage interface>;
        try {
        
            //Start the File system benchmarking here
            
            Sbk.run(args  /* Command line Arguments, use '-class ' option specify the you storage class name  */ , 
                  packageNmae /* the name of the package where your storage device object exists */ , 
                  null /* Name of the your performance benchmarking application, by default , storage class name will be used */ ,
                  null /* Logger, if you don't have your own logger, then prometheus logger will be used by default */ );
            
            
        } catch (ParseException | IllegalArgumentException | IOException |
                InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
      
   ```   
   *  check this example: [Start File system benchmarking](https://github.com/kmgowda/sbk-examples/blob/main/sbk-file/src/main/java/io.file/Main.java#L25)
   
4. That's all! Run your main method (your java application ) with "-help" to see the benchmarking options.    


## SBK Charts
you can log the performance results to CSV file using option '--csvfile'.
if you have SBK results in one or multiple CSV files,
then you can use python application 'sbk-charts' to compare the SBK benchmarking results plot the graphs into an Excel sheet.
refer [SBK Charts](sbk-charts.md) for further details.


## SBK Publications
1. The SBK is inspired from the Pravega benchmark tool, refer [Pravega benchmark tool](docs/kafka-pravega.pdf)
2. The SBK uses the multiple concurrent queues for fast performance benchmarking, refer [Design of SBK](docs/sbk.pdf) 


## SBK Discussions
___

<p align="center">
   Join SBK Slack Channel
</p>

<p align="center">
    <a href="https://sbkcrew.slack.com/?from=SBK">
        <img src="images/slack-logo.png" alt="SBK-Slack" width="50" height="50">
    </a>
</p>

___

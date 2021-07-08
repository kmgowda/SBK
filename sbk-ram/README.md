<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SBK-RAM : Results Aggregation Monitor

[![Api](https://img.shields.io/badge/SBK--RAM-API-brightgreen)](https://kmgowda.github.io/SBK/sbk-ram/javadoc/index.html)

The SBK (Storage Benchmark Kit) - RAM (Results Aggregation Monitor) combines the performance results supplied from
multiple SBK instances. The SBK-RAM is the GRPC Server. Multiple SBK instances can log the performance results to a
single SBM-RAM Server. The SBK-RAM determines the cumulative throughput and latency percentile values. Each SBK
instance reports all the latency values in bulk and SBK-RAM integrates all latency records and determines the
performance values for the whole SBK cluster (multi SBK instances). The SBK-RAM is useful if you want to stress the
storage server / Storage cluster with multiple storage clients (SBK instances) and analyse the throughput and
latency percentiles for the whole storage cluster/server. SBK-RAM logs the integrated results to standard output device/logger and to the grafana through prometheus metrics.

## Build SBK-RAM
SBK-RAM is a submodule/project of the SBK framework. If you [build SBK](https://github.com/kmgowda/SBK#build-sbk), it builds the SBK-RAM server too.

## Running SBK-RAM locally
The standard help output with SBK-RAM parameters as follows

```
kmg@kmgs-MBP SBK % ./sbk-ram/build/install/sbk-ram/bin/sbk-ram -help 
2021-07-08 16:58:10 INFO 
    _____   ____    _  __           _____               __  __
   / ____| |  _ \  | |/ /          |  __ \      /\     |  \/  |
  | (___   | |_) | | ' /   ______  | |__) |    /  \    | \  / |
   \___ \  |  _ <  |  <   |______| |  _  /    / /\ \   | |\/| |
   ____) | | |_) | | . \           | | \ \   / ____ \  | |  | |
  |_____/  |____/  |_|\_\          |_|  \_\ /_/    \_\ |_|  |_|

2021-07-08 16:58:10 INFO Storage Benchmark Kit - Results Aggregation Monitor
2021-07-08 16:58:10 INFO SBK-RAM Version: 0.892
2021-07-08 16:58:10 INFO Arguments List: [-help]
2021-07-08 16:58:10 INFO Java Runtime Version: 11.0.8+11

usage: sbk-ram
Storage Benchmark Kit - Results Aggregation Monitor

 -action <arg>    action [r: read, w: write, wr: write and read], default:
                  r
 -class <arg>     storage class name; run 'sbk -help' to see the list
 -context <arg>   Prometheus Metric context; default: 9719/metrics; 'no'
                  disables the metrics
 -help            Help message
 -max <arg>       Maximum number of connections; default: 1000
 -ramport <arg>   RAM port number; default: 9716
 -time <arg>      Latency Time Unit [ms:MILLISECONDS, mcs:MICROSECONDS,
                  ns:NANOSECONDS]; default: ms

Please report issues at https://github.com/kmgowda/SBK

```
An Example output of SBK-RAM with 2 SBK file system benchmakring instances are as follows:

```
kmg@kmgs-MBP SBK % ./sbk-ram/build/install/sbk-ram/bin/sbk-ram -class file -time ns
2021-07-08 16:59:39 INFO 
    _____   ____    _  __           _____               __  __
   / ____| |  _ \  | |/ /          |  __ \      /\     |  \/  |
  | (___   | |_) | | ' /   ______  | |__) |    /  \    | \  / |
   \___ \  |  _ <  |  <   |______| |  _  /    / /\ \   | |\/| |
   ____) | | |_) | | . \           | | \ \   / ____ \  | |  | |
  |_____/  |____/  |_|\_\          |_|  \_\ /_/    \_\ |_|  |_|

2021-07-08 16:59:39 INFO Storage Benchmark Kit - Results Aggregation Monitor
2021-07-08 16:59:39 INFO SBK-RAM Version: 0.892
2021-07-08 16:59:39 INFO Arguments List: [-class, file, -time, ns]
2021-07-08 16:59:39 INFO Java Runtime Version: 11.0.8+11
2021-07-08 16:59:39 INFO Time Unit: NANOSECONDS
2021-07-08 16:59:39 INFO Minimum Latency: 0 ns
2021-07-08 16:59:39 INFO Maximum Latency: 180000000000 ns
2021-07-08 16:59:39 INFO Window Latency Store: HashMap
2021-07-08 16:59:39 INFO SBK RAM Benchmark Started
2021-07-08 16:59:40 INFO SBK PrometheusLogger Started
2021-07-08 16:59:40 INFO SBK Connections PrometheusLogger Started
2021-07-08 16:59:40 INFO LatenciesRecord Benchmark Started
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     0 Readers,      0 Max Writers,     0 Max Readers,           0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ns avg latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ns 10th,       0 ns 25th,       0 ns 50th,       0 ns 75th,       0 ns 90th,       0 ns 95th,       0 ns 99th,       0 ns 99.9th,       0 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     4715504 records,  941288.7 records/sec,     8.98 MB/sec,    963.7 ns avg latency, 5133960 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    700 ns 10th,     716 ns 25th,     778 ns 50th,    1028 ns 75th,    1226 ns 90th,    1397 ns 95th,    2440 ns 99th,   21452 ns 99.9th,   36111 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     4442402 records,  887268.3 records/sec,     8.46 MB/sec,   1036.0 ns avg latency, 4822299 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    673 ns 10th,     711 ns 25th,     857 ns 50th,    1099 ns 75th,    1376 ns 90th,    1749 ns 95th,    3007 ns 99th,   18645 ns 99.9th,   39530 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5358312 records, 1069983.7 records/sec,    10.20 MB/sec,    854.7 ns avg latency, 2212725 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    667 ns 10th,     684 ns 25th,     707 ns 50th,     751 ns 75th,    1094 ns 90th,    1291 ns 95th,    2319 ns 99th,   20397 ns 99.9th,   35049 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6142255 records, 1228027.3 records/sec,    11.71 MB/sec,    747.0 ns avg latency, 1623235 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    664 ns 10th,     676 ns 25th,     691 ns 50th,     706 ns 75th,     723 ns 90th,     739 ns 95th,     865 ns 99th,   20743 ns 99.9th,   33301 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6166199 records, 1230875.0 records/sec,    11.74 MB/sec,    744.8 ns avg latency, 1837968 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    663 ns 10th,     673 ns 25th,     687 ns 50th,     703 ns 75th,     719 ns 90th,     730 ns 95th,     834 ns 99th,   20822 ns 99.9th,   33603 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6336097 records, 1266466.6 records/sec,    12.08 MB/sec,    724.5 ns avg latency, 1612212 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    658 ns 10th,     667 ns 25th,     678 ns 50th,     688 ns 75th,     698 ns 90th,     705 ns 95th,     798 ns 99th,   20233 ns 99.9th,   31158 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6307880 records, 1260059.1 records/sec,    12.02 MB/sec,    727.6 ns avg latency, 1442498 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    662 ns 10th,     669 ns 25th,     681 ns 50th,     692 ns 75th,     704 ns 90th,     711 ns 95th,     819 ns 99th,   20234 ns 99.9th,   32774 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6333069 records, 1264213.2 records/sec,    12.06 MB/sec,    725.2 ns avg latency, 1357557 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    662 ns 10th,     669 ns 25th,     680 ns 50th,     691 ns 75th,     702 ns 90th,     711 ns 95th,     845 ns 99th,   20150 ns 99.9th,   30953 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6167163 records, 1232586.0 records/sec,    11.75 MB/sec,    744.3 ns avg latency, 1617445 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    668 ns 10th,     678 ns 25th,     690 ns 50th,     704 ns 75th,     724 ns 90th,     749 ns 95th,     854 ns 99th,   20595 ns 99.9th,   32837 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6343907 records, 1267050.4 records/sec,    12.08 MB/sec,    723.7 ns avg latency, 1442508 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    660 ns 10th,     668 ns 25th,     680 ns 50th,     691 ns 75th,     703 ns 90th,     712 ns 95th,     797 ns 99th,   20100 ns 99.9th,   30692 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6339272 records, 1266775.7 records/sec,    12.08 MB/sec,    723.9 ns avg latency, 1347972 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    666 ns 10th,     673 ns 25th,     684 ns 50th,     696 ns 75th,     707 ns 90th,     715 ns 95th,     812 ns 99th,   18203 ns 99.9th,   31180 ns 99.99th.
Sbk-Ram     0 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6303786 records, 1259793.9 records/sec,    12.01 MB/sec,    726.3 ns avg latency, 1313093 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    663 ns 10th,     672 ns 25th,     685 ns 50th,     698 ns 75th,     709 ns 90th,     718 ns 95th,     800 ns 99th,   19582 ns 99.9th,   31511 ns 99.99th.
Sbk-Ram     0 Connections,     1 Max Connections: file Reading     0 Writers,     0 Readers,      0 Max Writers,     0 Max Readers,           0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ns avg latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ns 10th,       0 ns 25th,       0 ns 50th,       0 ns 75th,       0 ns 90th,       0 ns 95th,       0 ns 99th,       0 ns 99.9th,       0 ns 99.99th.
^C
2021-07-08 17:00:51 INFO LatenciesRecord Benchmark Shutdown
2021-07-08 17:00:51 INFO SBK PrometheusLogger Shutdown
2021-07-08 17:00:51 INFO SBK RAM Benchmark Shutdown

```

Note that the SBK-RAM indicates the number of active connections and maximum connections in a session.
while running an SBK instance make sure that you supply the RAM Host address (IP address). Optionally you supply the
port number too , the default port number is **9716**.

A sample SBK instance execution output is as follows:

```
kmg@kmgs-MBP SBK % ./driver-file/build/install/sbk-file/bin/sbk-file -readers 1 -size 10 -seconds 60 -time ns -context no -ram localhost 
2021-07-08 16:59:42 INFO Reflections took 63 ms to scan 2 urls, producing 73 keys and 98 values 
2021-07-08 16:59:42 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-07-08 16:59:42 INFO Storage Benchmark Kit
2021-07-08 16:59:42 INFO SBK Version: 0.892
2021-07-08 16:59:42 INFO Arguments List: [-readers, 1, -size, 10, -seconds, 60, -time, ns, -context, no, -ram, localhost]
2021-07-08 16:59:42 INFO Java Runtime Version: 11.0.8+11
2021-07-08 16:59:42 INFO Storage Drivers Package: io.sbk
2021-07-08 16:59:42 INFO sbk.applicationName: sbk-file
2021-07-08 16:59:42 INFO sbk.appHome: /Users/kmg/projects/SBK/driver-file/build/install/sbk-file
2021-07-08 16:59:42 INFO sbk.className: file
2021-07-08 16:59:42 INFO '-class': 
2021-07-08 16:59:42 INFO Available Storage Drivers in package 'io.sbk': 1 [File]
2021-07-08 16:59:43 INFO Arguments to Driver 'File' : [-readers, 1, -size, 10, -seconds, 60, -time, ns, -context, no, -ram, localhost]
2021-07-08 16:59:43 INFO Time Unit: NANOSECONDS
2021-07-08 16:59:43 INFO Minimum Latency: 0 ns
2021-07-08 16:59:43 INFO Maximum Latency: 180000000000 ns
2021-07-08 16:59:43 INFO Window Latency Store: HashMap
2021-07-08 16:59:43 INFO Total Window Latency Store: HashMap
2021-07-08 16:59:43 INFO SBK Benchmark Started
2021-07-08 16:59:43 INFO SBK PrometheusLogger Started
2021-07-08 16:59:43 INFO SBK GRPC Logger Started
2021-07-08 16:59:43 INFO Synchronous File Reader initiated !
2021-07-08 16:59:43 INFO Performance Logger Started
2021-07-08 16:59:43 INFO SBK Benchmark initiated Readers
2021-07-08 16:59:43 INFO Reader 0 started , run seconds: 60
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     4715504 records,  942912.1 records/sec,     8.99 MB/sec,    963.7 ns avg latency, 5133960 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    700 ns 10th,     716 ns 25th,     778 ns 50th,    1028 ns 75th,    1226 ns 90th,    1397 ns 95th,    2440 ns 99th,   21452 ns 99.9th,   36111 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     4442402 records,  888299.7 records/sec,     8.47 MB/sec,   1036.0 ns avg latency, 4822299 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    673 ns 10th,     711 ns 25th,     857 ns 50th,    1099 ns 75th,    1376 ns 90th,    1749 ns 95th,    3007 ns 99th,   18645 ns 99.9th,   39530 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5358312 records, 1071447.8 records/sec,    10.22 MB/sec,    854.7 ns avg latency, 2212725 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    667 ns 10th,     684 ns 25th,     707 ns 50th,     751 ns 75th,    1094 ns 90th,    1291 ns 95th,    2319 ns 99th,   20397 ns 99.9th,   35049 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6142255 records, 1228204.1 records/sec,    11.71 MB/sec,    747.0 ns avg latency, 1623235 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    664 ns 10th,     676 ns 25th,     691 ns 50th,     706 ns 75th,     723 ns 90th,     739 ns 95th,     865 ns 99th,   20743 ns 99.9th,   33301 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6166199 records, 1232992.0 records/sec,    11.76 MB/sec,    744.8 ns avg latency, 1837968 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    663 ns 10th,     673 ns 25th,     687 ns 50th,     703 ns 75th,     719 ns 90th,     730 ns 95th,     834 ns 99th,   20822 ns 99.9th,   33603 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6336097 records, 1266965.9 records/sec,    12.08 MB/sec,    724.5 ns avg latency, 1612212 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    658 ns 10th,     667 ns 25th,     678 ns 50th,     688 ns 75th,     698 ns 90th,     705 ns 95th,     798 ns 99th,   20233 ns 99.9th,   31158 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6307880 records, 1261323.7 records/sec,    12.03 MB/sec,    727.6 ns avg latency, 1442498 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    662 ns 10th,     669 ns 25th,     681 ns 50th,     692 ns 75th,     704 ns 90th,     711 ns 95th,     819 ns 99th,   20234 ns 99.9th,   32774 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6333069 records, 1266360.4 records/sec,    12.08 MB/sec,    725.2 ns avg latency, 1357557 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    662 ns 10th,     669 ns 25th,     680 ns 50th,     691 ns 75th,     702 ns 90th,     711 ns 95th,     845 ns 99th,   20150 ns 99.9th,   30953 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6167163 records, 1233185.5 records/sec,    11.76 MB/sec,    744.3 ns avg latency, 1617445 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    668 ns 10th,     678 ns 25th,     690 ns 50th,     704 ns 75th,     724 ns 90th,     749 ns 95th,     854 ns 99th,   20595 ns 99.9th,   32837 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6343907 records, 1268527.7 records/sec,    12.10 MB/sec,    723.7 ns avg latency, 1442508 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    660 ns 10th,     668 ns 25th,     680 ns 50th,     691 ns 75th,     703 ns 90th,     712 ns 95th,     797 ns 99th,   20100 ns 99.9th,   30692 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6339272 records, 1267600.8 records/sec,    12.09 MB/sec,    723.9 ns avg latency, 1347972 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    666 ns 10th,     673 ns 25th,     684 ns 50th,     696 ns 75th,     707 ns 90th,     715 ns 95th,     812 ns 99th,   18203 ns 99.9th,   31180 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6303786 records, 1263544.5 records/sec,    12.05 MB/sec,    726.3 ns avg latency, 1313093 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    663 ns 10th,     672 ns 25th,     685 ns 50th,     698 ns 75th,     709 ns 90th,     718 ns 95th,     800 ns 99th,   19582 ns 99.9th,   31511 ns 99.99th.
2021-07-08 17:00:43 INFO Reader 0 exited
Total : File Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,    70955846 records, 1182597.4 records/sec,    11.28 MB/sec,    775.5 ns avg latency, 5133960 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    664 ns 10th,     673 ns 25th,     688 ns 50th,     706 ns 75th,     786 ns 90th,    1038 ns 95th,    1610 ns 99th,   20276 ns 99.9th,   32937 ns 99.99th.
2021-07-08 17:00:43 INFO Performance Logger Shutdown
2021-07-08 17:00:43 INFO SBK PrometheusLogger Shutdown
2021-07-08 17:00:43 INFO SBK GRPC Logger Shutdown
2021-07-08 17:00:44 INFO SBK Benchmark Shutdown


```

Note that option **-ram** is used to supply the SBK-RAM host ; In the above example, its localhost and default port
is 9716.

## SBK-RAM Docker Containers
The SBK-RAM Docker images are available at [SBK Docker](https://hub.docker.com/repository/docker/kmgowda/sbk-ram)

The SBK docker image pull command is
```
docker pull kmgowda/sbk-ram
```

you can straightaway run the docker image too, For example
```
docker run -p 127.0.0.1:9716:9716/tcp -p 127.0.0.1:9719:9719/tcp kmgowda/sbk-ram:latest -class file -time ns
```
* Note that the option **-p 127.0.0.1:9719:9719/tcp** redirects the 9719 port to local port to send the performance
  metric data for Prometheus.
* Another option **-p 127.0.0.1:9716:9716/tcp** redirect the port 9716 port to local port to receive the performance
  results from SBK instances.
* Avoid using the **--network host** option , because this option overrides the port redirection.


## Running SBK-RAM Docker Compose
The SBK-RAM docker compose consists of SBK-RAM docker image, Grafana and prometheus docker images.
The [grafana image](https://github.com/kmgowda/SBK/blob/master/grafana/Dockerfile) contains the [dashboards](https://github.com/kmgowda/SBK/tree/master/grafana/dashboards) which can be directly deployed for the performance analytics.

As an example, just follow the below steps to see the performance graphs

1. In the sbk-ram directory build the 'sbk-ram' service of the [docker compose](https://github.com/kmgowda/SBK/blob/master/sbk-ram/docker-compose.yml) file as follows.

 ```
 <SBK dir>% docker-compose build

 ```

1. Run the 'sbk-ram' service as follows.

 ```
 <SBK dir>% docker-compose run -p 127.0.0.1:9716:9716/tcp sbk-ram -class file -time ns

 ```
Note that , 9716 is the exposed port from sbk-ram container to receive the benchmark results from remote SBK
instances via localhost.
The option **-class** is the same as in SBK command/application. you should use the same storage class and time unit in
SBK instances too.

1. login to [grafana localhost port 3000](http://localhost:3000) with username **admin** and password **sbk**
1. go to dashboard menu and pick the dashboard of the storage device on which you are running the performance benchmarking.
   in the above example, you can choose the [File system dashboard](https://github.com/kmgowda/SBK/blob/master/grafana/dashboards/sbk-file.json).
1. The SBK-RAM docker compose runs the SBK-RAM image as a docker container.
   In case, if you are running SBK as an application, and you want to see the SBK performance graphs using Grafana,
   then use [Grafana Docker compose](https://github.com/kmgowda/SBK/tree/master/grafana)

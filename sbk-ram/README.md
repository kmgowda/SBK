<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SBK-RAM : Results Aggregation Monitor

[![Api](https://img.shields.io/badge/SBK--RAM-API-brightgreen)](https://github.com/kmgowda/SBK/blob/kmg-ram-3/sbk-ram/javadoc/index.html)

The SBK (Storage Benchmark Kit) - RAM (Results Aggregation Monitor) combines the performance results supplied from 
multiple SBK instances. The SBK-RAM is the GRPC Server. Multiple SBK instances can log the performance results to a 
single SBM-RAM Server. The SBK-RAM determines the cumulative throughput and latency percentile values. Each SBK 
instance reports all the latency values in bulk and SBK-RAM integrates all latency records and determines the 
performance values for the whole SBK cluster (multi SBK instances). The SBK-RAM is useful if you want stress the 
storage server / Storage cluster with multiple storage clients (SBK instances) and analyse the throughput and 
latency percentiles for the whole storage cluster/server. SBK-RAM logs the integrated results to standard output device/logger and to the grafana through prometheus metrics.

## Build SBK-RAM
SBK-RAM is submodule/project of the SBK framework. If you [build SBK](https://github.com/kmgowda/SBK#build-sbk) , it builds the SBK-RAM server too. 

## Running SBK-RAM locally
The standard help output with SBK-RAM parameters as follows

```
kmg@kmgs-MBP SBK % ./sbk-ram/build/install/sbk-ram/bin/sbk-ram  -help      
.
usage: sbk-ram
 -action <arg>    action [r: read, w: write, wr: write and read]; default:
                  r
 -class <arg>     storage class name; run 'sbk -help' to see the list
 -context <arg>   Prometheus Metric context; default context:
                  9719/metrics; 'no' disables the metrics
 -help            Help message
 -max <arg>       Maximum number of connections; default: 1000
 -time <arg>      Latency Time Unit [ms:MILLISECONDS, mcs:MICROSECONDS,
                  ns:NANOSECONDS]; default: ms
```
An Example output of SBK-RAM with 2 SBK file system benchmakring instances are as follows:

```
kmg@kmgs-MBP SBK % ./sbk-ram/build/install/sbk-ram/bin/sbk-ram -class file -time ns
2021-06-14 21:15:53 INFO 
    _____   ____    _  __           _____               __  __
   / ____| |  _ \  | |/ /          |  __ \      /\     |  \/  |
  | (___   | |_) | | ' /   ______  | |__) |    /  \    | \  / |
   \___ \  |  _ <  |  <   |______| |  _  /    / /\ \   | |\/| |
   ____) | | |_) | | . \           | | \ \   / ____ \  | |  | |
  |_____/  |____/  |_|\_\          |_|  \_\ /_/    \_\ |_|  |_|

2021-06-14 21:15:53 INFO Java Runtime Version: 11.0.8+11
2021-06-14 21:15:53 INFO Arguments List: [-class, file, -time, ns]
2021-06-14 21:15:53 INFO sbk-ram Version: 0.9
2021-06-14 21:15:53 INFO sbk.appHome: /Users/kmg/projects/SBK/sbk-ram/build/install/sbk-ram
2021-06-14 21:15:53 INFO Time Unit: NANOSECONDS
2021-06-14 21:15:53 INFO Minimum Latency: 0 ns
2021-06-14 21:15:53 INFO Maximum Latency: 180000000000 ns
2021-06-14 21:15:53 INFO Window Latency Store: HashMap
2021-06-14 21:15:54 INFO SBK PrometheusLogger Started
2021-06-14 21:15:54 INFO SBK Connections PrometheusLogger Started
2021-06-14 21:15:54 INFO LatenciesRecord Benchmark Started
2021-06-14 21:15:54 INFO SBK Server Benchmark Started
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     0 Readers,      0 Max Writers,     0 Max Readers,           0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ns avg latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ns 10th,       0 ns 25th,       0 ns 50th,       0 ns 75th,       0 ns 90th,       0 ns 95th,       0 ns 99th,       0 ns 99.9th,       0 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6016696 records, 1201290.3 records/sec,    11.46 MB/sec,    753.6 ns avg latency, 4701560 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    638 ns 10th,     650 ns 25th,     676 ns 50th,     698 ns 75th,     782 ns 90th,     872 ns 95th,    1084 ns 99th,   21167 ns 99.9th,   34026 ns 99.99th.
Sbk-Ram     2 Connections,     2 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5524296 records, 1103342.1 records/sec,    10.52 MB/sec,    832.9 ns avg latency, 2672790 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    664 ns 10th,     715 ns 25th,     755 ns 50th,     786 ns 75th,     820 ns 90th,     845 ns 95th,     996 ns 99th,   23673 ns 99.9th,   35169 ns 99.99th.
Sbk-Ram     2 Connections,     2 Max Connections: file Reading     0 Writers,     2 Readers,      0 Max Writers,     2 Max Readers,    10903470 records, 2178088.5 records/sec,    20.77 MB/sec,    844.2 ns avg latency, 4320064 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    736 ns 10th,     750 ns 25th,     766 ns 50th,     786 ns 75th,     823 ns 90th,     872 ns 95th,    1079 ns 99th,   22480 ns 99.9th,   33797 ns 99.99th.
Sbk-Ram     2 Connections,     2 Max Connections: file Reading     0 Writers,     2 Readers,      0 Max Writers,     2 Max Readers,    11240535 records, 2244934.0 records/sec,    21.41 MB/sec,    821.7 ns avg latency, 2893732 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    733 ns 10th,     744 ns 25th,     757 ns 50th,     771 ns 75th,     792 ns 90th,     822 ns 95th,     951 ns 99th,   21167 ns 99.9th,   31821 ns 99.99th.
Sbk-Ram     2 Connections,     2 Max Connections: file Reading     0 Writers,     2 Readers,      0 Max Writers,     2 Max Readers,    11247583 records, 2246260.9 records/sec,    21.42 MB/sec,    820.3 ns avg latency, 1892051 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    731 ns 10th,     740 ns 25th,     754 ns 50th,     769 ns 75th,     785 ns 90th,     809 ns 95th,     953 ns 99th,   21658 ns 99.9th,   31651 ns 99.99th.
Sbk-Ram     2 Connections,     2 Max Connections: file Reading     0 Writers,     2 Readers,      0 Max Writers,     2 Max Readers,    11317708 records, 2260819.5 records/sec,    21.56 MB/sec,    815.2 ns avg latency, 1633119 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    730 ns 10th,     739 ns 25th,     751 ns 50th,     764 ns 75th,     779 ns 90th,     793 ns 95th,     917 ns 99th,   21453 ns 99.9th,   31005 ns 99.99th.
Sbk-Ram     2 Connections,     2 Max Connections: file Reading     0 Writers,     2 Readers,      0 Max Writers,     2 Max Readers,    11303629 records, 2258362.9 records/sec,    21.54 MB/sec,    815.4 ns avg latency, 2070967 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    731 ns 10th,     742 ns 25th,     755 ns 50th,     767 ns 75th,     782 ns 90th,     800 ns 95th,     929 ns 99th,   21270 ns 99.9th,   31585 ns 99.99th.
Sbk-Ram     1 Connections,     2 Max Connections: file Reading     0 Writers,     2 Readers,      0 Max Writers,     2 Max Readers,    13162538 records, 2627762.8 records/sec,    25.06 MB/sec,    790.8 ns avg latency, 2003716 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    659 ns 10th,     732 ns 25th,     750 ns 50th,     765 ns 75th,     781 ns 90th,     796 ns 95th,     914 ns 99th,   20653 ns 99.9th,   31295 ns 99.99th.
Sbk-Ram     1 Connections,     2 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6607381 records, 1319378.2 records/sec,    12.58 MB/sec,    692.3 ns avg latency, 1121182 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    636 ns 10th,     639 ns 25th,     650 ns 50th,     667 ns 75th,     679 ns 90th,     687 ns 95th,     801 ns 99th,   18788 ns 99.9th,   29976 ns 99.99th.
Sbk-Ram     1 Connections,     2 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6308646 records, 1259935.2 records/sec,    12.02 MB/sec,    725.4 ns avg latency, 1390166 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    651 ns 10th,     663 ns 25th,     669 ns 50th,     680 ns 75th,     692 ns 90th,     716 ns 95th,     834 ns 99th,   21386 ns 99.9th,   33710 ns 99.99th.
Sbk-Ram     1 Connections,     2 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6500241 records, 1298350.6 records/sec,    12.38 MB/sec,    704.2 ns avg latency, 1355878 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    637 ns 10th,     644 ns 25th,     659 ns 50th,     671 ns 75th,     682 ns 90th,     693 ns 95th,     816 ns 99th,   20195 ns 99.9th,   31217 ns 99.99th.
Sbk-Ram     0 Connections,     2 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6497385 records, 1297751.5 records/sec,    12.38 MB/sec,    702.3 ns avg latency, 1523618 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    636 ns 10th,     641 ns 25th,     652 ns 50th,     668 ns 75th,     683 ns 90th,     695 ns 95th,     820 ns 99th,   20177 ns 99.9th,   30607 ns 99.99th.
Sbk-Ram     0 Connections,     2 Max Connections: file Reading     0 Writers,     0 Readers,      0 Max Writers,     0 Max Readers,           0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ns avg latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ns 10th,       0 ns 25th,       0 ns 50th,       0 ns 75th,       0 ns 90th,       0 ns 95th,       0 ns 99th,       0 ns 99.9th,       0 ns 99.99th.
Sbk-Ram     0 Connections,     2 Max Connections: file Reading     0 Writers,     0 Readers,      0 Max Writers,     0 Max Readers,           0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ns avg latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      0 ns 10th,       0 ns 25th,       0 ns 50th,       0 ns 75th,       0 ns 90th,       0 ns 95th,       0 ns 99th,       0 ns 99.9th,       0 ns 99.99th.
2021-06-14 21:17:10 INFO LatenciesRecord Benchmark Shutdown
2021-06-14 21:17:10 INFO SBK PrometheusLogger Shutdown
2021-06-14 21:17:10 INFO SBK Benchmark Shutdown

```

Note that the SBK-RAM indicates the number of active connections and maximum connections in a session.
while running SBK instance make sure that you supply the RAM Host address (IP address). Optionally you supply the 
port number too , the default port number is **9716**.

A sample SBK instance execution output is as follows:

```
kmg@kmgs-MBP SBK % ./driver-file/build/install/sbk-file/bin/sbk-file -readers 1 -size 10 -seconds 60 -time ns -context no  -ram localhost  
2021-06-14 21:15:58 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-06-14 21:15:58 INFO Java Runtime Version: 11.0.8+11
2021-06-14 21:15:58 INFO SBK Version: 0.9
2021-06-14 21:15:58 INFO Arguments List: [-readers, 1, -size, 10, -seconds, 60, -time, ns, -context, no, -ram, localhost]
2021-06-14 21:15:58 INFO sbk.applicationName: sbk-file
2021-06-14 21:15:58 INFO sbk.className: file
2021-06-14 21:15:58 INFO sbk.appHome: /Users/kmg/projects/SBK/driver-file/build/install/sbk-file
2021-06-14 21:15:58 INFO Reflections took 56 ms to scan 2 urls, producing 70 keys and 94 values 
2021-06-14 21:15:58 INFO Available Drivers : 1
2021-06-14 21:15:58 INFO Arguments to Driver 'File' : [-readers, 1, -size, 10, -seconds, 60, -time, ns, -context, no, -ram, localhost]
2021-06-14 21:15:58 INFO Time Unit: NANOSECONDS
2021-06-14 21:15:58 INFO Minimum Latency: 0 ns
2021-06-14 21:15:58 INFO Maximum Latency: 180000000000 ns
2021-06-14 21:15:58 INFO Window Latency Store: HashMap
2021-06-14 21:15:58 INFO Total Window Latency Store: HashMap
2021-06-14 21:15:58 INFO SBK Benchmark Started
2021-06-14 21:15:58 INFO SBK PrometheusLogger Started
2021-06-14 21:15:59 INFO SBK GRPC Logger Started
2021-06-14 21:15:59 INFO Synchronous File Reader initiated !
2021-06-14 21:15:59 INFO Performance Logger Started
2021-06-14 21:15:59 INFO SBK Benchmark initiated Readers
2021-06-14 21:15:59 INFO Reader 0 started , run seconds: 60
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6016696 records, 1203098.5 records/sec,    11.47 MB/sec,    753.6 ns avg latency, 4701560 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    638 ns 10th,     650 ns 25th,     676 ns 50th,     698 ns 75th,     782 ns 90th,     872 ns 95th,    1084 ns 99th,   21167 ns 99.9th,   34026 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5524296 records, 1104638.2 records/sec,    10.53 MB/sec,    832.9 ns avg latency, 2672790 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    664 ns 10th,     715 ns 25th,     755 ns 50th,     786 ns 75th,     820 ns 90th,     845 ns 95th,     996 ns 99th,   23673 ns 99.9th,   35169 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5582782 records, 1116332.8 records/sec,    10.65 MB/sec,    828.7 ns avg latency, 1962616 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    733 ns 10th,     745 ns 25th,     761 ns 50th,     776 ns 75th,     799 ns 90th,     826 ns 95th,     977 ns 99th,   21991 ns 99.9th,   32625 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5624397 records, 1124654.3 records/sec,    10.73 MB/sec,    821.9 ns avg latency, 1527747 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    734 ns 10th,     746 ns 25th,     759 ns 50th,     773 ns 75th,     791 ns 90th,     818 ns 95th,     949 ns 99th,   21329 ns 99.9th,   32008 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5644957 records, 1128765.5 records/sec,    10.76 MB/sec,    819.2 ns avg latency, 1550152 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    732 ns 10th,     740 ns 25th,     754 ns 50th,     769 ns 75th,     783 ns 90th,     798 ns 95th,     945 ns 99th,   21713 ns 99.9th,   31576 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5653345 records, 1130441.1 records/sec,    10.78 MB/sec,    818.3 ns avg latency, 1462118 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    730 ns 10th,     738 ns 25th,     751 ns 50th,     765 ns 75th,     782 ns 90th,     795 ns 95th,     921 ns 99th,   21658 ns 99.9th,   30874 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     5661921 records, 1132157.7 records/sec,    10.80 MB/sec,    816.2 ns avg latency, 2070967 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    738 ns 10th,     749 ns 25th,     758 ns 50th,     770 ns 75th,     784 ns 90th,     802 ns 95th,     930 ns 99th,   21163 ns 99.9th,   32136 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6018051 records, 1203368.0 records/sec,    11.48 MB/sec,    765.6 ns avg latency, 1548310 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    641 ns 10th,     664 ns 25th,     740 ns 50th,     762 ns 75th,     778 ns 90th,     794 ns 95th,     909 ns 99th,   20352 ns 99.9th,   30705 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6607381 records, 1321211.8 records/sec,    12.60 MB/sec,    692.3 ns avg latency, 1121182 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    636 ns 10th,     639 ns 25th,     650 ns 50th,     667 ns 75th,     679 ns 90th,     687 ns 95th,     801 ns 99th,   18788 ns 99.9th,   29976 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6308646 records, 1261476.8 records/sec,    12.03 MB/sec,    725.4 ns avg latency, 1390166 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    651 ns 10th,     663 ns 25th,     669 ns 50th,     680 ns 75th,     692 ns 90th,     716 ns 95th,     834 ns 99th,   21386 ns 99.9th,   33710 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6500241 records, 1299788.2 records/sec,    12.40 MB/sec,    704.2 ns avg latency, 1355878 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    637 ns 10th,     644 ns 25th,     659 ns 50th,     671 ns 75th,     682 ns 90th,     693 ns 95th,     816 ns 99th,   20195 ns 99.9th,   31217 ns 99.99th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,     6497385 records, 1302346.9 records/sec,    12.42 MB/sec,    702.3 ns avg latency, 1523618 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    636 ns 10th,     641 ns 25th,     652 ns 50th,     668 ns 75th,     683 ns 90th,     695 ns 95th,     820 ns 99th,   20177 ns 99.9th,   30607 ns 99.99th.
2021-06-14 21:16:59 INFO Reader 0 exited
Total : File Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,    71640098 records, 1194001.6 records/sec,    11.39 MB/sec,    769.9 ns avg latency, 4701560 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    642 ns 10th,     663 ns 25th,     727 ns 50th,     759 ns 75th,     781 ns 90th,     806 ns 95th,     950 ns 99th,   20847 ns 99.9th,   32292 ns 99.99th.
2021-06-14 21:16:59 INFO Performance Logger Shutdown
2021-06-14 21:16:59 INFO SBK PrometheusLogger Shutdown
2021-06-14 21:16:59 INFO SBK GRPC Logger Shutdown
2021-06-14 21:17:00 INFO SBK Benchmark Shutdown

```

Note that option  **-ram** is used to supply the SBK-RAM host ; In the above example, its localhost and default port 
is 9716.

## SBK-RAM Docker Containers
The SBK-RAM Docker images are available at [SBK Docker](https://hub.docker.com/repository/docker/kmgowda/sbk-ram)

The SBK docker image pull command is
```
docker pull kmgowda/sbk-ram
```

you can straightaway run the docker image too, For example
```
docker run  -p 127.0.0.1:9716:9716/tcp -p 127.0.0.1:9719:9719/tcp  kmgowda/sbk-ram:latest -class file -time ns
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
   
1.  Run the 'sbk-ram' service as follows.

   ```
   <SBK dir>% docker-compose run  -p 127.0.0.1:9716:9716/tcp  sbk-ram  -class file -time ns

   ```
   Note that , 9716 is the exposed port from sbk-ram container to received the benchmark results from remote SBK 
   instances via localhost.
   The option **-class** is same as in SBK command/application. you should use same storage class and time unit in 
   SBK instances too. 

1. login to [grafana local host port 3000](http://localhost:3000) with username **admin** and password **sbk**
1. go to dashboard menu and pick the dashboard of the storage device on which you are running the performance benchmarking.
   in the above example, you can choose the [File system dashboard](https://github.com/kmgowda/SBK/blob/master/grafana/dashboards/sbk-file.json).
1. The SBK-RAM docker compose runs the SBK-RAM image as docker container.
   In case, if you are running SBK as an application, and you want to see the SBK performance graphs using Grafana,
   then use [Grafana Docker compose](https://github.com/kmgowda/SBK/tree/master/grafana)
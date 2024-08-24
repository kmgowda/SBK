<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SBK-GEM : Group Execution Monitor

[![Api](https://img.shields.io/badge/SBK--GEM-API-brightgreen)](https://kmgowda.github.io/SBK/sbk-gem/javadoc/index.html)

The SBK (Storage Benchmark Kit) - GEM (Group Execution Monitor) combines SBK-RAM and SBK. you can just execute the 
SBK instances on multiple hosts with single SBK-GEM command. For any given cluster, you are running ssh-server in 
each node of the cluster , then SBK-GEM can be used run the SBK in all the nodes of the cluster. The SBK-GEM copies 
the SBK application to all nodes of the cluster. The node in which SBK-GEM command issued executes the SBK-RAM and 
in all the given nodes SBK command is executed. The SBK-RAM integrate the performance results supplied 
by remote nodes.

## Build SBK-GEM
SBK-GEM is a submodule/project of the SBK framework. If you [build SBK](./../README.md#build-sbk), it builds the SBK-GEM package too.

## Running SBK-GEM locally
The standard help output with SBK-GEM parameters as follows

```
kmg@kmgs-MacBook-Pro SBK % ./sbk-gem/build/install/sbk-gem/bin/sbk-gem
2024-08-24 16:52:17 INFO Reflections took 11 ms to scan 0 urls, producing 0 keys and 0 values
2024-08-24 16:52:17 INFO Reflections took 31 ms to scan 1 urls, producing 4 keys and 6 values
2024-08-24 16:52:17 INFO 
   _____   ____    _  __            _____   ______   __  __
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|

2024-08-24 16:52:17 INFO Storage Benchmark Kit - Group Execution Monitor
2024-08-24 16:52:17 INFO SBK-GEM Version: 5.3
2024-08-24 16:52:17 INFO SBK-GEM Website: https://github.com/kmgowda/SBK
2024-08-24 16:52:17 INFO Arguments List: []
2024-08-24 16:52:17 INFO Java Runtime Version: 17.0.2+8
2024-08-24 16:52:17 INFO SBP Version Major: 3, Minor: 0
2024-08-24 16:52:17 INFO Storage Drivers Package: io.sbk.driver
2024-08-24 16:52:17 INFO sbk.applicationName: 
2024-08-24 16:52:17 INFO sbk.className: 
2024-08-24 16:52:17 INFO sbk.appHome: /Users/kmg/projects/SBK/sbk-gem/build/install/sbk-gem
2024-08-24 16:52:17 INFO '-class': 
2024-08-24 16:52:17 INFO Storage Classes in package 'io.sbk.driver': 0 []
2024-08-24 16:52:17 INFO Gem Logger Classes in package 'io.gem.logger': 1 [GemPrometheusLogger]

usage: sbk-gem -out GemPrometheusLogger
Storage Benchmark Kit - Group Execution Monitor

 -context <arg>         Prometheus Metric context;
                        'no' disables this option; default: 9719/metrics
 -copy <arg>            Copy the SBK package to remote hosts; default:
                        true
 -csvfile <arg>         CSV file to record results;
                        'no' disables this option, default: no
 -delete <arg>          Delete SBK package after benchmark; default: true
 -gempass <arg>         ssh user password of the remote hosts, default:
 -gemport <arg>         ssh port of the remote hosts, default: 22
 -gemuser <arg>         ssh user name of the remote hosts, default: user
 -help                  Help message
 -localhost <arg>       this local SBM host name, default:
                        kmgs-MacBook-Pro.local
 -maxlatency <arg>      Maximum latency;
                        use '-time' for time unit; default:180000 ms
 -millisecsleep <arg>   Idle sleep in milliseconds; default: 0 ms
 -minlatency <arg>      Minimum latency;
                        use '-time' for time unit; default:0 ms
 -nodes <arg>           remote hostnames separated by ',';
                        default:localhost
 -out <arg>             Logger Driver Class,
                        Available Drivers [GemPrometheusLogger]
 -readers <arg>         Number of readers
 -records <arg>         Number of records(events) if 'seconds' not
                        specified;
                        otherwise, Maximum records per second by
                        writer(s); and/or
                        Number of records per second by reader(s)
 -ro <arg>              Readonly Benchmarking,
                        Applicable only if both writers and readers are
                        set; default: false
 -rq <arg>              Benchmark Reade Requests; default: false
 -rsec <arg>            Number of seconds/step for readers, default: 0
 -rstep <arg>           Number of readers/step, default: 1
 -sbkcommand <arg>      remote sbk command; command path is relative to
                        'sbkdir', default: bin/sbk
 -sbkdir <arg>          directory path of sbk application, default:
                        /Users/kmg/projects/SBK/sbk-gem/build/install/sbk-
                        gem
 -sbmport <arg>         SBM port number; default: 9717
 -sbmsleepms <arg>      SBM idle milliseconds to sleep; default: 10 ms
 -seconds <arg>         Number of seconds to run
                        if not specified, runs forever
 -size <arg>            Size of each message (event or record)
 -sync <arg>            Each Writer calls flush/sync after writing <arg>
                        number of of events(records); and/or
                        <arg> number of events(records) per Write or Read
                        Transaction
 -throughput <arg>      If > 0, throughput in MB/s
                        If 0, writes/reads 'records'
                        If -1, get the maximum throughput (default: -1)
 -time <arg>            Latency Time Unit [ms:MILLISECONDS,
                        mcs:MICROSECONDS, ns:NANOSECONDS]; default: ms
 -wq <arg>              Benchmark Write Requests; default: false
 -writers <arg>         Number of writers
 -wsec <arg>            Number of seconds/step for writers, default: 0
 -wstep <arg>           Number of writers/step, default: 1

Please report issues at https://github.com/kmgowda/SBK

```
An Example output of SBK-GEM with 1 SBK file system benchmarking instances is as follows:

```
kmg@kmgs-MBP SBK % ./sbk-gem/build/install/sbk-gem/bin/sbk-gem -class file -readers 1 -size 10 -time ns -seconds 60 -gemuser kmg -gempass pass@123456 -sbkdir /Users/kmg/projects/SBK/build/install/sbk                              
2024-08-24 17:03:28 INFO Reflections took 9 ms to scan 0 urls, producing 0 keys and 0 values
2024-08-24 17:03:28 INFO Reflections took 27 ms to scan 1 urls, producing 4 keys and 6 values
2024-08-24 17:03:28 INFO 
   _____   ____    _  __            _____   ______   __  __
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|

2024-08-24 17:03:28 INFO Storage Benchmark Kit - Group Execution Monitor
2024-08-24 17:03:28 INFO SBK-GEM Version: 5.3
2024-08-24 17:03:28 INFO SBK-GEM Website: https://github.com/kmgowda/SBK
2024-08-24 17:03:28 INFO Arguments List: [-class, file, -readers, 1, -size, 10, -time, ns, -seconds, 60, -gemuser, kmg, -gempass, Laki@2322, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk]
2024-08-24 17:03:28 INFO Java Runtime Version: 17.0.2+8
2024-08-24 17:03:28 INFO SBP Version Major: 3, Minor: 0
2024-08-24 17:03:28 INFO Storage Drivers Package: io.sbk.driver
2024-08-24 17:03:28 INFO sbk.applicationName: 
2024-08-24 17:03:28 INFO sbk.className: 
2024-08-24 17:03:28 INFO sbk.appHome: /Users/kmg/projects/SBK/sbk-gem/build/install/sbk-gem
2024-08-24 17:03:28 INFO '-class': file
2024-08-24 17:03:28 INFO Storage Classes in package 'io.sbk.driver': 0 []
2024-08-24 17:03:28 INFO Gem Logger Classes in package 'io.gem.logger': 1 [GemPrometheusLogger]
2024-08-24 17:03:28 WARN Instantiation of storage class 'file' from the package 'io.sbk.driver' failed!, error: java.lang.ClassNotFoundException: class 'file' not found in package: io.sbk.driver
2024-08-24 17:03:28 INFO SBK-GEM [1]: Arguments to process : [-readers, 1, -size, 10, -time, ns, -seconds, 60, -gemuser, kmg, -gempass, Laki@2322, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk]
2024-08-24 17:03:28 INFO Time Unit: NANOSECONDS
2024-08-24 17:03:28 INFO Minimum Latency: 0 ns
2024-08-24 17:03:28 INFO Maximum Latency: 180000000000 ns
2024-08-24 17:03:28 INFO SBK dir: /Users/kmg/projects/SBK/build/install/sbk
2024-08-24 17:03:28 INFO SBK command: bin/sbk
2024-08-24 17:03:28 INFO Arguments to remote SBK command: -class file -readers 1 -size 10 -seconds 60 -out GrpcLogger -time ns -minlatency 0 -maxlatency 180000000000 -wq true -rq true -context no -sbm kmgs-MacBook-Pro.local -sbmport 9717
2024-08-24 17:03:28 INFO SBK-GEM: Arguments to remote SBK command verification Success..
2024-08-24 17:03:28 INFO Arguments to SBM: [-class, file, -action, r, -time, ns, -minlatency, 0, -maxlatency, 180000000000, -port, 9717, -wq, true, -rq, true, -max, 1, -millisecsleep, 10]
2024-08-24 17:03:28 INFO Logger for SBM: GemPrometheusLogger
2024-08-24 17:03:28 INFO SBK-GEM: Arguments to SBM command verification Success..
2024-08-24 17:03:28 INFO Window Latency Store: HashMap, Size: 512 MB
2024-08-24 17:03:28 INFO Total Window Latency Store: HashMap, Size: 1024 MB
2024-08-24 17:03:28 INFO Total Window Extension: None, Size: 0 MB
2024-08-24 17:03:28 INFO SBK GEM Benchmark Started
2024-08-24 17:03:28 INFO SBK-GEM: Ssh Connection to host 'localhost' starting...
2024-08-24 17:03:28 INFO Using MinaServiceFactoryFactory
2024-08-24 17:03:28 INFO resolveEffectiveResolver(kmg@localhost:22/null) no configuration file at /Users/kmg/.ssh/config
2024-08-24 17:03:28 WARN Server at localhost/127.0.0.1:22 presented unverified EC key: SHA256:6kTtY0SqflZ/04wvcClPuoe9ZRxaVyeakHBYuVfTkSg
2024-08-24 17:03:28 INFO Server announced support for publickey-hostbound@openssh.com version 0
2024-08-24 17:03:29 INFO SBK-GEM: Ssh Connection to host 'localhost' Success.
2024-08-24 17:03:29 INFO SBK-GEM: Ssh session establishment Success..
2024-08-24 17:03:29 INFO SBK-GEM: Matching Java Major Version: 17 Success..
2024-08-24 17:03:29 INFO SBK-GEM: Removing the remote directory: 'sbk'  Success..
2024-08-24 17:03:29 INFO SBK-GEM: Creating remote directory: 'sbk'  Success..
2024-08-24 17:03:33 INFO Copy SBK application: 'bin/sbk' to remote nodes Success..
2024-08-24 17:03:33 INFO SBM Started
2024-08-24 17:03:33 INFO SBK PrometheusLogger Started
2024-08-24 17:03:33 INFO SBK Connections PrometheusLogger Started
2024-08-24 17:03:33 INFO SbmLatencyBenchmark Started : 10 milliseconds idle sleep
2024-08-24 17:03:33 INFO SBK-GEM: Remote SBK command: sbk/bin/sbk -class file -readers 1 -size 10 -seconds 60 -out GrpcLogger -time ns -minlatency 0 -maxlatency 180000000000 -wq true -rq true -context no -sbm kmgs-MacBook-Pro.local -sbmport 9717
SBM     1 connections,     1 max connections: File Reading     0 writers,     0 readers,      0 max writers,     0 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,         0.0 read request MB,                 0 read request records,         0.0 read request records/sec,     0.00 read request MB/sec,     0.00 write response pending MB,             0 write response pending records,      0.00 read response pending MB,             0 read response pending records,      0.00 write read request pending MB,             0 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,         0.0 MB,                0 records,         0.0 records/sec,     0.00 MB/sec,      0.0 ns avg latency,       0 ns min latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ns 5th,       0 ns 10th,       0 ns 20th,       0 ns 25th,       0 ns 30th,       0 ns 40th,       0 ns 50th,       0 ns 60th,       0 ns 70th,       0 ns 75th,       0 ns 80th,       0 ns 90th,       0 ns 92.5th,       0 ns 95th,       0 ns 97.5th,       0 ns 99th,       0 ns 99.25th,       0 ns 99.5th,       0 ns 99.75th,       0 ns 99.9th,       0 ns 99.95th,       0 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        46.6 read request MB,           4888539 read request records,    975412.4 read request records/sec,     9.30 read request MB/sec,   -46.41 write response pending MB,      -4866091 write response pending records,      0.02 read response pending MB,        224490 read response pending records,    -46.62 write read request pending MB,      -4888539 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        46.4 MB,          4866091 records,    970933.4 records/sec,     9.26 MB/sec,    948.4 ns avg latency,       0 ns min latency, 4136393 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     833 ns 5th,     838 ns 10th,     848 ns 20th,     853 ns 25th,     856 ns 30th,     861 ns 40th,     867 ns 50th,     875 ns 60th,     882 ns 70th,     888 ns 75th,     896 ns 80th,    1044 ns 90th,    1068 ns 92.5th,    1238 ns 95th,    1316 ns 97.5th,    1576 ns 99th,    1704 ns 99.25th,    1969 ns 99.5th,    2896 ns 99.75th,   17876 ns 99.9th,   23334 ns 99.95th,   32718 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        46.2 read request MB,           4841599 read request records,    966803.2 read request records/sec,     9.22 read request MB/sec,   -92.69 write response pending MB,      -9718973 write response pending records,      0.01 read response pending MB,        111650 read response pending records,    -92.79 write read request pending MB,      -9730138 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        46.3 MB,          4852882 records,    969056.3 records/sec,     9.24 MB/sec,    953.4 ns avg latency,       0 ns min latency, 2916972 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:     831 ns 5th,     836 ns 10th,     850 ns 20th,     854 ns 25th,     858 ns 30th,     868 ns 40th,     875 ns 50th,     883 ns 60th,     897 ns 70th,     903 ns 75th,     915 ns 80th,     966 ns 90th,    1028 ns 92.5th,    1051 ns 95th,    1073 ns 97.5th,    1103 ns 99th,    1146 ns 99.25th,    1381 ns 99.5th,   14951 ns 99.75th,   22057 ns 99.9th,   26177 ns 99.95th,   35805 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        46.2 read request MB,           4849377 read request records,    967925.3 read request records/sec,     9.23 read request MB/sec,  -138.96 write response pending MB,     -14570701 write response pending records,      0.01 read response pending MB,         88150 read response pending records,   -139.04 write read request pending MB,     -14579515 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        46.3 MB,          4851728 records,    968394.6 records/sec,     9.24 MB/sec,    956.7 ns avg latency,       0 ns min latency, 1947324 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     830 ns 5th,     836 ns 10th,     850 ns 20th,     853 ns 25th,     856 ns 30th,     866 ns 40th,     873 ns 50th,     880 ns 60th,     892 ns 70th,     900 ns 75th,     917 ns 80th,    1049 ns 90th,    1072 ns 92.5th,    1140 ns 95th,    1292 ns 97.5th,    1343 ns 99th,    1369 ns 99.25th,    1460 ns 99.5th,    3014 ns 99.75th,   20632 ns 99.9th,   24448 ns 99.95th,   34089 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        49.1 read request MB,           5149636 read request records,   1029719.6 read request records/sec,     9.82 read request MB/sec,  -188.08 write response pending MB,     -19721991 write response pending records,      0.01 read response pending MB,         71610 read response pending records,   -188.15 write read request pending MB,     -19729151 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        49.1 MB,          5151290 records,   1030050.4 records/sec,     9.82 MB/sec,    901.5 ns avg latency,       0 ns min latency, 1858411 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     817 ns 5th,     826 ns 10th,     834 ns 20th,     836 ns 25th,     840 ns 30th,     848 ns 40th,     853 ns 50th,     858 ns 60th,     865 ns 70th,     869 ns 75th,     875 ns 80th,     889 ns 90th,     901 ns 92.5th,    1000 ns 95th,    1061 ns 97.5th,    1083 ns 99th,    1090 ns 99.25th,    1108 ns 99.5th,    1357 ns 99.75th,   17327 ns 99.9th,   22051 ns 99.95th,   30905 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        49.1 read request MB,           5147303 read request records,   1028886.7 read request records/sec,     9.81 read request MB/sec,  -237.19 write response pending MB,     -24870800 write response pending records,      0.01 read response pending MB,         56540 read response pending records,   -237.24 write read request pending MB,     -24876454 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        49.1 MB,          5148809 records,   1029187.7 records/sec,     9.82 MB/sec,    901.3 ns avg latency,       0 ns min latency, 1716458 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     820 ns 5th,     829 ns 10th,     835 ns 20th,     837 ns 25th,     840 ns 30th,     847 ns 40th,     853 ns 50th,     858 ns 60th,     865 ns 70th,     870 ns 75th,     875 ns 80th,     891 ns 90th,     899 ns 92.5th,     948 ns 95th,    1058 ns 97.5th,    1080 ns 99th,    1088 ns 99.25th,    1107 ns 99.5th,    1380 ns 99.75th,   17492 ns 99.9th,   22228 ns 99.95th,   30941 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        48.2 read request MB,           5055869 read request records,   1010927.9 read request records/sec,     9.64 read request MB/sec,  -285.40 write response pending MB,     -29925836 write response pending records,      0.01 read response pending MB,         64870 read response pending records,   -285.46 write read request pending MB,     -29932323 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        48.2 MB,          5055036 records,   1010761.3 records/sec,     9.64 MB/sec,    917.6 ns avg latency,       0 ns min latency, 1566318 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     829 ns 5th,     832 ns 10th,     837 ns 20th,     839 ns 25th,     842 ns 30th,     849 ns 40th,     855 ns 50th,     860 ns 60th,     867 ns 70th,     872 ns 75th,     877 ns 80th,     926 ns 90th,    1046 ns 92.5th,    1077 ns 95th,    1277 ns 97.5th,    1323 ns 99th,    1335 ns 99.25th,    1353 ns 99.5th,    1884 ns 99.75th,   17199 ns 99.9th,   22158 ns 99.95th,   31058 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        48.2 read request MB,           5056918 read request records,   1010908.6 read request records/sec,     9.64 read request MB/sec,  -333.63 write response pending MB,     -34983227 write response pending records,      0.01 read response pending MB,         60140 read response pending records,   -333.68 write read request pending MB,     -34989241 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        48.2 MB,          5057391 records,   1011003.2 records/sec,     9.64 MB/sec,    918.2 ns avg latency,       0 ns min latency, 2147700 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     829 ns 5th,     833 ns 10th,     839 ns 20th,     843 ns 25th,     847 ns 30th,     853 ns 40th,     857 ns 50th,     864 ns 60th,     872 ns 70th,     877 ns 75th,     881 ns 80th,     907 ns 90th,     937 ns 92.5th,    1027 ns 95th,    1062 ns 97.5th,    1086 ns 99th,    1098 ns 99.25th,    1174 ns 99.5th,    2266 ns 99.75th,   20376 ns 99.9th,   23490 ns 99.95th,   32986 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        47.9 read request MB,           5020992 read request records,   1003812.1 read request records/sec,     9.57 read request MB/sec,  -381.53 write response pending MB,     -40005973 write response pending records,      0.00 read response pending MB,         42600 read response pending records,   -381.57 write read request pending MB,     -40010233 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        47.9 MB,          5022746 records,   1004162.8 records/sec,     9.58 MB/sec,    924.3 ns avg latency,       0 ns min latency, 1816351 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:     823 ns 5th,     830 ns 10th,     835 ns 20th,     838 ns 25th,     841 ns 30th,     848 ns 40th,     854 ns 50th,     859 ns 60th,     867 ns 70th,     872 ns 75th,     877 ns 80th,     944 ns 90th,    1048 ns 92.5th,    1075 ns 95th,    1266 ns 97.5th,    1310 ns 99th,    1323 ns 99.25th,    1336 ns 99.5th,    1812 ns 99.75th,   20367 ns 99.9th,   23246 ns 99.95th,   33883 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        48.8 read request MB,           5112469 read request records,   1020722.6 read request records/sec,     9.73 read request MB/sec,  -430.29 write response pending MB,     -45119121 write response pending records,      0.00 read response pending MB,         35810 read response pending records,   -430.32 write read request pending MB,     -45122702 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        48.8 MB,          5113148 records,   1020858.2 records/sec,     9.74 MB/sec,    907.5 ns avg latency,       0 ns min latency, 1698880 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     826 ns 5th,     830 ns 10th,     834 ns 20th,     836 ns 25th,     838 ns 30th,     845 ns 40th,     851 ns 50th,     856 ns 60th,     861 ns 70th,     865 ns 75th,     871 ns 80th,     893 ns 90th,     936 ns 92.5th,    1050 ns 95th,    1080 ns 97.5th,    1166 ns 99th,    1281 ns 99.25th,    1316 ns 99.5th,    1564 ns 99.75th,   17723 ns 99.9th,   22639 ns 99.95th,   31346 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        48.3 read request MB,           5060783 read request records,   1011950.4 read request records/sec,     9.65 read request MB/sec,  -478.56 write response pending MB,     -50180218 write response pending records,      0.00 read response pending MB,         32670 read response pending records,   -478.59 write read request pending MB,     -50183485 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        48.3 MB,          5061097 records,   1012013.2 records/sec,     9.65 MB/sec,    916.1 ns avg latency,       0 ns min latency, 1664327 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     829 ns 5th,     832 ns 10th,     836 ns 20th,     838 ns 25th,     841 ns 30th,     848 ns 40th,     854 ns 50th,     859 ns 60th,     867 ns 70th,     872 ns 75th,     877 ns 80th,     907 ns 90th,    1007 ns 92.5th,    1069 ns 95th,    1261 ns 97.5th,    1329 ns 99th,    1337 ns 99.25th,    1349 ns 99.5th,    1570 ns 99.75th,   17431 ns 99.9th,   22697 ns 99.95th,   31465 ns 99.99th

SBM     1 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        49.2 read request MB,           5163582 read request records,   1032045.8 read request records/sec,     9.84 read request MB/sec,  -527.80 write response pending MB,     -55343739 write response pending records,      0.00 read response pending MB,         33280 read response pending records,   -527.83 write read request pending MB,     -55347067 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       5 seconds,        49.2 MB,          5163521 records,   1032033.6 records/sec,     9.84 MB/sec,    899.1 ns avg latency,       0 ns min latency, 1728715 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     819 ns 5th,     829 ns 10th,     833 ns 20th,     835 ns 25th,     837 ns 30th,     843 ns 40th,     849 ns 50th,     855 ns 60th,     859 ns 70th,     862 ns 75th,     867 ns 80th,     884 ns 90th,     899 ns 92.5th,    1008 ns 95th,    1066 ns 97.5th,    1086 ns 99th,    1095 ns 99.25th,    1111 ns 99.5th,    1349 ns 99.75th,   17443 ns 99.9th,   22216 ns 99.95th,   31133 ns 99.99th

SBM     0 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,        48.4 read request MB,           5076152 read request records,   1792209.7 read request records/sec,    17.09 read request MB/sec,  -576.21 write response pending MB,     -60420360 write response pending records,      0.00 read response pending MB,         28600 read response pending records,   -576.24 write read request pending MB,     -60423219 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,       2 seconds,        48.4 MB,          5076621 records,   1792375.3 records/sec,    17.09 MB/sec,    911.4 ns avg latency,       0 ns min latency, 1555326 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     817 ns 5th,     822 ns 10th,     832 ns 20th,     834 ns 25th,     836 ns 30th,     842 ns 40th,     849 ns 50th,     855 ns 60th,     860 ns 70th,     865 ns 75th,     870 ns 80th,     896 ns 90th,    1021 ns 92.5th,    1075 ns 95th,    1279 ns 97.5th,    1328 ns 99th,    1334 ns 99.25th,    1348 ns 99.5th,    1569 ns 99.75th,   17619 ns 99.9th,   21967 ns 99.95th,   30974 ns 99.99th

Total : SBM     0 connections,     1 max connections: File Reading     0 writers,     1 readers,      0 max writers,     1 max readers,          0.0 write request MB,                0 write request records,         0.0 write request records/sec,     0.00 write request MB/sec,       576.2 read request MB,          60423219 read request records,    960806.6 read request records/sec,     9.16 read request MB/sec,  -576.21 write response pending MB,     -60420360 write response pending records,      0.00 read response pending MB,         28600 read response pending records,   -576.24 write read request pending MB,     -60423219 write read request pending records,              0 write timeout events,     0.00 write timeout events/sec,             0 read timeout events,     0.00 read timeout events/sec,      62 seconds,       576.2 MB,         60420360 records,    960761.1 records/sec,     9.16 MB/sec,    920.9 ns avg latency,       0 ns min latency, 4136393 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:     824 ns 5th,     831 ns 10th,     836 ns 20th,     839 ns 25th,     843 ns 30th,     851 ns 40th,     856 ns 50th,     862 ns 60th,     871 ns 70th,     876 ns 75th,     882 ns 80th,     924 ns 90th,    1007 ns 92.5th,    1060 ns 95th,    1109 ns 97.5th,    1308 ns 99th,    1329 ns 99.25th,    1358 ns 99.5th,    2129 ns 99.75th,   19616 ns 99.9th,   23133 ns 99.95th,   32323 ns 99.99th

2024-08-24 17:04:35 INFO SbmLatencyBenchmark Shutdown
2024-08-24 17:04:35 INFO SBK PrometheusLogger Shutdown
2024-08-24 17:04:35 INFO SBM Shutdown
2024-08-24 17:04:35 INFO SBK GEM Benchmark Shutdown

SBK-GEM Remote Results
--------------------------------------------------------------------------------

Host 1: localhost, return code: 0
--------------------------------------------------------------------------------

```
Note that at the results, The SBK-GEM prints the return code of each of the remote host.


## SBK-GEM Grafana Dashboards
The SBK-GEM executes the SBK-RAM in local host; and SBK-RAM by default it starts the http server and all the output 
benchmark data is directed to the default port number: **9719** and **metrics** context. you can refer [SBK-RAM 
grafana Dashboards](https://kmgowda.github.io/SBK/sbk-ram/javadoc/index.html) for further details.

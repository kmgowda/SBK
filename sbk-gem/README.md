<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SBK-GEM : Group Execution Monitor

[![Api](https://img.shields.io/badge/SBK--GEM-API-brightgreen)](https://kmgowda.github.io/SBK/sbk-ram/javadoc/index.html)

The SBK (Storage Benchmark Kit) - GEM (Group Execution Monitor) combines SBK-RAM and SBK. you can just execute the 
SBK instances on multiple hosts with single SBK-GEM command. For any given cluster, you are running ssh-server in 
each node of the cluster , then SBK-GEM can be used run the SBK in all the nodes of the cluster. The SBK-GEM copies 
the SBK application to all nodes of the cluster. The node in which SBK-GEM command issued executes the SBK-RAM and 
in all the given nodes SBK command is executed. The SBK-RAM integrate the performance results supplied 
by remote nodes.

## Build SBK-GEM
SBK-GEM is a submodule/project of the SBK framework. If you [build SBK](https://github.com/kmgowda/SBK#build-sbk), 
it builds the SBK-GEM package too.

## Running SBK-GEM locally
The standard help output with SBK-GEM parameters as follows

```
kmg@kmgs-MBP SBK % ./sbk-gem/build/install/sbk-gem/bin/sbk-gem                                                                                                                                    
2021-08-07 14:50:30 INFO Reflections took 70 ms to scan 3 urls, producing 85 keys and 109 values 
2021-08-07 14:50:30 INFO 
   _____   ____    _  __            _____   ______   __  __
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|

2021-08-07 14:50:30 INFO Storage Benchmark Kit - Group Execution Monitor
2021-08-07 14:50:30 INFO SBK-GEM Version: 0.91
2021-08-07 14:50:30 INFO Arguments List: []
2021-08-07 14:50:30 INFO Java Runtime Version: 11.0.8+11
2021-08-07 14:50:30 INFO Storage Drivers Package: io.sbk
2021-08-07 14:50:30 INFO sbk.applicationName: 
2021-08-07 14:50:30 INFO sbk.className: 
2021-08-07 14:50:30 INFO sbk.appHome: /Users/kmg/projects/SBK/sbk-gem/build/install/sbk-gem
2021-08-07 14:50:30 INFO '-class': 
2021-08-07 14:50:30 INFO Available Storage Drivers in package 'io.sbk': 0 []

usage: sbk-gem
Storage Benchmark Kit - Group Execution Monitor

 -context <arg>      Prometheus Metric context; 'no' disables this option;
                     default: 9719/metrics
 -copy <arg>         Copy the SBK package to remote hosts; default: true
 -csvfile <arg>      CSV file to record results; 'no' disables this
                     option, default: no
 -delete <arg>       Delete SBK package after benchmark; default: true
 -gempass <arg>      ssh user password of the remote hosts, default:
 -gemport <arg>      ssh port of the remote hosts, default: 22
 -gemuser <arg>      ssh user name of the remote hosts, default: user
 -help               Help message
 -localhost <arg>    this local RAM host name, default: kmgs-MBP.Dlink
 -maxlatency <arg>   Maximum latency; use '-time' for time unit; default:
                     180000 ms
 -minlatency <arg>   Minimum latency; use '-time' for time unit; default:
                     0 ms
 -nodes <arg>        remote hostnames separated by ',' , default:
                     localhost
 -ramport <arg>      RAM port number; default: 9716
 -readers <arg>      Number of readers
 -records <arg>      Number of records(events) if 'seconds' not specified;
                     otherwise, Maximum records per second by writer(s)
                     and/or Number of records per second by reader(s)
 -rsec <arg>         Number of seconds/step for readers, default: 0
 -rstep <arg>        Number of readers/step, default: 1
 -sbkcommand <arg>   remote sbk command; command path is relative to
                     'sbkdir', default: bin/sbk
 -sbkdir <arg>       directory path of sbk application, default:
                     /Users/kmg/projects/SBK/sbk-gem/build/install/sbk-gem
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
An Example output of SBK-GEM with 1 SBK file system benchmarking instances is as follows:

```
kmg@kmgs-MBP SBK % ./sbk-gem/build/install/sbk-gem/bin/sbk-gem -class file -readers 1 -size 10 -time ns -seconds 60 -gemuser kmg -gempass Laki@2322 -sbkdir /Users/kmg/projects/SBK/build/install/sbk                              
2021-08-07 14:52:56 INFO Reflections took 63 ms to scan 3 urls, producing 85 keys and 109 values 
2021-08-07 14:52:56 INFO 
   _____   ____    _  __            _____   ______   __  __
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|

2021-08-07 14:52:56 INFO Storage Benchmark Kit - Group Execution Monitor
2021-08-07 14:52:56 INFO SBK-GEM Version: 0.91
2021-08-07 14:52:56 INFO Arguments List: [-class, file, -readers, 1, -size, 10, -time, ns, -seconds, 60, -gemuser, kmg, -gempass, Laki@2322, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk]
2021-08-07 14:52:56 INFO Java Runtime Version: 11.0.8+11
2021-08-07 14:52:56 INFO Storage Drivers Package: io.sbk
2021-08-07 14:52:56 INFO sbk.applicationName: 
2021-08-07 14:52:56 INFO sbk.className: 
2021-08-07 14:52:56 INFO sbk.appHome: /Users/kmg/projects/SBK/sbk-gem/build/install/sbk-gem
2021-08-07 14:52:56 INFO '-class': file
2021-08-07 14:52:56 INFO Available Storage Drivers in package 'io.sbk': 0 []
2021-08-07 14:52:56 WARN Instantiation of storage class 'file' from the package 'io.sbk' failed!, error: java.lang.ClassNotFoundException: storage class 'file' not found in package: io.sbk
2021-08-07 14:52:56 INFO SBK-GEM [1]: Arguments to process : [-readers, 1, -size, 10, -time, ns, -seconds, 60, -gemuser, kmg, -gempass, Laki@2322, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk]
2021-08-07 14:52:56 INFO Time Unit: NANOSECONDS
2021-08-07 14:52:56 INFO Minimum Latency: 0 ns
2021-08-07 14:52:56 INFO Maximum Latency: 180000000000 ns
2021-08-07 14:52:56 INFO SBK dir: /Users/kmg/projects/SBK/build/install/sbk
2021-08-07 14:52:56 INFO SBK command: bin/sbk
2021-08-07 14:52:56 INFO Arguments to remote SBK command: -class file -readers 1 -size 10 -seconds 60 -time ns -minlatency 0 -maxlatency 180000000000 -context no -ram kmgs-MBP.Dlink -ramport 9716
2021-08-07 14:52:56 INFO SBK-GEM: Arguments to remote SBK command verification Success..
2021-08-07 14:52:56 INFO Arguments to  SBK-RAM: [-class, file, -action, r, -max, 1]
2021-08-07 14:52:56 INFO SBK-GEM: Arguments to SBK-RAM command verification Success..
2021-08-07 14:52:56 INFO Window Latency Store: HashMap
2021-08-07 14:52:57 INFO SBK GEM Benchmark Started
2021-08-07 14:52:57 INFO Using MinaServiceFactoryFactory
2021-08-07 14:52:57 WARN Server at localhost/127.0.0.1:22 presented unverified EC key: SHA256:6kTtY0SqflZ/04wvcClPuoe9ZRxaVyeakHBYuVfTkSg
2021-08-07 14:52:57 INFO SBK-GEM: Ssh Connection to host 'localhost' Success..
2021-08-07 14:52:57 INFO SBK-GEM: Ssh session establishment Success..
2021-08-07 14:52:57 WARN globalRequest(ClientConnectionService[ClientSessionImpl[kmg@localhost/127.0.0.1:22]])[hostkeys-00@openssh.com, want-reply=false] failed (SshException) to process: EdDSA provider not supported
2021-08-07 14:52:57 INFO SBK-GEM: Matching Java Major Version: 11 Success..
2021-08-07 14:52:57 INFO SBK-GEM: Removing the remote directory: 'sbk'  Success..
2021-08-07 14:52:57 INFO SBK-GEM: Creating remote directory: 'sbk'  Success..
2021-08-07 14:53:00 INFO Copy SBK application: 'bin/sbk' to remote nodes Success..
2021-08-07 14:53:00 INFO SBK RAM Benchmark Started
2021-08-07 14:53:00 INFO SBK PrometheusLogger Started
2021-08-07 14:53:00 INFO SBK Connections PrometheusLogger Started
2021-08-07 14:53:00 INFO LatenciesRecord Benchmark Started
2021-08-07 14:53:00 INFO SBK-GEM: Remote SBK command: sbk/bin/sbk -class file -readers 1 -size 10 -seconds 60 -time ns -minlatency 0 -maxlatency 180000000000 -context no -ram kmgs-MBP.Dlink -ramport 9716
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     0 Readers,      0 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,                0 records,         0.0 records/sec,     0.00 MB/sec,      0.0 ns avg latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  0%, SLC-2:  0%; Latency Percentiles:       0 ns 10th,       0 ns 20th,       0 ns 25th,       0 ns 30th,       0 ns 40th,       0 ns 50th,       0 ns 60th,       0 ns 70th,       0 ns 75th,       0 ns 80th,       0 ns 90th,       0 ns 92.5th,       0 ns 95th,       0 ns 97.5th,       0 ns 99th,       0 ns 99.25th,       0 ns 99.5th,       0 ns 99.75th,       0 ns 99.9th,       0 ns 99.95th,       0 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.1 MB,          5991050 records,   1196814.0 records/sec,    11.41 MB/sec,    755.1 ns avg latency, 3281382 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  2%, SLC-2: 87%; Latency Percentiles:     658 ns 10th,     665 ns 20th,     667 ns 25th,     670 ns 30th,     675 ns 40th,     681 ns 50th,     685 ns 60th,     690 ns 70th,     693 ns 75th,     697 ns 80th,     753 ns 90th,     925 ns 92.5th,     997 ns 95th,    1198 ns 97.5th,    1247 ns 99th,    1273 ns 99.25th,    1396 ns 99.5th,    2638 ns 99.75th,   17231 ns 99.9th,   21124 ns 99.95th,   28670 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        59.9 MB,          6283905 records,   1256399.0 records/sec,    11.98 MB/sec,    726.1 ns avg latency, 3071001 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  2%, SLC-2: 88%; Latency Percentiles:     654 ns 10th,     662 ns 20th,     665 ns 25th,     668 ns 30th,     675 ns 40th,     681 ns 50th,     685 ns 60th,     690 ns 70th,     693 ns 75th,     697 ns 80th,     718 ns 90th,     735 ns 92.5th,     750 ns 95th,     813 ns 97.5th,     860 ns 99th,     902 ns 99.25th,    1041 ns 99.5th,    1303 ns 99.75th,   17601 ns 99.9th,   21278 ns 99.95th,   29343 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        59.3 MB,          6217791 records,   1242894.2 records/sec,    11.85 MB/sec,    734.9 ns avg latency, 1719584 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 88%; Latency Percentiles:     664 ns 10th,     676 ns 20th,     680 ns 25th,     682 ns 30th,     685 ns 40th,     688 ns 50th,     693 ns 60th,     697 ns 70th,     700 ns 75th,     703 ns 80th,     712 ns 90th,     716 ns 92.5th,     726 ns 95th,     791 ns 97.5th,     854 ns 99th,     877 ns 99.25th,    1013 ns 99.5th,    1411 ns 99.75th,   20075 ns 99.9th,   22957 ns 99.95th,   31329 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        60.7 MB,          6365550 records,   1271612.4 records/sec,    12.13 MB/sec,    717.4 ns avg latency, 1501527 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  2%, SLC-2: 89%; Latency Percentiles:     657 ns 10th,     666 ns 20th,     668 ns 25th,     671 ns 30th,     679 ns 40th,     682 ns 50th,     685 ns 60th,     689 ns 70th,     691 ns 75th,     694 ns 80th,     701 ns 90th,     705 ns 92.5th,     710 ns 95th,     731 ns 97.5th,     804 ns 99th,     836 ns 99.25th,     916 ns 99.5th,    1144 ns 99.75th,   16058 ns 99.9th,   20937 ns 99.95th,   29657 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        58.2 MB,          6104477 records,   1218441.0 records/sec,    11.62 MB/sec,    747.5 ns avg latency, 1400744 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 88%; Latency Percentiles:     679 ns 10th,     682 ns 20th,     683 ns 25th,     685 ns 30th,     688 ns 40th,     692 ns 50th,     697 ns 60th,     703 ns 70th,     708 ns 75th,     715 ns 80th,     755 ns 90th,     793 ns 92.5th,     834 ns 95th,     848 ns 97.5th,     889 ns 99th,     962 ns 99.25th,    1129 ns 99.5th,    1438 ns 99.75th,   18040 ns 99.9th,   23123 ns 99.95th,   30918 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        60.2 MB,          6308332 records,   1260843.2 records/sec,    12.02 MB/sec,    723.7 ns avg latency, 1280230 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 89%; Latency Percentiles:     661 ns 10th,     670 ns 20th,     676 ns 25th,     679 ns 30th,     683 ns 40th,     686 ns 50th,     691 ns 60th,     697 ns 70th,     699 ns 75th,     702 ns 80th,     711 ns 90th,     715 ns 92.5th,     721 ns 95th,     752 ns 97.5th,     827 ns 99th,     849 ns 99.25th,     928 ns 99.5th,    1186 ns 99.75th,   16101 ns 99.9th,   21498 ns 99.95th,   30205 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        60.5 MB,          6340484 records,   1265034.1 records/sec,    12.06 MB/sec,    720.5 ns avg latency, 1695339 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 89%; Latency Percentiles:     665 ns 10th,     672 ns 20th,     677 ns 25th,     680 ns 30th,     682 ns 40th,     685 ns 50th,     687 ns 60th,     692 ns 70th,     694 ns 75th,     697 ns 80th,     705 ns 90th,     709 ns 92.5th,     715 ns 95th,     756 ns 97.5th,     846 ns 99th,     862 ns 99.25th,     936 ns 99.5th,    1186 ns 99.75th,   15594 ns 99.9th,   21033 ns 99.95th,   28514 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        60.0 MB,          6288530 records,   1256607.3 records/sec,    11.98 MB/sec,    726.2 ns avg latency, 1465237 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 89%; Latency Percentiles:     665 ns 10th,     673 ns 20th,     678 ns 25th,     680 ns 30th,     684 ns 40th,     687 ns 50th,     692 ns 60th,     698 ns 70th,     700 ns 75th,     703 ns 80th,     713 ns 90th,     717 ns 92.5th,     724 ns 95th,     745 ns 97.5th,     812 ns 99th,     841 ns 99.25th,     925 ns 99.5th,    1182 ns 99.75th,   16537 ns 99.9th,   21780 ns 99.95th,   30335 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        60.2 MB,          6308808 records,   1259225.6 records/sec,    12.01 MB/sec,    723.7 ns avg latency, 1293712 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 89%; Latency Percentiles:     667 ns 10th,     675 ns 20th,     678 ns 25th,     680 ns 30th,     683 ns 40th,     686 ns 50th,     689 ns 60th,     694 ns 70th,     697 ns 75th,     699 ns 80th,     710 ns 90th,     714 ns 92.5th,     722 ns 95th,     748 ns 97.5th,     816 ns 99th,     842 ns 99.25th,     902 ns 99.5th,    1128 ns 99.75th,   16221 ns 99.9th,   21703 ns 99.95th,   29205 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        60.1 MB,          6297006 records,   1258332.3 records/sec,    12.00 MB/sec,    725.1 ns avg latency, 1450599 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 89%; Latency Percentiles:     662 ns 10th,     671 ns 20th,     678 ns 25th,     680 ns 30th,     683 ns 40th,     686 ns 50th,     691 ns 60th,     696 ns 70th,     699 ns 75th,     702 ns 80th,     712 ns 90th,     717 ns 92.5th,     728 ns 95th,     779 ns 97.5th,     847 ns 99th,     863 ns 99.25th,     972 ns 99.5th,    1205 ns 99.75th,   16058 ns 99.9th,   21581 ns 99.95th,   29901 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        60.1 MB,          6305623 records,   1258801.7 records/sec,    12.00 MB/sec,    723.7 ns avg latency, 1419445 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 89%; Latency Percentiles:     665 ns 10th,     674 ns 20th,     679 ns 25th,     681 ns 30th,     683 ns 40th,     686 ns 50th,     690 ns 60th,     694 ns 70th,     697 ns 75th,     699 ns 80th,     708 ns 90th,     711 ns 92.5th,     718 ns 95th,     752 ns 97.5th,     829 ns 99th,     851 ns 99.25th,     946 ns 99.5th,    1192 ns 99.75th,   16368 ns 99.9th,   21960 ns 99.95th,   30023 ns 99.99th.
Sbk-Ram     0 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        3 seconds,        60.4 MB,          6334459 records,   2063629.7 records/sec,    19.68 MB/sec,    719.0 ns avg latency, 1679693 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:  1%, SLC-2: 88%; Latency Percentiles:     663 ns 10th,     670 ns 20th,     674 ns 25th,     678 ns 30th,     682 ns 40th,     684 ns 50th,     687 ns 60th,     691 ns 70th,     693 ns 75th,     696 ns 80th,     704 ns 90th,     708 ns 92.5th,     713 ns 95th,     743 ns 97.5th,     814 ns 99th,     837 ns 99.25th,     890 ns 99.5th,    1108 ns 99.75th,   15861 ns 99.9th,   21088 ns 99.95th,   28331 ns 99.99th.
2021-08-07 14:54:03 INFO LatenciesRecord Benchmark Shutdown
2021-08-07 14:54:03 INFO SBK PrometheusLogger Shutdown
2021-08-07 14:54:03 INFO SBK RAM Benchmark Shutdown
2021-08-07 14:54:03 INFO SBK GEM Benchmark Shutdown

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

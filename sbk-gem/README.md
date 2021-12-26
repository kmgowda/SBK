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
 -ramport <arg>      RAM port number; default: 9717
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
kmg@kmgs-MBP SBK % ./sbk-gem/build/install/sbk-gem/bin/sbk-gem -class file -readers 1 -size 10 -time ns -seconds 60 -gemuser kmg -gempass pass@123456 -sbkdir /Users/kmg/projects/SBK/build/install/sbk                              
2021-08-20 14:25:51 INFO Reflections took 69 ms to scan 3 urls, producing 88 keys and 115 values 
2021-08-20 14:25:51 INFO 
   _____   ____    _  __            _____   ______   __  __
  / ____| |  _ \  | |/ /           / ____| |  ____| |  \/  |
 | (___   | |_) | | ' /   ______  | |  __  | |__    | \  / |
  \___ \  |  _ <  |  <   |______| | | |_ | |  __|   | |\/| |
  ____) | | |_) | | . \           | |__| | | |____  | |  | |
 |_____/  |____/  |_|\_\           \_____| |______| |_|  |_|

2021-08-20 14:25:51 INFO Storage Benchmark Kit - Group Execution Monitor
2021-08-20 14:25:51 INFO SBK-GEM Version: 0.91
2021-08-20 14:25:51 INFO Arguments List: [-class, file, -readers, 1, -size, 10, -time, ns, -seconds, 60, -gemuser, kmg, -gempass, pass@123456, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk]
2021-08-20 14:25:51 INFO Java Runtime Version: 11.0.8+11
2021-08-20 14:25:51 INFO Storage Drivers Package: io.sbk
2021-08-20 14:25:51 INFO sbk.applicationName: 
2021-08-20 14:25:51 INFO sbk.className: 
2021-08-20 14:25:51 INFO sbk.appHome: /Users/kmg/projects/SBK/sbk-gem/build/install/sbk-gem
2021-08-20 14:25:51 INFO '-class': file
2021-08-20 14:25:51 INFO Available Storage Drivers in package 'io.sbk': 0 []
2021-08-20 14:25:51 WARN Instantiation of storage class 'file' from the package 'io.sbk' failed!, error: java.lang.ClassNotFoundException: storage class 'file' not found in package: io.sbk
2021-08-20 14:25:51 INFO SBK-GEM [1]: Arguments to process : [-readers, 1, -size, 10, -time, ns, -seconds, 60, -gemuser, kmg, -gempass, pass@123456, -sbkdir, /Users/kmg/projects/SBK/build/install/sbk]
2021-08-20 14:25:51 INFO Time Unit: NANOSECONDS
2021-08-20 14:25:51 INFO Minimum Latency: 0 ns
2021-08-20 14:25:51 INFO Maximum Latency: 180000000000 ns
2021-08-20 14:25:51 INFO SBK dir: /Users/kmg/projects/SBK/build/install/sbk
2021-08-20 14:25:51 INFO SBK command: bin/sbk
2021-08-20 14:25:51 INFO Arguments to remote SBK command: -class file -readers 1 -size 10 -seconds 60 -time ns -minlatency 0 -maxlatency 180000000000 -context no -ram kmgs-MBP.Dlink -ramport 9717
2021-08-20 14:25:51 INFO SBK-GEM: Arguments to remote SBK command verification Success..
2021-08-20 14:25:51 INFO Arguments to  SBK-RAM: [-class, file, -action, r, -max, 1]
2021-08-20 14:25:51 INFO SBK-GEM: Arguments to SBK-RAM command verification Success..
2021-08-20 14:25:52 INFO Window Latency Store: HashMap, Size: 512 MB
2021-08-20 14:25:52 INFO Total Window Latency Store: HashMap, Size: 1024 MB
2021-08-20 14:25:52 INFO Total Window Extension: None, Size: 0 MB
2021-08-20 14:25:52 INFO SBK GEM Benchmark Started
2021-08-20 14:25:52 INFO Using MinaServiceFactoryFactory
2021-08-20 14:25:52 WARN Server at localhost/127.0.0.1:22 presented unverified EC key: SHA256:6kTtY0SqflZ/04wvcClPuoe9ZRxaVyeakHBYuVfTkSg
2021-08-20 14:25:52 INFO SBK-GEM: Ssh Connection to host 'localhost' Success..
2021-08-20 14:25:52 INFO SBK-GEM: Ssh session establishment Success..
2021-08-20 14:25:52 WARN globalRequest(ClientConnectionService[ClientSessionImpl[kmg@localhost/127.0.0.1:22]])[hostkeys-00@openssh.com, want-reply=false] failed (SshException) to process: EdDSA provider not supported
2021-08-20 14:25:52 INFO SBK-GEM: Matching Java Major Version: 11 Success..
2021-08-20 14:25:52 INFO SBK-GEM: Removing the remote directory: 'sbk'  Success..
2021-08-20 14:25:52 INFO SBK-GEM: Creating remote directory: 'sbk'  Success..
2021-08-20 14:25:55 INFO Copy SBK application: 'bin/sbk' to remote nodes Success..
2021-08-20 14:25:55 INFO SBK RAM Benchmark Started
2021-08-20 14:25:56 INFO SBK PrometheusLogger Started
2021-08-20 14:25:56 INFO SBK Connections PrometheusLogger Started
2021-08-20 14:25:56 INFO LatenciesRecord Benchmark Started
2021-08-20 14:25:56 INFO SBK-GEM: Remote SBK command: sbk/bin/sbk -class file -readers 1 -size 10 -seconds 60 -time ns -minlatency 0 -maxlatency 180000000000 -context no -ram kmgs-MBP.Dlink -ramport 9717
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     0 Readers,      0 Max Writers,     0 Max Readers,        5 seconds,         0.0 MB,                0 records,         0.0 records/sec,     0.00 MB/sec,      0.0 ns avg latency,       0 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   0; Latency Percentiles:       0 ns 10th,       0 ns 20th,       0 ns 25th,       0 ns 30th,       0 ns 40th,       0 ns 50th,       0 ns 60th,       0 ns 70th,       0 ns 75th,       0 ns 80th,       0 ns 90th,       0 ns 92.5th,       0 ns 95th,       0 ns 97.5th,       0 ns 99th,       0 ns 99.25th,       0 ns 99.5th,       0 ns 99.75th,       0 ns 99.9th,       0 ns 99.95th,       0 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        52.3 MB,          5482367 records,   1094823.9 records/sec,    10.44 MB/sec,    827.0 ns avg latency, 4976086 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     675 ns 10th,     681 ns 20th,     684 ns 25th,     687 ns 30th,     692 ns 40th,     698 ns 50th,     708 ns 60th,     726 ns 70th,     735 ns 75th,     749 ns 80th,     836 ns 90th,     989 ns 92.5th,    1131 ns 95th,    1299 ns 97.5th,    1448 ns 99th,    1963 ns 99.25th,    2785 ns 99.5th,   16101 ns 99.75th,   22526 ns 99.9th,   25453 ns 99.95th,   32818 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        55.7 MB,          5843752 records,   1167122.6 records/sec,    11.13 MB/sec,    786.4 ns avg latency, 3759403 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     673 ns 10th,     679 ns 20th,     682 ns 25th,     685 ns 30th,     689 ns 40th,     694 ns 50th,     700 ns 60th,     711 ns 70th,     721 ns 75th,     730 ns 80th,     801 ns 90th,     825 ns 92.5th,     835 ns 95th,     851 ns 97.5th,    1007 ns 99th,    1123 ns 99.25th,    1257 ns 99.5th,   16104 ns 99.75th,   22434 ns 99.9th,   25130 ns 99.95th,   31812 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        53.1 MB,          5564312 records,   1112291.8 records/sec,    10.61 MB/sec,    822.6 ns avg latency, 2499157 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     693 ns 10th,     698 ns 20th,     702 ns 25th,     705 ns 30th,     713 ns 40th,     723 ns 50th,     728 ns 60th,     734 ns 70th,     738 ns 75th,     743 ns 80th,     773 ns 90th,     791 ns 92.5th,     808 ns 95th,     829 ns 97.5th,    1019 ns 99th,    1149 ns 99.25th,    1954 ns 99.5th,   18006 ns 99.75th,   23487 ns 99.9th,   25449 ns 99.95th,   32980 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        53.0 MB,          5556600 records,   1110901.3 records/sec,    10.59 MB/sec,    823.0 ns avg latency, 2222784 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   9; Latency Percentiles:     693 ns 10th,     699 ns 20th,     702 ns 25th,     705 ns 30th,     713 ns 40th,     722 ns 50th,     728 ns 60th,     736 ns 70th,     741 ns 75th,     747 ns 80th,     786 ns 90th,     801 ns 92.5th,     811 ns 95th,     829 ns 97.5th,     990 ns 99th,    1136 ns 99.25th,    2039 ns 99.5th,   18060 ns 99.75th,   23405 ns 99.9th,   24743 ns 99.95th,   31924 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        56.2 MB,          5889528 records,   1175348.4 records/sec,    11.21 MB/sec,    776.2 ns avg latency, 1919626 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     677 ns 10th,     681 ns 20th,     684 ns 25th,     686 ns 30th,     690 ns 40th,     694 ns 50th,     699 ns 60th,     706 ns 70th,     711 ns 75th,     721 ns 80th,     745 ns 90th,     762 ns 92.5th,     806 ns 95th,     839 ns 97.5th,    1139 ns 99th,    1191 ns 99.25th,    1265 ns 99.5th,   15422 ns 99.75th,   22520 ns 99.9th,   26356 ns 99.95th,   33135 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.3 MB,          6011604 records,   1200795.5 records/sec,    11.45 MB/sec,    760.1 ns avg latency, 1872033 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   9; Latency Percentiles:     676 ns 10th,     681 ns 20th,     683 ns 25th,     685 ns 30th,     689 ns 40th,     693 ns 50th,     696 ns 60th,     700 ns 70th,     703 ns 75th,     706 ns 80th,     718 ns 90th,     725 ns 92.5th,     745 ns 95th,     794 ns 97.5th,     840 ns 99th,     904 ns 99.25th,    1097 ns 99.5th,   15084 ns 99.75th,   22120 ns 99.9th,   25564 ns 99.95th,   32337 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        58.0 MB,          6079619 records,   1214649.0 records/sec,    11.58 MB/sec,    750.9 ns avg latency, 2811343 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   9; Latency Percentiles:     673 ns 10th,     677 ns 20th,     678 ns 25th,     679 ns 30th,     682 ns 40th,     686 ns 50th,     689 ns 60th,     693 ns 70th,     696 ns 75th,     698 ns 80th,     708 ns 90th,     713 ns 92.5th,     721 ns 95th,     752 ns 97.5th,     823 ns 99th,     865 ns 99.25th,    1069 ns 99.5th,   14925 ns 99.75th,   22076 ns 99.9th,   26094 ns 99.95th,   31842 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.7 MB,          6053821 records,   1209964.9 records/sec,    11.54 MB/sec,    754.3 ns avg latency, 1797372 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     675 ns 10th,     678 ns 20th,     679 ns 25th,     681 ns 30th,     684 ns 40th,     687 ns 50th,     690 ns 60th,     694 ns 70th,     696 ns 75th,     699 ns 80th,     708 ns 90th,     713 ns 92.5th,     722 ns 95th,     758 ns 97.5th,     830 ns 99th,     863 ns 99.25th,    1065 ns 99.5th,   15031 ns 99.75th,   22165 ns 99.9th,   26075 ns 99.95th,   32080 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.7 MB,          6053323 records,   1210133.8 records/sec,    11.54 MB/sec,    754.4 ns avg latency, 1651644 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     674 ns 10th,     677 ns 20th,     679 ns 25th,     680 ns 30th,     683 ns 40th,     686 ns 50th,     689 ns 60th,     693 ns 70th,     696 ns 75th,     699 ns 80th,     710 ns 90th,     716 ns 92.5th,     727 ns 95th,     753 ns 97.5th,     820 ns 99th,     865 ns 99.25th,    1063 ns 99.5th,   15069 ns 99.75th,   22211 ns 99.9th,   26293 ns 99.95th,   32048 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.1 MB,          5984359 records,   1195660.5 records/sec,    11.40 MB/sec,    764.3 ns avg latency, 1997768 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     676 ns 10th,     680 ns 20th,     683 ns 25th,     685 ns 30th,     689 ns 40th,     692 ns 50th,     696 ns 60th,     700 ns 70th,     703 ns 75th,     706 ns 80th,     719 ns 90th,     727 ns 92.5th,     741 ns 95th,     775 ns 97.5th,     839 ns 99th,     902 ns 99.25th,    1083 ns 99.5th,   15106 ns 99.75th,   22132 ns 99.9th,   25918 ns 99.95th,   32916 ns 99.99th.
Sbk-Ram     1 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,        57.1 MB,          5987817 records,   1195572.8 records/sec,    11.40 MB/sec,    763.2 ns avg latency, 1752429 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     675 ns 10th,     679 ns 20th,     681 ns 25th,     684 ns 30th,     688 ns 40th,     692 ns 50th,     696 ns 60th,     700 ns 70th,     703 ns 75th,     707 ns 80th,     719 ns 90th,     724 ns 92.5th,     739 ns 95th,     776 ns 97.5th,     841 ns 99th,     906 ns 99.25th,    1091 ns 99.5th,   15145 ns 99.75th,   22315 ns 99.9th,   26292 ns 99.95th,   33686 ns 99.99th.
Sbk-Ram     0 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        2 seconds,        57.4 MB,          6014953 records,   2113074.1 records/sec,    20.15 MB/sec,    758.4 ns avg latency, 1594881 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   9; Latency Percentiles:     675 ns 10th,     678 ns 20th,     680 ns 25th,     682 ns 30th,     685 ns 40th,     690 ns 50th,     694 ns 60th,     698 ns 70th,     702 ns 75th,     706 ns 80th,     724 ns 90th,     732 ns 92.5th,     743 ns 95th,     772 ns 97.5th,     835 ns 99th,     891 ns 99.25th,    1077 ns 99.5th,   15003 ns 99.75th,   22048 ns 99.9th,   25664 ns 99.95th,   31907 ns 99.99th.
Total : Sbk-Ram     0 Connections,     1 Max Connections: file Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,       62 seconds,       672.6 MB,         70522055 records,   1121007.8 records/sec,    10.69 MB/sec,    777.4 ns avg latency, 4976086 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:     676 ns 10th,     680 ns 20th,     683 ns 25th,     685 ns 30th,     689 ns 40th,     694 ns 50th,     698 ns 60th,     705 ns 70th,     711 ns 75th,     719 ns 80th,     742 ns 90th,     755 ns 92.5th,     789 ns 95th,     832 ns 97.5th,    1101 ns 99th,    1240 ns 99.25th,    1346 ns 99.5th,   15844 ns 99.75th,   22615 ns 99.9th,   25762 ns 99.95th,   32402 ns 99.99th.
2021-08-20 14:26:59 INFO LatenciesRecord Benchmark Shutdown
2021-08-20 14:26:59 INFO SBK PrometheusLogger Shutdown
2021-08-20 14:26:59 INFO SBK RAM Benchmark Shutdown
2021-08-20 14:26:59 INFO SBK GEM Benchmark Shutdown

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

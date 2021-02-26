<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# NATS Driver for SBK
The NATS driver for SBK supports the End to End latency. This means you have supply both writers and readers while benchmarking.

## NATS standalone server installation 
Refer to this page : https://docs.nats.io/nats-server/installation
or simple just execute the below command
```
 docker run -p 4222:4222 -ti nats:latest
```
you need to have Dockers installed on your system.
An example, SBK benchmarkig command is
```
./build/install/sbk/bin/sbk -class Nats -uri nats://localhost:4222 -topic kmg-topic-1 -size 10 -writers 5 -readers 5 -seconds 60
```

sample output is as follows:
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class Nats -uri nats://localhost:4222 -topic kmg-topic-1 -size 10 -writers 5 -readers 5 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-02-27 21:00:23 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-02-27 21:00:23 INFO Java Runtime Version: 11.0.8+11
2021-02-27 21:00:23 INFO SBK Version: 0.861
2021-02-27 21:00:23 INFO Arguments List: [-class, Nats, -uri, nats://localhost:4222, -topic, kmg-topic-1, -size, 10, -writers, 5, -readers, 5, -seconds, 60]
2021-02-27 21:00:23 INFO sbk.applicationName: sbk
2021-02-27 21:00:23 INFO sbk.className: 
2021-02-27 21:00:23 INFO Reflections took 76 ms to scan 34 urls, producing 52 keys and 172 values 
2021-02-27 21:00:23 INFO Available Drivers : 33
2021-02-27 21:00:23 INFO Arguments to Driver 'Nats' : [-uri, nats://localhost:4222, -topic, kmg-topic-1, -size, 10, -writers, 5, -readers, 5, -seconds, 60]
2021-02-27 21:00:23 INFO Time Unit: MILLISECONDS
2021-02-27 21:00:23 INFO Minimum Latency: 0 ms
2021-02-27 21:00:23 INFO Maximum Latency: 180000 ms
2021-02-27 21:00:23 INFO Window Latency Store: Array
2021-02-27 21:00:23 INFO Total Window Latency Store: HashMap
2021-02-27 21:00:24 INFO PrometheusLogger Started
Nats Write_Reading    6806858 records, 1361099.4 records/sec,    12.98 MB/sec,   1938.7 ms avg latency,    3250 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   1026 ms 10th,    1272 ms 25th,    1899 ms 50th,    2614 ms 75th,    2989 ms 90th,    3108 ms 95th,    3203 ms 99th,    3233 ms 99.9th,    3249 ms 99.99th.
Nats Write_Reading   10621733 records, 2123921.8 records/sec,    20.26 MB/sec,   4867.5 ms avg latency,    6569 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3621 ms 10th,    4083 ms 25th,    4844 ms 50th,    5677 ms 75th,    6158 ms 90th,    6323 ms 95th,    6471 ms 99th,    6551 ms 99.9th,    6566 ms 99.99th.
Nats Write_Reading   10439760 records, 2087534.5 records/sec,    19.91 MB/sec,   6961.7 ms avg latency,    7728 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   6653 ms 10th,    6792 ms 25th,    6964 ms 50th,    7124 ms 75th,    7253 ms 90th,    7331 ms 95th,    7489 ms 99th,    7687 ms 99.9th,    7724 ms 99.99th.
Nats Write_Reading   10078178 records, 2015232.6 records/sec,    19.22 MB/sec,   7366.4 ms avg latency,    8214 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   7045 ms 10th,    7200 ms 25th,    7355 ms 50th,    7523 ms 75th,    7693 ms 90th,    7808 ms 95th,    8011 ms 99th,    8178 ms 99.9th,    8209 ms 99.99th.
Nats Write_Reading   10297393 records, 2058655.1 records/sec,    19.63 MB/sec,   7568.2 ms avg latency,    8429 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   7237 ms 10th,    7371 ms 25th,    7543 ms 50th,    7752 ms 75th,    7941 ms 90th,    8029 ms 95th,    8197 ms 99th,    8399 ms 99.9th,    8418 ms 99.99th.
Nats Write_Reading   10731608 records, 2145892.4 records/sec,    20.46 MB/sec,   7418.0 ms avg latency,    8268 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   7115 ms 10th,    7243 ms 25th,    7409 ms 50th,    7576 ms 75th,    7738 ms 90th,    7833 ms 95th,    8066 ms 99th,    8203 ms 99.9th,    8252 ms 99.99th.
Nats Write_Reading    9901647 records, 1979933.4 records/sec,    18.88 MB/sec,   7517.6 ms avg latency,    8346 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   7152 ms 10th,    7298 ms 25th,    7496 ms 50th,    7742 ms 75th,    7920 ms 90th,    8010 ms 95th,    8130 ms 99th,    8302 ms 99.9th,    8339 ms 99.99th.
Nats Write_Reading   11158703 records, 2231294.3 records/sec,    21.28 MB/sec,   7374.7 ms avg latency,    8071 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   6984 ms 10th,    7204 ms 25th,    7401 ms 50th,    7572 ms 75th,    7714 ms 90th,    7802 ms 95th,    7955 ms 99th,    8029 ms 99.9th,    8060 ms 99.99th.
Nats Write_Reading   11659356 records, 2331404.9 records/sec,    22.23 MB/sec,   6782.4 ms avg latency,    7776 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   6466 ms 10th,    6613 ms 25th,    6760 ms 50th,    6949 ms 75th,    7109 ms 90th,    7207 ms 95th,    7558 ms 99th,    7743 ms 99.9th,    7771 ms 99.99th.
Nats Write_Reading   11549096 records, 2309357.3 records/sec,    22.02 MB/sec,   6590.4 ms avg latency,    7078 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   6359 ms 10th,    6479 ms 25th,    6589 ms 50th,    6713 ms 75th,    6824 ms 90th,    6885 ms 95th,    6983 ms 99th,    7050 ms 99.9th,    7067 ms 99.99th.
Nats Write_Reading   11893478 records, 2378220.0 records/sec,    22.68 MB/sec,   6646.0 ms avg latency,    7245 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   6410 ms 10th,    6522 ms 25th,    6651 ms 50th,    6769 ms 75th,    6878 ms 90th,    6946 ms 95th,    7097 ms 99th,    7204 ms 99.9th,    7236 ms 99.99th.
Nats Write_Reading   11611321 records, 2327851.0 records/sec,    22.20 MB/sec,   6519.9 ms avg latency,    7138 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   6210 ms 10th,    6359 ms 25th,    6531 ms 50th,    6683 ms 75th,    6806 ms 90th,    6876 ms 95th,    6999 ms 99th,    7070 ms 99.9th,    7115 ms 99.99th.
Total : Nats Write_Reading  126749131 records, 2112485.5 records/sec,    20.15 MB/sec,   6595.9 ms avg latency,    8429 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   5001 ms 10th,    6518 ms 25th,    6891 ms 50th,    7369 ms 75th,    7636 ms 90th,    7785 ms 95th,    8018 ms 99th,    8217 ms 99.9th,    8397 ms 99.99th.
2021-02-27 21:01:24 INFO SBK Performance Shutdown
Unable to stop reader thread
2021-02-27 21:01:26 INFO PrometheusLogger Stopped
2021-02-27 21:01:27 INFO SBK Benchmark Shutdown

```
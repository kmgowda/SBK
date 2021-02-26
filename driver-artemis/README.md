<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# ActiveMQ Artemis Driver for SBK
The Artemis driver for SBK supports multi writers , readers and End to End latency performance benchmarking.

## ActiveMQ Artemis standalone server installation 
Refer to this page : https://hub.docker.com/r/vromero/activemq-artemis to get the docker image of Artemis Server
or simple just execute the below command
```
docker run -p 61616:61616 -p 8161:8161 -e ARTEMIS_USERNAME=admin -e ARTEMIS_PASSWORD=admin -ti vromero/activemq-artemis
```
you can log in to Artemis console at : http://localhost:8161 with username : admin (ARTEMIS_USERNAME), and password: admin (ARTEMIS_PASSWORD)

user name and passwords are very important create the authenticated sessions.

you need to have Dockers installed on your system.
An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class artemis -uri tcp://localhost:61616   -topic kmg-topic-3 -size 100 -writers 1 -readers 1   -seconds 120
```

The sample output is as follows
```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class artemis -uri tcp://localhost:61616   -topic kmg-topic-3 -size 100 -writers 1 -readers 1   -seconds 120
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-02-26 20:55:38 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-02-26 20:55:38 INFO Java Runtime Version: 11.0.8+11
2021-02-26 20:55:38 INFO SBK Version: 0.861
2021-02-26 20:55:38 INFO Arguments List: [-class, artemis, -uri, tcp://localhost:61616, -topic, kmg-topic-3, -size, 100, -writers, 1, -readers, 1, -seconds, 120]
2021-02-26 20:55:38 INFO sbk.applicationName: sbk
2021-02-26 20:55:38 INFO sbk.className: 
2021-02-26 20:55:39 INFO Reflections took 71 ms to scan 34 urls, producing 52 keys and 172 values 
2021-02-26 20:55:39 INFO Available Drivers : 33
2021-02-26 20:55:39 INFO Arguments to Driver 'Artemis' : [-uri, tcp://localhost:61616, -topic, kmg-topic-3, -size, 100, -writers, 1, -readers, 1, -seconds, 120]
2021-02-26 20:55:39 INFO Time Unit: MILLISECONDS
2021-02-26 20:55:39 INFO Minimum Latency: 0 ms
2021-02-26 20:55:39 INFO Maximum Latency: 180000 ms
2021-02-26 20:55:39 INFO Window Latency Store: Array
2021-02-26 20:55:39 INFO Total Window Latency Store: HashMap
2021-02-26 20:55:39 INFO PrometheusLogger Started
Artemis Write_Reading       2521 records,     504.0 records/sec,     0.05 MB/sec,      2.0 ms avg latency,      25 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       2 ms 50th,       2 ms 75th,       3 ms 90th,       4 ms 95th,       6 ms 99th,      11 ms 99.9th,      25 ms 99.99th.
Artemis Write_Reading       3143 records,     628.3 records/sec,     0.06 MB/sec,      1.6 ms avg latency,     108 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       3 ms 99th,      25 ms 99.9th,     108 ms 99.99th.
Artemis Write_Reading       2542 records,     508.3 records/sec,     0.05 MB/sec,      2.0 ms avg latency,     944 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,      26 ms 99.9th,     944 ms 99.99th.
Artemis Write_Reading       3525 records,     704.7 records/sec,     0.07 MB/sec,      1.4 ms avg latency,     197 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,     197 ms 99.99th.
Artemis Write_Reading       3555 records,     710.7 records/sec,     0.07 MB/sec,      1.4 ms avg latency,       9 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       6 ms 99.9th,       9 ms 99.99th.
Artemis Write_Reading       3607 records,     721.3 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      32 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,      32 ms 99.99th.
Artemis Write_Reading       3497 records,     699.3 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      58 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,      23 ms 99.9th,      58 ms 99.99th.
Artemis Write_Reading       3390 records,     677.9 records/sec,     0.06 MB/sec,      1.5 ms avg latency,     202 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,      26 ms 99.9th,     202 ms 99.99th.
Artemis Write_Reading       3616 records,     723.1 records/sec,     0.07 MB/sec,      1.4 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       7 ms 99.99th.
Artemis Write_Reading       3541 records,     707.9 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      28 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,      28 ms 99.99th.
Artemis Write_Reading       3502 records,     700.3 records/sec,     0.07 MB/sec,      1.4 ms avg latency,     159 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,     159 ms 99.99th.
Artemis Write_Reading       3519 records,     703.7 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      65 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,      65 ms 99.99th.
Artemis Write_Reading       2927 records,     585.2 records/sec,     0.06 MB/sec,      1.7 ms avg latency,    1005 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       6 ms 99.9th,    1005 ms 99.99th.
Artemis Write_Reading       3618 records,     723.3 records/sec,     0.07 MB/sec,      1.4 ms avg latency,       8 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       8 ms 99.99th.
Artemis Write_Reading       3566 records,     713.1 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      29 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,      29 ms 99.99th.
Artemis Write_Reading       3783 records,     756.3 records/sec,     0.07 MB/sec,      1.3 ms avg latency,       6 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       6 ms 99.99th.
Artemis Write_Reading       3630 records,     725.7 records/sec,     0.07 MB/sec,      1.4 ms avg latency,       6 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       6 ms 99.99th.
Artemis Write_Reading       3444 records,     688.7 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      31 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       6 ms 99.9th,      31 ms 99.99th.
Artemis Write_Reading       3710 records,     741.7 records/sec,     0.07 MB/sec,      1.3 ms avg latency,       9 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,       9 ms 99.99th.
Artemis Write_Reading       3326 records,     665.1 records/sec,     0.06 MB/sec,      1.5 ms avg latency,     120 ms max latency;        1 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       3 ms 99th,      23 ms 99.9th,     120 ms 99.99th.
Artemis Write_Reading       2145 records,     428.9 records/sec,     0.04 MB/sec,      2.3 ms avg latency,    1009 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       6 ms 99.9th,    1009 ms 99.99th.
Artemis Write_Reading       3617 records,     723.3 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      44 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,      44 ms 99.99th.
Artemis Write_Reading       3670 records,     733.9 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      10 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,      10 ms 99.99th.
Artemis Write_Reading       3659 records,     736.4 records/sec,     0.07 MB/sec,      1.4 ms avg latency,      33 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       5 ms 99.9th,      33 ms 99.99th.
Total : Artemis Write_Reading      81053 records,     675.4 records/sec,     0.06 MB/sec,      1.5 ms avg latency,    1009 ms max latency;        1 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       2 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       7 ms 99.9th,     120 ms 99.99th.
2021-02-26 20:57:46 INFO SBK Performance Shutdown
2021-02-26 20:57:46 INFO PrometheusLogger Stopped
2021-02-26 20:57:47 INFO SBK Benchmark Shutdown

```
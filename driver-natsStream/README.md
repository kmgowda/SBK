<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# NATS Streaming Driver for SBK
The NATS driver for SBK supports writers , readers benchmarking along with End to End Latency benchmarking.

## NATS standalone Streaming server installation 
Refer to this page : https://hub.docker.com/_/nats-streaming to get the NATS streaming docker image.
or simple just execute the below command
```
docker run -p 4222:4222 -ti nats-streaming:latest
```
you need to have Dockers installed on your system.
An example, SBK benchmarkig command is
```
./build/install/sbk/bin/sbk -class Natsstream -uri nats://localhost:4222  -cluster test-cluster -topic kmg-topic-2 -size 100 -writers 1 -readers 1   -seconds 60
```

sample output is as follows:

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class Natsstream -uri nats://localhost:4222  -cluster test-cluster -topic kmg-topic-2 -size 100 -writers 1 -readers 1   -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-02-27 21:04:54 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-02-27 21:04:54 INFO Java Runtime Version: 11.0.8+11
2021-02-27 21:04:54 INFO SBK Version: 0.861
2021-02-27 21:04:54 INFO Arguments List: [-class, Natsstream, -uri, nats://localhost:4222, -cluster, test-cluster, -topic, kmg-topic-2, -size, 100, -writers, 1, -readers, 1, -seconds, 60]
2021-02-27 21:04:54 INFO sbk.applicationName: sbk
2021-02-27 21:04:54 INFO sbk.className: 
2021-02-27 21:04:54 INFO Reflections took 69 ms to scan 34 urls, producing 52 keys and 172 values 
2021-02-27 21:04:54 INFO Available Drivers : 33
2021-02-27 21:04:54 INFO Arguments to Driver 'NatsStream' : [-uri, nats://localhost:4222, -cluster, test-cluster, -topic, kmg-topic-2, -size, 100, -writers, 1, -readers, 1, -seconds, 60]
2021-02-27 21:04:54 INFO Time Unit: MILLISECONDS
2021-02-27 21:04:54 INFO Minimum Latency: 0 ms
2021-02-27 21:04:54 INFO Maximum Latency: 180000 ms
2021-02-27 21:04:54 INFO Window Latency Store: Array
2021-02-27 21:04:54 INFO Total Window Latency Store: HashMap
2021-02-27 21:04:55 INFO PrometheusLogger Started
NatsStream Write_Reading       5049 records,    1009.4 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      43 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       4 ms 99.9th,      43 ms 99.99th.
NatsStream Write_Reading       5330 records,    1065.8 records/sec,     0.10 MB/sec,      0.9 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       5 ms 99.99th.
NatsStream Write_Reading       5238 records,    1047.4 records/sec,     0.10 MB/sec,      0.9 ms avg latency,      14 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,      14 ms 99.99th.
NatsStream Write_Reading       5231 records,    1044.7 records/sec,     0.10 MB/sec,      0.9 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,       3 ms 99.99th.
NatsStream Write_Reading       4980 records,     995.8 records/sec,     0.09 MB/sec,      1.0 ms avg latency,      16 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,       2 ms 99.9th,      16 ms 99.99th.
NatsStream Write_Reading       4635 records,     926.8 records/sec,     0.09 MB/sec,      1.1 ms avg latency,     211 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,     211 ms 99.99th.
NatsStream Write_Reading       4478 records,     895.1 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       3 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 90th,       2 ms 95th,       2 ms 99th,       3 ms 99.9th,       3 ms 99.99th.
NatsStream Write_Reading       5111 records,    1021.6 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       7 ms 99.99th.
NatsStream Write_Reading       5321 records,    1064.0 records/sec,     0.10 MB/sec,      0.9 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       7 ms 99.99th.
NatsStream Write_Reading       4164 records,     832.3 records/sec,     0.08 MB/sec,      1.2 ms avg latency,     468 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       2 ms 95th,       2 ms 99th,       9 ms 99.9th,     468 ms 99.99th.
NatsStream Write_Reading       5162 records,    1032.0 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       6 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,       6 ms 99.99th.
NatsStream Write_Reading       5142 records,    1033.2 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      10 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,      10 ms 99.99th.
Total : NatsStream Write_Reading      59841 records,     997.3 records/sec,     0.10 MB/sec,      1.0 ms avg latency,     468 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 90th,       1 ms 95th,       2 ms 99th,       3 ms 99.9th,      40 ms 99.99th.
2021-02-27 21:05:55 INFO SBK Performance Shutdown
Unable to stop reader thread
2021-02-27 21:05:56 INFO PrometheusLogger Stopped
2021-02-27 21:05:57 INFO SBK Benchmark Shutdown

```
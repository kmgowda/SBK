<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# OpenIO Driver for SBK
The OpenIO S3 driver extends the MinIO SBK driver for S3 compliant performance benchmarking.
This S3 Driver for SBK supports the multiple writers, readers performance benchmarking.
The End to End latency benchmarking is not supported.

## OpenIO Performance benchmarking with SBK and OpenIO docker images
See this page :   https://hub.docker.com/r/openio/sds/ to fetch OpenIO docker images.
Use SBK to do writers and readers performance benchmarking. user the option **-url**  to supply the ip address and port details of the OpenIO Server address.

docker command to pull and run the openio docker image
```
docker run openio/sds:latest
```

you can run the below command to see the writer benchmarking uploading the objects in to openIO Server
```
./build/install/sbk/bin/sbk -class openio -url http://127.0.0.1:6007  -writers 1 -size 100 -seconds 60
```
The above command creates the bucket named `sbk` with objects prefixed with `sbk-`
the default key is "demo:demo" and password is "DEMO_PASS"

sample output:
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class openio -url http://127.0.0.1:6007  -writers 1 -size 100 -seconds 60 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-06-04 12:38:16 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-06-04 12:38:16 INFO Java Runtime Version: 11.0.8+11
2021-06-04 12:38:16 INFO SBK Version: 0.89
2021-06-04 12:38:16 INFO Arguments List: [-class, openio, -url, http://127.0.0.1:6007, -writers, 1, -size, 100, -seconds, 60]
2021-06-04 12:38:16 INFO sbk.applicationName: sbk
2021-06-04 12:38:16 INFO sbk.className: sbk
2021-06-04 12:38:16 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2021-06-04 12:38:16 INFO Reflections took 81 ms to scan 40 urls, producing 90 keys and 229 values 
2021-06-04 12:38:16 INFO Available Drivers : 39
2021-06-04 12:38:16 INFO Arguments to Driver 'OpenIO' : [-url, http://127.0.0.1:6007, -writers, 1, -size, 100, -seconds, 60]
2021-06-04 12:38:16 INFO Time Unit: MILLISECONDS
2021-06-04 12:38:16 INFO Minimum Latency: 0 ms
2021-06-04 12:38:16 INFO Maximum Latency: 180000 ms
2021-06-04 12:38:16 INFO Window Latency Store: Array
2021-06-04 12:38:16 INFO Total Window Latency Store: HashMap
2021-06-04 12:38:16 INFO SBK Benchmark Started
2021-06-04 12:38:16 INFO SBK PrometheusLogger Started
2021-06-04 12:38:17 INFO Bucket 'sbk' already exists.
2021-06-04 12:38:17 INFO Performance Logger Started
2021-06-04 12:38:17 INFO SBK Benchmark initiated Writers
2021-06-04 12:38:17 INFO Writer 0 started , run seconds: 60
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         267 records,      53.2 records/sec,     0.01 MB/sec,     18.7 ms avg latency,      31 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     16 ms 10th,      17 ms 25th,      18 ms 50th,      20 ms 75th,      22 ms 90th,      23 ms 95th,      29 ms 99th,      31 ms 99.9th,      31 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         282 records,      56.4 records/sec,     0.01 MB/sec,     17.7 ms avg latency,      25 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     16 ms 10th,      16 ms 25th,      17 ms 50th,      18 ms 75th,      21 ms 90th,      22 ms 95th,      24 ms 99th,      25 ms 99.9th,      25 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         286 records,      57.2 records/sec,     0.01 MB/sec,     17.5 ms avg latency,      40 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     16 ms 10th,      16 ms 25th,      17 ms 50th,      18 ms 75th,      20 ms 90th,      21 ms 95th,      24 ms 99th,      40 ms 99.9th,      40 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         175 records,      35.0 records/sec,     0.00 MB/sec,     28.6 ms avg latency,      58 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     19 ms 10th,      20 ms 25th,      25 ms 50th,      36 ms 75th,      42 ms 90th,      47 ms 95th,      56 ms 99th,      58 ms 99.9th,      58 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         252 records,      50.2 records/sec,     0.00 MB/sec,     19.9 ms avg latency,      39 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      19 ms 50th,      21 ms 75th,      24 ms 90th,      28 ms 95th,      38 ms 99th,      39 ms 99.9th,      39 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         294 records,      58.6 records/sec,     0.01 MB/sec,     17.0 ms avg latency,      36 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     15 ms 10th,      16 ms 25th,      17 ms 50th,      17 ms 75th,      19 ms 90th,      21 ms 95th,      24 ms 99th,      36 ms 99.9th,      36 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         284 records,      56.7 records/sec,     0.01 MB/sec,     17.6 ms avg latency,      42 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     15 ms 10th,      16 ms 25th,      17 ms 50th,      18 ms 75th,      20 ms 90th,      22 ms 95th,      33 ms 99th,      42 ms 99.9th,      42 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         272 records,      54.2 records/sec,     0.01 MB/sec,     18.4 ms avg latency,      34 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     15 ms 10th,      16 ms 25th,      17 ms 50th,      20 ms 75th,      23 ms 90th,      25 ms 95th,      31 ms 99th,      34 ms 99.9th,      34 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         200 records,      39.9 records/sec,     0.00 MB/sec,     25.0 ms avg latency,     125 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     15 ms 10th,      16 ms 25th,      18 ms 50th,      21 ms 75th,      55 ms 90th,      75 ms 95th,      97 ms 99th,     125 ms 99.9th,     125 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         213 records,      42.5 records/sec,     0.00 MB/sec,     23.5 ms avg latency,      85 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     15 ms 10th,      16 ms 25th,      18 ms 50th,      21 ms 75th,      45 ms 90th,      69 ms 95th,      78 ms 99th,      85 ms 99.9th,      85 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         301 records,      60.2 records/sec,     0.01 MB/sec,     16.6 ms avg latency,      33 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     15 ms 10th,      16 ms 25th,      16 ms 50th,      17 ms 75th,      19 ms 90th,      20 ms 95th,      25 ms 99th,      33 ms 99.9th,      33 ms 99.99th.
OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         281 records,      57.4 records/sec,     0.01 MB/sec,     17.4 ms avg latency,      36 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     15 ms 10th,      16 ms 25th,      16 ms 50th,      18 ms 75th,      20 ms 90th,      23 ms 95th,      28 ms 99th,      36 ms 99.9th,      36 ms 99.99th.
Total : OpenIO Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        3107 records,      51.8 records/sec,     0.00 MB/sec,     19.3 ms avg latency,     125 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     15 ms 10th,      16 ms 25th,      17 ms 50th,      19 ms 75th,      23 ms 90th,      30 ms 95th,      63 ms 99th,      85 ms 99.9th,     125 ms 99.99th.
2021-06-04 12:39:17 INFO Performance Logger Shutdown
2021-06-04 12:39:17 INFO Writer 0 exited
2021-06-04 12:39:17 INFO SBK PrometheusLogger Shutdown
2021-06-04 12:39:18 INFO SBK Benchmark Shutdown



```



For read performance benchmarking, you run the below command.
```
./build/install/sbk/bin/sbk -class openio -url http://127.0.0.1:6007  -readers 1 -size 100 -seconds 60
```

sample output
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class openio -url http://127.0.0.1:6007  -readers 1 -size 100 -seconds 60
\SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-06-04 12:40:05 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-06-04 12:40:05 INFO Java Runtime Version: 11.0.8+11
2021-06-04 12:40:05 INFO SBK Version: 0.89
2021-06-04 12:40:05 INFO Arguments List: [-class, openio, -url, http://127.0.0.1:6007, -readers, 1, -size, 100, -seconds, 60]
2021-06-04 12:40:05 INFO sbk.applicationName: sbk
2021-06-04 12:40:05 INFO sbk.className: sbk
2021-06-04 12:40:05 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2021-06-04 12:40:06 INFO Reflections took 68 ms to scan 40 urls, producing 90 keys and 229 values 
2021-06-04 12:40:06 INFO Available Drivers : 39
2021-06-04 12:40:06 INFO Arguments to Driver 'OpenIO' : [-url, http://127.0.0.1:6007, -readers, 1, -size, 100, -seconds, 60]
2021-06-04 12:40:06 INFO Time Unit: MILLISECONDS
2021-06-04 12:40:06 INFO Minimum Latency: 0 ms
2021-06-04 12:40:06 INFO Maximum Latency: 180000 ms
2021-06-04 12:40:06 INFO Window Latency Store: Array
2021-06-04 12:40:06 INFO Total Window Latency Store: HashMap
2021-06-04 12:40:06 INFO SBK Benchmark Started
2021-06-04 12:40:06 INFO SBK PrometheusLogger Started
2021-06-04 12:40:06 INFO Bucket 'sbk' already exists.
2021-06-04 12:40:06 INFO Performance Logger Started
2021-06-04 12:40:06 INFO SBK Benchmark initiated Readers
2021-06-04 12:40:06 INFO Reader 0 started , run seconds: 60
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         252 records,      50.3 records/sec,     0.00 MB/sec,     18.9 ms avg latency,      30 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      18 ms 25th,      19 ms 50th,      20 ms 75th,      21 ms 90th,      21 ms 95th,      27 ms 99th,      30 ms 99.9th,      30 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         276 records,      55.2 records/sec,     0.01 MB/sec,     18.1 ms avg latency,      23 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      21 ms 95th,      22 ms 99th,      23 ms 99.9th,      23 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         272 records,      54.3 records/sec,     0.01 MB/sec,     18.4 ms avg latency,      47 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      21 ms 95th,      30 ms 99th,      47 ms 99.9th,      47 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         269 records,      53.7 records/sec,     0.01 MB/sec,     17.9 ms avg latency,      32 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     16 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      20 ms 95th,      22 ms 99th,      32 ms 99.9th,      32 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         279 records,      55.6 records/sec,     0.01 MB/sec,     17.9 ms avg latency,      23 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      20 ms 95th,      21 ms 99th,      23 ms 99.9th,      23 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         276 records,      55.1 records/sec,     0.01 MB/sec,     18.1 ms avg latency,      33 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     16 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      21 ms 95th,      25 ms 99th,      33 ms 99.9th,      33 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         276 records,      55.0 records/sec,     0.01 MB/sec,     18.1 ms avg latency,      31 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     16 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      21 ms 95th,      23 ms 99th,      31 ms 99.9th,      31 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         266 records,      53.1 records/sec,     0.01 MB/sec,     18.2 ms avg latency,      28 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      20 ms 95th,      22 ms 99th,      28 ms 99.9th,      28 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         275 records,      55.0 records/sec,     0.01 MB/sec,     18.2 ms avg latency,      44 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     16 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      20 ms 95th,      25 ms 99th,      44 ms 99.9th,      44 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         276 records,      55.1 records/sec,     0.01 MB/sec,     18.1 ms avg latency,      30 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      20 ms 95th,      22 ms 99th,      30 ms 99.9th,      30 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         274 records,      54.7 records/sec,     0.01 MB/sec,     18.3 ms avg latency,      53 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      20 ms 95th,      24 ms 99th,      53 ms 99.9th,      53 ms 99.99th.
OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         247 records,      50.2 records/sec,     0.00 MB/sec,     19.2 ms avg latency,      37 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      18 ms 50th,      20 ms 75th,      23 ms 90th,      24 ms 95th,      34 ms 99th,      37 ms 99.9th,      37 ms 99.99th.
Total : OpenIO Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        3238 records,      54.0 records/sec,     0.01 MB/sec,     18.3 ms avg latency,      53 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;     17 ms 10th,      17 ms 25th,      18 ms 50th,      19 ms 75th,      20 ms 90th,      21 ms 95th,      24 ms 99th,      37 ms 99.9th,      53 ms 99.99th.
2021-06-04 12:41:06 INFO Performance Logger Shutdown
2021-06-04 12:41:07 INFO SBK PrometheusLogger Shutdown
2021-06-04 12:41:08 INFO SBK Benchmark Shutdown


```
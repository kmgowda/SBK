<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Ceph S3 compliant Driver for SBK
The Ceph S3 driver extends the MinIO SBK driver for S3 compliant performance benchmarking.
This S3 Driver for SBK supports the multiple writers, readers performance benchmarking.
The End to End latency benchmarking is not supported.

## Ceph S3 Performance benchmarking with SBK and Ceph S3 docker images
See this page : https://github.com/ceph/cn to create the Ceph S3 Cluster and volumes with docker images.
Use SBK to do writers and readers performance benchmarking. user the option **-url**  to supply the ip address and
port details of the Ceph Server address. 

you can run the below command to see the writer benchmarking uploading the objects into Ceph S3 Server
```
/build/install/sbk/bin/sbk -class cephs3 -key 6ER3X9SZAV6PJ9B8AZNV -secret 6zPa5QN8qQBFYBYsrGIvUzjWQbaLhLg1N9VBo49t  -writers 1 -size 100 -seconds 60
```
The above command creates the bucket named `sbk` with objects prefixed with `sbk-`

sample output:
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class cephs3 -key 6ER3X9SZAV6PJ9B8AZNV -secret 6zPa5QN8qQBFYBYsrGIvUzjWQbaLhLg1N9VBo49t  -writers 1 -size 100 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-05-31 18:34:09 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-05-31 18:34:09 INFO Java Runtime Version: 11.0.8+11
2021-05-31 18:34:09 INFO SBK Version: 0.89
2021-05-31 18:34:09 INFO Arguments List: [-class, cephs3, -key, 6ER3X9SZAV6PJ9B8AZNV, -secret, 6zPa5QN8qQBFYBYsrGIvUzjWQbaLhLg1N9VBo49t, -writers, 1, -size, 100, -seconds, 60]
2021-05-31 18:34:09 INFO sbk.applicationName: sbk
2021-05-31 18:34:09 INFO sbk.className: sbk
2021-05-31 18:34:10 INFO Reflections took 63 ms to scan 39 urls, producing 90 keys and 230 values 
2021-05-31 18:34:10 INFO Available Drivers : 38
2021-05-31 18:34:10 INFO Arguments to Driver 'CephS3' : [-key, 6ER3X9SZAV6PJ9B8AZNV, -secret, 6zPa5QN8qQBFYBYsrGIvUzjWQbaLhLg1N9VBo49t, -writers, 1, -size, 100, -seconds, 60]
2021-05-31 18:34:10 INFO Time Unit: MILLISECONDS
2021-05-31 18:34:10 INFO Minimum Latency: 0 ms
2021-05-31 18:34:10 INFO Maximum Latency: 180000 ms
2021-05-31 18:34:10 INFO Window Latency Store: Array
2021-05-31 18:34:10 INFO Total Window Latency Store: HashMap
2021-05-31 18:34:10 INFO SBK Benchmark Started
2021-05-31 18:34:10 INFO SBK PrometheusLogger Started
2021-05-31 18:34:11 INFO Bucket 'sbk' does not exist.
2021-05-31 18:34:11 INFO Creating the Bucket: sbk
2021-05-31 18:34:15 INFO Performance Logger Started
2021-05-31 18:34:15 INFO SBK Benchmark initiated Writers
2021-05-31 18:34:15 INFO Writer 0 started , run seconds: 60
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         286 records,      57.2 records/sec,     0.01 MB/sec,     17.4 ms avg latency,    2014 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      11 ms 75th,      12 ms 90th,      13 ms 95th,      17 ms 99th,    2014 ms 99.9th,    2014 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         508 records,     101.4 records/sec,     0.01 MB/sec,      9.9 ms avg latency,      14 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      10 ms 75th,      11 ms 90th,      11 ms 95th,      12 ms 99th,      14 ms 99.9th,      14 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         510 records,     101.8 records/sec,     0.01 MB/sec,      9.8 ms avg latency,      21 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      10 ms 75th,      11 ms 90th,      11 ms 95th,      13 ms 99th,      21 ms 99.9th,      21 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         507 records,     101.3 records/sec,     0.01 MB/sec,      9.9 ms avg latency,      20 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      10 ms 75th,      11 ms 90th,      12 ms 95th,      16 ms 99th,      20 ms 99.9th,      20 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         358 records,      62.8 records/sec,     0.01 MB/sec,     11.1 ms avg latency,      35 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      11 ms 75th,      15 ms 90th,      18 ms 95th,      29 ms 99th,      35 ms 99.9th,      35 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         105 records,      19.9 records/sec,     0.00 MB/sec,     66.9 ms avg latency,    2993 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      10 ms 75th,      12 ms 90th,      14 ms 95th,    2992 ms 99th,    2993 ms 99.9th,    2993 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         494 records,      98.7 records/sec,     0.01 MB/sec,     10.1 ms avg latency,      80 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      10 ms 75th,      12 ms 90th,      13 ms 95th,      17 ms 99th,      80 ms 99.9th,      80 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         290 records,      47.2 records/sec,     0.00 MB/sec,     21.2 ms avg latency,    2923 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      11 ms 75th,      15 ms 90th,      17 ms 95th,      27 ms 99th,    2923 ms 99.9th,    2923 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         482 records,      96.2 records/sec,     0.01 MB/sec,     10.4 ms avg latency,      33 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      10 ms 75th,      12 ms 90th,      14 ms 95th,      28 ms 99th,      33 ms 99.9th,      33 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         350 records,      70.0 records/sec,     0.01 MB/sec,     14.3 ms avg latency,      49 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      11 ms 50th,      15 ms 75th,      30 ms 90th,      35 ms 95th,      43 ms 99th,      49 ms 99.9th,      49 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         493 records,      98.4 records/sec,     0.01 MB/sec,     10.2 ms avg latency,      26 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      11 ms 75th,      12 ms 90th,      14 ms 95th,      20 ms 99th,      26 ms 99.9th,      26 ms 99.99th.
CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,         191 records,      67.8 records/sec,     0.01 MB/sec,     14.7 ms avg latency,      46 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,      10 ms 25th,      12 ms 50th,      17 ms 75th,      25 ms 90th,      29 ms 95th,      46 ms 99th,      46 ms 99.9th,      46 ms 99.99th.
Total : CephS3 Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        4574 records,      76.2 records/sec,     0.01 MB/sec,     13.1 ms avg latency,    2993 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      9 ms 10th,       9 ms 25th,      10 ms 50th,      11 ms 75th,      13 ms 90th,      17 ms 95th,      32 ms 99th,      80 ms 99.9th,    2993 ms 99.99th.
2021-05-31 18:35:15 INFO Performance Logger Shutdown
2021-05-31 18:35:15 INFO Writer 0 exited
2021-05-31 18:35:15 INFO SBK PrometheusLogger Shutdown
2021-05-31 18:35:16 INFO SBK Benchmark Shutdown

```



For read performance benchmarking, you run the below command.
```
/build/install/sbk/bin/sbk -class cephs3 -key 6ER3X9SZAV6PJ9B8AZNV -secret 6zPa5QN8qQBFYBYsrGIvUzjWQbaLhLg1N9VBo49t  -readers 1 -size 100 -seconds 60
```

sample output
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class cephs3 -key 6ER3X9SZAV6PJ9B8AZNV -secret 6zPa5QN8qQBFYBYsrGIvUzjWQbaLhLg1N9VBo49t  -readers 1 -size 100 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-05-31 18:35:56 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-05-31 18:35:56 INFO Java Runtime Version: 11.0.8+11
2021-05-31 18:35:56 INFO SBK Version: 0.89
2021-05-31 18:35:56 INFO Arguments List: [-class, cephs3, -key, 6ER3X9SZAV6PJ9B8AZNV, -secret, 6zPa5QN8qQBFYBYsrGIvUzjWQbaLhLg1N9VBo49t, -readers, 1, -size, 100, -seconds, 60]
2021-05-31 18:35:56 INFO sbk.applicationName: sbk
2021-05-31 18:35:56 INFO sbk.className: sbk
2021-05-31 18:35:56 INFO Reflections took 74 ms to scan 39 urls, producing 90 keys and 230 values 
2021-05-31 18:35:56 INFO Available Drivers : 38
2021-05-31 18:35:56 INFO Arguments to Driver 'CephS3' : [-key, 6ER3X9SZAV6PJ9B8AZNV, -secret, 6zPa5QN8qQBFYBYsrGIvUzjWQbaLhLg1N9VBo49t, -readers, 1, -size, 100, -seconds, 60]
2021-05-31 18:35:56 INFO Time Unit: MILLISECONDS
2021-05-31 18:35:56 INFO Minimum Latency: 0 ms
2021-05-31 18:35:56 INFO Maximum Latency: 180000 ms
2021-05-31 18:35:56 INFO Window Latency Store: Array
2021-05-31 18:35:56 INFO Total Window Latency Store: HashMap
2021-05-31 18:35:56 INFO SBK Benchmark Started
2021-05-31 18:35:56 INFO SBK PrometheusLogger Started
2021-05-31 18:35:58 INFO Bucket 'sbk' already exists.
2021-05-31 18:35:58 INFO Performance Logger Started
2021-05-31 18:35:58 INFO SBK Benchmark initiated Readers
2021-05-31 18:35:58 INFO Reader 0 started , run seconds: 60
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         162 records,      32.2 records/sec,     0.00 MB/sec,      9.7 ms avg latency,      14 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       8 ms 25th,       9 ms 50th,      12 ms 75th,      13 ms 90th,      13 ms 95th,      14 ms 99th,      14 ms 99.9th,      14 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         167 records,      33.4 records/sec,     0.00 MB/sec,      9.3 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       8 ms 25th,       8 ms 50th,      11 ms 75th,      12 ms 90th,      13 ms 95th,      13 ms 99th,      13 ms 99.9th,      13 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         169 records,      33.5 records/sec,     0.00 MB/sec,      9.0 ms avg latency,      24 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      10 ms 75th,      12 ms 90th,      13 ms 95th,      14 ms 99th,      24 ms 99.9th,      24 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         166 records,      33.1 records/sec,     0.00 MB/sec,      9.5 ms avg latency,     128 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      10 ms 75th,      12 ms 90th,      13 ms 95th,      15 ms 99th,     128 ms 99.9th,     128 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         170 records,      33.8 records/sec,     0.00 MB/sec,      8.9 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      10 ms 75th,      12 ms 90th,      13 ms 95th,      13 ms 99th,      13 ms 99.9th,      13 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         168 records,      33.6 records/sec,     0.00 MB/sec,      8.8 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      11 ms 75th,      12 ms 90th,      13 ms 95th,      13 ms 99th,      13 ms 99.9th,      13 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         171 records,      34.0 records/sec,     0.00 MB/sec,      8.5 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      10 ms 75th,      12 ms 90th,      12 ms 95th,      13 ms 99th,      13 ms 99.9th,      13 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         170 records,      33.9 records/sec,     0.00 MB/sec,      8.7 ms avg latency,      15 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      10 ms 75th,      12 ms 90th,      12 ms 95th,      13 ms 99th,      15 ms 99.9th,      15 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         168 records,      33.6 records/sec,     0.00 MB/sec,      9.0 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      11 ms 75th,      12 ms 90th,      13 ms 95th,      13 ms 99th,      13 ms 99.9th,      13 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         170 records,      33.9 records/sec,     0.00 MB/sec,      8.7 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      10 ms 75th,      12 ms 90th,      12 ms 95th,      13 ms 99th,      13 ms 99.9th,      13 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         170 records,      33.7 records/sec,     0.00 MB/sec,      8.9 ms avg latency,      14 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      11 ms 75th,      12 ms 90th,      13 ms 95th,      13 ms 99th,      14 ms 99.9th,      14 ms 99.99th.
CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,         162 records,      33.8 records/sec,     0.00 MB/sec,      8.7 ms avg latency,      13 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      6 ms 10th,       7 ms 25th,       8 ms 50th,      11 ms 75th,      12 ms 90th,      12 ms 95th,      13 ms 99th,      13 ms 99.9th,      13 ms 99.99th.
Total : CephS3 Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        2013 records,      33.5 records/sec,     0.00 MB/sec,      9.0 ms avg latency,     128 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;      7 ms 10th,       7 ms 25th,       8 ms 50th,      10 ms 75th,      12 ms 90th,      13 ms 95th,      13 ms 99th,      15 ms 99.9th,     128 ms 99.99th.
2021-05-31 18:36:58 INFO Performance Logger Shutdown
2021-05-31 18:36:59 INFO SBK PrometheusLogger Shutdown
2021-05-31 18:37:00 INFO SBK Benchmark Shutdown

```
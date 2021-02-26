<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# RabbitMQ Benchmarking

For local system RabbitMQ benchmarking, install the RabbitMQ on your system.

make sure that you have set the right set of admin privileges

```
# for admin user
sudo rabbitmqctl add_user admin admin

rabbitmqctl change_password admin admin

rabbitmqctl set_user_tags admin administrator

rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```

You can set the previlages for guest user too

```
sudo rabbitmqctl add_user guest guest

rabbitmqctl change_password guest guest

rabbitmqctl set_user_tags guest administrator

rabbitmqctl set_permissions -p / guest ".*" ".*" ".*"
```

To allow remote users to access your server,  add the following content to in the file /etc/rabbitmq/rabbitmq.config

In case , if you have installed the Rabbit MQ in windows system, then to allow remote users to access your server,  add the following content to in the file c:\Users\[your user name]\AppData\Roaming\RabbitMQ\rabbitmq.config or :\Users\[your user name]\AppData\Roaming\RabbitMQ\advanced.config (%APPDATA%\RabbitMQ\rabbit.config or advanced.conifg).

```
[{rabbit, [{loopback_users, []}]}].

```

you can enable the web access 

```
sudo rabbitmq-plugins enable rabbitmq_management
```


After completeting the above steps you can start your local rabbitmq server

```
rabbitmq-server
```

Now, you can access your local RabbitMQ server : http://localhost:15672/#/


## RabbitMQ Docker image
you can run the below command to start the standalone docker image

```
docker run -p 127.0.0.1:5672:5672/tcp --name some-rabbit rabbitmq:3
```

then run the SBK command as follows :
```
./build/install/sbk/bin/sbk -class rabbitmq  -broker 127.0.0.1  -topic kmg-topic-2 -size 100 -writers 1 -readers 1   -seconds 60 -async true
```

The sample output is as follows:

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class rabbitmq  -broker 127.0.0.1  -topic kmg-topic-2 -size 100 -writers 1 -readers 1   -seconds 60 -async true
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-02-27 21:24:40 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-02-27 21:24:40 INFO Java Runtime Version: 11.0.8+11
2021-02-27 21:24:40 INFO SBK Version: 0.861
2021-02-27 21:24:40 INFO Arguments List: [-class, rabbitmq, -broker, 127.0.0.1, -topic, kmg-topic-2, -size, 100, -writers, 1, -readers, 1, -seconds, 60, -async, true]
2021-02-27 21:24:40 INFO sbk.applicationName: sbk
2021-02-27 21:24:40 INFO sbk.className: 
2021-02-27 21:24:40 INFO Reflections took 55 ms to scan 34 urls, producing 52 keys and 172 values 
2021-02-27 21:24:40 INFO Available Drivers : 33
2021-02-27 21:24:40 INFO Arguments to Driver 'RabbitMQ' : [-broker, 127.0.0.1, -topic, kmg-topic-2, -size, 100, -writers, 1, -readers, 1, -seconds, 60, -async, true]
2021-02-27 21:24:40 INFO Time Unit: MILLISECONDS
2021-02-27 21:24:40 INFO Minimum Latency: 0 ms
2021-02-27 21:24:40 INFO Maximum Latency: 180000 ms
2021-02-27 21:24:40 INFO Window Latency Store: Array
2021-02-27 21:24:40 INFO Total Window Latency Store: HashMap
2021-02-27 21:24:40 INFO PrometheusLogger Started
RabbitMQ Write_Reading      86115 records,   17219.6 records/sec,     1.64 MB/sec,   2862.7 ms avg latency,  303472 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,      271 higher;    483 ms 10th,    1207 ms 25th,    1940 ms 50th,    2728 ms 75th,    3194 ms 90th,    3362 ms 95th,    3460 ms 99th,    3484 ms 99.9th,    3485 ms 99.99th.
RabbitMQ Write_Reading     110115 records,   22018.6 records/sec,     2.10 MB/sec,   4644.0 ms avg latency,    5652 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3904 ms 10th,    4382 ms 25th,    4751 ms 50th,    4897 ms 75th,    5176 ms 90th,    5200 ms 95th,    5648 ms 99th,    5652 ms 99.9th,    5652 ms 99.99th.
RabbitMQ Write_Reading     104937 records,   20983.2 records/sec,     2.00 MB/sec,   4337.8 ms avg latency,    4798 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3841 ms 10th,    4224 ms 25th,    4415 ms 50th,    4543 ms 75th,    4645 ms 90th,    4691 ms 95th,    4776 ms 99th,    4797 ms 99.9th,    4798 ms 99.99th.
RabbitMQ Write_Reading     107820 records,   21559.7 records/sec,     2.06 MB/sec,   4955.8 ms avg latency,    5499 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   4590 ms 10th,    4808 ms 25th,    4975 ms 50th,    5118 ms 75th,    5248 ms 90th,    5487 ms 95th,    5495 ms 99th,    5499 ms 99.9th,    5499 ms 99.99th.
RabbitMQ Write_Reading      96917 records,   19379.5 records/sec,     1.85 MB/sec,   5077.5 ms avg latency,    5522 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   4853 ms 10th,    4943 ms 25th,    5061 ms 50th,    5188 ms 75th,    5344 ms 90th,    5406 ms 95th,    5488 ms 99th,    5517 ms 99.9th,    5521 ms 99.99th.
RabbitMQ Write_Reading      95266 records,   19049.4 records/sec,     1.82 MB/sec,   5537.2 ms avg latency,    6362 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   5235 ms 10th,    5356 ms 25th,    5528 ms 50th,    5664 ms 75th,    5926 ms 90th,    5962 ms 95th,    6007 ms 99th,    6362 ms 99.9th,    6362 ms 99.99th.
RabbitMQ Write_Reading      99818 records,   19959.6 records/sec,     1.90 MB/sec,   5590.3 ms avg latency,    6111 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   5356 ms 10th,    5447 ms 25th,    5597 ms 50th,    5741 ms 75th,    5832 ms 90th,    5885 ms 95th,    6048 ms 99th,    6104 ms 99.9th,    6110 ms 99.99th.
RabbitMQ Write_Reading      87774 records,   17551.3 records/sec,     1.67 MB/sec,   5458.7 ms avg latency,    6162 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   5142 ms 10th,    5275 ms 25th,    5444 ms 50th,    5626 ms 75th,    5791 ms 90th,    5892 ms 95th,    6102 ms 99th,    6159 ms 99.9th,    6162 ms 99.99th.
RabbitMQ Write_Reading      73548 records,   14703.7 records/sec,     1.40 MB/sec,   7143.5 ms avg latency,    8220 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   6229 ms 10th,    6594 ms 25th,    7089 ms 50th,    7717 ms 75th,    7926 ms 90th,    8194 ms 95th,    8216 ms 99th,    8219 ms 99.9th,    8220 ms 99.99th.
RabbitMQ Write_Reading      87380 records,   17472.5 records/sec,     1.67 MB/sec,   6758.3 ms avg latency,    7878 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   5472 ms 10th,    6353 ms 25th,    6771 ms 50th,    7381 ms 75th,    7637 ms 90th,    7744 ms 95th,    7851 ms 99th,    7875 ms 99.9th,    7878 ms 99.99th.
RabbitMQ Write_Reading     103059 records,   20607.7 records/sec,     1.97 MB/sec,   5713.6 ms avg latency,    6219 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   5431 ms 10th,    5528 ms 25th,    5687 ms 50th,    5890 ms 75th,    6066 ms 90th,    6142 ms 95th,    6187 ms 99th,    6215 ms 99.9th,    6219 ms 99.99th.
RabbitMQ Write_Reading     108899 records,   21832.2 records/sec,     2.08 MB/sec,   5294.3 ms avg latency,    5842 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   5049 ms 10th,    5138 ms 25th,    5271 ms 50th,    5448 ms 75th,    5543 ms 90th,    5553 ms 95th,    5840 ms 99th,    5841 ms 99.9th,    5842 ms 99.99th.
Total : RabbitMQ Write_Reading    1161648 records,   19360.8 records/sec,     1.85 MB/sec,   5238.7 ms avg latency,  303472 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,      271 higher;   4044 ms 10th,    4800 ms 25th,    5277 ms 50th,    5657 ms 75th,    6499 ms 90th,    7303 ms 95th,    7913 ms 99th,    8211 ms 99.9th,    8219 ms 99.99th.
2021-02-27 21:25:40 INFO SBK Performance Shutdown
2021-02-27 21:25:46 INFO PrometheusLogger Stopped
2021-02-27 21:25:47 INFO SBK Benchmark Shutdown


```

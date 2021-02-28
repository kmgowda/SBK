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

Note that setting "-async" true , creates the callback Readers.

The sample output is as follows:

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class rabbitmq  -broker 127.0.0.1  -topic kmg-topic-2 -size 100 -writers 1 -readers 1   -seconds 60 -async true           
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-02-28 11:37:18 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-02-28 11:37:18 INFO Java Runtime Version: 11.0.8+11
2021-02-28 11:37:18 INFO SBK Version: 0.861
2021-02-28 11:37:18 INFO Arguments List: [-class, rabbitmq, -broker, 127.0.0.1, -topic, kmg-topic-2, -size, 100, -writers, 1, -readers, 1, -seconds, 60, -async, true]
2021-02-28 11:37:18 INFO sbk.applicationName: sbk
2021-02-28 11:37:18 INFO sbk.className: 
2021-02-28 11:37:18 INFO Reflections took 55 ms to scan 34 urls, producing 52 keys and 172 values 
2021-02-28 11:37:18 INFO Available Drivers : 33
2021-02-28 11:37:18 INFO Arguments to Driver 'RabbitMQ' : [-broker, 127.0.0.1, -topic, kmg-topic-2, -size, 100, -writers, 1, -readers, 1, -seconds, 60, -async, true]
2021-02-28 11:37:18 INFO Time Unit: MILLISECONDS
2021-02-28 11:37:18 INFO Minimum Latency: 0 ms
2021-02-28 11:37:18 INFO Maximum Latency: 180000 ms
2021-02-28 11:37:18 INFO Window Latency Store: Array
2021-02-28 11:37:18 INFO Total Window Latency Store: HashMap
2021-02-28 11:37:18 INFO PrometheusLogger Started
2021-02-28 11:37:18 INFO Starting RabbitMQ CallbackReader
RabbitMQ Write_Reading      89832 records,   17962.8 records/sec,     1.71 MB/sec,   1781.6 ms avg latency,    3279 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    341 ms 10th,    1071 ms 25th,    1892 ms 50th,    2513 ms 75th,    2967 ms 90th,    3241 ms 95th,    3270 ms 99th,    3278 ms 99.9th,    3278 ms 99.99th.
RabbitMQ Write_Reading     112054 records,   22406.3 records/sec,     2.14 MB/sec,   3336.4 ms avg latency,    3851 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3116 ms 10th,    3207 ms 25th,    3327 ms 50th,    3421 ms 75th,    3593 ms 90th,    3688 ms 95th,    3809 ms 99th,    3847 ms 99.9th,    3851 ms 99.99th.
RabbitMQ Write_Reading     103706 records,   20737.1 records/sec,     1.98 MB/sec,   3935.9 ms avg latency,    4217 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3759 ms 10th,    3851 ms 25th,    3937 ms 50th,    4035 ms 75th,    4110 ms 90th,    4140 ms 95th,    4179 ms 99th,    4214 ms 99.9th,    4217 ms 99.99th.
RabbitMQ Write_Reading     104080 records,   20811.8 records/sec,     1.98 MB/sec,   3927.1 ms avg latency,    4211 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3751 ms 10th,    3849 ms 25th,    3935 ms 50th,    4017 ms 75th,    4089 ms 90th,    4125 ms 95th,    4175 ms 99th,    4206 ms 99.9th,    4210 ms 99.99th.
RabbitMQ Write_Reading     103387 records,   20673.3 records/sec,     1.97 MB/sec,   4144.1 ms avg latency,    4436 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3988 ms 10th,    4053 ms 25th,    4151 ms 50th,    4229 ms 75th,    4297 ms 90th,    4333 ms 95th,    4404 ms 99th,    4432 ms 99.9th,    4436 ms 99.99th.
RabbitMQ Write_Reading     106129 records,   21221.6 records/sec,     2.02 MB/sec,   3706.1 ms avg latency,    4302 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3400 ms 10th,    3524 ms 25th,    3651 ms 50th,    3896 ms 75th,    4077 ms 90th,    4149 ms 95th,    4237 ms 99th,    4299 ms 99.9th,    4301 ms 99.99th.
RabbitMQ Write_Reading     129338 records,   25862.4 records/sec,     2.47 MB/sec,   3850.8 ms avg latency,    4549 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3195 ms 10th,    3416 ms 25th,    4004 ms 50th,    4186 ms 75th,    4319 ms 90th,    4531 ms 95th,    4547 ms 99th,    4549 ms 99.9th,    4549 ms 99.99th.
RabbitMQ Write_Reading     125160 records,   25027.0 records/sec,     2.39 MB/sec,   3470.1 ms avg latency,    3772 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3284 ms 10th,    3383 ms 25th,    3486 ms 50th,    3579 ms 75th,    3640 ms 90th,    3670 ms 95th,    3734 ms 99th,    3767 ms 99.9th,    3772 ms 99.99th.
RabbitMQ Write_Reading     126476 records,   25290.1 records/sec,     2.41 MB/sec,   3486.3 ms avg latency,    3846 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3267 ms 10th,    3359 ms 25th,    3492 ms 50th,    3607 ms 75th,    3709 ms 90th,    3759 ms 95th,    3845 ms 99th,    3846 ms 99.9th,    3846 ms 99.99th.
RabbitMQ Write_Reading     130238 records,   26042.4 records/sec,     2.48 MB/sec,   3494.5 ms avg latency,    4422 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3145 ms 10th,    3260 ms 25th,    3419 ms 50th,    3582 ms 75th,    4127 ms 90th,    4147 ms 95th,    4419 ms 99th,    4421 ms 99.9th,    4422 ms 99.99th.
RabbitMQ Write_Reading     123002 records,   24595.5 records/sec,     2.35 MB/sec,   3507.3 ms avg latency,    3965 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3008 ms 10th,    3389 ms 25th,    3584 ms 50th,    3721 ms 75th,    3836 ms 90th,    3882 ms 95th,    3928 ms 99th,    3959 ms 99.9th,    3964 ms 99.99th.
RabbitMQ Write_Reading     121128 records,   24279.0 records/sec,     2.32 MB/sec,   3585.4 ms avg latency,    3957 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3305 ms 10th,    3441 ms 25th,    3585 ms 50th,    3729 ms 75th,    3840 ms 90th,    3947 ms 95th,    3956 ms 99th,    3957 ms 99.9th,    3957 ms 99.99th.
Total : RabbitMQ Write_Reading    1374530 records,   22908.8 records/sec,     2.18 MB/sec,   3540.6 ms avg latency,    4549 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3134 ms 10th,    3364 ms 25th,    3589 ms 50th,    3920 ms 75th,    4126 ms 90th,    4201 ms 95th,    4415 ms 99th,    4547 ms 99.9th,    4549 ms 99.99th.
2021-02-28 11:38:18 INFO SBK Performance Shutdown
2021-02-28 11:38:22 INFO PrometheusLogger Stopped
2021-02-28 11:38:23 INFO SBK Benchmark Shutdown

```

The SBK command with synchronous reader is as follows :
```
./build/install/sbk/bin/sbk -class rabbitmq  -broker 127.0.0.1  -topic kmg-topic-3 -size 100 -writers 1 -readers 1   -seconds 60 
```

Note that setting "-async" true , creates the callback Readers.

The sample output is as follows:

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class rabbitmq  -broker 127.0.0.1  -topic kmg-topic-3 -size 100 -writers 1 -readers 1   -seconds 60 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-02-28 11:40:39 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-02-28 11:40:39 INFO Java Runtime Version: 11.0.8+11
2021-02-28 11:40:39 INFO SBK Version: 0.861
2021-02-28 11:40:39 INFO Arguments List: [-class, rabbitmq, -broker, 127.0.0.1, -topic, kmg-topic-3, -size, 100, -writers, 1, -readers, 1, -seconds, 60]
2021-02-28 11:40:39 INFO sbk.applicationName: sbk
2021-02-28 11:40:39 INFO sbk.className: 
2021-02-28 11:40:40 INFO Reflections took 59 ms to scan 34 urls, producing 52 keys and 172 values 
2021-02-28 11:40:40 INFO Available Drivers : 33
2021-02-28 11:40:40 INFO Arguments to Driver 'RabbitMQ' : [-broker, 127.0.0.1, -topic, kmg-topic-3, -size, 100, -writers, 1, -readers, 1, -seconds, 60]
2021-02-28 11:40:40 INFO Time Unit: MILLISECONDS
2021-02-28 11:40:40 INFO Minimum Latency: 0 ms
2021-02-28 11:40:40 INFO Maximum Latency: 180000 ms
2021-02-28 11:40:40 INFO Window Latency Store: Array
2021-02-28 11:40:40 INFO Total Window Latency Store: HashMap
2021-02-28 11:40:40 INFO PrometheusLogger Started
2021-02-28 11:40:40 INFO Starting RabbitMQ Reader
RabbitMQ Write_Reading      89040 records,   17804.4 records/sec,     1.70 MB/sec,   1842.2 ms avg latency,    3293 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;    408 ms 10th,    1098 ms 25th,    1986 ms 50th,    2637 ms 75th,    3047 ms 90th,    3198 ms 95th,    3269 ms 99th,    3290 ms 99.9th,    3292 ms 99.99th.
RabbitMQ Write_Reading     102985 records,   20592.9 records/sec,     1.96 MB/sec,   3623.8 ms avg latency,    3972 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3403 ms 10th,    3509 ms 25th,    3624 ms 50th,    3740 ms 75th,    3846 ms 90th,    3886 ms 95th,    3934 ms 99th,    3968 ms 99.9th,    3972 ms 99.99th.
RabbitMQ Write_Reading      98227 records,   19641.5 records/sec,     1.87 MB/sec,   4275.5 ms avg latency,    4595 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   4071 ms 10th,    4183 ms 25th,    4276 ms 50th,    4381 ms 75th,    4463 ms 90th,    4513 ms 95th,    4569 ms 99th,    4593 ms 99.9th,    4595 ms 99.99th.
RabbitMQ Write_Reading      98087 records,   19613.5 records/sec,     1.87 MB/sec,   4382.8 ms avg latency,    4668 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   4213 ms 10th,    4289 ms 25th,    4380 ms 50th,    4478 ms 75th,    4558 ms 90th,    4594 ms 95th,    4644 ms 99th,    4663 ms 99.9th,    4668 ms 99.99th.
RabbitMQ Write_Reading     102789 records,   20553.7 records/sec,     1.96 MB/sec,   4299.0 ms avg latency,    4658 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   4071 ms 10th,    4180 ms 25th,    4301 ms 50th,    4415 ms 75th,    4542 ms 90th,    4579 ms 95th,    4629 ms 99th,    4653 ms 99.9th,    4657 ms 99.99th.
RabbitMQ Write_Reading     102962 records,   20584.2 records/sec,     1.96 MB/sec,   4150.6 ms avg latency,    4427 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   4001 ms 10th,    4057 ms 25th,    4148 ms 50th,    4242 ms 75th,    4312 ms 90th,    4343 ms 95th,    4394 ms 99th,    4423 ms 99.9th,    4427 ms 99.99th.
RabbitMQ Write_Reading     100881 records,   20172.2 records/sec,     1.92 MB/sec,   4209.6 ms avg latency,    4505 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   4047 ms 10th,    4106 ms 25th,    4205 ms 50th,    4302 ms 75th,    4374 ms 90th,    4413 ms 95th,    4498 ms 99th,    4504 ms 99.9th,    4505 ms 99.99th.
RabbitMQ Write_Reading     102031 records,   20402.1 records/sec,     1.95 MB/sec,   4140.7 ms avg latency,    4448 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3982 ms 10th,    4050 ms 25th,    4141 ms 50th,    4230 ms 75th,    4304 ms 90th,    4339 ms 95th,    4384 ms 99th,    4439 ms 99.9th,    4447 ms 99.99th.
RabbitMQ Write_Reading     102547 records,   20505.3 records/sec,     1.96 MB/sec,   4252.5 ms avg latency,    4538 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   4092 ms 10th,    4160 ms 25th,    4255 ms 50th,    4350 ms 75th,    4411 ms 90th,    4445 ms 95th,    4487 ms 99th,    4535 ms 99.9th,    4538 ms 99.99th.
RabbitMQ Write_Reading     104133 records,   20822.4 records/sec,     1.99 MB/sec,   4120.5 ms avg latency,    4440 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3948 ms 10th,    4011 ms 25th,    4119 ms 50th,    4218 ms 75th,    4307 ms 90th,    4345 ms 95th,    4393 ms 99th,    4430 ms 99.9th,    4434 ms 99.99th.
RabbitMQ Write_Reading     102594 records,   20514.7 records/sec,     1.96 MB/sec,   4110.0 ms avg latency,    4425 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3938 ms 10th,    4002 ms 25th,    4100 ms 50th,    4217 ms 75th,    4293 ms 90th,    4337 ms 95th,    4403 ms 99th,    4420 ms 99.9th,    4424 ms 99.99th.
RabbitMQ Write_Reading     125906 records,   25241.8 records/sec,     2.41 MB/sec,   3735.8 ms avg latency,    4430 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3225 ms 10th,    3401 ms 25th,    3775 ms 50th,    4042 ms 75th,    4177 ms 90th,    4238 ms 95th,    4383 ms 99th,    4426 ms 99.9th,    4429 ms 99.99th.
Total : RabbitMQ Write_Reading    1232182 records,   20536.4 records/sec,     1.96 MB/sec,   3944.8 ms avg latency,    4668 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;   3374 ms 10th,    3950 ms 25th,    4142 ms 50th,    4284 ms 75th,    4395 ms 90th,    4466 ms 95th,    4579 ms 99th,    4646 ms 99.9th,    4663 ms 99.99th.
2021-02-28 11:41:40 INFO SBK Performance Shutdown
2021-02-28 11:41:43 INFO PrometheusLogger Stopped
2021-02-28 11:41:44 INFO SBK Benchmark Shutdown

```
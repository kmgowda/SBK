<!--
Copyright (c) 2020 KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Storage Benchmark Kit (SBK) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![Version](https://img.shields.io/badge/release-0.5-blue)](https://github.com/kmgowda/dsb/releases)

The SBK (Storage Benchmark Kit) is an open source software frame-work for the performance benchmarking of any generic both persistent or non-persistent storage systems. If you are curious measure the  maximum throughput of your storage device/system, then SBK is the right  for you. The SBK itself a very high-performance benchmark  too. It massively writes the data to storage system. This  supports multi writers and readers. This  also supports End to End latency. The percentile is calculated for complete data written/read without any sampling; hence the percentiles are 100% accurate.

Currently SBK supports benchmarking of Apache Kafka, Pulsar and Pravega distributed streaming storages. In future, many more storage storage systems drivers will be plugged in. Refer to :   [[Add your driver](https://github.com/kmgowda/dsb/blob/master/README.md#add-your-driver-to-dsb-kit )] to know how to add your driver/storage device for performance benchmarking.

we welcome if you are interested in contributing this open source by adding a driver your storage device and any features to SBK. Refer to : 


## Build SBK

### Prerequisites

- Java 8+
- Gradle 4+

### Building

Checkout the source code:

```
git clone https://github.com/kmgowda/sbk.git
cd dsb
```

Build the SBK:

```
./gradlew build
```

untar the SBK  to local folder

```
tar -xvf ./build/distributions/sbk.tar -C ./run
```

Running SBK locally:

```
<dir>/SBK$ ./run/SBK/bin/sbk  -help
 usage: sbk
 -class <arg>        Benchmark class (refer to driver-* folder)
 -csv <arg>          CSV file to record write/read latencies
 -flush <arg>        Each Writer calls flush after writing <arg> number of
                     of events(records); Not applicable, if both writers
                     and readers are specified
 -help               Help message
 -readers <arg>      Number of readers
 -records <arg>      Number of records(events) if 'time' not specified;
                     otherwise, Maximum records per second by writer(s)
                     and/or Number of records per reader
 -size <arg>         Size of each message (event or record)
 -throughput <arg>   if > 0 , throughput in MB/s
                     if 0 , writes 'events'
                     if -1, get the maximum throughput
 -time <arg>         Number of seconds the SBK runs (24hrs by default)
 -writers <arg>      Number of writers
```

## Running Performance benchmarking

SBK outputs the number of records written/read , throughput in terms of MB/s and the average and maximum latency for every 5 seconds time interval as show in below.

```
Writing     152372 records,   30328.8 records/sec,   28.92 MB/sec,    35.4 ms avg latency,  1238.0 ms max latency
Writing     178680 records,   35382.2 records/sec,   33.74 MB/sec,    26.2 ms avg latency,   189.0 ms max latency
Writing     176365 records,   35160.5 records/sec,   33.53 MB/sec,    27.2 ms avg latency,   197.0 ms max latency
Writing      73151 records,   14621.4 records/sec,   13.94 MB/sec,    62.8 ms avg latency,   399.0 ms max latency
```

At the end of the benchmarking session, SBK outputs the total data written/read , average throughput and latency , maximum latency  and the percentiles 50th, 75th, 95th, 99th , 99.9th and 99.99th for the complete data records written/read.
An example  final output is show as below:

```
Writing (Total)      641805 records,   20696.0 records/sec,   19.74 MB/sec,    32.7 ms avg latency,  1238.0 ms max latency
Writing Latencies 22 ms 50th, 31 ms 75th, 90 ms 95th, 168 ms 99th, 1064 ms 99.9th, 1099 ms 99.99th.
```


The SBK  can be executed to
 - write/read specific amount of events/records to/from the Pravega cluster
 - write/read the events/records for the specified amount of time

The SBK can be executed in the following modes:
```
1. Burst Mode
2. Throughput Mode
3. OPS Mode or  Events Rate / Rate limiter Mode
4. End to End Latency Mode
```

### 1 - Burst Mode
In this mode, the SBK pushes/pulls the messages to/from the Pravega client as much as possible.
This mode is used to find the maximum and throughput that can be obtained from the Pravega cluster.
This mode can be used for both producers and consumers.

```
For example:
<SBK directory>/run/SBK/bin/SBK  -controller tcp://127.0.0.1:9090  -stream streamname1  -segments 1  -producers 1  -size 100   -throughput -1   -time 60

The -throughput -1  indicates the burst mode.
This test will executed for 60 seconds because option -time 60 is used.
This test tries to write and read events of size 100 bytes to/from the stream 'streamname1'.
The option '-controller tcp://127.0.0.1:9090' specifies the pravega controller IP address and port number.
Note that -producers 1 indicates 1 producer/writers.

in the case you want to write/read the certain number of events use the -events option without -time option as follows

<SBK directory>/run/SBK/bin/SBK -controller tcp://127.0.0.1:9090  -stream streamname1  -segments 1  -producers 1  -size 100   -throughput -1   -events 1000000

-events <number> indicates that total <number> of events to write/read
```

### 2 - Throughput Mode
In this mode, the SBK  pushes the messages to the Pravega client with specified approximate maximum throughput in terms of Mega Bytes/second (MB/s).
This mode is used to find the least latency that can be obtained from the Pravega cluster for given throughput.
This mode is used only for write operation.

```
For example:
<SBK directory>/run/SBK/bin/SBK   -controller tcp://127.0.0.1:9090  -stream streamname5  -segments 5  -producers 5   -size 100   -throughput 10   -time 300

The -throughput <positive number>  indicates the Throughput mode.

This test will be executed with approximate max throughput of 10MB/sec.
This test will executed for 300 seconds (5 minutes) because option -time 60 is used.
This test tries to write and read events of size 100 bytes to/from the stream 'streamname5' of 5 segments.
If the stream 'streamname5' is not existing , then it will be created with the 5 segments.
if the steam is already existing then it will be scaled up/down to 5 segments.
Note that -producers 5 indicates 5 producers/writers .

in the case you want to write/read the certain number of events use the -events option without -time option as follows

<SBK directory>/run/SBK/bin/SBK  -controller tcp://127.0.0.1:9090  -stream streamname5  -segments 5  -producers 1  -size 100   -throughput 10   -events 1000000

-events 1000000 indicates that total 1000000 (1 million) of events will be written at the throughput speed of 10MB/sec
```

### 3 - OPS Mode or  Events Rate / Rate Limiter Mode
This mode is another form of controlling writers throughput by limiting the number of events per second.
In this mode, the SBK  pushes the messages to the Pravega client with specified approximate maximum events per sec.
This mode is used to find the least latency  that can be obtained from the Pravega cluster for events rate.
This mode is used only for write operation.

```
For example:
<SBK directory>/run/SBK/bin/SBK   -controller tcp://127.0.0.1:9090  -stream streamname1  -segments 1  -producers 5  -size 100  -events 1000   -time 60

The -events <event numbers>  (1000 ) specifies the events per second to write.
Note that the option "-throughput"  SHOULD NOT supplied for this OPS Mode or  Events Rate / Rate limiter Mode.

This test will be executed with approximate 1000 events per second by 6 producers.
This test will executed for 300 seconds (5 minutes) because option -time 60 is used.
Note that in this mode, there is 'NO total number of events' to specify hence user must supply the time to run using -time option.
```

### 4 - End to End Latency Mode
In this mode, the SBK  writes and read the messages to the Pravega cluster and records the end to end latency.
End to end latency means the time duration between the beginning of the writing event/record to stream and the time after reading the event/record.
in this mode user must specify both the number of producers and consumers.
The -throughput option (Throughput mode) or -events (late limiter) can used to limit the writers throughput or events rate.

```
For example:
<SBK directory>/run/SBK/bin/SBK  -controller tcp://127.0.0.1:9090  -stream streamname3  -segments 1  -producers 1 -consumers 1  -size 100  -throughput -1   -time 60

The user should specify both producers and consumers count  for write to read or End to End latency mode. it should be set to true.
The -throughput -1 specifies the writes tries to write the events at the maximum possible speed.
```

### Recording the latencies to CSV files
User can use the option "-csv <file name>" to record the latencies of writers/readers.
    
## Add your driver to SBK
1. Create the gradle sub project preferable with the name driver-<your driver/storage device name>.
        See the Example: [[Pulsar driver](https://github.com/kmgowda/sbk/tree/master/driver-pravega)]   

2. create the package io.sbk.< your driver name>
        See the Example: [[Pulsar driver package](https://github.com/kmgowda/sbk/tree/master/driver-pulsar/src/main/java/io/sbk/Pulsar)]   
    
3. In your driver package you have to implement the Interface: [[Benchmark](https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Benchmark.java)]
        See the Example:  [[Pulsar class](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/Pulsar.java)]
    
4. Extend the class Writer: [[Writer](https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Writer.java)]

5. Extend the class Reader: [[Reader](https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Reader.java)]

6. That's all ; Now, Build the SBK with your driver with the command:

```
./gradlew build
```

untar the SBK  to local folder

```
tar -xvf ./build/distributions/sbk.tar -C ./run
```

7.  To invoke the benchmarking of the your driver you have issue the parameters "-class < your driver name>"

Example: For pulsar driver
```
<SBK directory>/run/sbk/bin/sbk  -class Pulsar -help
 usage: sbk -class Pulsar
 -ackQuorum <arg>       ackQuorum (default: 1)
 -admin <arg>           Admin URI, required to create the partitioned
                        topic
 -broker <arg>          Broker URI
 -class <arg>           Benchmark class (refer to driver-* folder)
 -cluster <arg>         Cluster name (optional parameter)
 -csv <arg>             CSV file to record write/read latencies
 -deduplication <arg>   Enable or Disable Deduplication; by default
                        disabled
 -ensembleSize <arg>    ensembleSize (default: 1)
 -flush <arg>           Each Writer calls flush after writing <arg> number
                        of of events(records); Not applicable, if both
                        writers and readers are specified
 -help                  Help message
 -partitions <arg>      Number of partitions of the topic (default: 1)
 -readers <arg>         Number of readers
 -records <arg>         Number of records(events) if 'time' not specified;
                        otherwise, Maximum records per second by writer(s)
                        and/or Number of records per reader
 -size <arg>            Size of each message (event or record)
 -throughput <arg>      if > 0 , throughput in MB/s
                        if 0 , writes 'events'
                        if -1, get the maximum throughput
 -time <arg>            Number of seconds this SBK runs (24hrs by default)
 -topic <arg>           Topic name
 -writeQuorum <arg>     writeQuorum (default: 1)
 -writers <arg>         Number of writers
```

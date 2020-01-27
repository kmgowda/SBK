<!--
Copyright (c) 2020 KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Storage Benchmark Kit (SBK) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![Version](https://img.shields.io/badge/release-0.5-blue)](https://github.com/kmgowda/dsb/releases)

The SBK (Storage Benchmark Kit) is an open source software frame-work for the performance benchmarking of any generic both persistent or non-persistent storage systems. If you are curious measure the  maximum throughput of your storage device/system, then SBK is the right software for you. The SBK itself a very high-performance benchmark  too. It massively writes the data to storage system. This  supports multi writers and readers. This  also supports End to End latency. The percentile is calculated for complete data written/read without any sampling; hence the percentiles are 100% accurate.

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
tar -xvf ./build/distributions/sbk.tar -C ./build/distributions/.
```

Running SBK locally:

```
<dir>/sbk$ ./build/distributions/sbk/bin/sbk  -help
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
 -time <arg>         Number of seconds this SBK runs (24hrs by default)
 -writers <arg>      Number of writers
```

## Running Performance benchmarking

SBK outputs the number of records written/read, throughput in terms of MB/s and the average and maximum latency for every 5 seconds time interval as show in below.

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
1. Burst Mode (Max rate mode)
2. Throughput Mode
3. Rate limiter Mode (Recrods Rate or Events Rate Mode)
4. End to End Latency Mode
```

### 1 - Burst Mode / Max Rate Mode
In this mode, the SBK pushes/pulls the messages to/from the storage client(device/driver) as much as possible.
This mode is used to find the maximum and throughput that can be obtained from the storage device or storage cluster (server).
This mode can be used for both writers and readers.
By default, the SBK runs in Burst mode.

```
For example: The Burst mode for pulsar single writer as follows

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-223  -partitions 1  -writers 1 -size 1000  -time 60 -throughput -1


The -throughput -1  indicates the burst mode. Note that, you dont supply the parameter -throughput then also its burst mode.
This test will executed for 60 seconds because option -time 60 is used.
This test tries to write and read events of size 1000 bytes to/from the topic 'topic-k-223'.
The option '-broker tcp://localhost:6650' specifies the Pulsar broker IP address and port number for write operations.
The option '-admin http://localhost:8080' specifies the Pulsar admin IP and port number for topic creation and deletion.
Note that -producers 1 indicates 1 producer/writers.

in the case you want to write/read the certain number of records.events use the -records option without -time option as follows

<SBK directory>/build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-223  -partitions 1  -writers 1 -size 1000  -records 100000 -throughput -1

-records <number> indicates that total <number> of records to write/read
```

### 2 - Throughput Mode
In this mode, the SBK  pushes the messages to the storage client(device/driver) with specified approximate maximum throughput in terms of Mega Bytes/second (MB/s).
This mode is used to find the least latency that can be obtained from the storage device or storage cluster (server) for given throughput.
This mode is used only for write operation.

```
For example:  The througput mode for pulsar 5 writers as follows
<SBK directory> ./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-223  -partitions 1  -writers 5 -size 1000  -time 120  -throughput 10

The -throughput <positive number>  indicates the Throughput mode.

This test will be executed with approximate max throughput of 10MB/sec.
This test will executed for 120 seconds (2 minutes) because option -time 120 is used.
This test tries to write and read events of size 1000 bytes to/from the topic 'topic-k-223' of 1 partition.
If the toic 'topic-k-223' is not existing , then it will be created with  1 segment.
if the steam is already existing then it will be deleted and recreated with 1 segment.
Note that -writers 5 indicates 5 producers/writers .

in the case you want to write/read the certain number of events use the -records option without -time option as follows

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-k-223  -partitions 1  -writers 5 -size 1000  -records 1000000  -throughput 10

-records 1000000 indicates that total 1000000 (1 million) of events will be written at the throughput speed of 10MB/sec
```

### 3 - Rate limiter Mode (Recrods Rate or Events Rate Mode)
This mode is another form of controlling writers throughput by limiting the number of records per second.
In this mode, the SBK  pushes the messages to the storage client (device/driver) with specified approximate maximum records per sec.
This mode is used to find the least latency  that can be obtained from the storage device or storage cluster (server) for events rate.
This mode is used only for write operation.

```
For example:  The Rate limiter Mode for pulsar 5 writers as follows

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broke
r tcp://localhost:6650 -topic topic-k-225  -partitions 10  -writers 5 -size 100  -time 60  -records 1000

The -records <records numbes>  (1000) specifies the records per second to write.
Note that the option "-throughput"  SHOULD NOT supplied for this  Rate limiter Mode (Recrods Rate or Events Rate Mode).

This test will be executed with approximate 1000 events per second by 5 writers.
The topic "topic-k-225" with 10 partitions are created to run this test.
This test will executed for 60seconds (1 minutes) because option -time 60 is used.
Note that in this mode, there is 'NO total number of events' to specify hence user must supply the time to run using -time option.
```

### 4 - End to End Latency Mode
In this mode, the SBK  writes and read the messages to the storage client (device/driver) and records the end to end latency.
End to end latency means the time duration between the beginning of the writing event/record to stream and the time after reading the event/record.
in this mode user must specify both the number of writers and readers.
The -throughput option (Throughput mode) or -records (late limiter) can used to limit the writers throughput or records rate.

```
For example: The End to End latency of between single writer and single reader of pulsar is as follows:

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broker tcp://localhost:6650 -topic topic-km-1  -partitions 1  -writers 1 -readers 1 -size 1000 -throughput -1 -time 60 

The user should specify both writers and readers count for write to read or End to End latency mode.
The -throughput -1 specifies the writes tries to write the events at the maximum possible speed.
```

## Recording the latencies to CSV files
User can use the option "-csv [file name]" to record the latencies of writers/readers.
    
## Add your driver to SBK
1. Create the gradle sub project preferable with the name driver-<your driver(storage device) name>.

    * See the Example:[[Pulsar driver](https://github.com/kmgowda/sbk/tree/master/driver-pulsar)]   

2. Create the package io.sbk.< your driver name>

    * See the Example: [[Pulsar driver package](https://github.com/kmgowda/sbk/tree/master/driver-pulsar/src/main/java/io/sbk/Pulsar)]   
    
3. In your driver package you have to implement the Interface: [[Benchmark](https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Benchmark.java)]

    * See the Example:  [[Pulsar class](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/Pulsar.java)]
        
    * you have to implement the following methods of Benchmark Interface:
        
      a). Add the Addtional parameters for your driver : https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Benchmark.java#L23
        
      b). Parse your driver specific paramters: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Benchmark.java#L30
        
      c). Open the storage: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Benchmark.java#L37
        
      d). Close the storage: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Benchmark.java#L44
        
      e). Create the writer: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Benchmark.java#L53
        
      f). Create the Reader: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Benchmark.java#L53
    
4. Extend the class Writer: [[Writer](https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Writer.java)]
    * See the Example: https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/PulsarWriter.java
    
    * you have to implement the following methods of Writer class:
        
      a). Writer Data [Async or Sync]: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Writer.java#L41
        
      b). Flush the data: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Writer.java#L47
        
      c). Close the Writer: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Writer.java#L53
        
      d). In case , if you want to have your own recordWrite implemenation to write data and record the start and end time, then you can override: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Writer.java#L64
        
5. Extend the class Reader: [[Reader](https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Reader.java)]

    * See the Example: https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/PulsarReader.java

    * you have to implement the following methods of Reader class:
        
      a). Read Data (synchronous reades): https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Reader.java#L35
        
      b). Close the Reader: https://github.com/kmgowda/sbk/blob/master/sbk-api/src/main/java/io/sbk/api/Reader.java#L41 
          
6. That's all ; Now, Build the SBK included your driver with the command:

```
./gradlew build
```

untar the SBK  to local folder

```
tar -xvf ./build/distributions/sbk.tar -C ./build/distributions/.
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

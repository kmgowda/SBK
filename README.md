<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Storage Benchmark Kit (SBK) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)  [![Api](https://img.shields.io/badge/SBK-API-brightgreen)](https://kmgowda.github.io/SBK/javadoc/index.html) [![Version](https://img.shields.io/badge/release-0.61-blue)](https://github.com/kmgowda/SBK/releases/tag/0.61)

The SBK (Storage Benchmark Kit) is an open source software frame-work for the performance benchmarking of any storage system. If you are curious to measure the  maximum throughput performance of your storage device/system, then SBK is the right software for you. The SBK itself a very high-performance benchmark  tool/frame work. It massively writes the data to storage system and reads the data from strorage system. The SBK supports multi writers and readers and also the End to End latency benchmarking. The percentiles are calculated for complete data written/read without any sampling; hence the percentiles are 100% accurate.

Currently SBK supports benchmarking of
1. [Apache Kafka](https://kafka.apache.org)
2. [Apache Pulsar](https://pulsar.apache.org)
3. [Pravega](http://pravega.io) distributed streaming storage systems
4. Local mounted File Systems
5. [Java Concurrent Queue [Message Queue]](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ConcurrentLinkedQueue.html).

In future, many more storage storage systems drivers will be plugged in. 

we welcome open source developers to contribute to this project by adding a driver your storage device and any features to SBK. Refer to : 
* [[Contributing to SBK](https://github.com/kmgowda/sbk/blob/master/README.md#contributing-to-sbk)] for the Contributing guidlines.
* [[Add your storage driver to SBK](https://github.com/kmgowda/sbk/blob/master/README.md#add-your-driver-to-sbk)] to know how to add your driver (storage device driver or client) for performance benchmarking.  

## Build SBK

### Prerequisites

- Java 8+
- Gradle 4+

### Building

Checkout the source code:

```
git clone https://github.com/kmgowda/SBK.git
cd SBK
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
<SBK directory>$ ./build/distributions/sbk/bin/sbk  -help
usage: sbk
 -class <arg>        Benchmark Driver Class,
                     Available Drivers [ConcurrentQ, File, Kafka, Pravega,
                     Pulsar]
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
 -version            Version
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
 - write/read specific amount of events/records to/from the storage driver (device/cluster)
 - write/read the events/records for the specified amount of time

The SBK can be executed in the following modes:
```
1. Burst Mode (Max rate mode)
2. Throughput Mode
3. Rate limiter Mode
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

### 3 - Rate limiter Mode
This mode is another form of controlling writers throughput by limiting the number of records per second.
In this mode, the SBK  pushes the messages to the storage client (device/driver) with specified approximate maximum records per sec.
This mode is used to find the least latency  that can be obtained from the storage device or storage cluster (server) for events rate.
This mode is used only for write operation.

```
For example:  The Rate limiter Mode for pulsar 5 writers as follows

<SBK directory>./build/distributions/sbk/bin/sbk -class Pulsar -admin http://localhost:8080 -broke
r tcp://localhost:6650 -topic topic-k-225  -partitions 10  -writers 5 -size 100  -time 60  -records 1000

The -records <records numbes>  (1000) specifies the records per second to write.
Note that the option "-throughput"  SHOULD NOT supplied for this  Rate limiter Mode.

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

## Contributing to SBK
All submissions to the master are done through pull requests. If you'd like to make a change:

1. Create a new Git hub issue ([SBK issues](https://github.com/kmgowda/sbk/issues)) describing the problem / feature.
2. Fork a branch.
3. Make your changes. 
    * you can refer ([Oracle Java Coding Style](https://www.oracle.com/technetwork/java/codeconvtoc-136057.html)) for coding style; however, Running the Gradle build helps you to fix the Coding syte issues too. 
4. Verify all changes are working and Gradle build checkstyle is good.
5. Submit a pull request with Issue Numer, Description and your Sign-off.

Make sure that you update the issue with all details of testing you have done; it will helpful for me to review and merge.

Another important point to consider is how to keep up with changes against the base the branch (the one your pull request is comparing against). Let's assume that the base branch is master. To make sure that your changes reflect the recent commits, I recommend that you rebase frequently. The command I suggest you use is:

```
git pull --rebase upstream master
git push --force origin <pr-branch-name>
```
in the above, I'm assuming that:

* upstream is sbk/sbk.git
* origin is youraccount/sbk.git

The rebase might introduce conflicts, so you better do it frequently to avoid outrageous sessions of conflict resolving.

### Lombok
SBK uses [[Lombok](https://projectlombok.org)] for code optimizations; I suggest the same for all the contributors too.
If you use an IDE you'll need to install a plugin to make the IDE understand it. Using IntelliJ is recommended.

To import the source into IntelliJ:

1. Import the project directory into IntelliJ IDE. It will automatically detect the gradle project and import things correctly.
2. Enable `Annotation Processing` by going to `Build, Execution, Deployment` -> `Compiler` > `Annotation Processors` and checking 'Enable annotation processing'.
3. Install the `Lombok Plugin`. This can be found in `Preferences` -> `Plugins`. Restart your IDE.
4. Pravega should now compile properly.

For eclipse, you can generate eclipse project files by running `./gradlew eclipse`.


## Add your driver to SBK
1. Create the gradle sub project preferable with the name **driver-<your driver(storage device) name>**.

    * See the Example:[[Pulsar driver](https://github.com/kmgowda/sbk/tree/master/driver-pulsar)]   


2. Create the package **io.sbk.< your driver name>** 

    * See the Example: [[Pulsar driver package](https://github.com/kmgowda/sbk/tree/master/driver-pulsar/src/main/java/io/sbk/Pulsar)]   
    

3. In your driver package you have to implement the Interface: [[Benchmark](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Benchmark.html)]

    * See the Example:  [[Pulsar class](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/Pulsar.java)]
    
    * you have to implement the following methods of Benchmark Interface:
        
      a). Add the Addtional parameters (Command line Parameters) for your driver :[[addArgs](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Benchmark.html#addArgs-io.sbk.api.Parameters-)]
      * The default command line parameters are listed in the help output here : [[Building SBK](https://github.com/kmgowda/sbk#building)]
        
      b). Parse your driver specific paramters: [[parseArgs](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Benchmark.html#parseArgs-io.sbk.api.Parameters-)]
        
      c). Open the storage: [[openStorage](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Benchmark.html#openStorage-io.sbk.api.Parameters-)]
        
      d). Close the storage:[[closeStorage](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Benchmark.html#closeStorage-io.sbk.api.Parameters-)]
        
      e). Create a single writer instance:[[createWriter](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Benchmark.html#createWriter-int-io.sbk.api.Parameters-)]
        * Create Writer will be called multiple times by SBK incase of Multi writers are specified in the command line.   
        
      f). Create a single Reader instance:[[createReader](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Benchmark.html#createReader-int-io.sbk.api.Parameters-)]
        * Create Reader will be called multiple times by SBK incase of Multi readers are specified in the command line. 
        
      g). Get the Data Type :[[getDataType](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Benchmark.html#getDataType--)]
        * In case if your data type is byte[] (Byte Array), No need to override this method. see the example:   [[Pulsar class](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/Pulsar.java)]
        * If your Benchmark,  Reader and Writer classes operates on different data type such as String or custom data type, then you have to override this default implemenation.

    
4. Implement the Writer Interface: [[Writer](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Writer.html)]

    * See the Example: [[Pulsar Writer](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/PulsarWriter.java)]
    
    * you have to implement the following methods of Writer class:
        
      a). Writer Data [Async or Sync]: [[writeAsync](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Writer.html#writeAsync-byte:A-)]
        
      b). Flush the data: [[flush](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Writer.html#flush--)]
        
      c). Close the Writer: [[close](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Writer.html#close--)]
        
      d). In case , if you want to have your own recordWrite implemenation to write data and record the start and end time, then you can override: [[recordWrite](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Writer.html#recordWrite-byte:A-io.sbk.api.QuadConsumer-)]


5. Implement the Reader Interface: [[Reader](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Reader.html)]

    * See the Example: [[Pulsar Reader](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/src/main/java/io/sbk/Pulsar/PulsarReader.java)]

    * you have to implement the following methods of Reader class:
        
      a). Read Data (synchronous reades): [[read](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Reader.html#read--)]
        
      b). Close the Reader:[[close](https://kmgowda.github.io/SBK/javadoc/io/sbk/api/Reader.html#close--)] 


6.  Add the Gradle dependecy [ compile project(":sbk-api")]   to your sub-project (driver)

    * see the Example:[[Pulsar Gradle Build](https://github.com/kmgowda/sbk/blob/master/driver-pulsar/build.gradle)]


7. Add your sub project to main gradle as dependency.

    * see the Example: [[SBK Gradle](https://github.com/kmgowda/sbk/blob/master/build.gradle#L66)]
    
    * make sure that gradle settings file: [[SBK Gradle Settings](https://github.com/kmgowda/sbk/blob/master/settings.gradle#L13)] has your Storage driver sub project name


8. That's all ; Now, Build the SBK included your driver with the command:

```
./gradlew build
```

untar the SBK  to local folder

```
tar -xvf ./build/distributions/sbk.tar -C ./build/distributions/.
```


9.  To invoke the benchmarking of the your driver you have issue the parameters "-class < your driver name>"

Example: For pulsar driver
```
<SBK directory>/run/sbk/bin/sbk  -class Pulsar -help
usage: sbk -class Pulsar
 -ackQuorum <arg>       AckQuorum (default: 1)
 -admin <arg>           Admin URI, required to create the partitioned
                        topic
 -broker <arg>          Broker URI
 -class <arg>           Benchmark Driver Class,
                        Available Drivers [Kafka, Pravega, Pulsar]
 -cluster <arg>         Cluster name (optional parameter)
 -csv <arg>             CSV file to record write/read latencies
 -deduplication <arg>   Enable or Disable Deduplication; by default
                        disabled
 -ensembleSize <arg>    EnsembleSize (default: 1)
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
 -threads <arg>         io threads per Topic; by default (writers +
                        readers)
 -throughput <arg>      if > 0 , throughput in MB/s
                        if 0 , writes 'events'
                        if -1, get the maximum throughput
 -time <arg>            Number of seconds this SBK runs (24hrs by default)
 -topic <arg>           Topic name
 -version               Version
 -writeQuorum <arg>     WriteQuorum (default: 1)
 -writers <arg>         Number of writers
```

## Design of SBK
The SBK is a spin-off from pravega benchmark tool, refer to the paper : [[Distributed Streaming Storage Performance Benchmarking: Kafka and Pravega](https://www.researchgate.net/publication/338171860_Distributed_Streaming_Storage_Performance_Benchmarking_Kafka_and_Pravega)] to know the internal design details of SBK and comparision of Kafka and Pravega in terms of performance benchmarking.

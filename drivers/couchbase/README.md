<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# CouchBase performance benchmarking with SBK
The CouchBase driver for SBK supports single/multiple Writer , single/multiple reader performance benchmarking.

## setting up Couchbase local Docker server
To make simple demo/test, you can run the local Couchbase server docker image as follows:

```
docker run -itd --name couchbase-server -p 8091-8094:8091-8094 -p 11210:11210 couchbase:community 
```

make sure you have redirected the port 8091 from docker to local system
Next, visit http://localhost:8091 on the host machine to see the Web Console to start Couchbase Server setup.

refer: https://hub.docker.com/_/couchbase for the full docker setup details.


Now, you can run the Couchbase writer benchmarking as follows. 
```
sanjay@sanjay-Lap:~$ SBK Driver/SBK (master)
$ ./build/install/sbk/bin/sbk.bat -class couchbase -writers 1 -size 100 -seconds 60 -url couchbase://localhost -password admin1 -bucket Demo
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-08-22 22:01:33 INFO Reflections took 141 ms to scan 49 urls, producing 103 keys and 221 values
2022-08-22 22:01:33 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-08-22 22:01:33 INFO Storage Benchmark Kit
2022-08-22 22:01:33 INFO SBK Version: 1.0
2022-08-22 22:01:33 INFO SBK Website: https://github.com/kmgowda/SBK
2022-08-22 22:01:33 INFO Arguments List: [-class, couchbase, -writers, 1, -size, 100, -seconds, 60, -url, couchbase://localhost, -password, admin1, -bucket, Demo]
2022-08-22 22:01:33 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-08-22 22:01:33 INFO Storage Drivers Package: io.sbk
2022-08-22 22:01:33 INFO sbk.applicationName: sbk
2022-08-22 22:01:33 INFO sbk.appHome: sanjay@sanjay-Lap:~$ SBK Driver\SBK\build\install\sbk\bin\..
2022-08-22 22:01:33 INFO sbk.className:
2022-08-22 22:01:33 INFO '-class': couchbase
2022-08-22 22:01:33 INFO Available Storage Drivers in package 'io.sbk': 42 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive, Jdbc,
Kafka, LevelDB, MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null,
OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB,
SeaweedS3, SQLite]
2022-08-22 22:01:33 INFO Arguments to Driver 'Couchbase' : [-writers, 1, -size, 100, -seconds, 60, -url, couchbase://localhost, -password, admin1, -bucket, Demo]
2022-08-22 22:01:34 INFO Time Unit: MILLISECONDS
2022-08-22 22:01:34 INFO Minimum Latency: 0 ms
2022-08-22 22:01:34 INFO Maximum Latency: 180000 ms
2022-08-22 22:01:34 INFO Window Latency Store: Array, Size: 1 MB
2022-08-22 22:01:34 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-08-22 22:01:34 INFO Total Window Extension: None, Size: 0 MB
2022-08-22 22:01:34 INFO SBK Benchmark Started
2022-08-22 22:01:34 INFO SBK PrometheusLogger Started
2022-08-22 22:01:36 INFO [com.couchbase.core][DnsSrvLookupFailedEvent][142ms] DNS SRV lookup failed (name not found). This is expected if the there is no DNS SRV record associated with the hostname in the connection string. Will now try to bootstrap directly from the given hostname. To suppress this message, specify an IP address instead of a hostname (for example: 127.0.0.1 instead of localhost), specify more than one hostname, or set the `io.enableDnsSrv` client setting to false.
2022-08-22 22:01:38 INFO [com.couchbase.core][CoreCreatedEvent] {"clientVersion":"3.3.3","clientGitHash":"${buildNumber}","coreVersion":"2.3.3","coreGitHash":"${buildNumber}","userAgent":"couchbase-java/3.3.3 (Windows 11 10.0 amd64; Java HotSpot(TM) 64-Bit Server VM 17.0.4.1+1-LTS-2)","maxNumRequestsInRetry":32768,"ioEnvironment":{"nativeIoEnabled":true,"eventLoopThreadCount":4,"eventLoopGroups":["NioEventLoopGroup"]},"ioConfig":{"captureTraffic":[],"mutationTokensEnabled":true,"networkResolution":"auto","dnsSrvEnabled":true,"tcpKeepAlivesEnabled":true,"tcpKeepAliveTimeMs":60000,"configPollIntervalMs":2500,"kvCircuitBreakerConfig":"disabled","queryCircuitBreakerConfig":"disabled","viewCircuitBreakerConfig":"disabled","searchCircuitBreakerConfig":"disabled","analyticsCircuitBreakerConfig":"disabled","managerCircuitBreakerConfig":"disabled","eventingCircuitBreakerConfig":"disabled","backupCircuitBreakerConfig":"disabled","numKvConnections":1,"maxHttpConnections":12,"idleHttpConnectionTimeoutMs":4500,"configIdleRedialTimeoutMs":300000,"memcachedHashingStrategy":"StandardMemcachedHashingStrategy"},"compressionConfig":{"enabled":true,"minRatio":0.83,"minSize":32},"securityConfig":{"tlsEnabled":false,"nativeTlsEnabled":true,"hostnameVerificationEnabled":true,"trustCertificates":null,"trustManagerFactory":null,"ciphers":[]},"timeoutConfig":{"kvMs":2500,"kvDurableMs":10000,"managementMs":75000,"queryMs":75000,"viewMs":75000,"searchMs":75000,"analyticsMs":75000,"connectMs":10000,"disconnectMs":10000,"eventingMs":75000,"backupMs":75000},"loggerConfig":{"customLogger":null,"fallbackToConsole":false,"consoleLogLevel":{"name":"INFO","resourceBundleName":"sun.util.logging.resources.logging","localizedName":"INFO"},"consoleLoggerFormatter":"DefaultLoggerFormatter","disableSlf4j":false,"loggerName":"CouchbaseLogger","diagnosticContextEnabled":false},"orphanReporterConfig":{"emitIntervalMs":10000,"sampleSize":10,"queueLength":1024,"enabled":true},"thresholdLoggingTracerConfig":{"enabled":true,"emitIntervalMs":10000,"sampleSize":10,"queueLength":1024,"kvThresholdMs":500,"queryThresholdMs":1000,"searchThresholdMs":1000,"analyticsThresholdMs":1000,"viewThresholdMs":1000},"loggingMeterConfig":{"enabled":true,"emitIntervalMs":600000},"retryStrategy":"BestEffortRetryStrategy","requestTracer":"ThresholdLoggingTracer","meter":"LoggingMeter","numRequestCallbacks":0,"scheduler":"ParallelScheduler","schedulerThreadCount":8,"transactionsConfig":{"durabilityLevel":"MAJORITY","timeoutMs":15000,"cleanupConfig":{"runLostAttemptsCleanupThread":true,"runRegularAttemptsCleanupThread":true,"cleanupWindowMs":60000,"cleanupSet":""},"numAtrs":1024,"metadataCollection":"none","scanConsistency":"none"}} {"coreId":"0x834eecc200000001","seedNodes":[{"address":"localhost"}]}
2022-08-22 22:01:38 INFO [com.couchbase.transactions][TransactionsStartedEvent] Transactions successfully initialised, regular cleanup enabled=true, lost cleanup enabled=true
2022-08-22 22:01:38 INFO [com.couchbase.node][NodeConnectedEvent] Node connected {"coreId":"0x834eecc200000001","managerPort":"8091","remote":"localhost"}
2022-08-22 22:01:42 INFO [com.couchbase.core][BucketOpenedEvent][197ms] Opened bucket "Demo" {"coreId":"0x834eecc200000001"}
2022-08-22 22:01:45 INFO CQueuePerl Start
2022-08-22 22:01:45 INFO Performance Recorder Started
2022-08-22 22:01:45 INFO SBK Benchmark initiated Writers
2022-08-22 22:01:45 INFO Writer 0 started , run seconds: 60
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.4 MB,             3717 records,       743.3 records/sec,     0.07 MB/sec,      1.3 ms avg latency,      70 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       2 ms 70th,       2 ms 75th,       2 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       3 ms 97.5th,       3 ms 99th,       3 ms 99.25th,       3 ms 99.5th,       4 ms 99.75th,       5 ms 99.9th,      49 ms 99.95th,      70 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.4 MB,             4136 records,       827.0 records/sec,     0.08 MB/sec,      1.2 ms avg latency,      98 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  11; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       2 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       3 ms 99.25th,       3 ms 99.5th,       4 ms 99.75th,       7 ms 99.9th,       8 ms 99.95th,      98 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.5 MB,             4831 records,       966.0 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       5 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   1; Latency Percentiles:       0 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       3 ms 99.75th,       4 ms 99.9th,       4 ms 99.95th,       5 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.5 MB,             4785 records,       956.8 records/sec,     0.09 MB/sec,      1.0 ms avg latency,      57 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   4; Latency Percentiles:       0 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       3 ms 99.5th,       3 ms 99.75th,       4 ms 99.9th,       7 ms 99.95th,      57 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.4 MB,             4530 records,       904.2 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      60 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  11; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       3 ms 99.75th,       8 ms 99.9th,      44 ms 99.95th,      60 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.4 MB,             4400 records,       879.8 records/sec,     0.08 MB/sec,      1.1 ms avg latency,     137 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  19; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       3 ms 99.5th,       3 ms 99.75th,      11 ms 99.9th,      46 ms 99.95th,     137 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.4 MB,             4081 records,       815.9 records/sec,     0.08 MB/sec,      1.2 ms avg latency,     259 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  16; Latency Percentiles:       0 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       4 ms 99th,       4 ms 99.25th,       5 ms 99.5th,       6 ms 99.75th,      15 ms 99.9th,      30 ms 99.95th,     259 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.4 MB,             3823 records,       764.4 records/sec,     0.07 MB/sec,      1.3 ms avg latency,     292 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  48; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       3 ms 97.5th,       4 ms 99th,       4 ms 99.25th,       5 ms 99.5th,       8 ms 99.75th,      50 ms 99.9th,     165 ms 99.95th,     292 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.4 MB,             3718 records,       700.7 records/sec,     0.07 MB/sec,      1.4 ms avg latency,     313 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  31; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       2 ms 75th,       2 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       3 ms 97.5th,       5 ms 99th,       6 ms 99.25th,       8 ms 99.5th,      13 ms 99.75th,      26 ms 99.9th,      26 ms 99.95th,     313 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.3 MB,             3297 records,       575.8 records/sec,     0.05 MB/sec,      1.5 ms avg latency,     445 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  43; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       2 ms 70th,       2 ms 75th,       2 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       3 ms 97.5th,       4 ms 99th,       5 ms 99.25th,       5 ms 99.5th,       7 ms 99.75th,      11 ms 99.9th,     119 ms 99.95th,     445 ms 99.99th.
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,         0.4 MB,             3963 records,       792.4 records/sec,     0.08 MB/sec,      1.4 ms avg latency,     720 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  59; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       2 ms 75th,       2 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       3 ms 99th,       3 ms 99.25th,       4 ms 99.5th,       6 ms 99.75th,       7 ms 99.9th,      14 ms 99.95th,     720 ms 99.99th.
2022-08-22 22:02:45 INFO Writer 0 exited
Couchbase Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        3 seconds,         0.3 MB,             3473 records,       879.5 records/sec,     0.08 MB/sec,      1.1 ms avg latency,      17 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       3 ms 99.25th,       3 ms 99.5th,       4 ms 99.75th,       7 ms 99.9th,      14 ms 99.95th,      17 ms 99.99th.
Total : Couchbase Writing     0 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,       60 seconds,         4.6 MB,            48754 records,       812.6 records/sec,     0.08 MB/sec,      1.2 ms avg latency,     720 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  28; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       3 ms 99th,       3 ms 99.25th,       4 ms 99.5th,       5 ms 99.75th,      10 ms 99.9th,      26 ms 99.95th,     259 ms 99.99th.
2022-08-22 22:02:45 INFO Performance Recorder Exited
2022-08-22 22:02:45 INFO CQueuePerl Shutdown
2022-08-22 22:02:45 INFO [com.couchbase.transactions.cleanup][LogEvent] Waiting for 1 regular background threads to exit
2022-08-22 22:02:45 INFO [com.couchbase.transactions.cleanup.regular][LogEvent] Stopping background cleanup thread for transactions from this client
2022-08-22 22:02:45 INFO [com.couchbase.transactions.cleanup.lost][LogEvent] Client da036 stopping lost cleanup process, 0 threads running
2022-08-22 22:02:45 INFO [com.couchbase.transactions.cleanup.lost][LogEvent] Client da036 stopped lost cleanup process and removed client from client records
2022-08-22 22:02:45 INFO [com.couchbase.transactions.cleanup][LogEvent] Background threads have exitted
2022-08-22 22:02:45 INFO [com.couchbase.core][BucketClosedEvent][2622us] Closed bucket "Demo" {"coreId":"0x834eecc200000001"}
2022-08-22 22:02:45 INFO [com.couchbase.node][NodeDisconnectedEvent][2432us] Node disconnected {"coreId":"0x834eecc200000001","managerPort":"8091","remote":"localhost"}
2022-08-22 22:02:45 INFO [com.couchbase.core][ShutdownCompletedEvent][26ms] Completed shutdown and closed all open buckets {"coreId":"0x834eecc200000001"}
2022-08-22 22:02:45 INFO SBK PrometheusLogger Shutdown
2022-08-22 22:02:46 INFO SBK Benchmark Shutdown


sanjay@sanjay-Lap:~$ - SBK Driver/SBK (master)
$ ./build/install/sbk/bin/sbk.bat -class couchbase -readers 1 -size 100 -seconds 60 -url couchbase://localhost -password admin1 -bucket Demo
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/E:/Freelancing%20Work/Freelancing%20project%209%20-%20SBK%20Driver/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-08-22 22:03:17 INFO Reflections took 250 ms to scan 49 urls, producing 103 keys and 221 values
2022-08-22 22:03:17 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-08-22 22:03:17 INFO Storage Benchmark Kit
2022-08-22 22:03:17 INFO SBK Version: 1.0
2022-08-22 22:03:17 INFO SBK Website: https://github.com/kmgowda/SBK
2022-08-22 22:03:17 INFO Arguments List: [-class, couchbase, -readers, 1, -size, 100, -seconds, 60, -url, couchbase://localhost, -password, admin1, -bucket, Demo]
2022-08-22 22:03:17 INFO Java Runtime Version: 17.0.4.1+1-LTS-2
2022-08-22 22:03:17 INFO Storage Drivers Package: io.sbk
2022-08-22 22:03:17 INFO sbk.applicationName: sbk
2022-08-22 22:03:17 INFO sbk.appHome: E:\Freelancing Work\Freelancing project 9 - SBK Driver\SBK\build\install\sbk\bin\..
2022-08-22 22:03:17 INFO sbk.className:
2022-08-22 22:03:17 INFO '-class': couchbase
2022-08-22 22:03:17 INFO Available Storage Drivers in package 'io.sbk': 42 [Activemq,
Artemis, AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, Couchbase, CouchDB,
CSV, Db2, Derby, FdbRecord, File, FileStream, FoundationDB, H2, HDFS, Hive, Jdbc,
Kafka, LevelDB, MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null,
OpenIO, PostgreSQL, Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB,
SeaweedS3, SQLite]
2022-08-22 22:03:17 INFO Arguments to Driver 'Couchbase' : [-readers, 1, -size, 100, -seconds, 60, -url, couchbase://localhost, -password, admin1, -bucket, Demo]
2022-08-22 22:03:18 INFO Time Unit: MILLISECONDS
2022-08-22 22:03:18 INFO Minimum Latency: 0 ms
2022-08-22 22:03:18 INFO Maximum Latency: 180000 ms
2022-08-22 22:03:18 INFO Window Latency Store: Array, Size: 1 MB
2022-08-22 22:03:18 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-08-22 22:03:18 INFO Total Window Extension: None, Size: 0 MB
2022-08-22 22:03:18 INFO SBK Benchmark Started
2022-08-22 22:03:19 INFO SBK PrometheusLogger Started
2022-08-22 22:03:22 INFO [com.couchbase.core][DnsSrvLookupFailedEvent][130ms] DNS SRV lookup failed (name not found). This is expected if the there is no DNS SRV record associated with the hostname in the connection string. Will now try to bootstrap directly from the given hostname. To suppress this message, specify an IP address instead of a hostname (for example: 127.0.0.1 instead of localhost), specify more than one hostname, or set the `io.enableDnsSrv` client setting to false.
2022-08-22 22:03:23 INFO [com.couchbase.core][CoreCreatedEvent] {"clientVersion":"3.3.3","clientGitHash":"${buildNumber}","coreVersion":"2.3.3","coreGitHash":"${buildNumber}","userAgent":"couchbase-java/3.3.3 (Windows 11 10.0 amd64; Java HotSpot(TM) 64-Bit Server VM 17.0.4.1+1-LTS-2)","maxNumRequestsInRetry":32768,"ioEnvironment":{"nativeIoEnabled":true,"eventLoopThreadCount":4,"eventLoopGroups":["NioEventLoopGroup"]},"ioConfig":{"captureTraffic":[],"mutationTokensEnabled":true,"networkResolution":"auto","dnsSrvEnabled":true,"tcpKeepAlivesEnabled":true,"tcpKeepAliveTimeMs":60000,"configPollIntervalMs":2500,"kvCircuitBreakerConfig":"disabled","queryCircuitBreakerConfig":"disabled","viewCircuitBreakerConfig":"disabled","searchCircuitBreakerConfig":"disabled","analyticsCircuitBreakerConfig":"disabled","managerCircuitBreakerConfig":"disabled","eventingCircuitBreakerConfig":"disabled","backupCircuitBreakerConfig":"disabled","numKvConnections":1,"maxHttpConnections":12,"idleHttpConnectionTimeoutMs":4500,"configIdleRedialTimeoutMs":300000,"memcachedHashingStrategy":"StandardMemcachedHashingStrategy"},"compressionConfig":{"enabled":true,"minRatio":0.83,"minSize":32},"securityConfig":{"tlsEnabled":false,"nativeTlsEnabled":true,"hostnameVerificationEnabled":true,"trustCertificates":null,"trustManagerFactory":null,"ciphers":[]},"timeoutConfig":{"kvMs":2500,"kvDurableMs":10000,"managementMs":75000,"queryMs":75000,"viewMs":75000,"searchMs":75000,"analyticsMs":75000,"connectMs":10000,"disconnectMs":10000,"eventingMs":75000,"backupMs":75000},"loggerConfig":{"customLogger":null,"fallbackToConsole":false,"consoleLogLevel":{"name":"INFO","resourceBundleName":"sun.util.logging.resources.logging","localizedName":"INFO"},"consoleLoggerFormatter":"DefaultLoggerFormatter","disableSlf4j":false,"loggerName":"CouchbaseLogger","diagnosticContextEnabled":false},"orphanReporterConfig":{"emitIntervalMs":10000,"sampleSize":10,"queueLength":1024,"enabled":true},"thresholdLoggingTracerConfig":{"enabled":true,"emitIntervalMs":10000,"sampleSize":10,"queueLength":1024,"kvThresholdMs":500,"queryThresholdMs":1000,"searchThresholdMs":1000,"analyticsThresholdMs":1000,"viewThresholdMs":1000},"loggingMeterConfig":{"enabled":true,"emitIntervalMs":600000},"retryStrategy":"BestEffortRetryStrategy","requestTracer":"ThresholdLoggingTracer","meter":"LoggingMeter","numRequestCallbacks":0,"scheduler":"ParallelScheduler","schedulerThreadCount":8,"transactionsConfig":{"durabilityLevel":"MAJORITY","timeoutMs":15000,"cleanupConfig":{"runLostAttemptsCleanupThread":true,"runRegularAttemptsCleanupThread":true,"cleanupWindowMs":60000,"cleanupSet":""},"numAtrs":1024,"metadataCollection":"none","scanConsistency":"none"}} {"coreId":"0x6c07466300000001","seedNodes":[{"address":"localhost"}]}
2022-08-22 22:03:23 INFO [com.couchbase.transactions][TransactionsStartedEvent] Transactions successfully initialised, regular cleanup enabled=true, lost cleanup enabled=true
2022-08-22 22:03:23 INFO CQueuePerl Start
2022-08-22 22:03:23 INFO Performance Recorder Started
2022-08-22 22:03:23 INFO [com.couchbase.node][NodeConnectedEvent] Node connected {"coreId":"0x6c07466300000001","managerPort":"8091","remote":"localhost"}
2022-08-22 22:03:23 INFO SBK Benchmark initiated Readers
2022-08-22 22:03:23 INFO Reader 0 started , run seconds: 60
2022-08-22 22:03:24 INFO [com.couchbase.core][BucketOpenedEvent][694ms] Opened bucket "Demo" {"coreId":"0x6c07466300000001"}
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.3 MB,             2834 records,       566.7 records/sec,     0.05 MB/sec,      1.8 ms avg latency,    1183 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  88; Latency Percentiles:       1 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       2 ms 70th,       2 ms 75th,       2 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       3 ms 97.5th,       4 ms 99th,       4 ms 99.25th,       5 ms 99.5th,       6 ms 99.75th,      11 ms 99.9th,      12 ms 99.95th,    1183 ms 99.99th.
2022-08-22 22:03:32 WARN [com.couchbase.tracing][OverThresholdRequestsRecordedEvent][10s] Requests over Threshold found: {"kv":{"top_requests":[{"operation_name":"get","last_dispatch_duration_us":8436,"last_remote_socket":"localhost:11210","last_local_id":"6C07466300000001/0000000090FD154C","last_local_socket":"127.0.0.1:58674","total_dispatch_duration_us":8436,"operation_id":"0x1","timeout_ms":2500,"total_duration_us":1141730}],"total_count":1}}
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.5 MB,             4838 records,       967.4 records/sec,     0.09 MB/sec,      1.0 ms avg latency,       8 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   2; Latency Percentiles:       0 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       3 ms 99.75th,       6 ms 99.9th,       7 ms 99.95th,       8 ms 99.99th.
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.4 MB,             4391 records,       876.8 records/sec,     0.08 MB/sec,      1.1 ms avg latency,     273 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  21; Latency Percentiles:       0 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       4 ms 99th,       5 ms 99.25th,       9 ms 99.5th,      21 ms 99.75th,      34 ms 99.9th,      58 ms 99.95th,     273 ms 99.99th.
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.5 MB,             5152 records,      1030.0 records/sec,     0.10 MB/sec,      1.0 ms avg latency,     122 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:  10; Latency Percentiles:       0 ms 10th,       0 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       4 ms 99th,       4 ms 99.25th,       4 ms 99.5th,       8 ms 99.75th,      20 ms 99.9th,      31 ms 99.95th,     122 ms 99.99th.
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.5 MB,             4964 records,       992.6 records/sec,     0.09 MB/sec,      1.0 ms avg latency,      59 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:       0 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       3 ms 99th,       3 ms 99.25th,       3 ms 99.5th,       9 ms 99.75th,      18 ms 99.9th,      25 ms 99.95th,      59 ms 99.99th.
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.5 MB,             5368 records,      1073.2 records/sec,     0.10 MB/sec,      0.9 ms avg latency,      26 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:       0 ms 10th,       0 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       3 ms 99th,       4 ms 99.25th,       4 ms 99.5th,      10 ms 99.75th,      16 ms 99.9th,      23 ms 99.95th,      26 ms 99.99th.
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.5 MB,             5420 records,      1083.8 records/sec,     0.10 MB/sec,      0.9 ms avg latency,      14 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   2; Latency Percentiles:       0 ms 10th,       0 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       3 ms 99th,       3 ms 99.25th,       4 ms 99.5th,       4 ms 99.75th,       6 ms 99.9th,      11 ms 99.95th,      14 ms 99.99th.
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.5 MB,             5727 records,      1145.2 records/sec,     0.11 MB/sec,      0.9 ms avg latency,      24 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       0 ms 10th,       0 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       3 ms 99.75th,       7 ms 99.9th,       9 ms 99.95th,      24 ms 99.99th.
Couchbase Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,         0.5 MB,             5496 records,      1099.0 records/sec,     0.10 MB/sec,      0.9 ms avg latency,      21 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       0 ms 10th,       0 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       2 ms 99.5th,       3 ms 99.75th,      11 ms 99.9th,      17 ms 99.95th,      21 ms 99.99th.
2022-08-22 22:04:13 INFO Reader 0 exited with EOF
Couchbase Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,        4 seconds,         0.4 MB,             4568 records,      1024.7 records/sec,     0.10 MB/sec,      0.9 ms avg latency,      20 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   3; Latency Percentiles:       0 ms 10th,       0 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       1 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       2 ms 99th,       2 ms 99.25th,       3 ms 99.5th,       4 ms 99.75th,      12 ms 99.9th,      16 ms 99.95th,      20 ms 99.99th.
Total : Couchbase Reading     0 Writers,     0 Readers,      0 Max Writers,     1 Max Readers,       49 seconds,         4.6 MB,            48758 records,       985.5 records/sec,     0.09 MB/sec,      1.0 ms avg latency,    1183 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:       0 ms 10th,       1 ms 20th,       1 ms 25th,       1 ms 30th,       1 ms 40th,       1 ms 50th,       1 ms 60th,       1 ms 70th,       1 ms 75th,       1 ms 80th,       2 ms 90th,       2 ms 92.5th,       2 ms 95th,       2 ms 97.5th,       3 ms 99th,       3 ms 99.25th,       4 ms 99.5th,       6 ms 99.75th,      14 ms 99.9th,      21 ms 99.95th,      59 ms 99.99th.
2022-08-22 22:04:13 INFO Performance Recorder Exited
2022-08-22 22:04:13 INFO CQueuePerl Shutdown
2022-08-22 22:04:13 INFO [com.couchbase.transactions.cleanup][LogEvent] Waiting for 1 regular background threads to exit
2022-08-22 22:04:13 INFO [com.couchbase.transactions.cleanup.regular][LogEvent] Stopping background cleanup thread for transactions from this client
2022-08-22 22:04:13 INFO [com.couchbase.transactions.cleanup.lost][LogEvent] Client dfba5 stopping lost cleanup process, 0 threads running
2022-08-22 22:04:13 INFO [com.couchbase.transactions.cleanup.lost][LogEvent] Client dfba5 stopped lost cleanup process and removed client from client records
2022-08-22 22:04:13 INFO [com.couchbase.transactions.cleanup][LogEvent] Background threads have exitted
2022-08-22 22:04:13 INFO [com.couchbase.core][BucketClosedEvent][2278us] Closed bucket "Demo" {"coreId":"0x6c07466300000001"}
2022-08-22 22:04:13 INFO [com.couchbase.node][NodeDisconnectedEvent][1763us] Node disconnected {"coreId":"0x6c07466300000001","managerPort":"8091","remote":"localhost"}
2022-08-22 22:04:13 INFO [com.couchbase.core][ShutdownCompletedEvent][28ms] Completed shutdown and closed all open buckets {"coreId":"0x6c07466300000001"}
2022-08-22 22:04:13 INFO SBK PrometheusLogger Shutdown
2022-08-22 22:04:14 INFO SBK Benchmark Shutdown

```

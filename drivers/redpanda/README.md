<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Redpanda Performance benchmarking using SBK
The Redpanda driver for SBK supports multi writers and readers performance benchmarking. It also supports the End to End Latency benchmarking.
The Redpanda driver uses the Kafka for IO operations.

An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class redpanda -writers 1 -size 100 -seconds 120 
```

by default, the SBK uses the local Redpanda Broker: 127.0.0.1:9092, and default topic name is 'redpanda-1'


SSample SBK Redpanda write benchmarking output

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class redpanda -writers 1 -size 100 -seconds 120 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-31 18:07:03 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-31 18:07:03 INFO Java Runtime Version: 11.0.8+11
2021-01-31 18:07:03 INFO SBK Version: 0.851
2021-01-31 18:07:03 INFO Arguments List: [-class, redpanda, -writers, 1, -size, 100, -seconds, 120]
2021-01-31 18:07:03 INFO sbk.applicationName: sbk
2021-01-31 18:07:03 INFO sbk.className: 
2021-01-31 18:07:03 INFO Reflections took 56 ms to scan 33 urls, producing 49 keys and 164 values 
2021-01-31 18:07:03 INFO Available Drivers : 32
2021-01-31 18:07:03 INFO Arguments to Driver 'RedPanda' : [-writers, 1, -size, 100, -seconds, 120]
2021-01-31 18:07:03 INFO Time Unit: MILLISECONDS
2021-01-31 18:07:03 INFO Minimum Latency: 0 ms
2021-01-31 18:07:03 INFO Maximum Latency: 1000 ms
2021-01-31 18:07:03 INFO Maximum Latency for Reporting Interval: 100 ms
2021-01-31 18:07:03 INFO ProducerConfig values: 
	acks = all
	batch.size = 16384
	bootstrap.servers = [127.0.0.1:9092]
	buffer.memory = 33554432
	client.dns.lookup = default
	client.id = 
	compression.type = none
	connections.max.idle.ms = 540000
	delivery.timeout.ms = 120000
	enable.idempotence = false
	interceptor.classes = []
	key.serializer = class org.apache.kafka.common.serialization.ByteArraySerializer
	linger.ms = 0
	max.block.ms = 60000
	max.in.flight.requests.per.connection = 5
	max.request.size = 1048576
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partitioner.class = class org.apache.kafka.clients.producer.internals.DefaultPartitioner
	receive.buffer.bytes = 32768
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retries = 2147483647
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	security.providers = null
	send.buffer.bytes = 131072
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	transaction.timeout.ms = 60000
	transactional.id = null
	value.serializer = class org.apache.kafka.common.serialization.ByteArraySerializer

2021-01-31 18:07:03 INFO Kafka version: 2.4.1
2021-01-31 18:07:03 INFO Kafka commitId: c57222ae8cd7866b
2021-01-31 18:07:03 INFO Kafka startTimeMs: 1612096623824
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Got error produce response with correlation id 5 on topic-partition redpanda-1-0, retrying (2147483646 attempts left). Error: UNKNOWN_TOPIC_OR_PARTITION
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Received unknown topic or partition error in produce request on partition redpanda-1-0. The topic-partition may not exist or the user may not have Describe access to it
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Got error produce response with correlation id 6 on topic-partition redpanda-1-0, retrying (2147483646 attempts left). Error: UNKNOWN_TOPIC_OR_PARTITION
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Received unknown topic or partition error in produce request on partition redpanda-1-0. The topic-partition may not exist or the user may not have Describe access to it
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Got error produce response with correlation id 7 on topic-partition redpanda-1-0, retrying (2147483646 attempts left). Error: UNKNOWN_TOPIC_OR_PARTITION
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Received unknown topic or partition error in produce request on partition redpanda-1-0. The topic-partition may not exist or the user may not have Describe access to it
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Got error produce response with correlation id 8 on topic-partition redpanda-1-0, retrying (2147483646 attempts left). Error: NOT_LEADER_FOR_PARTITION
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Received invalid metadata error in produce request on partition redpanda-1-0 due to org.apache.kafka.common.errors.NotLeaderForPartitionException: This server is not the leader for that topic-partition.. Going to request metadata update now
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Got error produce response with correlation id 9 on topic-partition redpanda-1-0, retrying (2147483646 attempts left). Error: NOT_LEADER_FOR_PARTITION
2021-01-31 18:07:04 WARN [Producer clientId=producer-1] Received invalid metadata error in produce request on partition redpanda-1-0 due to org.apache.kafka.common.errors.NotLeaderForPartitionException: This server is not the leader for that topic-partition.. Going to request metadata update now
RedPanda Writing      634178 records,  126810.2 records/sec,    12.09 MB/sec,   1498.7 ms avg latency,    2119 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:     524 ms 10th,     992 ms 25th,    1725 ms 50th,    2070 ms 75th,    2113 ms 99th,    2117 ms 99.9th,    2118 ms 99.99th. 
RedPanda Writing      739113 records,  147763.5 records/sec,    14.09 MB/sec,   2016.4 ms avg latency,    2106 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1970 ms 10th,    1988 ms 25th,    2011 ms 50th,    2041 ms 75th,    2098 ms 99th,    2103 ms 99.9th,    2106 ms 99.99th. 
RedPanda Writing      701517 records,  140275.3 records/sec,    13.38 MB/sec,   2169.6 ms avg latency,    2327 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2042 ms 10th,    2055 ms 25th,    2158 ms 50th,    2282 ms 75th,    2321 ms 99th,    2325 ms 99.9th,    2326 ms 99.99th. 
RedPanda Writing      737933 records,  147557.1 records/sec,    14.07 MB/sec,   2074.5 ms avg latency,    2157 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2036 ms 10th,    2060 ms 25th,    2074 ms 50th,    2083 ms 75th,    2150 ms 99th,    2155 ms 99.9th,    2156 ms 99.99th. 
RedPanda Writing      707292 records,  141430.1 records/sec,    13.49 MB/sec,   2112.8 ms avg latency,    2276 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1984 ms 10th,    2043 ms 25th,    2089 ms 50th,    2203 ms 75th,    2271 ms 99th,    2275 ms 99.9th,    2275 ms 99.99th. 
RedPanda Writing      753463 records,  150662.5 records/sec,    14.37 MB/sec,   2032.1 ms avg latency,    2129 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1983 ms 10th,    1997 ms 25th,    2028 ms 50th,    2063 ms 75th,    2113 ms 99th,    2126 ms 99.9th,    2128 ms 99.99th. 
RedPanda Writing      756132 records,  151196.2 records/sec,    14.42 MB/sec,   1999.8 ms avg latency,    2073 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1942 ms 10th,    1958 ms 25th,    2007 ms 50th,    2035 ms 75th,    2070 ms 99th,    2071 ms 99.9th,    2072 ms 99.99th. 
RedPanda Writing      741781 records,  148326.5 records/sec,    14.15 MB/sec,   2031.7 ms avg latency,    2145 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1986 ms 10th,    1999 ms 25th,    2014 ms 50th,    2057 ms 75th,    2136 ms 99th,    2143 ms 99.9th,    2144 ms 99.99th. 
RedPanda Writing      762492 records,  152467.9 records/sec,    14.54 MB/sec,   2018.9 ms avg latency,    2126 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1944 ms 10th,    1981 ms 25th,    2005 ms 50th,    2081 ms 75th,    2121 ms 99th,    2124 ms 99.9th,    2125 ms 99.99th. 
RedPanda Writing      738968 records,  147764.0 records/sec,    14.09 MB/sec,   2037.4 ms avg latency,    2183 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1939 ms 10th,    1968 ms 25th,    1999 ms 50th,    2142 ms 75th,    2178 ms 99th,    2181 ms 99.9th,    2182 ms 99.99th. 
RedPanda Writing      745731 records,  149116.4 records/sec,    14.22 MB/sec,   2043.9 ms avg latency,    2143 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1976 ms 10th,    2007 ms 25th,    2043 ms 50th,    2073 ms 75th,    2121 ms 99th,    2141 ms 99.9th,    2142 ms 99.99th. 
RedPanda Writing      754397 records,  150849.2 records/sec,    14.39 MB/sec,   1988.1 ms avg latency,    2073 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1931 ms 10th,    1944 ms 25th,    1979 ms 50th,    2037 ms 75th,    2070 ms 99th,    2071 ms 99.9th,    2072 ms 99.99th. 
RedPanda Writing      706994 records,  141370.5 records/sec,    13.48 MB/sec,   2098.5 ms avg latency,    2221 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2018 ms 10th,    2038 ms 25th,    2102 ms 50th,    2147 ms 75th,    2215 ms 99th,    2219 ms 99.9th,    2220 ms 99.99th. 
RedPanda Writing      733490 records,  146639.3 records/sec,    13.98 MB/sec,   2119.1 ms avg latency,    2290 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1987 ms 10th,    2026 ms 25th,    2111 ms 50th,    2220 ms 75th,    2283 ms 99th,    2287 ms 99.9th,    2289 ms 99.99th. 
RedPanda Writing      639207 records,  127815.8 records/sec,    12.19 MB/sec,   2371.9 ms avg latency,    2761 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2036 ms 10th,    2080 ms 25th,    2321 ms 50th,    2702 ms 75th,    2755 ms 99th,    2759 ms 99.9th,    2761 ms 99.99th. 
RedPanda Writing      747257 records,  148767.1 records/sec,    14.19 MB/sec,   2034.3 ms avg latency,    2084 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2001 ms 10th,    2021 ms 25th,    2035 ms 50th,    2058 ms 75th,    2079 ms 99th,    2082 ms 99.9th,    2083 ms 99.99th. 
RedPanda Writing      718533 records,  143677.9 records/sec,    13.70 MB/sec,   2113.1 ms avg latency,    2203 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2048 ms 10th,    2061 ms 25th,    2098 ms 50th,    2172 ms 75th,    2198 ms 99th,    2200 ms 99.9th,    2201 ms 99.99th. 
RedPanda Writing      686875 records,  137320.1 records/sec,    13.10 MB/sec,   2164.1 ms avg latency,    2258 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2062 ms 10th,    2079 ms 25th,    2195 ms 50th,    2215 ms 75th,    2253 ms 99th,    2256 ms 99.9th,    2258 ms 99.99th. 
RedPanda Writing      684350 records,  136842.6 records/sec,    13.05 MB/sec,   2192.3 ms avg latency,    2332 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2122 ms 10th,    2148 ms 25th,    2177 ms 50th,    2243 ms 75th,    2324 ms 99th,    2330 ms 99.9th,    2332 ms 99.99th. 
RedPanda Writing      753761 records,  150722.1 records/sec,    14.37 MB/sec,   2099.6 ms avg latency,    2324 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1926 ms 10th,    1998 ms 25th,    2083 ms 50th,    2242 ms 75th,    2315 ms 99th,    2321 ms 99.9th,    2323 ms 99.99th. 
RedPanda Writing      761021 records,  152173.8 records/sec,    14.51 MB/sec,   1930.6 ms avg latency,    2081 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1871 ms 10th,    1881 ms 25th,    1923 ms 50th,    1982 ms 75th,    2052 ms 99th,    2076 ms 99.9th,    2079 ms 99.99th. 
RedPanda Writing      705365 records,  141044.8 records/sec,    13.45 MB/sec,   2185.2 ms avg latency,    2314 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2094 ms 10th,    2138 ms 25th,    2186 ms 50th,    2245 ms 75th,    2308 ms 99th,    2312 ms 99.9th,    2313 ms 99.99th. 
RedPanda Writing      736451 records,  147260.7 records/sec,    14.04 MB/sec,   2023.6 ms avg latency,    2176 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1959 ms 10th,    1983 ms 25th,    2003 ms 50th,    2032 ms 75th,    2171 ms 99th,    2174 ms 99.9th,    2176 ms 99.99th. 
RedPanda Writing      706994 records,  142769.4 records/sec,    13.62 MB/sec,   2117.8 ms avg latency,    2205 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    2052 ms 10th,    2062 ms 25th,    2121 ms 50th,    2168 ms 75th,    2197 ms 99th,    2201 ms 99.9th,    2204 ms 99.99th. 
RedPanda Writing(Total)     17353295 records,  144610.8 records/sec,    13.79 MB/sec,   2061.1 ms avg latency,    2761 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:    1957 ms 10th,    2000 ms 25th,    2056 ms 50th,    2137 ms 75th,    2693 ms 99th,    2746 ms 99.9th,    2759 ms 99.99th. 
2021-01-31 18:09:03 INFO SBK Performance Shutdown
2021-01-31 18:09:04 INFO [Producer clientId=producer-1] Closing the Kafka producer with timeoutMillis = 9223372036854775807 ms.
2021-01-31 18:09:07 INFO SBK Benchmark Shutdown


```
The sample SBK Redpanda read bencharking output is below
```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class redpanda -readers 1 -size 100 -seconds 120 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-31 18:09:15 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-31 18:09:15 INFO Java Runtime Version: 11.0.8+11
2021-01-31 18:09:15 INFO SBK Version: 0.851
2021-01-31 18:09:15 INFO Arguments List: [-class, redpanda, -readers, 1, -size, 100, -seconds, 120]
2021-01-31 18:09:15 INFO sbk.applicationName: sbk
2021-01-31 18:09:15 INFO sbk.className: 
2021-01-31 18:09:15 INFO Reflections took 63 ms to scan 33 urls, producing 49 keys and 164 values 
2021-01-31 18:09:15 INFO Available Drivers : 32
2021-01-31 18:09:15 INFO Arguments to Driver 'RedPanda' : [-readers, 1, -size, 100, -seconds, 120]
2021-01-31 18:09:15 INFO Time Unit: MILLISECONDS
2021-01-31 18:09:15 INFO Minimum Latency: 0 ms
2021-01-31 18:09:15 INFO Maximum Latency: 1000 ms
2021-01-31 18:09:15 INFO Maximum Latency for Reporting Interval: 100 ms
2021-01-31 18:09:15 INFO ConsumerConfig values: 
	allow.auto.create.topics = true
	auto.commit.interval.ms = 5000
	auto.offset.reset = earliest
	bootstrap.servers = [127.0.0.1:9092]
	check.crcs = true
	client.dns.lookup = default
	client.id = 
	client.rack = 
	connections.max.idle.ms = 540000
	default.api.timeout.ms = 60000
	enable.auto.commit = true
	exclude.internal.topics = true
	fetch.max.bytes = 52428800
	fetch.max.wait.ms = 500
	fetch.min.bytes = 1
	group.id = 1612096755784
	group.instance.id = null
	heartbeat.interval.ms = 3000
	interceptor.classes = []
	internal.leave.group.on.close = true
	isolation.level = read_committed
	key.deserializer = class org.apache.kafka.common.serialization.ByteArrayDeserializer
	max.partition.fetch.bytes = 1048576
	max.poll.interval.ms = 300000
	max.poll.records = 2147483647
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partition.assignment.strategy = [class org.apache.kafka.clients.consumer.RangeAssignor]
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	security.providers = null
	send.buffer.bytes = 131072
	session.timeout.ms = 10000
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	value.deserializer = class org.apache.kafka.common.serialization.ByteArrayDeserializer

2021-01-31 18:09:15 INFO Kafka version: 2.4.1
2021-01-31 18:09:15 INFO Kafka commitId: c57222ae8cd7866b
2021-01-31 18:09:15 INFO Kafka startTimeMs: 1612096755876
2021-01-31 18:09:15 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Subscribed to topic(s): redpanda-1
2021-01-31 18:09:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Discovered group coordinator 0.0.0.0:9092 (id: 2147483646 rack: null)
2021-01-31 18:09:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] (Re-)joining group
2021-01-31 18:09:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] (Re-)joining group
2021-01-31 18:09:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Finished assignment for group at generation 1: {consumer-1612096755784-1-1b0a3829-a9c2-4987-ae12-e1563c3c1bab=Assignment(partitions=[redpanda-1-0])}
2021-01-31 18:09:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Successfully joined group with generation 1
2021-01-31 18:09:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Adding newly assigned partitions: redpanda-1-0
2021-01-31 18:09:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Found no committed offset for partition redpanda-1-0
2021-01-31 18:09:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Resetting offset for partition redpanda-1-0 to offset 0.
RedPanda Reading     1247936 records,  249437.5 records/sec,    23.79 MB/sec,      4.7 ms avg latency,     289 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       4 ms 50th,       5 ms 75th,       7 ms 99th,       9 ms 99.9th,     289 ms 99.99th. 
RedPanda Reading     1381728 records,  276235.1 records/sec,    26.34 MB/sec,      4.2 ms avg latency,       7 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       4 ms 50th,       5 ms 75th,       6 ms 99th,       7 ms 99.9th,       7 ms 99.99th. 
RedPanda Reading     1371072 records,  274159.6 records/sec,    26.15 MB/sec,      4.3 ms avg latency,      11 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       3 ms 10th,       4 ms 25th,       4 ms 50th,       5 ms 75th,       7 ms 99th,      11 ms 99.9th,      11 ms 99.99th. 
RedPanda Reading     1255040 records,  250607.0 records/sec,    23.90 MB/sec,      4.7 ms avg latency,      14 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       4 ms 50th,       5 ms 75th,      10 ms 99th,      11 ms 99.9th,      14 ms 99.99th. 
RedPanda Reading     1105856 records,  220950.2 records/sec,    21.07 MB/sec,      5.3 ms avg latency,      24 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       5 ms 50th,       5 ms 75th,      15 ms 99th,      24 ms 99.9th,      24 ms 99.99th. 
RedPanda Reading     1167424 records,  233251.5 records/sec,    22.24 MB/sec,      5.0 ms avg latency,      15 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       5 ms 50th,       5 ms 75th,      11 ms 99th,      15 ms 99.9th,      15 ms 99.99th. 
RedPanda Reading     1268064 records,  253562.1 records/sec,    24.18 MB/sec,      4.6 ms avg latency,      12 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       4 ms 50th,       5 ms 75th,       9 ms 99th,      11 ms 99.9th,      12 ms 99.99th. 
RedPanda Reading     1238464 records,  247494.8 records/sec,    23.60 MB/sec,      4.7 ms avg latency,      15 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       5 ms 50th,       5 ms 75th,       9 ms 99th,      12 ms 99.9th,      15 ms 99.99th. 
RedPanda Reading     1296480 records,  258985.2 records/sec,    24.70 MB/sec,      4.5 ms avg latency,      12 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       4 ms 50th,       5 ms 75th,       6 ms 99th,       9 ms 99.9th,      12 ms 99.99th. 
RedPanda Reading      981536 records,  196267.9 records/sec,    18.72 MB/sec,      6.0 ms avg latency,      18 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       5 ms 25th,       5 ms 50th,       7 ms 75th,      13 ms 99th,      18 ms 99.9th,      18 ms 99.99th. 
RedPanda Reading     1139008 records,  227619.5 records/sec,    21.71 MB/sec,      5.2 ms avg latency,      16 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       5 ms 50th,       5 ms 75th,      13 ms 99th,      16 ms 99.9th,      16 ms 99.99th. 
RedPanda Reading     1089280 records,  217681.9 records/sec,    20.76 MB/sec,      5.4 ms avg latency,      20 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       5 ms 50th,       6 ms 75th,      13 ms 99th,      20 ms 99.9th,      20 ms 99.99th. 
RedPanda Reading      995744 records,  199069.2 records/sec,    18.98 MB/sec,      5.9 ms avg latency,      22 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       5 ms 25th,       5 ms 50th,       6 ms 75th,      14 ms 99th,      22 ms 99.9th,      22 ms 99.99th. 
RedPanda Reading     1019424 records,  203803.3 records/sec,    19.44 MB/sec,      5.8 ms avg latency,      17 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       5 ms 50th,       6 ms 75th,      14 ms 99th,      17 ms 99.9th,      17 ms 99.99th. 
RedPanda Reading     1100234 records,  205267.5 records/sec,    19.58 MB/sec,      4.8 ms avg latency,      14 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       5 ms 50th,       5 ms 75th,       8 ms 99th,      14 ms 99.9th,      14 ms 99.99th. 
RedPanda Reading           0 records,       0.0 records/sec,     0.00 MB/sec,      NaN ms avg latency,       0 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       0 ms 99.99th. 
RedPanda Reading(Total)     17657290 records,  145917.2 records/sec,    13.92 MB/sec,      5.0 ms avg latency,     289 ms max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       4 ms 10th,       4 ms 25th,       5 ms 50th,       5 ms 75th,      11 ms 99th,      17 ms 99.9th,      24 ms 99.99th. 
2021-01-31 18:11:16 INFO SBK Performance Shutdown
2021-01-31 18:11:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Revoke previously assigned partitions redpanda-1-0
2021-01-31 18:11:16 INFO [Consumer clientId=consumer-1612096755784-1, groupId=1612096755784] Member consumer-1612096755784-1-1b0a3829-a9c2-4987-ae12-e1563c3c1bab sending LeaveGroup request to coordinator 0.0.0.0:9092 (id: 2147483646 rack: null) due to the consumer is being closed
2021-01-31 18:11:17 INFO SBK Benchmark Shutdown

```

## Redpanda Dockers

Below docker command can be used launch the single Redpanda broker container.

```
docker run -ti -p 9092:9092 vectorized/redpanda

```
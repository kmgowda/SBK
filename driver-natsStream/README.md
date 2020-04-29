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
./build/install/sbk/bin/sbk -class Natsstream -uri nats://localhost:4222  -cluster test-cluster -topic kmg-topic-2 -size 100 -writers 1 -readers 1   -time 60
```


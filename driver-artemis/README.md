<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# ActiveMQ Artemis Driver for SBK
The Artemis driver for SBK supports multi writers , readers and End to End latency performance benchmarking.

## ActiveMQ Artemis standalone server installation 
Refer to this page : https://hub.docker.com/r/vromero/activemq-artemis to get the docker image of Artemis Server
or simple just execute the below command
```
docker run -p 61616:61616 -p 8161:8161 -e ARTEMIS_USERNAME=admin -e ARTEMIS_PASSWORD=admin -ti vromero/activemq-artemis
```
you can log in to Artemis console at : http://localhost:8161 with user name : admin (ARTEMIS_USERNAME), and password: admin (ARTEMIS_PASSWORD)

user name and passwords are very important create the authenticated sessions.

you need to have Dockers installed on your system.
An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class artemis -uri tcp://localhost:61616   -topic kmg-topic-3 -size 100 -writers 1 -readers 1   -time 60
```


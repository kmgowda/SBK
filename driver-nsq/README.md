<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# NSQ Driver for SBK
The NSQ driver for SBK supports benchmarking of multi writers , readers and end to end latency.

## NSQ standalone server installation 
Refer to this page : https://nsq.io/deployment/installing.html for nsq installation.
for docker deployment, refer to this page : https://nsq.io/deployment/docker.html

you need to have Dockers installed on your system.
An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class nsq -uri localhost:4150 -topic kmg-topic-4 -size 100  -writers 1  -seconds 60
```

<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SeaweedFS S3 compliant Driver for SBK
The SeaweedFS S3 driver extends the MinIO SBK driver for S3 compliant performance benchmarking.
This S3 Driver for SBK supports the multiple writers, readers performance benchmarking.
The End to End latency benchmarking is not supported.

## SeaweedFS S3 Performance benchmarking with SBK
See this page : https://github.com/chrislusf/seaweedfs to create the SeaweedFS S3 server and volumes.
Use SBK to do writers and readers performance benchmarking. user the option **-url**  to supply the ip address and 
port details of the SeaweedFS Server address. Example command is as follows

```
./build/install/sbk/bin/sbk -class seaweeds3 -url http://127.0.0.1:8333 -writers 1 -size 100 -seconds 60
```

## SeaweedFS S3 Performance benchmarking with the docker image

you can run the local SeaweedFS S3 driver with docker image
```
docker run -p 8333:8333 chrislusf/seaweedfs server -s3
```
you can run the below command to see the writer benchmarking uploading the objects into seaweedFS S3 Server
```
./build/install/sbk/bin/sbk -class seaweeds3 -writers 1 -size 100 -seconds 60
```
The above command creates the bucket named `sbk` with objects prefixed with `sbk-`

For read performance benchmarking, you run the below command.
```
./build/install/sbk/bin/sbk -class seaweeds3 -readers 1 -size 100 -seconds 60
```

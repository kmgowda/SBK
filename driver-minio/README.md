<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# MinIO Driver for SBK
The MinIO driver for SBK supports the multiple writers, readers performance benchmarking.
The End to End latency benchmarking is not supported. 

## MinIO performance benchmarking with https://play.min.io

you can log in to this page : https://play.min.io with key="Q3AM3UQ867SPQQA43P2F" and secret="zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG"
to see the existing buckets and objects.

you can run the below command to see the writer benchmarking uploading the objects into https://play.min.io
```
./build/install/sbk/bin/sbk -class minio -writers 1 -size 100 -seconds 60
```
The above command creates the bucket named `sbk` with objects prefixed with `sbk-`

For read performance benchmarking, you run the below command.
```
./build/install/sbk/bin/sbk -class minio -writers 1 -size 100 -seconds 60
```

The SBK Docker command writer benchmarking uploading the objects into https://play.min.io

```
docker run -p 127.0.0.1:8080:8080/tcp  kmgowda/sbk -class minio -writers 1 -size 10 -seconds 60
```

The SBK docker command to read the objects from https://play.min.io
```
docker run -p 127.0.0.1:8080:8080/tcp  kmgowda/sbk -class minio -readers 1 -size 10 -seconds 60
```

you can override the default access key and secret key by using the options `-key` and `-secret`

## MinIO docker performance benchmarking
you can start the MinIO docker Server with the below command:

```
docker run -p 9000:9000 --name minio1   -e "MINIO_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE"   -e "MINIO_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"   -v /mnt/data:/data   minio/minio server /data
```

Now, you can conduct write performance benchmarking with below command.

```
 ./build/install/sbk/bin/sbk -class minio -url http://127.0.0.1:9000 -key AKIAIOSFODNN7EXAMPLE -secret wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY  -writers 1 -size 100 -records 60 -recreate true
```

for reader performance benchmarking, you can run the below command

```
 ./build/install/sbk/bin/sbk -class minio -url http://127.0.0.1:9000 -key AKIAIOSFODNN7EXAMPLE -secret wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY  -readers 1 -size 100 -records 60 -recreate true
```

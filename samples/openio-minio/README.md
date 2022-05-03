- #### For MinIO performance benchmarking following commands were working perfectly fine, with option __-csvfile__.

1. To get write benchmarks.
```
./build/install/sbk/bin/sbk -class minio -writers 1 -size 100 -seconds 60 -csvfile file_name_write.csv
```
2. To get read benchmarks.
```
./build/install/sbk/bin/sbk -class minio -readers 1 -size 100 -seconds 60 -csvfile file_name_read.csv
```
-------

- #### For OpenIO performance benchmarking i used docker image and deployed OpenIO first.

As per OpenIO Documentation following commands i used to set it up.
1. To pull Docker Image
```
docker pull openio/sds:20.04
```
2. To run image and map the oioswift port ( here i used __-d__ option for detached mode )
```
docker run -d --name oio-sds -p 6007:6007 openio/sds
```
i also used this command to know private IP `docker inspect -f '{{ .NetworkSettings.IPAddress }}' oio-sds`
that's how i set up OpenIO image in my system.

- #### After that i used following commands to get benchmarking in  __.csv__ files.

1. To get write benchmarks.
```
./build/install/sbk/bin/sbk -class openio -url http://127.0.0.1:6007  -writers 1 -size 100 -seconds 60 -csvfile OpenIo_file_name_write.csv
```
2. To get read benchmarks.
```
./build/install/sbk/bin/sbk -class openio -url http://127.0.0.1:6007  -readers 1 -size 100 -seconds 60 -csvfile OpenIO_file_name_read.csv
```
------

- #### At last i used __sbk-chart__ application to create __.xlsx__ files.
----------
| openio_minio_writer_results.xlsx | [openio_minio_writer_results.xlsx](openio_minio_writer_results.xlsx)                              |
| ------- |---------------------------------------------------------------------------------------------------| 
| openio_minio_reader_results.xlsx | [openio_minio_reader_results.xlsx](openio_minio_reader_results.xlsx)                              |
| OpenIO_read_benchmarks.csv | [OpenIO_read_benchmarks.csv](OpenIO_read_benchmarks.csv)                                          |
| OpenIO_write_benchmarks.csv | [OpenIO_write_benchmarks.csv](OpenIO_write_benchmarks.csv) |
| minio_read_benchmarks.csv | [minio_read_benchmarks.csv](minio_read_benchmarks.csv) |
| minio_write_benchmarks.csv | [minio_write_benchmarks.csv](minio_write_benchmarks.csv) |

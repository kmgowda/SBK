<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# File System Benchmarking with SBK
The File system driver for SBK supports single Writer , single reader and multiple readers performance benchmarking.
The End to End Latency Performance benchmarking is not supported.

## File write Benchmarking with SBK and FIO
The FIO (Flexible I/O tester) supports multiple files writing at a time. whereas SBK uses single file for write/read operation.
Both FIO And SBK can be used with Write operations with buffering and writes can be with in sync (Sync to file system).

An Example SBK command for file write with sync (flush) enabled is as follows

```
 ./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 5000 -flush 1
```
In the above example, the file size of 5GB (Giga Bytes) are written with 1048576 (1MB) block/record size.
The data is flushed for every block/record (1MB in this example) write.  The output is as follows 

```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 5000 -flush 1
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-05-12 01:52:28 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-05-12 01:52:28 INFO SBK version: 0.75
2020-05-12 01:52:28 INFO Reflections took 61 ms to scan 16 urls, producing 22 keys and 79 values
Writing        543 records,     104.7 records/sec,   104.73 MB/sec,      0.8 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       4 ms 99.9th,       4 ms 99.99th.

Writing        524 records,     104.8 records/sec,   104.78 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.7 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.7 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.96 MB/sec,      0.7 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.7 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.7 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.96 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.7 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing(Total)       5000 records,     502.3 records/sec,   502.26 MB/sec,      0.7 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       4 ms 99.99th.
```

An equivalent command in FIO to sequentially write 5GB of data with 1MB block size to a single file with sync enabled is as follows.
```
fio --name=write --iodepth=1 --rw=write --bs=1048576 --size=10G --numjobs=1  --group_reporting  --nrfiles=1 --sync=1
```
The output is as follows:
```
fio --name=write --iodepth=1 --rw=write --bs=1048576 --size=10G --numjobs=1  --group_reporting  --nrfiles=1 --sync=1

write: (g=0): rw=write, bs=(R) 1024KiB-1024KiB, (W) 1024KiB-1024KiB, (T) 1024KiB-1024KiB, ioengine=psync, iodepth=1
fio-3.1
Starting 1 process
Jobs: 1 (f=1): [W(1)][100.0%][r=0KiB/s,w=468MiB/s][r=0,w=468 IOPS][eta 00m:00s]
write: (groupid=0, jobs=1): err= 0: pid=242719: Tue May 12 08:00:26 2020
  write: IOPS=465, BW=465MiB/s (488MB/s)(10.0GiB/22014msec)
    clat (usec): min=790, max=654069, avg=2009.05, stdev=13176.78
     lat (usec): min=805, max=654089, avg=2036.18, stdev=13176.90
    clat percentiles (usec):
     |  1.00th=[   807],  5.00th=[   824], 10.00th=[   840], 20.00th=[   889],
     | 30.00th=[  1106], 40.00th=[  1565], 50.00th=[  1647], 60.00th=[  1795],
     | 70.00th=[  1893], 80.00th=[  2089], 90.00th=[  2933], 95.00th=[  3097],
     | 99.00th=[  3392], 99.50th=[  3458], 99.90th=[  9110], 99.95th=[ 71828],
     | 99.99th=[608175]
   bw (  KiB/s): min=30720, max=1036288, per=100.00%, avg=511229.49, stdev=204741.38, samples=41
   iops        : min=   30, max= 1012, avg=499.24, stdev=199.94, samples=41
  lat (usec)   : 1000=26.24%
  lat (msec)   : 2=49.95%, 4=23.49%, 10=0.23%, 20=0.03%, 100=0.01%
  lat (msec)   : 750=0.05%
  cpu          : usr=1.58%, sys=40.66%, ctx=30721, majf=0, minf=887
  IO depths    : 1=100.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     issued rwt: total=0,10240,0, short=0,0,0, dropped=0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=1

Run status group 0 (all jobs):
  WRITE: bw=465MiB/s (488MB/s), 465MiB/s-465MiB/s (488MB/s-488MB/s), io=10.0GiB (10.7GB), run=22014-22014msec

Disk stats (read/write):
  sde: ios=0/50910, merge=0/0, ticks=0/24191, in_queue=24167, util=66.78%

```

With FIO, you can change the io engines too. Typically, io engine `psync` with `thread` option is equivalent with SBK.
An example,

```
fio --name=write --ioengine=psync --iodepth=1 --rw=write --bs=1048576 --size=5G --numjobs=1  --group_reporting  --sync=1 --thread --nrfiles=1
```

The SBK can be used with buffered writes and reads. An example command of buffered write without -flush option is as follows.
```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 100000
```

output for 100GB buffered file write is as follows
```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 100000
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-05-12 06:51:13 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-05-12 06:51:13 INFO SBK version: 0.75
2020-05-12 06:51:13 INFO Reflections took 63 ms to scan 16 urls, producing 22 keys and 79 values
Writing       7150 records,    1429.4 records/sec,  1429.43 MB/sec,      0.7 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       4 ms 99.99th.
Writing       7434 records,    1486.2 records/sec,  1486.21 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       2 ms 99.99th.
Writing       7392 records,    1478.1 records/sec,  1478.10 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       2 ms 99.99th.
Writing       7272 records,    1454.1 records/sec,  1454.11 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       6688 records,    1337.3 records/sec,  1337.33 MB/sec,      0.7 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       3 ms 99.99th.
Writing       6730 records,    1344.9 records/sec,  1344.92 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       6764 records,    1352.5 records/sec,  1352.53 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       6812 records,    1362.1 records/sec,  1362.13 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       7285 records,    1456.7 records/sec,  1456.71 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       6703 records,    1340.3 records/sec,  1340.33 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       6635 records,    1326.7 records/sec,  1326.73 MB/sec,      0.8 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       6717 records,    1343.1 records/sec,  1343.13 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       6702 records,    1340.1 records/sec,  1340.13 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing       6754 records,    1350.5 records/sec,  1350.53 MB/sec,      0.7 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing(Total)     100000 records,    1384.9 records/sec,  1384.87 MB/sec,      0.7 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       2 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
```

the fio buffered write with pthreads is as follows
```
fio --name=write --ioengine=psync --iodepth=1 --rw=write --bs=1048576 --size=100G --numjobs=1  --group_reporting  --thread --nrfiles=1
```
The output for buffered write without `sync` option for 100GB file as follows

```
fio --name=write --ioengine=psync --iodepth=1 --rw=write --bs=1048576 --size=100G --numjobs=1  --group_reporting  --thread --nrfiles=1

write: (g=0): rw=write, bs=(R) 1024KiB-1024KiB, (W) 1024KiB-1024KiB, (T) 1024KiB-1024KiB, ioengine=psync, iodepth=1
fio-3.1
Starting 1 thread
Jobs: 1 (f=1): [W(1)][100.0%][r=0KiB/s,w=1883MiB/s][r=0,w=1882 IOPS][eta 00m:00s]
write: (groupid=0, jobs=1): err= 0: pid=240475: Tue May 12 07:21:22 2020
  write: IOPS=1829, BW=1830MiB/s (1919MB/s)(100GiB/55957msec)
    clat (usec): min=376, max=1279, avg=441.51, stdev=55.54
     lat (usec): min=389, max=1293, avg=454.53, stdev=55.59
    clat percentiles (usec):
     |  1.00th=[  396],  5.00th=[  400], 10.00th=[  400], 20.00th=[  404],
     | 30.00th=[  408], 40.00th=[  408], 50.00th=[  412], 60.00th=[  416],
     | 70.00th=[  486], 80.00th=[  498], 90.00th=[  506], 95.00th=[  515],
     | 99.00th=[  635], 99.50th=[  660], 99.90th=[  750], 99.95th=[  799],
     | 99.99th=[  824]
   bw (  MiB/s): min=  666, max= 2404, per=100.00%, avg=2183.97, stdev=260.34, samples=93
   iops        : min=  666, max= 2404, avg=2183.94, stdev=260.34, samples=93
  lat (usec)   : 500=81.15%, 750=18.75%, 1000=0.09%
  lat (msec)   : 2=0.01%
  cpu          : usr=2.59%, sys=97.41%, ctx=85, majf=0, minf=1927
  IO depths    : 1=100.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     issued rwt: total=0,102400,0, short=0,0,0, dropped=0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=1

Run status group 0 (all jobs):
  WRITE: bw=1830MiB/s (1919MB/s), 1830MiB/s-1830MiB/s (1919MB/s-1919MB/s), io=100GiB (107GB), run=55957-55957msec

Disk stats (read/write):
  sde: ios=0/89881, merge=0/3, ticks=0/5184325, in_queue=5188927, util=65.26%

```
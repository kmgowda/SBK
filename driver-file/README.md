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
The FIO (Flexible I/O tester) supports multiple files writing at a time. where as SBK uses single file for write/read operation.
Both FIO And SBK can be used with Write operations with buffering and  writes can be with in sync (Sync to file system).

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
fio --name=write --iodepth=1 --rw=write --bs=1048576 --size=5G --numjobs=1  --group_reporting  --nrfiles=1 --sync=1
```
The output is as follows:
```
fio --name=write --iodepth=1 --rw=write --bs=1048576 --size=5G --numjobs=1  --group_reporting  --nrfiles=1 --sync=1
write: (g=0): rw=write, bs=(R) 1024KiB-1024KiB, (W) 1024KiB-1024KiB, (T) 1024KiB-1024KiB, ioengine=psync, iodepth=1
fio-3.1
Starting 1 process
Jobs: 1 (f=1): [W(1)][90.9%][r=0KiB/s,w=542MiB/s][r=0,w=542 IOPS][eta 00m:01s]
write: (groupid=0, jobs=1): err= 0: pid=157689: Tue May 12 01:53:43 2020
  write: IOPS=481, BW=482MiB/s (505MB/s)(5120MiB/10624msec)
    clat (usec): min=789, max=618716, avg=1934.94, stdev=13410.20
     lat (usec): min=800, max=618738, avg=1960.91, stdev=13410.15
    clat percentiles (usec):
     |  1.00th=[   807],  5.00th=[   832], 10.00th=[   848], 20.00th=[   881],
     | 30.00th=[   971], 40.00th=[  1254], 50.00th=[  1565], 60.00th=[  1680],
     | 70.00th=[  1811], 80.00th=[  1991], 90.00th=[  2868], 95.00th=[  3032],
     | 99.00th=[  3359], 99.50th=[  3458], 99.90th=[  9634], 99.95th=[434111],
     | 99.99th=[616563]
   bw (  KiB/s): min=151552, max=921600, per=100.00%, avg=517734.40, stdev=184686.84, samples=20
   iops        : min=  148, max=  900, avg=505.60, stdev=180.36, samples=20
  lat (usec)   : 1000=31.91%
  lat (msec)   : 2=48.46%, 4=19.28%, 10=0.27%, 100=0.02%, 500=0.02%
  lat (msec)   : 750=0.04%
  cpu          : usr=1.56%, sys=41.59%, ctx=15443, majf=0, minf=560
  IO depths    : 1=100.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     issued rwt: total=0,5120,0, short=0,0,0, dropped=0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=1

Run status group 0 (all jobs):
  WRITE: bw=482MiB/s (505MB/s), 482MiB/s-482MiB/s (505MB/s-505MB/s), io=5120MiB (5369MB), run=10624-10624msec

Disk stats (read/write):
  sde: ios=0/25038, merge=0/1, ticks=0/10498, in_queue=10478, util=60.98%
```

With FIO, you can change the io engines too. Typically, io engine `psync` with `thread` option is equivalent with SBK.
An example,

```
fio --name=write --ioengine=psync --iodepth=1 --rw=write --bs=1048576 --size=5G --numjobs=1  --group_reporting  --sync=1 --thread --nrfiles=1
```


<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# File System Benchmarking with SBK using File channel write/Read APIs
The File system driver for SBK supports single Writer , single reader and multiple readers performance benchmarking.
SBK does not support the End to End latency for file system stream benchmarking.

## File write Benchmarking with SBK and FIO
The FIO (Flexible I/O tester) supports multiple files writing at a time. whereas SBK uses single file for write/read operation.
Both FIO And SBK can be used with Write operations with buffering and writes can be with in sync (Sync to file system).

An Example SBK command for file write with sync (flush) enabled is as follows

```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 10000 -flush 1
```
In the above example, the file size of 10 GB (Giga Bytes) are written with 1048576 (1MB) block/record size.
The data is flushed for every block/record (1MB in this example) write.  The output is as follows 
```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 10000 -flush 1
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-06-09 04:07:07 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-06-09 04:07:07 INFO SBK version: 0.77
2020-06-09 04:07:07 INFO Reflections took 58 ms to scan 18 urls, producing 22 keys and 87 values
Writing        542 records,     104.7 records/sec,   104.67 MB/sec,      0.5 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.5 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       2 ms 99.9th,       2 ms 99.99th.
Writing        526 records,     105.1 records/sec,   105.07 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.96 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        526 records,     105.2 records/sec,   105.18 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        526 records,     105.2 records/sec,   105.16 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        526 records,     105.1 records/sec,   105.07 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        526 records,     105.2 records/sec,   105.18 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        526 records,     105.2 records/sec,   105.18 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        526 records,     105.2 records/sec,   105.18 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        525 records,     105.0 records/sec,   104.98 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        526 records,     105.2 records/sec,   105.18 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing        527 records,     105.4 records/sec,   105.38 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing(Total)      10000 records,     455.7 records/sec,   455.71 MB/sec,      0.5 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       2 ms 99.99th.
```

An equivalent command in FIO to sequentially write 10 GB of data with 1MB block size to a single file with sync enabled is as follows.
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

The SBK can be used with  writes and reads. An example command of write without -flush option is as follows.
```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 100000
```

output for 100GB file write is as follows
```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 100000
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-06-09 04:10:21 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-06-09 04:10:21 INFO SBK version: 0.77
2020-06-09 04:10:21 INFO Reflections took 58 ms to scan 18 urls, producing 22 keys and 87 values
Writing      11252 records,    2250.0 records/sec,  2249.95 MB/sec,      0.4 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing      11428 records,    2285.1 records/sec,  2285.14 MB/sec,      0.4 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing      11302 records,    2259.9 records/sec,  2259.95 MB/sec,      0.4 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing      11112 records,    2222.0 records/sec,  2221.96 MB/sec,      0.4 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing      11193 records,    2238.2 records/sec,  2238.15 MB/sec,      0.4 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing      11369 records,    2273.3 records/sec,  2273.35 MB/sec,      0.4 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing      10953 records,    2190.2 records/sec,  2190.16 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing      10887 records,    2176.1 records/sec,  2176.09 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing       9850 records,    1969.6 records/sec,  1969.61 MB/sec,      0.5 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Writing(Total)     100000 records,    2205.3 records/sec,  2205.27 MB/sec,      0.5 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       1 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.

```

The fio write with pthreads is as follows
```
fio --name=write --ioengine=psync --iodepth=1 --rw=write --bs=1048576 --size=100G --numjobs=1  --group_reporting  --thread --nrfiles=1
```
The output for write without `sync` option for 100GB file as follows

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

The SBK file read command is as follows:
```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -readers 1 -records 100000
```

output for 100GB file write is as follows
```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -readers 1 -records 100000
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/data/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2020-06-09 04:38:26 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2020-06-09 04:38:26 INFO SBK version: 0.77
2020-06-09 04:38:27 INFO Reflections took 57 ms to scan 18 urls, producing 22 keys and 87 values
Reading      25722 records,    5143.4 records/sec,  5143.37 MB/sec,      0.2 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Reading      25819 records,    5162.8 records/sec,  5162.77 MB/sec,      0.2 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Reading      25847 records,    5168.4 records/sec,  5168.37 MB/sec,      0.2 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.
Reading(Total)     100000 records,    5064.1 records/sec,  5064.06 MB/sec,      0.2 ms avg latency,       2 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       1 ms 95th,       1 ms 99th,       1 ms 99.9th,       1 ms 99.99th.

```

The fio read is as follows
```
fio --name=read  --iodepth=1 --rw=read --bs=1048576 --size=100G --numjobs=1  --group_reporting  --thread --nrfiles=1
```
The output for read  for 100GB file as follows

```
fio --name=read  --iodepth=1 --rw=read --bs=1048576 --size=100G --numjobs=1  --group_reporting   --nrfiles=1     read: (g=0): rw=read, bs=(R) 1024KiB-1024KiB, (W) 1024KiB-1024KiB, (T) 1024KiB-1024KiB, ioengine=psync, iodepth=1
fio-3.1
Starting 1 process
Jobs: 1 (f=1): [R(1)][99.5%][r=497MiB/s,w=0KiB/s][r=497,w=0 IOPS][eta 00m:01s]
read: (groupid=0, jobs=1): err= 0: pid=737513: Tue Jun  9 23:44:25 2020
   read: IOPS=523, BW=524MiB/s (549MB/s)(100GiB/195505msec)
    clat (usec): min=159, max=132771, avg=1820.71, stdev=4890.83
     lat (usec): min=159, max=132771, avg=1820.84, stdev=4890.83
    clat percentiles (usec):
     |  1.00th=[  273],  5.00th=[  289], 10.00th=[  302], 20.00th=[  314],
     | 30.00th=[  326], 40.00th=[  343], 50.00th=[  359], 60.00th=[  375],
     | 70.00th=[  392], 80.00th=[ 2835], 90.00th=[ 6325], 95.00th=[ 6980],
     | 99.00th=[ 9896], 99.50th=[42730], 99.90th=[61604], 99.95th=[67634],
     | 99.99th=[82314]
   bw (  KiB/s): min=51200, max=1150976, per=100.00%, avg=560739.66, stdev=56052.56, samples=374
   iops        : min=   50, max= 1124, avg=547.59, stdev=54.74, samples=374
  lat (usec)   : 250=0.40%, 500=74.65%, 750=0.55%, 1000=0.25%
  lat (msec)   : 2=1.02%, 4=7.83%, 10=14.31%, 20=0.12%, 50=0.55%
  lat (msec)   : 100=0.32%, 250=0.01%
  cpu          : usr=0.11%, sys=30.14%, ctx=57450, majf=0, minf=2477
  IO depths    : 1=100.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     issued rwt: total=102400,0,0, short=0,0,0, dropped=0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=1

Run status group 0 (all jobs):
   READ: bw=524MiB/s (549MB/s), 524MiB/s-524MiB/s (549MB/s-549MB/s), io=100GiB (107GB), run=195505-195505msec

Disk stats (read/write):
  sde: ios=408744/0, merge=0/0, ticks=9887857/0, in_queue=9886977, util=94.47%

```

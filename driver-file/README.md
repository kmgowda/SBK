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

An Example SBK command for file write with sync (sync) enabled is as follows

```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 10000 -sync 1
```
In the above example, the file size of 10 GB (Giga Bytes) are written with 1048576 (1MB) block/record size.
The data is flushed for every block/record (1MB in this example) write.  The output is as follows 
```
./build/install/sbk/bin/sbk -class file -file tmp.txt -size 1048576  -writers 1 -records 10000 -sync 1
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

The SBK can be used with  writes and reads. An example command of write without -sync option is as follows.
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


Sample file system benchmarking on windows

Single File system Writer Benchmark results

```
C:\kmg\SBK>build\install\sbk\bin\sbk -class file -writers 1 -size 100 -time ns -seconds 120
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/C:/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/C:/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/C:/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-03-18 19:21:10 INFO Reflections took 113 ms to scan 46 urls, producing 98 keys and 218 values
2022-03-18 19:21:10 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-03-18 19:21:10 INFO Storage Benchmark Kit
2022-03-18 19:21:10 INFO SBK Version: 0.98
2022-03-18 19:21:10 INFO SBK Website: https://github.com/kmgowda/SBK
2022-03-18 19:21:10 INFO Arguments List: [-class, file, -writers, 1, -size, 100, -time, ns, -seconds, 120]
2022-03-18 19:21:10 INFO Java Runtime Version: 17.0.2+8-LTS-86
2022-03-18 19:21:10 INFO Storage Drivers Package: io.sbk
2022-03-18 19:21:10 INFO sbk.applicationName: sbk
2022-03-18 19:21:10 INFO sbk.appHome: C:\kmg\SBK\build\install\sbk\bin\..
2022-03-18 19:21:10 INFO sbk.className:
2022-03-18 19:21:10 INFO '-class': file
2022-03-18 19:21:10 INFO Available Storage Drivers in package 'io.sbk': 40 [Artemis,
AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, Derby,
FdbRecord, File, FileStream, FoundationDB, HDFS, Hive, Ignite, Jdbc, Kafka, LevelDB,
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL,
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-03-18 19:21:10 INFO Arguments to Driver 'File' : [-writers, 1, -size, 100, -time, ns, -seconds, 120]
2022-03-18 19:21:10 INFO Time Unit: NANOSECONDS
2022-03-18 19:21:10 INFO Minimum Latency: 0 ns
2022-03-18 19:21:10 INFO Maximum Latency: 180000000000 ns
2022-03-18 19:21:10 INFO Window Latency Store: HashMap, Size: 192 MB
2022-03-18 19:21:10 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-03-18 19:21:10 INFO Total Window Extension: None, Size: 0 MB
2022-03-18 19:21:10 INFO SBK Benchmark Started
2022-03-18 19:21:10 INFO SBK PrometheusLogger Started
2022-03-18 19:21:10 INFO CQueuePerl Start
2022-03-18 19:21:10 INFO Performance Recorder Started
2022-03-18 19:21:10 INFO SBK Benchmark initiated Writers
2022-03-18 19:21:10 INFO Writer 0 started , run seconds: 120
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       155.1 MB,          1626521 records,    325239.1 records/sec,    31.02 MB/sec,   2973.3 ns avg latency, 4040500 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2700 ns 30th,    5000 ns 40th,    6300 ns 50th,   21700 ns 60th,   71300 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       159.1 MB,          1668729 records,    333678.9 records/sec,    31.82 MB/sec,   2926.3 ns avg latency, 5631000 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2700 ns 30th,    5000 ns 40th,    5700 ns 50th,   19600 ns 60th,   67200 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       158.6 MB,          1662550 records,    332443.0 records/sec,    31.70 MB/sec,   2934.8 ns avg latency, 25568800 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2700 ns 30th,    5000 ns 40th,    6000 ns 50th,   20300 ns 60th,   68300 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       167.9 MB,          1761080 records,    352145.5 records/sec,    33.58 MB/sec,   2770.5 ns avg latency, 4058800 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4200 ns 40th,    5300 ns 50th,   19700 ns 60th,   62400 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       164.3 MB,          1723331 records,    344597.2 records/sec,    32.86 MB/sec,   2829.9 ns avg latency, 2764800 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5300 ns 50th,   20100 ns 60th,   66700 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       162.4 MB,          1703126 records,    340556.9 records/sec,    32.48 MB/sec,   2866.3 ns avg latency, 5179600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5800 ns 50th,   20500 ns 60th,   68100 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       165.3 MB,          1732903 records,    346511.1 records/sec,    33.05 MB/sec,   2816.4 ns avg latency, 3045100 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5400 ns 50th,   19700 ns 60th,   65400 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       168.0 MB,          1761508 records,    352231.0 records/sec,    33.59 MB/sec,   2770.7 ns avg latency, 5376200 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4100 ns 40th,    5200 ns 50th,   20200 ns 60th,   64100 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       163.4 MB,          1713316 records,    342594.5 records/sec,    32.67 MB/sec,   2849.7 ns avg latency, 3880400 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5700 ns 50th,   20300 ns 60th,   68700 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       160.8 MB,          1686083 records,    337147.7 records/sec,    32.15 MB/sec,   2895.4 ns avg latency, 26976300 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    6000 ns 50th,   20900 ns 60th,   68500 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       163.3 MB,          1712363 records,    342403.7 records/sec,    32.65 MB/sec,   2850.2 ns avg latency, 1871200 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    6600 ns 50th,   20600 ns 60th,   67700 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       164.3 MB,          1722974 records,    344525.6 records/sec,    32.86 MB/sec,   2833.6 ns avg latency, 6030600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4800 ns 40th,    5900 ns 50th,   20700 ns 60th,   68100 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       169.5 MB,          1777666 records,    355462.1 records/sec,    33.90 MB/sec,   2745.1 ns avg latency, 1561800 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4100 ns 40th,    5100 ns 50th,   20300 ns 60th,   62900 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       164.2 MB,          1722042 records,    344339.4 records/sec,    32.84 MB/sec,   2835.4 ns avg latency, 26253600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4700 ns 40th,    5500 ns 50th,   21200 ns 60th,   67800 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       160.5 MB,          1683434 records,    336619.3 records/sec,    32.10 MB/sec,   2900.4 ns avg latency, 26945800 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5700 ns 50th,   21000 ns 60th,   68700 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       168.9 MB,          1770655 records,    354060.1 records/sec,    33.77 MB/sec,   2757.3 ns avg latency, 1706600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4100 ns 40th,    5200 ns 50th,   20500 ns 60th,   63100 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       165.3 MB,          1733473 records,    346624.2 records/sec,    33.06 MB/sec,   2816.1 ns avg latency, 1766500 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   7; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4700 ns 40th,    6000 ns 50th,   20600 ns 60th,   67200 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       161.5 MB,          1693851 records,    338702.4 records/sec,    32.30 MB/sec,   2883.0 ns avg latency, 26372600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2400 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    6400 ns 50th,   22000 ns 60th,   69300 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       164.1 MB,          1720578 records,    344046.4 records/sec,    32.81 MB/sec,   2836.5 ns avg latency, 5006100 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    6500 ns 50th,   22000 ns 60th,   69400 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       165.7 MB,          1736991 records,    347328.7 records/sec,    33.12 MB/sec,   2810.8 ns avg latency, 5256700 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4800 ns 40th,    5300 ns 50th,   21700 ns 60th,   68500 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       162.4 MB,          1702833 records,    340498.2 records/sec,    32.47 MB/sec,   2866.3 ns avg latency, 4518100 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5900 ns 50th,   21900 ns 60th,   69800 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       160.5 MB,          1683065 records,    336545.3 records/sec,    32.10 MB/sec,   2900.7 ns avg latency, 1749100 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    5000 ns 40th,    5700 ns 50th,   22100 ns 60th,   70500 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        5 seconds,       161.1 MB,          1689679 records,    337868.2 records/sec,    32.22 MB/sec,   2888.5 ns avg latency, 4973500 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5900 ns 50th,   23400 ns 60th,   73200 ns 70th.
File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,        4 seconds,       159.2 MB,          1669166 records,    335381.7 records/sec,    31.98 MB/sec,   2911.3 ns avg latency, 21113100 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5800 ns 50th,   23400 ns 60th,   72000 ns 70th.
Total : File Writing     1 Writers,     0 Readers,      1 Max Writers,     0 Max Readers,      120 seconds,      3915.6 MB,         41057917 records,    342149.3 records/sec,    32.63 MB/sec,   2851.7 ns avg latency, 26976300 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   8; Latency Percentiles:    2400 ns 10th,    2500 ns 20th,    2500 ns 25th,    2600 ns 30th,    4900 ns 40th,    5700 ns 50th,   21600 ns 60th,   67900 ns 70th.
2022-03-18 19:23:10 INFO Performance Recorder Exited
2022-03-18 19:23:10 INFO CQueuePerl Shutdown
2022-03-18 19:23:11 INFO Writer 0 exited
2022-03-18 19:23:11 INFO SBK PrometheusLogger Shutdown
2022-03-18 19:23:12 INFO SBK Benchmark Shutdown
```


single file system reader benchmark results

```
C:\kmg\SBK>build\install\sbk\bin\sbk -class file -readers 1 -size 100 -time ns -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/C:/kmg/SBK/build/install/sbk/lib/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/C:/kmg/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/C:/kmg/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-03-18 19:26:31 INFO Reflections took 106 ms to scan 46 urls, producing 98 keys and 218 values
2022-03-18 19:26:32 INFO
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-03-18 19:26:32 INFO Storage Benchmark Kit
2022-03-18 19:26:32 INFO SBK Version: 0.98
2022-03-18 19:26:32 INFO SBK Website: https://github.com/kmgowda/SBK
2022-03-18 19:26:32 INFO Arguments List: [-class, file, -readers, 1, -size, 100, -time, ns, -seconds, 60]
2022-03-18 19:26:32 INFO Java Runtime Version: 17.0.2+8-LTS-86
2022-03-18 19:26:32 INFO Storage Drivers Package: io.sbk
2022-03-18 19:26:32 INFO sbk.applicationName: sbk
2022-03-18 19:26:32 INFO sbk.appHome: C:\kmg\SBK\build\install\sbk\bin\..
2022-03-18 19:26:32 INFO sbk.className:
2022-03-18 19:26:32 INFO '-class': file
2022-03-18 19:26:32 INFO Available Storage Drivers in package 'io.sbk': 40 [Artemis,
AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, Derby,
FdbRecord, File, FileStream, FoundationDB, HDFS, Hive, Ignite, Jdbc, Kafka, LevelDB,
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL,
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-03-18 19:26:32 INFO Arguments to Driver 'File' : [-readers, 1, -size, 100, -time, ns, -seconds, 60]
2022-03-18 19:26:32 INFO Time Unit: NANOSECONDS
2022-03-18 19:26:32 INFO Minimum Latency: 0 ns
2022-03-18 19:26:32 INFO Maximum Latency: 180000000000 ns
2022-03-18 19:26:32 INFO Window Latency Store: HashMap, Size: 192 MB
2022-03-18 19:26:32 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-03-18 19:26:32 INFO Total Window Extension: None, Size: 0 MB
2022-03-18 19:26:32 INFO SBK Benchmark Started
2022-03-18 19:26:32 INFO SBK PrometheusLogger Started
2022-03-18 19:26:32 INFO Synchronous File Reader initiated !
2022-03-18 19:26:32 INFO CQueuePerl Start
2022-03-18 19:26:32 INFO Performance Recorder Started
2022-03-18 19:26:32 INFO SBK Benchmark initiated Readers
2022-03-18 19:26:32 INFO Reader 0 started , run seconds: 60
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       231.1 MB,          2423453 records,    484592.2 records/sec,    46.21 MB/sec,   1989.3 ns avg latency, 3376600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3400 ns 40th,    3700 ns 50th,    7600 ns 60th,   31700 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       235.4 MB,          2468178 records,    493536.9 records/sec,    47.07 MB/sec,   1964.7 ns avg latency, 2779200 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3200 ns 40th,    3700 ns 50th,    6400 ns 60th,   31400 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       235.4 MB,          2468865 records,    493673.5 records/sec,    47.08 MB/sec,   1964.7 ns avg latency, 2813600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3200 ns 40th,    3700 ns 50th,    6400 ns 60th,   31700 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       237.9 MB,          2494117 records,    498723.4 records/sec,    47.56 MB/sec,   1945.1 ns avg latency, 1798800 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3100 ns 40th,    3700 ns 50th,    6300 ns 60th,   30900 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       240.2 MB,          2518973 records,    503690.6 records/sec,    48.04 MB/sec,   1927.2 ns avg latency, 1911800 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    2800 ns 40th,    3600 ns 50th,    6600 ns 60th,   31400 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       238.9 MB,          2505268 records,    500953.3 records/sec,    47.77 MB/sec,   1936.2 ns avg latency, 2029000 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3000 ns 40th,    3700 ns 50th,    6600 ns 60th,   31400 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       236.2 MB,          2476892 records,    495279.2 records/sec,    47.23 MB/sec,   1959.0 ns avg latency, 2167200 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3200 ns 40th,    3700 ns 50th,    6500 ns 60th,   31400 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       239.0 MB,          2505892 records,    501078.2 records/sec,    47.79 MB/sec,   1937.5 ns avg latency, 1842000 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3000 ns 40th,    3700 ns 50th,    6400 ns 60th,   31200 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       220.6 MB,          2313580 records,    462623.4 records/sec,    44.12 MB/sec,   2099.4 ns avg latency, 49080200 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3600 ns 40th,    5600 ns 50th,    8000 ns 60th,   33000 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       238.3 MB,          2498266 records,    499553.2 records/sec,    47.64 MB/sec,   1943.4 ns avg latency, 1893200 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3000 ns 40th,    3700 ns 50th,    6500 ns 60th,   31000 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        5 seconds,       230.5 MB,          2416887 records,    483279.8 records/sec,    46.09 MB/sec,   2008.6 ns avg latency, 15369600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3300 ns 40th,    4000 ns 50th,    8300 ns 60th,   32300 ns 70th.
File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,        4 seconds,       235.3 MB,          2467104 records,    494514.9 records/sec,    47.16 MB/sec,   1959.7 ns avg latency, 1764600 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   5; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3200 ns 40th,    3700 ns 50th,    6700 ns 60th,   31300 ns 70th.
Total : File Reading     0 Writers,     1 Readers,      0 Max Writers,     1 Max Readers,       60 seconds,      2818.8 MB,         29557475 records,    492624.5 records/sec,    46.98 MB/sec,   1968.6 ns avg latency, 49080200 ns max latency;        0 invalid latencies; Discarded Latencies:       0 lower,        0 higher; SLC-1:   0, SLC-2:   6; Latency Percentiles:    1700 ns 10th,    1800 ns 20th,    1800 ns 25th,    1800 ns 30th,    3200 ns 40th,    3800 ns 50th,    6900 ns 60th,   31600 ns 70th.
2022-03-18 19:27:32 INFO Performance Recorder Exited
2022-03-18 19:27:32 INFO CQueuePerl Shutdown
2022-03-18 19:27:32 INFO Reader 0 exited
2022-03-18 19:27:32 INFO SBK PrometheusLogger Shutdown
2022-03-18 19:27:33 INFO SBK Benchmark Shutdown


```



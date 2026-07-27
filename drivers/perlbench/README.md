<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# PerlBench driver

`PerlBench` is SBK's synthetic driver for measuring the end-to-end overhead
and scalability of the SBK harness and its PerL timestamp pipeline. It removes
storage, network, serialization, and vendor-SDK work from the operation path:

- a write completes immediately;
- a reader returns the same worker-owned, preallocated byte array;
- neither operation allocates, blocks, locks, or accesses shared driver state.

This is not a storage benchmark. Use it to compare PerL configurations,
estimate the measurement ceiling of a machine, and check whether timestamp
collection can keep up with a proposed storage workload.

## What the driver measures

```mermaid
flowchart LR
    W["SBK worker"]
    S["start = clock"]
    OP["PerlBench no-op write<br/>or preallocated read"]
    E["end = clock"]
    Q["Timestamp queue"]
    R["Single PerL recorder"]
    O["Window and total results"]

    W --> S --> OP --> E --> Q --> R --> O

    classDef timed fill:#dcfce7,stroke:#166534,color:#111
    classDef transport fill:#dbeafe,stroke:#1d4ed8,color:#111
    class OP timed
    class Q,R transport
```

The reported operation latency is `end - start`. Queue insertion occurs after
the end timestamp, so displayed latency percentiles do **not** directly measure
enqueue latency. Queue implementation affects:

- the maximum sustainable end-to-end operation rate;
- allocation rate and garbage-collection pressure;
- queue backlog and the recorder's ability to drain it;
- process memory during large record-count runs.

Use the PerL JMH task when the research question is specifically queue
add/poll latency and normalized bytes allocated per operation:

```bash
./gradlew :perl:timeStampQueuePerformanceTest
```

## Queue selection

The common `-mpscqueue` SBK option works with real storage drivers and is
especially useful with `PerlBench`.

| Option | Values and default | Meaning |
|---|---|---|
| `-mpscqueue` | `true` or `false`; property default | `true` selects intrusive `TimeStampMpscQueue`; `false` selects JDK `ConcurrentLinkedQueue` |

Queue topology is deliberately not exposed through the CLI. SBK loads
`maxQs` and `qPerWorker` from
`sbk-api/src/main/resources/sbk.properties`. The default `maxQs=0` creates
per-worker channels and represents SBK's normal high-throughput deployment.
Advanced queue research can set `maxQs=1` and rebuild SBK to force every
writer or reader to publish to one shared MPSC queue.

At startup SBK prints the effective selection:

```text
PerL Timestamp Queue: TimeStampMpscQueue (MPSC)
PerL Timestamp Queue Topology: 10 queue(s) per worker
```

or:

```text
PerL Timestamp Queue: ConcurrentLinkedQueue (JDK)
PerL Timestamp Queue Topology: 10 queue(s) per worker
```

## Test modes

### Exact records: completion, latency, and throughput

`-records` without `-seconds` divides the requested total across the workers
and waits until every assigned operation has completed and every timestamp has
been consumed. Odd totals are supported; the first workers receive the
remainder one record at a time.

Compare the two queue paths with the same command except for `-mpscqueue`:

```bash
# Intrusive timestamp node and queue
./build/install/sbk/bin/sbk \
  -class perlbench -writers 4 -size 1024 -records 20000000 \
  -time ns -thread p -mpscqueue true

# TimeStamp plus the JDK queue's internal node
./build/install/sbk/bin/sbk \
  -class perlbench -writers 4 -size 1024 -records 20000000 \
  -time ns -thread p -mpscqueue false
```

Confirm that each result reports exactly `20000000 records` and
`0 invalid latencies`. Record mode is the best end-to-end check for lost,
duplicated, or undrained measurements.

### Timed saturation: maximum throughput

`-seconds` runs at maximum speed when `-throughput` remains `-1`. At the
duration boundary SBK stops workload generation and reports the measurements
accumulated by that hard deadline. Timed mode answers: "How many operations
can this complete pipeline sustain for this period?"

```bash
./build/install/sbk/bin/sbk \
  -class perlbench -writers 1 -size 1024 -seconds 30 \
  -time ns -thread p -mpscqueue true

./build/install/sbk/bin/sbk \
  -class perlbench -writers 1 -size 1024 -seconds 30 \
  -time ns -thread p -mpscqueue false
```

Repeat with `-writers 2`, `4`, `8`, and the available CPU count. A queue can
win with one producer yet lose at a particular contention level, so one thread
count is not a scalability study.

### Rate-controlled latency

Use `-throughput MBPS` with `-seconds` to hold offered data throughput near a
target. This separates behavior at a realistic load from maximum-saturation
behavior.

For 1,024-byte records, `-throughput 1000` is approximately 1,048,576
records/second across all workers:

```bash
./build/install/sbk/bin/sbk \
  -class perlbench -writers 4 -size 1024 -seconds 30 -throughput 1000 \
  -time ns -thread p -mpscqueue true

./build/install/sbk/bin/sbk \
  -class perlbench -writers 4 -size 1024 -seconds 30 -throughput 1000 \
  -time ns -thread p -mpscqueue false
```

Both runs should approach the same configured MB/s. Compare high-percentile
latencies, CPU consumption, allocation, GC pauses, and queue-drain stability.
Do not interpret a small difference in the displayed no-op latency as direct
queue latency because enqueue is outside the timed interval.

### Reader path

The reader reuses one payload per reader worker, so it exercises SBK's reader
control flow without per-record driver allocation:

```bash
./build/install/sbk/bin/sbk \
  -class perlbench -readers 4 -size 1024 -records 1000000 \
  -time ns -thread p -mpscqueue true
```

Run the same command with `-mpscqueue false`. No preparation step is required.

## Reproducible A/B protocol

1. Build once with `./gradlew clean :pathingJar installDist --rerun-tasks`.
2. Keep the JVM, GC, heap, compact-header setting, CPU governor, affinity,
   worker count, queue topology, duration/count, and record size identical.
3. Alternate queue order to reduce thermal and background-load bias.
4. Warm up before collecting results.
5. Use at least three forks; five or more are preferable for publication.
6. Record the median and confidence interval, not only the best run.
7. Capture allocation with JMH and process memory/GC with JFR, `jcmd`,
   `/usr/bin/time -v`, or equivalent OS tools.
8. For topology research, test separate builds with `maxQs=1` and the
   default `maxQs=0` in `sbk.properties`.
9. Confirm exact counts and zero invalid latencies in record mode.
10. Treat differences smaller than run-to-run variation as inconclusive.

## Example results and interpretation

The following results are observations, not performance guarantees. They were
collected on 2026-07-27 on a 16-vCPU VMware guest with an Intel Xeon Platinum
8462Y+, Linux 5.15, JDK 25.0.2, ZGC, and compact object headers.

### PerL queue microbenchmark

| Metric | `TimeStampMpscQueue` | JDK queue path | Observed difference |
|---|---:|---:|---:|
| Add/poll round trip | 33.107 ns/op | 47.673 ns/op | MPSC 30.55% lower |
| Normalized allocation | 40 B/op | 56 B/op | MPSC 28.57% lower |
| Four-producer enqueue throughput | 5.90 M ops/s | 5.16 M ops/s | MPSC 14.35% higher; 99.9% confidence intervals did not overlap |

The allocation difference is structural: the intrusive
`TimeStampNode` is both payload and link, whereas the JDK path allocates a
`TimeStamp` plus a private queue node. Throughput remains dependent on
contention, scheduling, and consumer service rate.

### PerlBench end-to-end observations

| Workload | `TimeStampMpscQueue` | JDK queue path | Interpretation |
|---|---:|---:|---|
| One writer, six-second saturation | 7.87 M records/s | 6.20 M records/s | MPSC 26.9% higher in this single run |
| Four writers, six-second saturation | 5.61 M records/s | 4.47 M records/s | MPSC 25.5% higher in this single run |
| Four writers, 1,000 MB/s target | 993.60 MB/s | 993.35 MB/s | Both sustained the requested load |
| One reader, six-second saturation | 8.11 M records/s | 6.16 M records/s | MPSC 31.6% higher in this single run |
| Four writers, 20 M exact records, three-run median | 5.53 M records/s | 5.19 M records/s | MPSC 6.6% higher under shared-queue contention |
| Same exact-record test, median peak RSS | 1.24 GiB | 1.54 GiB | MPSC used about 20% less process memory |

Every exact-count run completed all requested records with zero invalid
latencies. The results demonstrate the expected memory advantage and show
that adopting JDK CLQ's one-node tail-slack strategy removed unnecessary
shared-tail updates without sacrificing the intrusive queue's latency.
Results remain environment-specific, so the fallback and runtime comparison
options remain valuable.

For queue algorithm diagrams, memory-ordering details, correctness evidence,
and the complete JMH methodology, see
[TimeStampMpscQueue: architecture, correctness, and performance](../../docs/TIMESTAMP_MPSC_QUEUE.md).

## Limitations

- The driver has no storage durability, network, or data-integrity semantics.
- Its no-op latency is dominated by clocks and harness control flow.
- Both linked queues are unbounded; prolonged producer overload can grow
  memory.
- Peak RSS includes the JVM heap and SBK latency stores, not only queue nodes.
- A synthetic result must not replace testing with the intended real driver.
- Virtualized-host results are especially sensitive to scheduling noise.

# Benchmark methodology and result validation

## Design the experiment

Record these before running:

- question and success criterion;
- SBK commit/version and exact command or YML;
- driver and vendor SDK version;
- JDK version and JVM options;
- load-host CPU, NUMA, memory, disk, NIC, OS, and power policy;
- backend topology, version, durability, replication, compression, encryption,
  and capacity state;
- record size, writer/reader counts, rate limit, duration/count, and latency
  unit;
- whether data and filesystem caches are warm or cold;
- network path, bandwidth, MTU, and expected bottleneck.

Use a warm-up that is not mixed into reported comparisons. Run at least three
measured repetitions in randomized order. Compare distributions and confidence
intervals; report an inconclusive result when run-to-run variance is larger than
the observed difference.

## Understand SBK output

- `records/sec` and `MB/sec` describe completed operations in the reporting
  window.
- average, minimum, maximum, and percentiles use the selected `-time` unit.
- p50 is the median; p95/p99 describe tail latency; p99.9 and p99.99 require
  enough samples to be statistically meaningful.
- request/response pending and timeout counters expose pressure and incomplete
  work.
- regular interval lines show time evolution. The final `Total` line is the
  cumulative result and should not be mixed with interval samples.

Do not use a fixed rule such as “p99 must be within three times p50” for every
storage system. Tail shape depends on queueing, durability, retries, compaction,
GC, and workload phase.

## Hard validity checks

A publishable run should have:

- expected completed records and worker counts;
- no unexplained I/O, authentication, timeout, or callback errors;
- zero invalid latencies;
- no unexplained lower/higher discarded latencies;
- no unintended data deletion or namespace collision;
- no load-generator CPU, heap, network, or disk bottleneck unless that is the
  stated subject of the test;
- stable backend health with no hidden throttling or recovery event.

If any check fails, keep the raw output but label the run invalid or explain the
limitation. Never omit error lines while presenting the final total.

## Report template

```text
Objective:
Topology:
SBK version / commit:
JDK and JVM options:
Driver / backend version:
Exact command or sanitized YML:
Warm-up:
Measured repetitions:
Completed operations:
Throughput median and variability:
Latency p50 / p95 / p99 / p99.9:
Errors, timeouts, invalid/discarded latencies:
Load-host utilization:
Backend utilization:
Conclusion and limitations:
Raw-result locations:
```

For a distributed run, also include per-host return codes, aggregate SBM
connections, clock synchronization state, and callback-network topology.

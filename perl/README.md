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

# PerL: Performance Logger

PerL is SBK's storage-independent measurement library. It accepts operation timestamps and counts from concurrent producers, moves them through concurrent queues, calculates periodic and total latency statistics, and publishes results through a performance logger.

## Position in SBK

```text
SBK writer/readers -> PerlChannel -> queue array -> recorder -> latency windows -> logger
```

PerL does not know which storage backend produced an operation. `sbk-api` depends on PerL and supplies storage-specific context through its logger integration.

## Main abstractions

| Type | Responsibility |
|---|---|
| `Perl` | Built measurement pipeline and lifecycle |
| `PerlChannel` | Producer-facing submission and exception interface |
| `TimeStamp` | Start/end time, record count, and byte count |
| `TimeStampNode` | Intrusive timestamp and MPSC link in one allocation |
| `LatencyRecordWindow` | Latency accumulation and reporting contract |
| `PeriodicRecorder` | Periodic-window processing |
| `PerformanceRecorder` | Queue-draining recorder lifecycle |
| `PerlBuilder` | Selects time, queues, recorder, and latency storage from configuration |

Implementations live primarily under `io.perl.api.impl`. Clock representations live under `io.time`.

## Concurrency model

Workers submit records to `PerlChannel`; recorder logic drains the configured queue array and updates latency windows. Histogram and percentile work therefore does not execute in the driver operation itself. PerL records every submitted measurement rather than selecting a sample.

The measurement transport is designed to avoid explicit locks in the producer path. This statement does not imply that the JVM, vendor client, operating system, or storage system is lock-free.

### Production timestamp queue

By default, `CQueuePerl` uses `TimeStampMpscQueueChannel`, backed by
`TimeStampMpscQueueArray`. Each submitted measurement is one `TimeStampNode`:
the object inherits the timestamp payload and contains its own queue link.
Enqueue therefore performs no wrapper-node allocation. Multiple producers
publish with a CAS on the last node; the single recorder owns the head and
dequeues without a head CAS.

The consumer accumulates 16 retired predecessors, release-publishes a recovery
head, and then self-links every node in that batch. A producer suspended on any
retired node detects the self-link and resumes from the recovery head. This
keeps stale-producer retention bounded while grouping reclamation stores.
Nodes are not pooled because pooling retains heap and introduces ownership and
ABA hazards.

For an SBK command, select the fallback at runtime:

```bash
./build/install/sbk/bin/sbk \
  -class perlbench -writers 4 -size 1024 -records 1000000 \
  -mpscqueue false
```

`-mpscqueue true` selects the intrusive queue. If the option is absent, SBK
uses `MpscQueueEnable` from
`sbk-api/src/main/resources/sbk.properties`. Standalone PerL users instead set
the same property in `perl/src/main/resources/perl.properties`:

```properties
MpscQueueEnable=false
```

The fallback creates a `TimeStamp` and lets
`ConcurrentLinkedQueue` create its private node. It remains useful for
compatibility and comparative validation.

### Generic CQueue and JDK 25 ConcurrentLinkedQueue

`CQueue` is PerL's specialized unbounded multiple-producer, single-consumer
(MPSC) linked queue. Producers publish a newly allocated node with a
compare-and-set on the previous last node. Its only consumer owns the head, so
dequeue does not need the item and head compare-and-set operations required by
a queue that supports competing consumers. Head and tail state are padded to
reduce false sharing.

This narrower contract explains the lower latency and higher four-producer,
one-consumer throughput measured by `cqueuePerformanceTest`. It does not mean
that `CQueue` produces less garbage:

| GC characteristic | `CQueue` | JDK 25 `ConcurrentLinkedQueue` |
|---|---|---|
| Allocation per enqueue | One linked node | One linked node |
| Payload release after poll | Clears the consumed item | Clears the consumed item |
| Retired-head handling | Self-links one retirement boundary every 16 dequeues | CAS-advances the head and self-links the retired head |
| Stalled traversal retention | Producers detect a self-link and restart from a published recovery head; retained retired chains are bounded by the batch | Self-links redirect stale traversals and prevent long-chain retention |
| Interior dead-node cleanup | Not applicable; operation is unsupported | Traversals opportunistically unlink dead nodes |
| Per-queue overhead | Padded head and tail holders | Unpadded head and tail references |

JDK 25 `ConcurrentLinkedQueue` remains the stronger general-purpose GC
implementation because it also handles iterators, multiple consumers, and
interior dead-node removal. `CQueue` amortizes its reclamation work over
16 dequeues and allows a suspended producer to recover without adding a
consumer compare-and-set. It still allocates one node for every record.

The generic `CQueueArray` remains available for controlled experiments; the
production path uses the specialized intrusive timestamp queue described
above. The complete generic algorithm, memory-ordering rules, and usage
constraints are documented in the
[`CQueue` Javadoc](src/main/java/io/perl/api/impl/CQueue.java).

## Configuration

Standalone PerL defaults are in `src/main/resources/perl.properties`. SBK
launches use `sbk-api/src/main/resources/sbk.properties`, which contains the
equivalent queue, idle, latency-storage, and histogram defaults.

For SBK:

- `-mpscqueue true|false` overrides only `MpscQueueEnable`;
- `-idletimeoutseconds N` overrides `idleTimeoutSeconds`; the property default
  is 600 seconds for both standalone PerL and SBK;
- `qPerWorker` and `maxQs` determine queue topology and remain property-backed;
- invalid negative `maxQs` or `qPerWorker` below the supported minimum is
  rejected while common parameters are constructed;
- startup logs show the effective queue implementation and topology.

Use `SbkParameters.loadPerlConfig()` and
`SbkBenchmark.buildPerlConfig()` to follow SBK's property-to-runtime path.
Use `PerlBuilder` as the source-level entry point for standalone PerL
construction.

## Elastic idle waiting

With the default `sleepMS=0`, `PerformanceRecorderElasticWait` is the sole
consumer of the timestamp queues. The class name describes its adaptive idle
policy rather than a tight spin: after a complete scan finds no data, the recorder calls
`LockSupport.parkNanos(idleNS)`. `ElasticWait` learns the number of completed
parks per millisecond and uses that rate to decide how many parks may occur
before the recorder samples the clock again.

```text
records available -> reuse TimeStamp.endTime -> no recorder clock call
queues empty       -> park and count         -> no clock call within batch
batch complete     -> sample clock once      -> update EMA and next threshold
```

Calibration begins conservatively with a clock check after one park. If the
clock has not advanced, the bootstrap batch grows exponentially up to a
bounded threshold. Once measurable elapsed time exists, the observed park
rate is folded into an exponential moving average. This adapts to operating
system timer granularity, scheduling, CPU speed, and platform versus virtual
thread behavior instead of assuming that `parkNanos` sleeps for precisely the
requested duration.

A window may alternate between idle and active periods. On the first empty
scan after consuming data, the recorder calls `ElasticWait.startIdle()` with
elapsed time derived from the last record's `endTime`. It discards the
previous idle-period counters but retains the learned EMA rate. Active
processing time therefore cannot dilute the next idle-rate sample, and the
transition introduces no additional clock query. The sample origin never
moves backwards if producer completion timestamps arrive out of order.

`ElasticWait` applies only to the default idle-parking recorder.
Setting `sleepMS > 0` selects `PerformanceRecorderIdleSleep`, which sleeps for
the configured interval and does not use adaptive calibration. `idleNS`
controls the responsiveness/idle-CPU tradeoff; its SBK default is 1 ms and its
enforced minimum is 1 µs. Neither setting changes the operation latency
already captured by the worker, but a longer idle delay can temporarily grow
the queue backlog after new data arrives.

PerL also enforces `idleTimeoutSeconds` while every timestamp channel remains
empty. The single consumer retains the last event time as ordinary local state;
no lock, atomic variable, volatile coordination, or producer-side check is
added. The deadline comparison runs only from the existing empty-channel slow
path. Expiration completes the PerL future exceptionally with
`BenchmarkIdleTimeoutException`, allowing SBK or another embedding application
to stop its workers and report a failed benchmark instead of waiting forever.

The complete recorder state and timing diagrams are in
[the internal design guide](../docs/sbk-internals.md#pillar-3--elasticwait-amortising-clock-queries).

## Build and test

From the repository root:

```bash
./gradlew :perl:check
./gradlew :perl:jmh
```

The normal project build also checks PerL:

```bash
./gradlew check
```

Use JMH for performance claims and deterministic unit tests for percentile/window correctness. Avoid wall-clock assertions where a fake or explicit `Time` implementation can make the test stable.

The normal `:perl:check` task starts dedicated JVMs with fixed 32 MB heaps for
`cqueueGcTest` and `timeStampMpscQueueGcTest`. The timestamp test enqueues and
consumes 20 million records while a producer is paused on a stale queue node.
It fails if the retired chain reaches the 16-node batch size, the producer
cannot recover, the process runs out of heap, or a consumed node remains
strongly reachable. Unit coverage also verifies identity, FIFO order,
multi-producer delivery, per-producer ordering, channel selection, clearing,
and deterministic stale-producer recovery.

These tests establish bounded retired-node retention and prompt payload release
for the documented multiple-producer, single-consumer contract. They do not
claim equivalence with every general-purpose operation or every possible GC
schedule supported by JDK `ConcurrentLinkedQueue`.

Use the dedicated concurrency tools to validate properties that ordinary unit
tests cannot prove:

```bash
./gradlew :perl:lincheckTest
./gradlew :perl:jcstress
./gradlew :perl:concurrencyCheck
```

`lincheckTest` checks `add` and `poll` histories against a sequential FIFO
model using both controlled model checking and scheduler stress. Its operation
model permits concurrent producers and places `poll` in a non-parallel group,
so the test enforces the queue's real multiple-producer, single-consumer
contract rather than accidentally testing unsupported multiple consumers.

`jcstress` runs Java Memory Model outcome tests. One test races publication
against consumption and forbids lost, duplicated, or partially visible
timestamp fields. A second test races two producers and accepts only complete
four-node histories that preserve each producer's FIFO order. Reports are
written to `perl/build/reports/jcstress/`.

`concurrencyCheck` is the complete queue-correctness gate. It serializes unit
tests, the constrained-heap reclamation test, Lincheck, and JCStress so that
the CPU-intensive tools do not interfere with one another. It also applies
Checkstyle to both dedicated test source sets.

To verify the MPSC queue performance claim against JDK 25
`ConcurrentLinkedQueue`, run the dedicated JMH performance test on an otherwise
idle system:

```bash
./gradlew :perl:cqueuePerformanceTest
```

The task uses warmup iterations, three isolated JVM forks, compact object
headers, and separate successful-producer throughput metrics. It passes only
when `CQueue` has at least 5% lower enqueue/dequeue round-trip latency and at
least 2% higher four-producer/one-consumer throughput, with non-overlapping
99.9% confidence intervals. Its JSON report is written to
`perl/build/reports/jmh/cqueue-performance.json`. The report also contains JMH
GC-profiler metrics such as allocation rate and normalized bytes allocated.
It verifies that normalized CQueue allocation does not exceed the equivalent
JDK operation by more than one byte. A stalled-producer soak benchmark also
checks that the retired-node chain remains below the 16-node retirement batch
while continuing to allocate and reclaim nodes. Compare normalized allocation
for equivalent operations; a faster queue can show a higher allocation rate
per second merely because it completes more operations. This
environment-sensitive test is intentionally separate from `check`;
correctness and stress tests remain part of the normal build.

The intrusive production queue has equivalent correctness and constrained-heap
coverage. Run its dedicated JMH verification with:

```bash
./gradlew :perl:timeStampQueuePerformanceTest
```

The comparison includes the complete allocation performed by PerL. On JDK 25
with compact object headers, inspect `gc.alloc.rate.norm` to confirm that the
intrusive round trip allocates one 40-byte `TimeStampNode`, while the JDK path
allocates a 32-byte `TimeStamp` plus its queue node (56 bytes total on the
tested runtime). The four-producer/one-consumer comparison parks briefly after
an empty poll, matching PerL's `ElasticWait` behavior and preventing empty-poll
speed from distorting producer throughput. The verification requires lower
round-trip latency, removal of at least eight allocation bytes per operation,
and at least 2% higher producer throughput. It also reports the 99.9% MPSC
throughput confidence intervals as diagnostics; interval overlap is not a hard
gate because host noise can widen an otherwise faster result. For a firm
throughput comparison, use multiple forks and pin the producer and consumer
threads to dedicated physical cores; latency and the structural allocation
reduction are more stable signals. The report is written to
`perl/build/reports/jmh/timestamp-queue-performance.json`. Results are
host-specific; preserve the same JVM flags and an otherwise idle host when
comparing changes.

The production retirement batch remains 16. Concurrency-model tests inject
smaller batches: Lincheck uses 2 so short histories cross the retirement
boundary, while JCStress forces both stale-tail recovery and the
release/acquire recovery-head fallback. Manual padding around the separate
producer and consumer holders is a best-effort false-sharing heuristic, not a
correctness requirement; Java does not guarantee field order or cache-line
placement.

The detailed
[TimeStampMpscQueue research guide](../docs/TIMESTAMP_MPSC_QUEUE.md) explains
the linearization points, acquire/release edges, batched retired-head
reclamation, stale-producer recovery, JDK 25 `ConcurrentLinkedQueue`
differences, correctness evidence, and performance methodology. It also records
one environment where the intrusive path reduced latency and allocation and
passed the point-estimate throughput gate, while the producer-throughput
confidence intervals overlapped. Throughput is not a universal property of the
algorithm.

## Use as a library

Published coordinates use the project group and version defined in `gradle.properties`. Refer to [Maven Central](https://central.sonatype.com/), the repository's GitHub Packages configuration, or a locally published build for the currently available version instead of copying a version from this README.

Gradle example:

```groovy
dependencies {
    implementation "io.sbk:perl:<version>"
}
```

For programmatic usage, read these in order:

1. `perl/src/test/java/io/perl/test/PerlTest.java`
2. `perl/src/main/java/io/perl/api/impl/PerlBuilder.java`
3. `sbk-api/src/main/java/io/sbk/api/impl/SbkBenchmark.java`
4. `sbk-api/src/main/java/io/sbk/api/Writer.java`

## Further reading

- [Architecture and code flow](../docs/ARCHITECTURE.md#perl-measurement-pipeline)
- [Detailed internal design](../docs/sbk-internals.md#3-perl--the-performance-logger-foundation)
- [Contribution workflow](../CONTRIBUTING.md)

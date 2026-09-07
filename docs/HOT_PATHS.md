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

# SBK hot-path and critical-path inventory

This document is the authoritative file-level sensitivity map for humans and
software agents changing SBK. Read it before editing PerL, the SBK harness,
SBM, a driver operation, or SBK-GEM runtime control.

The list is intentionally conservative. A file is sensitive when code in it
can execute for every storage operation, timestamp, measurement, queue item,
or measurement batch. A sensitive file can also contain cold methods; the
classification applies to the relevant methods and call paths, not blindly to
every line in the file.

## Normative policy

[`AGENTS.md`](../AGENTS.md#hot-path-latency-policy-mandatory-for-every-software-agent)
owns the normative hot-path rules, prohibited changes, explicit-confirmation
process, and required performance/concurrency evidence. This document owns
only the file and method classification. Apply the `AGENTS.md` approval gate
to every H0, H1, and H2 path below. When the two documents disagree, stop and
correct them rather than choosing the less restrictive interpretation.

## Classification

| Mark | Meaning | Review expectation |
|---|---|---|
| **H0** | Executes for each storage operation or callback | Strictest: no extra per-operation work without approval |
| **H1** | Executes for each timestamp, queue item, or latency update | Strictest: preserve queue and recorder algorithms |
| **H2** | Executes for each measurement batch/window or gRPC forwarding batch | Avoid extra copying, traversal, allocation, or coordination |
| **C** | Lifecycle/control path that determines correctness or bounded completion | May use coordination, but must preserve ordering, deadlines, and result integrity |

## PerL

### Timestamp submission and queue transport

| Class/file | Mark | Sensitive responsibility |
|---|---:|---|
| `perl/src/main/java/io/perl/api/PerlChannel.java` | H1 | Producer timestamp submission contract |
| `perl/src/main/java/io/perl/api/Channel.java` | H1 | Producer/consumer channel contract |
| `perl/src/main/java/io/perl/api/Queue.java` | H1 | Queue `add`/`poll` contract |
| `perl/src/main/java/io/perl/api/QueueArray.java` | H1 | Sharded queue contract |
| `perl/src/main/java/io/perl/api/TimeStamp.java` | H1 | Per-measurement timestamp/count/byte carrier |
| `perl/src/main/java/io/perl/api/TimeStampNode.java` | H1 | Queue node carrying a timestamp |
| `perl/src/main/java/io/perl/api/impl/CQueuePerl.java` | H1 | PerL producer channels and consumer polling |
| `perl/src/main/java/io/perl/api/impl/TimeStampMpscQueue.java` | H1 | MPSC timestamp enqueue/dequeue and publication protocol |
| `perl/src/main/java/io/perl/api/impl/TimeStampMpscQueueArray.java` | H1 | Sharded MPSC timestamp queues |
| `perl/src/main/java/io/perl/api/impl/CQueue.java` | H1 | Generic concurrent queue implementation |
| `perl/src/main/java/io/perl/api/impl/CQueueArray.java` | H1 | Generic sharded queue implementation |
| `perl/src/main/java/io/perl/api/impl/AtomicQueue.java` | H1 | Atomic queue implementation |
| `perl/src/main/java/io/perl/api/impl/SyncQueue.java` | H1 | Synchronized queue implementation |
| `perl/src/main/java/io/perl/api/impl/ConcurrentLinkedQueueArray.java` | H1 | Sharded concurrent queue used by aggregation paths |

### Consumer loops, latency recording, and percentiles

| Class/file | Mark | Sensitive responsibility |
|---|---:|---|
| `perl/src/main/java/io/perl/api/PerformanceRecorder.java` | H1 | Shared channel/recorder state and consumer-loop contract |
| `perl/src/main/java/io/perl/api/impl/PerformanceRecorderElasticWait.java` | H1 | Queue traversal and adaptive recorder loop |
| `perl/src/main/java/io/perl/api/impl/PerformanceRecorderIdleSleep.java` | H1 | Queue traversal and idle-sleep recorder loop |
| `perl/src/main/java/io/perl/api/PeriodicRecorder.java` | H1 | Per-measurement recorder contract |
| `perl/src/main/java/io/perl/api/PeriodicWindow.java` | H2 | Periodic-window reporting contract |
| `perl/src/main/java/io/perl/api/TotalPeriodicWindow.java` | H2 | Periodic and total-window reporting contract |
| `perl/src/main/java/io/perl/api/LatencyRecorder.java` | H1 | Latency validity, count, byte, and total updates |
| `perl/src/main/java/io/perl/api/LatencyWindow.java` | H2 | Window statistics and percentile extraction |
| `perl/src/main/java/io/perl/api/LatencyRecord.java` | H1/H2 | Mutable aggregate counters consumed by recorders |
| `perl/src/main/java/io/perl/api/LatencyRecordWindow.java` | H1/H2 | Exact latency-window recording contract |
| `perl/src/main/java/io/perl/api/LatencyPercentiles.java` | H2 | Percentile target/result state |
| `perl/src/main/java/io/perl/api/ReportLatencies.java` | H2 | Per-latency callback used during percentile traversal |
| `perl/src/main/java/io/perl/api/impl/ArrayLatencyRecorder.java` | H1/H2 | Dense exact frequency update and percentile traversal |
| `perl/src/main/java/io/perl/api/impl/LongHashMapLatencyRecorder.java` | H1/H2 | Sparse exact frequency update and percentile traversal |
| `perl/src/main/java/io/perl/api/impl/HashMapLatencyRecorder.java` | H1/H2 | Boxed-map exact recorder |
| `perl/src/main/java/io/perl/api/impl/MapLatencyRecorder.java` | H1/H2 | Map recorder base behavior |
| `perl/src/main/java/io/perl/api/impl/HybridPagedLatencyRecorder.java` | H1/H2 | SBM hybrid sparse/dense page recorder |
| `perl/src/main/java/io/perl/api/impl/HdrExtendedLatencyRecorder.java` | H1/H2 | HdrHistogram extension recording |
| `perl/src/main/java/io/perl/api/impl/CSVExtendedLatencyRecorder.java` | H1/H2 | CSV latency extension recording and rotation |
| `perl/src/main/java/io/perl/api/impl/ArrayWindowPeriodicRecorder.java` | H1/H2 | Window and total recorder update |
| `perl/src/main/java/io/perl/api/impl/ArrayWindowLatencyPeriodicRecorder.java` | H1/H2 | Window and latency-extension update |
| `perl/src/main/java/io/perl/api/impl/TotalWindowPeriodicRecorder.java` | H1/H2 | Periodic and total aggregation |
| `perl/src/main/java/io/perl/api/impl/TotalWindowLatencyPeriodicRecorder.java` | H1/H2 | Periodic, total, and extension aggregation |
| `perl/src/main/java/io/perl/api/impl/TotalWindowLatencyRecorder.java` | H1/H2 | Shared window/total recorder behavior |
| `perl/src/main/java/io/perl/api/impl/TotalLatencyRecordWindow.java` | H2 | Window and total-window reporting fan-out |

### PerL window logging

| Class/file | Mark | Sensitive responsibility |
|---|---:|---|
| `perl/src/main/java/io/perl/logger/PerformanceLogger.java` | H1/H2 | Per-measurement latency callback and per-window logger contract |
| `perl/src/main/java/io/perl/logger/Print.java` | H2 | Per-window and total result output contract |
| `perl/src/main/java/io/perl/logger/ReportLatency.java` | H1 | Per-measurement latency callback contract |
| `perl/src/main/java/io/perl/logger/impl/DefaultLogger.java` | H1/H2 | Final no-op measurement callback and default window output |
| `perl/src/main/java/io/perl/logger/impl/DefaultPrometheusLogger.java` | H1/H2/C | Inherited measurement callback, window metrics, and server lifecycle |
| `perl/src/main/java/io/perl/logger/impl/Metrics.java` | H2 | Window metric state consumed by metrics reporters |
| `perl/src/main/java/io/perl/logger/impl/PrintMetrics.java` | H2 | Window metric publication contract and implementation |
| `perl/src/main/java/io/perl/logger/impl/PrometheusMetricsServer.java` | H2/C | Window metric publication and Prometheus server lifecycle |
| `perl/src/main/java/io/perl/logger/impl/PrometheusServer.java` | C | Metrics registry and HTTP-server lifecycle |
| `perl/src/main/java/io/perl/logger/impl/ResultsLogger.java` | H2 | Per-window result construction and dispatch |

### PerL selection and timing boundary

`perl/src/main/java/io/perl/api/impl/PerlBuilder.java`,
`perl/src/main/java/io/perl/api/Perl.java`, and
`perl/src/main/java/io/perl/api/GetPerlChannel.java` are **C** construction
paths. They select which hot implementation runs. Changes can alter memory,
accuracy, or dispatch characteristics even when made outside the loop.

The implementations under `perl/src/main/java/io/time/` are **H0/H1** when
their `getTime`, conversion, or duration methods are called by record/read/write
paths. Do not add clock reads or conversions to those call sites.

## SBK harness

### Writer, reader, callback, and driver-call paths

| Class/file | Mark | Sensitive responsibility |
|---|---:|---|
| `sbk-api/src/main/java/io/sbk/api/DataWriter.java` | H0 | Writer workload contract |
| `sbk-api/src/main/java/io/sbk/api/DataRecordsWriter.java` | H0 | Fixed/time/rate/sync/batch writer loops |
| `sbk-api/src/main/java/io/sbk/api/Writer.java` | H0 | Driver write adapter, completion timing, PerL submission |
| `sbk-api/src/main/java/io/sbk/api/DataReader.java` | H0 | Reader workload contract |
| `sbk-api/src/main/java/io/sbk/api/DataRecordsReader.java` | H0 | Fixed/time/rate reader loops |
| `sbk-api/src/main/java/io/sbk/api/Reader.java` | H0 | Driver read adapter, timing, PerL submission |
| `sbk-api/src/main/java/io/sbk/api/AsyncReader.java` | H0 | Async read completion and PerL submission |
| `sbk-api/src/main/java/io/sbk/api/AbstractCallbackReader.java` | H0 | Callback delivery and benchmark recording |
| `sbk-api/src/main/java/io/sbk/api/Callback.java` | H0 | Callback record contract |
| `sbk-api/src/main/java/io/sbk/api/BiConsumer.java` | H0 | Callback adapter contract |
| `sbk-api/src/main/java/io/sbk/api/impl/SbkWriter.java` | H0/C | Selects and runs the specialized writer loop |
| `sbk-api/src/main/java/io/sbk/api/impl/SbkReader.java` | H0/C | Selects and runs the specialized reader loop |
| `sbk-api/src/main/java/io/sbk/api/impl/SbkCallbackReader.java` | H0/C | Selects and runs callback-reader recording |
| `sbk-api/src/main/java/io/sbk/api/Status.java` | H0 | Mutable operation timing/count/byte state |
| `sbk-api/src/main/java/io/sbk/api/RateController.java` | H0 | Per-operation pacing contract |
| `sbk-api/src/main/java/io/sbk/api/impl/SbkRateController.java` | H0 | Per-operation pacing implementation |
| `sbk-api/src/main/java/io/sbk/data/DataType.java` | H0 | Payload creation, length, and embedded timestamp access |
| `sbk-api/src/main/java/io/sbk/data/impl/ByteArray.java` | H0 | Byte-array payload operations |
| `sbk-api/src/main/java/io/sbk/data/impl/NioByteBuffer.java` | H0 | NIO payload operations |
| `sbk-api/src/main/java/io/sbk/data/impl/ProtoBufByteString.java` | H0 | Protobuf payload operations |
| `sbk-api/src/main/java/io/sbk/data/impl/SbkString.java` | H0 | String payload operations |

Every enabled or disabled driver implementation of
`sbk-api/src/main/java/io/sbk/api/Writer.java`,
`sbk-api/src/main/java/io/sbk/api/Reader.java`,
`sbk-api/src/main/java/io/sbk/api/AsyncReader.java`, or
`sbk-api/src/main/java/io/sbk/api/AbstractCallbackReader.java` is **H0**,
including its SDK completion callback and helpers invoked once per operation.
This covers files matching `drivers/*/src/main/java/**/*Writer.java` and
`drivers/*/src/main/java/**/*Reader.java`, plus any per-operation helper
regardless of its name. Driver configuration, client construction, catalog
discovery, and shutdown are cold only when they are not called from an
operation method.

### Request accounting and distributed forwarding

| Class/file | Mark | Sensitive responsibility |
|---|---:|---|
| `sbk-api/src/main/java/io/sbk/logger/WriteRequestsLogger.java` | H0 | Per-write request accounting contract |
| `sbk-api/src/main/java/io/sbk/logger/ReadRequestsLogger.java` | H0 | Per-read request accounting contract |
| `sbk-api/src/main/java/io/sbk/logger/CountReaders.java` | C | Reader lifecycle-count contract |
| `sbk-api/src/main/java/io/sbk/logger/CountWriters.java` | C | Writer lifecycle-count contract |
| `sbk-api/src/main/java/io/sbk/logger/CountRW.java` | C | Combined worker lifecycle-count contract |
| `sbk-api/src/main/java/io/sbk/logger/SetRW.java` | C | Worker-count initialization contract |
| `sbk-api/src/main/java/io/sbk/logger/RWPrint.java` | H2 | Per-window and total read/write result-output contract |
| `sbk-api/src/main/java/io/sbk/logger/Logger.java` | H1/H2/C | Measurement callback, reporting, configuration, and lifecycle contract |
| `sbk-api/src/main/java/io/sbk/logger/RWLogger.java` | H0/H1/H2/C | Request accounting, measurement callback, output, and lifecycle contract |
| `sbk-api/src/main/java/io/sbk/logger/LoggerConfig.java` | C | Logger startup configuration |
| `sbk-api/src/main/java/io/sbk/logger/MetricsConfig.java` | C | Metrics endpoint startup configuration |
| `sbk-api/src/main/java/io/sbk/logger/SbmHostConfig.java` | C | Distributed logger endpoint configuration |
| `sbk-api/src/main/java/io/sbk/logger/impl/AbstractRWLogger.java` | H0/H2/C | Request counters, periodic results, and logger lifecycle |
| `sbk-api/src/main/java/io/sbk/logger/impl/AbstractSystemLogger.java` | H2 | System-output window formatting |
| `sbk-api/src/main/java/io/sbk/logger/impl/SystemLogger.java` | H1/H2 | Final no-op measurement callback and system result output |
| `sbk-api/src/main/java/io/sbk/logger/impl/Sl4jLogger.java` | H1/H2 | Inherited measurement callback and SLF4J result publication |
| `sbk-api/src/main/java/io/sbk/logger/impl/CSVLogger.java` | H1/H2/C | Inherited measurement callback, CSV publication, and file lifecycle |
| `sbk-api/src/main/java/io/sbk/logger/impl/PrometheusLogger.java` | H1/H2/C | Inherited measurement callback, metrics publication, and server lifecycle |
| `sbk-api/src/main/java/io/sbk/logger/impl/WebLogger.java` | H1/H2/C | Inherited measurement callback, web publication, and client lifecycle |
| `sbk-api/src/main/java/io/sbk/logger/impl/GrpcLogger.java` | H1/H2/C | Latency accumulation, measurement-batch creation, and stream lifecycle |
| `sbk-api/src/main/java/io/sbk/logger/impl/GrpcLatencyAccumulator.java` | H1/H2 | Batch latency/count/byte accumulation |
| `sbk-api/src/main/java/io/sbk/logger/impl/GrpcStreamSender.java` | H2/C | Queueing, streaming, draining, and closing measurement batches |
| `sbk-api/src/main/java/io/sbk/logger/impl/PrometheusLinks.java` | C | Metrics endpoint discovery and diagnostic output |
| `sbk-api/src/main/java/io/sbk/logger/impl/SbkPrometheusServer.java` | H2/C | Window metric updates and Prometheus server lifecycle |
| `sbk-api/src/main/proto/sbp.proto` | H2/C | Wire contract for distributed measurements |

Formatting and I/O are intentionally outside H0/H1. Logger classes marked H1
have a callback selected into the per-measurement path; for system-style
loggers that callback is deliberately a final no-op. Added traversal, copying,
blocking, or inconsistent field ordering in H2 can still corrupt or delay
results.

`sbk-api/src/main/java/io/sbk/api/impl/SbkBenchmark.java`,
`sbk-api/src/main/java/io/sbk/api/Worker.java`,
`sbk-api/src/main/java/io/sbk/api/Storage.java`,
`sbk-api/src/main/java/io/sbk/api/impl/Sbk.java`,
`sbk-api/src/main/java/io/sbk/main/SbkMain.java`, and
`sbk-api/src/main/java/io/sbk/utils/ApplicationShutdownHook.java` are **C**.
They own worker creation, storage lifecycle, error propagation, result
completion, and bounded shutdown. Do not confuse “not per-record” with “low
risk”: edits require lifecycle ordering and termination tests.

## SBM

| Class/file | Mark | Sensitive responsibility |
|---|---:|---|
| `sbm/src/main/java/io/sbm/api/impl/SbmGrpcService.java` | H2/C | `streamLatencies().onNext` ingests every measurement batch; other RPCs are lifecycle control |
| `sbm/src/main/java/io/sbm/api/impl/SbmLatencyBenchmark.java` | H2/C | Sharded batch enqueue/poll, aggregation loop, drain, and stop sentinel |
| `sbm/src/main/java/io/sbm/api/SbmPeriodicRecorder.java` | H2 | Per-batch recorder contract |
| `sbm/src/main/java/io/sbm/api/impl/SbmTotalWindowLatencyPeriodicRecorder.java` | H2 | Converts and merges each incoming measurement batch |
| `perl/src/main/java/io/perl/api/impl/ConcurrentLinkedQueueArray.java` | H2 | SBM's sharded inbound queue implementation |
| `sbm/src/main/java/io/sbm/logger/CountConnections.java` | C | Client connection lifecycle-count contract |
| `sbm/src/main/java/io/sbm/logger/RamLogger.java` | H2 | Aggregate reporting contract |
| `sbm/src/main/java/io/sbm/logger/impl/AbstractRamLogger.java` | H2/C | Aggregate calculation, output dispatch, and connection lifecycle |
| `sbm/src/main/java/io/sbm/logger/impl/SbmPrometheusLogger.java` | H2/C | Window metric publication and server lifecycle |
| `sbm/src/main/java/io/sbm/logger/impl/SbmPrometheusServer.java` | H2/C | Aggregate metric updates and Prometheus server lifecycle |
| `sbm/src/main/java/io/sbm/logger/impl/SbmWebLogger.java` | H2/C | Window web publication and client lifecycle |

`sbm/src/main/java/io/sbm/api/impl/SbmBenchmark.java`,
`sbm/src/main/java/io/sbm/api/impl/Sbm.java`,
`sbm/src/main/java/io/sbm/main/SbmMain.java`, and
`sbm/src/main/java/io/sbm/api/SbmRegistry.java` are **C**. They create the
server and recorder, coordinate clients, quiesce gRPC, drain all queue shards,
emit final results, and enforce cleanup deadlines. Keep gRPC callbacks and
logging outside service monitors, and preserve the server-quiesce -> queue-
drain -> recorder-stop ordering.

## SBK-GEM

SBK-GEM has **no H0/H1 measurement hot path**. It launches remote SBK
processes and embeds/controls SBM; the remote SBK processes and SBM own the
per-record and per-batch paths described above. Do not label SSH transfer speed
or orchestration code as storage-measurement latency.

Its logger types inherit or select the embedded SBM aggregate reporting path
and are therefore H2/C even though GEM orchestration itself is not a
measurement hot path:

| Class/file | Mark | Sensitive responsibility |
|---|---:|---|
| `sbk-gem/src/main/java/io/gem/logger/GemLogger.java` | H2/C | GEM aggregate logger and option contract |
| `sbk-gem/src/main/java/io/gem/logger/impl/AbstractGemLogger.java` | H2/C | Shared GEM/SBM logger selection and configuration |
| `sbk-gem/src/main/java/io/gem/logger/impl/GemPrometheusLogger.java` | H2/C | Prometheus aggregate logger selection and inherited publication |
| `sbk-gem/src/main/java/io/gem/logger/impl/GemWebLogger.java` | H2/C | Web aggregate logger selection and inherited publication |

The following are nevertheless **C** distributed critical paths because they
control whether all nodes run the same workload, start together, return valid
results, and terminate:

- `sbk-gem/src/main/java/io/gem/api/impl/SbkGemBenchmark.java`
- `sbk-gem/src/main/java/io/gem/api/impl/BenchmarkLifecycle.java`
- `sbk-gem/src/main/java/io/gem/api/impl/DistributedWorkloadPlanner.java`
- `sbk-gem/src/main/java/io/gem/api/impl/DistributedResultPrinter.java`
- `sbk-gem/src/main/java/io/gem/api/impl/DeploymentOrchestrator.java`
- `sbk-gem/src/main/java/io/gem/api/impl/DeploymentTransport.java`
- `sbk-gem/src/main/java/io/gem/api/impl/RuntimeDeploymentTransport.java`
- `sbk-gem/src/main/java/io/gem/api/impl/RemoteEnvironmentPreparer.java`
- `sbk-gem/src/main/java/io/gem/api/impl/RemoteRuntimeLifecycle.java`
- `sbk-gem/src/main/java/io/gem/api/impl/RuntimeLeaseController.java`
- `sbk-gem/src/main/java/io/gem/api/impl/RuntimeLeaseManager.java`
- `sbk-gem/src/main/java/io/gem/api/impl/SbkGemExecutors.java`
- `sbk-gem/src/main/java/io/gem/api/SshClientManager.java`
- `sbk-gem/src/main/java/io/gem/api/SshSession.java`
- `sbk-gem/src/main/java/io/gem/agent/SbkGemRemoteAgentMain.java`
- `sbk-gem/src/main/java/io/gem/agent/RemoteAgentProtocol.java`

Edits here should be tested for bounded startup/shutdown, cancellation,
partial-node failure, callback isolation, deterministic runtime selection,
credential redaction, exact return-code attribution, and cleanup. Optimizing
these paths must not change benchmark timing or silently omit a node.

## Required workflow for a sensitive-file change

1. Identify the table entry and the exact methods reached per operation,
   timestamp, queue item, window, or batch.
2. Inspect callers and implementations; do not infer coldness from a class or
   method name.
3. If the edit adds any cost prohibited by the normative `AGENTS.md` policy,
   stop and obtain the required explicit human confirmation.
4. Prefer startup selection, precomputation, immutable configuration, or an
   existing error/lifecycle slow path.
5. Preserve functional correctness with focused tests.
6. For an approved hot-path edit, run the agreed JMH test and a representative
   end-to-end SBK/PerlBench test, reporting throughput, latency, allocation,
   variance, commands, JDK, and host conditions.
7. For queue/concurrency changes, additionally run stress and memory-model
   verification appropriate to the algorithm.
8. State in the PR which sensitive methods changed. If none did, say so
   explicitly; do not claim “no hot-path change” merely because the file also
   contains control-plane code.

When a new writer, reader, queue, recorder, timestamp carrier, measurement
accumulator, or forwarding implementation is added, update this inventory in
the same PR.

## Automated validation

Run the repository validator after changing this inventory, an agent entry
point, or its packaging:

```bash
./gradlew verifyHotPathDocumentation
```

The task is part of the root `check` lifecycle and builds the installed
distribution before validating it. It fails when a concrete source path in
this document does not exist, when a Java file is cited only by bare filename,
when a likely-sensitive recorder/queue/window/timestamp/writer/reader/
accumulator/forwarder candidate is omitted, when any Java file in the PerL,
SBK, SBM, or SBK-GEM logger trees is omitted, when a supported agent entry
point no longer routes to this document, or when the resolved distribution
omits the guide or a tool-specific adapter. It also checks the independent
Maven and release packaging declarations.

Name-pattern coverage is deliberately conservative but cannot prove that an
arbitrarily named helper is cold. Classification completeness and whether a
method is actually hot still require code review; no static path check can
replace call-graph inspection and performance analysis.

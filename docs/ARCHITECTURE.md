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

# SBK architecture and code flow

This guide explains how SBK is assembled and how one benchmark command moves through the code. It is intended for software engineers, reviewers, operators, and coding agents. For algorithm-level detail, continue with [sbk-internals.md](sbk-internals.md).

## System context

SBK separates workload control from storage-specific I/O. The same harness can drive a local file, an S3 endpoint, a message broker, a database, or an in-memory queue because all backends implement the same `Storage<T>` contract.

```mermaid
flowchart TB
    USER[User, script, or YML file]
    SBK[SBK single-node launcher]
    DRIVER[Storage driver]
    BACKEND[Storage system]
    PERL[PerL measurement engine]
    LOGGER[Output logger]
    SBM[SBM distributed aggregator]
    GEM[SBK-GEM SSH orchestrator]

    USER --> SBK
    GEM --> SBK
    SBK --> DRIVER
    DRIVER --> BACKEND
    SBK --> PERL
    PERL --> LOGGER
    LOGGER -->|GrpcLogger| SBM
    GEM --> SBM
```

## Build-time module boundaries

```mermaid
flowchart LR
    PERL[perl]
    WEB[sbk-web-console]
    API[sbk-api]
    DRIVERS[drivers projects]
    ROOT[root distribution]
    SBM[sbm]
    GEM[sbk-gem]
    YAL[sbk-yal]
    GYAL[sbk-gem-yal]

    API --> PERL
    API --> WEB
    DRIVERS --> API
    ROOT --> API
    ROOT --> DRIVERS
    SBM --> API
    GEM --> SBM
    YAL --> API
    GYAL --> GEM
```

| Module | Owns | Does not own |
|---|---|---|
| `perl` | Timestamp abstraction, concurrent queues, latency windows, percentiles, periodic/total recording | Storage semantics or CLI driver configuration |
| `sbk-web-console` | Local HTTP console server/client protocol, bounded histories, standalone process, and browser resources | Benchmark workers, measurement aggregation, or application-specific logger adapters |
| `sbk-api` | Storage/logger SPIs, bootstrap, common CLI, worker orchestration, payload types | Vendor SDK calls |
| `drivers/*` | Backend configuration and I/O adaptation | Benchmark scheduling or general percentile computation |
| `sbm` | SBP/gRPC ingestion and multi-client aggregation | Remote process launch |
| `sbk-gem` | SSH connections, remote distribution/launch, embedded SBM lifecycle | Driver implementation |
| `sbk-yal` | Mapping a YML document to SBK arguments | A separate benchmark engine |
| `sbk-gem-yal` | Mapping YML to SBK-GEM arguments | A separate distributed protocol |

## Single-node bootstrap

The installed script starts `io.sbk.main.SbkMain`, configured by the Gradle application plugin.

```mermaid
sequenceDiagram
    participant Main as SbkMain
    participant Boot as Sbk
    participant Scan as Package scanners
    participant Driver as Storage driver
    participant Logger as RWLogger
    participant Bench as SbkBenchmark

    Main->>Boot: run(args)
    Boot->>Scan: scan driver and logger packages
    Scan-->>Boot: matching classes
    Boot->>Driver: construct selected -class
    Boot->>Logger: construct selected -out
    Boot->>Driver: addArgs(parameters)
    Boot->>Logger: addArgs(parameters)
    Boot->>Boot: parse and validate merged CLI
    Boot->>Driver: parseArgs(parameters)
    Boot->>Logger: parseArgs(parameters)
    Boot-->>Main: new SbkBenchmark(...)
    Main->>Bench: start()
```

Important details:

- `StoragePackage` scans `io.sbk.driver` and `RWLoggerPackage` scans `io.sbk.logger` using the Reflections library.
- Selection uses the simple class name case-insensitively. It is package scanning, not Java `ServiceLoader` registration.
- `-class` and `-out` are removed before the merged driver/logger argument parser handles the remainder.
- Without `-out`, `SystemLogger` is selected.
- The chosen driver's `DataType<T>` controls payload creation, sizing, and optional timestamp embedding.

Primary sources:

- `sbk-api/src/main/java/io/sbk/main/SbkMain.java`
- `sbk-api/src/main/java/io/sbk/api/impl/Sbk.java`
- `sbk-api/src/main/java/io/sbk/api/Package.java`
- `sbk-api/src/main/java/io/sbk/api/StoragePackage.java`
- `sbk-api/src/main/java/io/sbk/api/RWLoggerPackage.java`

## Benchmark lifecycle

`SbkBenchmark` is the lifecycle owner. Its states prevent a benchmark from being started or stopped twice.

```mermaid
stateDiagram-v2
    [*] --> BEGIN
    BEGIN --> RUN: start
    RUN --> END: workers finish
    RUN --> END: duration timeout
    RUN --> END: performance idle timeout
    RUN --> END: shutdown or error
    END --> [*]
```

At `start()` it:

1. Opens the selected `RWLogger`.
2. Calls `Storage.openStorage()` once for shared driver resources.
3. Calls `createWriter(id, params)` and `createReader(id, params)` for configured workers.
4. Wraps driver objects in `SbkWriter` and `SbkReader` harness workers.
5. Starts write/read PerL recorders where the selected action needs them.
6. Distributes count-based records across workers, preserving the remainder on the last worker.
7. Starts workers in configured steps and optionally delays between steps.
8. Schedules `stop()` for timed runs, applies the PerL performance-event idle
   deadline only to fixed-record runs, and also chains shutdown after worker completion.

At shutdown it stops new work, closes driver readers and writers, closes the storage and logger, shuts down executors, and completes the benchmark future. Duration-based shutdown can interrupt an SDK call, so drivers must tolerate interruption-related exceptions during normal teardown. The shared `-idletimeoutseconds` option defaults to 600 seconds and applies only when a positive `-records` target is used without `-seconds`. It must be strictly greater than the active logger reporting interval. It is evaluated by PerL only while its queues are empty and by SBM only while fixed-record mode is selected and its ingestion queues are empty; each positive result renews the full deadline. SBK-GEM forwards the mode and value to remote SBK clients and its embedded SBM.

PerL, SBK, SBM, and SBK-GEM use the same terminal lifecycle vocabulary. Their final log message identifies successful `-seconds` or `-records` mode completion, an explicit lifecycle stop, an `-idletimeoutseconds` exit, or an internal exception. A failure remains authoritative during cleanup: later recorder, logger, storage, SSH-session, or embedded-SBM failures are retained as suppressed causes and prevent a misleading successful completion message.

Primary source: `sbk-api/src/main/java/io/sbk/api/impl/SbkBenchmark.java`.

## Operation path and timing

The driver-facing types are deliberately layered:

- `Storage<T>` creates and owns driver resources.
- `DataWriter<T>` and `DataReader<T>` define higher-level record loops.
- `Writer<T>` and `Reader<T>` provide the common single-operation primitives and default timed behavior.
- `SbkWriter` and `SbkReader` choose the correct loop for duration/count, rate control, combined read/write operation, and request logging.

### Synchronous write

```mermaid
sequenceDiagram
    participant Worker as SbkWriter
    participant Writer as Driver Writer
    participant Time as Time
    participant Channel as PerlChannel

    Worker->>Writer: recordWrite(...)
    Writer->>Time: getCurrentTime()
    Writer->>Writer: writeAsync(payload)
    Note over Writer: returns null for synchronous completion
    Writer->>Time: getCurrentTime()
    Writer->>Channel: send(start, end, records, bytes)
```

### Asynchronous write

When `writeAsync()` returns a `CompletableFuture`, the default implementation records completion time in the future callback. Exceptional completion is passed to the PerL exception handler. A driver must not report a future as complete before the storage operation has reached the completion semantics promised by that driver.

### Read

`Reader.read()` returns one payload, while default reader methods time the operation, derive the byte count through `DataType<T>`, and submit the result. Callback or batch-oriented backends can implement the more specialized reader abstractions.

The harness already measures calls. A driver should only override timing helpers when its backend semantics require it, such as measuring a batch, embedding a producer timestamp, or avoiding an unavoidable adapter copy.

Primary sources:

- `sbk-api/src/main/java/io/sbk/api/Storage.java`
- `sbk-api/src/main/java/io/sbk/api/Writer.java`
- `sbk-api/src/main/java/io/sbk/api/Reader.java`
- `sbk-api/src/main/java/io/sbk/api/impl/SbkWriter.java`
- `sbk-api/src/main/java/io/sbk/api/impl/SbkReader.java`

## PerL measurement pipeline

PerL decouples measurement ingestion from statistics calculation.

```mermaid
flowchart LR
    W1[Worker 1] --> C[PerlChannel]
    W2[Worker 2] --> C
    WN[Worker N] --> C
    C --> Q[Selected timestamp queue array]
    Q --> R[Performance recorder]
    R --> P[Periodic window]
    R --> T[Total window]
    P --> L[RWLogger]
    T --> L
```

Each measurement contains start time, end time, record count, and byte count. PerL records all submitted operations rather than sampling them. The recorder drains concurrent queues and updates latency storage and counters away from the I/O worker.

By default, the selected array contains intrusive
`TimeStampMpscQueue` instances. Each submitted `TimeStampNode` is both the
measurement payload and its linked-queue node, so enqueue does not allocate a
second wrapper. `-mpscqueue false` selects the compatibility path based on JDK
`ConcurrentLinkedQueue<TimeStamp>`; `-mpscqueue true` selects the intrusive
path. The default comes from `MpscQueueEnable` in
`sbk-api/src/main/resources/sbk.properties`.

Queue topology is a separate concern. `qPerWorker` and `maxQs` remain
property-backed settings rather than public CLI options. SBK prints both the
effective queue implementation and topology after argument parsing, before the
benchmark starts.

The timestamp queues have a multiple-producer, single-consumer workload:
worker threads produce measurements and the PerL recorder consumes them.
`TimeStampMpscQueue` specializes for that contract; the JDK fallback retains
general MPMC Collection behavior. See
[the queue research guide](TIMESTAMP_MPSC_QUEUE.md) for linearization,
memory-ordering, reclamation, complexity, and benchmark evidence.

`TimeStampMpscQueue` uses one single-use `TimeStampNode` as both payload and
link. Producers publish through a CAS on the predecessor's `next` reference;
the single consumer owns `head`. Every 16 dequeues, the consumer
release-publishes a recovery head and self-links the retired predecessor
batch. A producer paused on an old node detects that self-link and resumes
from the recovery head. This bounds stale-chain retention without pooling
nodes or adding a consumer-side head CAS. The specialization is appropriate
only for PerL's many-producer/one-consumer hand-off; it is not intended for
multiple consumers, iterators, arbitrary removals, or general collection use.

The default recorder also avoids querying the clock on every queue scan.
While records are available it reuses `TimeStamp.endTime`. While all queues
are empty, `ElasticWait` parks and checks the clock only after an adaptively
calibrated batch. The learned parks-per-millisecond rate is retained in an
exponential moving average. When activity briefly interrupts idleness, the
first subsequent empty scan starts a clean idle sample from the last record
timestamp while retaining that learned rate. This prevents active time from
diluting calibration and adds no new clock read. Setting `sleepMS > 0`
selects the simpler sleeping recorder and bypasses `ElasticWait`.

The phrase “lock-free hot path” applies to harness measurement transport. It does not promise that a vendor SDK, filesystem, JVM scheduler, allocator, or backend is lock-free. Driver wrappers should avoid adding their own locks to the per-operation path. In `sbk-api`, PerL, and SBM, keep per-operation branches, live bookkeeping state, conversions, clock reads, allocation, dynamic dispatch, and non-inlined call depth to the measured minimum; choose mode-specific implementations during startup where benchmarks justify specialization. Do not add atomic, `VarHandle`, fence, volatile, mutex, monitor, or conditional-wait operations. Existing PerL queue publication primitives are part of its proven MPSC memory-ordering protocol and must not be removed without a correctness proof plus stress and performance evidence. SBK-GEM is lifecycle orchestration, not a per-record measurement hot path.

Primary sources:

- `perl/src/main/java/io/perl/api/PerlChannel.java`
- `perl/src/main/java/io/perl/api/TimeStampNode.java`
- `perl/src/main/java/io/perl/api/impl/TimeStampMpscQueue.java`
- `perl/src/main/java/io/perl/api/impl/TimeStampMpscQueueArray.java`
- `perl/src/main/java/io/perl/api/impl/ConcurrentLinkedQueueArray.java`
- `perl/src/main/java/io/perl/api/impl/PerformanceRecorderElasticWait.java`
- `perl/src/main/java/io/perl/api/impl/PerformanceRecorderIdleSleep.java`
- `perl/src/main/java/io/perl/api/impl/ElasticWait.java`
- `perl/src/main/java/io/perl/api/impl/PerlBuilder.java`

## Output boundary

`RWLogger` is both a lifecycle interface and a metrics sink. Implementations can print, write CSV, expose Prometheus metrics, or forward data over gRPC.

| Logger | Use |
|---|---|
| `SystemLogger` | Default terminal output |
| `Sl4jLogger` | SLF4J logging path |
| `CSVLogger` | CSV result persistence |
| `WebLogger` | Console/CSV behavior plus dependency-free local live graphs; see [WebLogger guide](WEB_LOGGER.md) |
| `PrometheusLogger` | Metrics endpoint plus inherited result behavior |
| `GrpcLogger` | SBP/gRPC forwarding to SBM |

Logger discovery follows the same class-scanning pattern as drivers. New loggers belong under `io.sbk.logger` and must implement `RWLogger`, usually by extending the existing abstract implementation.

## Distributed flow

SBM aggregates measurements; it does not generate storage load. SBK-GEM orchestrates load generators; it does not replace the driver or the single-node harness.

```mermaid
sequenceDiagram
    participant Operator
    participant GEM as SBK-GEM
    participant SBM
    participant HostA as Remote SBK A
    participant HostB as Remote SBK B
    participant Storage

    Operator->>GEM: start connections and SBK arguments
    GEM->>SBM: start embedded aggregator
    GEM->>HostA: reconcile Java and SBK, run SbkMain through Java agent
    GEM->>HostB: reconcile Java and SBK, run SbkMain through Java agent
    HostA->>Storage: driver operations
    HostB->>Storage: driver operations
    HostA->>SBM: GrpcLogger measurements
    HostB->>SBM: GrpcLogger measurements
    SBM-->>Operator: aggregate windows and totals
```

SBP messages and gRPC services are generated from protobuf definitions in `sbk-api/src/main/proto`. SBM receives client registrations and latency records through `SbmGrpcService`, queues them in `SbmLatencyBenchmark`, and merges them into periodic and total windows. The default gRPC port is 9717. For SBK-GEM coordinated runs, each remote SBK opens its storage and creates its workers before registering. After all prepared clients reach the registration barrier, SBM starts its aggregation consumer and reporting clock before GEM explicitly releases the pending registration responses. Deployment and remote driver initialization therefore do not dilute aggregate duration or throughput, and short benchmarks cannot run ahead of the aggregator.

Nanosecond SBM windows use an exact hybrid paged recorder. Low-occupancy
latency pages remain compact sorted primitive pairs and dense pages promote to
direct counter arrays. This reduces per-value hash storage and changes
periodic ordering work from sorting every exact latency to sorting active page
identifiers, without sampling or reducing percentile precision. Page geometry
and sparse promotion are configured in `sbm.properties`. Independent periodic
and total exact-memory targets avoid reusing the primitive map's payload-only
budget semantics. Periodic cache reclamation occurs only after a natural
reporting boundary, while total-window pressure prints and resets the total
before reclamation. Other time units keep the standard PerL recorder selection.

SBK-GEM uses Apache MINA SSHD for connection, exact-file transfer, and command execution. It supports password-first authentication with SSH-agent/OpenSSH key fallback, or passwordless public-key authentication when no password is configured. Server host-key verification is disabled by default so stale or changed `known_hosts` entries cannot block unattended authentication. Passwordless deployments can explicitly enable accept-new trust with `-hostkeycheck true`, using the launching user's `~/.ssh/known_hosts` or the path selected by `-knownhosts`: a previously unknown key is recorded on first contact, while a changed key is rejected. Supplying `-gempass` or `SBK_GEM_SSH_PASSWD` always disables host-key verification regardless of that option.

Remote execution uses two independent content-addressed deployments: an SBK-only archive and, only when required, a separately copied controller JDK. GEM reuses a same-or-newer preferred/PATH JDK; otherwise it creates or reuses one cached plain-tar JDK archive, streams that single file through Apache MINA SCP, and invokes the standard remote `tar` executable to extract it into an atomic staging directory. SBK archives likewise use a single-file bulk SCP stream and are extracted by the Java agent. SFTP is limited to remote-directory resolution, small agent installation, and atomic bootstrap metadata, avoiding serialized payload and lifecycle round trips. Directory resolution and agent verification/installation share one operation per physical target; the local agent is hashed once, and each target's Java/OS probe starts as soon as that target is ready instead of waiting at a cluster-wide phase barrier. Bootstrap progress reports the remaining hosts when this work exceeds the configured interval. Both content and executable/POSIX permission state participate in the managed JDK identity, and an unusable cached identity is repaired. A persisted controller-side metadata identity avoids rereading all JDK bytes when the installed JDK is unchanged. Physical work is deduplicated by SSH user, host, port, and configured path before bootstrap and again by the resolved case-sensitive path, so multiple logical clients on one target share agent, JDK, and SBK provisioning. Gradle generates an identity for the installed runtime, allowing an unchanged cached plain-tar archive to be reused without rehashing the full runtime or archive. A new archive hashes runtime files once and calculates its SHA-256 while writing; the remote agent always performs full archive and per-file verification, and an archive digest mismatch triggers one controller rebuild/retry. Per-identity locks serialize matching archive builds; the cache-wide management lock is limited to enumeration and cleanup. The SBK archive preserves contained relative symbolic links, normalized directory modes, and executable file state; escaping or absolute links are rejected. A small packaged Java agent is installed through SFTP and receives length-delimited, aggregate-size-bounded requests over SSH stdin. It probes Java/OS, verifies and extracts the SBK archive with Commons Compress, checks every file digest, atomically activates the runtime, manages lifecycle locks/current markers/leases locally, removes retired runtimes, and launches `io.sbk.main.SbkMain` with `ProcessBuilder`. Remote deployment does not generate shell scripts; bulk transfer requires the standard remote `scp` executable and first-time JDK provisioning also requires standard `tar`, both available on supported Linux and macOS hosts. Controller, containers, and nodes must use the same supported operating system, Linux or macOS; CPU architecture and distribution/version are deliberately not part of compatibility. Exact SBK and Java identities are reused independently, so changing one does not force copying the other. Successful Java probe responses are retained for platform verification, and post-provisioning probes run concurrently, eliminating redundant sequential SSH commands. Startup never holds the lifecycle-state lock across network or disk work, allowing shutdown to cancel an in-progress deployment. Connection and control operations use bounded platform-thread concurrency, JDK/SBK transfers use an independent smaller bounded pool, and benchmark-duration remote commands use virtual threads; controller platform-thread count therefore does not grow with the node inventory. `packagescleanup=true` retires inactive non-current managed SBK identities without deleting current/leased runtimes or user-managed JDKs. Bundle preparation and both JDK/SBK transfer paths report bytes, percentage, throughput, and ETA; activation and lifecycle work emit bounded progress messages through the same lifecycle scheduler.

The complete deployment description above is the `-fullcopy true` flow. By default,
`-fullcopy false` makes the same Gradle build contract select the requested
driver's transitive SBK closure and a runtime-only Java image generated with
`jlink`. Gradle's independent
`generateSbkCompactJavaRuntime` task produces the same Java image without
SBK-GEM; normal `build`, `installDist`, and `distTar` outputs remain complete.
Both compact artifacts retain independent content identities, cache reuse,
parallel host transfer, verification, and atomic activation.

GEM resolves `-fullcopy` once at benchmark construction. Full-artifact
selection lives in `FullRuntimeCopyPolicy`, while compact Java and
driver-scoped SBK selection lives in `MinimalRuntimeCopyPolicy`. Both policies
return the same `ManagedJavaRuntime` and `SbkRuntimeBundle` abstractions to the
shared orchestration path, so archive progress, caching, transfer, verification,
activation, leases, and cleanup are implemented only once.

The generated `bin/sbk-gem` and `bin/sbk-gem-yal` launchers select the local
distribution through the internal `sbk.appHome` system property. SBK-GEM
validates and packages that standard installation, then the remote agent
launches `SbkMain` directly from its verified pathing and main JARs. Directory
and launcher overrides are not part of the deployment contract.

## Configuration layers

Configuration comes from several layers:

1. Gradle/application defaults establish program name, main class, and application home.
2. Resource property files provide harness, logger, SBM, GEM, and driver defaults.
3. Common CLI options are registered by `SbkParameters`.
4. The selected logger and driver add their CLI options.
5. Parsed CLI values override applicable defaults.
6. YAL variants translate YML entries into the same argument model; they do not bypass normal validation.

For the PerL transport, `MpscQueueEnable` supplies the default and the common
`-mpscqueue true|false` option overrides it for that benchmark. The topology
properties `qPerWorker` and `maxQs` are validated when `SbkParameters` loads
`sbk.properties`, but are not exposed as command-line options. This keeps an
A/B queue comparison to one explicit runtime switch while preventing
accidental topology changes between runs.

Use generated `-help` output as the authority for accepted options. Use the relevant resource property file as the authority for defaults that are not printed in help.

Operational defaults have one owning source:

| Settings | Authoritative source |
|---|---|
| JDK download version, checksums, and bootstrap timeouts | `gradle/sbk-java-bootstrap.properties` |
| Build repositories, publication endpoints, and Maven POM identity | `gradle.properties` |
| Release-gate topology, timing, workload, and artifact limits | `gradle/release-qualification.properties` |
| SBK command defaults | `sbk-api/src/main/resources/sbk-command.properties` |
| SBK PerL queue topology | `sbk-api/src/main/resources/sbk.properties` |
| SBK lifecycle, executor reserve, and shutdown timeouts | `sbk-api/src/main/resources/sbk-runtime.properties` |
| Logger reporting and request-ID dimensions | `sbk-api/src/main/resources/logger.properties` |
| Direct SBK Prometheus endpoint defaults | `sbk-api/src/main/resources/metrics.properties` |
| SBM client transport queue, flow-control stall, and close timeouts | `sbk-api/src/main/resources/sbmhost.properties` |
| SBM server defaults, including its default action | `sbm/src/main/resources/sbm.properties` |
| SBM/GEM aggregate Prometheus endpoint defaults | `sbm/src/main/resources/sbm-metrics.properties` |
| Web Console server, client, browser, retention, and log settings | `sbk-web-console/src/main/resources/webconsole.properties` |
| GEM orchestration and bounded diagnostic settings | `sbk-gem/src/main/resources/gem.properties` |
| SBK-YAL default input file | `sbk-yal/src/main/resources/sbk-yal.properties` |
| SBK-GEM-YAL default input file | `sbk-gem-yal/src/main/resources/gem-yal.properties` |

The SBP failure text limits are protocol constraints rather than operator tuning;
`SbpFailureLimits` is the shared client/server authority. Container manifests read
service ports from their runtime properties where the build DSL supports it. Fixed
HTTP paths, status codes, unit conversions, exit codes, collection dimensions, and
algorithm constants remain named source constants because changing them is a code or
protocol change rather than runtime configuration.

## Packaging and class loading

The root distribution includes `sbk-api` and all drivers declared as API dependencies in `build-drivers.gradle`. Drivers also must be included in `settings-drivers.gradle` so Gradle creates their projects.

The generated launcher uses a pathing JAR whose manifest points at runtime dependencies. After changing dependencies, force regeneration:

```bash
./gradlew clean :pathingJar :installDist --rerun-tasks
```

A driver can compile successfully yet be unavailable at runtime if either registration file is missing or the distribution/pathing JAR is stale.

## Safe extension boundaries

| Desired change | Primary location |
|---|---|
| Support a backend | `drivers/<name>/` plus both driver registration files |
| Add a workload-wide CLI option | `SbkParameters` and `ParameterOptions` contracts |
| Add a payload representation | `io.sbk.data` implementation |
| Add result output | `io.sbk.logger` implementation |
| Change latency storage or percentile behavior | `perl` |
| Change timestamp queue selection or topology | `perl`, `SbkParameters`, and `SbkBenchmark` |
| Change distributed aggregation | `sbm` and protobuf compatibility review |
| Change remote launch | `sbk-gem` |
| Change YML mapping | the applicable YAL module |

Crossing these boundaries should be an explicit design decision. In particular, do not put vendor behavior into `sbk-api` or generic measurement behavior into a driver.

## Failure propagation

- Synchronous driver failures normally surface as `IOException` and end the worker path.
- Asynchronous failures complete the returned future exceptionally and are forwarded through the PerL exception handler.
- Recorder or logger failures invoke benchmark shutdown through configured exception handlers.
- Timed shutdown can race with SDK dispatchers; interruption and rejected-execution conditions during teardown may represent a clean stop.
- Remote GEM failures add SSH, transfer, remote-process, and callback-network failure domains.

When diagnosing a failure, identify the boundary first: argument discovery, distribution/classpath, driver open, worker I/O, measurement recorder, logger, gRPC aggregation, or SSH orchestration.

## Source reading order

For a practical code walkthrough:

1. `sbk-api/src/main/java/io/sbk/main/SbkMain.java`
2. `sbk-api/src/main/java/io/sbk/api/impl/Sbk.java`
3. `sbk-api/src/main/java/io/sbk/api/Storage.java`
4. One simple driver, such as `drivers/file/`
5. `sbk-api/src/main/java/io/sbk/api/impl/SbkBenchmark.java`
6. `SbkWriter`, `SbkReader`, `Writer`, and `Reader`
7. `perl/src/main/java/io/perl/api/impl/PerlBuilder.java`
8. The PerL queue and recorder selected by that builder
9. `drivers/perlbench/` and [the queue research guide](TIMESTAMP_MPSC_QUEUE.md)
   for a controlled end-to-end queue comparison
10. `GrpcLogger` and `SbmGrpcService` for distributed reporting
11. `SbkGem` and `SbkGemBenchmark` for remote orchestration

## Architectural invariants

Review changes against these invariants:

- Storage-specific behavior remains behind `Storage<T>` and its reader/writer objects.
- Benchmark timing is performed consistently by the harness unless a documented backend constraint requires an override.
- Every accepted operation contributes to measurement; no silent sampling is introduced.
- Histogram and reporting work stays off the driver operation path.
- Driver wrappers do not add synchronization to the per-record path.
- Async completion represents the documented storage completion point.
- Driver discovery and distribution registration remain consistent.
- Protobuf and gRPC changes consider mixed-version compatibility.
- Shutdown remains idempotent and tolerates in-flight I/O.

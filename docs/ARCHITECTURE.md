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
    API[sbk-api]
    DRIVERS[drivers projects]
    ROOT[root distribution]
    SBM[sbm]
    GEM[sbk-gem]
    YAL[sbk-yal]
    GYAL[sbk-gem-yal]

    API --> PERL
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
8. Schedules `stop()` for timed runs and also chains shutdown after worker completion.

At shutdown it stops new work, closes driver readers and writers, closes the storage and logger, shuts down executors, and completes the benchmark future. Duration-based shutdown can interrupt an SDK call, so drivers must tolerate interruption-related exceptions during normal teardown.

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
    C --> Q[Concurrent queue array]
    Q --> R[Performance recorder]
    R --> P[Periodic window]
    R --> T[Total window]
    P --> L[RWLogger]
    T --> L
```

Each measurement contains start time, end time, record count, and byte count. PerL records all submitted operations rather than sampling them. The recorder drains concurrent queues and updates latency storage and counters away from the I/O worker.

The phrase “lock-free hot path” applies to harness measurement transport. It does not promise that a vendor SDK, filesystem, JVM scheduler, allocator, or backend is lock-free. Driver wrappers should avoid adding their own locks to the per-operation path.

Primary sources:

- `perl/src/main/java/io/perl/api/PerlChannel.java`
- `perl/src/main/java/io/perl/api/impl/ConcurrentLinkedQueueArray.java`
- `perl/src/main/java/io/perl/api/impl/PerformanceRecorderIdleBusyWait.java`
- `perl/src/main/java/io/perl/api/impl/PerformanceRecorderIdleSleep.java`
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
    GEM->>HostA: reconcile Java and SBK, export SBK_JAVA_HOME, start SBK
    GEM->>HostB: reconcile Java and SBK, export SBK_JAVA_HOME, start SBK
    HostA->>Storage: driver operations
    HostB->>Storage: driver operations
    HostA->>SBM: GrpcLogger measurements
    HostB->>SBM: GrpcLogger measurements
    SBM-->>Operator: aggregate windows and totals
```

SBP messages and gRPC services are generated from protobuf definitions in `sbk-api/src/main/proto`. SBM receives client registrations and latency records through `SbmGrpcService`, queues them in `SbmLatencyBenchmark`, and merges them into periodic and total windows. The default gRPC port is 9717.

SBK-GEM uses Apache MINA SSHD for connection, copy, and command execution. It supports passwordless public-key authentication through the launching user's SSH agent and OpenSSH-configured identity files; an explicitly configured password is an optional fallback. Server identities are verified against that user's `~/.ssh/known_hosts`, or the path selected by `-knownhosts`, so unknown or changed host keys fail before deployment. Strict checking is the default; `-hostkeycheck false` is an explicit, insecure opt-out for isolated environments. Before launch it checks the requested Java major version and exact SBK version on every host. It can reuse Java from `PATH` or a configured Java home, or copy its local JVM when provisioning is enabled. Each launch exports the verified node-specific `SBK_JAVA_HOME`. With `copy=true` (the default), SBK is copied only to missing or mismatched targets. `delete=true` removes a mismatched installation before replacement, while `deleteafter=false` preserves the verified deployment after the run. GEM verifies copied versions, resolves each executable to a node-specific absolute path, and constructs remote SBK arguments with `GrpcLogger` pointing back to its embedded SBM instance.

## Configuration layers

Configuration comes from several layers:

1. Gradle/application defaults establish program name, main class, and application home.
2. Resource property files provide harness, logger, SBM, GEM, and driver defaults.
3. Common CLI options are registered by `SbkParameters`.
4. The selected logger and driver add their CLI options.
5. Parsed CLI values override applicable defaults.
6. YAL variants translate YML entries into the same argument model; they do not bypass normal validation.

Use generated `-help` output as the authority for accepted options. Use the relevant resource property file as the authority for defaults that are not printed in help.

## Packaging and class loading

The root distribution includes `sbk-api` and all drivers declared as API dependencies in `build-drivers.gradle`. Drivers also must be included in `settings-drivers.gradle` so Gradle creates their projects.

The generated launcher uses a pathing JAR whose manifest points at runtime dependencies. After changing dependencies, force regeneration:

```bash
./gradlew clean :pathingJar installDist --rerun-tasks
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
9. `GrpcLogger` and `SbmGrpcService` for distributed reporting
10. `SbkGem` and `SbkGemBenchmark` for remote orchestration

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

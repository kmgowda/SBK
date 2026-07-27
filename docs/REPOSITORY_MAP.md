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

# Repository map

This map helps readers locate ownership without searching the entire multi-project build.

## Top level

| Path | Purpose |
|---|---|
| `build.gradle` | Shared project configuration, distribution assembly, pathing JAR, start scripts, container/release integration |
| `settings.gradle` | Core module inclusion and delegation to driver settings |
| `settings-drivers.gradle` | Enabled driver Gradle projects |
| `build-drivers.gradle` | Drivers bundled into the root distribution |
| `gradle.properties` | Project and dependency versions |
| `gradle/` | Shared Java, application, quality, publishing, and IDE Gradle logic |
| `checkstyle/` | Style configuration and allowed import package prefixes |
| `config/` | Deployment and monitoring configuration assets |
| `docs/` | Engineering, architecture, agent, and specification documentation |
| `grafana/` | Dashboards and local monitoring stack |
| `dockers/` | Docker image-generation support |
| `kubernetes/` | Kubernetes examples |
| `.github/` | CI, templates, and repository automation |

## `perl/`

Important packages:

| Package | Responsibility |
|---|---|
| `io.perl.api` | Public measurement contracts, `TimeStamp`, intrusive `TimeStampNode`, windows, and channels |
| `io.perl.api.impl` | Intrusive and JDK queue paths, queue arrays, recorder loops, histogram/window implementations, builder |
| `io.perl.config` | Measurement defaults |
| `io.perl.logger` | PerL-level reporting interfaces and implementations |
| `io.time` | Millisecond, microsecond, and nanosecond clock abstractions |
| `io.state` | Shared lifecycle states |

Start at `io.perl.api.impl.PerlBuilder` to see how configuration constructs
the recorder. For SBK launches, `SbkParameters` loads the PerL defaults and
`SbkBenchmark` applies the common `-mpscqueue` override before invoking that
builder. The specialized queue algorithm is documented in
[TIMESTAMP_MPSC_QUEUE.md](TIMESTAMP_MPSC_QUEUE.md).

For recorder clock-query behavior, follow
`PerformanceRecorderIdleBusyWait` into `ElasticWait`. Records provide the
current time through their existing `endTime`; empty scans park and use an
adaptive counter before querying the clock. `ElasticWait.startIdle()` resets
only the current idle sample after an active interval while retaining the
learned moving-average park rate. Its deterministic transition and overflow
tests are in `ElasticWaitTest`; the Null driver supplies an end-to-end idle
reporting test.

## `sbk-api/`

| Package | Responsibility |
|---|---|
| `io.sbk.main` | Single-node executable entry point |
| `io.sbk.api` | Storage, reader, writer, benchmark, worker, and discovery contracts |
| `io.sbk.api.impl` | Benchmark bootstrap, lifecycle, worker adapters, and rate controller |
| `io.sbk.params` | Public parsed-parameter contracts |
| `io.sbk.params.impl` | Common CLI definition and parser |
| `io.sbk.data` | Payload abstraction and built-in representations |
| `io.sbk.logger` | Logger contracts, counters, request-event hooks |
| `io.sbk.logger.impl` | System, SLF4J, CSV, Prometheus, and gRPC loggers |
| `io.sbk.action` | Workload action selection |
| `io.sbk.thread` | Executor type selection |
| `io.sbk.utils` | Argument and general utilities |
| `src/main/proto` | SBP/gRPC service and message definitions |
| `src/main/resources` | Harness and logger defaults, banner, logging configuration |

## Driver project

A typical driver is:

```text
drivers/<name>/
├── build.gradle
├── README.md
└── src/main/
    ├── java/io/sbk/driver/<ClassName>/
    │   ├── <ClassName>.java
    │   ├── <ClassName>Config.java
    │   ├── <ClassName>Writer.java
    │   └── <ClassName>Reader.java
    └── resources/<name>.properties
```

Some drivers legitimately differ: they may share a JDBC implementation, provide callback or asynchronous readers, omit unsupported read/write directions, or use a custom payload type.

The synthetic drivers have no backend:

| Driver | Location | Purpose |
|---|---|---|
| `Null` | `drivers/null` | Pending operations, idle windows, timeout, interruption, and shutdown |
| `PerlBench` | `drivers/perlbench` | Immediate completions for end-to-end SBK/PerL queue throughput and allocation comparison |

They are intentionally separate. The default Null write does not produce a
completed timestamp; PerlBench is designed to produce timestamps as fast as
the harness permits.

## Distributed modules

### `sbm/`

- `io.sbm.main.SbmMain`: executable entry point.
- `io.sbm.api.impl.Sbm`: bootstrap and argument wiring.
- `SbmBenchmark`: gRPC server and recorder lifecycle.
- `SbmGrpcService`: SBP endpoint.
- `SbmLatencyBenchmark`: concurrent ingestion and aggregation loop.
- `io.sbm.logger`: aggregated-output contracts and Prometheus implementation.

### `sbk-gem/`

- `io.gem.main.SbkGemMain`: executable entry point.
- `io.gem.api.impl.SbkGem`: discovery, argument parsing, and distributed benchmark construction.
- `SbkGemBenchmark`: embedded SBM and remote-run lifecycle.
- `SshSession` and `SshUtils`: Apache MINA SSHD integration.
- `io.gem.params`: remote connection and benchmark argument model.

### YAL modules

- `sbk-yal`: `SbkYal` and `SbkYmlMap` translate a YML file into standard SBK arguments.
- `sbk-gem-yal`: `SbkGemYal` and `SbkGemYmlMap` do the same for distributed execution.

## Tests

Tests currently concentrate on PerL, utility behavior, and a small number of drivers. Locate them with:

```bash
find . -path '*/src/test/*' -type f | sort
```

Because many vendor drivers require external services, their strongest verification is module checks plus a controlled integration smoke test against the real backend.

## Common change map

| Change | Inspect first | Verify first |
|---|---|---|
| Driver bug | Driver storage/writer/reader and properties | `./gradlew :drivers:<name>:check` |
| New driver | `drivers/sbktemplate`, similar driver, both registration files | New driver `check` |
| Common CLI | `SbkParameters`, `ParameterOptions`, worker consumers | `./gradlew :sbk-api:check` |
| Timing or percentiles | `Writer`/`Reader`, PerL builder/window/recorder | `./gradlew :perl:check :sbk-api:check` |
| Elastic idle calibration | `ElasticWait`, `PerformanceRecorderIdleBusyWait`, `ElasticWaitTest`, Null idle test | `./gradlew :perl:test :drivers:null:test` |
| Timestamp queue algorithm | `TimeStampNode`, `TimeStampMpscQueue`, queue arrays/channels | `./gradlew :perl:concurrencyCheck :perl:timeStampQueuePerformanceTest` |
| End-to-end queue comparison | `drivers/perlbench`, `SbkParameters`, `SbkBenchmark` | `./gradlew :drivers:perlbench:check :sbk-api:check` |
| Logger | `RWLogger`, `AbstractRWLogger`, similar implementation | `./gradlew :sbk-api:check` |
| gRPC aggregation | Proto definitions, `GrpcLogger`, `SbmGrpcService` | `./gradlew :sbm:check` |
| Remote launch | `SbkGem`, `SbkGemBenchmark`, SSH classes | `./gradlew :sbk-gem:check` |
| Distribution dependency | Root build and pathing JAR | `./gradlew clean :pathingJar installDist --rerun-tasks` |

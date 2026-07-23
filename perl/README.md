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
| `LatencyRecordWindow` | Latency accumulation and reporting contract |
| `PeriodicRecorder` | Periodic-window processing |
| `PerformanceRecorder` | Queue-draining recorder lifecycle |
| `PerlBuilder` | Selects time, queues, recorder, and latency storage from configuration |

Implementations live primarily under `io.perl.api.impl`. Clock representations live under `io.time`.

## Concurrency model

Workers submit records to `PerlChannel`; recorder logic drains the configured queue array and updates latency windows. Histogram and percentile work therefore does not execute in the driver operation itself. PerL records every submitted measurement rather than selecting a sample.

The measurement transport is designed to avoid explicit locks in the producer path. This statement does not imply that the JVM, vendor client, operating system, or storage system is lock-free.

## Configuration

Defaults are in `src/main/resources/perl.properties` and `sbk-api/src/main/resources/sbk.properties`. They control queue counts, idle behavior, latency storage limits, and histogram fallback. Use `PerlBuilder` as the source-level entry point for understanding how a configuration selects implementations.

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
`perl/build/reports/jmh/cqueue-performance.json`. This environment-sensitive
test is intentionally separate from `check`; correctness and stress tests
remain part of the normal build.

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

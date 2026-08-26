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

# SBK storage drivers

Each subdirectory is a Gradle project adapting one backend or local data structure to the `Storage<T>` SPI. The authoritative inventory, lifecycle contract, implementation rules, and verification procedure are in [docs/DRIVER_GUIDE.md](../docs/DRIVER_GUIDE.md).

## Find a driver

```bash
# Enabled Gradle projects
rg "^include 'drivers:" settings-drivers.gradle

# Driver source classes
rg "implements Storage" drivers/*/src/main/java -g '*.java'

# Runtime discovery after building
./gradlew installDist
./build/install/sbk/bin/sbk -help
```

The per-driver README describes backend prerequisites and examples. Always confirm current flags through:

```bash
./build/install/sbk/bin/sbk -class <driver> -help
```

Two synthetic drivers intentionally cover different harness states:

| Driver | Use |
|---|---|
| [`Null`](null/README.md) | Pending operations, zero-record periodic windows, timeout, interruption, and shutdown |
| [`PerlBench`](perlbench/README.md) | High-rate completed operations and reproducible `TimeStampMpscQueue` versus JDK `ConcurrentLinkedQueue` comparisons |

Neither produces storage-system results. Do not use `Null` as the queue
throughput baseline: its default operation deliberately remains incomplete.

## Status distinctions

- Enabled: present in both `settings-drivers.gradle` and `build-drivers.gradle`.
- Source-only/disabled: present in the tree but commented out of registration.
- Template: `sbktemplate`, which must never be treated as a benchmark backend.

ChromaDB, HaloDB, and Ignite are currently disabled. ChromaDB is source-only because its Java client adds an approximately 829 MiB all-platform local-embedding dependency closure to the aggregate distribution. HaloDB's external package availability is a known constraint.

## Build one driver

```bash
./gradlew :drivers:<name>:check
```

Driver completion also requires the full build, installed-distribution discovery, and a controlled real-backend smoke test. See [CONTRIBUTING.md](../CONTRIBUTING.md).

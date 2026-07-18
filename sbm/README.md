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

# SBM: Storage Benchmark Monitor

SBM is SBK's distributed measurement aggregator. SBK clients using `GrpcLogger` send SBP/gRPC latency records to SBM, which merges them into cluster-wide periodic and total statistics.

SBM does not execute storage operations and does not launch remote processes. Use SBK for load generation and SBK-GEM for SSH orchestration.

## Data flow

```mermaid
flowchart LR
    A[SBK client A / GrpcLogger] --> G[SBM gRPC service]
    B[SBK client B / GrpcLogger] --> G
    G --> Q[Concurrent ingestion queues]
    Q --> R[Aggregate recorder]
    R --> O[Prometheus and result output]
```

The default gRPC port is `9717`. Container configuration also exposes the configured metrics port. Keep the gRPC service on a trusted benchmark network unless an external security layer is provided.

## Build

```bash
./gradlew :sbm:check
./gradlew :sbm:installDist
```

The launcher is generated under `sbm/build/install/sbm/bin/sbm`.

## Run

Display authoritative options:

```bash
./sbm/build/install/sbm/bin/sbm -help
```

Start with defaults:

```bash
./sbm/build/install/sbm/bin/sbm
```

Then point one or more installed SBK clients at it:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk.bin \
  -writers 1 -size 4096 -seconds 30 \
  -out GrpcLogger -sbm <sbm-host> -sbmport 9717
```

Use a hostname or address reachable from every SBK client. Firewalls, containers, and NAT must permit the callback path.

## Internal ownership

| Class | Responsibility |
|---|---|
| `io.sbm.main.SbmMain` | Executable entry point |
| `io.sbm.api.impl.Sbm` | Logger discovery, arguments, and benchmark construction |
| `SbmBenchmark` | gRPC server and aggregate-recorder lifecycle |
| `SbmGrpcService` | SBP registration and latency-record endpoint |
| `SbmLatencyBenchmark` | Concurrent queue ingestion and window dispatch |
| `SbmTotalWindowLatencyPeriodicRecorder` | Periodic and total aggregate windows |
| `SbmPrometheusLogger` | Aggregated output and metrics |

Protocol sources are under `sbk-api/src/main/proto`; generated Java/gRPC sources are build products.

## Operational checks

- Confirm every client uses compatible SBP/protobuf definitions.
- Synchronize clocks when interpreting cross-host timestamps or correlated events.
- Record client count, worker count, network topology, interval, and latency bounds.
- Watch rejected registrations, disconnected clients, invalid latencies, and discarded lower/upper values.
- Do not combine already calculated client percentiles; SBM aggregates latency records/windows before reporting percentiles.

## Containers and dashboards

The module's Jib configuration builds an `sbm` image and declares its service/metrics ports. Repository monitoring assets are under [`grafana/`](../grafana/) and container guidance under [`dockers/`](../dockers/).

## Further reading

- [Distributed architecture](../docs/ARCHITECTURE.md#distributed-flow)
- [Detailed SBM internals](../docs/sbk-internals.md#6-sbm--the-distributed-results-aggregator)
- [SBK-GEM](../sbk-gem/README.md)

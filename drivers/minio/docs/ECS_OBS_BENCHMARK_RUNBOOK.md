<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Dell ECS/ObjectScale S3 performance benchmark runbook

This is the operational runbook for measuring Dell ECS/ObjectScale—also
called ECS/OBS in some environments—with SBK's `MinIO` driver. It starts with
a safe qualification, progresses to reproducible performance matrices, and
defines when a result is valid.

Use these companion documents:

- [MinIO/S3 driver README](../README.md): complete driver option
  reference and non-ECS backends.
- [MinIO/S3 implementation](IMPLEMENTATION.md): exact request,
  byte, concurrency, memory, retry, and lifecycle semantics.
- [ECS/OBS agent runbook](ECS_OBS_AGENT_RUNBOOK.md): deterministic workflow for
  Devin, Windsurf, Codex, Cursor, and other software agents.
- [Distributed benchmark runner](../../../.devin/skills/sbk-distributed-benchmark-runner/SKILL.md):
  SBK-GEM topology and remote-load-generator safety.

## 1. Safety and validity rules

1. Use a dedicated namespace, bucket, prefix, and Object User approved for
   benchmarking. Never point `-recreate true`, delete, or bucket-delete
   workloads at application data.
2. Store S3 credentials in `SBK_S3_ACCESS_KEY` and `SBK_S3_SECRET_KEY` or an
   approved secret launcher. Do not put secrets in committed commands, YML,
   shell history, issue text, or benchmark reports.
3. ECS management credentials are not S3 credentials. SBK needs an Object
   User access/secret pair.
4. Use the S3 data plane: normally `9020` for HTTP or `9021` for HTTPS. Ports
   `443` and `4443` are management paths, not S3 benchmark endpoints.
5. Prefer HTTPS. The confirmed lab commands below use HTTP only because
   `10.236.66.181:9020` is an isolated test endpoint supplied for this work.
6. First use fixed `-records` to prove exact completion. Only then use timed
   `-seconds` runs for saturation.
7. Reject a run with a nonzero exit, `BenchmarkCleanupTimeoutException`, S3 or
   I/O errors, unexpected endpoint failures/retries, invalid latencies,
   unexplained discarded latencies, missing Total output, or a load-generator
   bottleneck that is not the subject of the test.
8. Do not compare a single run. Use at least three measured repetitions after
   warm-up, retain every raw result, and report variability.

## 2. What must be recorded before testing

Create a test record containing:

| Area | Required facts |
|---|---|
| Objective | PUT/GET bandwidth, small-object operations/sec, Range GET, LIST/stat metadata rate, multipart, mixed workload, or scalability |
| SBK | Git commit/version, JDK, exact command/YML, logger, JVM options |
| Load generator | host count, CPU/NUMA, memory, NIC speed, MTU, OS, route/RTT, CPU and network telemetry |
| ECS/ObjectScale | version, node/appliance count and type, disks, front-end network, replication group, protection policy, capacity/fill level, health/rebuild state |
| S3 identity | endpoint list, namespace, dedicated bucket/prefix, Object User permissions; never copy the secret into the record |
| Workload | operation mix, object-size distribution, workers, async depth, multipart part size, duration/count, data compressibility/dedupability, retries |
| Controls | TLS, checksum, SSE, versioning, cache state, concurrent cluster activity |
| Acceptance | expected records, allowed errors/retries, latency/throughput target, load-host saturation threshold |

Dell recommends locating a bucket in the VDC closest to the application and
keeping fewer than 1,000 buckets in one namespace for best performance. Do not
create one bucket per request to simulate ordinary object I/O.

## 3. Build and discover the current contract

From the repository root:

```bash
./gradlew :drivers:minio:check :installDist
./build/install/sbk/bin/sbk -class minio -help
./build/install/sbk/bin/sbk-gem -class minio -help
```

Source and generated help are authoritative. Regenerate help after changing
branches or versions. The dated lab evidence later in this runbook records the
exact SBK version used for those measurements.

## 4. Prepare ECS access

Obtain these values from the ECS administrator:

```bash
export SBK_S3_ACCESS_KEY='<ecs-object-user>'
export SBK_S3_SECRET_KEY='<ecs-object-user-secret>'

export SBK=./build/install/sbk/bin/sbk
export ECS_ENDPOINT='http://10.236.66.181:9020'
export ECS_NAMESPACE='sbk-ns'
export ECS_BUCKET='sbk-doc-runbook-20260831'
export ECS_PREFIX='qualification-1m'
```

The access key and secret must belong to an ECS Object User in the intended
namespace. Do not reuse the management administrator password.

For TLS, use `https://<endpoint>:9021`. Install the ECS CA certificate in the
load-generator trust store. Use `-insecure true` only for an explicitly
approved lab with a self-signed certificate.

### Endpoint preflight

An unauthenticated S3 probe should return S3 XML, commonly an `AccessDenied`
error—not HTML or a management JSON response:

```bash
curl -sS -D - "$ECS_ENDPOINT/" -o /tmp/ecs-s3-probe.xml
head -20 /tmp/ecs-s3-probe.xml
```

Then let SBK validate SigV4, namespace routing, permission, and bucket setup
with a tiny fixed-count PUT:

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" \
  -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "$ECS_PREFIX" \
  -writers 1 -size 4096 -records 10 -time ms \
  -endpoint-metrics true
```

Expected: exit 0, exactly 10 Total records, endpoint `operations=10`,
`retries=0`, `failures=0`, zero invalid/discarded latencies, and successful
shutdown.

## 5. Required qualification sequence

### Step 1: populate a reusable object set

This command creates 1 MiB objects using two synchronous writers. It does not
empty an existing bucket.

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" \
  -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "$ECS_PREFIX" \
  -writers 2 -size 1048576 -records 100 -time ms \
  -data-seed 20260831 -data-compressibility 0 -data-dedupable false \
  -warmup-requests 2 -warmup-operation put-get \
  -endpoint-metrics true
```

Use `-recreate true` only when the bucket is disposable and complete deletion
of all versions/objects is explicitly intended.

### Step 2: full-object GET correctness

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" \
  -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "$ECS_PREFIX" \
  -readers 2 -size 1048576 -records 100 -time ms \
  -read-operation get -verify-read-size true \
  -endpoint-metrics true
```

The bucket/prefix must already contain objects. Startup performs one bounded
catalog LIST; timed GETs do not perform a HEAD before every request.

### Step 3: Range GET correctness

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" \
  -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "$ECS_PREFIX" \
  -readers 2 -size 4096 -records 100 -time ms \
  -read-operation range-get \
  -range-offset 8192 -range-length 4096 \
  -range-offset-distribution sequential \
  -range-window-length 1048576 -range-alignment 4096 \
  -verify-read-size true -endpoint-metrics true
```

### Step 4: LIST correctness

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" \
  -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -readers 1 -size 1 -records 20 -time ms \
  -read-operation list \
  -list-prefixes "$ECS_PREFIX" -list-max-keys 100 \
  -list-max-entries 1000 -list-api-version 2 \
  -endpoint-metrics true
```

For LIST, use records/sec and latency. SBK reports zero bytes because object
sizes in LIST metadata are not response-wire or object-payload throughput.

### Step 5: multipart correctness

First use fixed records so every upload can drain before shutdown:

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" \
  -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix qualification-mpu-15m \
  -writers 1 -size 15728640 -records 4 -time ms \
  -part-size 5242880 -mpu-concurrent-parts 3 \
  -data-seed 20260831 -data-compressibility 0 -data-dedupable false \
  -endpoint-metrics true
```

Expected: 4 timed records, 60 MiB, four endpoint operations, zero failures,
and exit 0. A timed multipart run that prints
`BenchmarkCleanupTimeoutException` is invalid even if a Total line appeared.

## 6. Performance workflow

After qualification, run each matrix point at least three times. Use a
separate prefix per point and retain a credential-free manifest:

```bash
-prefix "run-${RUN_ID}" -run-manifest "results/${RUN_ID}.json"
```

### Concurrency sweep

For each representative object size, test `1, 2, 4, 8, 16, ...` workers until:

- throughput stops increasing materially;
- p99/p99.9 rises beyond the service objective;
- ECS reports throttling or resource saturation; or
- the load generator reaches its CPU, heap, or network limit.

Do not begin with retries. Keep `-retry-max-attempts 1` and enable
`-endpoint-metrics true` so ECS saturation remains visible. Run a separate
production-behavior series with retries only if the application retries.
When retries are enabled, SBK prints a process-wide retry total even without
endpoint metrics; endpoint metrics remain necessary for URL-level attribution.

Baseline timed PUT:

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "put-${RUN_ID}" \
  -writers "$WORKERS" -size "$OBJECT_BYTES" \
  -seconds 300 -time ms \
  -retry-max-attempts 1 -endpoint-metrics true
```

Baseline timed GET over pre-populated data:

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "$POPULATED_PREFIX" \
  -readers "$READERS" -size "$OBJECT_BYTES" \
  -seconds 300 -time ms \
  -read-operation get -verify-read-size true \
  -retry-max-attempts 1 -endpoint-metrics true
```

Use 4 KiB/16 KiB for metadata/request-rate pressure, 64 KiB/1 MiB for common
object services, and 16/64/256 MiB for bandwidth and multipart studies. Match
the application's real distribution whenever known.

### Bounded asynchronous client

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "async-${RUN_ID}" \
  -writers 4 -size 1048576 -seconds 300 -time ms \
  -async true -async-depth 4 \
  -async-max-inflight 16 -async-max-memory-mb 1024 \
  -http-max-requests 32 -http-max-requests-per-host 32 \
  -endpoint-metrics true
```

Sweep depth `1, 2, 4, 8, 16` while keeping the process-wide and memory bounds
explicit. Async permits are acquired before timing, so latency excludes local
waiting for an available slot.

### Large-object multipart

```bash
"$SBK" -class minio \
  -url "$ECS_ENDPOINT" -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "multipart-${RUN_ID}" \
  -writers 4 -size 67108864 -seconds 300 -time ms \
  -part-size 8388608 -mpu-concurrent-parts 4 \
  -async-max-memory-mb 2048 -endpoint-metrics true
```

Part size must be 5 MiB through 5 GiB, and S3 allows at most 10,000 parts.
Measure part-size and part-concurrency sweeps separately from outer writer or
async-depth sweeps.

### Object-size distributions

```bash
# Fixed: every object uses -size.
-size 1048576 -object-size-distribution fixed

# Seeded, reproducible uniform random sizes; inclusive bounds.
-size 1048576 -object-size-distribution uniform:4096:1048576

# Deterministic sequential byte-by-byte sweep; not random.
-size 1048576 -object-size-distribution sweep:4096:1048576

# Exact repeating weighted mix; weights need not sum to 100.
-size 1048576 -object-size-distribution weighted:4096=70,65536=20,1048576=10
```

SBK reports aggregate latency for a mixed-size run. Run sizes independently
when per-size percentiles are required.

### Operation mixes

```bash
# Writer mix: exact repeating cycle.
-writers 8 -write-mix put=80,copy=15,tag-set=5

# Reader mix over existing objects.
-readers 8 -read-mix get=70,range-get=20,stat=10
```

Writer mixes may contain only writer operations; reader mixes only reader
operations. A mixed result is an application-level aggregate, not a
per-operation percentile breakdown.

### Data-reduction matrix

ECS compression/dedup behavior can dominate results. Test at least:

```bash
# Incompressible, anti-dedup data.
-data-compressibility 0 -data-dedupable false -data-seed 20260831

# 30% compressible, anti-dedup data.
-data-compressibility 30 -data-dedupable false -data-seed 20260831

# Fully compressible and dedup-friendly control.
-data-compressibility 100 -data-dedupable true -data-seed 20260831
```

Do not compare runs with different data shapes as if only client concurrency
changed.

### Metadata and namespace workloads

| Workload | Command fragment | Primary metrics |
|---|---|---|
| HEAD/stat | `-readers N -read-operation stat` | operations/sec, p50/p99 |
| Range GET | `-read-operation range-get -range-offset O -range-length L` | operations/sec, range MB/sec, latency |
| LIST | `-read-operation list -list-prefixes p1,p2 -list-max-keys 1000` | LIST operations/sec, entries/page context, latency |
| Tag GET | `-read-operation tag-get` | operations/sec, latency |
| Tag set/delete | `-write-operation tag-set` or `tag-delete` | operations/sec, latency |
| Server-side copy | `-write-operation copy -copy-prefix copied` | operations/sec, logical bytes/sec, latency |
| Update | `-write-operation update` | overwrite operations/sec, payload MB/sec, latency |
| Delete | `-write-operation delete` | operations/sec, latency, independent absence check |
| Bucket stat/list | `-read-operation bucket-stat` or `bucket-list` | account/bucket metadata operations/sec |
| Bucket create | `-write-operation bucket-create -bucket-prefix p` | create operations/sec; avoid large namespace bucket counts |
| Bucket delete | `-write-operation bucket-delete -bucket-targets b1,b2` | explicit empty-bucket deletion only |

## 7. Multiple endpoints and distributed load

### One process, several ECS data endpoints

Use one `-url` option with comma-separated URLs. Workers are assigned
round-robin; setup and catalog discovery use the first endpoint.

```bash
export ECS_ENDPOINTS='http://10.236.66.181:9020,http://10.236.66.182:9020,http://10.236.66.183:9020,http://10.236.66.184:9020'

"$SBK" -class minio \
  -url "$ECS_ENDPOINTS" -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix "multi-endpoint-${RUN_ID}" \
  -writers 8 -size 1048576 -seconds 300 -time ms \
  -endpoint-metrics true
```

Per-endpoint counters show worker attribution, not internal ECS disk/node
ownership. Use a load balancer/VIP when that is the production access path;
use explicit node IPs only when the test objective is front-end-path balance.

### Several load-generator hosts with SBK-GEM

Use SBK-GEM only when load must originate from several hosts. First make the
ordinary command pass on every load node. Then run a fixed-count distributed
qualification before a timed run.

```bash
export SBK_GEM_SSH_PASSWD='<inject-at-runtime-if-password-auth-is-required>'
export SBK_S3_ACCESS_KEY='<ecs-object-user>'
export SBK_S3_SECRET_KEY='<ecs-object-user-secret>'

./build/install/sbk/bin/sbk-gem \
  -nodes 'loadgen-a.example.test,loadgen-b.example.test' \
  -gemuser sbk -hostkeycheck true \
  -class minio \
  -url 'http://10.236.66.181:9020' \
  -bucket "$ECS_BUCKET" \
  -extra-headers "x-emc-namespace=$ECS_NAMESPACE" \
  -prefix gem-qualification \
  -writers 1 -size 1048576 -totalrecords 100 -time ms \
  -endpoint-metrics true
```

For independent existing-object catalogs, every remote process needs a unique
`-partition-index`. A single GEM command forwards one common driver argument
set, so do not set `-partition-count > 1` there while every process would keep
the default index zero. Use manually launched SBK/GrpcLogger clients with
unique indices, separate per-node commands, or pre-partitioned manifests when
catalog partitioning is required. Pure PUT does not require a startup catalog.

Use `-totalthroughput` for one aggregate MB/s target or `-totalrecords` with
`-seconds` for one aggregate records/sec target. Never sum a per-client rate
and call it an aggregate target without checking GEM's distribution output.

## 8. Complete option map

The following is the complete MinIO-driver option inventory. Defaults are in
[`minio.properties`](../src/main/resources/minio.properties),
and detailed constraints are in the
[driver README](../README.md#full-cli-flag-reference).

| Category | Options |
|---|---|
| Connection/auth | `-url`, `-bucket`, `-key`, `-secret`, `-region`, `-recreate`, `-insecure`, `-auth-version`, `-extra-headers` |
| Operation selection | `-write-operation`, `-read-operation`, `-write-mix`, `-read-mix`, `-mixed-read-source` |
| Async/concurrency | `-async`, `-async-depth`, `-async-max-inflight`, `-async-max-memory-mb` |
| Object layout/catalog | `-prefix`, `-copy-prefix`, `-fs-access`, `-key-distribution`, `-object-file`, `-catalog-max-objects`, `-partition-count`, `-partition-index`, `-partition-by-prefix`, `-run-manifest` |
| Object sizes | `-object-size-distribution`; common SBK `-size` supplies the fixed/default size |
| Range/LIST | `-range-offset`, `-range-length`, `-range-offset-distribution`, `-range-window-length`, `-range-alignment`, `-list-max-keys`, `-list-max-entries`, `-list-prefixes`, `-list-start-after`, `-list-delimiter`, `-list-api-version`, `-list-fetch-owner`, `-list-include-user-metadata` |
| Multipart | `-part-size`, `-mpu-concurrent-parts` |
| Integrity/security | `-checksum`, `-verify-read-size`, `-sse-enabled`, `-versioning-enabled` |
| Tagging | `-tagging-enabled`, `-tagging-tags` |
| Bucket workloads | `-bucket-targets`, `-bucket-prefix`, `-cleanup-created-buckets` |
| Data shape | `-data-compressibility`, `-data-dedupable`, `-data-seed` |
| Retry/warm-up/visibility | `-retry-max-attempts`, `-retry-backoff-ms`, `-retry-strategy`, `-retry-max-backoff-ms`, `-retry-jitter`, `-warmup-requests`, `-warmup-operation`, `-endpoint-metrics`, `-endpoint-preflight` |
| HTTP transport | `-connect-timeout-ms`, `-read-timeout-ms`, `-write-timeout-ms`, `-http-max-requests`, `-http-max-requests-per-host`, `-http-max-idle-connections`, `-http-keepalive-seconds` |

Relevant common SBK controls are:

| Option | Use in ECS/OBS work |
|---|---|
| `-writers`, `-readers` | Synchronous worker count or async submitter count |
| `-size` | Fixed object/range size and fallback for several options |
| `-records` | Fixed total when `-seconds` is absent; records/sec target when `-seconds` is present |
| `-seconds` | Timed steady-state run; omit for exact fixed-count qualification |
| `-throughput` | MB/s target; `-1` means maximum, `0` selects record-rate control |
| `-time` | Use `ms` for ordinary S3 latency; `mcs` only for very fast local metadata paths |
| `-minlatency`, `-maxlatency` | Exact recorder range; samples outside it are reported as discarded |
| `-thread` | `v` default, `p` platform, `f` fork-join; hold constant across comparisons |
| `-wstep`, `-wsec`, `-rstep`, `-rsec` | Worker ramp controls; use staged runs for concurrency-knee discovery |
| `-sync` | Async burst-and-drain interval; normally leave zero for continuous async load |
| `-wq`, `-rq` | Request-counter reporting |
| `-ro` | Mixed writer/reader read-only measurement mode; prefer isolated PUT and GET for service latency |
| `-millisecsleep` | Idle poll sleep; not an S3 pacing control |
| `-idletimeoutseconds` | Fixed-record no-progress failure deadline |
| `-out` | `SystemLogger`, `CSVLogger`, `PrometheusLogger`, `WebLogger`, or `GrpcLogger` as appropriate |
| `-help` | Generate the effective common + MinIO contract |

Boolean values are strict (`true` or `false`), and duplicate operation-mix,
tag, or header keys are rejected. A nonempty `-write-mix`/`-read-mix` is the
effective operation contract; the corresponding single-operation option is
only the fallback. For fixed-record destructive or mixed workloads, SBK
validates aggregate target capacity before timing. That validation cannot
guarantee publication timing or per-reader delivery.

For mixed writer/reader tests, prefer the default
`-mixed-read-source catalog`. The explicit `published` mode consumes only
objects completed by the same run and can wait indefinitely if writers finish
without supplying every reader operation. Use a finite common SBK guard such
as `-idletimeoutseconds 60`, keep publication and consumption rates balanced,
and reject any run terminated by that no-progress timeout. There is no separate
MinIO idle-timeout option because that would duplicate the harness control.

Driver operations are:

- writers: `put`, `update`, `copy`, `delete`, `tag-set`, `tag-delete`,
  `bucket-create`, `bucket-delete`;
- readers: `get`, `range-get`, `stat`, `tag-get`, `list`, `bucket-stat`,
  `bucket-list`;
- aliases: `create`→`put`, `overwrite`→`update`, `head`→`stat`, and
  `range-read`→`range-get`.

Generate logger-specific help before adding output options:

```bash
./build/install/sbk/bin/sbk -class minio -out CSVLogger -help
./build/install/sbk/bin/sbk -class minio -out PrometheusLogger -help
./build/install/sbk/bin/sbk -class minio -out WebLogger -help
```

## 9. Result acceptance and interpretation

A valid fixed-count result has:

- exit code 0 and successful shutdown;
- exact requested Total records;
- endpoint completed operations equal to expected logical operations;
- retries/failures equal to the test's declared allowance;
- zero invalid and unexplained discarded latencies;
- expected logical bytes;
- independently confirmed target state when mutation matters.

A valid timed result additionally has:

- stable middle reporting windows;
- no forced-cleanup/incomplete-result warning;
- a duration long enough to amortize JVM, connection, and ECS cache effects;
- retained load-host and ECS telemetry proving which resource saturated.

Interpret metrics by operation:

- PUT/GET/Range GET: operations/sec, payload MB/sec, and percentiles.
- Stat/tags/delete/bucket operations: operations/sec and latency; bytes are
  normally zero.
- Copy: operations/sec and logical bytes/sec; data stays inside ECS.
- LIST: operations/sec and latency; data bytes are zero because the response
  contains metadata rather than object payload.
- Mixed workloads: aggregate metrics only. Split operations for
  per-operation percentiles.

Retries are part of operation latency. A clean-looking latency curve with
nonzero retries can conceal ECS throttling, so always retain endpoint totals.

## 10. Confirmed lab example

These results were measured on 2026-08-31 from this SBK checkout against the
provided Dell ECS endpoint `10.236.66.181:9020`, namespace `sbk-ns`, using
SBK 10.7 and JDK 25.0.2. The S3 service identified as Dell ECS/ViPR. The path
had roughly 270 ms request latency. The bucket was
`sbk-doc-runbook-20260831`.

These are **functional examples, not a product performance claim**. They are
short, single-load-generator runs over a high-latency route. Do not compare
your cluster to these numbers.

| Workload | Shape | Completed | Rate | Avg | p50 | p95 | p99 | Endpoint retries/failures |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| PUT | 1 MiB, 2 writers, timed 20 s | 69 / 69 MiB | 3.4 ops/s, 3.45 MB/s | 562.2 ms | 542 ms | 590 ms | 2041 ms | 0 / 0 |
| GET | 1 MiB, 2 readers, fixed 100 | 100 / 100 MiB | 3.1 ops/s, 3.11 MB/s | 631.3 ms | 557 ms | 1747 ms | 3191 ms | 0 / 0 |
| Range GET | 4 KiB at offset 8192, 2 readers, fixed 100 | 100 / 0.4 MiB | 6.7 ops/s | 290.7 ms | 289 ms | 292 ms | 579 ms | 0 / 0 |
| LIST | prefix scan, max 1000, 1 reader, fixed 20 | 20 LISTs | 3.5 ops/s | 284.6 ms | 269 ms | 577 ms | 577 ms | 0 / 0 |
| Multipart PUT | 15 MiB, 5 MiB parts, 3 concurrent parts, fixed 4 | 4 / 60 MiB | 0.2 ops/s, 2.53 MB/s | 5914.3 ms | 5198 ms | 11531 ms | 11531 ms | 0 / 0 |

The LIST rate and latency remain representative of that dated run. Current
versions intentionally report zero LIST data bytes; older results that showed
logical-object MB/s must not be interpreted as network throughput.

One exploratory timed async multipart run reached the five-second cleanup
deadline and exited 1. It is intentionally excluded from the valid table. That
is the expected runbook treatment of incomplete results.

## 11. Troubleshooting

| Symptom | Likely cause | Action |
|---|---|---|
| HTTP 405, HTML, or management JSON | Management endpoint used as S3 | Use ECS `:9020` or `:9021` |
| 403 AccessDenied | Wrong Object User, permission, bucket owner, or namespace | Verify Object User and add `x-emc-namespace` |
| TLS validation failure | ECS CA missing | Install the CA; lab-only fallback is `-insecure true` |
| Bucket missing with readers only | Read workload cannot create a bucket | Populate it first or select the correct bucket |
| Existing-object operation says catalog empty | Prefix/partition/manifest does not match data | Correct `-prefix`, partitioning, or `-object-file` |
| Range GET has no eligible object | Object too small for offset | Populate larger objects or reduce offset |
| Async memory guard fails | Depth × object size exceeds budget | Reduce depth/inflight/object size or deliberately raise budget |
| Throughput plateaus and p99 rises | Client, network, or ECS saturation | Inspect CPU/NIC/ECS telemetry; do not blindly add concurrency |
| Retries increase | HTTP 429/5xx or network I/O | Treat as saturation/failure evidence; compare retry-disabled and production-policy runs |
| Cleanup timeout / exit 1 | Outstanding SDK/driver/recorder work exceeded five seconds | Invalid run; lower outstanding work or use fixed-record qualification |
| LIST reports 0 MB/s | LIST is metadata, not object-payload transfer | Use LIST ops/s and latency |
| Timed endpoint operations differ slightly from Total | Completion occurred at reporting boundary | Use fixed records for exact parity; explain timed boundary differences |

## 12. Cleanup and evidence retention

Before cleanup, retain:

- sanitized command or YML;
- complete raw output and exit code;
- credential-free run manifest;
- SBK/JDK/backend versions and topology;
- load-generator CPU/heap/network telemetry;
- ECS health, capacity, node/network/disk telemetry;
- exact bucket/prefix and cleanup owner;
- all repetitions, including failed or slow runs.

Do not put a recursive bucket-deletion command in an unattended agent
workflow. The engineer who owns the test namespace must confirm the exact
bucket and retention requirement, then use approved ECS tooling. The lab
validation bucket `sbk-doc-runbook-20260831` was intentionally left in place
for review; it contains roughly 0.3 GiB created by the commands above.

## 13. Vendor references

- [Dell ECS Data Access Guide](https://www.delltechnologies.com/asset/en-us/products/storage/technical-support/docu95766.pdf):
  S3 endpoint and HTTP/HTTPS port behavior.
- [Dell ECS bucket and namespace addressing](https://www.dell.com/support/manuals/en-us/ecs-appliance-software-with-encryption/ecs_p_adminguide_3_5_0_1/bucket-and-namespace-addressing?guid=guid-f0db8d27-e4e1-48c5-a7e9-ef69d40fc604&lang=en-us):
  namespace header and addressing.
- [Dell ECS bucket management guidance](https://www.dell.com/support/kbdoc/en-us/000055896/ecs-bucket-management-overview-and-troubleshooting):
  namespace/bucket operational recommendations.
- [AWS S3 performance guidelines](https://docs.aws.amazon.com/AmazonS3/latest/userguide/optimizing-performance-guidelines.html):
  parallel requests, connection reuse, retries, and aligned Range GETs.
- [MinIO network and storage performance testing](https://docs.min.io/aistor/operations/network-performance-testing/):
  layered client/network/storage benchmarking methodology.

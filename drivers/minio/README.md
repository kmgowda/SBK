<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# S3 Performance Benchmarking with SBK (MinIO Driver)

> **For engineers new to S3 performance work.** This guide walks you from
> zero ("what is S3 benchmarking and why do we do it?") through hands-on
> runs against three real backends (MinIO, AWS S3, Dell ObjectScale / ECS)
> and ends with troubleshooting for the issues you will hit in practice.

Focused guides:

- [Dell ECS/ObjectScale benchmark runbook](docs/ECS_OBS_BENCHMARK_RUNBOOK.md):
  qualification, workload matrices, exact commands, acceptance rules, and
  confirmed lab results.
- [Driver implementation](docs/IMPLEMENTATION.md): request lifecycle, measurement
  boundaries, async/memory bounds, catalog, multipart, retry, and shutdown.
- [Software-agent ECS/OBS workflow](docs/ECS_OBS_AGENT_RUNBOOK.md):
  deterministic and safe execution for Devin, Windsurf, Codex, Cursor, and
  other agents.

---

## What is S3 performance benchmarking?

**S3** (Simple Storage Service) is an object-storage protocol originally
created by AWS. Today, dozens of products implement the same wire protocol —
**MinIO**, **Dell ECS / ObjectScale**, **Ceph RGW**, **SeaweedFS**, **Wasabi**,
**Backblaze B2**, etc. — so the same client can talk to any of them by just
changing the endpoint URL and credentials.

**Performance benchmarking** of an S3 store is the process of measuring two
things while a controlled, synthetic workload is running:

1. **Throughput** — how many operations per second, and how many MB per
   second, the storage system can sustain. Typical units: `records/sec`,
   `MB/sec`.
2. **Latency** — how long each individual operation took, from the client's
   point of view. Measured in milliseconds (ms). We report not just the
   average, but **percentiles**: p50 (median), p95, p99, p99.9 — because
   averages hide the tail behaviour that matters most in production.

You run two basic workloads:

- **Write benchmark** — the client issues many `PutObject` requests with
  generated payloads, measuring how fast the cluster can ingest new data.
- **Read benchmark** — the client issues many `GetObject` requests against
  objects that already exist in the bucket, measuring how fast the cluster
  can serve reads.

### Why does this matter?

A storage system's brochure number ("100 GB/s throughput!") almost never
matches what your application will actually see. Real performance depends on:

- **Object size** — a system can serve a million 1 KiB objects/sec but only
  a thousand 1 MiB objects/sec. Same hardware, different bottleneck.
- **Concurrency** — one client thread will hit a request-rate ceiling far
  below what the cluster can handle. You need enough parallel clients to
  saturate the link.
- **Workload shape** — random data vs. compressible data, dedup-friendly vs.
  unique payloads, fresh keys vs. overwrites — these can change measured
  throughput by 10× on the same hardware.
- **Network** — TLS handshake cost, RTT, packet loss.
- **The protocol path** — `PUT` vs multipart-upload, signature version,
  optional checksum headers.

SBK gives you knobs for all of these so you can produce numbers that
predict real-world behaviour rather than peak marketing numbers.

### Key concepts in 30 seconds

| Term | What it means |
|---|---|
| **Bucket** | An S3 "container" for objects, identified by name. Buckets are the unit of permissioning and configuration. |
| **Object** | A blob of bytes stored under a key in a bucket. Has metadata: content-type, ETag, optional tags, optional version. |
| **Object key** | The path-like name of an object inside a bucket, e.g. `users/42/profile.jpg`. Not a filesystem path — just a string. |
| **Access key / secret key** | S3 credentials. The access key is public (an identifier), the secret key signs each request using SigV4. **Not** a username/password. |
| **SigV4** | The cryptographic signature scheme S3 uses to authenticate every request. The MinIO Java SDK does this automatically. |
| **Endpoint** | The URL of the S3 service. **AWS S3** lives at `https://s3.<region>.amazonaws.com`; other vendors use their own URLs and ports. |
| **Multipart upload** | A way to split a large object into 5 MiB – 5 GiB parts that upload in parallel. Required for objects above 5 GiB; useful above ~16 MiB. |
| **Latency percentile** | "p95 = 100 ms" means 95 % of requests finished in ≤ 100 ms, 5 % took longer. p99.9 / p99.99 expose the rare-but-painful tail. |

---

## Default S3 ports (cheat-sheet)

A common mistake is pointing your client at the cluster's *management* UI
instead of the S3 *data plane*. Always confirm the port:

| Backend | Default S3 port (HTTP) | Default S3 port (HTTPS) | Notes |
|---|---|---|---|
| **AWS S3** | — | **443** | Endpoint is regional, e.g. `https://s3.us-east-1.amazonaws.com` |
| **MinIO server** | **9000** | **9000** (TLS optional) | The MinIO Console UI is usually on `:9001` |
| **Dell ECS / ObjectScale** | **9020** | **9021** | Management UI is on `:443` (returns HTML/JSON, not XML — *do not point S3 clients here*). The mgmt REST API is on `:4443`. |
| **Ceph RGW (Rados Gateway)** | **7480** | (varies) | Often fronted by a reverse proxy on `:443` or `:80` with TLS |
| **SeaweedFS S3 gateway** | **8333** | (varies) | |
| **Wasabi** | — | **443** | Regional endpoints, e.g. `https://s3.us-east-1.wasabisys.com` |
| **Backblaze B2 (S3 API)** | — | **443** | `https://s3.<region>.backblazeb2.com` |
| **Localstack (dev)** | **4566** | **4566** | All-in-one AWS emulator |

**How to recognise the right port**: a real S3 endpoint returns an **XML** body
when you `curl` it (either a bucket listing, or an XML `<Error>`). If you see
HTML or JSON, the port is wrong — see
[Troubleshooting](#troubleshooting).

---

## What this driver does

The `minio` driver in SBK speaks the **AWS S3 protocol** and works against
any S3-compatible storage system listed above. It supports:

- **Concurrent writes and reads** — any number of writer / reader threads,
  one process or distributed across many (via SBK-GEM).
- **Synchronous or bounded asynchronous clients** — use the MinIO SDK's
  `MinioClient` for one operation per worker, or `MinioAsyncClient` for a
  controlled number of in-flight operations per worker.
- **Object operations** — PUT/create, full-object update, server-side copy,
  delete, GET, ranged GET, stat/HEAD, tag set/get/delete, and list.
- **Bucket operations** — create, delete, existence/stat, and list.
- **Multipart upload** for large objects.
- **S3 checksum validation** — CRC32 / CRC32C / SHA1 / SHA256 / CRC64-NVMe.
- **Object tagging**, **bucket versioning**, **SSE-S3 encryption**.
- **Object key layout knobs** — flat keys, hierarchical "filesystem-style"
  keys, custom prefixes.
- **Data shaping** — control payload compressibility and defeat inline
  deduplication.
- **Vendor-specific HTTP headers** — required by some S3 backends
  (`x-emc-namespace` for Dell ECS / ObjectScale).
- **Tunable HTTP timeouts**.

What it does **not** do:

- Byte-for-byte content validation. `-verify-read-size` validates the number
  of returned bytes, while `-checksum` validates PUT transport integrity on
  backends that implement the requested checksum. Neither option compares a
  later GET body with the original generated payload.
- SigV2 (legacy S3 signing). Only SigV4 is implemented.
- Username / password login. S3 protocol has no such concept — only access
  key + secret key. Use an authorized management identity in the ECS UI to
  create or inspect S3 keys for an Object User; never pass management
  credentials to SBK.

---

## Table of contents

- [Quick start](#quick-start)
- [Prerequisites and safety](#prerequisites-and-safety)
- [Full CLI flag reference](#full-cli-flag-reference)
- [What each option proves](#what-each-option-proves)
- [Benchmark scenarios](#benchmark-scenarios)
  - [1. play.min.io (public sandbox)](#1-playminio-public-sandbox)
  - [2. Local MinIO server in Docker](#2-local-minio-server-in-docker)
  - [3. AWS S3](#3-aws-s3)
  - [4. Dell ObjectScale / Dell ECS](#4-dell-objectscale--dell-ecs)
  - [5. Ceph RGW](#5-ceph-rgw)
- [Example performance results (Dell ObjectScale)](#example-performance-results-dell-objectscale)
- [Advanced features](#advanced-features)
- [Tuning for higher throughput](#tuning-for-higher-throughput)
- [Troubleshooting](#troubleshooting)
- [Known constraints](#known-constraints)

---

## Quick start

Build the project (skip `halodb` if you don't have GitHub Packages credentials
configured):

```bash
./gradlew installDist
```

The launchable script will be at `./build/install/sbk/bin/sbk`.

Smoke-test against MinIO's public sandbox `play.min.io` (no credentials
needed). The public sandbox accepts HTTPS only, so opt in explicitly:

```bash
./build/install/sbk/bin/sbk -class minio -url https://play.min.io \
  -writers 1 -size 100 -seconds 30
```

What you'll see in the log (the columns are explained in
[Reading the output](#reading-the-output) below):

```
2026-06-06 ...  Bucket 'sbk' already exists
2026-06-06 ...  Writer 0 started , run seconds: 30
2026-06-06 ...  Total Minio Writing  1 writers, ... 95 records, 3.1 records/sec, ...
                    283 ms 5th, ... 285 ms 50th, ... 304 ms 95th, ... 320 ms 99th, ...
```

### Reading the output

The "Total Minio Writing/Reading" line at the end of every run summarises
the whole benchmark. The key numbers to look at:

| Column | Meaning |
|---|---|
| `N writers, M readers` | How many concurrent client threads were active |
| `<X> seconds` | Actual run duration |
| `<X> records` | Total operations completed (PUTs or GETs) |
| `<X> records/sec` | **Throughput in operations per second** |
| `<X> MB/sec` | **Throughput in bytes per second** (records × object size / seconds) |
| `<X> ms avg latency` | Average per-operation latency |
| `<X> ms 50th` | **Median (p50)** — half of operations were this fast or faster |
| `<X> ms 95th, 99th, 99.9th, 99.99th` | **Tail percentiles** — these expose the worst-case behaviour. p99.9 = 1-in-1000 slowest. |

The periodic in-run lines (every 5 s by default) give you a window-by-window
view so you can spot warm-up effects, GC pauses, etc.

---

## Prerequisites and safety

Before using any option, confirm the following:

1. **JDK 25 and a current SBK distribution** are available. Build with
   `./gradlew installDist`, then use `./build/install/sbk/bin/sbk`.
2. **The endpoint is the S3 data-plane endpoint**, not a management UI.
   Include `http://` or `https://` explicitly when transport security matters.
3. **The access key and secret key belong to an S3 user** with permissions for
   every operation being tested. Read-only credentials cannot run PUT, DELETE,
   tagging, versioning, or bucket lifecycle tests.
   Prefer injecting them as `SBK_S3_ACCESS_KEY` and `SBK_S3_SECRET_KEY`; this
   keeps credentials out of the operating-system process argument list.
4. **Use a dedicated benchmark bucket and prefix.** PUT creates data; UPDATE,
   COPY, tagging, GET, range GET, stat, and DELETE need existing objects;
   `-recreate true`, DELETE, and bucket deletion are destructive.
5. **Provide backend-specific configuration.** For example, Dell ECS /
   ObjectScale commonly needs `-extra-headers
   'x-emc-namespace=<namespace>'`.
6. **Provision client resources for the requested concurrency.** Async PUT
   retains a payload per in-flight operation. Multipart tests need enough heap,
   network bandwidth, and backend capacity for the selected object and part
   sizes.
7. **Run a correctness pass before a performance pass.** Start with one worker,
   a short, rate-controlled run and `-verify-read-size true`; then increase
   concurrency and duration.

### What a successful SBK run confirms

A completed record means the corresponding MinIO SDK future or synchronous SDK
call completed successfully and SBK recorded its latency. A successful run
therefore confirms client-to-endpoint connectivity, authentication,
authorization, request compatibility, and completion of the selected API
operation under the configured workload.

It does **not** automatically prove every server-side property. For example,
`-sse-enabled true` proves that the backend accepted SSE-S3 PUT requests, but
an administrator should inspect backend metadata or policy when an independent
at-rest-encryption audit is required. `-verify-read-size true` checks returned
byte counts, not byte-for-byte content. Checksum-enabled PUT verifies transport
integrity only when the target backend implements the requested S3 checksum.

For a trustworthy performance result, require all of the following:

- the process exits with code zero and prints `SBK Benchmark Shutdown`;
- the final result has nonzero records;
- `invalid latencies`, discarded latencies, and timeout events are zero;
- there are no `ERROR`, SDK exception, HTTP failure, or I/O failure messages;
- a follow-up read/stat/list or backend inspection confirms the expected state
  when the test changes objects or buckets.

---

## Full CLI flag reference

Every option also has a default in
[`src/main/resources/minio.properties`](src/main/resources/minio.properties).
Command-line flags override the properties file.

The tables below distinguish **what an option changes** from **what a passing
run confirms**. Options do not create independent assertions unless the
description explicitly says they validate something.

### Connection & credentials

| Flag | Default | Purpose |
|---|---|---|
| `-url <url[,url...]>` | `http://play.min.io` | One S3 endpoint or a comma-separated endpoint pool. Values without schemes use plain HTTP. With multiple endpoints, workers are assigned round-robin while setup and catalog discovery use the first endpoint. |
| `-bucket <name>` | `sbk` | Bucket to read / write |
| `-key <access-key>` | `SBK_S3_ACCESS_KEY`, then properties default | S3 access key. An explicit option takes precedence over the environment. |
| `-secret <secret-key>` | `SBK_S3_SECRET_KEY`, then properties default | S3 secret key. An explicit option takes precedence over the environment. The value is never echoed by `-help`. |
| `-region <region>` | empty; effective `us-east-1` | AWS region for SigV4 signing. An empty value uses `us-east-1`, so the SDK skips `GetBucketLocation`, which many non-AWS backends mishandle. Set the actual bucket region for AWS or any backend that requires another region. |
| `-recreate true|false` | `false` | Destructively empty and recreate the bucket before a writer run. Mixed writer/reader runs do not override this safety setting. |
| `-insecure true|false` | `false` | Skip certificate validation for explicit HTTPS endpoints. This does not select HTTP; transport is controlled by each endpoint URL scheme. |
| `-auth-version 4` | `4` | S3 signature version. The MinIO SDK implements SigV4; any other value is rejected during argument validation. |

SBK masks `-key`, `-secret`, and other common credential options in its own
argument logs. Explicit CLI values can still be visible to operating-system
process inspection, so environment injection is preferred for ordinary
single-host MinIO runs.

### Object naming

| Flag | Default | Purpose |
|---|---|---|
| `-fs-access true|false` | `false` | Spread keys across a 2-level hex directory tree (`aa/bb/sbk-<run>-<writer>-<sequence>`), mimicking how applications like Apache Hadoop S3A create paths. Helps test ListObjects / prefix-scan behavior. |
| `-prefix <p>` | `""` | Prepend `<p>/` to every generated object key |
| `-copy-prefix <p>` | `sbk-copy` | Destination-key prefix for server-side COPY |
| `-key-distribution sequential|hashed|random` | `sequential` | Select sequential suffixes, uniformly hashed two-level prefixes, or seeded random suffixes for generated object keys. |
| `-object-size-distribution <spec>` | `fixed` | Select `fixed`, seeded random `uniform:min:max`, deterministic sequential `sweep:min:max`, or `weighted:size=weight,...` object sizes. Both range bounds are inclusive. The largest configured size participates in the startup memory guard. |

### S3 operation selection

Writers execute the operation selected by `-write-operation`; readers execute
the operation selected by `-read-operation`. Each timed record is one logical
operation. Every S3 request is constructed and executed by MinIO SDK builders
and clients—the driver does not contain a separate S3 protocol implementation.

Use `-write-mix "put=80,copy=20"` or
`-read-mix "get=90,stat=10"` for deterministic weighted workloads. Weights
need not add to 100. Each worker follows an exact repeating weighted cycle,
with a worker-specific starting position; writer mixes may contain only
mutating operations and reader mixes only read operations. A mix is
authoritative when present; `-write-operation` or `-read-operation` is only its
fallback. Repeating the same operation in one mix is rejected rather than
creating two ambiguous weighted intervals.

`-fs-access` and `-key-distribution` are related but not duplicate controls.
`-fs-access true` adds the hierarchical two-level layout to sequential or
random suffixes. `-key-distribution hashed` selects that layout together with
deterministic sequential identities as a single workload shape.

| Flag/value | SDK operation and measured work | Prerequisite | What a successful record confirms |
|---|---|---|---|
| `-write-operation put` | `putObject` uploads one newly generated key and payload | Write permission; bucket exists or credentials can create it | The backend accepted and completed an object PUT of `-size` bytes |
| `-write-operation update` | `putObject` replaces a catalog-selected key with a new payload | Existing objects and overwrite permission | Full-object overwrite completed for an existing key |
| `-write-operation copy` | `copyObject` creates a destination under `-copy-prefix`; bytes stay server-side | Existing source objects and copy/read/write permission | The backend completed a server-side object copy |
| `-write-operation delete` | `removeObject`; each catalog entry is claimed once | Existing objects and delete permission | The backend accepted deletion of a discovered key; use LIST to independently confirm absence |
| `-write-operation tag-set` | `setObjectTags` replaces tags on a catalog-selected object | Existing objects, tag permission, nonempty `-tagging-tags` | A standalone object-tag update completed |
| `-write-operation tag-delete` | `deleteObjectTags` removes tags from a catalog-selected object | Existing tagged objects and tag permission | A standalone tag-delete request completed |
| `-write-operation bucket-create` | `makeBucket` creates a unique generated name | Account-level create-bucket permission | Bucket creation completed; cleanup behavior is controlled separately |
| `-write-operation bucket-delete` | `removeBucket` deletes each explicit target once | Explicit `-bucket-targets`; targets must be empty; delete-bucket permission | The named empty bucket was removed |
| `-read-operation get` | `getObject`; SBK drains the complete response body | Existing objects and read permission | The full object response was consumed; byte count is checked only with `-verify-read-size true` |
| `-read-operation range-get` | `getObject` with byte offset and length; SBK drains the response | An object larger than `-range-offset`; ranged-read support | The requested byte range was returned; byte count is checked only with `-verify-read-size true` |
| `-read-operation stat` | `statObject` (S3 HEAD) | Existing objects and metadata/read permission | Object metadata was resolved without downloading the body |
| `-read-operation tag-get` | `getObjectTags` | Existing objects and tag-read permission | The backend returned the object's tag set |
| `-read-operation list` | `listObjects`, consuming at most `-list-max-keys` entries | List-bucket permission | A prefix listing completed and its returned entries were consumed |
| `-read-operation bucket-stat` | `bucketExists` | Bucket visibility permission; optional explicit targets | Bucket-existence API completed and returned `true`; a missing bucket fails the record |
| `-read-operation bucket-list` | `listBuckets` | Account-level list-buckets permission | The account's bucket-list API completed |

`create`, `overwrite`, `head`, and `range-read` are accepted aliases for
`put`, `update`, `stat`, and `range-get`.

The object catalog is loaded once when an operation needs existing objects.
Pure PUT, LIST, and bucket workloads avoid that startup scan. This prevents an
unmeasured LIST or HEAD request from being added before every timed GET without
making write-only startup proportional to the bucket size. In a combined
PUT/GET run, completed PUTs are published to readers through a blocking queue.
For fixed-record workloads, startup also proves that one-shot DELETE and
bucket-delete targets, and mixed-run published objects, are sufficient for the
requested record count. An impossible finite workload fails before timing
instead of waiting indefinitely after its target set is exhausted.

### Bounded asynchronous mode

| Flag | Default | Purpose |
|---|---|---|
| `-async true|false` | `false` | Select `MinioAsyncClient` instead of synchronous operations |
| `-async-depth <1..1024>` | `32` | Maximum in-flight SDK futures **per SBK worker** |
| `-async-max-inflight <n>` | `0` (auto) | Process-wide in-flight ceiling shared by all workers. Auto derives `workers × async-depth`. |
| `-async-max-memory-mb <MiB>` | `1024` | Reject a configuration whose conservative async-buffer estimate exceeds this budget; zero disables the guard. |

Async mode applies worker-local and process-wide backpressure before latency
timing begins. Thus the
reported latency measures the remote S3 operation rather than local waiting
for a concurrency slot. Memory remains bounded by `-async-max-inflight`, and
startup rejects an obviously unsafe buffer budget before connecting. PUT
retains one pooled object payload per in-flight request; buffers are reused
only after their SDK futures complete. GET retains a reusable 64 KiB drain
buffer per in-flight request. Multipart streams use bounded views of the same
payload rather than allocating one part buffer per request. Start with a small depth such as 4
or 8 and increase it until throughput stops improving or tail latency becomes
unacceptable.

The shared OkHttp dispatcher is also configurable:

| Flag | Default | Purpose |
|---|---|---|
| `-http-max-requests <n>` | `0` (auto) | Maximum total queued/running OkHttp async calls |
| `-http-max-requests-per-host <n>` | `0` (auto) | Per-S3-endpoint maximum |
| `-http-max-idle-connections <n>` | `32` | Connections retained in the pool |
| `-http-keepalive-seconds <s>` | `300` | Idle connection retention |

An automatic request limit is derived from worker count and async depth when
either request-limit option is zero.

### Range, list, and bucket controls

| Flag | Default | Purpose |
|---|---|---|
| `-range-offset <bytes>` | `0` | First byte of a ranged GET |
| `-range-length <bytes>` | `0` | Bytes per ranged GET; zero uses SBK `-size` |
| `-list-max-keys <1..1000>` | `1000` | Maximum entries consumed by each timed LIST |
| `-list-prefixes <csv>` | empty | Prefixes assigned round-robin across LIST readers, preventing every reader from scanning the same keyspace. |
| `-object-file <path>` | empty | Load the startup catalog from strict local `key,size[,versionId]` CSV instead of listing S3. Size must be a nonnegative integer; keys cannot contain commas; blank lines and `#` comments are allowed. |
| `-catalog-max-objects <n>` | `1000000` | Bound discovered or manifest object references retained in memory. |
| `-partition-count <n>` | `1` | Split existing-object catalogs by stable key hash across distributed SBK/SBK-GEM processes. |
| `-partition-index <0..n-1>` | `0` | Partition owned by this process. Generated keys include the partition when count is greater than one. |
| `-partition-by-prefix true|false` | `false` | Put every generated partition under `partition-<index>/` and use that prefix in server-side LIST filtering. This avoids downloading the full catalog on every distributed client. |
| `-run-manifest <path>` | empty | Write a credential-free JSON record of the effective workload, data shape, integrity, retry, warm-up, HTTP, async, and partition settings. Endpoint URLs, access/secret keys, and header values are excluded. |
| `-bucket-targets <csv>` | empty | Explicit buckets for delete/stat; required for bucket delete |
| `-bucket-prefix <p>` | `sbk-benchmark` | Prefix for unique bucket-create names |
| `-cleanup-created-buckets true|false` | `true` | Remove successfully generated empty buckets at shutdown |

### SBK core options accepted by the MinIO driver

These options come from the common SBK harness, but they materially change an
S3 test and are therefore part of the MinIO command-line contract.

| Flag | Default | Effect and what it tests or confirms |
|---|---:|---|
| `-class minio` | required | Selects this driver. Its startup banner confirms that `io.sbk.driver.MinIO.MinIO` was discovered. |
| `-writers <n>` | none | Starts `n` concurrent mutating workers. A scaling series confirms how PUT or other writer throughput and tail latency change with client concurrency. |
| `-readers <n>` | none | Starts `n` concurrent read-only workers. Existing-object operations require a populated catalog. |
| `-size <bytes>` | required | Sets generated PUT/UPDATE payload size and the default range length. Metadata, tag, list, and bucket operations transfer zero payload bytes even though SBK still requires a positive size. |
| `-seconds <s>` | unlimited | Runs for a duration. When supplied with `-records`, records become the per-second workload rate rather than a fixed total. |
| `-records <n>` | `0` | Without `-seconds`, requests a fixed operation count. With `-seconds`, sets the target records per second used by the workers. |
| `-throughput <MB/s>` | `-1` | `> 0` limits aggregate data throughput in MB/s; `0` uses the `-records` rate; `-1` requests maximum throughput. This tests a controlled load level rather than saturation only. |
| `-thread p|f|v` | `p` | Selects platform, fork-join, or virtual worker threads. Compare runs to isolate client scheduling overhead; it does not change the MinIO SDK API. |
| `-sync <n>` | `0` | Drains pending MinIO async futures after each group of `n` submissions. In synchronous mode it adds no S3 durability guarantee. Use it to test burst-and-drain behavior; leave it `0` for a continuously full async pipeline. |
| `-ro true|false` | `false` | Common mixed-workload “benchmark reads only” mode. It is not an S3 permission control. For a clean MinIO read-only result, a separate `-readers` run over a pre-populated bucket is preferred. |
| `-wstep <n>` | `1` | Adds writers in increments of `n`; use with `-wsec` to generate a stepped write-concurrency curve in one run. |
| `-wsec <s>` | `0` | Holds each writer step for `s` seconds. A passing series shows the throughput/latency knee as write concurrency rises. |
| `-rstep <n>` | `1` | Adds readers in increments of `n`; use with `-rsec` for a stepped read-concurrency curve. |
| `-rsec <s>` | `0` | Holds each reader step for `s` seconds. |
| `-millisecsleep <ms>` | `0` | Adds an idle sleep to the common recorder path. This lowers client CPU usage but can perturb fine-grained latency, so leave it zero for peak-performance tests. |
| `-time ms|mcs|ns` | `ms` | Selects the display and histogram unit. Use `mcs` or `ns` only when the configured latency range has enough memory and the S3 operation is fast enough to benefit. |
| `-minlatency <value>` | `0` | Sets the lowest histogram latency in the unit selected by `-time`; samples below it are reported as discarded. |
| `-maxlatency <value>` | `180000 ms` | Sets the highest histogram latency in the selected unit; samples above it are reported as discarded. Increase it above HTTP timeouts for slow S3 or multipart tests. |
| `-wq true|false` | `false` | Includes write-request counters in logger output. This confirms request submission rate separately from completed-operation rate. |
| `-rq true|false` | `false` | Includes read-request counters in logger output. Useful for observing async in-flight/pending work. |
| `-out <logger>` | `SystemLogger` | Selects result output, for example `SystemLogger`, `CSVLogger`, `PrometheusLogger`, or `WebLogger`. It changes presentation, not S3 traffic. |
| `-csvfile <path>` | `no` | With a CSV-capable logger, stores periodic results for later comparison and regression analysis. |
| `-help` | off | Prints the effective common and MinIO driver options without starting a benchmark. |

At least one of `-writers` or `-readers` must be greater than zero. Prefer
`-seconds` for sustained performance measurements and fixed `-records` for
small correctness/lifecycle checks.

### Multipart upload (large objects)

| Flag | Default | Purpose |
|---|---|---|
| `-part-size <bytes>` | `0` (disabled) | Trigger multipart upload when object size ≥ part size. Valid range: **5 MiB ≤ partSize ≤ 5 GiB** (S3 spec). |
| `-mpu-concurrent-parts <0..1024>` | `0` | Values above one use the SDK's public low-level multipart API and upload bounded waves of parts per object. `0`/`1` retain normal SDK behavior. Requires `-part-size`; incompatible with whole-object `-checksum`. |

### S3 checksum validation

| Flag | Values | Purpose |
|---|---|---|
| `-checksum <algo>` | `crc32` / `crc32c` / `sha1` / `sha256` / `crc64nvme` / *empty* | When set, the driver computes the digest of every PUT payload and sends it as the `x-amz-checksum-<algo>` header. The server (if it supports it) verifies the body matched. |

### Object tagging

| Flag | Default | Purpose |
|---|---|---|
| `-tagging-enabled true|false` | `false` | Attach tags in the same native `PutObjectArgs` request. Use `-write-operation tag-set` to benchmark a separate `SetObjectTags` operation. |
| `-tagging-tags "k1=v1,k2=v2,..."` | `""` | Tag set, applied to every written object |

### Bucket versioning

| Flag | Default | Purpose |
|---|---|---|
| `-versioning-enabled true|false` | `false` | With writers, enable versioning on the bucket; with readers only, discover and address existing versions without mutating bucket configuration. |

### Data-shape controls

These shape the **payload bytes** the driver generates, not the protocol.
Useful when benchmarking storage with inline compression or deduplication.

| Flag | Default | Purpose |
|---|---|---|
| `-data-compressibility <0..100>` | `0` | Target compressibility percentage. Each 4 KiB chunk is split: `100-N`% random bytes (incompressible), `N`% zero bytes (highly compressible). `0` = fully random, `100` = all zeros. |
| `-data-dedupable true|false` | `true` | When `false`, stamps every 4 KiB chunk with a 16-byte `(objectId, chunkOffset)` anti-dedup marker. When `true`, no marker is added; random portions still vary, so identical payloads require `-data-compressibility 100`. |
| `-data-seed <long>` | `0` (random) | Non-zero seed makes generated payload streams reproducible for controlled comparisons. |
| `-verify-read-size true|false` | `false` | Fail a GET/range-GET whose consumed response bytes differ from catalog/range metadata. |

### Retry and connection warm-up

| Flag | Default | Purpose |
|---|---|---|
| `-retry-max-attempts <n>` | `1` | Total attempts for network I/O, HTTP 429, and HTTP 5xx failures. One disables retries. |
| `-retry-backoff-ms <ms>` | `0` | Fixed delay between retry attempts. |
| `-warmup-requests <n>` | `0` | Number of untimed requests distributed across configured endpoints before measurement. |
| `-warmup-operation connection|put|get|put-get` | `connection` | Warm only connection/authentication, or execute data-plane PUT, GET, or PUT+GET requests. Temporary warm-up objects are removed before measurement. |
| `-endpoint-metrics true|false` | `false` | Attribute completed operations, bytes, retries, and terminal failures to each configured endpoint. Disabled by default to keep the hot path minimal. |

When retries are enabled, all attempts and backoff remain one logical timed
SBK operation. This reports application-observed latency; use one attempt when
you need raw single-request service latency. A process-wide retry total is
always printed when attempts are enabled. `-endpoint-metrics true` additionally
attributes retries, completions, logical bytes, and terminal failures by URL.

### Server-side encryption

| Flag | Default | Purpose |
|---|---|---|
| `-sse-enabled true|false` | `false` | Request SSE-S3 (`AES256`) encryption with S3-managed keys for PUT and COPY. This is not SSE-KMS and does not use `aws:kms`. |

### Vendor-specific headers and HTTP tuning

| Flag | Default | Purpose |
|---|---|---|
| `-extra-headers "k1=v1,k2=v2"` | `""` | Inject arbitrary HTTP headers on every S3 request via an OkHttp interceptor. Primary use case: `x-emc-namespace=<ns>` for Dell ECS / ObjectScale. |
| `-connect-timeout-ms <ms>` | `0` (SDK default) | HTTP connect timeout |
| `-read-timeout-ms <ms>` | `0` (SDK default) | HTTP read timeout |
| `-write-timeout-ms <ms>` | `0` (SDK default) | HTTP write timeout |
| `-http-max-requests <n>` | `0` (auto) | Total OkHttp async request limit |
| `-http-max-requests-per-host <n>` | `0` (auto) | Per-host OkHttp async request limit |
| `-http-max-idle-connections <n>` | `32` | Idle connections retained |
| `-http-keepalive-seconds <s>` | `300` | Idle connection retention |

---

## What each option proves

This section is a test-planning checklist. “Confirms” means the configured run
completed through the MinIO SDK without an error; use the independent check in
the last column when the backend state itself must be proven.

### Endpoint, identity, and bucket options

| Option | Prerequisite | A successful run confirms | Independent check or limitation |
|---|---|---|---|
| `-url` | Reachable S3 data-plane URL | DNS/IP routing, selected HTTP/HTTPS transport, and S3 protocol compatibility | `curl` should return S3 XML, not a management page |
| `-url` with multiple URLs | Every CSV endpoint serves the same S3 namespace/buckets | Workers can operate through the round-robin endpoint pool | Compare per-node server metrics; SBK reports the aggregate |
| `-key` | Valid S3 access key | The supplied identity can authenticate | Does not prove permissions for operations not run |
| `-secret` | Secret paired with `-key` | SigV4 request signing succeeds | Keep it out of shell history and committed YAML |
| `-region` | Region accepted by the target | Requests are accepted with the selected SigV4 scope | For AWS, use the bucket's actual region |
| `-bucket` | S3-valid name and required permissions | Main-bucket setup and selected object workload complete | Writer runs may create a missing bucket |
| `-recreate` | Dedicated disposable bucket; delete/create permission | Existing contents/versions can be emptied and the bucket recreated before timing | **Destructive**; never use on shared data |
| `-insecure` | Explicit HTTPS endpoint with an untrusted certificate | TLS traffic works without certificate verification | Does not make HTTP secure and must not be used as a production security test |
| `-auth-version` | Value `4` | SigV4 authentication succeeds | Unsupported signature versions fail fast before connecting |
| `-extra-headers` | Backend documents the header and signing behavior | Requests with injected vendor/tenant headers are accepted | Confirm tenant routing in backend audit logs |

### Operation, catalog, and key-layout options

| Option | Prerequisite | A successful run confirms | Independent check or limitation |
|---|---|---|---|
| `-write-operation` | Permissions and objects required by the selected operation | The exact mutating API named in the operation table completes | Follow with LIST/GET/tag inspection for state verification |
| `-read-operation` | Permissions and objects required by the selected operation | The exact read-only API named in the operation table completes | GET byte count is asserted only with `-verify-read-size` |
| `-write-mix` | Only writer operations; existing objects if any component needs them | The deterministic weighted writer cycle completes | Compare per-operation backend metrics; SBK aggregates the mix |
| `-read-mix` | Only reader operations; existing objects where required | The deterministic weighted reader cycle completes | SBK reports aggregate mix latency |
| `-prefix` | Keys exist under the prefix for existing-object workloads | Generated PUT keys use the prefix or discovery is restricted to it | LIST the prefix to inspect keys |
| `-fs-access` | No special prerequisite | Hierarchical two-level generated keys are accepted | LIST to confirm distribution across leaf prefixes |
| `-copy-prefix` | Existing source objects and copy permission | COPY destinations under the configured prefix are accepted | LIST the destination prefix |
| `-key-distribution` | Sequential, hashed, or random | Generated keys use the requested namespace shape | Use the same seed for reproducible random-key comparisons |
| `-object-size-distribution` | Valid fixed, uniform, sweep, or weighted specification | The timed workload includes the requested object-size mix | SBK reports aggregate results; split runs when per-size percentiles are required |
| `-object-file` | Readable local `key,size[,versionId]` CSV matching the target bucket | Operations can run from a supplied catalog without startup LIST discovery | Stale/missing entries fail when used |
| `-catalog-max-objects` | Positive value; enough heap for retained references | Discovery or manifest loading remains bounded at the selected count | It caps client coverage; it does not cap bucket size |
| `-partition-count` | Same count on all distributed clients | Stable key-hash partitioning is enabled | Use one unique index for every process |
| `-partition-index` | Value from zero through `count - 1` | This process operates only on its assigned catalog/key partition | Duplicate indices duplicate work; missing indices leave gaps |
| `-partition-by-prefix` | Unique partition index on every process | Catalog discovery is server-filtered to this process's generated partition prefix | Existing objects must already follow the same partition-prefix convention |
| `-run-manifest` | Writable local parent directory | A credential-free JSON record of the effective run is written | The manifest is configuration evidence, not an object result manifest |

### Async, multipart, HTTP, retry, and warm-up options

| Option | Prerequisite | A successful run confirms | Independent check or limitation |
|---|---|---|---|
| `-async` | Backend and client resources support requested concurrency | Native `MinioAsyncClient` futures complete under bounded backpressure | Compare against sync mode using identical workload settings |
| `-async-depth` | `1..1024`; sufficient heap and sockets | Each worker sustains up to the configured in-flight depth | It is a ceiling, not proof that depth was continuously saturated |
| `-async-max-inflight` | Nonnegative process limit | All workers obey a shared in-flight ceiling | `0` derives workers × depth |
| `-async-max-memory-mb` | Nonnegative MiB budget | Startup's conservative async-buffer estimate fits the budget | It is a guard estimate, not a heap profiler |
| `-part-size` | `0`, or 5 MiB through 5 GiB; object large enough to split | SDK multipart PUT completes with the selected stream part size | Inspect backend multipart metrics if exact part behavior matters |
| `-mpu-concurrent-parts` | `2..1024`, nonzero `-part-size`, no whole-object checksum | Parts upload in bounded concurrent waves and successful parts are not restarted after a retry | Each logical PUT still contributes one SBK latency sample |
| `-connect-timeout-ms` | Nonnegative milliseconds | Connections complete inside the configured client timeout | `0` uses the SDK/OkHttp default |
| `-read-timeout-ms` | Nonnegative milliseconds | Response reads avoid the configured inactivity timeout | This is not an end-to-end operation deadline |
| `-write-timeout-ms` | Nonnegative milliseconds | Request-body writes avoid the configured inactivity timeout | This is not an end-to-end operation deadline |
| `-http-max-requests` | Nonnegative dispatcher limit | Async work completes under the process-wide OkHttp request ceiling | `0` selects an automatically derived value |
| `-http-max-requests-per-host` | Nonnegative per-host limit | Work completes under the endpoint-specific dispatcher ceiling | Keep it aligned with async depth and endpoint count |
| `-http-max-idle-connections` | Nonnegative pool size | Work completes with the selected reusable idle-connection pool | Zero intentionally disables idle retention |
| `-http-keepalive-seconds` | Positive seconds | Reused connections remain eligible for the selected idle period | Server/load-balancer idle timeout may be lower |
| `-retry-max-attempts` | Positive total attempt count | Transient I/O, HTTP 429, or HTTP 5xx failures can be retried within one measured operation | Retries inflate application-observed latency; use `1` for raw service latency |
| `-retry-backoff-ms` | Nonnegative delay | Retry delay is included in logical operation latency | Fixed delay only; not exponential backoff |
| `-warmup-requests` | Nonnegative count and permissions for the selected warm-up operation | Untimed warm-up requests complete before measurement | Data warm-up uses temporary objects and removes them before timing |
| `-endpoint-metrics` | Multiple endpoints when per-node attribution is useful | Per-endpoint completion/byte/retry/failure totals are printed at shutdown | Adds opt-in counters to completed-operation and retry paths |

### Range, listing, buckets, integrity, and data options

| Option | Prerequisite | A successful run confirms | Independent check or limitation |
|---|---|---|---|
| `-range-offset` | Nonnegative offset and at least one larger object | The backend serves a GET beginning at that byte | Used only by `range-get` |
| `-range-length` | Nonnegative length | The backend serves the requested range length | `0` uses `-size`; enable read-size verification |
| `-list-max-keys` | `1..1000` | Each timed LIST consumes up to the configured number of entries | A short prefix may return fewer |
| `-list-prefixes` | CSV prefixes visible to the credentials | LIST readers can be distributed over distinct namespaces | Used by LIST only |
| `-bucket-targets` | Explicit existing buckets and permission | Bucket-stat or bucket-delete can address known targets | Required for delete; delete targets must be empty |
| `-bucket-prefix` | S3-valid prefix and create permission | Unique generated bucket names under the prefix are accepted | Used by bucket-create only |
| `-cleanup-created-buckets` | Generated buckets remain empty and delete permission exists | Driver shutdown can remove buckets created by this run | `false` intentionally preserves them for inspection |
| `-checksum` | Backend supports the selected checksum header | PUT payload digest is accepted and server-side checksum validation succeeds | Unsupported backends can reject the request |
| `-tagging-enabled` | Nonempty tags and tag permission | PUT with native `PutObjectArgs.tags(...)` succeeds | Inspect tags using `tag-get` |
| `-tagging-tags` | Valid comma-separated key/value tags | Configured tags are accepted by PUT or tag-set | Values are reused for every operation |
| `-versioning-enabled` | Bucket versioning permission and backend support | Enabling versioning succeeds and discovery includes versions | Inspect bucket versioning state independently |
| `-sse-enabled` | Backend supports SSE-S3 and identity may request it | PUT/COPY with an `AES256` SSE-S3 request succeeds | It is SSE-S3, not SSE-KMS or SSE-C |
| `-data-compressibility` | Value `0..100` | Generated PUT data has the selected random/zero-byte shape | This controls client data; storage reduction must be checked server-side |
| `-data-dedupable` | No special prerequisite | PUT data either permits repetition or receives anti-dedup stamps | Verify physical reduction in backend telemetry |
| `-data-seed` | Any `long`; nonzero for reproducibility | Payload generation can be repeated across comparable runs | It does not make generated object keys identical |
| `-verify-read-size` | Correct catalog size; for range GET, correct range metadata | A GET fails if consumed bytes do not equal the expected length | It does not compare byte contents or checksums |

---

## Benchmark scenarios

### 1. `play.min.io` (public sandbox)

Use this first to confirm the driver and your network work end-to-end before
pointing at a real cluster. The public sandbox does not listen on plain HTTP,
so its HTTPS URL must be selected explicitly.

```bash
# Write benchmark, 30 seconds, 100-byte objects, 1 client
./build/install/sbk/bin/sbk -class minio -url https://play.min.io \
  -writers 1 -size 100 -seconds 30
```

```bash
# Read benchmark over the objects you just wrote
./build/install/sbk/bin/sbk -class minio -url https://play.min.io \
  -readers 1 -size 100 -seconds 30
```

The driver ships with `play.min.io` credentials baked into
[`minio.properties`](src/main/resources/minio.properties), so `-key` and
`-secret` can be omitted for this scenario. The explicit `-url` overrides
SBK's plain-HTTP transport default.

### 2. Local MinIO server in Docker

Start a single-node MinIO server:

```bash
docker run -d --name minio1 -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -v /tmp/minio-data:/data \
  minio/minio server /data --console-address ":9001"
```

Write benchmark (1 MiB objects, 8 concurrent writers, 2 minutes):

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 \
  -key minioadmin -secret minioadmin \
  -bucket sbk \
  -recreate true \
  -writers 8 -size 1048576 -seconds 120
```

Read benchmark over the same bucket (must run a writer pass first, or
pre-populate the bucket):

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 \
  -key minioadmin -secret minioadmin \
  -bucket sbk \
  -readers 8 -size 1048576 -seconds 120
```

Combined write/read in one invocation. Completed PUTs are handed to readers.
With the default `-ro false`, MinIO carries the PUT start timestamp into the
reader and reports write-start-to-GET-completion latency. This includes PUT,
handoff/queue delay, and GET; it is intentionally different from a standalone
GET latency. The bucket is recreated only when `-recreate true` is explicitly
supplied:

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 \
  -key minioadmin -secret minioadmin \
  -bucket sbk \
  -writers 4 -readers 4 -size 65536 -seconds 60
```

### 3. AWS S3

The driver works against AWS S3 unchanged — just point at the regional endpoint
and supply an IAM user's keys:

```bash
./build/install/sbk/bin/sbk -class minio \
  -url https://s3.us-east-1.amazonaws.com \
  -key AKIAxxxxxxxxxxxxxxxx \
  -secret 'wJalrXUtnFEMI/...' \
  -region us-east-1 \
  -bucket my-bench-bucket \
  -insecure false \
  -writers 16 -size 1048576 -seconds 300
```

For sizes ≥ 64 MiB, enable multipart upload. MinIO SDK 8.5.17 uploads parts
within one object sequentially; use multiple writers for parallel object
uploads:

```bash
./build/install/sbk/bin/sbk -class minio \
  -url https://s3.us-east-1.amazonaws.com \
  -key AKIAxxxx -secret '...' -region us-east-1 \
  -bucket my-bench-bucket \
  -writers 4 -size 268435456 -seconds 600 \
  -part-size 16777216
```

### 4. Dell ObjectScale / Dell ECS

ObjectScale (and the older Dell ECS) requires three things SBK users often
miss; the driver supports them all:

1. **The S3 data plane is not on port 443.** The default port is **9020 (HTTP)**
   or **9021 (HTTPS)**. Port 443 hosts the management UI and will reject all
   S3 requests with HTTP 405.
2. **Every request must carry a namespace identifier.** When using an IP-style
   endpoint (no DNS), the only way to send it is the `x-emc-namespace` HTTP
   header — wire it in with `-extra-headers`.
3. **Credentials are S3 access/secret keys belonging to an Object User**,
   not the cluster administrator's management credentials. Log in to the ObjectScale
   management UI to view or generate them.

#### Getting your S3 credentials

In a browser: open `https://<cluster-ip>/`, log in with an authorized cluster
management identity, navigate to **Object Users**, pick or create one, and
**Generate Secret Key**. Note:

- The **Object User name** is your **S3 access key**.
- The **Secret Key** is your **S3 secret key**.
- The **Namespace** that the user belongs to goes in `x-emc-namespace`.

Alternatively, with admin credentials you can fetch them from the management
REST API on port **4443**:

```bash
# 1) Authenticate, capture the session token
read -r -p 'ECS management user: ' ECS_ADMIN_USER
read -r -s -p 'ECS management password: ' ECS_ADMIN_PASSWORD
TOKEN=$(curl -sk -u "$ECS_ADMIN_USER:$ECS_ADMIN_PASSWORD" -X GET -D - \
  "https://<cluster-ip>:4443/login" \
  | awk -F': *' '/^X-SDS-AUTH-TOKEN:/{print $2}' | tr -d '\r\n')
unset ECS_ADMIN_PASSWORD

# 2) List namespaces & users
curl -sk -H "X-SDS-AUTH-TOKEN: $TOKEN" -H "Accept: application/json" \
  "https://<cluster-ip>:4443/object/namespaces" | jq .
curl -sk -H "X-SDS-AUTH-TOKEN: $TOKEN" -H "Accept: application/json" \
  "https://<cluster-ip>:4443/object/users" | jq .

# 3) Fetch a user's existing secret keys
curl -sk -H "X-SDS-AUTH-TOKEN: $TOKEN" -H "Accept: application/json" \
  "https://<cluster-ip>:4443/object/user-secret-keys/<object-user>" | jq .
```

#### Running the write benchmark

```bash
./build/install/sbk/bin/sbk -class minio \
  -url https://<cluster-ip>:9021 \
  -key '<object-user-name>' \
  -secret '<secret-key-1>' \
  -bucket 'sbk-bench' \
  -extra-headers 'x-emc-namespace=<namespace>' \
  -insecure true \
  -writers 4 -size 1048576 -seconds 300
```

#### Running the read benchmark

```bash
./build/install/sbk/bin/sbk -class minio \
  -url https://<cluster-ip>:9021 \
  -key '<object-user-name>' \
  -secret '<secret-key-1>' \
  -bucket 'sbk-bench' \
  -extra-headers 'x-emc-namespace=<namespace>' \
  -insecure true \
  -readers 4 -size 1048576 -seconds 300
```

#### Quick sanity check with `curl`

If something fails before the driver gets started, isolate the problem with
a raw S3 call (requires `awscli` for SigV4):

```bash
AWS_ACCESS_KEY_ID='<object-user-name>' \
AWS_SECRET_ACCESS_KEY='<secret-key-1>' \
aws --no-verify-ssl --endpoint-url https://<cluster-ip>:9021 \
    --region us-east-1 \
    s3api list-buckets
```

If this returns XML (`<ListAllMyBucketsResult>`), the cluster, endpoint, port,
and credentials are all correct. If it returns HTML / JSON / 405, fix that
before running SBK — see [Troubleshooting](#troubleshooting).

### 5. Ceph RGW

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://<rgw-host>:7480 \
  -key <ceph-access-key> \
  -secret <ceph-secret-key> \
  -bucket sbk \
  -writers 8 -size 1048576 -seconds 120
```

Some Ceph deployments expose RGW behind a reverse proxy on `https://…/s3/`;
in that case use the proxy URL. `-region us-east-1` (the default) is fine for
Ceph.

---

## Example performance results (Dell ObjectScale)

These numbers are actual measurements from a Dell ObjectScale cluster
(`https://10.249.249.223:9021`), captured from a single client host running
SBK against namespace `s3`, bucket `sbk-bench`, using Object User `testlogin`.

> ⚠️ **These are illustrative, not benchmarks of the cluster.** The client and
> the cluster were on the same lab network but the run was a single host with
> default OkHttp settings, modest concurrency, and short durations. They show
> the **shape of the numbers** SBK emits and how scaling concurrency / object
> size moves them — they are not a published throughput claim for ObjectScale.

The commands used:

```bash
# Common bits factored out
SBK=./build/install/sbk/bin/sbk
COMMON='-class minio -url https://10.249.249.223:9021 -insecure true
        -key testlogin -secret <SECRET>
        -bucket sbk-bench -extra-headers x-emc-namespace=s3'

# Run A: writes, 1 KiB, 1 writer, 30 s
$SBK $COMMON -writers 1 -size 1024 -seconds 30

# Run B: writes, 1 KiB, 8 writers, 30 s (concurrency scaling)
$SBK $COMMON -writers 8 -size 1024 -seconds 30

# Run C: writes, 1 MiB, 4 writers, 30 s (medium objects)
$SBK $COMMON -writers 4 -size 1048576 -seconds 30

# Run D: reads, 1 KiB, 4 readers, 30 s
$SBK $COMMON -readers 4 -size 1024 -seconds 30
```

### Measured results

| Run | Workload | Records | Throughput | MB/s | avg | p50 | p95 | p99 | p99.9 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| **A** | 1 KiB write, 1 writer | 103 | 3.4 rec/s | 0.00 | 291 ms | 289 ms | 304 ms | 320 ms | 330 ms |
| **B** | 1 KiB write, 8 writers | 488 | 16.3 rec/s | 0.02 | 489 ms | 479 ms | 595 ms | 1114 ms | 1213 ms |
| **C** | 1 MiB write, 4 writers | 158 | 5.3 rec/s | **5.26** | 751 ms | 686 ms | 1067 ms | 2651 ms | 2685 ms |
| **D** | 1 KiB read, 4 readers | 158 | 5.3 rec/s | 0.54 | 696 ms | 544 ms | 2135 ms | 2986 ms | 3128 ms |

### What the numbers tell us

**A → B: scaling writer concurrency from 1 → 8** boosted throughput from
3.4 to 16.3 rec/s (≈ 4.8× — not 8×, so the cluster or the client is starting
to push back), and the median latency rose from 289 → 479 ms (each individual
request slowed down because they're now queueing). The p99 jumped from 320 to
1114 ms — the **tail latency grew faster than the median**, which is the
classic signature of contention.

**C: large objects (1 MiB).** Per-record throughput dropped because each
record carries 1 MiB of payload on the wire, but **MB/sec rose to 5.26** —
the wire is now doing useful work. For 1 MiB objects, the median latency was
686 ms; this is your storage system's PUT latency for that size.

**D: reads were similar throughput to large-object writes but the p95 tail
is much worse** (2135 ms vs 1067 ms). On this cluster, GETs from a multi-MB
bucket appear less predictable than PUTs at this size — interesting datapoint
for a deeper investigation (warm/cold cache effects, internal compaction, …).

### How to interpret your own runs

1. **Run for at least 60 s.** Short runs are dominated by client warm-up,
   TLS handshake, JVM JIT compilation, and the cluster's cache warming.
2. **Watch the periodic 5-second windows** in the log, not just the total.
   The first window is often slower (warm-up); the median of windows 2..N
   is usually a better single number.
3. **Always look at p99 / p99.9** in addition to the average. An application
   sending 1000 requests/second will see the p99.9 latency once per second.
4. **Scale concurrency until throughput plateaus or p99 explodes.** That's
   the "knee" of the curve — the sweet spot for sizing.

---

## Advanced features

### End-to-end correctness workflow

Run this small workflow before a long saturation test. It uses a dedicated
bucket and prefix, verifies response lengths, and exercises both sync and async
paths. Replace the endpoint and credentials; do not use a production bucket.

```bash
SBK=./build/install/sbk/bin/sbk
ENDPOINT=http://127.0.0.1:9000
ACCESS_KEY=minioadmin
SECRET_KEY=minioadmin
BUCKET=sbk-minio-guide

# 1. Create/recreate the dedicated bucket and PUT at 20 objects/second.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -recreate true -prefix guide/base \
  -writers 1 -seconds 10 -records 20 -size 65536 \
  -data-seed 103 -data-compressibility 25 -data-dedupable false \
  -checksum crc32c -tagging-enabled true -tagging-tags purpose=sbk-guide \
  -run-manifest /tmp/sbk-minio-guide.json

# 2. Read the complete objects and assert the expected byte count.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base \
  -readers 2 -seconds 10 -records 20 -size 65536 \
  -read-operation get -verify-read-size true

# 3. Exercise native async ranged GET with bounded memory/concurrency.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base \
  -readers 2 -seconds 10 -records 20 -size 4096 \
  -read-operation range-get -range-offset 1024 -range-length 4096 \
  -verify-read-size true -async true -async-depth 4 \
  -async-max-inflight 8 -async-max-memory-mb 32

# 4. Check metadata, tags, and prefix listing independently.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base \
  -readers 1 -records 20 -size 1 -read-operation stat
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base \
  -readers 1 -records 20 -size 1 -read-operation tag-get
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -readers 1 -records 1 -size 1 \
  -read-operation list -list-prefixes guide/base -list-max-keys 100
```

Expected evidence:

- Step 1 reports completed writes at approximately the configured rate and
  writes a JSON manifest that contains no credentials.
- Step 2 reports completed GET records, nonzero read MB, and zero invalid
  latencies.
- Step 3 reports completed ranged reads with no size-verification failure.
- Step 4 reports successful metadata/tag/list records. Metadata operations
  correctly report zero payload MB because they do not transfer object bodies.

### Complete S3 operation cookbook

The commands below assume `SBK`, `ENDPOINT`, `ACCESS_KEY`, `SECRET_KEY`, and
`BUCKET` are set as in the workflow above and that `guide/base` contains
objects.

```bash
# UPDATE: overwrite existing keys with new 64 KiB payloads.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base -writers 2 -records 20 -size 65536 \
  -write-operation update

# COPY: server-side copy existing keys under guide/copied.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base -copy-prefix guide/copied \
  -writers 2 -records 20 -size 1 -write-operation copy

# TAG-SET, TAG-GET, and TAG-DELETE as separately timed API operations.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base -writers 1 -records 20 -size 1 \
  -write-operation tag-set -tagging-tags stage=verified,owner=storage
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base -readers 1 -records 20 -size 1 \
  -read-operation tag-get
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base -writers 1 -records 20 -size 1 \
  -write-operation tag-delete

# Weighted API mixes. Weights form a deterministic repeating cycle.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base -writers 4 -seconds 60 -size 65536 \
  -write-mix put=80,copy=20 -copy-prefix guide/mix-copy
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/base -readers 4 -seconds 60 -size 65536 \
  -read-mix get=80,stat=15,tag-get=5 -verify-read-size true

# Bucket existence and account bucket-list API latency.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -bucket-targets "$BUCKET" \
  -readers 1 -records 10 -size 1 -read-operation bucket-stat
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -readers 1 -records 10 -size 1 -read-operation bucket-list

# DELETE discovered objects only after all read/update/copy tests are done.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -prefix guide/ -writers 4 -seconds 30 -size 1 \
  -write-operation delete -catalog-max-objects 10000
```

Bucket lifecycle tests require create/delete-bucket permission. Creation uses
unique names; automatic cleanup is the safest default:

```bash
# Measure creation of 10 unique empty buckets and delete them at shutdown.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -writers 1 -records 10 -size 1 -write-operation bucket-create \
  -bucket-prefix sbk-guide-create -cleanup-created-buckets true

# Delete only explicitly named, already-empty disposable buckets.
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -writers 1 -records 2 -size 1 -write-operation bucket-delete \
  -bucket-targets sbk-delete-1,sbk-delete-2
```

For bucket deletion, a zero exit code confirms `removeBucket` completed.
Non-empty buckets are intentionally rejected by S3; the driver never
recursively erases them as part of `bucket-delete`.

### Multiple endpoints, distributed partitions, and object manifests

Use multiple URLs in `-url` only when every address exposes the same S3 cluster,
credentials, bucket, and namespace. SBK assigns workers round-robin:

```bash
$SBK -class minio \
  -url 'http://s3-node-1:9000,http://s3-node-2:9000,http://s3-node-3:9000' \
  -key "$ACCESS_KEY" -secret "$SECRET_KEY" -bucket "$BUCKET" \
  -writers 12 -size 1048576 -seconds 180 \
  -async true -async-depth 4 \
  -http-max-requests 64 -http-max-requests-per-host 32
```

This confirms that the client can execute through all configured endpoints,
but the result is aggregate. Use load-balancer or per-node server telemetry to
prove traffic distribution.

For independent SBK/SBK-GEM processes, give every process the same
`-partition-count` and a different `-partition-index`:

```bash
# Node/process 0
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -readers 4 -seconds 120 -size 65536 \
  -partition-count 2 -partition-index 0

# Node/process 1
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -readers 4 -seconds 120 -size 65536 \
  -partition-count 2 -partition-index 1
```

When startup LIST would be too expensive, prepare a local catalog:

```text
# /tmp/sbk-objects.csv: key,size[,versionId]
guide/base/object-0001,65536
guide/base/object-0002,65536
guide/base/object-0003,65536,3HL4kqtJlcpXroDTDmJ+rmSpXd3dIbrHY
```

```bash
$SBK -class minio -url "$ENDPOINT" -key "$ACCESS_KEY" -secret "$SECRET_KEY" \
  -bucket "$BUCKET" -object-file /tmp/sbk-objects.csv \
  -catalog-max-objects 100000 -readers 4 -seconds 60 -size 65536 \
  -read-operation get -verify-read-size true
```

The manifest must describe objects that really exist. SBK validates file
syntax at startup and validates response length when requested; it does not
preflight every key with an untimed HEAD.

### Operation examples

The following examples assume the bucket has first been populated with a PUT
run. They show the additional operation shapes without bypassing the MinIO
SDK:

```bash
# Bounded asynchronous PUT: 8 workers × 16 in-flight requests
./build/install/sbk/bin/sbk -class minio \
  -writers 8 -size 65536 -seconds 60 \
  -write-operation put -async true -async-depth 16

# Full-object overwrite of keys discovered at startup
./build/install/sbk/bin/sbk -class minio \
  -writers 4 -size 65536 -seconds 60 -write-operation update

# Server-side COPY (the object body does not pass through SBK)
./build/install/sbk/bin/sbk -class minio \
  -writers 4 -size 65536 -seconds 60 \
  -write-operation copy -copy-prefix copied

# Read 4 KiB beginning at byte 8192
./build/install/sbk/bin/sbk -class minio \
  -readers 8 -size 4096 -seconds 60 \
  -read-operation range-get -range-offset 8192 -range-length 4096

# Benchmark object metadata requests and listings
./build/install/sbk/bin/sbk -class minio \
  -readers 8 -size 1 -seconds 60 -read-operation stat
./build/install/sbk/bin/sbk -class minio \
  -readers 1 -size 1 -seconds 60 \
  -read-operation list -list-max-keys 1000
```

Bucket creation uses unique, S3-valid names. Bucket deletion is deliberately
restricted to an explicit list so a benchmark cannot accidentally delete its
main data bucket:

```bash
# Create unique empty buckets and remove them when the run closes
./build/install/sbk/bin/sbk -class minio \
  -writers 1 -size 1 -records 10 \
  -write-operation bucket-create -bucket-prefix sbk-create \
  -cleanup-created-buckets true

# Delete only these known-empty buckets
./build/install/sbk/bin/sbk -class minio \
  -writers 1 -size 1 -records 2 \
  -write-operation bucket-delete \
  -bucket-targets sbk-delete-1,sbk-delete-2
```

`bucket-delete` calls `MinioClient.removeBucket`; S3 rejects non-empty
buckets. The driver does not recursively delete their contents.

### Multipart upload

S3 multipart upload splits large objects into parts of 5 MiB to 5 GiB.
Enable it by setting `-part-size`:

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 -key minioadmin -secret minioadmin \
  -bucket sbk -recreate true \
  -writers 1 -size 268435456 -seconds 60 \
  -part-size 8388608 -mpu-concurrent-parts 8
```

With `-mpu-concurrent-parts 2` or greater, SBK uses MinIO SDK 8.5.17's public
low-level multipart API. It creates one upload, submits bounded waves of parts,
retries only failed parts, completes them in part-number order, and aborts the
upload after a terminal failure. The full payload remains one logical SBK
operation and therefore produces one latency sample. Values `0` and `1` retain
the SDK's normal `putObject` behavior. Concurrent multipart uploads require a
nonzero `-part-size` and cannot be combined with the driver's whole-object
`-checksum` option.

### S3 checksum validation

When `-checksum <algo>` is set, the driver computes the digest of every PUT
payload (locally) and sends it as the corresponding `x-amz-checksum-*`
header. Servers that support these headers verify the body integrity on
ingest.

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 -key minioadmin -secret minioadmin \
  -bucket sbk -recreate true \
  -checksum crc32c \
  -writers 4 -size 1048576 -seconds 60
```

Algorithms:

| `-checksum` | Header | Notes |
|---|---|---|
| `crc32` | `x-amz-checksum-crc32` | Java built-in CRC32 |
| `crc32c` | `x-amz-checksum-crc32c` | Java 9+ built-in CRC32C |
| `sha1` | `x-amz-checksum-sha1` | `java.security.MessageDigest` |
| `sha256` | `x-amz-checksum-sha256` | `java.security.MessageDigest` |
| `crc64nvme` | `x-amz-checksum-crc64nvme` | Reflected polynomial used by AWS S3, 256-entry table-driven implementation |

> ⚠️ **Compatibility note**: older S3 backends (Dell ObjectScale ≤ certain
> versions, older Ceph RGW) reject newer `x-amz-sdk-checksum-algorithm`
> announcement headers. The driver is intentionally pinned to MinIO SDK
> 8.5.17 to avoid this — see the comment in
> [`build.gradle`](build.gradle). User-requested checksums via `-checksum`
> work everywhere that accepts `x-amz-checksum-*`.

### Object tagging

Tag every written object:

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 -key minioadmin -secret minioadmin \
  -bucket sbk -recreate true \
  -tagging-enabled true \
  -tagging-tags 'env=prod,team=storage,benchmark=sbk' \
  -writers 1 -size 1024 -seconds 30
```

For ordinary PUT, tags are attached through the MinIO SDK's native
`PutObjectArgs.tags(...)` request. To measure tagging as a separate API call,
populate the bucket first and run `-write-operation tag-set`; that path uses
`setObjectTags`.

### Bucket versioning

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 -key minioadmin -secret minioadmin \
  -bucket sbk-versioned -recreate true \
  -versioning-enabled true \
  -writers 1 -size 1024 -seconds 30
```

When `-versioning-enabled true`, reads list with `includeVersions(true)` and
fetch each object by `versionId`.

### Data-shape controls (compressibility + anti-dedup)

For storage systems with inline compression or deduplication, the raw
random-bytes payload defeats both features and over-reports usable
throughput. Use `-data-compressibility` and `-data-dedupable` to model a
realistic workload:

```bash
# 30% compressible data with anti-dedup stamping (defeats inline dedup)
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 -key minioadmin -secret minioadmin \
  -bucket sbk -recreate true \
  -data-compressibility 30 \
  -data-dedupable false \
  -writers 4 -size 65536 -seconds 60
```

How it works (per 4 KiB chunk):

```
dedupable=true,  compressibility=0    →  random bytes (no compression, may dedup)
dedupable=true,  compressibility=100  →  all zeros (max compression + max dedup)
dedupable=true,  compressibility=50   →  2 KiB random + 2 KiB zeros
dedupable=false, compressibility=30   →  16 B (objectId, offset) + ~2.78 KiB random + ~1.21 KiB zeros
```

### Server-side encryption (SSE-S3)

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 -key minioadmin -secret minioadmin \
  -bucket sbk-encrypted -recreate true \
  -sse-enabled true \
  -writers 1 -size 1024 -seconds 30
```

The driver attaches `ServerSideEncryptionS3` to every PUT; objects are
encrypted at rest with server-managed keys. SSE-KMS and SSE-C are not
exposed by CLI flag (they need key material management); add them by
extending `MinIOWriter#sse` if needed.

### Object key layout (fsAccess + prefix)

By default, keys are `<bucket>-<run>-<writer>-<sequence>`. With
`-fs-access true` the keys are
spread across a 2-level hex tree (256 leaf "directories"), and `-prefix`
prepends an arbitrary key prefix:

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 -key minioadmin -secret minioadmin \
  -bucket sbk -recreate true \
  -fs-access true -prefix 'workload-1' \
  -writers 1 -size 1024 -records 5000
```

Sample keys produced:

```
workload-1/01/00/sbk-m5h2x8-0-1
workload-1/02/00/sbk-m5h2x8-0-2
...
```

Useful for testing prefix-scan behavior, S3A-style hashing, or systems that
optimize for hierarchical key layouts.

### Custom HTTP headers (vendor extensions)

`-extra-headers "k1=v1,k2=v2,..."` attaches an OkHttp interceptor that
stamps every S3 request with the supplied headers, *before* SigV4 signing.

Use cases:

- Dell ECS / ObjectScale: `x-emc-namespace=<namespace>`
- Trace / tenant IDs: `x-tenant-id=acme,x-trace-id=...`
- Anything else a particular backend requires

```bash
./build/install/sbk/bin/sbk -class minio \
  -url https://10.249.249.223:9021 \
  -key '<user>' -secret '<key>' \
  -bucket sbk-bench \
  -extra-headers 'x-emc-namespace=s3' \
  -writers 1 -size 100 -seconds 60
```

### HTTP client timeouts

```bash
./build/install/sbk/bin/sbk -class minio \
  -url https://my-cluster:9021 \
  -key '...' -secret '...' -bucket sbk \
  -connect-timeout-ms 5000 \
  -read-timeout-ms    30000 \
  -write-timeout-ms   30000 \
  -writers 1 -size 1024 -seconds 30
```

Defaults (when the flags are `0`) follow the OkHttp / MinIO SDK defaults
(typically 10 s connect, 10 s read, 10 s write).

---

## Tuning for higher throughput

Storage benchmarks are throughput-bound by request concurrency and network
bandwidth. Recommendations:

| Goal | Try |
|---|---|
| **Higher write throughput** | Increase `-writers`. Start at `#CPUs`, scale to `4 × #CPUs` for small objects, `1 × #CPUs` for ≥ 1 MiB objects. |
| **Higher read throughput** | Same as writers; the bottleneck is usually network bandwidth for ≥ 64 KiB objects. |
| **More concurrency without more SBK threads** | Add `-async true -async-depth 4`, then increase depth gradually. |
| **Saturate a single 10 GbE link** | `-size 67108864 -writers 16 -part-size 8388608` (64 MiB objects, 8 MiB parts) |
| **Tail-latency study** | Long run (`-seconds 1800`+) with moderate concurrency and CSV logging (see `-out csv`); look at p99, p99.9, p99.99 in the periodic dump. |
| **Stress prefix-listing** | `-fs-access true -prefix benchN/` so the bucket has 256 hash directories. |
| **Stress compression / dedup engines** | `-data-compressibility 30 -data-dedupable false`. |

If concurrency exceeds the endpoint or client dispatcher limits, throughput
will plateau and tail latency will climb. Keep a shared client (the driver
already uses one client per mode), then tune `-async-depth`,
`-http-max-requests`, and `-http-max-requests-per-host` together.

---

## Troubleshooting

### "non-S3 response from server (HTTP 405 …) — HINT: the endpoint is likely not an S3 service"

You're pointed at the wrong host / port. Many vendors run the management UI
on port 443 and the S3 data plane on a different port (9020 / 9021 for
ObjectScale, 9000 for MinIO server, 7480 for Ceph RGW). Probe:

```bash
for p in 9000 9020 9021 7480 80 443 8080; do
  echo "--- port $p ---"
  curl -sk --connect-timeout 3 "https://<host>:$p/" | head -c 200
  echo
done
```

A real S3 endpoint returns an XML body (either `<ListAllMyBucketsResult>` or
an `<Error>` like `<AccessDenied>`). HTML or `{"detail":...}` JSON means
wrong endpoint.

### "S3 error AccessDenied (HTTP 403)"

The credentials authenticated, but the user lacks permission on the bucket.
Common causes:

- The bucket is owned by a different Object User and ACLs forbid yours.
  Pick a fresh bucket name so the driver creates one owned by your user.
- Missing namespace header for Dell ECS / ObjectScale. Add
  `-extra-headers 'x-emc-namespace=<ns>'`.

### "S3 error InvalidRequest (HTTP 400): x-amz-sdk-checksum-algorithm specified, but no corresponding x-amz-checksum-* are found"

Specific to older S3-compatible backends (Dell ObjectScale ≤ certain
versions, older Ceph) and MinIO SDK 9.x. The driver pins to MinIO SDK
8.5.17 to avoid this; if you see this error, verify
[`drivers/minio/build.gradle`](build.gradle) still uses `8.5.17`.

### "non-XML response from server. Response code: 200, Content-Type: text/html"

Same root cause as 405 — the endpoint is not S3. With newer drivers we set
`region=us-east-1` by default, which avoids the `GetBucketLocation` round
trip and side-steps this for the bucket-exists check. If you still see this
on a working endpoint, capture the response with `curl` and check whether
the endpoint URL actually points to S3.

### `RejectedExecutionException` / `InterruptedIOException: executor rejected` at end of run

A benign shutdown-time race between SBK's framework lifecycle and the SDK's
HTTP dispatcher. The driver catches and swallows these specifically when
they happen during teardown. If you see them in the middle of a run, file an
issue — that indicates a different problem.

### Build fails on `halodb`

`halodb` is hosted on GitHub Packages which has aggressive rate limits.
Disable it for benchmarking-only builds — see the top-level project's
notes.

---

## Known constraints

- **Mixed-run latency differs from standalone latency.** With both writers and
  readers and the default `-ro false`, the driver reports write-start-to-GET
  completion latency for objects published by completed PUT/COPY operations.
  Run writers and readers separately when you need isolated PUT and GET
  service latency.
- **SigV2** is not supported (the MinIO SDK is SigV4-only). Unsupported
  `-auth-version` values fail during argument validation.
- **Concurrent multipart plus whole-object checksum** is rejected because the
  low-level multipart contract requires per-part checksum semantics that the
  current driver does not expose.
- **SSE-KMS** and **SSE-C** are not exposed by CLI flag.
- **`-data-compressibility 100 -data-dedupable false`** produces objects
  that are *mostly* compressible but still defeat dedup. Pure all-zeros
  payloads require `-data-compressibility 100 -data-dedupable true`.

For the full SBK option set (logging, output formats, CSV / JMX exporters,
distributed runs via SBK-GEM), see the top-level
[SBK README](../../README.md).

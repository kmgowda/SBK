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

- End-to-end (write → read) latency. The numbers reported are per-PUT and
  per-GET, not "time from when a writer put an object to when a reader
  could observe it".
- SigV2 (legacy S3 signing). Only SigV4 is implemented.
- Username / password login. S3 protocol has no such concept — only access
  key + secret key. If you have an admin username (e.g. ObjectScale
  `root`/`ChangeMe`), use it to log in to the management UI and generate
  S3 keys for an Object User.

---

## Table of contents

- [Quick start](#quick-start)
- [Full CLI flag reference](#full-cli-flag-reference)
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

Smoke-test against MinIO's public sandbox `play.min.io` (no credentials needed):

```bash
./build/install/sbk/bin/sbk -class minio -writers 1 -size 100 -seconds 30
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

## Full CLI flag reference

Every option also has a default in
[`src/main/resources/minio.properties`](src/main/resources/minio.properties).
Command-line flags override the properties file.

### Connection & credentials

| Flag | Default | Purpose |
|---|---|---|
| `-url <url>` | `https://play.min.io` | S3 endpoint URL, including scheme and port |
| `-bucket <name>` | `sbk` | Bucket to read / write |
| `-key <access-key>` | (play.min.io sandbox key) | S3 access key |
| `-secret <secret-key>` | (play.min.io sandbox secret) | S3 secret key |
| `-region <region>` | `us-east-1` (driver default) | AWS region for SigV4 signing. **Always set** so the SDK skips `GetBucketLocation`, which many non-AWS backends mishandle. |
| `-recreate true|false` | `false` (auto `true` if both writers and readers given) | Drop and recreate the bucket on `openStorage` |
| `-insecure true|false` | `true` | Skip TLS certificate validation. Useful for lab clusters with self-signed certs. |
| `-auth-version 2|4` | `4` | S3 signature version. The MinIO SDK is **always SigV4**; `2` is accepted but logs a warning and falls back to SigV4. |

### Object naming

| Flag | Default | Purpose |
|---|---|---|
| `-fs-access true|false` | `false` | Spread keys across a 2-level hex directory tree (`aa/bb/sbk-<uuid>`), mimicking how applications like Apache Hadoop S3A create paths. Helps test ListObjects / prefix-scan behavior. |
| `-prefix <p>` | `""` | Prepend `<p>/` to every generated object key |

### Workload size & rate (inherited from SBK core)

| Flag | Purpose |
|---|---|
| `-writers <n>` | Number of concurrent writer threads |
| `-readers <n>` | Number of concurrent reader threads |
| `-size <bytes>` | Object size in bytes |
| `-seconds <s>` | Run duration |
| `-records <n>` | Alternative to `-seconds`: fixed record count |
| `-throughput <rps>` | Cap on records/sec (use `0` for unlimited) |

### Multipart upload (large objects)

| Flag | Default | Purpose |
|---|---|---|
| `-part-size <bytes>` | `0` (disabled) | Trigger multipart upload when object size ≥ part size. Valid range: **5 MiB ≤ partSize ≤ 5 GiB** (S3 spec). |
| `-mpu-concurrent-parts <n>` | `0` | Concurrent parts in flight per object. Accepted for forward-compat but **info-only with the current MinIO SDK 8.5.x** (the SDK manages multipart parallelism internally based on part size). |

### S3 checksum validation

| Flag | Values | Purpose |
|---|---|---|
| `-checksum <algo>` | `crc32` / `crc32c` / `sha1` / `sha256` / `crc64nvme` / *empty* | When set, the driver computes the digest of every PUT payload and sends it as the `x-amz-checksum-<algo>` header. The server (if it supports it) verifies the body matched. |

### Object tagging

| Flag | Default | Purpose |
|---|---|---|
| `-tagging-enabled true|false` | `false` | When `true`, every PUT is followed by `SetObjectTags` |
| `-tagging-tags "k1=v1,k2=v2,..."` | `""` | Tag set, applied to every written object |

### Bucket versioning

| Flag | Default | Purpose |
|---|---|---|
| `-versioning-enabled true|false` | `false` | Enable versioning on the bucket. Reads will include version IDs when listing. |

### Data-shape controls

These shape the **payload bytes** the driver generates, not the protocol.
Useful when benchmarking storage with inline compression or deduplication.

| Flag | Default | Purpose |
|---|---|---|
| `-data-compressibility <0..100>` | `0` | Target compressibility percentage. Each 4 KiB chunk is split: `100-N`% random bytes (incompressible), `N`% zero bytes (highly compressible). `0` = fully random, `100` = all zeros. |
| `-data-dedupable true|false` | `true` | When `false`, stamps every 4 KiB chunk with a 16-byte `(objectId, chunkOffset)` header that defeats inline deduplication. |

### Server-side encryption

| Flag | Default | Purpose |
|---|---|---|
| `-sse-enabled true|false` | `false` | Encrypt at rest with S3-managed keys (SSE-S3 / `aws:kms` equivalent). |

### Vendor-specific headers and HTTP tuning

| Flag | Default | Purpose |
|---|---|---|
| `-extra-headers "k1=v1,k2=v2"` | `""` | Inject arbitrary HTTP headers on every S3 request via an OkHttp interceptor. Primary use case: `x-emc-namespace=<ns>` for Dell ECS / ObjectScale. |
| `-connect-timeout-ms <ms>` | `0` (SDK default) | HTTP connect timeout |
| `-read-timeout-ms <ms>` | `0` (SDK default) | HTTP read timeout |
| `-write-timeout-ms <ms>` | `0` (SDK default) | HTTP write timeout |

---

## Benchmark scenarios

### 1. `play.min.io` (public sandbox)

Zero-config smoke test. Use this first to confirm the driver and your network
work end-to-end before pointing at a real cluster.

```bash
# Write benchmark, 30 seconds, 100-byte objects, 1 client
./build/install/sbk/bin/sbk -class minio -writers 1 -size 100 -seconds 30
```

```bash
# Read benchmark over the objects you just wrote
./build/install/sbk/bin/sbk -class minio -readers 1 -size 100 -seconds 30
```

The driver ships with `play.min.io` credentials baked into
[`minio.properties`](src/main/resources/minio.properties), so `-url`, `-key`,
`-secret` can be omitted for this scenario.

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

Combined write-then-read in one invocation (driver auto-sets `-recreate true`
when both writers and readers are specified):

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

For sizes ≥ 64 MiB, enable multipart upload so the SDK uploads in parallel:

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
   not the cluster admin's `root` / `ChangeMe`. Log in to the ObjectScale
   management UI to view or generate them.

#### Getting your S3 credentials

In a browser: open `https://<cluster-ip>/`, log in as a cluster admin (e.g.
`root` / `ChangeMe`), navigate to **Object Users**, pick or create one, and
**Generate Secret Key**. Note:

- The **Object User name** is your **S3 access key**.
- The **Secret Key** is your **S3 secret key**.
- The **Namespace** that the user belongs to goes in `x-emc-namespace`.

Alternatively, with admin credentials you can fetch them from the management
REST API on port **4443**:

```bash
# 1) Authenticate, capture the session token
TOKEN=$(curl -sk -u root:ChangeMe -X GET -D - \
  "https://<cluster-ip>:4443/login" \
  | awk -F': *' '/^X-SDS-AUTH-TOKEN:/{print $2}' | tr -d '\r\n')

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

### Multipart upload

S3 multipart upload splits large objects into parts of 5 MiB to 5 GiB and
uploads them in parallel. Enable by setting `-part-size`:

```bash
./build/install/sbk/bin/sbk -class minio \
  -url http://127.0.0.1:9000 -key minioadmin -secret minioadmin \
  -bucket sbk -recreate true \
  -writers 1 -size 268435456 -seconds 60 \
  -part-size 8388608     # 8 MiB parts, ~32 parts per 256-MiB object
```

The SDK chunks the upload and uploads parts in parallel internally. There is
no per-object parallelism knob in the SDK version this driver uses; the
`-mpu-concurrent-parts` flag is accepted for forward compatibility but logged
as info-only.

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

Tags are issued via a follow-up `SetObjectTags` after each PUT, which works
on backends that don't accept `x-amz-tagging` on PUT.

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

By default, keys are `<bucket>-<uuid>`. With `-fs-access true` the keys are
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
workload-1/01/00/sbk-3c2ca874-6b35-45bd-a94b-cdee44e862d9
workload-1/02/00/sbk-9e8f0a1c-7d2a-43b1-90ee-1f53b8d3a2c1
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
| **Saturate a single 10 GbE link** | `-size 4194304 -writers 16 -part-size 4194304` (or higher) |
| **Tail-latency study** | Long run (`-seconds 1800`+) with moderate concurrency and CSV logging (see `-out csv`); look at p99, p99.9, p99.99 in the periodic dump. |
| **Stress prefix-listing** | `-fs-access true -prefix benchN/` so the bucket has 256 hash directories. |
| **Stress compression / dedup engines** | `-data-compressibility 30 -data-dedupable false`. |

If `-writers` × `-readers` exceeds the SDK's default HTTP connection pool,
you may see latency cliffs; bump the OkHttp pool by passing higher
timeouts and consider patching `MinIOWriter` to share a single client
instance (the driver already does — one `MinioClient` is shared across
all threads).

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

- **End-to-end latency** (write → propagation-to-read) is **not** supported.
  The driver reports per-PUT and per-GET latencies only.
- **SigV2** is not supported (the MinIO SDK is SigV4-only). The
  `-auth-version 2` flag is accepted but logs a warning and falls back.
- **`-mpu-concurrent-parts`** is accepted but is info-only with the current
  SDK version. Multipart parallelism is managed internally based on
  `-part-size`.
- **SSE-KMS** and **SSE-C** are not exposed by CLI flag.
- **`-data-compressibility 100 -data-dedupable false`** produces objects
  that are *mostly* compressible but still defeat dedup. Pure all-zeros
  payloads require `-data-compressibility 100 -data-dedupable true`.

For the full SBK option set (logging, output formats, CSV / JMX exporters,
distributed runs via SBK-GEM), see the top-level
[SBK README](../../README.md).

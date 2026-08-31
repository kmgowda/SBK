<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# MinIO/S3 driver implementation

This document explains how SBK's `MinIO` driver turns a benchmark command into
S3 requests. It is intended for reviewers, maintainers, and performance
engineers who need to know exactly what a reported operation measures.

For operating procedures, use the
[ECS/OBS benchmark runbook](ECS_OBS_BENCHMARK_RUNBOOK.md). For the
complete command-line reference and backend tutorials, use the
[driver README](../README.md). Software agents must also follow the
[ECS/OBS agent workflow](ECS_OBS_AGENT_RUNBOOK.md).

## Runtime stack

```text
SBK writer/reader worker
        |
        v
MinIOWriter / MinIOReader
        |
        +-- S3OperationMix       deterministic operation selection
        +-- S3ObjectCatalog     existing/published object selection
        +-- S3RetryPolicy       optional bounded retry sequence
        +-- S3AsyncExecutor     optional bounded SDK-future tracking
        |
        v
MinIO Java SDK 8.5.17
        |
        v
OkHttp connection pool / dispatcher
        |
        v
S3 endpoint: MinIO, ECS/ObjectScale, AWS S3, Ceph RGW, ...
```

The driver deliberately uses the vendor SDK instead of implementing S3
signing or wire protocol code. SBK's normal writer/reader adapters provide the
measurement timestamps. The synchronous driver path reports after the SDK call
and response consumption complete. The asynchronous path reports from the
tracked completion callback.

The SDK is intentionally pinned to 8.5.17 in
[`build.gradle`](../build.gradle). Later SDK behavior can add checksum
announcement headers that older ECS/ObjectScale and other S3-compatible
releases reject. An SDK upgrade is therefore a compatibility change, not a
routine dependency refresh.

## Class responsibilities

| Class | Responsibility |
|---|---|
| [`MinIO`](../src/main/java/io/sbk/driver/MinIO/MinIO.java) | CLI/configuration binding, client construction, bucket setup, warm-up, catalog discovery, endpoint assignment, manifests, and lifecycle |
| [`MinIOWriter`](../src/main/java/io/sbk/driver/MinIO/MinIOWriter.java) | PUT, update, copy, delete, tag mutation, and bucket mutation operations |
| [`MinIOReader`](../src/main/java/io/sbk/driver/MinIO/MinIOReader.java) | GET, Range GET, stat, tag read, LIST, bucket stat, and bucket list operations |
| [`S3AsyncExecutor`](../src/main/java/io/sbk/driver/MinIO/S3AsyncExecutor.java) | Per-worker and process-wide in-flight bounds; retains futures until their measurement callback completes |
| [`S3MultipartUploader`](../src/main/java/io/sbk/driver/MinIO/S3MultipartUploader.java) | Bounded multipart waves, ordered completion, per-part retry, and abort after terminal failure |
| [`S3ObjectCatalog`](../src/main/java/io/sbk/driver/MinIO/S3ObjectCatalog.java) | Bounded existing-object inventory, reader partitioning, delete claims, and completed-PUT publication |
| [`S3ObjectKey`](../src/main/java/io/sbk/driver/MinIO/S3ObjectKey.java) | Sequential, hashed, random, filesystem-style, and distributed-partition key layouts |
| [`S3ObjectSizeSelector`](../src/main/java/io/sbk/driver/MinIO/S3ObjectSizeSelector.java) | Fixed, seeded uniform, deterministic sweep, and deterministic weighted object sizes |
| [`S3OperationMix`](../src/main/java/io/sbk/driver/MinIO/S3OperationMix.java) | Exact repeating weighted operation cycles; no random selection noise |
| [`S3RetryPolicy`](../src/main/java/io/sbk/driver/MinIO/S3RetryPolicy.java) | Optional retries for network I/O, HTTP 429, and HTTP 5xx responses |
| [`S3DataGenerator`](../src/main/java/io/sbk/driver/MinIO/S3DataGenerator.java) | Reproducible payload content, compressibility, and anti-dedup stamping |
| [`S3PayloadPool`](../src/main/java/io/sbk/driver/MinIO/S3PayloadPool.java) | Reuses arrays while regenerating object content; buffers are not reused until SDK completion |
| [`S3EndpointMetrics`](../src/main/java/io/sbk/driver/MinIO/S3EndpointMetrics.java) | Optional completed-operation, logical-byte, retry, and terminal-failure totals per configured endpoint |

## Startup sequence

`MinIO.openStorage()` performs startup work outside the measured operation
path:

1. Normalize and deduplicate the comma-separated `-url` list. A missing scheme
   becomes `http://`.
2. Build sync and/or async SDK clients and dedicated OkHttp dispatchers.
3. Apply explicit timeouts, connection-pool limits, TLS policy, and extra
   headers such as ECS `x-emc-namespace`.
4. Check the main bucket when the selected operations use it.
5. Optionally empty and recreate the bucket. This is destructive and happens
   only when writers are configured and `-recreate true` is explicit.
6. Optionally enable bucket versioning.
7. Run the selected untimed warm-up and remove its temporary objects.
8. Build an object catalog only when an operation needs existing objects.
9. Validate prerequisites such as a nonempty catalog or a Range GET-eligible
   object.
10. Optionally write the credential-free run manifest.

Pure PUT, LIST, bucket-create, bucket-delete, bucket-stat, and bucket-list
workloads do not require an existing-object catalog. GET, Range GET, stat,
update, copy, delete, and object-tag operations do.

Catalog discovery is a recursive S3 LIST performed once at startup. It is
bounded by `-catalog-max-objects`. `-object-file` avoids that startup LIST by
loading `key,size[,versionId]` records from a local file. With
`-partition-by-prefix true`, distributed clients use a server-filterable
`partition-N/` prefix; otherwise each client receives keys by stable Java hash.

## Operation and byte semantics

One SBK record is one logical selected operation, including all configured
retries. Multipart PUT is also one logical record even though it makes several
S3 requests.

| Operation | Completion point | Bytes reported for that record |
|---|---|---:|
| PUT/update | SDK upload completes | uploaded object bytes |
| Copy | server-side copy completes | source object logical bytes; bytes do not cross the load-generator network |
| Delete/tag set/tag delete/stat/tag get | SDK request completes | 0 |
| GET | complete response body is drained | response-body bytes |
| Range GET | requested response range is drained | returned range bytes |
| LIST | result page entries are consumed | sum of the listed objects' logical sizes |
| Bucket create/delete/stat/list | SDK request completes | 0 |

The LIST `MB/sec` value is therefore **not LIST response-wire bandwidth**. It
is the logical size represented by returned entries. Judge LIST primarily by
operations/sec and latency, and retain `-list-max-keys` plus the populated
object count with the result.

`-verify-read-size true` checks GET and Range GET response lengths. It does not
compare response content with the original payload. `-checksum` asks the S3
service to validate the requested checksum on PUT; it is not a later GET
content comparison.

## Synchronous and asynchronous execution

In synchronous mode, each SBK worker performs one SDK operation at a time. A
comma-separated endpoint list assigns workers round-robin.

In asynchronous mode:

- every worker has at most `-async-depth` SDK operations in flight;
- `-async-max-inflight` bounds all workers in the process;
- concurrency permits are acquired before latency measurement begins, so
  local waiting for a permit is excluded from service latency;
- the future plus its measurement callback remains tracked until both finish;
- the driver retains one payload per in-flight PUT and one reusable 64 KiB
  drain buffer per in-flight GET;
- the startup memory guard rejects unsafe configurations before connecting.

The conservative buffer estimate is:

```text
writer payload bytes = min(process limit, writers * async depth) * largest object
reader buffer bytes  = readers * async depth * 64 KiB
estimated bytes      = writer payload bytes + reader buffer bytes
```

For synchronous execution, the estimate uses one object per writer and one
64 KiB buffer per reader. `uniform`, `sweep`, and `weighted` configurations use
their largest possible object in this calculation.

The OkHttp automatic total/per-host request limit is
`max(64, workers * asyncDepth)` unless explicitly overridden. Connection-pool
reuse avoids a TCP/TLS setup for every request.

## Multipart behavior

`-part-size` selects multipart upload for eligible objects. The part size is
validated against S3's 5 MiB through 5 GiB limits. With
`-mpu-concurrent-parts 2` or greater, `S3MultipartUploader`:

1. creates one multipart upload;
2. submits a bounded wave of parts;
3. retries failed parts under the configured retry policy;
4. completes parts in part-number order; and
5. aborts the multipart upload after terminal failure.

The driver uses bounded views of the object payload instead of allocating a
new array for every part. Concurrent multipart cannot be combined with the
whole-object `-checksum` option.

For high-RTT ECS paths, first validate multipart with a small fixed
`-records` run. A timed run can have uploads in flight when the duration ends;
SBK will allow at most five seconds for all cleanup and will exit nonzero if
final results may be incomplete.

## Retry and endpoint metrics

The default `-retry-max-attempts 1` disables retries. When enabled, a retry is
allowed for network `IOException`, HTTP 429, and HTTP 5xx. The entire retry
sequence remains one latency sample, so retry delay and additional attempts
increase the reported operation latency.

`-endpoint-metrics true` creates one counter group for each configured URL.
The shutdown summary contains completed logical operations/bytes, retry
attempts, and terminal failures. These are SDK completion counters rather than
PerL reporting-window counters: async completions at a timed boundary can make
them differ slightly from the final timed record count. A fixed-record
qualification should have exact completed-operation parity and zero failures.

## ECS/ObjectScale integration

For direct-IP ECS access, use the S3 data plane (`9020` HTTP or `9021` HTTPS),
not the management UI/API, and inject the namespace header:

```text
-url https://ecs.example.test:9021
-extra-headers x-emc-namespace=<namespace>
```

The header is installed in an OkHttp interceptor before the request proceeds.
Credentials are the ECS Object User access key and secret key; management
credentials are never used by this driver.

## Shutdown

Worker `close()` drains tracked async futures. `closeStorage()` then removes
only buckets created by a bucket-create workload when requested, closes SDK
clients, and prints endpoint totals. Interrupt/rejected-executor failures caused
by normal timed teardown are treated as clean shutdown signals.

The enclosing SBK lifecycle has an absolute five-second cleanup deadline. If
workers, the SDK, the driver, PerL, or a logger cannot complete within that
deadline, the executable exits with failure because final results may be
incomplete. Never publish a run containing
`BenchmarkCleanupTimeoutException`.

## Verification ownership

The driver unit tests cover option validation, SDK argument construction,
reader contracts, operation/size/key helpers, retry policy, payload reuse,
async bounds, and multipart orchestration. Real-backend qualification remains
required because SDK compatibility, TLS, namespace routing, permissions, and
server behavior cannot be proven by unit tests.

Use:

```bash
./gradlew :drivers:minio:check :installDist
./build/install/sbk/bin/sbk -class minio -help
```

Then follow the fixed-record and timed stages in the
[ECS/OBS benchmark runbook](ECS_OBS_BENCHMARK_RUNBOOK.md).

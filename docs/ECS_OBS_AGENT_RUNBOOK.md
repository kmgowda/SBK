<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# ECS/OBS benchmarking workflow for software agents

This document gives Devin, Windsurf, Codex, Cursor, Aider, and other software
agents a deterministic workflow for building and running Dell ECS/ObjectScale
S3 benchmarks with SBK. It supplements, and never weakens, the repository
rules in [`AGENTS.md`](../AGENTS.md).

An agent executing a benchmark must also read completely:

1. [ECS/OBS benchmark runbook](ECS_OBS_BENCHMARK_RUNBOOK.md)
2. [MinIO/S3 driver implementation](MINIO_DRIVER_IMPLEMENTATION.md)
3. [Benchmark methodology and results](../.devin/skills/sbk-benchmark-runner/references/methodology-and-results.md)
4. [SBK benchmark runner skill](../.devin/skills/sbk-benchmark-runner/SKILL.md)
5. For multiple load hosts only, the
   [distributed benchmark runner skill](../.devin/skills/sbk-distributed-benchmark-runner/SKILL.md)

## 1. Decide whether execution is authorized

A request to document, review, explain, or plan a benchmark does not authorize
remote writes. Before running against ECS, the agent needs an explicit target
and permission to create benchmark objects. Before using `-recreate true`,
delete, bucket-delete, or cleanup, it needs explicit authority for the exact
bucket/targets.

Never infer authority to:

- use a production namespace or bucket;
- retrieve or rotate an ECS Object User secret;
- use management administrator credentials for S3;
- disable TLS validation outside an approved lab;
- delete existing objects, versions, or buckets;
- run enough load to disrupt other tenants;
- publish raw results containing internal inventory or credentials.

If authority is missing, produce a sanitized plan and stop before mutation.

## 2. Required input contract

Resolve these inputs before constructing a performance command:

```text
objective: PUT | GET | Range GET | LIST/stat | multipart | mixed | scale
endpoint(s): S3 data-plane URL(s), including scheme and port
namespace: ECS namespace, if direct-IP/header routing is used
credentials source: environment/secret launcher; never literal in repository
dedicated bucket and prefix:
permission: create/write/read/delete/tag/version/SSE as required
load-generator topology: one host or SBK-GEM inventory
object-size distribution:
data compressibility/dedupability:
worker/depth sweep:
fixed qualification count:
timed duration and repetitions:
cleanup owner and policy:
acceptance criteria:
telemetry sources:
```

Do not guess an absent value that changes the target, destructive scope,
security posture, or load magnitude.

## 3. Source-of-truth discovery

From the repository root:

```bash
git status --short --branch
./gradlew :drivers:minio:check :installDist
./build/install/sbk/bin/sbk -class minio -help
```

For distributed load:

```bash
./build/install/sbk/bin/sbk-gem -class minio -help
```

Read the checked-in
[`minio.properties`](../drivers/minio/src/main/resources/minio.properties).
Never invent a MinIO option or reuse an option from a different driver. The
single endpoint/pool option is `-url`; there is no separate `-endpoints` flag.

## 4. Protect secrets

Use an approved secret provider or process environment:

```bash
export SBK_S3_ACCESS_KEY='<injected-object-user>'
export SBK_S3_SECRET_KEY='<injected-secret>'
```

Commands and YML committed to the repository must contain placeholders only.
Do not print the environment, management API response, or process command line
after injecting secrets. Redact access keys as well as secret keys when the
environment classifies user identities as sensitive.

For ECS, `-extra-headers "x-emc-namespace=$ECS_NAMESPACE"` is a routing input,
not a credential. It may still be internal inventory and should be sanitized
in public reports.

## 5. Classify the topology

- One SBK process connecting to remote ECS is a **single-load-generator** run.
  Use `sbk` or `sbk-yal`.
- Several ECS endpoints in one `-url` value still form one SBK process.
- Several load-generator hosts require `sbk-gem`/`sbk-gem-yal`, or manually
  launched `GrpcLogger` clients plus SBM.
- Do not use SBK-GEM merely because ECS is a cluster.

For a multi-host run, first make the exact ordinary SBK command pass on every
load host. Then validate SSH/deployment/SBM with a small fixed-count GEM run.

## 6. Execute in mandatory stages

### Stage A: read-only preflight

1. Confirm JDK 25 and SBK version.
2. Resolve the S3 data-plane endpoint. ECS defaults are 9020/9021; reject
   HTML/JSON/405 responses from management ports.
3. Confirm the namespace, Object User, bucket/prefix, permissions, route, RTT,
   MTU, and load-host capacity.
4. Confirm ECS health and that no rebuild, upgrade, or unrelated saturation is
   active.
5. Record the objective and acceptance criteria.

### Stage B: fixed-count qualification

Use one worker and a very small count in a dedicated prefix. Require exact
Total and endpoint operation counts. Then qualify the operations needed by the
planned workload:

- PUT to populate;
- GET with `-verify-read-size true`;
- Range GET with an eligible object;
- LIST with a known prefix and `-list-max-keys`;
- multipart with fixed records;
- tags, checksum, versioning, SSE, or bucket APIs only when in scope.

Do not move to timed load after a warning, retry, endpoint failure, mismatch,
or nonzero exit.

### Stage C: one-variable baseline

Hold everything constant except one factor:

1. worker count;
2. then async depth, if async behavior is in scope;
3. then object size/distribution;
4. then data shape;
5. then operation mix;
6. then endpoint/load-host count.

Use at least three repetitions in randomized order. Keep failed and slow runs.

### Stage D: sustained workload

Run long enough to observe stable windows, ECS background activity, and tail
latency. A normal qualification series uses 300 seconds; a durability or tail
study may need 1,800 seconds or more. Monitor both the load host and ECS.

### Stage E: independent state check

For mutations, independently verify the intended state through approved ECS or
S3 tooling. Do not use lagging billing/metering counts as immediate object
truth; ECS metering can settle later than the data path.

## 7. Workload-generation rules

### Use representative object populations

- `fixed`: one size from common `-size`.
- `uniform:min:max`: seeded random, inclusive distribution.
- `sweep:min:max`: deterministic sequential byte-size sweep; not random.
- `weighted:size=weight,...`: exact repeating weighted distribution.

Use a nonzero `-data-seed` for A/B reproducibility. Set
`-data-dedupable false` when the objective is physical ingest rather than
inline dedup benefit. Set the application-representative
`-data-compressibility` explicitly.

### Preserve overload visibility

For a saturation baseline:

```text
-retry-max-attempts 1 -endpoint-metrics true
```

HTTP 429/5xx retries can convert a capacity limit into higher apparent
latency. Enable retries in a separate run only when reproducing application
policy. Report retry count with throughput and latency.

### Bound async memory and concurrency

Always specify the relevant bounds in an async report:

```text
-async true
-async-depth <per-worker>
-async-max-inflight <process-wide>
-async-max-memory-mb <budget>
```

Increase depth gradually. Stop when throughput plateaus, p99 exceeds the
objective, retries appear, or the load host/backend saturates.

### Treat multipart separately

Multipart has outer object concurrency and inner part concurrency. Change one
at a time. First use fixed records; then timed load. Reject a timed run that
exceeds SBK's five-second cleanup deadline.

### Interpret operations correctly

- Copy bytes are logical server-side bytes, not client-network bytes.
- LIST bytes are the sum of listed object sizes, not response-wire bytes.
- Metadata operations often report zero bytes; use operations/sec and latency.
- Mixed workload percentiles combine operation types. Split runs when
  per-operation latency is required.

## 8. Distributed-agent workflow

Before SBK-GEM:

1. qualify ordinary SBK on each node;
2. verify trusted SSH host keys and noninteractive authentication;
3. verify Java/SBK provisioning space and permissions;
4. verify every node reaches ECS and the controller's SBM callback address;
5. use a one-node fixed-count GEM run;
6. use a two-node fixed-count run and confirm aggregate count;
7. progress through `1, 2, N` nodes and calculate scaling efficiency.

Use `-totalrecords` or `-totalthroughput` for aggregate targets. Do not pass
the same nonzero `-partition-index` to every node. Prefer partition prefixes
for large existing-object catalogs.

Do not commit `gempass`, SSH private keys, storage credentials, or production
inventories. The portable template lives at
[`example-sbk-gem-minio.yml`](../.devin/skills/sbk-distributed-benchmark-runner/references/example-sbk-gem-minio.yml).

## 9. Machine-checkable acceptance

An agent must classify a run as failed or qualified—not silently successful—if
any of these occurs:

```text
process exit != 0
missing Total line
BenchmarkCleanupTimeoutException
expected fixed records != Total records
endpoint operations != expected fixed operations
endpoint failures > declared allowance
endpoint retries > declared allowance
invalid latencies > 0
unexplained discarded latencies > 0
S3/HTTP/I/O/authentication exception
unexpected bucket/prefix/namespace
load generator is the unintended bottleneck
ECS health/rebuild/throttle event invalidates the interval
distributed client count or return code mismatch
```

For timed async runs, endpoint completion counts can straddle the reporting
boundary. Explain a small difference; use fixed records when exact parity is
required.

## 10. Required report

Every agent-produced result must include:

```text
Objective and acceptance criteria:
SBK commit/version and JDK:
Sanitized exact command/YML:
Load-generator topology and telemetry:
ECS/ObjectScale version/topology/health:
Endpoint mode (VIP vs explicit nodes), namespace, bucket/prefix:
Object-size and operation distributions:
Data shape, checksum, SSE, versioning:
Workers, async limits, multipart settings:
Warm-up, duration/count, repetitions:
Total operations and logical bytes:
Throughput median and run-to-run variability:
Latency p50/p95/p99/p99.9:
Retries, endpoint failures, invalid/discarded latencies:
Independent state check:
Conclusion, bottleneck evidence, and limitations:
Raw result and manifest locations:
Cleanup owner/status:
```

Never call a short connectivity run a cluster benchmark. Never generalize the
example numbers in the operator runbook to another ECS system.

## 11. Documentation/build changes

When an agent changes the MinIO implementation or options, it must update:

1. generated `-help` text in `MinIO.addArgs()`;
2. `minio.properties` defaults/comments;
3. [driver README](../drivers/minio/README.md);
4. [implementation guide](MINIO_DRIVER_IMPLEMENTATION.md);
5. this runbook or the operator runbook when workflows change;
6. unit tests and real-backend verification evidence.

Minimum verification:

```bash
./gradlew :drivers:minio:check :installDist
./build/install/sbk/bin/sbk -class minio -help
git diff --check
```

Follow the full repository definition of done in `AGENTS.md` for source or
build-logic changes.

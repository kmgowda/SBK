<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# DRIVER_SPECIFICATION.md — Spec-driven development for new SBK drivers

> **Purpose.** This file gives you (a human contributor or an AI coding
> agent driven by a human) a **fillable template** to specify a new SBK
> driver before any code is written. Once the spec is complete, the
> agent can turn it into working code by following
> <ref_file file="/root/projects/SBK/docs/AGENT_RECIPES.md" />
> §1 ("Add a new storage driver").
>
> **The spec is the contract.** Anything the spec says, the code must
> implement. Anything the spec is silent on, the code uses sensible
> defaults from the existing driver patterns.
>
> Before reading further, agents should have read
> <ref_file file="/root/projects/SBK/AGENTS.md" /> and the
> "Add a new storage driver" recipe.

---

## Table of contents

1. [How to use this template](#1-how-to-use-this-template)
2. [The spec template (fillable)](#2-the-spec-template-fillable)
3. [Worked example — the MinIO/S3 driver spec](#3-worked-example--the-miniosthree-driver-spec)
4. [Acceptance checklist](#4-acceptance-checklist)

---

## 1. How to use this template

### 1.1 Two workflows

**Workflow A — "Vibe coding"** (you know what you want; iterate fast):

1. Copy §2 into your scratch directory or a new branch.
2. Fill in the minimum needed: driver name, vendor SDK, one or two
   key config options.
3. Hand to an AI agent with the prompt: *"Implement this driver
   following AGENT_RECIPES.md §1."*
4. Iterate.

**Workflow B — "Spec-driven"** (formal; auditable; multi-person):

1. Copy §2 into `docs/specs/<drivername>.spec.md` (or similar; the
   path is up to your team).
2. Fill the **whole** template, including acceptance criteria and
   test plan.
3. Review with the team / commit to the branch.
4. Hand to an agent: *"Implement the spec in
   docs/specs/<drivername>.spec.md."*
5. Agent generates code, tests, and driver-level README.
6. Verification runs (Recipes §1.4 + §8) and the acceptance checklist
   (§4 below) gate the merge.
7. The spec stays in the repo as the source of truth for the feature.

### 1.2 Template conventions

- **Bold field names** are required.
- *Italic field names* are optional; agents will use sensible defaults
  if you leave them blank.
- `code-style` field values are taken verbatim into code or config.
- Sections with `→` show explicit cross-references to the implementing
  Java file the agent will produce.

---

## 2. The spec template (fillable)

> Copy everything inside the fenced block to a new file and fill it
> in. Anything in `<…>` is a placeholder.

```markdown
# Driver spec — <Driver Display Name>

## 0. Metadata

- **Driver short name (lowercase, no spaces)**: <e.g., `acmekv`>
- **Java class/package name (PascalCase)**: <e.g., `AcmeKv`>
- **Storage type**: <one of: object store / key-value / message queue /
  file system / database / in-memory cache / other>
- **Vendor SDK Maven coordinates**: `<group>:<artifact>:<version>`
- *SDK license*: <Apache-2.0 / MIT / proprietary / …>
- *Public link to SDK*: <URL>
- **Author / sponsor**: <name / team>
- **Spec status**: <draft | review | approved | implemented>

## 1. Problem statement

> 2–4 sentences. Why does this driver need to exist? What can a user
> do with SBK + this driver that they cannot do today?

## 2. Functional requirements

The driver MUST:

- [ ] Implement the seven `Storage<T>` SPI methods (see
      `sbk-api/src/main/java/io/sbk/api/Storage.java`).
- [ ] Support **single-threaded writes** correctly (no race in
      `createWriter`).
- [ ] Support **single-threaded reads** correctly (no race in
      `createReader`).
- [ ] Support **N concurrent writers** with `-writers N` (>= 1).
- [ ] Support **N concurrent readers** with `-readers N` (>= 1).
- [ ] Pass `./gradlew :drivers:<name>:check` (compile + checkstyle).
- [ ] Pass `./gradlew installDist` and show up under
      `sbk -help`.
- [ ] Run a 60-second smoke benchmark against a live target and emit
      records/sec, MB/sec, and 17+ latency percentiles.

The driver SHOULD (if applicable to the storage type):

- [ ] Support `-recreate true` to drop and recreate
      bucket/topic/table on `openStorage`.
- [ ] Support `-insecure true` for TLS endpoints with self-signed
      certs.
- [ ] Emit one-line, human-readable diagnostic on SDK errors
      (HTTP status + body for HTTP-based; error code + message for
      others). See the `explain()` helper pattern in
      `drivers/minio/src/main/java/io/sbk/driver/MinIO/MinIO.java`.
- [ ] Survive shutdown: catch `InterruptedIOException` and
      `RejectedExecutionException` and treat them as clean stop, not
      errors.

The driver MUST NOT:

- [ ] Take a `synchronized` lock or `ReentrantLock` on the hot path
      (the `writeAsync` / `read` per-record path).
- [ ] Re-time the operation with `System.nanoTime()` — the harness
      already does this around `writeAsync` / `read`.
- [ ] Allocate large buffers per record (one `ByteArrayInputStream`
      or equivalent is fine; a `new HashMap<>()` per record is not).
- [ ] Bring in a vendor SDK whose top-level package is not added to
      `checkstyle/import-control.xml`.

## 3. Configuration parameters

> Define every CLI flag the driver will accept. Each row corresponds
> to one row in `addArgs(...)` and one field in `<Name>Config.java`.

| CLI flag (`-flag`) | Java field name | Type | Default | Help text | Required? |
|---|---|---|---|---|---|
| `host` | `host` | String | `<…>` | `Storage host name or IP` | Yes |
| `port` | `port` | int | `<…>` | `Service port` | No |
| `key` | `accessKey` | String | `<…>` | `Access key` | <Yes/No> |
| `secret` | `secretKey` | String | `<…>` | `Secret key` | <Yes/No> |
| `bucket` | `bucket` | String | `<…>` | `Bucket / table / topic name` | <Yes/No> |
| `recreate` | `reCreate` | boolean | `false` | `Recreate bucket if present` | No |
| `insecure` | `insecure` | boolean | `false` | `Skip TLS validation` | No |
| `<more>` | `<…>` | `<…>` | `<…>` | `<…>` | `<…>` |

> Same table as a properties file (`<Name>.properties`) — the agent
> will generate this verbatim:

```properties
host=<default>
port=<default>
accessKey=<default>
secretKey=<default>
bucket=<default>
reCreate=false
insecure=false
```

## 4. Behaviour specification

### 4.1 `addArgs(InputOptions params)` — declare CLI flags
> The agent will read this section and emit one `params.addOption(...)`
> call per row of §3. **No additional logic in `addArgs`** unless
> declared here.

### 4.2 `parseArgs(ParameterOptions params)` — read CLI flags
> Same: one `config.X = params.getOptionValue(...)` per row of §3.
> List any **validation** the driver must do here (e.g. "fail if
> `port` is outside [1..65535]").

- Validation rules:
  - <e.g., "if both writers and readers are specified, force `recreate=true`">
  - <e.g., "if `port` is 0, default to 9000">
  - <…>

### 4.3 `openStorage(ParameterOptions params)` — connect / preflight

What the driver does at `open`:

1. Build the vendor client. *Sketch the client builder here.*
   ```
   client = AcmeClient.builder()
       .host(config.host).port(config.port)
       .credentials(config.accessKey, config.secretKey)
       .build();
   ```
2. <e.g., "Check if bucket exists. If yes and `reCreate`, delete and
   recreate. If no and writers > 0, create.">
3. <…>

### 4.4 `closeStorage(ParameterOptions params)` — disconnect

- Close the vendor client.
- Other cleanup: <…>

### 4.5 `createWriter(int id, ParameterOptions params)`

- Returns: `new <Name>Writer(id, client, config)`.
- The writer's `writeAsync(byte[] data)` does:
  > Describe the **one vendor API call** that uploads the record.
  > Example:
  > ```
  > client.put(generateKey(), data);
  > return null;     // synchronous; harness handles timing
  > ```
- Object/key naming: <e.g., "bucketName + '-' + UUID.randomUUID()">

### 4.6 `createReader(int id, ParameterOptions params)`

- Returns: `new <Name>Reader(id, client, config)`.
- The reader's `read()` does:
  > Describe the **one vendor API call** that fetches a record.
  > Example:
  > ```
  > return client.get(nextKey());
  > ```
- Iteration model: <e.g., "list bucket, iterate items, fetch each">

### 4.7 Error handling

For each kind of vendor exception, specify:

| Exception | Translates to | Notes |
|---|---|---|
| `IOException` (network) | re-throw as `IOException` | |
| `AcmeAuthException` | `IOException("auth failed: " + e.getMessage())` | One-line diagnostic |
| `AcmeQuotaException` | `IOException("quota exceeded for bucket " + bucket)` | |
| `InterruptedIOException` | swallow; treat as clean shutdown | See §2 MUST clause |
| `RejectedExecutionException` | swallow; treat as clean shutdown | |

### 4.8 Data type

- **Payload type**: `byte[]` (default) or `<other>`.
- If `<other>`, override `getDataType()` and provide the `DataType<T>`
  implementation. See
  `sbk-api/src/main/java/io/sbk/data/impl/ByteArray.java` for the
  reference.

## 5. Test plan

### 5.1 Local mock target (optional but encouraged)

If the vendor publishes a Docker image for local testing, document
how to start it:

```bash
docker run -d --name acme1 -p 1234:1234 acme/acme-kv:latest
```

### 5.2 Smoke tests

| # | Command | Expected outcome |
|---|---|---|
| 1 | `sbk -class <name> -host localhost -writers 1 -size 1024 -seconds 15` | 15s clean run; non-zero records/sec; latency percentiles printed |
| 2 | `sbk -class <name> -host localhost -readers 1 -size 1024 -seconds 15` | Same, for reads |
| 3 | `sbk -class <name> -host localhost -writers 4 -readers 4 -size 1024 -seconds 15` | Both writer and reader stats printed |
| 4 | <add more if the driver has special modes> | <…> |

### 5.3 Failure-mode tests (manual, not CI)

- Network unreachable → driver should fail at `openStorage` with a
  clear `IOException`, not a stack trace mid-benchmark.
- Wrong credentials → same: fail at `openStorage`.
- Endpoint up but not running the service → reasonable error message
  (see §4.7).

## 6. Acceptance criteria

> The PR merging the driver is accepted only if **all** boxes are
> checked. The agent should report which boxes pass after running its
> verification script.

- [ ] `./gradlew :drivers:<name>:check` exits 0.
- [ ] `./gradlew check` exits 0 (no other module regresses).
- [ ] `./gradlew installDist` produces a working `sbk` script.
- [ ] `sbk -class <name> -help` lists every flag in §3.
- [ ] All §5.2 smoke tests pass against a live target.
- [ ] `drivers/<name>/README.md` exists, contains:
      - one write example command,
      - one read example command,
      - a list of all CLI flags from §3.
- [ ] If new top-level dep packages were introduced, they are
      whitelisted in `checkstyle/import-control.xml`.
- [ ] `settings-drivers.gradle` and `build-drivers.gradle` both
      include the new driver.
- [ ] No new `synchronized` block or `Lock` on the per-record path
      (verify by `grep -E '\bsynchronized\b|\bLock\b' drivers/<name>/src/`).

## 7. Open questions / risks

> Anything the author wants flagged for review. Things like:
> - "The vendor SDK is only on JitPack, not Maven Central — needs a
>   `repositories { … }` block."
> - "The vendor SDK requires JNI native libraries — confirm they bundle
>   into the JAR."
> - "Auth model is OAuth, not access-key/secret — `addArgs` will need
>   more flags."

- <…>
- <…>
```

---

## 3. Worked example — the MinIO/S3 driver spec

> This is what a completed spec looks like. The agent that processes
> a spec in this form should be able to generate the `drivers/minio/`
> code that exists today, plus its tests and README, from the spec
> alone.

```markdown
# Driver spec — MinIO / S3-compatible object store

## 0. Metadata

- **Driver short name (lowercase, no spaces)**: `minio`
- **Java class/package name (PascalCase)**: `MinIO`
- **Storage type**: object store (S3 protocol)
- **Vendor SDK Maven coordinates**: `io.minio:minio:8.5.17`
- *SDK license*: Apache-2.0
- *Public link to SDK*: https://github.com/minio/minio-java
- **Author / sponsor**: KMG / SBK community
- **Spec status**: implemented (driver in tree as `drivers/minio/`)

> **SDK version note.** Pinned to 8.5.17 — *not* the latest 9.x.
> MinIO SDK 9.x adds an `x-amz-sdk-checksum-algorithm` header that
> older S3 backends (Dell ECS / ObjectScale, older Ceph RGW) reject
> with HTTP 400. The 8.5.17 pin is intentional. See
> `drivers/minio/build.gradle` for the comment.

## 1. Problem statement

S3 is the de-facto object-storage protocol. Many products implement
it (AWS S3, MinIO server, Dell ECS / ObjectScale, Ceph RGW, SeaweedFS,
Wasabi, Backblaze B2). Users need a single SBK driver that benchmarks
all of them with one set of flags. Differences between backends (port
numbers, custom HTTP headers, signature versions) should be exposed as
explicit configuration rather than hidden behind backend-specific code.

## 2. Functional requirements

All "MUST" / "SHOULD" / "MUST NOT" items from the template apply.

Driver-specific additions:

- MUST support multipart upload via `-part-size <bytes>`.
- MUST support S3 checksum validation via `-checksum {crc32|crc32c|
  sha1|sha256|crc64nvme}`.
- MUST support object tagging via `-tagging-enabled` + `-tagging-tags`.
- MUST support bucket versioning via `-versioning-enabled`.
- MUST support arbitrary extra HTTP headers via `-extra-headers
  "k=v,k=v"` (required for Dell ECS namespace routing).
- MUST support SSE-S3 encryption via `-sse-enabled`.
- MUST support configurable data-shape (compressibility, anti-dedup
  stamping) via `-data-compressibility` + `-data-dedupable`.

## 3. Configuration parameters

> Excerpt — see `drivers/minio/src/main/resources/minio.properties`
> for the full default set.

| CLI flag | Java field | Type | Default | Help text |
|---|---|---|---|---|
| `url` | `url` | String | `https://play.min.io` | S3 endpoint URL |
| `bucket` | `bucketName` | String | `sbk` | Bucket name |
| `key` | `accessKey` | String | (play.min.io sandbox) | Access key |
| `secret` | `secretKey` | String | (play.min.io sandbox) | Secret key |
| `region` | `region` | String | `""` (driver defaults to `us-east-1` to skip GetBucketLocation) | AWS region for SigV4 |
| `recreate` | `reCreate` | boolean | `false` | Recreate bucket if present |
| `insecure` | `insecure` | boolean | `true` | Skip TLS validation |
| `part-size` | `partSize` | long | `0` | Multipart part size in bytes (0=disabled, min 5 MiB) |
| `checksum` | `checksumAlgorithm` | String | `""` | Algorithm for `x-amz-checksum-*` |
| `tagging-enabled` | `taggingEnabled` | boolean | `false` | Enable object tagging |
| `tagging-tags` | `taggingTags` | String | `""` | CSV `k=v,k=v` tags |
| `versioning-enabled` | `versioningEnabled` | boolean | `false` | Enable bucket versioning |
| `data-compressibility` | `dataCompressibility` | int | `0` | 0..100, target compressibility |
| `data-dedupable` | `dataDedupable` | boolean | `true` | `false` = anti-dedup stamping |
| `sse-enabled` | `sseEnabled` | boolean | `false` | Enable SSE-S3 |
| `extra-headers` | `extraHeaders` | String | `""` | CSV `k=v,k=v` HTTP headers added to every request |
| `connect-timeout-ms` | `connectTimeoutMs` | long | `0` | HTTP connect timeout |

## 4. Behaviour specification

### 4.1 `addArgs`

One `params.addOption(...)` per row of §3. No additional logic.

### 4.2 `parseArgs`

Per row of §3, plus:

- If `partSize > 0`, validate it's in `[5 MiB, 5 GiB]`.
- If `checksumAlgorithm` is non-empty, call `S3ChecksumUtil.Algorithm.fromString(...)` to validate.
- If both writers > 0 and readers > 0, force `reCreate = true`.

### 4.3 `openStorage`

1. Build `MinioClient.builder()` with `endpoint`, `credentials`,
   `region` (defaults to `us-east-1` if unset — skips the broken
   `GetBucketLocation` round-trip on non-AWS backends).
2. If timeouts are non-zero or `extraHeaders` is non-empty, build a
   custom `OkHttpClient` with a `HeaderInjector` interceptor and
   pass via `httpClient(client, true)`.
3. If `insecure`, call `mclient.ignoreCertCheck()`.
4. Print a feature banner showing which advanced features are enabled.
5. Check `bucketExists(...)`. If yes and `reCreate`, empty and remove
   the bucket. If no and writers > 0, `makeBucket(...)`.
6. If `versioningEnabled`, call `setBucketVersioning(...)`.

### 4.4 `closeStorage`

`mclient.close()`.

### 4.5 `createWriter` / writer's `writeAsync`

- Generate object data via `S3DataGenerator` (applies compressibility
  + anti-dedup as configured).
- If `checksumAlgorithm` set, compute Base64 digest via
  `S3ChecksumUtil` and attach `x-amz-checksum-*` header.
- Call `client.putObject(PutObjectArgs.builder()...)`.
- If `taggingEnabled`, follow with `client.setObjectTags(...)`.
- Object key: `S3ObjectKey.next()` (prefix + optional fsAccess hash
  tree + bucket name + UUID).

### 4.6 `createReader` / reader's `recordRead`

- `client.listObjects(...)` with `recursive(true)` and
  `includeVersions(versioningEnabled)`.
- For each item: `client.statObject(...)` then `client.getObject(...)`.
- Use the worker's `t.endTime` reuse pattern; do not call the clock
  inside the loop.

### 4.7 Error handling

| Exception | Action |
|---|---|
| `InvalidResponseException` | Surface HTTP status, content-type, body; add the hint *"endpoint is likely not an S3 service"* |
| `ErrorResponseException` | Surface S3 error code + HTTP status |
| `InterruptedIOException` | Treat as clean shutdown — return without rethrowing |
| `RejectedExecutionException` | Treat as clean shutdown |

See the `explain(Exception)` helper pattern in the existing
`drivers/minio/src/main/java/io/sbk/driver/MinIO/MinIO.java`.

### 4.8 Data type

`byte[]` (default).

## 5. Test plan

### 5.1 Local mock target

```bash
docker run -d --name minio1 -p 9000:9000 -p 9001:9001 \
   -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin \
   minio/minio server /data --console-address ":9001"
```

### 5.2 Smoke tests

1. `sbk -class minio -writers 1 -size 100 -seconds 30` against
   `play.min.io` with bundled credentials.
2. Same with `-writers 8 -size 1048576 -seconds 60`.
3. Local Docker write: `sbk -class minio -url http://127.0.0.1:9000
   -key minioadmin -secret minioadmin -bucket sbk -recreate true
   -writers 4 -size 1048576 -seconds 60`.
4. Dell ObjectScale write: `sbk -class minio
   -url https://<host>:9021 -extra-headers x-emc-namespace=<ns>
   -key <user> -secret <pass> -bucket bench -writers 1 -seconds 30`.
5. Each `-checksum {crc32,crc32c,sha1,sha256,crc64nvme}` exercised
   in a 15-s run.

### 5.3 Failure-mode tests

- Wrong port (e.g., `:443` against ObjectScale) → driver should print
  the *"endpoint is likely not an S3 service"* hint with HTTP status
  and body.
- Wrong credentials → `ErrorResponseException AccessDenied` surfaces
  as one diagnostic line.

## 6. Acceptance criteria

All boxes from the template are checked.

## 7. Open questions / risks

- The MinIO SDK 9.x compatibility issue with older S3 backends (see
  Metadata note). Pin is documented and intentional.
- `-auth-version 2` (SigV2) is accepted but logs a warning — the
  MinIO SDK is SigV4-only. No code path for SigV2; the flag is for
  forward compatibility.
- `-mpu-concurrent-parts` is accepted but info-only on SDK 8.5.x.
  Multipart parallelism is internal to the SDK.
```

---

## 4. Acceptance checklist

The agent should produce the following table at the end of any
driver-implementation work and present it to the user.

| Check | Status |
|---|---|
| `./gradlew :drivers:<name>:check` passes | ☐ |
| `./gradlew check` passes (no regression elsewhere) | ☐ |
| `./gradlew installDist` produces a working binary | ☐ |
| `sbk -class <name> -help` lists every spec'd flag | ☐ |
| Every smoke test in §5.2 of the spec succeeds | ☐ |
| Failure-mode tests (§5.3) produce one-line diagnostics, not stack traces | ☐ |
| `drivers/<name>/README.md` exists and is filled in | ☐ |
| New top-level dep packages whitelisted in `checkstyle/import-control.xml` | ☐ |
| `settings-drivers.gradle` and `build-drivers.gradle` include the new driver | ☐ |
| No `synchronized` block or `Lock` on the per-record path | ☐ |
| No new `System.nanoTime()` call in the per-record path | ☐ |
| `InterruptedIOException` / `RejectedExecutionException` handled as clean shutdown | ☐ |

Only when every box is ticked can the work be considered "spec-complete".

---

## Appendix — Naming conventions in one place

| Concept | Pattern | Example |
|---|---|---|
| Driver short name (in CLI and dir name) | lowercase, no separators | `acmekv`, `minio`, `kafka` |
| Driver Java class | PascalCase, may include digits | `AcmeKv`, `MinIO`, `Kafka` |
| Driver Java package | `io.sbk.driver.<ClassName>` | `io.sbk.driver.AcmeKv` |
| Driver properties file | `<ClassName>.properties` | `AcmeKv.properties`, `minio.properties` |
| Writer / Reader / Config classes | `<ClassName>Writer/Reader/Config` | `AcmeKvWriter`, `MinIOConfig` |
| README path | `drivers/<short-name>/README.md` | `drivers/acmekv/README.md` |

The short name and the class name **don't have to match in case** —
that is by design (e.g. `minio` short / `MinIO` class). The class
discovery is case-insensitive.

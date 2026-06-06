<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# AGENT_RECIPES.md — Step-by-step task playbooks

> **Audience.** AI coding agents and human contributors who need
> concrete, copy-pasteable procedures for common tasks in this
> repository. Each recipe lists the **exact files to touch**, the
> **exact commands to run**, and **what success looks like**.
>
> Read <ref_file file="/root/projects/SBK/AGENTS.md" /> first for
> repo-wide conventions and gotchas. This document assumes you have.

---

## Index

1. [Add a new storage driver](#1-add-a-new-storage-driver)
2. [Modify an existing driver (add a CLI flag, fix a bug)](#2-modify-an-existing-driver)
3. [Add a new logger / metrics exporter](#3-add-a-new-logger--metrics-exporter)
4. [Add a new CLI flag at the harness level](#4-add-a-new-cli-flag-at-the-harness-level)
5. [Debug a driver that fails at runtime](#5-debug-a-driver-that-fails-at-runtime)
6. [Update or extend the architecture documentation](#6-update-or-extend-the-architecture-documentation)
7. [Bump a driver's vendor SDK version](#7-bump-a-drivers-vendor-sdk-version)
8. [Run a benchmark against a new cluster](#8-run-a-benchmark-against-a-new-cluster)

---

## 1. Add a new storage driver

**Goal:** Add a new entry to SBK so `sbk -class <newdriver> ...` works.

### 1.1 Prerequisites

Before you start, confirm:

- The storage system has a **Java client library** (or at least a usable
  HTTP/TCP protocol). SBK is Java-only.
- The library is on **Maven Central** (or another public repo) — adding
  a vendor SDK from a private repo requires extra build-file changes.
- You have read <ref_file file="/root/projects/SBK/drivers/sbktemplate/" />
  — it is the official starting scaffold.

### 1.2 Files to create

Pick a driver name, e.g. `acmekv`. The convention is:

- Directory name: lowercase (`acmekv`)
- Java class/package name: PascalCase, matches the directory case
  (`AcmeKv` → `io.sbk.driver.AcmeKv`)
- Properties file: matches the class name in title case
  (`AcmeKv.properties`) — see how MinIO does it
  (`minio.properties` because the class is `MinIO`).

```
drivers/acmekv/
├── build.gradle
└── src/main/
    ├── java/io/sbk/driver/AcmeKv/
    │   ├── AcmeKv.java                # the Storage<T> impl
    │   ├── AcmeKvConfig.java          # POJO mirroring properties
    │   ├── AcmeKvWriter.java          # the Writer<T> impl
    │   └── AcmeKvReader.java          # the Reader<T> impl
    └── resources/
        └── AcmeKv.properties          # default config values
```

### 1.3 Step-by-step

**Step A — Copy the scaffold.**

```bash
cp -r drivers/sbktemplate drivers/acmekv
```

**Step B — Rename packages, classes, and the properties file.**

Use exact replacement of the case-sensitive strings:

| Find | Replace with |
|---|---|
| `sbktemplate` (in `build.gradle`, file paths) | `acmekv` |
| `SbkTemplate` (class names, package, properties filename) | `AcmeKv` |
| The `SbkTemplate.properties` file → `AcmeKv.properties` |

```bash
cd drivers/acmekv
find src -name 'SbkTemplate*' -exec rename 's/SbkTemplate/AcmeKv/' {} \;
# (or mv each file by hand if `rename` isn't installed)
find . -type f \( -name '*.java' -o -name '*.properties' \) \
    -exec sed -i 's/SbkTemplate/AcmeKv/g' {} \;
mv src/main/java/io/sbk/driver/SbkTemplate src/main/java/io/sbk/driver/AcmeKv
```

**Step C — Edit `drivers/acmekv/build.gradle`** to add the vendor SDK
dependency:

```groovy
plugins {
    id 'java'
}
repositories {
    mavenCentral()
}
dependencies {
    api project(":sbk-api")
    api 'com.acme:acme-kv-java:1.2.3'   // <-- your vendor SDK
}
```

**Step D — Register the driver in the build system.** Edit:

- <ref_file file="/root/projects/SBK/settings-drivers.gradle" /> — add:
  ```groovy
  include 'drivers:acmekv'
  ```
- <ref_file file="/root/projects/SBK/build-drivers.gradle" /> — add:
  ```groovy
  api project(':drivers:acmekv')
  ```

**Step E — Update `checkstyle/import-control.xml`** if the SDK pulls in
a new top-level package. Look at the SDK's `pom.xml` to find its package
prefix (e.g., `com.acme`), then add to
<ref_file file="/root/projects/SBK/checkstyle/import-control.xml" />:

```xml
<allow pkg="com.acme" />
```

**Step F — Implement the four classes.** The contract is in
<ref_file file="/root/projects/SBK/sbk-api/src/main/java/io/sbk/api/Storage.java" />.
See <ref_file file="/root/projects/SBK/docs/DRIVER_SPECIFICATION.md" />
for a fillable spec and a worked example.

Minimal implementation sketch (replace with real SDK calls):

```java
// AcmeKv.java
public class AcmeKv implements Storage<byte[]> {
    private static final String CONFIGFILE = "AcmeKv.properties";
    private AcmeKvConfig config;
    private AcmeClient client;

    public void addArgs(InputOptions params) {
        config = loadConfigFromProperties(CONFIGFILE);
        params.addOption("host", true, "AcmeKV host (default: " + config.host + ")");
        params.addOption("port", true, "AcmeKV port (default: " + config.port + ")");
    }

    public void parseArgs(ParameterOptions params) {
        config.host = params.getOptionValue("host", config.host);
        config.port = Integer.parseInt(params.getOptionValue("port", String.valueOf(config.port)));
    }

    public void openStorage(ParameterOptions params) throws IOException {
        client = AcmeClient.connect(config.host, config.port);
    }

    public void closeStorage(ParameterOptions params) throws IOException {
        if (client != null) client.close();
    }

    public DataWriter<byte[]> createWriter(int id, ParameterOptions params) {
        return new AcmeKvWriter(id, client, config);
    }

    public DataReader<byte[]> createReader(int id, ParameterOptions params) {
        return new AcmeKvReader(id, client, config);
    }
}
```

The writer's minimum surface is `writeAsync(byte[] data)` — return
`null` for sync completion or a `CompletableFuture` for async. The
harness handles timing.

```java
// AcmeKvWriter.java
public class AcmeKvWriter implements Writer<byte[]> {
    private final AcmeClient client;
    private final AcmeKvConfig config;
    private final int id;

    public AcmeKvWriter(int id, AcmeClient client, AcmeKvConfig config) {
        this.id = id;
        this.client = client;
        this.config = config;
    }

    @Override
    public CompletableFuture<?> writeAsync(byte[] data) throws IOException {
        client.put(generateKey(), data);   // your vendor call
        return null;                       // synchronous; harness times the call
    }

    @Override
    public void close() throws IOException { /* no per-writer resources */ }
}
```

The reader's minimum surface is `read()` — return one record per call.
See <ref_file file="/root/projects/SBK/drivers/file/" /> for a complete
simple example, or
<ref_file file="/root/projects/SBK/drivers/minio/" /> for an example
with rich configuration.

**Step G — Add the README.** Every driver has its own `README.md` with:

- A short description of the storage system.
- The CLI command(s) for write and read benchmarks.
- Any vendor-specific configuration (auth, ports, etc.).

Use <ref_file file="/root/projects/SBK/drivers/minio/README.md" /> as the
gold-standard template.

### 1.4 Verify

```bash
# 1. Compile the driver alone (fastest)
./gradlew :drivers:acmekv:compileJava

# 2. Run checkstyle + tests on the driver
./gradlew :drivers:acmekv:check

# 3. Full project — confirms wiring into settings-drivers.gradle worked
./gradlew check

# 4. Build the distribution and confirm the driver is loadable
./gradlew installDist
./build/install/sbk/bin/sbk -help 2>&1 | grep -i acmekv
# (expected: "acmekv" appears in the storage driver list)

# 5. Smoke-test against a real backend
./build/install/sbk/bin/sbk -class acmekv -host my-cluster -writers 1 -size 1024 -seconds 30
```

### 1.5 Success criteria

- [ ] `./gradlew check` passes.
- [ ] `./gradlew installDist` produces a working `sbk` script.
- [ ] `sbk -help` lists the new driver.
- [ ] A benchmark against a real or mock backend reports records/sec
      and latency percentiles for at least 30 seconds without errors.
- [ ] A driver-level `README.md` exists and shows at least one write
      and one read command.

---

## 2. Modify an existing driver

**Goal:** Add a CLI flag, fix a bug, or change behaviour in an existing
driver.

### 2.1 Files to touch

For driver `<name>`:

- `drivers/<name>/src/main/java/io/sbk/driver/<Name>/<Name>.java`
  — the driver class. **Add the flag** in `addArgs()`, **read it** in
  `parseArgs()`, **act on it** in `openStorage()` or in
  `createWriter()`/`createReader()`.
- `drivers/<name>/src/main/java/io/sbk/driver/<Name>/<Name>Config.java`
  — add the field with a sensible default.
- `drivers/<name>/src/main/resources/<name>.properties` — add the
  default value (matches the field name in Config — Jackson maps them
  case-sensitively).
- `drivers/<name>/README.md` — document the new flag with at least one
  example.

### 2.2 Pattern for "add a new flag"

```java
// In <Name>.java addArgs(...)
params.addOption("my-flag", true, "Description (default: " + config.myFlag + ")");

// In <Name>.java parseArgs(...)
config.myFlag = params.getOptionValue("my-flag", config.myFlag);
// or for non-strings:
config.myInt = Integer.parseInt(params.getOptionValue("my-int", String.valueOf(config.myInt)));
config.myBool = Boolean.parseBoolean(params.getOptionValue("my-bool", String.valueOf(config.myBool)));
```

Add to `<Name>Config.java`:

```java
public String myFlag;
public int myInt;
public boolean myBool;
```

Add to `<name>.properties`:

```properties
myFlag=default-value
myInt=42
myBool=false
```

### 2.3 Verify

```bash
./gradlew :drivers:<name>:check
./gradlew :drivers:<name>:installDist   # (or full installDist)
./build/install/sbk/bin/sbk -class <name> -help    # check flag appears
./build/install/sbk/bin/sbk -class <name> -my-flag value ...    # test it
```

---

## 3. Add a new logger / metrics exporter

**Goal:** Send SBK's periodic latency/throughput numbers to a new
destination (InfluxDB, OpenTelemetry, Datadog, your custom collector).

### 3.1 Files to create

```
sbk-api/src/main/java/io/sbk/logger/impl/<Name>Logger.java
```

That is the **only** place to add the code — the harness picks it up
by package scan.

### 3.2 Implementation

Extend `AbstractRWLogger` and override the methods that matter for
your destination. The minimal surface is:

```java
package io.sbk.logger.impl;

public class InfluxLogger extends AbstractRWLogger {
    private InfluxDBClient influx;

    @Override
    public void open(InputParameterOptions params, String storageName,
                     Action action, Time time) throws IOException {
        super.open(params, storageName, action, time);
        influx = InfluxDBClient.connect(...);
    }

    @Override
    public void close(ParameterOptions params) throws IOException {
        influx.close();
        super.close(params);
    }

    @Override
    public void printPeriodic(int writers, int readers,
                              long records, double recsPerSec, double mbPerSec,
                              double avgLatency, long minLatency, long maxLatency,
                              long invalidLatencies, long lowerDiscard, long higherDiscard,
                              int slc1, int slc2, long[] percentileValues, ...) {
        // ship to InfluxDB
    }

    @Override
    public void printTotal(...) {
        // final summary
    }
}
```

`AbstractRWLogger` already supplies sensible defaults for everything
else. Look at
<ref_file file="/root/projects/SBK/sbk-api/src/main/java/io/sbk/logger/impl/CSVLogger.java" />
for a concrete reference of how much you need to override.

### 3.3 Verify

```bash
./gradlew :sbk-api:check
./gradlew installDist

# Run any benchmark with the new logger:
./build/install/sbk/bin/sbk -class file -file /tmp/sbk.bin \
   -out InfluxLogger -writers 1 -size 1024 -seconds 30

# Confirm: the logger appears in -help output
./build/install/sbk/bin/sbk -class file -help 2>&1 | grep -i influxlogger
```

### 3.4 Common pitfalls

- The class **must** end in `Logger` and live in `io.sbk.logger.impl.`
  Otherwise the package scanner won't find it.
- If you add a new vendor SDK dependency to `sbk-api/build.gradle`,
  remember to add the package to
  <ref_file file="/root/projects/SBK/checkstyle/import-control.xml" />.

---

## 4. Add a new CLI flag at the harness level

**Goal:** Add a flag that all drivers can see (not driver-specific).

### 4.1 Files to touch

- <ref_file file="/root/projects/SBK/sbk-api/src/main/java/io/sbk/params/impl/SbkParameters.java" />
  — declare the option and parse it.
- <ref_file file="/root/projects/SBK/sbk-api/src/main/java/io/sbk/params/ParameterOptions.java" />
  — add the getter to the public interface so drivers can read it.

### 4.2 Pattern

In `SbkParameters` constructor (or wherever options are declared):

```java
addOption("my-flag", true, "Description, default: " + defaultValue);
```

In `SbkParameters.parseArgs(...)`:

```java
this.myFlag = getOptionValue("my-flag", String.valueOf(defaultValue));
```

In the `ParameterOptions` interface:

```java
String getMyFlag();
```

### 4.3 Verify

```bash
./gradlew :sbk-api:check
./gradlew installDist
./build/install/sbk/bin/sbk -class file -help 2>&1 | grep my-flag
```

---

## 5. Debug a driver that fails at runtime

This is a checklist of diagnostic steps in order of likelihood. Run
through them top-to-bottom.

### 5.1 Does `sbk -help` even list the driver?

```bash
./build/install/sbk/bin/sbk -class <name> -help 2>&1 | head -20
```

- **No.** The driver is not in the distribution. Check:
  - Was it included in <ref_file file="/root/projects/SBK/settings-drivers.gradle" /> and
    <ref_file file="/root/projects/SBK/build-drivers.gradle" />?
  - Did you actually run `./gradlew installDist` after the change?
  - Did the pathing JAR get rebuilt? Run
    `rm -rf build && ./gradlew clean :pathingJar installDist --rerun-tasks`.

### 5.2 Does the JVM find all classes?

If you see `NoClassDefFoundError` at startup:

```bash
ls build/install/sbk/lib/ | grep <expected-jar>
```

If the jar is in `lib/` but not in the pathing manifest, it's the
stale-pathing-jar bug (see <ref_file file="/root/projects/SBK/AGENTS.md" />
§4.4). Fix with:

```bash
rm -rf build && ./gradlew clean :pathingJar installDist --rerun-tasks
```

### 5.3 Is the endpoint actually the storage protocol?

For network-based drivers (S3, Kafka, Cassandra), confirm the endpoint
talks the protocol you expect. For S3:

```bash
curl -sk -X GET "https://<host>:<port>/" | head -c 300
```

- **XML body** (e.g. `<ListAllMyBucketsResult>`, `<Error>...</Error>`)
  → S3 service confirmed.
- **HTML body** or JSON like `{"detail":"Method Not Allowed"}` → wrong
  port (e.g., management UI instead of S3 data plane). Try the
  vendor's default S3 port — see
  <ref_file file="/root/projects/SBK/drivers/minio/README.md" />
  "Default S3 ports" table.

### 5.4 Is it auth, or is it the request?

Look at the **HTTP status + response body** the SDK reports:

| Status | Likely cause | Action |
|---|---|---|
| `400 InvalidRequest` with `x-amz-sdk-checksum-algorithm` | MinIO SDK 9.x vs older S3 backend | Pin to SDK 8.5.17 (see <ref_file file="/root/projects/SBK/AGENTS.md" /> §4.3) |
| `403 AccessDenied` | Permissions on the bucket, or missing namespace header (Dell ECS) | Check `-extra-headers x-emc-namespace=...` for ECS; for AWS, check IAM policy |
| `403 SignatureDoesNotMatch` | Clock skew, wrong secret key, wrong region | Verify `-region`; check NTP; re-generate secret key |
| `404 NoSuchBucket` | Bucket doesn't exist or wrong endpoint | `-recreate true` to create it, or pre-create via vendor UI |
| `non-XML response` | Driver is talking to a non-S3 service | Re-check port (§5.3) |

### 5.5 Use the driver's error explainer

The MinIO driver has a helper
(<ref_file file="/root/projects/SBK/drivers/minio/src/main/java/io/sbk/driver/MinIO/MinIO.java" />
`explain(Exception)`) that pretty-prints HTTP status, content-type, and
body for SDK exceptions. If a similar driver lacks this, add it — it
turns multi-page stack traces into one diagnostic line. Pattern:

```java
private static String explain(Exception e) {
    if (e instanceof ErrorResponseException ere) {
        return "S3 error " + ere.errorResponse().code()
                + " (HTTP " + ere.response().code() + "): "
                + ere.errorResponse().message();
    }
    return e.getClass().getSimpleName() + ": " + e.getMessage();
}
```

---

## 6. Update or extend the architecture documentation

**Goal:** Edit
<ref_file file="/root/projects/SBK/docs/sbk-internals.md" /> with new
mermaid diagrams or sections.

### 6.1 Verify mermaid syntax before committing

The doc has 22 mermaid diagrams. They must render in GitHub's mermaid
viewer **and** in `@mermaid-js/mermaid-cli` v11+. Test locally:

```bash
# 1. Install mermaid-cli (Node 18+ required)
npm install -g @mermaid-js/mermaid-cli

# 2. Extract diagrams and render each
mkdir -p /tmp/mmd-check
awk -v dir=/tmp/mmd-check '
  /^```mermaid$/ {p=1; n++; out=dir"/diag-"n".mmd"; system("rm -f " out); next}
  /^```$/ && p {p=0; close(out); next}
  p {print >> out}
' docs/sbk-internals.md

for i in /tmp/mmd-check/diag-*.mmd; do
    mmdc -i "$i" -o "${i%.mmd}.svg" 2>&1 | grep -iE "Parse error" && echo "FAIL: $i"
done
```

### 6.2 Mermaid pitfalls (in order of frequency)

| Pitfall | Symptom | Fix |
|---|---|---|
| HTML entities (`&#91;`, `&lt;`, `&amp;`) in node labels | Literal `&#91;` shown instead of `[` | Use plain ASCII inside `[" "]` quoted node labels. |
| `++` in sequence-diagram messages | `Parse error: got '+'` | Replace with words: `increment count`. |
| Em-dash `—` or arrow `→` in sequence diagrams | `Parse error: got 'INVALID'` | Use `--` and `then`. |
| Unquoted parens in participant aliases (`participant X as Some (Name)`) | `Parse error: got NEWLINE` | Quote it: `participant X as "Some (Name)"`. |
| `<` or `>` inside sequence-diagram messages | Misinterpreted as arrow | Use words: `less than`, `greater than`. |
| `<b>...</b>` HTML in subgraph titles | Layout overlaps | Remove the HTML; use plain text. |

### 6.3 Cross-references and section numbering

The document uses strict sequential numbering for sections (`## N.`)
and pillars (`#### Pillar N`). If you insert a section, **renumber all
following ones** and update the Table of Contents at the top.

---

## 7. Bump a driver's vendor SDK version

**Goal:** Upgrade `drivers/<name>/build.gradle` to a newer SDK version.

### 7.1 Check the SDK's release notes for breaking changes

Before bumping, list the API methods the driver actually uses:

```bash
grep -nE 'import (com|io|org)\.' drivers/<name>/src/main/java/**/*.java
```

…and search the SDK's changelog for those method signatures. A common
trap is an SDK constructor that changes argument types or order
between minor versions.

### 7.2 Run the compile+checkstyle cycle

```bash
./gradlew :drivers:<name>:compileJava 2>&1 | grep -E 'error:' | head -20
```

Common breakage modes:
- Constructor signature changed → adapt the call site.
- A nested class moved to top-level (e.g. `ServerSideEncryption.S3` →
  `ServerSideEncryptionS3` in MinIO 6→8) → update the import.
- A method now requires a builder pattern → migrate to the builder.

### 7.3 Run an end-to-end smoke test

A compile-clean upgrade can still break at runtime. Always:

```bash
rm -rf build && ./gradlew clean installDist --rerun-tasks
./build/install/sbk/bin/sbk -class <name> ... -writers 1 -seconds 30
```

### 7.4 Beware: MinIO SDK is intentionally pinned

If you are bumping `drivers/minio`, read <ref_file file="/root/projects/SBK/AGENTS.md" />
§4.3 first. The 8.5.17 pin is deliberate — do not undo it without
confirming the user wants the consequences for older S3 backends.

---

## 8. Run a benchmark against a new cluster

**Goal:** Validate that the driver works against a real target before
declaring an integration "done".

### 8.1 Minimal smoke-test sequence

```bash
# Build
./gradlew installDist

# Confirm the binary works at all
./build/install/sbk/bin/sbk -help | head -10

# 15-second write smoke test
./build/install/sbk/bin/sbk -class <name> -url <endpoint> \
    -writers 1 -size 1024 -seconds 15 ...

# 15-second read against the same data
./build/install/sbk/bin/sbk -class <name> -url <endpoint> \
    -readers 1 -size 1024 -seconds 15 ...
```

Expected output ends with a `Total <Driver> Writing|Reading ... records,
X.Y records/sec, A.B ms avg latency ...` line. If you see that, the
driver is alive.

### 8.2 Diagnostic ladder if smoke test fails

1. Drop concurrency to `-writers 1` — multi-thread issues often hide
   under a single-thread test.
2. Drop run time to `-seconds 5` — fail fast.
3. Add `-recreate true` for systems where the bucket/table/topic must
   be pre-created.
4. For S3-compatible: check the port (see Recipe 5.3).
5. Re-read the driver's `README.md` for vendor-specific gotchas
   (e.g. ObjectScale needs `-extra-headers x-emc-namespace=...`).
6. Use the driver's `explain()` helper or add one (Recipe 5.5) to get
   structured error output.

### 8.3 What "good" looks like

A healthy run prints periodic 5-second windows and a final total:

```
2026-06-06 ...  Bucket 'sbk' already exists
2026-06-06 ...  Writer 0 started , run seconds: 60
2026-06-06 ...  Minio Writing 1 writers, ... 17 records, 3.4 rec/s, 285 ms avg ...
2026-06-06 ...  Minio Writing 1 writers, ... 18 records, 3.6 rec/s, 290 ms avg ...
...
2026-06-06 ...  Total Minio Writing 1 writers, ... 208 records, 3.5 rec/s, 289 ms avg latency, ...
                Latency Percentiles: 283 ms 5th, ..., 285 ms 50th, ..., 304 ms 95th, ..., 320 ms 99th, ...
2026-06-06 ...  SBK Benchmark Shutdown
```

A red flag run has any of:
- A stack trace mid-run.
- A `Total ... 0 records` line (the driver never managed a successful op).
- `RejectedExecutionException` mid-run (not at shutdown) — that's a
  real bug.

---

## Appendix — Common verification commands at a glance

```bash
# Quick driver-only check
./gradlew :drivers:<name>:check

# Quick driver compile-only (fastest)
./gradlew :drivers:<name>:compileJava

# Full project check (compile + checkstyle + test on every module)
./gradlew check

# Build the launchable distribution
./gradlew installDist

# Clean rebuild (use after pathing-jar staleness, classpath changes)
rm -rf build && ./gradlew clean installDist --rerun-tasks

# List drivers visible to the launcher
./build/install/sbk/bin/sbk -help 2>&1 | head -25

# Quick remote benchmark
./build/install/sbk/bin/sbk -class <name> ... -writers 1 -size 1024 -seconds 15
```

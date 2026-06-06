<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# AGENTS.md — AI Agent Guide for the SBK Repository

> **Audience.** This file is the standard entry point for AI coding agents
> (Devin, Claude Code, Cursor, GitHub Copilot, Continue, Aider, OpenAI
> Codex, etc.) working in this repository. It tells the agent **what
> SBK is, how to build and verify it, what conventions to follow, where
> things live, and what the common gotchas are**.
>
> Humans: see <ref_file file="/root/projects/SBK/README.md" /> for the
> end-user manual, and <ref_file file="/root/projects/SBK/docs/sbk-internals.md" />
> for the internal design.

---

## 1. What this repository is

**SBK** (Storage Benchmark Kit) is a Java framework for benchmarking *any*
S3-compatible / message-queue / file-system / database storage. It runs
identical measurement code against any backend via a small pluggable
driver SPI.

- **Languages.** Java only (no Kotlin, no Scala). Build with Gradle (wrapper
  in tree: `./gradlew`). JDK 25 required.
- **Modules.** 6 core modules + ~55 storage drivers. Each module is a
  Gradle subproject.
- **License.** Apache 2.0.
- **Branch model.** Trunk-based; PRs target `master`.

### Module map (memorise this)

| Module | Role | When you edit it |
|---|---|---|
| `perl/` | **PerL** — Performance Logger library (lock-free queues, latency windows, percentile math). Heart of the framework. | Rarely. Only when changing core measurement behaviour. |
| `sbk-api/` | The benchmark harness. Defines `Storage<T>` SPI, `RWLogger` SPI, `SbkBenchmark`, `Sbk` main bootstrap. | When changing the harness, CLI flags, the SPI, or how loggers work. |
| `sbm/` | **SBM** — Storage Benchmark Monitor (gRPC aggregator on port 9717). | When changing distributed aggregation. |
| `sbk-yal/` | YML-driven launcher (single-node). | Rarely. |
| `sbk-gem/` | **SBK-GEM** — SSH-based distributed launcher. | When changing the multi-host orchestration. |
| `sbk-gem-yal/` | YML-driven SBK-GEM. | Rarely. |
| `drivers/<name>/` | One subdirectory per storage backend. **~55 driver subprojects**, each a small wrapper around a vendor SDK. | When adding or fixing a driver. **This is the most common change.** |

**For new drivers, see <ref_file file="/root/projects/SBK/docs/DRIVER_SPECIFICATION.md" />
(spec template + worked example) and
<ref_file file="/root/projects/SBK/docs/AGENT_RECIPES.md" /> ("Add a storage
driver" recipe).**

---

## 2. Build, run, and verify

### Build commands

```bash
# Default build — runs compile + checkstyle + tests on every module.
./gradlew check

# Build the launchable scripts at ./build/install/sbk/bin/sbk
./gradlew installDist

# Build a single driver (much faster while iterating)
./gradlew :drivers:minio:check
./gradlew :drivers:minio:compileJava
```

### Run a benchmark from source

```bash
# Smoke-test against the public MinIO sandbox (no credentials needed)
./build/install/sbk/bin/sbk -class minio -writers 1 -size 100 -seconds 30

# Local file-system write benchmark (1 MiB records, 8 writers, 60 s)
./build/install/sbk/bin/sbk -class file -file /tmp/sbk.bin \
   -writers 8 -size 1048576 -seconds 60

# Help / list available drivers
./build/install/sbk/bin/sbk -help
```

### Verification — what counts as "done"

A change is **done** only after all of these succeed:

1. `./gradlew :drivers:<your-driver>:check` — module-level
2. `./gradlew check` — full project
3. `./gradlew installDist` — produces a working `sbk` script
4. The end-to-end smoke test above runs without errors

> **Driver changes:** also run the driver's CLI against a real backend
> (or `play.min.io` for S3) before declaring success. Compile-clean is
> necessary but not sufficient.

---

## 3. Repository conventions

### File-system conventions

| Path | Convention |
|---|---|
| `drivers/<name>/build.gradle` | Subproject build script. Declares the driver's vendor-SDK dependency. |
| `drivers/<name>/src/main/java/io/sbk/driver/<Name>/<Name>.java` | The `Storage<T>` impl. Class name = PascalCase of the driver dir name. **Match the package case** (e.g. `MinIO` driver dir → `io.sbk.driver.MinIO` package → `MinIO.java`). |
| `drivers/<name>/src/main/java/io/sbk/driver/<Name>/<Name>Writer.java` | The `Writer<T>` impl. |
| `drivers/<name>/src/main/java/io/sbk/driver/<Name>/<Name>Reader.java` | The `Reader<T>` impl. |
| `drivers/<name>/src/main/java/io/sbk/driver/<Name>/<Name>Config.java` | POJO holding driver-specific config. Bound from the properties file by Jackson. |
| `drivers/<name>/src/main/resources/<name>.properties` | Default values for every config field. |

**Driver discovery is by simple class name, case-insensitive**:
`-class minio` resolves to `io.sbk.driver.MinIO.MinIO`. The class name
must match the file/directory name (modulo case).

### Code conventions

- **Lombok** is available (`@Synchronized`, `@SuppressFBWarnings`, etc.).
  See <ref_file file="/root/projects/SBK/lombok.config" />.
- **Checkstyle is strict.** The most common violations a new driver
  triggers:
  - Single-statement `if` blocks **must** have braces.
  - All public methods need Javadoc with `@param`, `@return`, `@throws`.
  - No unused imports.
  - Imports of new packages must be explicitly allowed in
    <ref_file file="/root/projects/SBK/checkstyle/import-control.xml" />.
    *If a new dependency brings in a new top-level package, add it there.*
- **No `synchronized` blocks or `Lock` use in the driver hot path.**
  The harness's lock-free property depends on the driver also not
  blocking. The vendor SDK is fine; your wrapper code shouldn't add
  synchronization.
- **No allocation per record beyond what the SDK forces.** The PerL
  hot path is one `TimeStamp` per record; your driver should not
  allocate big arrays or maps per `writeAsync()` call.

### Style

- Tabs vs spaces: **4 spaces**, no tabs (enforced by `FileTabCharacter`).
- Copyright header on every Java file (enforced by `RegexpHeader`).
  Copy from any existing driver.
- Default branch: `master`.
- Commit messages: imperative mood ("Add halodb driver", not "Added").

---

## 4. Known gotchas (in priority order)

### 4.1 `halodb` driver build failure

The HaloDB artifact is hosted on GitHub Packages, which routinely
exceeds its bandwidth quota. The build will fail with a 405 / 403 from
`maven.pkg.github.com` unless valid credentials are in
`~/.gradle/gradle.properties`. **For agent work, `halodb` is currently
commented out** in
<ref_file file="/root/projects/SBK/settings-drivers.gradle" /> and
<ref_file file="/root/projects/SBK/build-drivers.gradle" />.
Do not re-enable it without confirming the user wants to deal with the
GitHub Packages credentials.

### 4.2 Add a new driver to **two** gradle files

When introducing a new driver subdirectory, you must edit:

1. `settings.gradle` — *no, you don't.* The driver projects are
   listed in `settings-drivers.gradle`.
2. <ref_file file="/root/projects/SBK/settings-drivers.gradle" /> — add
   `include 'drivers:<name>'`.
3. <ref_file file="/root/projects/SBK/build-drivers.gradle" /> — add
   `api project(':drivers:<name>')` so the driver is bundled into the
   `installDist` distribution.

**Forgetting either of these is the #1 source of "I added the driver
but `-class <name>` doesn't find it" issues.**

### 4.3 MinIO SDK is pinned to 8.5.17 (not the latest)

<ref_file file="/root/projects/SBK/drivers/minio/build.gradle" /> uses
`io.minio:minio:8.5.17`. **Do not upgrade to 9.x** without testing — the
9.x SDK sends an `x-amz-sdk-checksum-algorithm` header on every
`PutObject` that older S3 backends (Dell ECS / ObjectScale, older Ceph
RGW) reject with HTTP 400 *InvalidRequest*. The comment in the
`build.gradle` explains.

### 4.4 The pathing JAR can get stale after dependency changes

The `bin/sbk` script puts only `sbk-pathing-10.0.jar` + `sbk-10.0.jar`
on the classpath; everything else (your driver's vendor SDK, transitive
deps) is reached through the pathing jar's `Class-Path:` manifest. When
you change dependencies (especially driver vendor SDK versions),
Gradle's incremental build can leave a stale pathing manifest.

**Fix:**
```bash
rm -rf build && ./gradlew clean :pathingJar installDist --rerun-tasks
```

Symptom you'd hit otherwise: `NoClassDefFoundError` on a class that is
clearly present in `build/install/sbk/lib/`.

### 4.5 Adding a new top-level dependency package requires updating
`checkstyle/import-control.xml`

The checkstyle rule `ImportControl` enforces an allow-list of top-level
packages. If your new driver pulls in a vendor SDK at a new
top-level package (e.g. `software.amazon`, `org.apache.solr`,
`okhttp3`), you must add
`<allow pkg="package.name" />` to
<ref_file file="/root/projects/SBK/checkstyle/import-control.xml" />
or `checkstyleMain` will fail.

### 4.6 Mermaid diagrams in `docs/`

If you edit mermaid diagrams in <ref_file file="/root/projects/SBK/docs/sbk-internals.md" />,
test them with `mmdc` (mermaid-cli v11+ on Node 18+). Common pitfalls:

- HTML entities like `&#91;` / `&lt;` are rendered **literally** in some
  versions. Use plain ASCII inside `[" "]` node labels.
- `++` is a reserved token in sequence-diagram messages. Use
  `increment count` instead.
- Em-dash (`—`) and Unicode arrow (`→`) inside sequence-diagram messages
  cause parse errors. Use `--` and `then`/`->`.
- `participant X as Some (Name)` with unquoted parens fails — quote the
  alias: `participant X as "Some (Name)"`.

### 4.7 SBK shutdown is asynchronous; drivers must tolerate `InterruptedIOException`

When the benchmark duration expires, the SBK framework tears down the
SDK's HTTP dispatcher mid-call. Your driver should treat
`InterruptedIOException` and `RejectedExecutionException` as **clean
shutdowns**, not errors. The MinIO driver
(<ref_file file="/root/projects/SBK/drivers/minio/src/main/java/io/sbk/driver/MinIO/MinIOWriter.java" />)
shows the pattern.

### 4.8 The harness already times your call — don't time it again

The default `Writer<T>` and `Reader<T>` interface methods record
start/end timestamps and call `perlChannel.send(...)` for you. A
driver's `writeAsync(data)` only needs to perform the operation; the
harness handles timing. **Do not** add your own `System.nanoTime()`
calls in the hot path unless you have a specific reason — and if you
do, document it in the driver's README.

---

## 5. Where to look for deeper documentation

| Topic | Read |
|---|---|
| End-user manual | <ref_file file="/root/projects/SBK/README.md" /> |
| Internal design / why is SBK fast / mermaid diagrams | <ref_file file="/root/projects/SBK/docs/sbk-internals.md" /> |
| Step-by-step recipes (add a driver, add a logger, debug failures) | <ref_file file="/root/projects/SBK/docs/AGENT_RECIPES.md" /> |
| Driver spec template for spec-driven development | <ref_file file="/root/projects/SBK/docs/DRIVER_SPECIFICATION.md" /> |
| Original design papers | `docs/sbk.pdf`, `docs/sbp.pdf`, `docs/sbk-slc.pdf` |

---

## 6. The two AI-development workflows this repo supports

This repository works equally well for both styles of AI-assisted
development:

### 6.1 Vibe coding (informal, iterative)

For quick fixes, single-file edits, debugging:

1. Agent reads the relevant file + this `AGENTS.md` + the relevant
   `AGENT_RECIPES.md` recipe.
2. Agent makes the change.
3. Agent verifies with `./gradlew :drivers:<name>:check`.
4. Agent reports results to the human.

**Loop is small.** No spec document. Suitable for: bugfixes, logging
tweaks, small refactors, README updates.

### 6.2 Spec-driven development (formal, repeatable)

For larger work (a new driver, a new feature in `sbk-api`, a new logger
backend):

1. Human (or AI assistant) writes a spec by filling in
   <ref_file file="/root/projects/SBK/docs/DRIVER_SPECIFICATION.md" />
   (for drivers) or a similar markdown template.
2. Spec is reviewed / refined by the human.
3. Agent reads the spec + `AGENTS.md` + `AGENT_RECIPES.md`.
4. Agent generates code, tests, and docs according to the spec.
5. Agent runs the verification checklist; iterates on failures.
6. Spec stays in version control as the source of truth for the
   feature.

**Loop is larger** but produces auditable artefacts.

The spec template explicitly cross-references the recipes, so the
agent has a single deterministic path from spec → working code.

---

## 7. Things that are out of scope for an AI agent without explicit user approval

The following actions require explicit user confirmation **for every
specific action** (not blanket approval):

- Running `git push`, `git tag`, or any operation that publishes to a
  remote.
- Modifying the Apache 2.0 license headers or `LICENSE` file.
- Changing the SBK version in
  <ref_file file="/root/projects/SBK/gradle.properties" /> or in the
  root `build.gradle`.
- Adding a new top-level Gradle subproject (i.e., something parallel to
  `perl/`, `sbm/`, etc.). New *drivers* under `drivers/` are fine.
- Re-enabling `halodb` (see §4.1).
- Upgrading the MinIO SDK from 8.5.17 (see §4.3).
- Force-pushing, rewriting history, or deleting branches.

For everything else inside `drivers/`, `sbk-api/`, `perl/`, `sbm/`,
`docs/`, and the build files, normal edit-and-verify flow is fine.

---

## 8. Quick agent self-check before starting

Before making any change, the agent should be able to answer these
questions for the change at hand. If the agent can't answer them, it
should re-read this file and the relevant linked docs.

1. Which Gradle subproject does my change live in?
2. What's the verification command that proves my change is correct
   (typically a `./gradlew :<module>:check` command)?
3. Have I touched any of the gotcha areas in §4?
4. Have I updated `checkstyle/import-control.xml` if I added a new
   top-level dependency package?
5. For a driver change: have I updated **both**
   `settings-drivers.gradle` **and** `build-drivers.gradle`?
6. Are there any architecture invariants from
   <ref_file file="/root/projects/SBK/docs/sbk-internals.md" /> §8 my
   change must preserve (lock-free hot path, no sampling, no
   `synchronized` blocks, etc.)?

When in doubt, **prefer reading existing code over making assumptions**.
This codebase has 55 drivers; any specific pattern you need has almost
certainly been done before.

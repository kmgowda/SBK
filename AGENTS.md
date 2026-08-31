<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# AGENTS.md — AI Agent Guide for the SBK Repository

> **Universal entry point for all AI agents.** This file is the standard
> entry point for AI coding agents (Devin, Claude Code, Cursor, GitHub
> Copilot, Continue, Aider, OpenAI Codex, Windsurf, etc.) working in this
> repository. It tells the agent **what SBK is, how to build and verify it,
> what conventions to follow, where things live, and what the common
> gotchas are**.
>
> **Agent-specific configurations:**
> - Devin: See `.devin/skills/` for executable skills
> - Cursor: See `.cursor/rules/sbk.mdc`; `.cursorrules` is the legacy pointer
> - Aider: See `.aider.conf.yml` for Aider configuration
> - Codex and Windsurf: consume this root `AGENTS.md` directly
> - Benchmark execution: any agent may use the portable
>   `.devin/skills/sbk-benchmark-runner/` and
>   `.devin/skills/sbk-distributed-benchmark-runner/` knowledge packs
>
> **Humans:** see [README.md](README.md) for the end-user manual and
> [docs/sbk-internals.md](docs/sbk-internals.md) for the internal design.

---

## 1. What this repository is

**SBK** (Storage Benchmark Kit) is a Java framework for benchmarking *any*
S3-compatible / message-queue / file-system / database storage. It runs
identical measurement code against any backend via a small pluggable
driver SPI.

- **Languages.** Java only (no Kotlin, no Scala). Build with Gradle (wrapper
  in tree: `./gradlew`). JDK 25 required.
- **Modules.** 7 core modules + 52 enabled storage drivers. The source tree also
  contains disabled drivers and a driver template. Each module is a
  Gradle subproject.
- **License.** Apache 2.0.
- **Branch model.** Trunk-based; PRs target `master`.

### Module map (memorise this)

| Module | Role | When you edit it |
|---|---|---|
| `perl/` | **PerL** — Performance Logger library (lock-free queues, latency windows, percentile math). Heart of the framework. | Rarely. Only when changing core measurement behaviour. |
| `sbk-web-console/` | Independent Local Web Console runtime (HTTP server/client, protocol DTOs, UI resources). | When changing shared WebLogger transport or browser behavior. |
| `sbk-api/` | The benchmark harness. Defines `Storage<T>` SPI, `RWLogger` SPI, `SbkBenchmark`, `Sbk` main bootstrap. | When changing the harness, CLI flags, the SPI, or how loggers work. |
| `sbm/` | **SBM** — Storage Benchmark Monitor (gRPC aggregator on port 9717). | When changing distributed aggregation. |
| `sbk-yal/` | YML-driven launcher (single-node). | Rarely. |
| `sbk-gem/` | **SBK-GEM** — SSH-based distributed launcher. | When changing the multi-host orchestration. |
| `sbk-gem-yal/` | YML-driven SBK-GEM. | Rarely. |
| `drivers/<name>/` | One subdirectory per storage backend. **52 are enabled in the aggregate build**; disabled drivers and a template also remain in tree. | When adding or fixing a driver. **This is the most common change.** |

**For new drivers, see [docs/DRIVER_SPECIFICATION.md](docs/DRIVER_SPECIFICATION.md)
(spec template + worked example) and
[docs/AGENT_RECIPES.md](docs/AGENT_RECIPES.md) ("Add a storage driver"
recipe).**

---

## 2. Build, run, and verify

### Build commands

```bash
# Default build — runs compile + checkstyle + tests on every module.
./gradlew check

# Build the launchable scripts at ./build/install/sbk/bin/sbk
./gradlew :installDist

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

Verification is proportional to the affected surface:

1. Run the narrowest affected module check, such as
   `./gradlew :drivers:<name>:check`, `:sbk-api:check`, or `:perl:check`.
2. Run `./gradlew check` for source, dependency, or build-logic changes.
3. Run `./gradlew :installDist` when runtime packaging, discovery, launchers,
   drivers, or loggers are affected.
4. For driver behavior, run the installed CLI against a controlled real or
   mock backend (or `play.min.io` for S3). Compile-clean is not sufficient.
5. For documentation-only work, validate local links and references, render
   changed Mermaid diagrams when tooling is available, and run
   `git diff --check`; do not claim unrelated backend testing.

Report exact commands and distinguish a pass from a check that was not run or
could not run.

For a release candidate, use the authoritative gate rather than assembling a
manual subset:

```bash
./gradlew clean releasecheck \
  -Pprofile=release \
  -PreleaseInventory=/secure/sbk-release-inventory.properties \
  --no-daemon --rerun-tasks
```

See [`docs/RELEASE_QUALIFICATION.md`](docs/RELEASE_QUALIFICATION.md). A release
profile must fail when mandatory remote infrastructure is unavailable; it must
not convert missing GEM hosts or required backend coverage into a successful
skip. Use `-Pprofile=local-docker` for automatic disposable two-node
GEM functional coverage; it does not replace real-host release evidence.

Release qualification and publication are independent. Only an authorized
maintainer may dispatch an actual release with the root `publish` task and the
exact `-PreleaseConfirm=RELEASE-<version>` confirmation. The publication
workflow does not invoke or require `releasecheck`. The task uses locally
supplied `DOCKER_USERNAME` and `DOCKER_PASSWORD` credentials for Docker Hub,
then sends only the public image digest to GitHub. It returns after dispatch,
so the maintainer must monitor the asynchronous workflow and verify the public
tag, assets, packages, and container manifests. See
[`docs/RELEASE_PUBLICATION.md`](docs/RELEASE_PUBLICATION.md) for credentials,
prerelease/resume controls, the non-publishing artifact/container dry run, and
the complete publication contract.

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
| `drivers/<name>/src/main/resources/<config-file>.properties` | Default values for config fields. The filename must exactly match the storage class's resource lookup; existing drivers use both lowercase and class-case names. |

**Driver discovery is by simple class name, case-insensitive**:
`-class minio` resolves to `io.sbk.driver.MinIO.MinIO`. The class name
must match the file/directory name (modulo case).

### Code conventions

- **Lombok** is available (`@Synchronized`, `@SuppressFBWarnings`, etc.).
  See [`lombok.config`](lombok.config).
- **Checkstyle is strict.** The most common violations a new driver
  triggers:
  - Single-statement `if` blocks **must** have braces.
  - All public methods need Javadoc with `@param`, `@return`, `@throws`.
  - No unused imports.
  - Imports of new packages must be explicitly allowed in
    [`checkstyle/import-control.xml`](checkstyle/import-control.xml).
    *If a new dependency brings in a new top-level package, add it there.*
- **No `synchronized` blocks or `Lock` use in the driver hot path.**
  The harness's lock-free property depends on the driver also not
  blocking. The vendor SDK is fine; your wrapper code shouldn't add
  synchronization.
- **No allocation per record beyond what the SDK forces.** The PerL
  hot path is one `TimeStamp` per record; your driver should not
  allocate big arrays or maps per `writeAsync()` call.

### Hot-path latency policy (mandatory for every software agent)

This policy applies to **all** software agents and integrations working in
this repository, including Codex, Devin, Windsurf, Cursor, GitHub Copilot,
Claude Code, Continue, Aider, and future tools. Agent-specific configuration
may add guidance, but it must not weaken or bypass this policy.

The following are latency-critical hot paths:

- `sbk-api` per-record writer and reader loops, driver-call adapters, and
  benchmark measurement submission;
- PerL measurement recording, producer enqueue, consumer dequeue, queue
  traversal, and latency-window update paths; and
- SBM measurement ingestion, latency/count aggregation, and forwarding paths
  executed for each record or measurement batch.

`sbk-gem` is orchestration and lifecycle code; it does not execute the
per-record measurement path. Optimize its bounded startup, SSH, diagnostics,
and shutdown behavior for reliability, but do not describe those paths as
measurement hot paths. The remote SBK processes launched by SBK-GEM remain
subject to the `sbk-api` and PerL rules above.

**Keep the successful-operation path to the minimum work required to submit,
transport, aggregate, and record the measurement. Do not add new or redundant
per-operation conditional, coordination, state, or dispatch work to these hot
paths.** In particular, do not add:

- `if`, `switch`, ternary, short-circuit, state-polling, or other conditional
  branches, including extra conditions in an existing hot loop;
- atomic variables, atomic reads/writes, compare-and-set operations, or new
  `volatile` coordination fields;
- `VarHandle`, memory-fence, acquire/release, or other explicit memory-ordering
  operations;
- mutexes, monitors, `synchronized`, `Lock`, semaphore, or other contended
  coordination; or
- blocking or conditional waits such as `wait`, `await`, `sleep`, `park`, or
  blocking-queue operations.

Also minimize the non-coordination cost of every hot iteration:

- keep the live local/field state small; do not add redundant counters, flags,
  copied values, wrapper objects, temporary collections, or bookkeeping;
- avoid repeated getters, conversions, clock reads, bounds calculations, and
  values that can be computed once during startup or once per outer sweep;
- avoid new polymorphic/interface dispatch, lambda/callback indirection, and
  helper-call layers in per-record code; select specialized implementations at
  startup when modes have different requirements; and
- keep allocations and data movement at the minimum required by the existing
  SPI and transport contract.

Java source-level method count alone is not a performance metric: the JIT can
inline small monomorphic methods and eliminate locals. Agents must inspect the
actual call site and measure it rather than mechanically merging methods or
removing useful locals. The concern is additional work that remains in the
compiled hot path, especially dynamic dispatch, failed inlining, duplicated
loads, conversions, allocation, and enlarged live state.

Existing concurrency primitives that are required by a proven algorithm are
not authorization to add more, and must not be removed mechanically. For
example, PerL's multi-producer queue requires its existing publication
`VarHandle`/CAS and memory-ordering protocol. Changing such a primitive requires
a memory-model correctness argument plus queue stress, Lincheck, jcstress, GC,
and before/after performance evidence.

Keep EOF, disk-full, error handling, shutdown, lifecycle coordination, logging,
and configuration decisions outside the per-record measurement and queue
paths. Prefer existing exception propagation and worker/future lifecycle
boundaries. Moving a check into a helper does not make it acceptable if the
helper is still invoked for every record, enqueue, dequeue, or measurement.
Prefer startup specialization, separate duration/fixed-record implementations,
and existing empty-queue or lifecycle slow paths over a mode check in every
iteration. Do not duplicate a hot loop merely on intuition: require a focused
benchmark showing that specialization removes measurable compiled-path cost.

An exception to this rule requires **explicit confirmation from a human
developer before editing the hot path**. Before requesting confirmation, the
agent must:

1. identify the exact hot-path method and proposed branch, atomic/`VarHandle`
   operation, mutex, wait, allocation, state, or non-inlined invocation;
2. warn that the change can increase latency, jitter, contention, or reduce
   throughput;
3. explain why the behavior cannot be implemented outside the hot path; and
4. propose a before/after microbenchmark or representative benchmark that
   measures the performance impact.

A general request to implement a feature is not confirmation for hot-path
overhead. After explicit approval, keep the addition minimal, document the
reason in code, run the agreed performance comparison, and report the measured
delta. Use JMH for isolated costs and a representative SBK/PerlBench workload
for end-to-end throughput and latency; report allocation and variance where
applicable. Passing functional tests alone is not sufficient verification.

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
[`settings-drivers.gradle`](settings-drivers.gradle) and
[`build-drivers.gradle`](build-drivers.gradle).
Do not re-enable it without confirming the user wants to deal with the
GitHub Packages credentials.

### 4.2 Add a new driver to **two** gradle files

When introducing a new driver subdirectory, you must edit:

1. `settings.gradle` — *no, you don't.* The driver projects are
   listed in `settings-drivers.gradle`.
2. [`settings-drivers.gradle`](settings-drivers.gradle) — add
   `include 'drivers:<name>'`.
3. [`build-drivers.gradle`](build-drivers.gradle) — add
   `api project(':drivers:<name>')` so the driver is bundled into the
   `installDist` distribution.

**Forgetting either of these is the #1 source of "I added the driver
but `-class <name>` doesn't find it" issues.**

### 4.3 MinIO SDK is pinned to 8.5.17 (not the latest)

[`drivers/minio/build.gradle`](drivers/minio/build.gradle) uses
`io.minio:minio:8.5.17`. **Do not upgrade to 9.x** without testing — the
9.x SDK sends an `x-amz-sdk-checksum-algorithm` header on every
`PutObject` that older S3 backends (Dell ECS / ObjectScale, older Ceph
RGW) reject with HTTP 400 *InvalidRequest*. The comment in the
`build.gradle` explains.

### 4.4 The pathing JAR carries the runtime dependency graph

The `bin/sbk` script puts only the versioned pathing JAR and main SBK JAR
on the classpath; everything else (your driver's vendor SDK, transitive
deps) is reached through the pathing jar's `Class-Path:` manifest. The
`pathingJar` task declares `runtimeClasspath` as an input, so dependency graph
changes invalidate the manifest during incremental builds.

If files under an existing distribution were manually changed or copied,
regenerate it with:
```bash
./gradlew clean :pathingJar :installDist --rerun-tasks
```

The symptom of a manually inconsistent distribution is `NoClassDefFoundError`
for a class whose JAR is present under `build/install/sbk/lib/` but absent from
the pathing manifest.

### 4.5 Adding a new top-level dependency package requires updating
`checkstyle/import-control.xml`

The checkstyle rule `ImportControl` enforces an allow-list of top-level
packages. If your new driver pulls in a vendor SDK at a new
top-level package (e.g. `software.amazon`, `org.apache.solr`,
`okhttp3`), you must add
`<allow pkg="package.name" />` to
[`checkstyle/import-control.xml`](checkstyle/import-control.xml)
or `checkstyleMain` will fail.

### 4.6 Mermaid diagrams in `docs/`

If you edit Mermaid diagrams in [docs/sbk-internals.md](docs/sbk-internals.md),
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
([`MinIOWriter.java`](drivers/minio/src/main/java/io/sbk/driver/MinIO/MinIOWriter.java))
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
| End-user manual | [README.md](README.md) |
| Internal design / why SBK is fast / Mermaid diagrams | [docs/sbk-internals.md](docs/sbk-internals.md) |
| Step-by-step recipes (add a driver, add a logger, debug failures) | [docs/AGENT_RECIPES.md](docs/AGENT_RECIPES.md) |
| Driver spec template for spec-driven development | [docs/DRIVER_SPECIFICATION.md](docs/DRIVER_SPECIFICATION.md) |
| Dell ECS/ObjectScale benchmarking | [operator runbook](drivers/minio/docs/ECS_OBS_BENCHMARK_RUNBOOK.md), [agent runbook](drivers/minio/docs/ECS_OBS_AGENT_RUNBOOK.md), [MinIO implementation](drivers/minio/docs/IMPLEMENTATION.md) |
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
   [docs/DRIVER_SPECIFICATION.md](docs/DRIVER_SPECIFICATION.md)
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
  [`gradle.properties`](gradle.properties) or in the
  root `build.gradle`.
- Adding a new top-level Gradle subproject (i.e., something parallel to
  `perl/`, `sbm/`, etc.). New *drivers* under `drivers/` are fine.
- Re-enabling `halodb` (see §4.1).
- Upgrading the MinIO SDK from 8.5.17 (see §4.3).
- Adding a branch, atomic/`VarHandle` operation, mutex, wait, allocation,
  bookkeeping state, or non-inlined dispatch to an `sbk-api`, PerL, SBM, or
  driver hot path. The agent must first give the latency warning and obtain the
  specific developer confirmation required by §3.
- Force-pushing, rewriting history, or deleting branches.

For everything else inside `drivers/`, `sbk-web-console/`, `sbk-api/`, `perl/`, `sbm/`,
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
   [docs/sbk-internals.md](docs/sbk-internals.md) §8 my
   change must preserve (lock-free hot path, no sampling, no
   `synchronized` blocks, etc.)?
7. Does the change add any branch, atomic/`VarHandle` operation, mutex, wait,
   allocation, bookkeeping variable, conversion, clock read, or dynamic method
   invocation to a writer, reader, measurement, enqueue, dequeue, PerL, or SBM
   hot path? If so, stop, warn the developer, and obtain explicit confirmation
   before editing.

When in doubt, **prefer reading existing code over making assumptions**.
This codebase has more than 50 driver implementations; any specific pattern you need has almost
certainly been done before.

---
name: sbk-benchmark-runner
description: Plan, configure, run, and interpret single-load-generator Storage Benchmark Kit benchmarks with SBK or SBK-YAL. Use when an agent must select a local or service-backed driver, discover current options, create a reproducible CLI or YML workload, run a smoke/performance test, use SystemLogger/CSVLogger/PrometheusLogger/WebLogger, or diagnose benchmark results. Do not use this skill for multi-host load generation; use sbk-distributed-benchmark-runner.
---

# SBK Benchmark Runner

Follow the repository-wide safety and verification rules in `AGENTS.md`.
Use the installed distribution at `./build/install/sbk/bin/`. Treat generated
`-help` output and the selected driver's README as authoritative; options vary
by driver and logger.

## Select the execution model

1. Use `sbk` for one load-generator process configured on the command line.
2. Use `sbk-yal` for the same process configured from a reusable YML file.
3. A backend reached over the network still uses ordinary `sbk` or `sbk-yal`
   when only one load-generator host is required.
4. Use `$sbk-distributed-benchmark-runner` only for multiple load-generator
   hosts or manual aggregation through SBM.

Read [driver-connectivity.md](references/driver-connectivity.md) before choosing
a driver. Read [single-node-workflows.md](references/single-node-workflows.md)
for commands and YML examples. Read
[methodology-and-results.md](references/methodology-and-results.md) before
making performance claims.

## Run safely

1. Confirm JDK 25 and build the distribution when it is absent or stale:
   `./gradlew installDist`.
2. Discover options without guessing:
   - `./build/install/sbk/bin/sbk -help`
   - `./build/install/sbk/bin/sbk -class <driver> -help`
   - add `-out <logger>` before `-help` for logger options.
3. Read `drivers/<driver>/README.md` and inspect the driver's checked-in
   properties file for defaults.
4. Verify prerequisites: target path or service, credentials, permissions,
   network route, free space, and cleanup policy.
5. Run a short, bounded smoke test first. Prefer a dedicated test namespace,
   bucket, topic, table, or file.
6. Run the measured workload multiple times, preserving the exact command/YML,
   SBK version, JDK version, host facts, backend version, and raw output.
7. Reject or qualify a run containing I/O errors, timeouts, invalid latencies,
   unexpected discarded latencies, missing records, or target-side failures.

## Guardrails

- Never place real passwords, access keys, tokens, or private keys in a
  committed command or YML file.
- Do not create, recreate, delete, or overwrite backend data without confirming
  the selected driver options and target are dedicated to benchmarking.
- Do not infer a driver option from another driver. Generate driver-specific
  help.
- Use `-seconds` for a bounded throughput run. Use `-records` without
  `-seconds` when every requested operation must complete and be counted.
- Interpret latency in the unit selected by `-time`.
- Do not claim one configuration is faster from a single run.

## Bundled knowledge and examples

- [driver-connectivity.md](references/driver-connectivity.md): which drivers
  are self-contained/local and which need an external storage service.
- [single-node-workflows.md](references/single-node-workflows.md): SBK,
  SBK-YAL, logger, file, and MinIO workflows.
- [methodology-and-results.md](references/methodology-and-results.md):
  experimental design, result validation, and reporting.
- [example-sbk-file-write.yml](references/example-sbk-file-write.yml):
  safe local write example.
- [example-sbk-file-read.yml](references/example-sbk-file-read.yml):
  matching local read example.

---
name: sbk-distributed-benchmark-runner
description: Plan, configure, run, and diagnose distributed Storage Benchmark Kit workloads using standalone SBM, SBK-GEM, or SBK-GEM-YAL. Use when an agent must generate load from multiple hosts, aggregate manually launched SBK GrpcLogger clients, validate SSH/passwordless login and host keys, reconcile remote Java/SBK installations, create a distributed YML, or interpret per-host and aggregate results. Do not use merely because one SBK process connects to a remote backend.
---

# SBK Distributed Benchmark Runner

Follow the repository-wide safety and verification rules in `AGENTS.md`.
Use this skill for multiple load-generator hosts. For one load generator,
including one that connects to remote storage, use `$sbk-benchmark-runner`.

## Select the distributed mode

1. Use `sbk-gem` when one controller should connect over SSH, reconcile Java
   and SBK, launch the same workload on every node, and aggregate through its
   embedded SBM.
2. Use `sbk-gem-yal` for the same orchestration from a reusable YML file.
3. Use standalone `sbm` when another system launches SBK clients manually.
   Each client must select `GrpcLogger` and point to the SBM host/port.
4. Do not start a separate SBM for a normal SBK-GEM run; GEM owns one.

Read [distributed-topology.md](references/distributed-topology.md) before any
run. Use [gem-workflows.md](references/gem-workflows.md) for GEM/GEM-YAL and
[standalone-sbm.md](references/standalone-sbm.md) for manual aggregation.
Use [distributed-validation.md](references/distributed-validation.md) to
preflight and judge the result.

For distributed Dell ECS/ObjectScale S3 load, also read
[`drivers/minio/docs/ECS_OBS_AGENT_RUNBOOK.md`](../../../drivers/minio/docs/ECS_OBS_AGENT_RUNBOOK.md) and
[`drivers/minio/docs/ECS_OBS_BENCHMARK_RUNBOOK.md`](../../../drivers/minio/docs/ECS_OBS_BENCHMARK_RUNBOOK.md)
completely. Qualify the same ordinary MinIO-driver command on every load node
before adding GEM orchestration.

## Required workflow

1. Build the installed distribution with `./gradlew installDist`. For
   standalone SBM also run `./gradlew :sbm:installDist`.
2. Make the intended SBK command succeed on one load node first.
3. Confirm every load node can reach the backend.
4. Confirm controller-to-node SSH, trusted host keys, remote disk/permissions,
   and Java 25.
5. Confirm every node can reach the controller's advertised SBM host and port
   (default `9717`).
6. Start with one node, one worker, a dedicated target, and a short bounded run.
7. Scale node and worker counts only after client connections, exact counts,
   return codes, and aggregate metrics are correct.
8. Preserve sanitized topology, arguments/YML, versions, per-host responses,
   aggregate output, and backend/load-host telemetry.

## Guardrails

- Prefer SSH agent/key-file authentication. Never commit `gempass`, private
  keys, storage secrets, tokens, or production inventories.
- Keep host-key checking enabled and pre-populate trusted `known_hosts`.
- `-localhost` means the controller address advertised to remote clients; it
  must be reachable from every node and must not be loopback for remote nodes.
- Remote processes use `GrpcLogger`; choose `GemPrometheusLogger` or
  `GemWebLogger` for the controller-side aggregate.
- Confirm `-packagescleanup`, `-fullcopy`, and any preferred remote `-javadir`
  before allowing remote filesystem changes. Missing SBK content and an
  insufficient remote JDK are provisioned automatically from the
  launcher-selected controller distribution.
- Treat any failed remote return code, missing SBM connection, timeout, invalid
  latency, or unexplained discarded latency as a failed/qualified run.

## Bundled knowledge and examples

- [distributed-topology.md](references/distributed-topology.md): roles, flows,
  ports, and application-selection rules.
- [gem-workflows.md](references/gem-workflows.md): SSH, provisioning, GEM CLI,
  and GEM-YAL override behavior.
- [standalone-sbm.md](references/standalone-sbm.md): manual SBM/client setup.
- [distributed-validation.md](references/distributed-validation.md): preflight,
  failure isolation, and acceptance criteria.
- [example-sbk-gem-null-smoke.yml](references/example-sbk-gem-null-smoke.yml):
  non-storage orchestration smoke template.
- [sbk-gem-ecs.yml](../../../drivers/minio/examples/sbk-gem-ecs.yml): S3
  distributed workload template with explicit placeholders.

<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# SBK documentation

This directory contains the authoritative engineering documentation for Storage Benchmark Kit. The root [README](../README.md) is the product and quick-start entry point; this page routes readers to the right level of detail.

## Reading paths

### New user

1. [Project README](../README.md): requirements, build, first benchmark, and module overview.
2. [ECS/ObjectScale benchmark runbook](../drivers/minio/docs/ECS_OBS_BENCHMARK_RUNBOOK.md): safe qualification and performance workflows for Dell ECS/OBS using the MinIO driver.
3. [WebLogger guide](WEB_LOGGER.md): the dependency-free SBK Local Web Console for SBK, SBM, and SBK-GEM.
4. [PrometheusLogger and SBK Dashboard guide](PROMETHEUS_LOGGER.md): persistent Prometheus metrics and Grafana dashboards for direct and distributed runs.
5. The README under the selected `drivers/<name>/` directory: backend prerequisites and examples.
6. [Architecture and code flow](ARCHITECTURE.md): what happens after the command starts.

### New contributor

1. [Contributing guide](../CONTRIBUTING.md): workflow, standards, and verification.
2. [Repository map](REPOSITORY_MAP.md): where code and configuration live.
3. [Driver guide](DRIVER_GUIDE.md): the most common extension workflow.
4. [Engineering recipes](AGENT_RECIPES.md): task-oriented implementation procedures.

### Maintainer or reviewer

1. [Architecture and code flow](ARCHITECTURE.md): ownership boundaries and lifecycle.
2. [Internal design](sbk-internals.md): detailed PerL measurement flow,
   `ElasticWait`, timestamp queues, SBM, and SBP design.
3. [TimeStampMpscQueue research guide](TIMESTAMP_MPSC_QUEUE.md): intrusive MPSC queue architecture, correctness evidence, JDK comparison, and reproducible performance methodology.
4. [Exact latency recorders research guide](LATENCY_RECORDERS.md): dense-array, boxed-map, and primitive-map algorithms, memory models, correctness, and JDK 25 JMH results.
5. [Documentation maintenance](DOCUMENTATION_GUIDE.md): how to keep examples and links current.
6. [Agent-documentation distribution](AGENT_DOCUMENTATION_DISTRIBUTION.md): how documentation enters release artifacts.
7. [Release qualification](RELEASE_QUALIFICATION.md): the one-command local,
   CI, and release-candidate gates and their required infrastructure.
8. [Release publication](RELEASE_PUBLICATION.md): dry runs, contracted assets,
   GitHub Packages, Docker Hub/GHCR images, signing, and guarded publication.

### Coding agent

1. [AGENTS.md](../AGENTS.md): repository constraints and required verification.
2. [Coding-agent toolkit](AGENT_TOOLKIT.md): tool discovery, task routing, and
   the shared workflow for Codex, Windsurf, Devin, Cursor, Aider, and others.
3. [Engineering recipes](AGENT_RECIPES.md): deterministic task playbooks.
4. [Driver specification template](DRIVER_SPECIFICATION.md): spec-driven driver work.
5. [ECS/OBS agent runbook](../drivers/minio/docs/ECS_OBS_AGENT_RUNBOOK.md): authorized, staged, machine-checkable ECS/ObjectScale performance workflows.

`INSTRUCTIONS.md` is a compact compatibility pointer for tools that do not
discover `AGENTS.md` directly; it does not replace the full guide.

## Document ownership

| Document | Authoritative for |
|---|---|
| [README.md](../README.md) | Product overview, installation, first run, top-level commands |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Module boundaries, code flow, lifecycle, concurrency, extension points |
| [REPOSITORY_MAP.md](REPOSITORY_MAP.md) | Directory and important-file navigation |
| [DRIVER_GUIDE.md](DRIVER_GUIDE.md) | Driver inventory, contract, structure, and verification |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Human contribution workflow and definition of done |
| [AGENTS.md](../AGENTS.md) | Agent rules and repository-specific constraints |
| [AGENT_RECIPES.md](AGENT_RECIPES.md) | Exact task procedures |
| [AGENT_TOOLKIT.md](AGENT_TOOLKIT.md) | Cross-tool agent discovery, context routing, permissions, and verification |
| [AGENT_DOCUMENTATION_DISTRIBUTION.md](AGENT_DOCUMENTATION_DISTRIBUTION.md) | Agent-documentation release and artifact packaging |
| [RELEASE_QUALIFICATION.md](RELEASE_QUALIFICATION.md) | Automated build, concurrency, packaging, logger, SBM, GEM, documentation, publication, and performance release gates |
| [RELEASE_PUBLICATION.md](RELEASE_PUBLICATION.md) | Release dry run, artifact contract, container registries, authentication, and publication workflow |
| [DRIVER_SPECIFICATION.md](DRIVER_SPECIFICATION.md) | Fillable design template for new drivers |
| [DOCUMENTATION_GUIDE.md](DOCUMENTATION_GUIDE.md) | Documentation ownership, required content, and validation |
| [WEB_LOGGER.md](WEB_LOGGER.md) | SBK Local Web Console usage, lifecycle, options, security, and troubleshooting |
| [PROMETHEUS_LOGGER.md](PROMETHEUS_LOGGER.md) | PrometheusLogger, SBM/GEM aggregate exporters, and standalone SBK Dashboard deployment and operation |
| [MinIO ECS/OBS operator runbook](../drivers/minio/docs/ECS_OBS_BENCHMARK_RUNBOOK.md) | Dell ECS/ObjectScale S3 qualification, workload design, commands, option map, result acceptance, and validated examples |
| [MinIO ECS/OBS agent runbook](../drivers/minio/docs/ECS_OBS_AGENT_RUNBOOK.md) | Safe deterministic ECS/OBS benchmark execution and reporting for software agents |
| [sbk-internals.md](sbk-internals.md) | Detailed design rationale and research-oriented treatment |
| [TIMESTAMP_MPSC_QUEUE.md](TIMESTAMP_MPSC_QUEUE.md) | Intrusive timestamp queue architecture, JDK comparison, correctness evidence, and research methodology |
| [LATENCY_RECORDERS.md](LATENCY_RECORDERS.md) | Exact array and sparse-map latency storage, complexity, memory accounting, correctness, and reproducible JMH results |
| [PerlBench driver](../drivers/perlbench/README.md) | End-to-end timestamp-queue comparison using exact-count, timed, and rate-controlled SBK workloads |
| Component READMEs | Component operation and component-specific examples |
| Driver READMEs | Backend prerequisites, properties, limitations, and example commands |
| [MinIO driver implementation](../drivers/minio/docs/IMPLEMENTATION.md) | MinIO/S3 request, measurement, concurrency, memory, catalog, retry, and shutdown semantics |

When documents disagree, source code and generated `-help` output are authoritative. Correct the nearest authoritative document instead of copying a workaround into several READMEs.

## Generated and historical material

- `*/javadoc/` contains generated API documentation. Do not hand-edit generated HTML or its bundled legal notices.
- PDFs in `docs/` are design publications and historical references, not operational instructions.
- Long console transcripts in some driver READMEs are examples from particular environments, not performance guarantees.
- Dashboards, Docker definitions, and Kubernetes examples are operational assets; their nearby README explains their use.

## Documentation standards

- Use repository-relative links for files in this repository.
- Name the source class or Gradle task that supports an architectural claim.
- Mark defaults as defaults, not requirements, unless validation enforces them.
- Never present benchmark numbers as generally reproducible results.
- Keep commands runnable from the repository root unless a different directory is explicitly stated.
- Use `./gradlew`, not an assumed system Gradle installation.
- Update both the detailed guide and its entry-point link when introducing a new subsystem.

See [DOCUMENTATION_GUIDE.md](DOCUMENTATION_GUIDE.md) for the review checklist.

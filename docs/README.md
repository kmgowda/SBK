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
2. [WebLogger guide](WEB_LOGGER.md): dependency-free local live graphs for SBK, SBM, and SBK-GEM.
3. The README under the selected `drivers/<name>/` directory: backend prerequisites and examples.
4. [Architecture and code flow](ARCHITECTURE.md): what happens after the command starts.

### New contributor

1. [Contributing guide](../CONTRIBUTING.md): workflow, standards, and verification.
2. [Repository map](REPOSITORY_MAP.md): where code and configuration live.
3. [Driver guide](DRIVER_GUIDE.md): the most common extension workflow.
4. [Engineering recipes](AGENT_RECIPES.md): task-oriented implementation procedures.

### Maintainer or reviewer

1. [Architecture and code flow](ARCHITECTURE.md): ownership boundaries and lifecycle.
2. [Internal design](sbk-internals.md): detailed PerL, SBM, SBP, and measurement design.
3. [TimeStampMpscQueue research guide](TIMESTAMP_MPSC_QUEUE.md): intrusive MPSC queue architecture, correctness evidence, JDK comparison, and reproducible performance methodology.
4. [Documentation maintenance](DOCUMENTATION_GUIDE.md): how to keep examples and links current.
5. [Agent-documentation distribution](AGENT_DOCUMENTATION_DISTRIBUTION.md): how documentation enters release artifacts.

### Coding agent

1. [AGENTS.md](../AGENTS.md): repository constraints and required verification.
2. [INSTRUCTIONS.md](../INSTRUCTIONS.md): compact compatibility entry point.
3. [Engineering recipes](AGENT_RECIPES.md): deterministic task playbooks.
4. [Driver specification template](DRIVER_SPECIFICATION.md): spec-driven driver work.

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
| [DRIVER_SPECIFICATION.md](DRIVER_SPECIFICATION.md) | Fillable design template for new drivers |
| [WEB_LOGGER.md](WEB_LOGGER.md) | Local live dashboard usage, lifecycle, options, security, and troubleshooting |
| [sbk-internals.md](sbk-internals.md) | Detailed design rationale and research-oriented treatment |
| [TIMESTAMP_MPSC_QUEUE.md](TIMESTAMP_MPSC_QUEUE.md) | Intrusive timestamp queue architecture, JDK comparison, correctness evidence, and research methodology |
| [PerlBench driver](../drivers/perlbench/README.md) | End-to-end timestamp-queue comparison using exact-count, timed, and rate-controlled SBK workloads |
| Component READMEs | Component operation and component-specific examples |
| Driver READMEs | Backend prerequisites, properties, limitations, and example commands |

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

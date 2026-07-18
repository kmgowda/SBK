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

# Coding-agent toolkit

SBK exposes one shared body of repository knowledge to coding agents while allowing tool-specific configuration to remain thin. Human contributors can use the same documents; no engineering rule should exist only inside one vendor's agent configuration.

## Universal entry points

| File | Role |
|---|---|
| [`AGENTS.md`](../AGENTS.md) | Authoritative repository rules, constraints, gotchas, and definition of done |
| [`INSTRUCTIONS.md`](../INSTRUCTIONS.md) | Compact compatibility entry point that links to `AGENTS.md` |
| [`README.md`](../README.md) | Product, build, and run overview |
| [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) | Source-linked module and code-flow model |
| [`docs/REPOSITORY_MAP.md`](REPOSITORY_MAP.md) | Ownership and file navigation |
| [`docs/AGENT_RECIPES.md`](AGENT_RECIPES.md) | Deterministic procedures for common changes |
| [`docs/DRIVER_SPECIFICATION.md`](DRIVER_SPECIFICATION.md) | Formal design template for driver work |

An agent should read only the relevant deeper guides after the universal entry point, but it must read the complete instructions it selects.

## Tool-specific files

The repository may include configuration for individual coding tools, such as `.devin/skills/`, `.cursorrules`, or `.aider.conf.yml`. These files should:

- Point back to `AGENTS.md` rather than copying all rules.
- Add only syntax, permissions, or workflows unique to that tool.
- Never weaken repository safety or verification requirements.
- Avoid absolute checkout paths.
- Remain optional for contributors using other tools.

Availability of a tool-specific file does not mean that every installation of that tool automatically loads it; consult that tool's current documentation.

## Recommended agent workflow

1. Read `AGENTS.md` and inspect repository status.
2. Classify the change by module using `REPOSITORY_MAP.md`.
3. Read the relevant source, tests, build file, and component/driver README.
4. Use an `AGENT_RECIPES.md` procedure when one matches.
5. For substantial driver work, fill in `DRIVER_SPECIFICATION.md` before coding.
6. Preserve unrelated working-tree changes.
7. Implement the smallest coherent change.
8. Run focused checks, then full verification appropriate to risk.
9. Update the authoritative documentation in the same change.
10. Report changes, tests, limitations, and any unverified external behavior.

## Context by task

| Task | Read after `AGENTS.md` |
|---|---|
| Driver fix | Driver README, source, `DRIVER_GUIDE.md`, recipe 2 |
| New driver | `DRIVER_SPECIFICATION.md`, `DRIVER_GUIDE.md`, recipe 1, similar driver |
| Harness CLI | `ARCHITECTURE.md`, `SbkParameters`, recipe 4 |
| Measurement change | `sbk-internals.md`, PerL tests, architecture invariants |
| Logger | `RWLogger`, existing implementation, recipe 3 |
| Distributed aggregation | Architecture distributed flow, SBM README and source |
| Remote orchestration | SBK-GEM README, GEM source, failure-domain section |
| Documentation | `DOCUMENTATION_GUIDE.md`, recipe 6 |

## Permissions and safety

Documentation can describe commands that publish or alter remote state, but an agent must not infer permission to execute them. In particular, a normal implementation request does not authorize pushing, tagging, publishing, changing versions/licenses, re-enabling restricted drivers, destructive cleanup, or rewriting history.

Agents should prefer read-only discovery, scoped edits, Gradle wrapper commands, and explicit reporting. Secrets and private endpoints must not enter source, examples, logs, patches, or tool output.

## Verification baseline

```bash
./gradlew :<affected-module>:check
./gradlew check
./gradlew installDist
git diff --check
```

Driver work adds installed-distribution discovery and a real-backend smoke test. Dependency changes add a clean pathing-JAR rebuild. Documentation-only changes still require link/reference review, Mermaid validation where available, and `git diff --check`.

## Maintaining the toolkit

- Keep universal rules in `AGENTS.md`.
- Keep `INSTRUCTIONS.md` compact.
- Add task procedures to `AGENT_RECIPES.md` rather than a platform-specific prompt.
- Update source-linked architecture when ownership or control flow changes.
- Test distribution inclusion using [AGENT_DOCUMENTATION_DISTRIBUTION.md](AGENT_DOCUMENTATION_DISTRIBUTION.md).
- Review tool-specific files for stale paths and duplicated rules whenever universal instructions change.

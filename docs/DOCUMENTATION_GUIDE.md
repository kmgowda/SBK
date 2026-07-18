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

# Documentation maintenance guide

SBK documentation serves users, backend specialists, maintainers, researchers, and automated coding agents. A change is documented when the nearest authoritative guide explains the new behavior and all entry points lead to it.

## Where an update belongs

| Change | Required documentation |
|---|---|
| User-visible option or behavior | Root README if common, component/driver README, generated help text/Javadoc |
| Module or control-flow change | `ARCHITECTURE.md` and possibly `sbk-internals.md` |
| New driver | Driver README, `DRIVER_GUIDE.md` inventory, registration files |
| New development workflow | `CONTRIBUTING.md` and relevant `AGENT_RECIPES.md` recipe |
| New repository directory | `REPOSITORY_MAP.md` |
| New release packaging behavior | `AGENT_DOCUMENTATION_DISTRIBUTION.md` |
| Agent constraint or gotcha | `AGENTS.md`; keep `INSTRUCTIONS.md` as a compact pointer |

Avoid duplicating the same detailed explanation. Add a short summary and link to the authoritative document.

## Driver README minimum structure

Every runtime driver README should state:

1. What backend/API it benchmarks.
2. Whether reads and writes are supported.
3. External prerequisites and tested compatibility where known.
4. Driver-specific options and their defaults.
5. One minimal write command and one minimal read command, if supported.
6. Data/key/stream lifecycle behavior that affects repeated tests.
7. Durability, acknowledgement, batching, or transaction semantics.
8. Shutdown behavior and known limitations.
9. A reminder that displayed benchmark output is illustrative, not a guarantee.

The common build, architecture, and contribution instructions should be linked, not copied.

## Validate documentation

Run these checks from the repository root:

```bash
# Find Markdown links that reference repository-relative files
rg -n '\[[^]]+\]\([^)]+\)' --glob '*.md'

# Find stale Java-version and versioned-artifact references
rg -n 'Java (8|11|17|21|22|23|24)|JDK (8|11|17|21|22|23|24)|sbk-pathing-10\.0' --glob '*.md'

# Check whitespace errors
git diff --check
```

For Mermaid changes, use Mermaid CLI 11 or later when available. Render every changed diagram; GitHub rendering alone should not be the first syntax test.

For command changes, prefer exercising the actual command. At minimum, verify the task exists and that option names match the code. Driver help is generated only after discovery, so use:

```bash
./gradlew installDist
./build/install/sbk/bin/sbk -class <driver> -help
```

## Review checklist

- Links use correct case and repository-relative paths.
- Commands state their working directory or assume repository root.
- JDK requirement matches `gradle/java.gradle`.
- Project version is not hard-coded when `gradle.properties` is authoritative.
- Enabled-driver claims match both driver registration files.
- Defaults match the corresponding properties or parameter source.
- Examples do not expose credentials or internal endpoints.
- Performance output is labeled illustrative.
- Architecture claims identify source classes.
- A reader can distinguish local SBK, SBM aggregation, and SBK-GEM orchestration.
- Agent instructions do not grant publishing, destructive, or version-changing authority.

## Generated Javadocs

Modules with an `updateDocs` Gradle task copy generated Javadocs into their checked-in `javadoc/` directory. Do not edit those generated files manually. When an API signature or Javadoc changes and checked-in Javadocs are part of the requested deliverable, regenerate through Gradle and review the generated diff separately.

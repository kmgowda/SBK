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

# SBK coding-agent instructions

This is the compact compatibility entry point for tools that look for `INSTRUCTIONS.md`. The authoritative repository rules are in [AGENTS.md](AGENTS.md); read that file before changing code.

## Required context

- [README.md](README.md): product overview, build, and first run.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): module boundaries and runtime flow.
- [docs/REPOSITORY_MAP.md](docs/REPOSITORY_MAP.md): code navigation.
- [docs/AGENT_RECIPES.md](docs/AGENT_RECIPES.md): exact task procedures.
- [docs/DRIVER_GUIDE.md](docs/DRIVER_GUIDE.md): driver contract and verification.

## Non-negotiable facts

- SBK is a Java 25 multi-project Gradle build; use `./gradlew`.
- The dependency direction is `perl <- sbk-api <- drivers`.
- Drivers implement `Storage<T>` and should leave general scheduling and timing to the harness.
- Every enabled driver appears in both `settings-drivers.gradle` and `build-drivers.gradle`.
- Checkstyle is strict, including import-package allow-listing.
- Do not add synchronization or avoidable allocation to driver operation paths.
- Do not hand-edit generated Javadocs.
- HaloDB and Ignite are disabled; `sbktemplate` is not a runtime driver.
- MinIO 8.5.17 is intentionally pinned for older S3-compatible backend behavior.

## Normal workflow

1. Inspect the working tree and preserve unrelated changes.
2. Read the affected source, its module build file, and the nearest documentation.
3. Make a focused change.
4. Run the affected module check.
5. Run the full build and distribution verification when feasible.
6. Smoke-test driver behavior against the relevant backend.
7. Update documentation and report exact verification results.

```bash
git status --short
./gradlew :<module>:check
./gradlew check
./gradlew installDist
git diff --check
```

After dependency changes:

```bash
./gradlew clean :pathingJar installDist --rerun-tasks
```

## Actions requiring explicit authority

Do not infer permission to publish, push, tag, rewrite history, change the project version or license, add a new top-level module, re-enable HaloDB, or upgrade the pinned MinIO client. See [AGENTS.md](AGENTS.md) for the complete constraints and definition of done.

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

# Contributing to SBK

Thank you for improving Storage Benchmark Kit. Contributions include driver support, correctness fixes, measurement work, documentation, tests, deployment assets, and reproducible benchmark investigations.

## Before starting

1. Read the [project overview](README.md) and [architecture guide](docs/ARCHITECTURE.md).
2. For driver work, read [docs/DRIVER_GUIDE.md](docs/DRIVER_GUIDE.md) and the closest existing driver.
3. Check open [issues](https://github.com/kmgowda/SBK/issues) and pull requests for overlapping work.
4. Use a focused branch from the current `master`; pull requests target `master`.
5. Confirm JDK 25 is active with `./gradlew --version`.

For a large driver or cross-module feature, open or discuss a design first. The [driver specification template](docs/DRIVER_SPECIFICATION.md) captures completion semantics, configuration, test strategy, and risks before implementation.

## Development workflow

```bash
git clone https://github.com/kmgowda/SBK.git
cd SBK
./gradlew check
```

Make the smallest coherent change, then verify the affected module first:

```bash
./gradlew :sbk-api:check
./gradlew :perl:check
./gradlew :drivers:minio:check
```

Before requesting review, run:

```bash
./gradlew check
./gradlew :installDist
git diff --check
```

Dependency or distribution changes require:

```bash
./gradlew clean :pathingJar :installDist --rerun-tasks
```

Driver changes also require a real-backend smoke test. State the target version and exact sanitized command in the pull request.

## Code standards

- Java only; source and target level are Java 25.
- Four spaces, no tabs.
- Preserve the Apache 2.0 header on source files.
- Follow strict Checkstyle and import-control rules.
- Add Javadoc for public APIs, including relevant parameters, returns, and exceptions.
- Do not add synchronization or explicit locks to a driver operation path.
- Avoid unnecessary allocation per record.
- Keep vendor-specific behavior inside its driver.
- Keep histogram, percentile, and reporting work outside driver I/O methods.
- Preserve idempotent shutdown and tolerate partially opened resources.

Run `./gradlew :<module>:checkstyleMain` for focused style feedback.

## Tests

Add tests at the lowest practical layer:

- PerL algorithms: deterministic unit tests for queues, windows, and percentiles.
- Harness behavior: parser, utility, lifecycle, and worker tests.
- Drivers: configuration, serialization, key generation, and failure classification without a live service where possible.
- Backend integration: documented manual smoke tests or an opt-in integration task; do not make the default build depend on public services.

Performance-sensitive changes should include a rationale and, when appropriate, JMH or controlled before/after evidence. Benchmark numbers must include environment and configuration details.

## Documentation

Update documentation in the same change as behavior. Use [docs/DOCUMENTATION_GUIDE.md](docs/DOCUMENTATION_GUIDE.md) to choose the authoritative document and validation steps.

Do not hand-edit generated Javadocs. Use the module's `updateDocs` task if checked-in generated API documentation must change.

## Commit and pull-request guidance

- Use imperative commit subjects, for example `Fix MinIO shutdown handling`.
- Keep unrelated formatting or dependency changes out of a functional patch.
- Explain the problem, design, compatibility impact, and verification.
- Identify external systems and versions used in smoke tests.
- Call out changes to wire formats, CLI behavior, defaults, dependencies, or performance semantics.
- Never include secrets, private endpoints, or production data in commits or logs.

## Areas requiring maintainer agreement

Discuss these before implementation or explicitly call them out for approval:

- SBK version changes or release publication.
- License changes.
- New top-level modules alongside `perl`, `sbk-api`, or `sbm`.
- Protobuf/SBP compatibility changes.
- Re-enabling HaloDB.
- Upgrading MinIO beyond the intentionally compatible 8.5.17 line.
- Changes that alter what “operation complete” means for an existing driver.

Publishing, tagging, pushing another contributor's branch, and destructive history changes are never implied by a code-change request.

## Release maintainers

Release qualification and release publication are separate procedures. Use
the `release` profile in
[the qualification guide](docs/RELEASE_QUALIFICATION.md) to qualify the exact
commit first. Then follow the ordered checklist in
[the publication guide](docs/RELEASE_PUBLICATION.md) for the non-publishing
artifact dry run, independent Maven Central/JReleaser operation, locally
authenticated Docker Hub publication, guarded GitHub workflow dispatch,
recovery, and post-release verification. Never infer publication authority
from a request to build, test, or prepare a candidate.

## Definition of done

- The implementation respects module ownership and performance invariants.
- Focused and full checks pass.
- The installed distribution is verified when runtime packaging is affected.
- Relevant backend smoke tests pass.
- User, architecture, driver, and agent documentation is current.
- `git diff --check` reports no whitespace errors.
- The pull request contains enough evidence for another contributor to reproduce the result.

For task-specific commands and debugging ladders, see [docs/AGENT_RECIPES.md](docs/AGENT_RECIPES.md).

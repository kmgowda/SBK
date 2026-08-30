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

# Documentation in SBK release artifacts

SBK distributes human and agent documentation with executable and library release channels so that operational and development context remains available outside a Git checkout.

## Gradle application distributions

`./gradlew :installDist`, `:distZip`, and `:distTar` use the root `applicationDistribution` configuration.

The distribution root contains:

- `README.md`
- `AGENTS.md`
- `INSTRUCTIONS.md`
- Agent-specific configuration files that exist in the repository

Current tool-specific entries include the Cursor project rule and legacy
pointer, the Aider configuration, and the portable skills under
`.devin/skills/`. Windsurf and Codex consume the root `AGENTS.md` directly and
do not need duplicate repository rules.

The `docs/` directory contains the engineering index, architecture, repository map, driver guide, maintenance guide, recipes, specification template, toolkit, distribution guide, and detailed internals.

Verify locally:

```bash
./gradlew :installDist
find build/install/sbk -maxdepth 2 -type f | sort
```

The definitive inclusion list is the `applicationDistribution` block in the root `build.gradle`.

## Documentation JAR

Modules applying `gradle/maven.gradle` register a `docsJar` artifact with the `docs` classifier. It contains the root entry points, selected engineering documents, agent skills, and supported agent configuration files. Inside the JAR, `docs/README.md` is the documentation index and `docs/PROJECT_README.md` is the root product README.

Build and inspect without publishing:

```bash
./gradlew docsJar
find . -path '*/build/libs/*-docs.jar' -type f -print
jar tf <path-to-docs-jar>
```

Artifact names use the version from `gradle.properties`; documentation must not hard-code a release number.

## GitHub release workflow

The guarded release workflow:

1. Independently builds installed, ZIP, and TAR distributions for SBK, SBM,
   and the Local Web Console. Release qualification is a separate maintainer
   gate and is intentionally not invoked or consumed by this workflow.
2. Creates `sbk-agent-docs.tar.gz` from root entry points, the complete `docs/`
   directory, and agent configurations while preserving discovery paths such
   as `.cursor/rules/` and `.devin/skills/`.
3. Adds checksums, the release manifest, and direct CycloneDX SBOMs for the
   publishable core modules to the contracted asset directory. Qualification
   evidence remains under `build/reports/release-qualification/` and must be
   retained separately for the exact released commit.
4. Attaches the complete, verified directory to a draft GitHub Release and
   publishes the release only after container and package publication succeeds.

See [Release publication](RELEASE_PUBLICATION.md) for the safe local and Actions
dry runs. Editing this document or running a dry run does not publish anything.

## Maven publication

`gradle/maven.gradle` adds `docsJar` to each applicable Maven publication. The
release dry run exercises these publications against project-local repositories.
The guarded workflow uses the internal
`releasePublishCoreToGitHubPackages` task for GitHub Packages. Independent
maintainer-run Maven Central publication stages the explicit core allow-list
with `releaseStageCorePublications` and deploys it with JReleaser. The GitHub
workflow never configures or invokes JReleaser. Driver projects are excluded
from standalone publication. The root `publish` task first publishes the
multi-architecture Docker Hub image with local
`DOCKER_USERNAME`/`DOCKER_PASSWORD` credentials and then dispatches the
guarded GitHub-owned publication workflow with only its public immutable
digest. Docker Hub credentials are not sent to GitHub.

JReleaser configuration is centralized in
`gradle/release-publication.gradle`, which explicitly registers every core
module staging repository. `gradle/maven.gradle` only stages unsigned module
artifacts; Maven Central signing is owned exclusively by JReleaser.

## Maintenance checklist

When adding an authoritative document:

- Link it from [docs/README.md](README.md).
- Add it to root `applicationDistribution` when executable users need it.
- Add it to `docsJar` when library consumers need it.
- Add it to standalone GitHub release assets only if it is a primary entry point; the documentation archive already contains the whole `docs/` directory.
- Build and inspect artifacts before claiming inclusion.
- Keep file lists version-neutral and avoid fixed artifact names.

Source locations:

- Root distribution: `build.gradle`
- Documentation JAR: `gradle/maven.gradle`
- Release archive/assets: `gradle/release-publication.gradle` and
  `.github/workflows/release.yml`

<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# SBK release publication

SBK uses one guarded GitHub Actions workflow for GitHub Release assets,
GitHub Packages, Sonatype Central/Maven Central, Docker Hub, and the GitHub
Container Registry. Publication is separate from qualification:
`releasecheck` proves the candidate, while `.github/workflows/release.yml`
coordinates the remote publication steps. The established JReleaser
configuration remains the Maven Central publisher; the legacy Gradle
`-Pmaven` repository path is not used. Only JReleaser's GitHub tag/release
step is disabled so it cannot race the workflow's checksum-verified release.

## Safe local dry run

The following command runs the CI qualification profile, exercises every
core-module Maven `publish` task against project-local repositories, validates
the exact main/source/Javadoc/documentation artifact set that JReleaser will
deploy, assembles the container context, and creates the complete release asset
contract:

```bash
./gradlew clean releasePublicationDryRun \
  -Pprofile=ci \
  --no-daemon --rerun-tasks
```

It never creates or pushes a Git tag, GitHub Release, Maven package, Docker
Hub image, or GHCR image. The former `publish -Pgithub` selector has been
removed; GitHub Packages are part of the complete guarded release. Results are
written to:

```text
build/release-assets/
build/release-container/
```

The Gradle dry run prepares the Docker context but does not require a Docker
daemon. For the complete two-architecture dry run, open **Actions > SBK
Release > Run workflow**, enter the version, keep `dry_run=true`, and select
`ci`, `local-docker`, or `release`. The workflow builds and executes native
AMD64 and ARM64 images but performs no registry login or push.

The local dry run intentionally does not invoke `jreleaserDeploy`: JReleaser
requires real signing and Sonatype credentials even in its own dry-run mode.
The guarded publication job supplies those secrets, runs `jreleaserConfig`,
and then uses JReleaser for the actual Central deployment.

## Contracted GitHub Release assets

Every release contains:

```text
sbk-<version>.zip
sbk-<version>.tar
sbm-<version>.zip
sbm-<version>.tar
sbk-web-console-<version>.zip
sbk-web-console-<version>.tar
sbk-agent-docs.tar.gz
qualification.json
functional-summary.json
functional-results.tsv
<core-module>-<version>-sbom.cdx.json
<core-module>-<version>-sbom.cdx.xml
release-manifest.json
SHA256SUMS
```

`release-manifest.json` binds the candidate version and Git commit to every
payload checksum, qualification profile, supported container platform, image
repository, and immutable container digest. `SHA256SUMS` also covers the
manifest itself.

The root SBK distribution already contains the `sbk`, `sbk-yal`, `sbk-gem`,
and `sbk-gem-yal` launchers. SBM and the Local Web Console have separate
standalone distributions.

No `drivers:*` project is published as a standalone archive, Maven package, or
CycloneDX project. The guarded workflow calls `releaseStageCorePublications`
for local JReleaser staging and `releasePublishCoreToGitHubPackages` for GitHub
Packages. Both use the same explicit allow-list that excludes `drivers/`; the
scope verifier also fails if a driver ever applies `maven-publish`. Drivers
remain runtime plugins inside the SBK distribution and container. Direct
CycloneDX release files are generated only for the publishable core modules:
PerL, SBK API, SBK Web Console transport, SBM, SBK-YAL, SBK-GEM, and
SBK-GEM-YAL. `sbk-api` and `sbk-web-console` are included because they are
required compile/runtime dependencies of the public SBK API used by external
applications. BuildKit separately generates an image SBOM from the complete
container filesystem, including the driver libraries actually bundled there.

## Container publication

The workflow validates native `linux/amd64` and `linux/arm64` images before
publishing one multi-architecture manifest to:

```text
kmgowda/sbk
ghcr.io/kmgowda/sbk
```

Every version receives the exact version tag. Stable releases also update the
major-version and `latest` tags; prereleases never update stable aliases. The
workflow verifies both registries against the Buildx digest, emits BuildKit
SBOM/provenance attestations, and signs both immutable repository digests with
Cosign and GitHub OIDC.

## Authentication

GitHub workflow dispatch and the internal GitHub Packages publication resolve
the GitHub identity from these sources:

1. Gradle properties `github.username` and `github.token`, when Gradle is run
   directly;
2. `GITHUB_USER` and `GITHUB_TOKEN` environment variables;
3. the legacy `GITHUB_USERNAME` variable for compatibility; or
4. workflow-specific `RELEASE_GITHUB_USER` and `RELEASE_GITHUB_TOKEN` secrets,
   falling back to the workflow actor and repository `GITHUB_TOKEN`.

Docker Hub is a different authentication service; a GitHub token cannot be
used there. Configure either `DOCKERHUB_USER`/`DOCKERHUB_TOKEN`, the existing
`DOCKERHUB_USERNAME`/`DOCKERHUB_TOKEN` pair, or the generic
`RELEASE_DOCKER_USER`/`RELEASE_DOCKER_TOKEN` repository secrets.

JReleaser does not use the GitHub credentials above for Maven Central. Local
invocations resolve its private TOML configuration from Gradle property
`jreleaser.configFile`, environment variable `JRELEASER_CONFIG_FILE`, or the
existing default `~/.jreleaser/config.toml`, in that order. For GitHub Actions,
store the complete private TOML as the `JRELEASER_CONFIG_TOML` repository
secret; the workflow writes it to a mode-0600 temporary file and sets
`JRELEASER_CONFIG_FILE` before running JReleaser. GitHub credentials are not
exported to the JReleaser steps; they are scoped only to the subsequent
GitHub Packages publication step.

The release environment must also provide the real-host qualification secrets
documented in [Release qualification](RELEASE_QUALIFICATION.md).

## Publishing a release

Dispatch the complete guarded release from Gradle:

```bash
GITHUB_TOKEN=<actions-write-token> ./gradlew publish \
  -Pprofile=release \
  -PreleaseConfirm=RELEASE-10.5 \
  --no-daemon
```

The confirmation value must exactly match `RELEASE-<sbkVersion>`. The task
always sends `dry_run=false` and dispatches `.github/workflows/release.yml`
from `master`; therefore, it performs an actual release rather than a local
dry run. Use `-Pgithub.token=<token>` instead of `GITHUB_TOKEN` when Gradle
property-based authentication is preferred. This token authorizes only the
GitHub Actions workflow dispatch; JReleaser still obtains its Maven Central
configuration and credentials exclusively from the custom TOML file.

Optional release controls are:

```text
-PreleasePrerelease=true
-PreleaseResume=true
```

Both default to `false`. The task submits the following workflow inputs:

```text
version=<exact sbkVersion from gradle.properties>
profile=release
dry_run=false
prerelease=false
resume=false
```

The same inputs remain available through **Actions > SBK Release > Run
workflow** when a browser-driven release is required.

The workflow runs the authoritative real-host release gate, performs a local
Maven publication dry run, validates both container architectures, publishes
the image to Docker Hub and GHCR, stages only core packages with
`releaseStageCorePublications`, deploys them to Sonatype Central with
`jreleaserDeploy`, invokes `releasePublishCoreToGitHubPackages` for GitHub
Packages, creates an annotated tag, creates a draft GitHub Release,
uploads and compares every asset, and publishes the release last.

If publication stops after the exact tag or draft release is created, inspect
the partial state before rerunning with `resume=true`. Resume is accepted only
for the same tag and commit, and existing assets must be byte-identical.

Configure the `JRELEASER_CONFIG_TOML` release-environment secret with the same
signing and Sonatype configuration used by the existing local JReleaser file.
The workflow sets `JRELEASER_MAVENCENTRAL_STAGE=FULL`; the source default
remains `UPLOAD` so an ordinary maintainer invocation retains the current
inspect-before-publish behavior.

The published coordinates remain `io.github.kmgowda.sbk:<module>:<version>`.
External projects can therefore build custom storage benchmarks without an
SBK source checkout, for example with `sbk-api`, `sbk-gem`, or `sbm` from Maven
Central. The same coordinates may be resolved from GitHub Packages after
adding its repository and credentials. See the
[SBK examples repository](https://github.com/kmgowda/sbk-examples).

<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# SBK release publication

SBK uses one guarded GitHub Actions workflow for GitHub Release assets,
GitHub Packages, Docker Hub, and the GitHub Container Registry. Publication
is independent from qualification. `releasecheck` can prove a candidate, but
`.github/workflows/release.yml` neither invokes it nor requires its output.
Maven Central publication is an
independent maintainer operation performed with the established local
JReleaser configuration; the GitHub workflow never configures or invokes
JReleaser. The legacy Gradle `-Pmaven` repository path is not used.

## Safe local dry run

The following command exercises every core-module Maven `publish` task against
project-local repositories, validates
the exact main/source/Javadoc/documentation artifact set that JReleaser will
deploy, assembles the container context, and creates the complete release asset
contract:

```bash
./gradlew clean releasePublicationDryRun \
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
daemon. The GitHub release workflow never runs or consumes `releasecheck`.
Open **Actions > SBK Release > Run workflow**, enter the version, and keep
`dry_run=true` to build the complete asset contract and execute native AMD64
and ARM64 images without performing a registry login or push.

The local dry run and GitHub workflow intentionally do not invoke
`jreleaserDeploy`. JReleaser requires the maintainer's private signing and
Sonatype credentials and remains a separate Maven Central publication step.

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
<core-module>-<version>-sbom.cdx.json
<core-module>-<version>-sbom.cdx.xml
release-manifest.json
SHA256SUMS
```

`release-manifest.json` binds the version and Git commit to every payload
checksum, supported container platform, image repository, and immutable
container digest. `SHA256SUMS` also covers the manifest itself.

The root SBK distribution already contains the `sbk`, `sbk-yal`, `sbk-gem`,
and `sbk-gem-yal` launchers. SBM and the Local Web Console have separate
standalone distributions.

No `drivers:*` project is published as a standalone archive, Maven package, or
CycloneDX project. The guarded workflow calls
`releasePublishCoreToGitHubPackages` for GitHub Packages. Independent local
JReleaser publication uses `releaseStageCorePublications`. Both paths use the
same explicit allow-list that excludes `drivers/`; the
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

The AMD64 validation job also reports HIGH and CRITICAL Trivy findings. This
scan is advisory because the aggregate image intentionally bundles every
enabled driver's third-party SDK; findings must be assessed and remediated per
driver without blocking publication of unrelated benchmark backends. Native
image builds, launcher/version checks, and the real File benchmark remain
mandatory for both architectures.

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

JReleaser does not use the GitHub credentials above for Maven Central and is
never invoked by the GitHub workflow. Local invocations resolve its private
TOML configuration from Gradle property
`jreleaser.configFile`, environment variable `JRELEASER_CONFIG_FILE`, or the
existing default `~/.jreleaser/config.toml`, in that order. Keep this private
configuration on the authorized Maven Central publication host; it is not a
GitHub Actions secret.

The private TOML supplies JReleaser credentials and environment values. The
root Gradle release configuration remains authoritative for the staging
repository inventory and always registers PerL, SBK Web Console, SBK API,
SBM, SBK-YAL, SBK-GEM, and SBK-GEM-YAL. Module Maven publication stages
unsigned artifacts; JReleaser is the sole component that signs the Maven
Central payload. Legacy Gradle signing properties must not create
`signMavenJavaPublication` tasks.

Release qualification may independently use the real-host secrets documented
in [Release qualification](RELEASE_QUALIFICATION.md). The publication workflow
does not receive or require remote benchmark credentials.

## Publishing a release

Dispatch the complete guarded release from Gradle:

```bash
GITHUB_TOKEN=<actions-write-token> ./gradlew publish \
  -PreleaseConfirm=RELEASE-10.5 \
  --no-daemon
```

The confirmation value must exactly match `RELEASE-<sbkVersion>`. The task
always sends `dry_run=false` and dispatches `.github/workflows/release.yml`
from `master`; therefore, it performs an actual release rather than a local
dry run. Use `-Pgithub.token=<token>` instead of `GITHUB_TOKEN` when Gradle
property-based authentication is preferred. This token authorizes only the
GitHub Actions workflow dispatch and GitHub-owned publication steps.

Optional release controls are:

```text
-PreleasePrerelease=true
-PreleaseResume=true
```

Both default to `false`. The task submits the following workflow inputs:

```text
version=<exact sbkVersion from gradle.properties>
dry_run=false
prerelease=false
resume=false
```

The same inputs remain available through **Actions > SBK Release > Run
workflow** when a browser-driven release is required.

The workflow builds the complete contracted asset set without running or
depending on `releasecheck`, validates both container architectures, publishes
the image to Docker Hub and GHCR, invokes
`releasePublishCoreToGitHubPackages` for GitHub Packages, creates an annotated
tag, creates a draft GitHub Release, uploads and compares every asset, and
publishes the release last with the versioned details from
`.github/release-notes/<version>.md`. It does not configure or execute
JReleaser.

If publication stops after the exact tag or draft release is created, inspect
the partial state before rerunning with `resume=true`. Resume is accepted only
for the same tag and commit, and existing assets must be byte-identical.

Publish the Maven Central coordinates independently from an authorized host:

```bash
./gradlew verifyCorePublicationStaging jreleaserConfig --no-daemon
JRELEASER_MAVENCENTRAL_STAGE=FULL ./gradlew jreleaserDeploy --no-daemon
```

JReleaser resolves the private configuration described above. Its source
default remains `UPLOAD`; setting the stage to `FULL` is the explicit Central
release operation.

The published coordinates remain `io.github.kmgowda.sbk:<module>:<version>`.
External projects can therefore build custom storage benchmarks without an
SBK source checkout, for example with `sbk-api`, `sbk-gem`, or `sbm` from Maven
Central. The same coordinates may be resolved from GitHub Packages after
adding its repository and credentials. See the
[SBK examples repository](https://github.com/kmgowda/sbk-examples).

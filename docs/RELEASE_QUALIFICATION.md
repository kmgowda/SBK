<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# SBK Release Qualification

SBK has one authoritative release gate. It builds clean distributions, runs
the normal verification suite, exercises every logger family through the SBK
suite and standalone service launchers, verifies PerL concurrency, checks
generated API documentation, and records the commit and archive checksums that
were qualified.

The normal `./gradlew check` lifecycle remains the compile, static-analysis,
and unit-test gate. It intentionally does not start installed applications or
external services. Run `./gradlew releaseFunctionalTest -Pprofile=local` for
only the black-box JVM harness, or `releasecheck` for the complete ordered
release gate.

## Profiles

Use the `local` profile while developing:

```bash
./gradlew clean releasecheck \
  -Pprofile=local \
  --no-daemon --rerun-tasks
```

The `local-docker` profile adds fully automatic SBK-GEM and SBK-GEM-YAL
testing against two disposable local Docker SSH/JDK nodes. The `ci` profile adds
Maven-local publication verification. The `release` profile additionally runs
the JMH performance gates and requires real remote SBK-GEM hosts:

```bash
./gradlew clean releasecheck \
  -Pprofile=local-docker \
  --no-daemon --rerun-tasks
```

```bash
./gradlew clean releasecheck \
  -Pprofile=release \
  -PreleaseInventory=/secure/sbk-release-inventory.properties \
  --no-daemon --rerun-tasks
```

The release profile never treats unavailable mandatory infrastructure as a
successful skip. Missing inventory, hosts, credentials, or required tools fail
the gate.

The `SBK Release Qualification` GitHub Actions workflow provides the same
entry point. Its `ci` and `local-docker` profiles use a GitHub-hosted runner.
Its `release` profile requires a self-hosted Linux runner labeled
`sbk-release`, because that machine must have network and SSH access to the
private GEM inventory.

## Execution examples

### Local development gate

Use this command for a clean local checkout. It runs all builds, enabled-driver
checks, concurrency tests, packaging, documentation checks, and local
functional tests. It does not run the remote GEM or release-only performance
tests. The functional inventory verifies both fixed-record idle termination and
that timed runs ignore the fixed-record idle deadline.

```bash
./gradlew clean releasecheck \
  -Pprofile=local \
  --no-daemon --rerun-tasks
```

During development, use the clean-tree override to qualify uncommitted code:

```bash
./gradlew clean releasecheck \
  -Pprofile=local \
  -PreleaseRequireCleanTree=false \
  --no-daemon --rerun-tasks
```

Do not use this override as release evidence. A local-profile result verifies
the local functional scope but does not qualify a release candidate.

The functional harness is a dedicated JUnit source set executed by Gradle. It
uses JDK process, timeout, HTTP, socket, and file APIs on both macOS and Linux;
it does not require Bash, `curl`, GNU `timeout`, or Homebrew `gtimeout`.

### Local gate with automatic GEM

Use `local-docker` when a developer or ordinary GitHub-hosted runner must test
the complete GEM orchestration path without provisioning a permanent remote
host:

```bash
./gradlew clean releasecheck \
  -Pprofile=local-docker \
  --no-daemon --rerun-tasks
```

Every pull request runs the focused `releasePreflight releaseFunctionalTest`
portion of this profile as the `SBK-GEM two-node functional test` CI job. It
uploads its functional report and JUnit evidence even when the test fails. The
full `releasecheck` profile and private real-host release inventory remain
separate, stronger qualification gates.

Requirements are Docker with an accessible daemon and the OpenSSH
`ssh-keygen` client tool. The fixture runs SBK-GEM and SBK-GEM-YAL in a
dedicated Linux controller container, so Docker's Linux SSH nodes satisfy the
normal homogeneous-deployment contract even when Gradle itself runs on macOS.
The controller and nodes share an isolated Docker network; the controller owns
the disposable SSH agent and callback address, so the test does not depend on
host SSH-agent sockets, published node ports, or Docker Desktop host aliases.
The gate:

1. builds a JDK 25 fixture image;
2. creates an ephemeral Ed25519 key and isolated Docker network;
3. starts a Linux SBK-GEM controller and two non-root Linux SSH nodes on that
   network;
4. starts an SSH agent in the controller, verifies both generated
   `known_hosts` entries, and verifies the homogeneous remote platform/tool
   preflight;
5. runs `GemPrometheusLogger`, `GemWebLogger`, and SBK-GEM-YAL through the
   immutable SBK-plus-JDK archive creation, SHA-256 verified copy, atomic
   activation/reuse, managed-runtime lease and inactive-version cleanup,
   cache integrity, two-client coordinated launch, SBM callback, aggregation,
   and cleanup paths, requiring two successful nodes
   and two SBM registrations;
   the timed Prometheus case uses `-totalrecords`, the Web case combines
   per-client `-records` with `-totalthroughput`, and the fixed-count YAL case
   combines `totalrecords` with `totalthroughput`;
6. force-removes the controller, both nodes, their private network, and
   ephemeral credentials on both success and failure.

No inventory, persistent SSH key, exposed fixed port, or manual container
setup is needed. The fixture proves two-node fan-out, coordinated SBM startup,
aggregation, and protocol contracts on one Docker host. It does not replace
qualification across real hosts, networks, and storage backends in the
`release` profile.

### CI gate

The CI profile adds Maven main, source, Javadoc JAR, and POM generation checks.
It does not publish artifacts or require signing credentials.

```bash
./gradlew clean releasecheck \
  -Pprofile=ci \
  --no-daemon --rerun-tasks
```

### Complete release-candidate gate

Use a clean checkout and a private inventory containing real SSH-accessible
hosts:

```bash
./gradlew clean releasecheck \
  -Pprofile=release \
  -PreleaseInventory=/root/.config/sbk/release-inventory.properties \
  --no-daemon --rerun-tasks
```

This is the only profile that runs the release-only JMH checks and uses the
private real-host inventory. Both `local-docker` and `release` execute
SBK-GEM and SBK-GEM-YAL, but only `release` supplies production-like remote
evidence. A valid release result ends with output similar to:

```text
PASS: sbk-gem-GemPrometheusLogger
PASS: sbk-gem-GemWebLogger
PASS: sbk-gem-yal-release
SBK 10.6 release qualification: QUALIFIED
BUILD SUCCESSFUL
```

## Release inventory

Keep the inventory outside the repository. It contains locations, not secret
defaults. SSH keys and passphrases remain in the SSH agent or environment.

```properties
gem.nodes=sbk-node-1,sbk-node-2
gem.user=sbk
gem.knownHosts=/secure/sbk-release-known-hosts
```

`gem.knownHosts` is a path on the qualification runner, not the file contents.
The nodes must permit non-interactive SSH, reach the selected backend, and
connect back to the embedded SBM endpoint. SBK-GEM performs its normal version,
content-addressed SBK/Java deployment, execution, aggregation, and cleanup checks.

### Single-host GEM qualification

A single host is sufficient to automate the SSH deployment, remote execution,
SBM/gRPC aggregation, GEM logger, and GEM-YAL contracts. It does not validate
multi-host fan-out or aggregation across nodes.

To use a separate host:

```properties
gem.nodes=10.118.232.92
gem.user=sbk-release
gem.knownHosts=/home/sbk-release/.ssh/known_hosts
```

To use the qualification machine itself, run an SSH server and use loopback:

```properties
gem.nodes=127.0.0.1
gem.user=sbk-release
gem.knownHosts=/home/sbk-release/.ssh/known_hosts
```

Verify non-interactive access before starting the six-minute-or-longer gate:

```bash
ssh -o BatchMode=yes sbk-release@127.0.0.1 java -version
```

The SSH host key must already be verified in the configured `known_hosts`
file. Keep the private key on the qualification host or in its SSH agent; do
not add it to the inventory or repository.

### Automated GitHub Actions release gate

Register an isolated self-hosted Linux runner with these labels:

```text
self-hosted
linux
sbk-release
```

Configure these repository Actions secrets. The known-hosts secret is a path
on the self-hosted runner, not the file contents:

```text
SBK_RELEASE_GEM_NODES=127.0.0.1
SBK_RELEASE_GEM_USER=sbk-release
SBK_RELEASE_GEM_KNOWN_HOSTS=/home/sbk-release/.ssh/known_hosts
```

Open **Actions > SBK Release Qualification > Run workflow**, select the
`release` profile, and run it. The workflow creates the inventory under
`RUNNER_TEMP` and invokes the same complete release command shown above. After
the one-time runner and SSH setup, SBK-GEM and SBK-GEM-YAL require no manual
launcher commands.

Qualification does not itself create a tag or publish a release. After the
candidate qualifies, follow [Release publication](RELEASE_PUBLICATION.md) for
the non-mutating publication dry run and the guarded GitHub Release, GitHub
Packages, Docker Hub, and GHCR workflow.

## Automated functional coverage

The black-box harness runs binaries from the generated install trees and
checks:

- launcher presence for the complete suite, version output for SBK, SBK-YAL,
  SBM, SBK-GEM, and SBK-GEM-YAL, and the Local Web Console health contract;
- deterministic File writes with `SystemLogger`, `Sl4jLogger`, `CSVLogger`,
  `PrometheusLogger`, and `WebLogger`;
- CSV header/total output, Prometheus HTTP metrics, Web Console health/run
  registration, default board naming, and logger lifecycle messages;
- File EOF termination before the configured duration;
- fixed-record SBK and standalone SBM failure after a one-second performance-event idle deadline;
- invalid storage, missing gRPC host, missing YML, and malformed YML failures;
- SBK-YAL argument mapping and logger overrides;
- real `GrpcLogger` clients against child SBM processes using both
  `SbmPrometheusLogger` and `SbmWebLogger`; and
- both GEM logger families plus SBK-GEM-YAL when either the automatic
  `local-docker` fixture or the release inventory is selected, including
  aggregate `-totalrecords` and `-totalthroughput` distribution, existing
  per-client `-records`, and combined fixed aggregate record/throughput control.

All child processes have bounded startup and execution times. Logs and a test
summary are written below `build/reports/release-qualification/`; Gradle also
writes standard JUnit XML and HTML results for `releaseFunctionalTest`.

## Configuration

Safe harness defaults live in
`gradle/release-qualification.properties`. Override them with Gradle properties
such as `-PreleaseRecords=50000`. These settings configure only the test
harness; module runtime defaults remain in their existing authoritative
properties files.

The aggregate-throughput GEM cases default to 100 MB/s. Override that harness
input with `-PreleaseTotalThroughputMBPerSec=<value>` when the release fixture
needs a different rate.

The same file is the single source for child-process shutdown grace periods,
smoke and EOF workloads, SBM settling time, Docker node count and SSH
readiness, the pinned Docker JDK image, fixture SSH user/port and host alias,
socket connection timeout, and report-generation limits. Gradle passes those
resolved values to the JVM harness and Docker build, so the Java and container
sources do not maintain a second set of fallback defaults. Use the matching
Gradle property name to override one value, for example:

```bash
./gradlew releasecheck \
  -Pprofile=local-docker \
  -PreleaseDockerNodeCount=2 \
  -PreleaseSmokeBenchmarkSeconds=10
```

`releaseRequireCleanTree` defaults to `true`. The `local` and `local-docker`
profiles reject modifications to tracked files but ignore untracked files. The
`ci` and `release` profiles require a completely clean tree, including no
untracked files. A developer may disable the check for a local diagnostic run,
but a release candidate must use the default:

```bash
./gradlew releaseFunctionalTest \
  -Pprofile=local \
  -PreleaseRequireCleanTree=false
```

## Results and release rule

Successful qualification creates:

```text
build/reports/release-qualification/
├── qualification.json
├── functional-summary.json
├── functional-results.tsv
└── logs/
```

The **SBK Release Qualification** GitHub workflow uploads these files as an
immutable artifact named
`sbk-release-qualification-<commit>-<profile>` for independent inspection and
retention. Release publication neither consumes this artifact nor invokes
`releasecheck`; qualification and publication have separate lifecycles.

`qualification.json` contains the version, Git commit, profile, Java runtime,
and SHA-256 checksum of every application ZIP/TAR. A release is qualified only
when the command exits zero, no mandatory test was skipped, and the published
artifacts have the recorded checksums.

Inspect the functional and qualification results with:

```bash
cat build/reports/release-qualification/functional-summary.json
grep 'sbk-gem' build/reports/release-qualification/functional-results.tsv
cat build/reports/release-qualification/qualification.json
```

Release evidence must contain both `"status": "QUALIFIED"` and
`"profile": "release"`. A `local-docker` report is strong automated
functional evidence, but is not final release evidence. Entries named
`sbk-gem-external` or `sbk-gem-yal-external` mean that the local or CI profile
was used; they are not remote GEM results.

Common preflight failures are intentional:

- `Release qualification requires no uncommitted tracked changes` means the
  `local` or `local-docker` profile found a modified tracked file.
- `Release qualification requires a clean Git tree` means the `ci` or
  `release` profile found a tracked modification or an untracked file.
- `Release inventory does not exist` means the example inventory path was
  used without creating the file.
- `Release known-hosts file does not exist` means `gem.knownHosts` does not
  identify a file on the qualification machine.
- `The local-docker profile requires an accessible Docker daemon` means the
  Docker client cannot reach its daemon with the current user's permissions.
- SSH authentication or host-key failures must be corrected outside the
  repository; the release gate does not disable host-key verification.

The deterministic gate validates the SBK framework and enabled-driver build.
Runtime certification of every enabled driver still requires its actual
backend. Do not claim all-driver runtime certification unless each enabled
driver has a recorded backend result.

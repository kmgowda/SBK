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

## Profiles

Use the `local` profile while developing:

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=local \
  --no-daemon --rerun-tasks
```

The `local-docker` profile adds fully automatic SBK-GEM and SBK-GEM-YAL
testing against a disposable local Docker SSH/JDK node. The `ci` profile adds
Maven-local publication verification. The `release` profile additionally runs
the JMH performance gates and requires real remote SBK-GEM hosts:

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=local-docker \
  --no-daemon --rerun-tasks
```

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=release \
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
tests.

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=local \
  --no-daemon --rerun-tasks
```

During development, use the clean-tree override to qualify uncommitted code:

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=local \
  -PreleaseRequireCleanTree=false \
  --no-daemon --rerun-tasks
```

Do not use this override as release evidence. A local-profile result verifies
the local functional scope but does not qualify a release candidate.

### Local gate with automatic GEM

Use `local-docker` when a developer or ordinary GitHub-hosted runner must test
the complete GEM orchestration path without provisioning a permanent remote
host:

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=local-docker \
  --no-daemon --rerun-tasks
```

Requirements are Docker with an accessible daemon and the OpenSSH client
tools `ssh`, `ssh-agent`, `ssh-add`, `ssh-keygen`, and `ssh-keyscan`. The gate:

1. builds a JDK 25 fixture image;
2. creates an ephemeral Ed25519 key and isolated SSH agent;
3. starts one non-root SSH node with a dynamically assigned loopback port;
4. verifies the generated `known_hosts` entry and remote Java runtime;
5. runs `GemPrometheusLogger`, `GemWebLogger`, and SBK-GEM-YAL through the
   normal copy, remote launch, SBM callback, aggregation, and cleanup paths;
6. force-removes the container, SSH agent, and ephemeral credentials on both
   success and failure.

No inventory, persistent SSH key, exposed fixed port, or manual container
setup is needed. The fixture is intentionally a single host. It proves GEM's
deployment and protocol contracts, but it does not replace real multi-host
fan-out, network, and backend qualification in the `release` profile.

### CI gate

The CI profile adds Maven main, source, Javadoc JAR, and POM generation checks.
It does not publish artifacts or require signing credentials.

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=ci \
  --no-daemon --rerun-tasks
```

### Complete release-candidate gate

Use a clean checkout and a private inventory containing real SSH-accessible
hosts:

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=release \
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
SBK 10.5 release qualification: QUALIFIED
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
copy, Java, execution, aggregation, and cleanup checks.

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
- invalid storage, missing gRPC host, missing YML, and malformed YML failures;
- SBK-YAL argument mapping and logger overrides;
- real `GrpcLogger` clients against child SBM processes using both
  `SbmPrometheusLogger` and `SbmWebLogger`; and
- both GEM logger families plus SBK-GEM-YAL when either the automatic
  `local-docker` fixture or the release inventory is selected.

All child processes have bounded startup and execution times. Logs and a test
summary are written below `build/reports/release-qualification/`.

## Configuration

Safe harness defaults live in
`gradle/release-qualification.properties`. Override them with Gradle properties
such as `-PreleaseRecords=50000`. These settings configure only the test
harness; module runtime defaults remain in their existing authoritative
properties files.

`releaseRequireCleanTree` defaults to `true`. A developer may disable it for a
local diagnostic run, but a release candidate must use the default:

```bash
./gradlew releaseFunctionalTest \
  -PreleaseProfile=local \
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

- `Release qualification requires a clean Git tree` means tracked or
  untracked files remain in the checkout.
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

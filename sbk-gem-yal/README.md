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

# SBK-GEM-YAL: distributed YML launcher

SBK-GEM-YAL loads a YML description and delegates to SBK-GEM. It combines declarative configuration with the same SSH orchestration, embedded SBM aggregation, remote SBK processes, drivers, and loggers used by direct SBK-GEM execution.

## Build

```bash
./gradlew :sbk-gem-yal:check
./gradlew installDist
```

Use the root distribution because it supplies both the dedicated YAL/GEM
launchers and the complete enabled-driver dependency graph deployed remotely.

## Run

```bash
./build/install/sbk/bin/sbk-gem-yal -help
```

Use the example YML files in this module as structural references, then replace hosts, authentication, remote paths, and backend arguments for the benchmark environment.

The checked-in example intentionally omits `gempass`. Passwordless public-key
authentication is preferred: SBK-GEM uses the launching user's SSH agent and
OpenSSH-configured key files. Supply `gempass` only when the remote account
requires password authentication, or set `SBK_GEM_SSH_PASSWD` outside the YML
file. The remote host key must already be trusted in that user's
`~/.ssh/known_hosts` file only when `hostkeycheck` is enabled. Set
`knownhosts` to use a dedicated trust file. `hostkeycheck` defaults to `false`
for unattended passwordless deployment. Set it to `true` to enable accept-new
trust-on-first-use checking; a previously unknown key is recorded and a changed
key is rejected. Supplying `gempass` or `SBK_GEM_SSH_PASSWD` always bypasses
host-key checking, matching direct SBK-GEM behavior.

## Security and reproducibility

- Do not commit private keys, passwords, tokens, or production host inventories.
- Restrict permissions on external connection files.
- When host-key verification is required, set `hostkeycheck: true`, verify newly recorded fingerprints independently,
  and use `knownhosts` when a dedicated trust file is needed.
- Ensure remote clients can reach the embedded SBM callback host and port.
- Record the YML file, SBK commit, remote Java version, backend version, and host topology with results.

## Code map

- `io.gem.main.SbkGemYalMain`: executable entry point.
- `io.gem.api.impl.SbkGemYal`: loads YML and delegates to SBK-GEM.
- `io.gem.params.impl.SbkGemYmlMap`: maps YML fields into GEM arguments.

See [SBK-GEM](../sbk-gem/README.md) for orchestration behavior and the [distributed code flow](../docs/ARCHITECTURE.md#distributed-flow).

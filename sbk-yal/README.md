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

# SBK-YAL: YML Arguments Loader

SBK-YAL loads a YML benchmark description and delegates to the normal SBK bootstrap. It is a configuration adapter, not a separate benchmark engine: driver discovery, CLI validation, lifecycle, timing, and results are the same as direct SBK execution.

## Build

```bash
./gradlew :sbk-yal:check
./gradlew installDist
```

Use the root distribution because it assembles the enabled storage drivers and
registers the dedicated `sbk-yal` launcher.

## Run

The repository includes example files in this module. Display current options first:

```bash
./build/install/sbk/bin/sbk-yal -help
```

Then run a YML definition using the file option shown by that help output. YML keys are mapped by `io.sbk.params.impl.SbkYmlMap` into ordinary SBK arguments and passed to `Sbk.run(...)`.

## Behavior to understand

- The YML must select the same driver and supply the same required values as the equivalent CLI.
- Driver and logger options are validated by their normal parsers after mapping.
- A YML file should not contain credentials when it will be committed; use the backend's supported secure configuration mechanism.
- Relative backend paths are interpreted by the launched process, so state the working directory in automation.
- Use the direct CLI with `-class <driver> -help` to discover driver-specific option names.

## Code map

- `io.sbk.main.SbkYalMain`: entry point.
- `io.sbk.api.impl.SbkYal`: loads configuration and invokes SBK.
- `io.sbk.params.impl.SbkYmlMap`: YML-to-argument mapping.

See the [single-node architecture](../docs/ARCHITECTURE.md#single-node-bootstrap) and [root quick start](../README.md#run-a-benchmark).

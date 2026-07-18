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

# Linked blocking queue driver

`-class linkedbq` extends the in-process `ConcurrentQ` adapter with a linked blocking queue. It is intended for local queue and harness comparisons, not remote storage measurement.

```bash
./build/install/sbk/bin/sbk -class linkedbq -writers 1 -readers 1 -size 1024 -seconds 15
```

Blocking behavior is part of what this driver measures. Compare it only under identical JVM, worker, payload, and host settings. See [the driver guide](../../docs/DRIVER_GUIDE.md).

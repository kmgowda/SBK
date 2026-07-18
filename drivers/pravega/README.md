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

# Pravega driver

`-class pravega` benchmarks Pravega stream writers and readers. Options include controller URI, scope, stream, segment count, recreation behavior, and connection pooling; defaults are in `pravega.properties`.

```bash
./build/install/sbk/bin/sbk -class pravega -controller tcp://localhost:9090 -scope sbk -stream events -writers 1 -size 1024 -seconds 15
./build/install/sbk/bin/sbk -class pravega -controller tcp://localhost:9090 -scope sbk -stream events -readers 1 -size 1024 -seconds 15
```

Use a disposable scope/stream when recreation is enabled. Record segment count, scaling policy, acknowledgement behavior, and controller/segment-store topology. Run `-class pravega -help` for current options and see [the driver guide](../../docs/DRIVER_GUIDE.md).

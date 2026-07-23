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

# Concurrent queue variant driver

`-class conqueue` extends SBK's common in-process `ConcurrentQ` adapter with
PerL's non-blocking multiple-producer, single-consumer queue. It requires
exactly one reader, accepts one or more writers, and is intended for
queue/harness comparisons rather than external storage testing.

```bash
./build/install/sbk/bin/sbk -class conqueue -writers 1 -readers 1 -size 1024 -seconds 15
```

Use `-class conqueue -help` for current options and [the driver guide](../../docs/DRIVER_GUIDE.md) for shared semantics.

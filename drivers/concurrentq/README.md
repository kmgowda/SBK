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

# Concurrent queue driver

`-class concurrentq` benchmarks an in-process concurrent queue through the same SBK storage, writer, reader, and PerL paths used by external drivers. It is useful for correctness tests and measuring harness/queue behavior without a remote backend.

```bash
./build/install/sbk/bin/sbk -class concurrentq -writers 1 -readers 1 -size 1024 -seconds 15
```

The queue exists only inside the benchmark process. Results are host- and JVM-specific. See [the driver guide](../../docs/DRIVER_GUIDE.md).

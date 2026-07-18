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

# Atomic queue driver

`-class atomicq` is an in-process queue benchmark built on the common `ConcurrentQ` storage adapter with an atomic-queue implementation. It is useful for harness and queue comparisons; it does not measure an external storage service.

```bash
./build/install/sbk/bin/sbk -class atomicq -writers 1 -readers 1 -size 1024 -seconds 15
```

Results include JVM scheduling, allocation, and queue behavior on the local host. They are not a storage-network baseline. See [the driver guide](../../docs/DRIVER_GUIDE.md) and use `-class atomicq -help` for current options.

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

# Asynchronous file driver

`-class asyncfile` benchmarks local files through Java asynchronous file-channel APIs using `ByteBuffer` payloads. The driver supports `-file <path>` and creates per-worker readers or writers.

```bash
./build/install/sbk/bin/sbk -class asyncfile -file /tmp/sbk-async.bin -writers 1 -size 4096 -seconds 15
./build/install/sbk/bin/sbk -class asyncfile -file /tmp/sbk-async.bin -readers 1 -size 4096 -seconds 15
```

Use a non-production path and confirm file lifecycle behavior in `AsyncFile.java` before repeated runs. Run `-class asyncfile -help` for current options. Common driver design and verification are documented in [the driver guide](../../docs/DRIVER_GUIDE.md).

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

# Null driver

`-class null` performs local synthetic writer/reader work without an external storage system. It is useful for smoke tests and estimating harness/JVM overhead under a particular configuration.

Driver options include `-n`, the writer iteration-loop bound, and `-timeout`, the maximum per-worker timeout in milliseconds.

```bash
./build/install/sbk/bin/sbk -class null -writers 1 -size 1024 -seconds 15
```

Null-driver results are not storage results and should not be compared directly with durable backend operations. Use `-class null -help` for defaults.

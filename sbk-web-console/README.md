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

# SBK Web Console

`sbk-web-console` is the reusable Local Web Console runtime shared by SBK,
SBM, and SBK-GEM. It owns the HTTP server, asynchronous publishing client,
wire DTOs, bounded run histories, browser resources, and standalone entry
point. It does not depend on the SBK benchmark harness, PerL, SBM, or SBK-GEM.

The application-specific `WebLogger`, `SbmWebLogger`, and `GemWebLogger`
adapters remain in their owning modules and publish periodic summaries through
this module.

Build and run the standalone server:

```bash
./gradlew :sbk-web-console:installDist
./sbk-web-console/build/install/sbk-web-console/bin/sbk-web-console \
  -host 127.0.0.1 -port 9720 -minutes 180
```

Run its verification suite with:

```bash
./gradlew :sbk-web-console:check
```

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
this module. One server accepts multiple simultaneous benchmark runs on the
same port. Every run has an independent UUID, history, event stream, and lease,
and the browser run selector keeps their metrics separate.

Build and run the standalone server:

```bash
./gradlew :sbk-web-console:installDist
./sbk-web-console/build/install/sbk-web-console/bin/sbk-web-console \
  -port 9720 -websnapshotminutes 180 -webtimeoutminutes 1
```

The server binds to `0.0.0.0` and benchmark loggers print run URLs for `localhost`, loopback, hostname, and every
usable host IPv4 address at both benchmark start and completion. Remote browsers can use a printed hostname or IP
URL when routing and firewall rules permit. The service uses plain HTTP without authentication, so restrict it to a
trusted benchmark network or use an SSH tunnel.
`-webtimeoutminutes` sets the idle shutdown grace period in minutes and defaults to one minute.

When a WebLogger starts the server in the background, stdout and stderr are appended to
`$HOME/.sbk/logs/sbk-web-console-<port>.log`. The parent application reports whether it started this process or reused
an existing compatible process. The background log records WebLogger and browser/client connection counts and the
server's final exit reason.

Run its verification suite with:

```bash
./gradlew :sbk-web-console:check
```

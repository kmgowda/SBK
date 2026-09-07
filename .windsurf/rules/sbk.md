---
trigger: always_on
---

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

Read and follow `AGENTS.md` at the repository root before changing files. It
is the authoritative SBK repository policy.

Before editing a writer, reader, driver operation, callback, timestamp, queue,
latency recorder, measurement accumulator/forwarder, or SBM ingestion path,
read `docs/HOT_PATHS.md`. Treat H0, H1, and H2 files as sensitive. Do not add
branches, atomics/volatile state, locks, waits, allocations, bookkeeping,
clock reads, conversions, or dispatch layers without explicit human
confirmation for the exact proposed change.

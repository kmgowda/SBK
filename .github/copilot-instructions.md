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

# GitHub Copilot instructions for SBK

Read and follow `AGENTS.md` before proposing or applying repository changes.
It is the authoritative policy for architecture, safety, coding conventions,
permissions, and verification.

Before editing a writer, reader, driver operation, callback, timestamp, queue,
latency recorder, measurement accumulator, gRPC forwarding path, or SBM
ingestion path, read `docs/HOT_PATHS.md`. Treat every H0, H1, and H2 entry as
sensitive. Do not add branches, atomics or volatile state, memory fences,
locks, waits, allocations, bookkeeping, extra clock reads/conversions, or
dispatch layers without explicit human confirmation for that specific edit.
Do not treat a general feature request as that confirmation.

SBK-GEM is not a per-record measurement hot path, but its listed lifecycle
files are critical to bounded, correct distributed execution. Preserve
startup/shutdown deadlines, node/result attribution, and credential redaction.

---
description: Authoritative SBK repository instructions and task routing
alwaysApply: true
---

Read and follow `AGENTS.md` at the repository root before changing files. It is
the authoritative source for SBK architecture, safety constraints, coding
conventions, task-specific reading, and verification. Inspect the working tree
first and preserve unrelated changes. Use the relevant guide under `docs/` and
the nearest component or driver README. Do not infer permission to push, tag,
publish, rewrite history, change versions or licenses, or perform destructive
cleanup.

Before editing PerL, SBK reader/writer code, any driver operation, timestamp or
queue code, measurement recording/forwarding, or SBM ingestion, also read
`docs/HOT_PATHS.md`. Treat its H0/H1/H2 inventory as sensitive. Do not add a
branch, atomic/volatile operation, lock, wait, allocation, bookkeeping, clock
read, conversion, or dispatch layer there without explicit human confirmation
for the specific change.

# AI Agent Instructions for SBK Repository

> **Quick start for all AI agents.** Read this file first, then see AGENTS.md for complete details.

## What is SBK?

SBK (Storage Benchmark Kit) is a Java framework for benchmarking storage systems (S3, databases, message queues, file systems, etc.).

- **Language:** Java (JDK 25 REQUIRED)
- **Build:** Gradle (wrapper: `./gradlew`)
- **Structure:** 6 core modules + ~55 storage drivers
- **License:** Apache 2.0

## Before You Start

1. **Read AGENTS.md** - This is the main guide for all agents
2. **Check JDK version** - SBK requires JDK 25 (set SBK_JAVA_HOME or JAVA_HOME)
3. **Understand the build workflow** - See "Build commands" below

## Critical Conventions (Read This!)

### Build System
- New drivers MUST be added to **BOTH** `settings-drivers.gradle` AND `build-drivers.gradle`
- JDK 25 is required - build will fail with JDK 11 or other versions
- Use `./gradlew check` to verify changes
- Use `./gradlew installDist` to build distribution

### Code Style
- 4 spaces, no tabs
- All public methods need Javadoc with @param, @return, @throws
- Single-statement if blocks MUST have braces
- No synchronized blocks in driver hot paths (writeAsync/read)
- Lombok is available

### Common Gotchas
- Checkstyle/import-control.xml needs new top-level packages whitelisted
- Pathing JAR can get stale after dependency changes - clean rebuild needed
- halodb driver is disabled (GitHub Packages quota issue)
- MinIO SDK is pinned to 8.5.17 (not 9.x) for backend compatibility
- Drivers must handle InterruptedIOException as clean shutdown

## Build Commands

```bash
# Verify changes (fast for single module)
./gradlew :drivers:<name>:check

# Full project verification
./gradlew check

# Build distribution
./gradlew installDist

# Clean rebuild (if dependency changes or NoClassDefFoundError)
rm -rf build && ./gradlew clean :pathingJar installDist --rerun-tasks
```

## Driver Development Quick Reference

### Adding a new driver
1. Copy from `drivers/sbktemplate/`
2. Rename packages/classes (SbkTemplate → YourDriver)
3. Add vendor SDK to build.gradle
4. Add to settings-drivers.gradle: `include 'drivers:yourdriver'`
5. Add to build-drivers.gradle: `api project(':drivers:yourdriver')`
6. Update checkstyle/import-control.xml if new packages
7. Implement Storage<T>, Writer<T>, Reader<T>
8. Add README.md with examples
9. Verify: `./gradlew :drivers:yourdriver:check`, `./gradlew check`, `./gradlew installDist`

### Modifying an existing driver
1. Add flag in addArgs()
2. Add field to Config class
3. Add default to properties file
4. Parse in parseArgs()
5. Use the flag in openStorage/createWriter/createReader
6. Update README.md
7. Verify: `./gradlew :drivers:<name>:check`

## Verification Checklist

Before marking a task complete:
- [ ] ./gradlew check passes
- [ ] ./gradlew installDist succeeds
- [ ] No checkstyle violations
- [ ] Driver changes tested against real backend
- [ ] Documentation updated
- [ ] Both Gradle files updated for new drivers

## Documentation

- **AGENTS.md** - Main AI agent guide (READ THIS FIRST - comprehensive)
- **docs/AGENT_RECIPES.md** - Step-by-step recipes
- **docs/DRIVER_SPECIFICATION.md** - Driver spec template
- **docs/sbk-internals.md** - Architecture documentation
- **README.md** - User manual

## Agent-Specific Configurations

- **Devin:** See `.devin/skills/` for executable skills (sbk-driver-development, sbk-build-verify, sbk-benchmark-runner)
- **Cursor:** See `.cursorrules` for Cursor-specific rules
- **Aider:** See `.aider.conf.yml` for Aider configuration
- **All others:** Start with AGENTS.md (this is the universal entry point)

## Performance Benchmarking

```bash
# Basic benchmark
./build/install/sbk/bin/sbk -class <driver> -writers N -size <bytes> -seconds <duration>

# Example: MinIO with 8 writers, 1 MiB records, 60 seconds
./build/install/sbk/bin/sbk -class minio -writers 8 -size 1048576 -seconds 60
```

## Out of Scope (Requires User Approval)

- git push, git tag, or remote operations
- Modifying license headers or LICENSE file
- Changing SBK version
- Adding new top-level Gradle subprojects (new drivers are fine)
- Re-enabling halodb
- Upgrading MinIO SDK from 8.5.17
- Force-pushing or rewriting history

## Architecture Invariants

- PerL hot path is lock-free - don't add synchronization in drivers
- No sampling in measurement - every operation is timed
- Harness already times operations - don't add System.nanoTime() in drivers
- Shutdown is asynchronous - handle InterruptedIOException gracefully

## Need More Detail?

**Read AGENTS.md** - It contains:
- Complete module map and architecture
- Detailed build/run/verify instructions
- All 8 common gotchas with explanations
- Repository conventions (file structure, code style)
- Two AI workflows (vibe coding vs spec-driven)
- Out-of-scope actions list
- Quick agent self-check questions

**Then read docs/AGENT_RECIPES.md** for step-by-step procedures:
- Recipe 1: Add a new storage driver
- Recipe 2: Modify an existing driver
- Recipe 5: Debug a driver that fails at runtime
- Recipe 8: Run a benchmark against a new cluster

**Then read docs/DRIVER_SPECIFICATION.md** for formal driver development:
- Fillable spec template
- Worked example (MinIO driver)
- Acceptance checklist

## Summary

1. Read AGENTS.md first
2. Check JDK version (must be 25)
3. Follow the build workflow
4. Remember the critical conventions (dual Gradle files, checkstyle, no sync in hot path)
5. Use the verification checklist
6. Consult agent-specific configs if available

# SBK Build and Verify Skill

> Helps AI agents build and verify changes in the SBK repository with the correct JDK and workflow.

## When to use this skill

Invoke this skill when:
- Making any change to SBK code (drivers, core modules, documentation)
- Need to verify that a change doesn't break the build
- Running the standard SBK verification sequence
- Troubleshooting build failures

## What this skill provides

### Context
- SBK's JDK 25 requirement and SBK_JAVA_HOME environment variable
- The correct build and verification sequence for SBK
- Common build failure modes and their fixes
- Pathing JAR staleness issue and resolution
- Gradle wrapper usage and project structure

### Guidance
- Step-by-step verification workflow
- How to handle JDK version mismatches
- When to clean rebuild vs incremental build
- How to fix the pathing JAR staleness issue
- How to interpret build errors

### Permissions granted
- Exec permissions: `./gradlew`, `java` (for running SBK binaries)
- Read access to: `build/`, `gradle/`, all source directories
- Write access to: `build/` (for clean operations)

## Build and verify workflow

### Standard verification sequence

For any change, run this sequence in order:

```bash
# 1. Module-level check (fastest for driver changes)
./gradlew :drivers:<name>:check

# 2. Full project check
./gradlew check

# 3. Build distribution
./gradlew installDist

# 4. Smoke test (for driver changes)
./build/install/sbk/bin/sbk -class <name> -writers 1 -size 1024 -seconds 30
```

### When to clean rebuild

Run a clean rebuild when:
- Dependency versions changed (especially driver vendor SDKs)
- Seeing `NoClassDefFoundError` for classes that clearly exist in `build/install/sbk/lib/`
- Pathing JAR appears stale (manifest doesn't include new dependencies)

```bash
rm -rf build && ./gradlew clean :pathingJar installDist --rerun-tasks
```

### JDK requirement

SBK requires **JDK 25**. The build will fail with JDK 11 or other versions.

**Set JDK 25** using one of these methods:
```bash
# Option 1: SBK_JAVA_HOME (recommended)
export SBK_JAVA_HOME=/path/to/jdk-25

# Option 2: JAVA_HOME
export JAVA_HOME=/path/to/jdk-25
```

**Verify Java version**:
```bash
java -version  # Should show openjdk version "25.x.x"
```

**Error if wrong version**:
```
ERROR: SBK requires JDK 25, but found JDK 11
```

## Common build failures

### NoClassDefFoundError after dependency change

**Symptom**: `java.lang.NoClassDefFoundError: com/some/class` even though the JAR exists in `build/install/sbk/lib/`

**Cause**: Pathing JAR manifest is stale; Gradle incremental build didn't update the `Class-Path:` entry

**Fix**:
```bash
rm -rf build && ./gradlew clean :pathingJar installDist --rerun-tasks
```

### Checkstyle import control failure

**Symptom**: `checkstyleMain` fails with "Illegal import" or similar

**Cause**: New dependency introduced a top-level package not in the allow-list

**Fix**: Add the package to `checkstyle/import-control.xml`:
```xml
<allow pkg="new.package.name" />
```

### halodb driver build failure

**Symptom**: `Could not find com.oath.halodb:halodb:0.5.6` with 403/405 from GitHub Packages

**Cause**: GitHub Packages bandwidth quota exceeded or invalid credentials

**Status**: halodb driver is currently commented out in `settings-drivers.gradle` and `build-drivers.gradle`. Do not re-enable without user confirmation and valid GitHub Packages credentials.

### Single-statement if without braces

**Symptom**: Checkstyle failure for "need braces" on single-statement if blocks

**Fix**: Always use braces, even for single statements:
```java
// Wrong
if (condition)
    doSomething();

// Right
if (condition) {
    doSomething();
}
```

### Missing Javadoc on public methods

**Symptom**: Checkstyle failure for "missing a Javadoc comment"

**Fix**: Add Javadoc with `@param`, `@return`, `@throws` as appropriate:
```java
/**
 * Does something.
 * @param param the parameter
 * @return the result
 * @throws IOException if an I/O error occurs
 */
public String doSomething(String param) throws IOException {
    // ...
}
```

## Build commands reference

### Common commands

```bash
# Compile only (fastest iteration)
./gradlew :drivers:<name>:compileJava

# Checkstyle + compile + tests for one module
./gradlew :drivers:<name>:check

# Full project checkstyle + compile + tests
./gradlew check

# Build distribution scripts
./gradlew installDist

# Build distribution archives (ZIP/TAR)
./gradlew distZip distTar

# Clean everything
./gradlew clean

# Clean rebuild with pathing JAR refresh
rm -rf build && ./gradlew clean :pathingJar installDist --rerun-tasks
```

### Driver-specific commands

```bash
# Build and check a single driver
./gradlew :drivers:minio:check
./gradlew :drivers:minio:installDist

# List all drivers
./gradlew printDrivers -Preadme

# Add a new driver (interactive)
./gradlew addDriver -Pdriver=mynewdriver

# Delete a driver
./gradlew deleteDriver -Pdriver=mydriver
```

## Verification criteria

A change is **complete** only when:

1. ✅ `./gradlew :<module>:check` passes (module-level)
2. ✅ `./gradlew check` passes (full project, no regressions)
3. ✅ `./gradlew installDist` succeeds and produces working `sbk` script
4. ✅ Smoke test runs without errors (for driver changes)
5. ✅ No checkstyle violations
6. ✅ No test failures

## Related documentation

- <ref_file file="/root/projects/SBK/AGENTS.md" /> - §2: Build, run, and verify
- <ref_file file="/root/projects/SBK/AGENTS.md" /> - §4: Known gotchas (especially §4.4 pathing JAR)
- <ref_file file="/root/projects/SBK/docs/AGENT_RECIPES.md" /> - Recipe 7: Bump a driver's vendor SDK version

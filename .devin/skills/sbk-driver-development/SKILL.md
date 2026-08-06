# SBK Driver Development Skill

> Helps AI agents add new storage drivers or modify existing drivers in the SBK repository.

## When to use this skill

Invoke this skill when:
- Adding a new storage driver to SBK
- Modifying an existing driver (adding CLI flags, fixing bugs, changing behavior)
- A driver change requires updating build configuration

## What this skill provides

### Context
- SBK driver SPI contract and implementation patterns
- File structure conventions for drivers
- Build system integration (settings-drivers.gradle, build-drivers.gradle)
- Checkstyle and import-control requirements
- Common gotchas specific to SBK drivers

### Guidance
- Step-by-step workflow for adding a new driver from the sbktemplate scaffold
- Pattern for adding CLI flags (addArgs → Config class → properties file)
- Verification checklist specific to driver changes
- Error handling patterns for driver shutdown

### Required capabilities

This skill does not grant permissions. The agent still follows `AGENTS.md`
and its host tool's approval model. Driver work normally needs scoped edits
under `drivers/<name>/`, both driver-registration files, and sometimes
`checkstyle/import-control.xml`, plus permission to run the Gradle wrapper.

## Workflow

### Adding a new driver

1. **Copy scaffold**: `cp -r drivers/sbktemplate drivers/<newname>`
2. **Rename packages and classes**: Replace `SbkTemplate` with `PascalCaseName` throughout
3. **Edit build.gradle**: Add vendor SDK dependency
4. **Register in build system**:
   - Add `include 'drivers:<name>'` to `settings-drivers.gradle`
   - Add `api project(':drivers:<name>')` to `build-drivers.gradle`
5. **Update checkstyle**: If SDK brings new top-level packages, add to `checkstyle/import-control.xml`
6. **Implement the four classes**: `<Name>.java`, `<Name>Writer.java`, `<Name>Reader.java`, `<Name>Config.java`
7. **Add README.md**: Document driver with at least one write and one read example
8. **Verify**: Run `./gradlew :drivers:<name>:check`, `./gradlew check`, `./gradlew installDist`, smoke test

### Modifying an existing driver

1. **Add flag in addArgs()**: `params.addOption("flag", true, "description")`
2. **Add field to Config class**: Match the property name
3. **Add default to properties file**: In `drivers/<name>/src/main/resources/<name>.properties`
4. **Parse in parseArgs()**: Read the flag value into config
5. **Use the flag**: Act on it in `openStorage()`, `createWriter()`, or `createReader()`
6. **Update README.md**: Document the new flag with an example
7. **Verify**: Run `./gradlew :drivers:<name>:check` and test the new flag

## Common gotchas

### Dual Gradle file requirement
New drivers MUST be added to BOTH:
- `settings-drivers.gradle` (include statement)
- `build-drivers.gradle` (api dependency)

Forgetting either causes "driver not found" errors.

### Checkstyle import control
If the vendor SDK introduces a new top-level package (e.g., `software.amazon`, `okhttp3`), you MUST add it to `checkstyle/import-control.xml`:
```xml
<allow pkg="package.name" />
```

### Package naming convention
Driver directory: lowercase (`minio`)
Java package: PascalCase (`io.sbk.driver.MinIO`)
Class name: PascalCase, matches directory case (`MinIO`)

### Shutdown handling
Drivers MUST treat `InterruptedIOException` and `RejectedExecutionException` as clean shutdowns, not errors. SBK tears down HTTP clients mid-call when the benchmark duration expires.

### No synchronization in hot path
Do NOT add `synchronized` blocks or `Lock` usage in `writeAsync()` or `read()` methods. The harness depends on lock-free behavior.

## Verification checklist

Before considering a driver change complete:
- [ ] `./gradlew :drivers:<name>:check` passes
- [ ] `./gradlew check` passes (no regression elsewhere)
- [ ] `./gradlew installDist` produces working distribution
- [ ] `sbk -class <name> -help` lists the driver (and new flags if any)
- [ ] Smoke test against real backend runs for 30+ seconds without errors
- [ ] Driver README.md exists with at least one write and one read example
- [ ] If new packages introduced, `checkstyle/import-control.xml` is updated
- [ ] Both `settings-drivers.gradle` and `build-drivers.gradle` include the driver
- [ ] No `synchronized` blocks or `Lock` usage in per-record hot path

## Related documentation

- [`AGENTS.md`](../../../AGENTS.md) - repository conventions and gotchas
- [`docs/AGENT_RECIPES.md`](../../../docs/AGENT_RECIPES.md) - add-driver recipe
- [`docs/DRIVER_SPECIFICATION.md`](../../../docs/DRIVER_SPECIFICATION.md) - driver specification template
- [`drivers/sbktemplate`](../../../drivers/sbktemplate/) - starting scaffold

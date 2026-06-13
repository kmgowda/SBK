# Agent Documentation Distribution Across Release Channels

> Complete guide to how agent documentation is packaged and distributed through all SBK release channels.

## Overview

The SBK agent documentation (AGENTS.md, INSTRUCTIONS.md, Devin skills, Cursor rules, Aider config, etc.) is now included in all release channels to ensure AI agents have comprehensive context regardless of how they obtain SBK.

## Release Channels

### 1. Gradle Distributions (installDist, distZip, distTar)

**What's included:**
- `AGENTS.md` - Universal agent entry point
- `INSTRUCTIONS.md` - Quick-start guide
- `README.md` - User manual
- `docs/AGENT_RECIPES.md` - Step-by-step recipes
- `docs/DRIVER_SPECIFICATION.md` - Driver spec template
- `docs/AGENT_TOOLKIT.md` - Complete toolkit guide
- `docs/sbk-internals.md` - Architecture documentation
- `.devin/skills/` - Devin executable skills (3 skills)
- `.cursorrules` - Cursor-specific rules
- `.aider.conf.yml` - Aider configuration

**Distribution structure:**
```
build/install/sbk/
├── AGENTS.md
├── INSTRUCTIONS.md
├── README.md
├── docs/
│   ├── AGENT_RECIPES.md
│   ├── DRIVER_SPECIFICATION.md
│   ├── AGENT_TOOLKIT.md
│   └── sbk-internals.md
├── .devin/
│   └── skills/
│       ├── sbk-driver-development/
│       ├── sbk-build-verify/
│       └── sbk-benchmark-runner/
├── .cursorrules
├── .aider.conf.yml
├── bin/
└── lib/
```

**How to build:**
```bash
./gradlew installDist    # Creates build/install/sbk/
./gradlew distZip        # Creates build/distributions/sbk-10.1.zip
./gradlew distTar        # Creates build/distributions/sbk-10.1.tar
```

**Used by:** Users downloading SBK distributions directly

### 2. Maven Packages (Maven Central, GitHub Packages)

**What's included:**
- `sbk-10.1-docs.jar` - Separate JAR containing all agent documentation

**JAR structure:**
```
sbk-10.1-docs.jar
├── docs/
│   ├── AGENTS.md
│   ├── INSTRUCTIONS.md
│   ├── README.md
│   ├── AGENT_RECIPES.md
│   ├── DRIVER_SPECIFICATION.md
│   ├── AGENT_TOOLKIT.md
│   └── sbk-internals.md
├── devin-skills/
│   ├── sbk-driver-development/SKILL.md
│   ├── sbk-build-verify/SKILL.md
│   └── sbk-benchmark-runner/SKILL.md
└── agent-configs/
    ├── .cursorrules
    └── .aider.conf.yml
```

**Maven coordinates:**
```xml
<dependency>
  <groupId>io.github.kmgowda.sbk</groupId>
  <artifactId>sbk</artifactId>
  <version>10.1</version>
  <classifier>docs</classifier>
</dependency>
```

**How to publish:**
```bash
# To GitHub Packages
./gradlew publish -Pgithub

# To Maven Central (via jReleaser)
./gradlew publish
./gradlew jreleaserFullRelease
```

**Used by:** Maven users, build systems, automated dependency management

### 3. jReleaser Deployments

**Configuration in gradle/maven.gradle:**
- Enabled GitHub releases
- Enabled upload to Maven Central
- Added `docsJar: true` to Maven Central deployment
- Includes signed documentation JAR

**jReleaser workflow:**
```bash
# 1. Verify configuration
./gradlew jreleaserConfig

# 2. Stage artifacts locally
./gradlew publish

# 3. Deploy to Maven Central
./gradlew jreleaserFullRelease
```

**What gets deployed:**
- Main JAR (`sbk-10.1.jar`)
- Sources JAR (`sbk-10.1-sources.jar`)
- Javadoc JAR (`sbk-10.1-javadoc.jar`)
- **Docs JAR (`sbk-10.1-docs.jar`) - NEW**
- All JARs are signed

**Used by:** Automated release process, Maven Central users

### 4. GitHub Releases

**What's included:**
- Distribution archives (ZIP/TAR) with embedded documentation
- Individual documentation files as separate assets
- `sbk-agent-docs.tar.gz` - Compressed archive of all agent documentation

**Release assets:**
```
Release v10.1
├── sbk-10.1.zip                    # Distribution with docs
├── sbk-10.1.tar                    # Distribution with docs
├── sbk-agent-docs.tar.gz           # Standalone docs archive
├── AGENTS.md                       # Individual file
├── INSTRUCTIONS.md                 # Individual file
├── docs/AGENT_RECIPES.md          # Individual file
├── docs/DRIVER_SPECIFICATION.md   # Individual file
├── docs/AGENT_TOOLKIT.md          # Individual file
└── docs/sbk-internals.md          # Individual file
```

**GitHub Actions workflow:**
- Triggered on release creation
- Builds distribution archives
- Creates documentation archive
- Uploads all assets to GitHub release

**How to create release:**
```bash
# Tag the release
git tag -a v10.1 -m "Release v10.1"

# Push the tag
git push origin v10.1

# Or create via GitHub UI
# GitHub Actions will automatically build and attach assets
```

**Used by:** Users downloading from GitHub releases, CI/CD systems

## Configuration Details

### build.gradle Changes

Added to the `application` block:
```gradle
// Include documentation files in the distribution
applicationDistribution.into("docs") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("docs") {
        include "AGENT_RECIPES.md"
        include "DRIVER_SPECIFICATION.md"
        include "AGENT_TOOLKIT.md"
        include "sbk-internals.md"
    }
}

// Include AGENTS.md, INSTRUCTIONS.md, and README.md at the root
applicationDistribution.from("AGENTS.md")
applicationDistribution.from("INSTRUCTIONS.md")
applicationDistribution.from("README.md")

// Include agent-specific configurations
applicationDistribution.into(".devin") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(".devin") {
        include "skills/**/*"
    }
}
applicationDistribution.from(".cursorrules")
applicationDistribution.from(".aider.conf.yml")
```

### gradle/maven.gradle Changes

**Added docsJar task:**
```gradle
tasks.register('docsJar', Jar) {
    archiveBaseName = project.name
    archiveClassifier = 'docs'
    from('AGENTS.md') { into 'docs' }
    from('INSTRUCTIONS.md') { into 'docs' }
    from('README.md') { into 'docs' }
    from('docs') {
        include 'AGENT_RECIPES.md'
        include 'DRIVER_SPECIFICATION.md'
        include 'AGENT_TOOLKIT.md'
        include 'sbk-internals.md'
        into 'docs'
    }
    from('.devin/skills') { into 'devin-skills' }
    from('.cursorrules') { into 'agent-configs' }
    from('.aider.conf.yml') { into 'agent-configs' }
}
```

**Added to Maven publication:**
```gradle
publications {
    mavenJava(MavenPublication) {
        from(components.java)
        artifact(tasks.docsJar)  // NEW
        // ... rest of configuration
    }
}
```

**Updated jReleaser configuration:**
```gradle
jreleaser {
    release {
        github {
            enabled = true
            skipRelease = false
            skipTag = false
            overwrite = true
        }
    }

    deploy {
        maven {
            mavenCentral {
                sonatype {
                    // ...
                    docsJar = true  // NEW
                    // ...
                }
            }
        }
    }
}
```

### .github/workflows/gradle.yml Changes

**Enhanced release job:**
```yaml
- name: Create documentation archive
  run: |
    mkdir -p docs-archive
    cp AGENTS.md docs-archive/
    cp INSTRUCTIONS.md docs-archive/
    cp README.md docs-archive/
    cp .cursorrules docs-archive/
    cp .aider.conf.yml docs-archive/
    cp -r docs docs-archive/
    cp -r .devin/skills docs-archive/
    cd docs-archive
    tar -czf ../sbk-agent-docs.tar.gz *

- name: Upload release assets
  uses: softprops/action-gh-release@v1
  with:
    files: |
      build/distributions/*.zip
      build/distributions/*.tar
      sbk-agent-docs.tar.gz
      AGENTS.md
      INSTRUCTIONS.md
      docs/AGENT_RECIPES.md
      docs/DRIVER_SPECIFICATION.md
      docs/AGENT_TOOLKIT.md
      docs/sbk-internals.md
```

## Verification

### Verify Gradle Distribution
```bash
./gradlew installDist
ls -la build/install/sbk/
ls -la build/install/sbk/docs/
ls -la build/install/sbk/.devin/skills/
```

### Verify Maven Package
```bash
./gradlew publish
ls -la build/sbk-repo/io/github/kmgowda/sbk/sbk/10.1/
# Should see sbk-10.1-docs.jar
```

### Verify jReleaser Configuration
```bash
./gradlew jreleaserConfig
# Check that docsJar is enabled
```

### Verify GitHub Release
1. Create a release on GitHub
2. Check the release assets
3. Verify all documentation files are present

## Benefits

### For AI Agents
- **Always available:** Documentation is included regardless of how SBK is obtained
- **Complete context:** All agent-specific configs (Devin, Cursor, Aider) are included
- **Version-specific:** Documentation matches the exact SBK version being used
- **Easy access:** Individual files available in GitHub releases

### For Users
- **Single download:** Get everything needed in one distribution archive
- **Maven-friendly:** Documentation available as standard Maven artifact
- **GitHub-friendly:** Easy access to documentation via release assets
- **Version tracking:** Documentation version matches SBK version

### For Maintainers
- **Automated:** Documentation inclusion is automated in build process
- **Consistent:** Same documentation across all channels
- **Versioned:** Documentation is versioned with releases
- **Traceable:** Clear mapping between SBK version and documentation version

## Summary

The agent documentation is now comprehensively distributed across all SBK release channels:

✅ **Gradle distributions** - Embedded in installDist, distZip, distTar
✅ **Maven packages** - As separate -docs.jar artifact
✅ **jReleaser deployments** - Included in Maven Central uploads
✅ **GitHub releases** - As individual files and compressed archive

This ensures that any AI agent (Devin, Cursor, Aider, Claude, Copilot, Windsurf, etc.) can access the complete agent toolkit regardless of how SBK is obtained or deployed.

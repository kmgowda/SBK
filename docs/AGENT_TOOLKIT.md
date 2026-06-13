# AI Agent Toolkit for SBK

> Complete guide to agent-friendly documentation and configurations for the SBK repository.

## Overview

This repository now has comprehensive support for AI-powered development and benchmarking across multiple agent platforms (Devin, Cursor, Aider, Claude Code, GitHub Copilot, Windsurf, etc.).

## What's Available

### 1. Universal Documentation (All Agents)

#### AGENTS.md (Main Entry Point)
- **Location:** `/root/projects/SBK/AGENTS.md`
- **Purpose:** Universal entry point for all AI agents
- **Contents:**
  - Project overview and module map
  - Build, run, and verify commands
  - Repository conventions and file structure
  - 8 common gotchas with detailed explanations
  - Two AI workflows (vibe coding vs spec-driven)
  - Out-of-scope actions requiring user approval
  - Quick agent self-check questions
- **Used by:** All agents (Devin, Cursor, Aider, Claude, Copilot, Windsurf, etc.)

#### INSTRUCTIONS.md (Quick Start)
- **Location:** `/root/projects/SBK/INSTRUCTIONS.md`
- **Purpose:** Quick-start guide for agents who need immediate context
- **Contents:**
  - SBK overview in 30 seconds
  - Critical conventions (must-read before starting)
  - Build commands reference
  - Driver development quick reference
  - Verification checklist
  - Links to detailed documentation
- **Used by:** All agents as a fast on-ramp

#### docs/AGENT_RECIPES.md (Step-by-Step Procedures)
- **Location:** `/root/projects/SBK/docs/AGENT_RECIPES.md`
- **Purpose:** Detailed, copy-pasteable recipes for common tasks
- **Contents:** 8 numbered recipes
  1. Add a new storage driver
  2. Modify an existing driver
  3. Add a new logger / metrics exporter
  4. Add a new CLI flag at the harness level
  5. Debug a driver that fails at runtime
  6. Update or extend the architecture documentation
  7. Bump a driver's vendor SDK version
  8. Run a benchmark against a new cluster
- **Used by:** All agents for detailed task execution

#### docs/DRIVER_SPECIFICATION.md (Spec-Driven Development)
- **Location:** `/root/projects/SBK/docs/DRIVER_SPECIFICATION.md`
- **Purpose:** Fillable template for formal driver specifications
- **Contents:**
  - Fillable spec template (metadata, requirements, config, behavior, test plan)
  - Worked example (MinIO/S3 driver as complete spec)
  - Acceptance checklist
- **Used by:** Agents doing formal, auditable driver development

### 2. Platform-Specific Configurations

#### Devin Skills (Executable Capabilities)
- **Location:** `/root/projects/SBK/.devin/skills/`
- **Purpose:** Devin-specific executable skills with permissions
- **Skills:**
  1. **sbk-driver-development** (101 lines)
     - Guides adding/modifying drivers
     - Context: Driver SPI, file structure, build integration
     - Permissions: Read/write to drivers, build configs, checkstyle
  2. **sbk-build-verify** (147 lines)
     - Handles SBK build and verification workflow
     - Context: JDK 25 requirement, build sequence, common failures
     - Permissions: Exec for gradlew/java, read/write to build dir
  3. **sbk-benchmark-runner** (224 lines)
     - Helps run performance benchmarks
     - Context: Command structure, driver flags, metric interpretation
     - Permissions: Exec for sbk binary, read access to driver docs
- **Used by:** Devin CLI agents

#### Cursor Rules
- **Location:** `/root/projects/SBK/.cursorrules`
- **Purpose:** Cursor-specific rules and conventions
- **Contents:**
  - Project overview
  - Key conventions (build system, code style, file structure)
  - Common gotchas
  - Driver development workflow
  - Verification checklist
  - Documentation references
  - Architecture invariants
- **Used by:** Cursor AI

#### Aider Configuration
- **Location:** `/root/projects/SBK/.aider.conf.yml`
- **Purpose:** Aider-specific configuration with system prompt
- **Contents:**
  - Model settings
  - File exclusions (build/, .gradle/, etc.)
  - Read-only files
  - Auto-commit settings
  - SBK-specific system prompt (full context from AGENTS.md)
  - Always-read files (AGENTS.md, AGENT_RECIPES.md)
  - Suggested verification commands
- **Used by:** Aider AI

### 3. Supporting Documentation

#### docs/sbk-internals.md
- **Purpose:** Internal architecture, PerL methodology, mermaid diagrams
- **Used by:** Agents needing deep understanding of SBK internals

#### README.md
- **Purpose:** End-user manual for running SBK benchmarks
- **Used by:** Agents and humans for usage reference

## How Each Agent Platform Uses This

### Devin
1. **Primary entry:** AGENTS.md (universal)
2. **Executable skills:** `.devin/skills/` (3 skills for specific tasks)
3. **Workflow:** Agent reads AGENTS.md → invokes appropriate skill → follows guidance
4. **Permissions:** Skills grant scoped tool access for specific tasks

### Cursor
1. **Primary entry:** `.cursorrules` (auto-loaded by Cursor)
2. **Fallback:** AGENTS.md if more detail needed
3. **Workflow:** Cursor loads .cursorrules → agent follows rules → consults AGENTS.md for details
4. **Integration:** Cursor automatically reads .cursorrules at project open

### Aider
1. **Primary entry:** `.aider.conf.yml` (auto-loaded by Aider)
2. **System prompt:** Contains condensed SBK context
3. **Always-read:** AGENTS.md and AGENT_RECIPES.md loaded each session
4. **Workflow:** Aider loads config → agent sees system prompt → reads always-read files → proceeds

### Claude Code
1. **Primary entry:** AGENTS.md (universal entry point)
2. **Fallback:** INSTRUCTIONS.md for quick context
3. **Workflow:** Agent reads AGENTS.md → follows conventions → consults AGENT_RECIPES.md for procedures
4. **Integration:** Claude Code looks for AGENTS.md automatically

### GitHub Copilot
1. **Primary entry:** AGENTS.md (context from repo)
2. **Workflow:** Copilot uses repo context → AGENTS.md provides conventions
3. **Integration:** Copilot Chat can reference AGENTS.md for guidance

### Windsurf
1. **Primary entry:** AGENTS.md (universal)
2. **Fallback:** INSTRUCTIONS.md for quick start
3. **Workflow:** Similar to Claude Code - reads AGENTS.md first
4. **Integration:** Windsurf respects standard documentation files

### Continue
1. **Primary entry:** AGENTS.md (universal)
2. **Workflow:** Continue reads AGENTS.md for project context
3. **Integration:** Continue uses standard markdown documentation

### OpenAI Codex
1. **Primary entry:** AGENTS.md (universal)
2. **Workflow:** Codex uses AGENTS.md as context for code generation
3. **Integration:** Codex can reference documentation files

## File Structure Summary

```
/root/projects/SBK/
├── AGENTS.md                          # Universal entry point (all agents)
├── INSTRUCTIONS.md                    # Quick start (all agents)
├── .cursorrules                       # Cursor-specific rules
├── .aider.conf.yml                    # Aider configuration
├── .devin/
│   ├── config.local.json             # Devin permissions
│   └── skills/                       # Devin executable skills
│       ├── sbk-driver-development/
│       │   └── SKILL.md
│       ├── sbk-build-verify/
│       │   └── SKILL.md
│       └── sbk-benchmark-runner/
│           └── SKILL.md
├── docs/
│   ├── AGENT_RECIPES.md              # Step-by-step recipes (all agents)
│   ├── DRIVER_SPECIFICATION.md      # Driver spec template (all agents)
│   └── sbk-internals.md             # Architecture documentation
└── README.md                         # User manual (humans + agents)
```

## Verification Checklist

To verify the agent toolkit is complete:

- [x] AGENTS.md exists and is comprehensive
- [x] INSTRUCTIONS.md exists as quick-start guide
- [x] docs/AGENT_RECIPES.md exists with 8 recipes
- [x] docs/DRIVER_SPECIFICATION.md exists with template and worked example
- [x] .cursorrules exists for Cursor
- [x] .aider.conf.yml exists for Aider
- [x] .devin/skills/ exists with 3 skills
- [x] All files reference each other correctly
- [x] AGENTS.md updated to reference agent-specific configs
- [x] Documentation included in Gradle distribution (build.gradle updated)
- [x] GitHub Actions workflow updated for releases

## Usage Examples

### Example 1: Devin agent adding a driver
1. Agent reads AGENTS.md
2. Agent invokes `sbk-driver-development` skill
3. Skill provides context, permissions, and workflow
4. Agent follows skill guidance
5. Agent invokes `sbk-build-verify` skill for verification
6. Agent invokes `sbk-benchmark-runner` skill for testing

### Example 2: Cursor agent modifying a driver
1. Cursor loads .cursorrules automatically
2. Agent follows rules for driver modification
3. Agent consults AGENTS.md for detailed conventions
4. Agent follows AGENT_RECIPES.md Recipe 2 step-by-step
5. Agent uses verification checklist from .cursorrules

### Example 3: Aider agent running a benchmark
1. Aider loads .aider.conf.yml automatically
2. Agent sees SBK system prompt
3. Aider loads AGENTS.md and AGENT_RECIPES.md (always-read)
4. Agent follows benchmark workflow
5. Agent uses suggested commands from config

### Example 4: Claude Code agent debugging
1. Agent reads AGENTS.md (universal entry)
2. Agent reads INSTRUCTIONS.md for quick context
3. Agent follows AGENT_RECIPES.md Recipe 5 (debugging)
4. Agent uses common gotchas from AGENTS.md
5. Agent verifies with build commands

## Benefits

### For AI Agents
- **Immediate context:** All agents have project-specific knowledge without training
- **Reduced errors:** Common gotchas documented and prevented
- **Faster onboarding:** Quick-start guide + detailed documentation
- **Platform-specific optimization:** Each agent gets optimized configuration
- **Executable guidance:** Devin skills provide active guidance with permissions

### For Human Developers
- **Consistent agent behavior:** All agents follow same conventions
- **Better agent assistance:** Agents have full project context
- **Reduced supervision:** Agents can work more independently
- **Quality assurance:** Verification checklists and acceptance criteria
- **Documentation maintenance:** Single source of truth for all agents

### For the Project
- **Lower barrier to entry:** New contributors (human or AI) can contribute quickly
- **Consistent code quality:** All changes follow same conventions
- **Better benchmarking:** Agents can run and interpret benchmarks correctly
- **Easier maintenance:** Documentation centralizes project knowledge
- **Future-proof:** Works across current and future agent platforms

## Maintenance

When updating SBK:
1. Update AGENTS.md if conventions change
2. Update AGENT_RECIPES.md if workflows change
3. Update DRIVER_SPECIFICATION.md if SPI changes
4. Update .cursorrules if code style changes
5. Update .aider.conf.yml if build process changes
6. Update Devin skills if new common tasks emerge
7. Update INSTRUCTIONS.md if critical gotchas change
8. Ensure all cross-references remain valid

## Summary

The SBK repository now has a complete, multi-platform agent toolkit that:

✅ **Works universally** across Devin, Cursor, Aider, Claude, Copilot, Windsurf, Continue, and others
✅ **Provides platform-specific optimization** for each major agent
✅ **Includes executable capabilities** via Devin skills
✅ **Covers all common tasks** (driver development, build/verify, benchmarking)
✅ **Prevents common errors** through gotchas documentation
✅ **Ensures quality** through verification checklists
✅ **Scales with the project** through maintainable documentation structure

Any AI agent working on this repository will have the context, guidance, and permissions needed to contribute effectively.

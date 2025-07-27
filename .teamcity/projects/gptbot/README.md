# Automated Semantic Versioning with Conventional Commits

This document explains the automated semantic versioning system implemented in the TeamCity build configuration for the `wwhatsapp-node` project. The system automatically determines version increments based on commit message patterns following the [Conventional Commits](https://www.conventionalcommits.org/) specification.

## 🎯 Overview

The build system analyzes commit messages since the last Git tag to automatically determine the appropriate semantic version increment, eliminating manual version management while ensuring consistent versioning practices.

## 📋 Commit Message Patterns

### Version Increment Rules

| Commit Pattern | Version Impact | Example | Version Change |
|----------------|----------------|---------|----------------|
| `release: <message>` | **Major** (X.0.0) | `release: new API version` | `1.2.3` → `2.0.0` |
| `BREAKING CHANGE:` | **Major** (X.0.0) | `feat!: remove deprecated endpoints` | `1.2.3` → `2.0.0` |
| `feat: <message>` | **Minor** (x.Y.0) | `feat: add user authentication` | `1.2.3` → `1.3.0` |
| `fix: <message>` | **Patch** (x.y.Z) | `fix: resolve login timeout` | `1.2.3` → `1.2.4` |
| `docs:`, `style:`, etc. | **Patch** (x.y.Z) | `docs: update API documentation` | `1.2.3` → `1.2.4` |

### Supported Conventional Commit Types

#### Major Version Increment (Breaking Changes)
- `release:` - Major release announcements
- `BREAKING CHANGE:` - API breaking changes
- `feat!:` - Breaking feature changes (with exclamation mark)

#### Minor Version Increment (New Features)
- `feat:` - New features
- `feat(scope):` - Scoped new features

#### Patch Version Increment (Bug Fixes & Maintenance)
- `fix:` - Bug fixes
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, missing semicolons, etc.)
- `refactor:` - Code refactoring without functionality changes
- `perf:` - Performance improvements
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks, dependency updates
- `ci:` - CI/CD configuration changes
- `build:` - Build system or external dependency changes

## 🔄 Versioning Logic Flow

### 1. Tagged Commit (Release Build)
```bash
# Current commit has a Git tag
git tag v1.2.3
git push origin v1.2.3

# TeamCity Output:
# ✅ Found exact Git tag: v1.2.3
# 📦 Release version: 1.2.3
# 🏷️ Docker images: 
#    - protonmath/wwhatsapp-node:1.2.3
#    - protonmath/wwhatsapp-node:latest
```

### 2. Development Build (Commits After Tag)
```bash
# Latest tag: v1.2.3
# New commits since tag:
git commit -m "feat: add dark mode support"
git commit -m "fix: resolve theme switching bug"

# TeamCity Analysis:
# 🔍 Analyzing commits since v1.2.3...
#   📝 Commit: feat: add dark mode support
#     ✨ Feature detected
#   📝 Commit: fix: resolve theme switching bug
#     🐛 Fix/Other change detected
# 📈 Version bump type: minor (feat overrides fix)
# 🔄 Next version would be: 1.3.0
# 🚧 Pre-release version: 1.3.0-alpha.2+a1b2c3d
```

### 3. Fresh Repository (No Tags)
```bash
# No tags in repository
# TeamCity Output:
# 🆕 No tags found, using initial version: 0.1.0-alpha.42+c3d4e5f
```

## 🌟 Version Format Examples

### Release Versions (Tagged Commits)
- `1.0.0` - Major release
- `1.2.0` - Minor release  
- `1.2.3` - Patch release

### Pre-Release Versions (Development)

#### Main/Master Branch
- `1.3.0-alpha.5+a1b2c3d` - 5 commits ahead of v1.2.3, next minor version
- `2.0.0-alpha.3+b2c3d4e` - 3 commits ahead with breaking changes

#### Feature Branches
- `1.3.0-feature-auth.2+c3d4e5f` - Feature branch with 2 commits
- `1.2.4-bugfix-login.1+d4e5f6a` - Bug fix branch with 1 commit

## 🚀 Development Workflow

### For Feature Development
```bash
# 1. Create feature branch
git checkout -b feature/user-dashboard
git push -u origin feature/user-dashboard

# 2. Make commits with conventional messages
git commit -m "feat: add user dashboard layout"
git commit -m "feat: implement dashboard widgets"
git commit -m "fix: resolve responsive design issues"
git commit -m "test: add dashboard component tests"

# 3. TeamCity automatically builds:
# Version: 1.3.0-feature-user-dashboard.4+f1a2b3c
# (Next version would be 1.3.0 due to 'feat:' commits)
```

### For Bug Fixes
```bash
# 1. Create bug fix branch
git checkout -b bugfix/memory-leak
git push -u origin bugfix/memory-leak

# 2. Make commits
git commit -m "fix: resolve memory leak in session management"
git commit -m "test: add memory usage tests"

# 3. TeamCity automatically builds:
# Version: 1.2.4-bugfix-memory-leak.2+a3b4c5d
# (Next version would be 1.2.4 due to 'fix:' commits)
```

### For Releases
```bash
# 1. Merge to main/master
git checkout main
git merge feature/user-dashboard
git push origin main

# 2. TeamCity builds pre-release:
# Version: 1.3.0-alpha.6+g2h3i4j

# 3. When ready to release, create tag
git tag v1.3.0
git push origin v1.3.0

# 4. TeamCity builds final release:
# Version: 1.3.0
# Docker images: protonmath/wwhatsapp-node:1.3.0, latest
```

## 🔍 Priority System

When multiple commit types exist since the last tag, the system uses this priority:

1. **Major** (Breaking changes) - Highest priority
2. **Minor** (Features) - Medium priority  
3. **Patch** (Fixes/Other) - Lowest priority

### Example: Mixed Commits
```bash
# Commits since v1.2.3:
git commit -m "fix: resolve login bug"          # Patch
git commit -m "feat: add user preferences"     # Minor  
git commit -m "docs: update README"            # Patch

# Result: Minor version bump (1.3.0) due to 'feat:' presence
```

```bash
# Commits since v1.2.3:
git commit -m "feat: add new feature"          # Minor
git commit -m "fix: resolve bug"              # Patch
git commit -m "release: breaking API changes" # Major

# Result: Major version bump (2.0.0) due to 'release:' presence
```

## 🏷️ Docker Image Tagging Strategy

### Release Images (Tagged Commits)
```bash
# For tag v1.2.3:
protonmath/wwhatsapp-node:1.2.3
protonmath/wwhatsapp-node:latest
```

### Development Images (Pre-release)
```bash
# Main branch:
protonmath/wwhatsapp-node:1.3.0-alpha.5+a1b2c3d
protonmath/wwhatsapp-node:latest

# Feature branch:
protonmath/wwhatsapp-node:1.3.0-feature-auth.2+b2c3d4e  
protonmath/wwhatsapp-node:latest
```

## 📊 TeamCity Build Output

### Successful Analysis Example
```
=== Git Tag Version Extraction with Conventional Commits ===
Current branch: main
Current commit: a1b2c3d4e5f6789...

📋 Latest tag: v1.2.3
🔢 Commits since tag: 3
🔍 Analyzing commits since v1.2.3...
  📝 Commit: feat: add user authentication
    ✨ Feature detected
  📝 Commit: fix: resolve login timeout
    🐛 Fix/Other change detected  
  📝 Commit: docs: update API documentation
    🐛 Fix/Other change detected
📈 Version bump type: minor
🔄 Next version would be: 1.3.0
🚧 Pre-release version: 1.3.0-alpha.3+a1b2c3d

=== Final Result ===
🏷️ Semantic Version: 1.3.0-alpha.3+a1b2c3d
```

## 🎯 Benefits

### ✅ Automated Version Management
- No manual version updates required
- Consistent versioning across all builds
- Reduces human error in version assignment

### ✅ Semantic Versioning Compliance
- Follows [SemVer](https://semver.org/) specification
- Clear distinction between breaking, feature, and patch changes
- Predictable version increments

### ✅ Git Integration
- Uses Git tags as source of truth for releases
- Includes commit hashes for traceability
- Branch-aware versioning for development workflows

### ✅ CI/CD Integration
- Seamless integration with TeamCity
- Automatic Docker image tagging
- Build parameters for additional automation

## 🛠️ Advanced Features

### Version Parameters
The build sets additional parameters for advanced use cases:
```bash
env.SEMANTIC_VERSION=1.3.0-alpha.3+a1b2c3d
env.VERSION_MAJOR=1
env.VERSION_MINOR=3  
env.VERSION_PATCH=0
```

### Branch-Specific Behavior
- **Main/Master**: Clean pre-release versions (`-alpha.N`)
- **Feature branches**: Include sanitized branch name (`-feature-name.N`)
- **All branches**: Include git commit hash (`+hash`)

## 📝 Best Practices

### Commit Message Guidelines
```bash
# ✅ Good examples:
git commit -m "feat: add user authentication system"
git commit -m "fix: resolve memory leak in session handler"
git commit -m "docs: update API documentation for v2"
git commit -m "release: major API restructure"

# ❌ Avoid:
git commit -m "update stuff"
git commit -m "fixes"
git commit -m "WIP"
```

### Release Management
1. **Use feature branches** for development
2. **Merge to main** when features are complete  
3. **Create tags** only for stable releases
4. **Use descriptive commit messages** following conventional format
5. **Review version increments** before tagging releases

## 🔧 Configuration Details

The versioning logic is implemented in the TeamCity build step `Extract Git Tag Version` using bash scripting with:

- **Git tag analysis** for current version detection
- **Commit message parsing** with regex patterns
- **Version increment calculation** based on semver rules
- **Branch-aware naming** for development versions
- **Parameter injection** for TeamCity integration

This system provides a robust, automated approach to semantic versioning that scales with your development workflow while maintaining consistency and traceability.
